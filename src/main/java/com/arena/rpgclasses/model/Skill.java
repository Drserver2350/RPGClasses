package com.arena.rpgclasses.model;

import org.bukkit.entity.Player;

public final class Skill {

    @FunctionalInterface
    public interface Executor {
        /** @return true if the skill was actually cast (consume mana / start cooldown) */
        boolean cast(Player caster, PlayerData data, RPGClass clazz, int level);
    }

    private final String name;
    private final String description;
    private final int unlockLevel;
    private final double cooldownSeconds;
    private final double manaCost;
    private final Executor executor;

    public Skill(String name, String description, int unlockLevel, double cooldownSeconds, double manaCost, Executor executor) {
        this.name = name;
        this.description = description;
        this.unlockLevel = unlockLevel;
        this.cooldownSeconds = cooldownSeconds;
        this.manaCost = manaCost;
        this.executor = executor;
    }

    public String name() { return name; }
    public String description() { return description; }
    public int unlockLevel() { return unlockLevel; }
    public double cooldownSeconds() { return cooldownSeconds; }
    public double manaCost() { return manaCost; }
    public Executor executor() { return executor; }
}
