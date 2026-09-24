package com.arena.rpgclasses.model;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.List;

/**
 * Immutable definition of an RPG class.
 */
public final class RPGClass {

    public enum Role { TANK, MELEE_DPS, RANGED_DPS, CASTER, SUPPORT, HYBRID }

    private final String id;            // e.g. "pyromancer" (also the resource-pack item model id)
    private final String displayName;   // e.g. "Pyromancer"
    private final Role role;
    private final TextColor color;
    private final Material fallbackIcon;
    private final String lore;
    private final String passiveName;
    private final String passiveDescription;
    private final double bonusHealth;   // added to 20
    private final double bonusSpeed;    // multiplier added (0.1 = +10%)
    private final double bonusDamage;   // multiplier added to attack damage
    private final double bonusArmor;
    private final List<Skill> skills;
    private final String castSound;   // sounds.json key, e.g. cast.fire
    private final String particle;    // custom particle kind

    public RPGClass(String id, String displayName, Role role, TextColor color, Material fallbackIcon, String lore,
                    String passiveName, String passiveDescription,
                    double bonusHealth, double bonusSpeed, double bonusDamage, double bonusArmor,
                    List<Skill> skills, String castSound, String particle) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
        this.color = color;
        this.fallbackIcon = fallbackIcon;
        this.lore = lore;
        this.passiveName = passiveName;
        this.passiveDescription = passiveDescription;
        this.bonusHealth = bonusHealth;
        this.bonusSpeed = bonusSpeed;
        this.bonusDamage = bonusDamage;
        this.bonusArmor = bonusArmor;
        this.skills = List.copyOf(skills);
        this.castSound = castSound;
        this.particle = particle;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Role role() { return role; }
    public TextColor color() { return color; }
    public Material fallbackIcon() { return fallbackIcon; }
    public String lore() { return lore; }
    public String passiveName() { return passiveName; }
    public String passiveDescription() { return passiveDescription; }
    public double bonusHealth() { return bonusHealth; }
    public double bonusSpeed() { return bonusSpeed; }
    public double bonusDamage() { return bonusDamage; }
    public double bonusArmor() { return bonusArmor; }
    public List<Skill> skills() { return skills; }
    public String castSound() { return castSound; }
    public String particle() { return particle; }

    public String roleName() {
        return switch (role) {
            case TANK -> "Tank";
            case MELEE_DPS -> "Melee DPS";
            case RANGED_DPS -> "Ranged DPS";
            case CASTER -> "Caster";
            case SUPPORT -> "Support";
            case HYBRID -> "Hybrid";
        };
    }
}
