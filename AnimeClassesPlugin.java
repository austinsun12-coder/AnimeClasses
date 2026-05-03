package com.animeclasses;

import com.animeclasses.gui.ClassSelectionGUI;
import com.animeclasses.listeners.*;
import com.animeclasses.managers.ClassManager;
import com.animeclasses.managers.CooldownManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AnimeClassesPlugin extends JavaPlugin {

    private static AnimeClassesPlugin instance;
    private ClassManager classManager;
    private CooldownManager cooldownManager;
    private ClassSelectionGUI selectionGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        classManager = new ClassManager(this);
        cooldownManager = new CooldownManager();
        selectionGUI = new ClassSelectionGUI(this);

        // Register all listeners
        getServer().getPluginManager().registerEvents(new AbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new PassiveListener(this), this);
        getServer().getPluginManager().registerEvents(new RestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(selectionGUI, this);

        // Start the global passive tick scheduler (runs every second)
        new PassiveTickRunnable(this).runTaskTimer(this, 20L, 20L);

        // Start cooldown display scheduler (runs every 4 ticks = ~5 times/sec)
        new CooldownDisplayRunnable(this).runTaskTimer(this, 20L, 4L);

        getLogger().info("AnimeClasses enabled! 12 classes loaded.");
    }

    @Override
    public void onDisable() {
        if (classManager != null) classManager.saveAllData();
        getLogger().info("AnimeClasses disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("animeclass")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cOnly players can use this.");
                return true;
            }
            selectionGUI.open(p);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("resetclass")) {
            if (!sender.hasPermission("animeclasses.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            Player target = args.length > 0
                    ? getServer().getPlayer(args[0])
                    : (sender instanceof Player p ? p : null);
            if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
            classManager.removeClass(target);
            sender.sendMessage("§aReset class for " + target.getName());
            return true;
        }
        return false;
    }

    public static AnimeClassesPlugin getInstance() { return instance; }
    public ClassManager getClassManager() { return classManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
    public ClassSelectionGUI getSelectionGUI() { return selectionGUI; }

    // ── Inner runnables ─────────────────────────────────────────────────────

    /** Runs every second; applies persistent passives to all online players. */
    private static class PassiveTickRunnable extends BukkitRunnable {
        private final AnimeClassesPlugin plugin;
        PassiveTickRunnable(AnimeClassesPlugin plugin) { this.plugin = plugin; }
        @Override public void run() {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.getClassManager().tickPassives(p);
            }
        }
    }

    /** Runs every 4 ticks; updates the cooldown action-bar display. */
    private static class CooldownDisplayRunnable extends BukkitRunnable {
        private final AnimeClassesPlugin plugin;
        CooldownDisplayRunnable(AnimeClassesPlugin plugin) { this.plugin = plugin; }
        @Override public void run() {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.getClassManager().updateCooldownDisplay(p);
            }
        }
    }
}
