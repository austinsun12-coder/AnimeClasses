package com.animeclasses.listeners;

import com.animeclasses.AnimeClass;
import com.animeclasses.AnimeClassesPlugin;
import com.animeclasses.utils.ClassUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class RestrictionListener implements Listener {

    private final AnimeClassesPlugin plugin;

    public RestrictionListener(AnimeClassesPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Prevent equipping restricted gear via inventory drag/click ───────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getClassManager().hasClass(player)) return;
        AnimeClass clazz = plugin.getClassManager().getClass(player);

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) {
            item = e.getCursor();
        }
        if (item == null || item.getType() == Material.AIR) return;

        Material mat = item.getType();

        switch (clazz) {
            case GOJO -> {
                // Cannot equip shields
                if (mat == Material.SHIELD) {
                    e.setCancelled(true);
                    player.sendMessage("§cArrogant Fighter: You refuse to use a shield.");
                }
            }
            case SAITAMA -> {
                // Cannot equip Diamond/Netherite armor/weapons or helmets
                if (ClassUtils.isDiamondOrNetherite(mat)) {
                    e.setCancelled(true);
                    player.sendMessage("§cDiscount Shopper: No Diamond or Netherite gear!");
                }
                if (ClassUtils.isHelmet(mat)) {
                    e.setCancelled(true);
                    player.sendMessage("§cBald Cape: No helmets!");
                }
            }
            case LIGHT -> {
                if (ClassUtils.isHelmet(mat)) {
                    e.setCancelled(true);
                    player.sendMessage("§cGod Complex: You cannot wear a helmet.");
                }
            }
        }
    }

    // ── Prevent armor equip via right-click shortcuts ─────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractArmorEquip(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = e.getPlayer();
        if (!plugin.getClassManager().hasClass(player)) return;
        AnimeClass clazz = plugin.getClassManager().getClass(player);

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;
        Material mat = item.getType();

        switch (clazz) {
            case SAITAMA -> {
                if (ClassUtils.isDiamondOrNetherite(mat) || ClassUtils.isHelmet(mat)) {
                    e.setCancelled(true);
                    player.sendMessage("§cYou can't use that!");
                }
            }
            case GOJO -> {
                if (mat == Material.SHIELD) {
                    e.setCancelled(true);
                    player.sendMessage("§cArrogant Fighter: No shields!");
                }
            }
            case LIGHT -> {
                if (ClassUtils.isHelmet(mat)) {
                    e.setCancelled(true);
                    player.sendMessage("§cGod Complex: No helmets!");
                }
            }
        }
    }

    // ── Prevent bed sleeping for L ────────────────────────────────────────────

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent e) {
        Player player = e.getPlayer();
        if (!plugin.getClassManager().hasClass(player)) return;
        if (plugin.getClassManager().getClass(player) == AnimeClass.L) {
            e.setCancelled(true);
            player.sendMessage("§fInsomniac: You cannot sleep. The phantoms will come...");
        }
    }

    // ── Tick-based gear strip (runs in passive tick every second) ─────────────
    // Called from ClassManager.tickPassives — we expose as a static helper

    public static void enforceGearRestrictions(Player player, AnimeClass clazz) {
        switch (clazz) {
            case SAITAMA -> {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null && helmet.getType() != Material.AIR) {
                    player.getInventory().setHelmet(null);
                    player.getWorld().dropItemNaturally(player.getLocation(), helmet);
                    player.sendMessage("§cBald Cape: Helmet removed!");
                }
                checkAndStripDiamondNetherite(player);
            }
            case LIGHT -> {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null && helmet.getType() != Material.AIR) {
                    player.getInventory().setHelmet(null);
                    player.getWorld().dropItemNaturally(player.getLocation(), helmet);
                    player.sendMessage("§cGod Complex: Helmet removed!");
                }
            }
            case GOJO -> {
                // Shield in offhand
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand.getType() == Material.SHIELD) {
                    player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                    player.getWorld().dropItemNaturally(player.getLocation(), offhand);
                    player.sendMessage("§cArrogant Fighter: Shield removed!");
                }
            }
        }
    }

    private static void checkAndStripDiamondNetherite(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] != null && ClassUtils.isDiamondOrNetherite(armor[i].getType())) {
                ItemStack dropped = armor[i];
                armor[i] = new ItemStack(Material.AIR);
                player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                player.sendMessage("§cDiscount Shopper: Gear stripped!");
            }
        }
        player.getInventory().setArmorContents(armor);
    }
}
