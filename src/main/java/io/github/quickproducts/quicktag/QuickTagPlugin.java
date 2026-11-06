package io.github.quickproducts.quicktag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuickTagPlugin extends JavaPlugin implements Listener {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Integer> lastReveal = new HashMap<>();
    private NametagManager nametags;
    private BukkitTask refreshTask;
    private String actionbar;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        nametags = new NametagManager();
        getServer().getPluginManager().registerEvents(this, this);
        refreshNametags();
        refreshTask = Bukkit.getScheduler().runTaskTimer(this, this::refreshNametags, 1L, 10L);
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) refreshTask.cancel();
        if (nametags != null) nametags.close();
        lastReveal.clear();
    }

    private void loadSettings() {
        actionbar = getConfig().getString("actionbar", "<yellow>Ник: <white><nickname>");
    }

    private void refreshNametags() {
        nametags.refresh(getServer().getScoreboardManager().getMainScoreboard(), getServer().getOnlinePlayers());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        refreshNametags();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastReveal.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        reveal(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        reveal(event);
    }

    private void reveal(PlayerInteractEntityEvent event) {
        Player viewer = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || !viewer.isSneaking()
                || !(event.getRightClicked() instanceof Player target)
                || !viewer.canSee(target) || !viewer.hasLineOfSight(target)) return;
        int tick = Bukkit.getCurrentTick();
        if (Integer.valueOf(tick).equals(lastReveal.put(viewer.getUniqueId(), tick))) return;
        viewer.sendActionBar(miniMessage.deserialize(actionbar,
                Placeholder.component("nickname", Component.text(target.getName()))));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            message(sender, "usage", "<yellow>/quicktag reload");
            return true;
        }
        if (!sender.hasPermission("quicktag.reload")) {
            message(sender, "no-permission", "<red>Нет права quicktag.reload.");
            return true;
        }
        reloadConfig();
        loadSettings();
        refreshNametags();
        message(sender, "reloaded", "<green>QuickTag: конфигурация перезагружена.");
        return true;
    }

    private void message(CommandSender sender, String key, String fallback) {
        sender.sendMessage(miniMessage.deserialize(getConfig().getString("messages." + key, fallback)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return sender.hasPermission("quicktag.reload") && args.length == 1
                && "reload".startsWith(args[0].toLowerCase(java.util.Locale.ROOT)) ? List.of("reload") : List.of();
    }
}
