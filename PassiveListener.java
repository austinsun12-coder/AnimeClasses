package com.animeclasses.listeners;

import com.animeclasses.AnimeClass;
import com.animeclasses.AnimeClassesPlugin;
import com.animeclasses.managers.ClassManager;
import com.animeclasses.utils.ClassUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public class PassiveListener implements Listener {

    private final AnimeClassesPlugin plugin;
    private final java.util.Random random = new java.util.Random();

    public PassiveListener(AnimeClassesPlugin plugin) {
        this.plugin = plugin;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DAMAGE DEALT BY PLAYER
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!plugin.getClassManager().hasClass(attacker)) return;

        AnimeClass clazz = plugin.getClassManager().getClass(attacker);
        UUID uuid = attacker.getUniqueId();

        switch (clazz) {
            case ITADORI -> handleItadoriAttack(e, attacker, uuid);
            case SAITAMA -> handleSaitamaAttack(e, attacker, uuid);
            case GOKU -> handleGokuAttack(e, attacker, uuid);
            case WUKONG -> handleWukongAttack(e, attacker, uuid);
            case ASH -> handleAshAttack(e, attacker);
            case LEBRON -> handleLebronAttack(e, attacker, uuid);
            default -> {}
        }
    }

    // ─── Itadori: Divergent Fist / Black Flash ──────────────────────────────

    private void handleItadoriAttack(EntityDamageByEntityEvent e, Player attacker, UUID uuid) {
        // Saitama boredom reset
        plugin.getClassManager().resetSaitamaBoredom(uuid);

        // Must be unarmed
        if (attacker.getInventory().getItemInMainHand().getType() != Material.AIR) {
            // Penalise sword/axe use
            if (ClassUtils.isSwordOrAxe(attacker.getInventory().getItemInMainHand().getType())) {
                e.setDamage(e.getDamage() * 0.5);
            }
            return;
        }

        // Base unarmed = iron sword damage (~7)
        e.setDamage(7.0);

        // 25% Black Flash
        if (random.nextDouble() < 0.25) {
            e.setDamage(7.0 * 4); // 4x
            if (e.getEntity() instanceof LivingEntity target) {
                target.setVelocity(attacker.getLocation().getDirection().multiply(3.0).add(new Vector(0, 0.5, 0)));
            }
            attacker.getWorld().spawnParticle(Particle.FLASH, attacker.getLocation(), 3);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1f, 1.5f);
            attacker.sendMessage("§c§lBLACK FLASH!");

            // Hunger drain
            int hunger = attacker.getFoodLevel();
            attacker.setFoodLevel(Math.max(0, hunger - 4)); // 2 drumsticks = 4 points
            if (hunger <= 0) {
                attacker.damage(2.0); // 1 heart recoil
                attacker.sendMessage("§cCursed Energy Burnout! Recoil damage!");
            }
        }
    }

    // ─── Saitama ─────────────────────────────────────────────────────────────

    private void handleSaitamaAttack(EntityDamageByEntityEvent e, Player attacker, UUID uuid) {
        plugin.getClassManager().resetSaitamaBoredom(uuid);

        AbilityListener al = getAbilityListener();
        if (al == null) return;

        if (attacker.getInventory().getItemInMainHand().getType() != Material.AIR) return;

        if (al.isSeriousPunchPrimed(uuid)) {
            al.consumeSeriousPunch(uuid);
            if (e.getEntity() instanceof LivingEntity target) {
                // Non-boss = instakill
                if (!(target instanceof EnderDragon) && !(target instanceof WitherSkeleton)
                        && !(target instanceof Wither) && !(target instanceof ElderGuardian)) {
                    target.setHealth(0);
                    e.setDamage(0);
                } else {
                    // 15 hearts true damage to bosses/players
                    e.setDamage(30.0);
                }
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.8f);
                attacker.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 5, 2, 2, 2, 0);
            }
        }
    }

    // ─── Goku ────────────────────────────────────────────────────────────────

    private void handleGokuAttack(EntityDamageByEntityEvent e, Player attacker, UUID uuid) {
        // Fair Fighter: 0 damage while sneaking
        if (attacker.isSneaking()) {
            e.setDamage(0);
            e.setCancelled(true);
            return;
        }
        plugin.getClassManager().resetSaitamaBoredom(uuid); // not needed but keeps pattern
    }

    // ─── Sun Wukong ──────────────────────────────────────────────────────────

    private void handleWukongAttack(EntityDamageByEntityEvent e, Player attacker, UUID uuid) {
        Material mainHand = attacker.getInventory().getItemInMainHand().getType();
        if (mainHand == Material.STICK) {
            // Diamond sword base damage ~8
            e.setDamage(8.0);
        }

        // Golden Headband: hitting passive mob damages self
        if (ClassUtils.isPassiveMob(e.getEntity())) {
            attacker.damage(4.0); // 2 hearts magic damage
            attacker.sendMessage("§6The Golden Headband tightens! §c-2 hearts");
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
        }
    }

    // ─── Ash ─────────────────────────────────────────────────────────────────

    private void handleAshAttack(EntityDamageByEntityEvent e, Player attacker) {
        // 60% damage reduction on direct attacks
        e.setDamage(e.getDamage() * 0.4);
    }

    // ─── LeBron: Posterize slam dunk ─────────────────────────────────────────

    private void handleLebronAttack(EntityDamageByEntityEvent e, Player attacker, UUID uuid) {
        // Critical hit = falling from 3+ blocks
        if (attacker.getFallDistance() >= 3.0 && e.getEntity() instanceof LivingEntity target) {
            // Shockwave knockback
            for (Entity nearby : attacker.getNearbyEntities(4, 4, 4)) {
                if (nearby instanceof LivingEntity le && !nearby.equals(attacker)) {
                    Vector away = nearby.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
                    le.setVelocity(away.multiply(2.5).add(new Vector(0, 0.8, 0)));
                }
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 3, false, true));
            attacker.sendMessage("§6§lPOSTERIZE!");
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);
            attacker.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation(), 3, 1, 1, 1, 0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DAMAGE RECEIVED BY PLAYER
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!plugin.getClassManager().hasClass(player)) return;

        AnimeClass clazz = plugin.getClassManager().getClass(player);
        UUID uuid = player.getUniqueId();

        switch (clazz) {
            case SAITAMA -> handleSaitamaDamage(e);
            case GOKU -> handleGokuDamage(e, player, uuid);
            case LUFFY -> handleLuffyDamage(e, player);
            case EREN -> handleErenDamage(e, player);
            case NARUTO -> handleNarutoDamage(e, player);
            case LIGHT -> handleLightDamage(e, player);
            case ASH -> handleAshDamage(e, player);
            default -> {}
        }
    }

    // ─── Saitama: immune to fall + fire ──────────────────────────────────────

    private void handleSaitamaDamage(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL
                || cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) {
            e.setCancelled(true);
        }
    }

    // ─── Goku: Saiyan Blood ──────────────────────────────────────────────────

    private void handleGokuDamage(EntityDamageEvent e, Player player, UUID uuid) {
        double health = player.getHealth();
        int amp;
        if (health <= 4.0) amp = 2;       // ≤2 hearts → Strength III
        else if (health <= 10.0) amp = 1;  // ≤5 hearts → Strength II
        else amp = 0;                       // else → Strength I

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 5 * 20, amp, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 5 * 20, 1, false, false));

        // Tail weakness: damage from behind → Slowness II
        if (e instanceof EntityDamageByEntityEvent dbe) {
            Entity damager = dbe.getDamager();
            // Behind = damager is roughly opposite to player's facing direction
            Vector facing = player.getLocation().getDirection();
            Vector toDamager = damager.getLocation().toVector().subtract(player.getLocation().toVector());
            if (facing.dot(toDamager) < -0.5) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 1, false, true));
            }
        }
    }

    // ─── Luffy: Rubber body ───────────────────────────────────────────────────

    private void handleLuffyDamage(EntityDamageEvent e, Player player) {
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
            return;
        }
        // Reduced knockback is handled by attribute, no cancel needed
    }

    // ─── Eren ────────────────────────────────────────────────────────────────

    private void handleErenDamage(EntityDamageEvent e, Player player) {
        // Reduced fall in human form
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL
                && !plugin.getClassManager().isErenTitan(player.getUniqueId())) {
            e.setDamage(e.getDamage() * 0.5);
        }
    }

    // ─── Naruto: Reckless - double magic/explosion damage ────────────────────

    private void handleNarutoDamage(EntityDamageEvent e, Player player) {
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause == EntityDamageEvent.DamageCause.MAGIC
                || cause == EntityDamageEvent.DamageCause.SPELL
                || cause == EntityDamageEvent.DamageCause.EXPLOSION
                || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            e.setDamage(e.getDamage() * 2.0);
        }
    }

    // ─── Light: God Complex - +50% from players and named mobs ───────────────

    private void handleLightDamage(EntityDamageEvent e, Player player) {
        if (!(e instanceof EntityDamageByEntityEvent dbe)) return;
        Entity damager = dbe.getDamager();
        if (damager instanceof Player
                || (damager instanceof LivingEntity le && le.getCustomName() != null)) {
            e.setDamage(e.getDamage() * 1.5);
        }
    }

    // ─── Ash: no modifier on received damage ─────────────────────────────────

    private void handleAshDamage(EntityDamageEvent e, Player player) {
        // Max health cap enforced in tickPassives
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FOOD EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!plugin.getClassManager().hasClass(player)) return;
        AnimeClass clazz = plugin.getClassManager().getClass(player);

        if (clazz == AnimeClass.GOKU) {
            // Extra exhaustion: each hunger restore is reduced by 30%
            if (e.getFoodLevel() > player.getFoodLevel()) return; // gaining food is fine
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent e) {
        Player player = e.getPlayer();
        if (!plugin.getClassManager().hasClass(player)) return;
        AnimeClass clazz = plugin.getClassManager().getClass(player);
        Material mat = e.getItem().getType();

        if (clazz == AnimeClass.L) {
            if (ClassUtils.isSweet(mat)) {
                new BukkitRunnable() {
                    @Override public void run() {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 20, 1, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 20, 0, false, true));
                        player.sendMessage("§f§lSugar Rush!");
                    }
                }.runTaskLater(plugin, 1L);
            }
        }

        if (clazz == AnimeClass.LUFFY) {
            if (ClassUtils.isVegetable(mat)) {
                new BukkitRunnable() {
                    @Override public void run() {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 5 * 20, 1, false, true));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 8 * 20, 0, false, true));
                        player.sendMessage("§cDevil Fruit Curse! You can't eat that!");
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MOVEMENT EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        if (!plugin.getClassManager().hasClass(player)) return;
        AnimeClass clazz = plugin.getClassManager().getClass(player);
        UUID uuid = player.getUniqueId();

        if (clazz == AnimeClass.LEBRON) {
            if (player.isSprinting()) {
                plugin.getClassManager().incrementSprintTick(uuid);
                int ticks = plugin.getClassManager().getSprintTicks(uuid);
                if (ticks >= 300) { // 15 seconds at 20 ticks/s (called every tick via move)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 4 * 20, 2, false, true));
                    plugin.getClassManager().resetSprintTick(uuid);
                    player.sendMessage("§c§lFourth Quarter Cramps!");
                }
            } else {
                plugin.getClassManager().resetSprintTick(uuid);
            }
        }

        // Naruto: water sprint — prevent sinking only when moving fast
        if (clazz == AnimeClass.NARUTO) {
            if (player.isSprinting() && player.isInWater()) {
                Vector vel = player.getVelocity();
                if (vel.getY() < 0) {
                    player.setVelocity(new Vector(vel.getX(), 0.1, vel.getZ()));
                }
            }
        }

        // Luffy: cannot swim upward
        if (clazz == AnimeClass.LUFFY) {
            if (player.isInWater()) {
                Vector vel = player.getVelocity();
                if (vel.getY() > 0 && player.isSwimming()) {
                    player.setVelocity(new Vector(vel.getX(), -0.1, vel.getZ()));
                }
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PROJECTILE EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getHitEntity() instanceof Player player)) return;
        if (!plugin.getClassManager().hasClass(player)) return;
        if (plugin.getClassManager().getClass(player) != AnimeClass.GOJO) return;

        // Infinity: cancel the hit and bounce the projectile back
        e.setCancelled(true);
        Projectile proj = e.getEntity();
        Vector bounce = proj.getVelocity().negate().multiply(0.8);
        proj.setVelocity(bounce);
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // JOIN / QUIT
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        // Re-apply attributes on join if they have a class
        new BukkitRunnable() {
            @Override public void run() {
                if (plugin.getClassManager().hasClass(player)) {
                    // The class was loaded from disk; re-apply attributes
                    plugin.getClassManager().setClass(player,
                            plugin.getClassManager().getClass(player));
                }
            }
        }.runTaskLater(plugin, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        plugin.getClassManager().saveAllData();
    }

    // ─── Ash: Tamed wolves get buffed stats ──────────────────────────────────

    @EventHandler
    public void onEntityTame(EntityTameEvent e) {
        if (!(e.getOwner() instanceof Player player)) return;
        if (!plugin.getClassManager().hasClass(player)) return;
        if (plugin.getClassManager().getClass(player) != AnimeClass.ASH) return;
        if (!(e.getEntity() instanceof Wolf wolf)) return;

        new BukkitRunnable() {
            @Override public void run() {
                ClassUtils.addAttribute(wolf, org.bukkit.attribute.Attribute.MOVEMENT_SPEED,
                        "ash_wolf_speed", 0.06, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
                ClassUtils.addAttribute(wolf, org.bukkit.attribute.Attribute.ATTACK_DAMAGE,
                        "ash_wolf_dmg", 4.0, org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER);
                wolf.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                wolf.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, false, false));
                player.sendMessage("§9§lYour wolf is now powered up!");
            }
        }.runTaskLater(plugin, 5L);
    }

    // Helper to get ability listener from registered listeners
    private AbilityListener getAbilityListener() {
        for (var entry : org.bukkit.event.HandlerList.getRegisteredListeners(plugin)) {
            if (entry.getListener() instanceof AbilityListener al) return al;
        }
        return null;
    }
}
