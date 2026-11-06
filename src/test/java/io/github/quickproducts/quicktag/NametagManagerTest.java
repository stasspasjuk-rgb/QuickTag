package io.github.quickproducts.quicktag;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NametagManagerTest {
    @Test
    void unassignedPlayersHiddenOnMainAndPersonalBoardsAndCleanedUp() {
        Board main = new Board();
        Board personal = new Board();
        Player alice = player("Alice", personal.board);
        try (NametagManager manager = new NametagManager()) {
            manager.refresh(main.board, List.of(alice));
            assertEquals(Team.OptionStatus.NEVER, main.teamOf("Alice").getOption(Team.Option.NAME_TAG_VISIBILITY));
            assertEquals(Team.OptionStatus.NEVER, personal.teamOf("Alice").getOption(Team.Option.NAME_TAG_VISIBILITY));
            verify(main.teamOf("Alice")).setCanSeeFriendlyInvisibles(false);
            verify(personal.teamOf("Alice")).setCanSeeFriendlyInvisibles(false);
            manager.refresh(main.board, List.of());
            assertNull(main.teamOf("Alice"));
            assertTrue(personal.teams.isEmpty());
        }
        assertTrue(main.teams.isEmpty());
    }

    @Test
    void existingTeamMembershipAndOtherPropertiesPreservedAcrossRefreshAndReload() {
        Board board = new Board();
        Team red = board.addTeam("red", Team.OptionStatus.FOR_OTHER_TEAMS);
        red.addEntry("Alice");
        Player alice = player("Alice", board.board);
        try (NametagManager manager = new NametagManager()) {
            manager.refresh(board.board, List.of(alice));
            manager.refresh(board.board, List.of(alice));
            assertSame(red, board.teamOf("Alice"));
            assertEquals(Team.OptionStatus.NEVER, red.getOption(Team.Option.NAME_TAG_VISIBILITY));
            verify(red, never()).setAllowFriendlyFire(anyBoolean());
            verify(red, never()).setOption(eq(Team.Option.COLLISION_RULE), any());
        }
        assertEquals(Team.OptionStatus.FOR_OTHER_TEAMS, red.getOption(Team.Option.NAME_TAG_VISIBILITY));
        assertSame(red, board.teamOf("Alice"));
        verify(red, never()).unregister();
    }

    @Test
    void newTeamAndChangedPersonalBoardHandledWithoutKeepingOldBoard() {
        Board main = new Board();
        Board first = new Board();
        Board second = new Board();
        Player alice = player("Alice", first.board);
        try (NametagManager manager = new NametagManager()) {
            manager.refresh(main.board, List.of(alice));
            Team ranked = main.addTeam("ranked", Team.OptionStatus.ALWAYS);
            ranked.addEntry("Alice");
            when(alice.getScoreboard()).thenReturn(second.board);
            manager.refresh(main.board, List.of(alice));
            assertTrue(first.teams.isEmpty());
            assertNotNull(second.teamOf("Alice"));
            assertSame(ranked, main.teamOf("Alice"));
            assertEquals(Team.OptionStatus.NEVER, ranked.getOption(Team.Option.NAME_TAG_VISIBILITY));
        }
        assertTrue(second.teams.isEmpty());
        assertEquals(Team.OptionStatus.ALWAYS, main.teams.get("ranked").getOption(Team.Option.NAME_TAG_VISIBILITY));
    }

    @Test
    void reservedNameCollisionDoesNotDeleteAnotherPluginsTeam() {
        Board board = new Board();
        Team existing = board.addTeam("quicktag_hidden", Team.OptionStatus.ALWAYS);
        try (NametagManager manager = new NametagManager()) {
            manager.refresh(board.board, List.of(player("Alice", board.board)));
            assertNotSame(existing, board.teamOf("Alice"));
        }
        assertEquals(Map.of("quicktag_hidden", existing), board.teams);
        verify(existing, never()).unregister();
    }

    private static Player player(String name, Scoreboard board) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getScoreboard()).thenReturn(board);
        return player;
    }

    private static class Board {
        final Scoreboard board = mock(Scoreboard.class);
        final Map<String, Team> teams = new HashMap<>();
        final Map<String, Team> entries = new HashMap<>();

        Board() {
            when(board.getTeam(anyString())).thenAnswer(call -> teams.get(call.getArgument(0)));
            when(board.getEntryTeam(anyString())).thenAnswer(call -> entries.get(call.getArgument(0)));
            when(board.registerNewTeam(anyString())).thenAnswer(call -> addTeam(call.getArgument(0), Team.OptionStatus.ALWAYS));
        }

        Team teamOf(String name) { return entries.get(name); }

        Team addTeam(String name, Team.OptionStatus initial) {
            Team team = mock(Team.class);
            Set<String> members = new HashSet<>();
            AtomicReference<Team.OptionStatus> visibility = new AtomicReference<>(initial);
            when(team.getName()).thenReturn(name);
            when(team.getEntries()).thenAnswer(call -> Set.copyOf(members));
            when(team.getOption(Team.Option.NAME_TAG_VISIBILITY)).thenAnswer(call -> visibility.get());
            doAnswer(call -> { visibility.set(call.getArgument(1)); return null; })
                    .when(team).setOption(eq(Team.Option.NAME_TAG_VISIBILITY), any());
            doAnswer(call -> {
                String entry = call.getArgument(0);
                Team previous = entries.get(entry);
                if (previous != null && previous != team) previous.removeEntry(entry);
                members.add(entry);
                entries.put(entry, team);
                return null;
            }).when(team).addEntry(anyString());
            when(team.removeEntry(anyString())).thenAnswer(call -> {
                String entry = call.getArgument(0);
                entries.remove(entry, team);
                return members.remove(entry);
            });
            doAnswer(call -> {
                teams.remove(name, team);
                members.forEach(entry -> entries.remove(entry, team));
                members.clear();
                return null;
            }).when(team).unregister();
            teams.put(name, team);
            return team;
        }
    }
}
