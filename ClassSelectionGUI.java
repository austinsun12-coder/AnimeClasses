package com.animeclasses.gui;

import com.animeclasses.AnimeClass;
import com.animeclasses.AnimeClassesPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ClassSelectionGUI implements Listener {

    private final AnimeClassesPlugin plugin;
    private static final String GUI_TITLE = "§8Select Your Class";
    private static final int ROWS = 2; // 18 slots, 12 classes + borders

    public ClassSelectionGUI(AnimeClassesPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, ROWS * 9, net.kyori.adventure.text.Component.text(GUI_TITLE));

        AnimeClass[] classes = AnimeClass.values();
        // Fill slots 1–12 with class icons
        int[] slots = {1,2,3,4,5,6,7,8,10,11,12,13};
        for (int i = 0; i < classes.length && i < slots.length; i++) {
            inv.setItem(slots[i], buildIcon(classes[i], player));
        }

        // Slot 17 = reset/no class (barrier)
        ItemStack reset = new ItemStack(Material.BARRIER);
        ItemMeta rm = reset.getItemMeta();
        rm.displayName(net.kyori.adventure.text.Component.text("§cRemove Class"));
        rm.lore(List.of(net.kyori.adventure.text.Component.text("§7Resets you to no class.")));
        reset.setItemMeta(rm);
        inv.setItem(17, reset);

        player.openInventory(inv);
    }

    private ItemStack buildIcon(AnimeClass clazz, Player player) {
        ItemStack item = new ItemStack(clazz.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(clazz.getColoredName()));

        AnimeClass current = plugin.getClassManager().getClass(player);
        boolean selected = clazz.equals(current);

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(net.kyori.adventure.text.Component.text("§7" + clazz.getSeries()));
        lore.add(net.kyori.adventure.text.Component.text(""));
        lore.add(net.kyori.adventure.text.Component.text(selected ? "§a▶ Currently selected" : "§eClick to select"));
        meta.lore(lore);

        // Glow effect if selected
        if (selected) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!e.getView().title().equals(net.kyori.adventure.text.Component.text(GUI_TITLE))) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.BARRIER) {
            plugin.getClassManager().removeClass(player);
            player.closeInventory();
            player.sendMessage("§7Your class has been removed.");
            return;
        }

        for (AnimeClass clazz : AnimeClass.values()) {
            if (clazz.getIcon() == clicked.getType()
                    && clicked.getItemMeta() != null
                    && clicked.getItemMeta().hasDisplayName()) {
                // Match by display name color code
                String disp = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                        .legacySection().serialize(clicked.getItemMeta().displayName());
                if (disp.contains(clazz.getDisplayName())) {
                    plugin.getClassManager().setClass(player, clazz);
                    player.closeInventory();
                    return;
                }
            }
        }
    }
}
