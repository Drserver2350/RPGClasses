package com.arena.rpgclasses.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mutable per-player state. Level/XP is tracked per class so switching keeps progress.
 */
public final class PlayerData {

    private final UUID uuid;
    private String classId;                              // null = no class chosen
    private final Map<String, Integer> levels = new HashMap<>();
    private final Map<String, Double> xp = new HashMap<>();
    private double mana;
    private int selectedSkill = 0;                       // 0..2
    private final Map<String, Long> cooldowns = new HashMap<>(); // key = classId:skillIndex -> expiry millis
    private long lastClassChange = 0L;

    // transient combat state used by skills/passives
    public long lastCombatMillis = 0L;
    public int comboCounter = 0;
    public long tempFlagUntil = 0L;
    public String tempFlag = null;

    public PlayerData(UUID uuid) { this.uuid = uuid; }

    public UUID uuid() { return uuid; }
    public String classId() { return classId; }
    public void setClassId(String id) { this.classId = id; this.selectedSkill = 0; }
    public boolean hasClass() { return classId != null; }

    public int level() { return classId == null ? 1 : levels.getOrDefault(classId, 1); }
    public int level(String id) { return levels.getOrDefault(id, 1); }
    public void setLevel(String id, int lvl) { levels.put(id, Math.max(1, lvl)); }
    public double xp() { return classId == null ? 0 : xp.getOrDefault(classId, 0.0); }
    public double xp(String id) { return xp.getOrDefault(id, 0.0); }
    public void setXp(String id, double v) { xp.put(id, Math.max(0, v)); }
    public Map<String, Integer> allLevels() { return levels; }
    public Map<String, Double> allXp() { return xp; }

    public double mana() { return mana; }
    public void setMana(double m) { this.mana = Math.max(0, m); }

    public int selectedSkill() { return selectedSkill; }
    public void setSelectedSkill(int i) { this.selectedSkill = Math.floorMod(i, 3); }

    public long lastClassChange() { return lastClassChange; }
    public void setLastClassChange(long t) { this.lastClassChange = t; }

    public long cooldownRemaining(int skillIndex) {
        Long exp = cooldowns.get(classId + ":" + skillIndex);
        if (exp == null) return 0L;
        return Math.max(0L, exp - System.currentTimeMillis());
    }

    public void setCooldown(int skillIndex, double seconds) {
        cooldowns.put(classId + ":" + skillIndex, System.currentTimeMillis() + (long) (seconds * 1000));
    }

    public void clearCooldowns() { cooldowns.clear(); }

    public void flag(String name, long millis) { tempFlag = name; tempFlagUntil = System.currentTimeMillis() + millis; }
    public boolean hasFlag(String name) { return name.equals(tempFlag) && System.currentTimeMillis() < tempFlagUntil; }
}
