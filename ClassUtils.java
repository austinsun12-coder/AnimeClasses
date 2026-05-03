package com.animeclasses.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ClassUtils {

    private static final String MOD_KEY_PREFIX = "animeclasses_";

    public static void addAttribute(Player player, Attribute attribute,
                                    String key, double value,
                                    AttributeModifier.Operation operation) {
        addAttributeToEntity(player, attribute, key, value, operation);
    }

    public static void addAttribute(org.bukkit.entity.LivingEntity entity, Attribute attribute,
                                    String key, double value,
                                    AttributeModifier.Operation operation) {
        addAttributeToEntity(entity, attribute, key, value, operation);
    }

    private static void addAttributeToEntity(org.bukkit.entity.LivingEntity entity, Attribute attribute,
                                              String key, double value,
                                              AttributeModifier.Operation operation) {
        AttributeInstance inst = entity.getAttribute(attribute);
        if (inst == null) return;
        removeAttributeKey(inst, key);
        org.bukkit.NamespacedKey nsKey = new org.bukkit.NamespacedKey(
                com.animeclasses.AnimeClassesPlugin.getInstance(), key);
        inst.addModifier(new AttributeModifier(nsKey, value, operation));
    }

    public static void resetAttributes(Player player) {
        Attribute[] toReset = {
            Attribute.MOVEMENT_SPEED,
            Attribute.MAX_HEALTH,
            Attribute.SCALE,
            Attribute.BLOCK_INTERACTION_RANGE,
            Attribute.ENTITY_INTERACTION_RANGE,
            Attribute.ATTACK_REACH,
            Attribute.SAFE_FALL_DISTANCE,
            Attribute.ATTACK_DAMAGE
        };
        for (Attribute attr : toReset) {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst == null) continue;
            List<AttributeModifier> toRemove = new ArrayList<>();
            for (AttributeModifier mod : inst.getModifiers()) {
                if (mod.getKey().getNamespace().equals("animeclasses")) {
                    toRemove.add(mod);
                }
            }
            toRemove.forEach(inst::removeModifier);
        }
        // Restore default max health
        setMaxHealth(player, 20.0);
    }

    public static void setMaxHealth(Player player, double health) {
        AttributeInstance inst = player.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) return;
        removeAttributeKey(inst, "max_health_cap");
        double base = inst.getBaseValue(); // 20.0
        double diff = health - base;
        if (diff != 0) {
            org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(
                    com.animeclasses.AnimeClassesPlugin.getInstance(), "max_health_cap");
            inst.addModifier(new AttributeModifier(key, diff, AttributeModifier.Operation.ADD_NUMBER));
        }
        if (player.getHealth() > health) player.setHealth(health);
    }

    private static void removeAttributeKey(AttributeInstance inst, String key) {
        inst.getModifiers().stream()
                .filter(m -> m.getKey().getKey().equals(key))
                .forEach(inst::removeModifier);
    }

    /** Returns true if the item material is a sword or axe. */
    public static boolean isSwordOrAxe(org.bukkit.Material mat) {
        String name = mat.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE");
    }

    /** Returns true if material is Diamond or Netherite gear. */
    public static boolean isDiamondOrNetherite(org.bukkit.Material mat) {
        String name = mat.name();
        return name.startsWith("DIAMOND_") || name.startsWith("NETHERITE_");
    }

    /** Returns true if material is a helmet. */
    public static boolean isHelmet(org.bukkit.Material mat) {
        String name = mat.name();
        return name.endsWith("_HELMET") || mat == org.bukkit.Material.CARVED_PUMPKIN
                || mat == org.bukkit.Material.PLAYER_HEAD || mat == org.bukkit.Material.SKELETON_SKULL;
    }

    /** Returns true if the material is a "sweet" food item. */
    public static boolean isSweet(org.bukkit.Material mat) {
        return mat == org.bukkit.Material.COOKIE
                || mat == org.bukkit.Material.CAKE
                || mat == org.bukkit.Material.SWEET_BERRIES
                || mat == org.bukkit.Material.HONEY_BOTTLE
                || mat == org.bukkit.Material.PUMPKIN_PIE;
    }

    /** Returns true if material is a meat-type food. */
    public static boolean isMeat(org.bukkit.Material mat) {
        return mat == org.bukkit.Material.COOKED_PORKCHOP
                || mat == org.bukkit.Material.PORKCHOP
                || mat == org.bukkit.Material.BEEF
                || mat == org.bukkit.Material.COOKED_BEEF
                || mat == org.bukkit.Material.CHICKEN
                || mat == org.bukkit.Material.COOKED_CHICKEN
                || mat == org.bukkit.Material.MUTTON
                || mat == org.bukkit.Material.COOKED_MUTTON
                || mat == org.bukkit.Material.RABBIT
                || mat == org.bukkit.Material.COOKED_RABBIT;
    }

    /** Returns true if material is a non-meat food Luffy can't eat. */
    public static boolean isVegetable(org.bukkit.Material mat) {
        return mat == org.bukkit.Material.BREAD
                || mat == org.bukkit.Material.CARROT
                || mat == org.bukkit.Material.BAKED_POTATO
                || mat == org.bukkit.Material.POTATO
                || mat == org.bukkit.Material.APPLE
                || mat == org.bukkit.Material.GOLDEN_APPLE
                || mat == org.bukkit.Material.ENCHANTED_GOLDEN_APPLE
                || mat == org.bukkit.Material.MELON_SLICE
                || mat == org.bukkit.Material.PUMPKIN_PIE
                || mat == org.bukkit.Material.BEETROOT
                || mat == org.bukkit.Material.BEETROOT_SOUP
                || mat == org.bukkit.Material.MUSHROOM_STEW;
    }

    /** True if entity is a passive/friendly mob. */
    public static boolean isPassiveMob(org.bukkit.entity.Entity e) {
        return e instanceof org.bukkit.entity.Animals
                || e instanceof org.bukkit.entity.Villager
                || e instanceof org.bukkit.entity.WanderingTrader
                || e instanceof org.bukkit.entity.Squid
                || e instanceof org.bukkit.entity.Bat
                || e instanceof org.bukkit.entity.IronGolem;
    }
}
