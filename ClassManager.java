package com.animeclasses.managers;

import com.animeclasses.AnimeClass;
import com.animeclasses.AnimeClassesPlugin;
import com.animeclasses.utils.ClassUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClassManager {

    private final AnimeClassesPlugin plugin;
    private final Map<UUID, AnimeClass> playerClasses = new HashMap<>();
    // Tracks data per player (timers, flags, etc.)
    private final Map<UUID, Map<String, Object>> playerData = new HashMap<>();

    // Tracks how long each player has been sprinting continuously (ticks)
    private final Map<UUID, Integer> sprintTicks = new HashMap<>();
    // Tracks boredom timer for Saitama (ticks since last combat hit)
    private final Map<UUID, Integer> saitamaBoredom = new HashMap<>();
    // Tracks whether Saitama boredom debuff is active
    private final Set<UUID> saitamaDebuffed = new HashSet<>();
    // Tracks Gojo blindfold check
    // Tracks Naruto clone lists
    private final Map<UUID, List<UUID>> narutoClones = new HashMap<>();
    // Tracks Eren titan state
    private final Set<UUID> erenTitan = new HashSet<>();

    private File dataFile;
    private YamlConfiguration dataConfig;

    public ClassManager(AnimeClassesPlugin plugin) {
        this.plugin = plugin;
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    // ─── Assignment ─────────────────────────────────────────────────────────

    public void setClass(Player player, AnimeClass clazz) {
        UUID uuid = player.getUniqueId();
        // Remove old class modifiers first
        removeClass(player);
        playerClasses.put(uuid, clazz);
        playerData.put(uuid, new HashMap<>());
        applyClassAttributes(player, clazz);
        player.sendMessage("§6You are now playing as §e" + clazz.getColoredName() + "§6!");
        player.sendMessage("§7Use §f/animeclass §7to change class.");
        saveData();
    }

    public void removeClass(Player player) {
        UUID uuid = player.getUniqueId();
        AnimeClass old = playerClasses.remove(uuid);
        playerData.remove(uuid);
        erenTitan.remove(uuid);
        narutoClones.remove(uuid);
        saitamaBoredom.remove(uuid);
        saitamaDebuffed.remove(uuid);
        sprintTicks.remove(uuid);
        // Remove all potion effects applied by us
        for (PotionEffectType t : PotionEffectType.values()) {
            player.removePotionEffect(t);
        }
        // Reset attributes
        ClassUtils.resetAttributes(player);
        saveData();
    }

    public AnimeClass getClass(Player player) {
        return playerClasses.get(player.getUniqueId());
    }

    public boolean hasClass(Player player) {
        return playerClasses.containsKey(player.getUniqueId());
    }

    // ─── Attribute setup ────────────────────────────────────────────────────

    private void applyClassAttributes(Player player, AnimeClass clazz) {
        ClassUtils.resetAttributes(player);
        switch (clazz) {
            case EREN -> {
                // Normal form speed boost
                ClassUtils.addAttribute(player, Attribute.MOVEMENT_SPEED,
                        "eren_speed", 0.03, AttributeModifier.Operation.ADD_NUMBER);
                // Reduced fall damage (handled in listener but we set safe fall via attribute)
                ClassUtils.addAttribute(player, Attribute.SAFE_FALL_DISTANCE,
                        "eren_fall", 5.0, AttributeModifier.Operation.ADD_NUMBER);
            }
            case LEBRON -> {
                // 1.2x scale
                ClassUtils.addAttribute(player, Attribute.SCALE,
                        "lebron_scale", 0.2, AttributeModifier.Operation.ADD_NUMBER);
                // Jump boost III equivalent via block_interaction_range not needed; we handle jump in listener
            }
            case LUFFY -> {
                // Extended reach
                ClassUtils.addAttribute(player, Attribute.BLOCK_INTERACTION_RANGE,
                        "luffy_reach", 3.0, AttributeModifier.Operation.ADD_NUMBER);
                ClassUtils.addAttribute(player, Attribute.ENTITY_INTERACTION_RANGE,
                        "luffy_reach_entity", 3.0, AttributeModifier.Operation.ADD_NUMBER);
                ClassUtils.addAttribute(player, Attribute.ATTACK_REACH,
                        "luffy_attack_reach", 3.0, AttributeModifier.Operation.ADD_NUMBER);
            }
            default -> {} // Most passives handled via potion effects in tickPassives
        }
    }

    // ─── Passive Tick (called every second) ─────────────────────────────────

    public void tickPassives(Player player) {
        AnimeClass clazz = getClass(player);
        if (clazz == null) return;
        UUID uuid = player.getUniqueId();

        // Always enforce gear restrictions
        com.animeclasses.listeners.RestrictionListener.enforceGearRestrictions(player, clazz);

        switch (clazz) {
            case GOJO -> tickGojo(player, uuid);
            case ITADORI -> tickItadori(player, uuid);
            case SAITAMA -> tickSaitama(player, uuid);
            case GOKU -> tickGoku(player, uuid);
            case LUFFY -> tickLuffy(player, uuid);
            case EREN -> tickEren(player, uuid);
            case WUKONG -> {} // passive handled in listener
            case L -> tickL(player, uuid);
            case LIGHT -> tickLight(player, uuid);
            case NARUTO -> tickNaruto(player, uuid);
            case ASH -> {} // passives handled in listener
            case LEBRON -> tickLebron(player, uuid);
        }
    }

    private void tickGojo(Player p, UUID uuid) {
        // Permanent Hunger I
        applyPermanent(p, PotionEffectType.HUNGER, 0);

        // Blindfold check: if no helmet in daylight → nausea + blindness
        if (p.getInventory().getHelmet() == null
                && p.getWorld().isDayTime()
                && !p.getWorld().hasStorm()) {
            applyPermanent(p, PotionEffectType.NAUSEA, 0);
            applyPermanent(p, PotionEffectType.BLINDNESS, 0);
        } else {
            p.removePotionEffect(PotionEffectType.NAUSEA);
            p.removePotionEffect(PotionEffectType.BLINDNESS);
        }
    }

    private void tickItadori(Player p, UUID uuid) {
        applyPermanent(p, PotionEffectType.RESISTANCE, 0);
        applyPermanent(p, PotionEffectType.JUMP_BOOST, 0);
        applyPermanent(p, PotionEffectType.SPEED, 1);

        // Sukuna's toll: below 3 hearts → wither
        if (p.getHealth() < 6.0 && !p.hasPotionEffect(PotionEffectType.WITHER)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0, true, false));
        }
    }

    private void tickSaitama(Player p, UUID uuid) {
        // Increment boredom counter
        int ticks = saitamaBoredom.getOrDefault(uuid, 0) + 1;
        saitamaBoredom.put(uuid, ticks);

        if (ticks >= 180) { // 3 minutes (180 seconds)
            if (!saitamaDebuffed.contains(uuid)) {
                saitamaDebuffed.add(uuid);
            }
            applyPermanent(p, PotionEffectType.WEAKNESS, 1);
            applyPermanent(p, PotionEffectType.SLOWNESS, 0);
        }
    }

    /** Called from the combat listener when Saitama hits something. */
    public void resetSaitamaBoredom(UUID uuid) {
        saitamaBoredom.put(uuid, 0);
        saitamaDebuffed.remove(uuid);
        Player p = plugin.getServer().getPlayer(uuid);
        if (p != null) {
            p.removePotionEffect(PotionEffectType.WEAKNESS);
            p.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private void tickGoku(Player p, UUID uuid) {
        // Exhaustion tripled: we forcefully add exhaustion each tick
        p.setExhaustion(p.getExhaustion() + 0.04f * 2); // extra on top of vanilla
    }

    private void tickLuffy(Player p, UUID uuid) {
        // Rain → Slowness I
        if (p.getWorld().hasStorm() && p.getWorld().getHighestBlockAt(p.getLocation()).getY() < p.getLocation().getY()) {
            applyPermanent(p, PotionEffectType.SLOWNESS, 0);
        } else {
            p.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private void tickEren(Player p, UUID uuid) {
        if (!erenTitan.contains(uuid)) {
            // Normal form passives
            applyPermanent(p, PotionEffectType.SPEED, 0);
        }
    }

    private void tickL(Player p, UUID uuid) {
        // Frail body: enforce 6-heart max
        if (p.getMaxHealth() != 12.0) {
            ClassUtils.setMaxHealth(p, 12.0);
        }
    }

    private void tickLight(Player p, UUID uuid) {
        if (p.getMaxHealth() != 16.0) {
            ClassUtils.setMaxHealth(p, 16.0);
        }
    }

    private void tickNaruto(Player p, UUID uuid) {
        applyPermanent(p, PotionEffectType.JUMP_BOOST, 1);
        applyPermanent(p, PotionEffectType.SPEED, 0);
        // Clean up dead clones from tracking list
        List<UUID> clones = narutoClones.get(uuid);
        if (clones != null) {
            clones.removeIf(cid -> plugin.getServer().getEntity(cid) == null);
        }
    }

    private void tickLebron(Player p, UUID uuid) {
        // Sprint tracking is handled every tick in PassiveListener via isSprinting()
        // Here we apply Jump Boost III
        applyPermanent(p, PotionEffectType.JUMP_BOOST, 2);
    }

    // ─── Eren Titan Helpers ─────────────────────────────────────────────────

    public boolean isErenTitan(UUID uuid) { return erenTitan.contains(uuid); }
    public void setErenTitan(UUID uuid, boolean titan) {
        if (titan) erenTitan.add(uuid);
        else erenTitan.remove(uuid);
    }

    // ─── Sprint tracking for LeBron ─────────────────────────────────────────

    public void incrementSprintTick(UUID uuid) {
        sprintTicks.put(uuid, sprintTicks.getOrDefault(uuid, 0) + 1);
    }
    public void resetSprintTick(UUID uuid) { sprintTicks.put(uuid, 0); }
    public int getSprintTicks(UUID uuid) { return sprintTicks.getOrDefault(uuid, 0); }

    // ─── Naruto clone tracking ───────────────────────────────────────────────

    public List<UUID> getNarutoClones(UUID uuid) {
        return narutoClones.computeIfAbsent(uuid, k -> new ArrayList<>());
    }
    public void addNarutoClone(UUID player, UUID clone) {
        getNarutoClones(player).add(clone);
    }

    // ─── Generic data map ───────────────────────────────────────────────────

    public Object getData(UUID uuid, String key) {
        Map<String, Object> data = playerData.get(uuid);
        return data == null ? null : data.get(key);
    }
    public void setData(UUID uuid, String key, Object value) {
        playerData.computeIfAbsent(uuid, k -> new HashMap<>()).put(key, value);
    }

    // ─── Cooldown Display ───────────────────────────────────────────────────

    public void updateCooldownDisplay(Player player) {
        AnimeClass clazz = getClass(player);
        if (clazz == null) return;
        CooldownManager cd = plugin.getCooldownManager();
        UUID uuid = player.getUniqueId();

        String bar = clazz.getColoredName() + " §8| ";

        switch (clazz) {
            case GOJO ->
                bar += "§fHollow Purple: " + cd.getFormattedRemaining(uuid, "hollow_purple");
            case SAITAMA ->
                bar += "§fSerious Punch: " + cd.getFormattedRemaining(uuid, "serious_punch");
            case GOKU ->
                bar += "§fKi Blast: " + cd.getFormattedRemaining(uuid, "ki_blast");
            case EREN ->
                bar += "§fColossal Shift: " + cd.getFormattedRemaining(uuid, "titan_shift")
                     + (isErenTitan(uuid) ? " §c[TITAN]" : "");
            case WUKONG ->
                bar += "§fNimbus Cloud: " + cd.getFormattedRemaining(uuid, "nimbus_cloud");
            case L ->
                bar += "§fDetective Sense: " + cd.getFormattedRemaining(uuid, "detective");
            case LIGHT ->
                bar += "§fKira's Judgement: " + cd.getFormattedRemaining(uuid, "kira");
            case NARUTO ->
                bar += "§fShadow Clones: " + cd.getFormattedRemaining(uuid, "shadow_clones");
            case ASH ->
                bar += "§fWolf Pack Recall: " + cd.getFormattedRemaining(uuid, "wolf_recall");
            default ->
                bar += "§7No active ability";
        }

        player.sendActionBar(net.kyori.adventure.text.Component.text(bar));
    }

    // ─── Persistence ────────────────────────────────────────────────────────

    private void loadData() {
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.contains("classes")) {
            for (String uuidStr : dataConfig.getConfigurationSection("classes").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String className = dataConfig.getString("classes." + uuidStr);
                    AnimeClass clazz = AnimeClass.valueOf(className);
                    playerClasses.put(uuid, clazz);
                } catch (Exception ignored) {}
            }
        }
    }

    public void saveAllData() { saveData(); }

    private void saveData() {
        if (dataFile == null) return;
        dataConfig = new YamlConfiguration();
        for (Map.Entry<UUID, AnimeClass> e : playerClasses.entrySet()) {
            dataConfig.set("classes." + e.getKey().toString(), e.getValue().name());
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save player data: " + e.getMessage());
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void applyPermanent(Player p, PotionEffectType type, int amplifier) {
        PotionEffect current = p.getPotionEffect(type);
        // Only apply if not already at this level or higher (avoids flicker)
        if (current == null || current.getAmplifier() < amplifier || current.getDuration() < 60) {
            p.addPotionEffect(new PotionEffect(type, 100, amplifier, true, false, false));
        }
    }
}
