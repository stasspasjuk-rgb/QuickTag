package io.github.quickproducts.quicktag;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Changes only nametag visibility; never moves entries out of existing teams. */
final class NametagManager implements AutoCloseable {
    private final Map<Scoreboard, BoardState> boards = new IdentityHashMap<>();

    void refresh(Scoreboard main, Collection<? extends Player> players) {
        Set<Scoreboard> active = Collections.newSetFromMap(new IdentityHashMap<>());
        active.add(main);
        Set<String> names = new HashSet<>();
        for (Player player : players) {
            active.add(player.getScoreboard());
            names.add(player.getName());
        }
        // Release unused personal boards so repeated board changes do not leak memory.
        var iterator = boards.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!active.contains(entry.getKey())) {
                entry.getValue().restore(entry.getKey());
                iterator.remove();
            }
        }
        for (Scoreboard board : active) {
            boards.computeIfAbsent(board, ignored -> new BoardState()).apply(board, names);
        }
    }

    @Override
    public void close() {
        boards.forEach((board, state) -> state.restore(board));
        boards.clear();
    }

    private static final class BoardState {
        private final Map<Team, Team.OptionStatus> original = new HashMap<>();
        private Team own;

        void apply(Scoreboard board, Set<String> names) {
            original.keySet().removeIf(team -> !team.equals(board.getTeam(team.getName())));
            if (own != null && !own.equals(board.getTeam(own.getName()))) {
                own = null;
            }
            if (own != null) {
                for (String entry : new HashSet<>(own.getEntries())) {
                    if (!names.contains(entry)) own.removeEntry(entry);
                }
            }
            for (String name : names) {
                Team team = board.getEntryTeam(name);
                if (team == null) {
                    if (own == null) {
                        String id = "quicktag_hidden";
                        int suffix = 0;
                        while (board.getTeam(id) != null) id = "qtag_" + ++suffix;
                        own = board.registerNewTeam(id);
                        // Team membership must not expose invisible teammates.
                        own.setCanSeeFriendlyInvisibles(false);
                        own.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                    }
                    own.addEntry(name);
                    team = own;
                }
                if (!team.equals(own)) {
                    original.putIfAbsent(team, team.getOption(Team.Option.NAME_TAG_VISIBILITY));
                }
                if (team.getOption(Team.Option.NAME_TAG_VISIBILITY) != Team.OptionStatus.NEVER) {
                    team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                }
            }
        }

        void restore(Scoreboard board) {
            original.forEach((team, value) -> {
                if (team.equals(board.getTeam(team.getName()))
                        && team.getOption(Team.Option.NAME_TAG_VISIBILITY) == Team.OptionStatus.NEVER) {
                    team.setOption(Team.Option.NAME_TAG_VISIBILITY, value);
                }
            });
            original.clear();
            if (own != null && own.equals(board.getTeam(own.getName()))) own.unregister();
            own = null;
        }
    }
}
