package com.animeclasses.listeners;

import com.animeclasses.AnimeClass;
import com.animeclasses.AnimeClassesPlugin;
import com.animeclasses.managers.ClassManager;
import com.animeclasses.managers.CooldownManager;
import com.animeclasses.utils.ClassUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class AbilityListener implements Listener {

    private final AnimeClassesPlugin plugin;
    // Track players who have Serious Punch primed
    private final Set<UUID> seriousPunchPrimed = new HashSet<>();
    // Track if Wukong is in air for double jump
    private final Map<UUID, Boolean> wukongJumped = new HashMap<>();

    public AbilityListener(AnimeClassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player player = e.getPlayer();
        if (!plugin.getClassManager().hasClass(player)) return;
        if (!player.isSneaking()) return;

        AnimeClass clazz = plugin.getClassManager().getClass(player);
        Action action = e.getAction();

        boolean isLeftClick = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
        boolean isRightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

        if (isLeftClick) handlePrimary(player, clazz, e);
        else if (isRightClick) handleSecondary(player, clazz, e);
    }

    // ─── Primary (Crouch + Left Click) ──────────────────────────────────────

    private void handlePrimary(Player player, AnimeClass clazz, PlayerInteractEvent e) {
        UUID uuid = player.getUniqueId();
        CooldownManager cd = plugin.getCooldownManager();

        switch (clazz) {
            case GOJO -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "hollow_purple")) {
                    player.sendMessage("§cHollow Purple is on cooldown: " + cd.getFormattedRemaining(uuid, "hollow_purple"));
                    return;
                }
                fireHollowPurple(player);
                cd.setCooldown(uuid, "hollow_purple", 3 * 60 * 1000L);
            }
            case SAITAMA -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "serious_punch")) {
                    player.sendMessage("§cSerious Punch on cooldown: " + cd.getFormattedRemaining(uuid, "serious_punch"));
                    return;
                }
                seriousPunchPrimed.add(uuid);
                player.sendMessage("§e§lSerious Punch PRIMED — next unarmed hit is lethal!");
                player.getWorld().spawnParticle(Particle.FLASH, player.getLocation(), 1);
                cd.setCooldown(uuid, "serious_punch", 60 * 1000L);
            }
            case GOKU -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "ki_blast")) {
                    return;
                }
                fireKiBlast(player);
                cd.setCooldown(uuid, "ki_blast", 3000L); // 3-second fire rate
            }
            case EREN -> {
                e.setCancelled(true);
                if (plugin.getClassManager().isErenTitan(uuid)) {
                    player.sendMessage("§cYou are already in Titan form!");
                    return;
                }
                if (!cd.isReady(uuid, "titan_shift")) {
                    player.sendMessage("§cTitan Shift on cooldown: " + cd.getFormattedRemaining(uuid, "titan_shift"));
                    return;
                }
                if (player.getHealth() < 6.0) {
                    player.sendMessage("§cNot enough health to shift! (need 3+ hearts)");
                    return;
                }
                activateTitanShift(player);
                cd.setCooldown(uuid, "titan_shift", 5 * 60 * 1000L);
            }
            case WUKONG -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "nimbus_cloud")) {
                    return;
                }
                activateNimbusCloud(player, uuid);
                cd.setCooldown(uuid, "nimbus_cloud", 5000L);
            }
            case L -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "detective")) {
                    player.sendMessage("§cDetective Sense on cooldown: " + cd.getFormattedRemaining(uuid, "detective"));
                    return;
                }
                activateDetectiveSense(player);
                cd.setCooldown(uuid, "detective", 30 * 1000L);
            }
            case LIGHT -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "kira")) {
                    player.sendMessage("§cKira's Judgement on cooldown: " + cd.getFormattedRemaining(uuid, "kira"));
                    return;
                }
                activateKiraJudgement(player);
                cd.setCooldown(uuid, "kira", 60 * 1000L);
            }
            case NARUTO -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "shadow_clones")) {
                    player.sendMessage("§cShadow Clones on cooldown: " + cd.getFormattedRemaining(uuid, "shadow_clones"));
                    return;
                }
                spawnShadowClones(player);
                cd.setCooldown(uuid, "shadow_clones", 30 * 1000L);
            }
            case ASH -> {
                e.setCancelled(true);
                if (!cd.isReady(uuid, "wolf_recall")) {
                    player.sendMessage("§cWolf Pack Recall on cooldown: " + cd.getFormattedRemaining(uuid, "wolf_recall"));
                    return;
                }
                wolfPackRecall(player);
                cd.setCooldown(uuid, "wolf_recall", 3 * 60 * 1000L);
            }
            default -> {} // Other classes have no primary active
        }
    }

    // ─── Secondary (Crouch + Right Click) ───────────────────────────────────

    private void handleSecondary(Player player, AnimeClass clazz, PlayerInteractEvent e) {
        // Currently no classes have distinct secondary active abilities in the design doc.
        // Reserved for future use.
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Ability Implementations
    // ══════════════════════════════════════════════════════════════════════════

    // ─── Gojo: Hollow Purple ─────────────────────────────────────────────────

    private void fireHollowPurple(Player player) {
        player.sendMessage("§5§lHollow Purple!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 2f, 0.5f);

        // Drain hunger
        player.setFoodLevel(0);

        Location origin = player.getEyeLocation();
        Vector dir = origin.getDirection().normalize();

        // Destroy blocks and damage entities in a line up to 40 blocks
        new BukkitRunnable() {
            int step = 0;
            @Override public void run() {
                if (step > 40) { cancel(); return; }
                Location loc = origin.clone().add(dir.clone().multiply(step));
                loc.getWorld().spawnParticle(Particle.WITCH, loc, 10, 0.3, 0.3, 0.3, 0);

                // Damage entities at this location
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (entity.equals(player)) continue;
                    if (entity instanceof LivingEntity living) {
                        living.damage(18.0, player); // ~9 hearts
                    }
                }

                // Break blocks
                Block block = loc.getBlock();
                if (block.getType() != Material.AIR && block.getType() != Material.BEDROCK) {
                    block.breakNaturally();
                }
                step++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ─── Goku: Ki Blast ──────────────────────────────────────────────────────

    private void fireKiBlast(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1.5f);
        Fireball fb = player.getWorld().spawn(player.getEyeLocation().add(player.getLocation().getDirection()),
                Fireball.class);
        fb.setDirection(player.getLocation().getDirection().normalize().multiply(2));
        fb.setShooter(player);
        fb.setIsIncendiary(false);
        fb.setYield(3.0f);
    }

    // ─── Eren: Titan Shift ───────────────────────────────────────────────────

    private void activateTitanShift(Player player) {
        UUID uuid = player.getUniqueId();
        ClassManager cm = plugin.getClassManager();

        // Cost health
        player.setHealth(Math.max(0.5, player.getHealth() - 6.0)); // -3 hearts

        // Apply Titan scale (5x)
        ClassUtils.addAttribute(player, org.bukkit.attribute.Attribute.SCALE,
                "eren_titan_scale", 4.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);

        // Add health, damage, reach
        ClassUtils.addAttribute(player, org.bukkit.attribute.Attribute.MAX_HEALTH,
                "eren_titan_hp", 30.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
        ClassUtils.addAttribute(player, org.bukkit.attribute.Attribute.ATTACK_DAMAGE,
                "eren_titan_dmg", 10.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
        ClassUtils.addAttribute(player, org.bukkit.attribute.Attribute.ATTACK_REACH,
                "eren_titan_reach", 4.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 30.0));

        cm.setErenTitan(uuid, true);

        player.sendMessage("§c§lCOLOSSAL SHIFT!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2f, 0.5f);

        // Auto-revert after 30 seconds
        new BukkitRunnable() {
            @Override public void run() {
                if (plugin.getServer().getPlayer(uuid) != null && cm.isErenTitan(uuid)) {
                    revertErenTitan(player.getServer().getPlayer(uuid));
                }
            }
        }.runTaskLater(plugin, 30 * 20L);
    }

    public void revertErenTitan(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        ClassUtils.resetAttributes(player);
        // Re-apply base Eren attributes
        ClassUtils.addAttribute(player, org.bukkit.attribute.Attribute.MOVEMENT_SPEED,
                "eren_speed", 0.03, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
        ClassUtils.addAttribute(player, org.bukkit.attribute.Attribute.SAFE_FALL_DISTANCE,
                "eren_fall", 5.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);

        plugin.getClassManager().setErenTitan(uuid, false);

        // Post-titan exhaustion
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15 * 20, 1, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 15 * 20, 1, false, true));
        player.setFoodLevel(Math.max(0, player.getFoodLevel() - 10));

        player.sendMessage("§7You reverted from Titan form. Exhaustion hits...");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.7f);
    }

    // ─── Sun Wukong: Nimbus Cloud ────────────────────────────────────────────

    private void activateNimbusCloud(Player player, UUID uuid) {
        if (!player.isOnGround()) {
            // Air dash
            Vector dir = player.getLocation().getDirection().normalize().multiply(2.0);
            dir.setY(0.3);
            player.setVelocity(dir);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 1.5f);
        } else {
            // Double jump boost
            player.setVelocity(new Vector(player.getVelocity().getX(), 1.0, player.getVelocity().getZ()));
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.3, 0, 0.3, 0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1f, 2f);
        }
    }

    // ─── L: Detective Sense ──────────────────────────────────────────────────

    private void activateDetectiveSense(Player player) {
        player.sendMessage("§f§lDetective sense activated! Highlighting all within 30 blocks...");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);

        for (Entity entity : player.getNearbyEntities(30, 30, 30)) {
            if (entity instanceof LivingEntity le) {
                le.setGlowing(true);
                // Remove glow after 10s
                new BukkitRunnable() {
                    @Override public void run() { le.setGlowing(false); }
                }.runTaskLater(plugin, 10 * 20L);
            }
        }
    }

    // ─── Light: Kira's Judgement ─────────────────────────────────────────────

    private void activateKiraJudgement(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(), player.getEyeLocation().getDirection(),
                20.0, 1.0,
                e -> !e.equals(player) && e instanceof LivingEntity);

        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage("§cNo target in sight.");
            return;
        }

        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 10 * 20, 2, false, true));
        player.sendMessage("§4§lKira's Judgement cast on §c" + (target instanceof Player tp ? tp.getName() : target.getType().name()));
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 0.5f);
        target.getWorld().spawnParticle(Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
    }

    // ─── Naruto: Shadow Clones ───────────────────────────────────────────────

    private void spawnShadowClones(Player player) {
        UUID uuid = player.getUniqueId();
        ClassManager cm = plugin.getClassManager();

        // Drain XP
        if (player.getTotalExperience() <= 0) {
            // Drain health instead
            player.damage(2.0);
            player.sendMessage("§cNo chakra! Shadow Clones drain your health instead!");
        } else {
            player.setLevel(Math.max(0, player.getLevel() - 1));
        }

        for (int i = 0; i < 3; i++) {
            Zombie clone = player.getWorld().spawn(
                    player.getLocation().add(
                            (Math.random() - 0.5) * 3, 0, (Math.random() - 0.5) * 3),
                    Zombie.class);
            clone.setCustomName("§eNaruto Clone");
            clone.setCustomNameVisible(true);
            clone.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15 * 20, 2, false, false));
            clone.setCanPickupItems(false);

            // Dye armor orange-ish using leather armor
            org.bukkit.inventory.EntityEquipment eq = clone.getEquipment();
            org.bukkit.inventory.ItemStack chestplate = new org.bukkit.inventory.ItemStack(Material.LEATHER_CHESTPLATE);
            org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) chestplate.getItemMeta();
            meta.setColor(org.bukkit.Color.ORANGE);
            chestplate.setItemMeta(meta);
            eq.setChestplate(chestplate);
            eq.setChestplateDropChance(0f);

            cm.addNarutoClone(uuid, clone.getUniqueId());

            // Target nearest entity
            if (player.getLastDamageCause() != null) { /* targeting handled passively */ }

            // Remove after 15 seconds
            new BukkitRunnable() {
                @Override public void run() {
                    if (clone.isValid()) {
                        clone.getWorld().spawnParticle(Particle.SMOKE, clone.getLocation(), 15, 0.3, 0.5, 0.3, 0.05);
                        clone.remove();
                    }
                }
            }.runTaskLater(plugin, 15 * 20L);
        }
        player.sendMessage("§6§lShadow Clone Jutsu!");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.2f);
    }

    // ─── Ash: Wolf Pack Recall ───────────────────────────────────────────────

    private void wolfPackRecall(Player player) {
        int count = 0;
        for (Entity e : player.getWorld().getEntities()) {
            if (e instanceof Wolf wolf && wolf.isTamed() && player.equals(wolf.getOwner())) {
                wolf.teleport(player.getLocation().add(
                        (Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4));
                count++;
            }
        }
        // Also check other worlds
        for (org.bukkit.World world : plugin.getServer().getWorlds()) {
            if (world.equals(player.getWorld())) continue;
            for (Entity e : world.getEntities()) {
                if (e instanceof Wolf wolf && wolf.isTamed() && player.equals(wolf.getOwner())) {
                    wolf.teleport(player.getLocation().add(
                            (Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4));
                    count++;
                }
            }
        }
        player.sendMessage("§9§lWolf Pack Recall! §7(" + count + " wolves recalled)");
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 2f, 1f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0,1,0), 40, 1, 1, 1, 0.5);
    }

    // ─── Public helpers for other listeners ──────────────────────────────────

    public boolean isSeriousPunchPrimed(UUID uuid) { return seriousPunchPrimed.contains(uuid); }
    public void consumeSeriousPunch(UUID uuid) { seriousPunchPrimed.remove(uuid); }
}
