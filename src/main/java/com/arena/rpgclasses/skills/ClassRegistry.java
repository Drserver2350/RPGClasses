package com.arena.rpgclasses.skills;

import com.arena.rpgclasses.model.RPGClass;
import com.arena.rpgclasses.model.RPGClass.Role;
import com.arena.rpgclasses.model.Skill;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

import static com.arena.rpgclasses.util.FX.*;

/**
 * All 20 class definitions and their 60 skills live here.
 */
public final class ClassRegistry {

    private final Map<String, RPGClass> classes = new LinkedHashMap<>();

    public ClassRegistry() { registerAll(); }

    public RPGClass get(String id) { return id == null ? null : classes.get(id.toLowerCase(Locale.ROOT)); }
    public Collection<RPGClass> all() { return classes.values(); }
    public List<RPGClass> list() { return new ArrayList<>(classes.values()); }

    private void add(RPGClass c) { classes.put(c.id(), c); }

    private static TextColor c(int hex) { return TextColor.color(hex); }
    private static Color col(int r, int g, int b) { return Color.fromRGB(r, g, b); }

    // ================================================================================
    private void registerAll() {

        // ------------------------------------------------------------ 1. WARRIOR
        add(new RPGClass("warrior", "Warrior", Role.MELEE_DPS, c(0xC0392B), Material.IRON_SWORD,
                "A disciplined front-line fighter. Reliable damage, solid defence.",
                "Battle Hardened", "+15% melee damage. Every 4th hit staggers the target.",
                4, 0.0, 0.15, 1, List.of(
                new Skill("Cleave", "Sweep your blade, hitting all enemies in front of you.", 1, 6, 15, (p, d, cl, lv) -> {
                    var hits = cone(p, 4.5, 60);
                    p.swingMainHand();
                    sound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
                    particles(p.getLocation().add(p.getLocation().getDirection().multiply(1.5)).add(0, 1, 0), Particle.SWEEP_ATTACK, 4, 0.8, 0.3, 0.8, 0);
                    for (var e : hits) damage(p, e, 5 + lv * 0.35);
                    return true;
                }),
                new Skill("Charge", "Dash forward and knock enemies flying.", 5, 10, 20, (p, d, cl, lv) -> {
                    p.setVelocity(p.getLocation().getDirection().setY(0.15).normalize().multiply(1.8));
                    sound(p.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.6f);
                    repeat(8, 1, () -> {
                        particles(p.getLocation(), Particle.CLOUD, 4, 0.2, 0.1, 0.2, 0.02);
                        for (var e : enemiesNear(p, p.getLocation(), 2)) { damage(p, e, 4 + lv * 0.3); knockback(p, e, 1.2, 0.5); }
                    });
                    return true;
                }),
                new Skill("War Cry", "Roar: allies gain Strength, enemies are weakened.", 10, 40, 40, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.4f);
                    ringDust(p.getLocation(), 6, col(200, 40, 40), 48);
                    for (var a : alliesNear(p, 8)) effect(a, PotionEffectType.STRENGTH, 10 + lv * 0.3, lv >= 20 ? 1 : 0);
                    for (var e : enemiesNear(p, p.getLocation(), 8)) effect(e, PotionEffectType.WEAKNESS, 8, 0);
                    return true;
                })), "cast.generic", "star"));

        // ------------------------------------------------------------ 2. PALADIN
        add(new RPGClass("paladin", "Paladin", Role.HYBRID, c(0xF1C40F), Material.GOLDEN_SWORD,
                "A holy knight who heals allies and smites the wicked.",
                "Divine Favor", "Your melee hits heal you for 8% of damage dealt. Undead take +25% damage.",
                6, 0.0, 0.05, 2, List.of(
                new Skill("Holy Strike", "Your next strike bursts with holy light.", 1, 7, 15, (p, d, cl, lv) -> {
                    d.flag("holystrike", 6000);
                    sound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.8f);
                    helix(p.getLocation(), 2.2, col(255, 230, 120), 20);
                    msg(p, "Your blade glows with holy light!");
                    return true;
                }),
                new Skill("Lay on Hands", "Heal yourself and nearby allies.", 5, 18, 30, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                    for (var a : alliesNear(p, 6)) { heal(a, 6 + lv * 0.4); particles(a.getLocation().add(0, 1, 0), Particle.END_ROD, 12, 0.4, 0.6, 0.4, 0.02); }
                    return true;
                }),
                new Skill("Consecration", "Bless the ground: allies regenerate, undead burn.", 10, 35, 45, (p, d, cl, lv) -> {
                    Location center = p.getLocation().clone();
                    sound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.2f);
                    repeat(16, 10, () -> {
                        ringDust(center, 5, col(255, 215, 0), 40);
                        for (var a : alliesNear(p, 30)) if (a.getLocation().distanceSquared(center) <= 25) { effect(a, PotionEffectType.REGENERATION, 2, 1); effect(a, PotionEffectType.RESISTANCE, 2, 0); }
                        for (var e : enemiesNear(p, center, 5)) { e.setFireTicks(40); damage(p, e, 1.5 + lv * 0.1); }
                    });
                    return true;
                })), "cast.holy", "star"));

        // ------------------------------------------------------------ 3. BERSERKER
        add(new RPGClass("berserker", "Berserker", Role.MELEE_DPS, c(0x8E2B2B), Material.IRON_AXE,
                "Rage incarnate. The lower your health, the harder you hit.",
                "Blood Frenzy", "Deal up to +60% damage as your health drops. Kills grant a burst of speed.",
                2, 0.05, 0.1, 0, List.of(
                new Skill("Reckless Swing", "Devastating axe blow that also costs you 2 hearts.", 1, 5, 10, (p, d, cl, lv) -> {
                    var t = target(p, 4);
                    if (t == null) { msg(p, "No target in reach."); return false; }
                    p.swingMainHand();
                    sound(p.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 0.7f);
                    damage(p, t, 9 + lv * 0.6);
                    particles(t.getLocation().add(0, 1, 0), Particle.CRIT, 20, 0.3, 0.3, 0.3, 0.3);
                    p.setHealth(Math.max(1, p.getHealth() - 4));
                    return true;
                }),
                new Skill("Bloodlust", "Enter a frenzy: Speed II, Strength, Haste for a short time.", 5, 30, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_WOLF_GROWL, 1f, 0.6f);
                    effect(p, PotionEffectType.SPEED, 8 + lv * 0.2, 1);
                    effect(p, PotionEffectType.STRENGTH, 8 + lv * 0.2, 0);
                    effect(p, PotionEffectType.HASTE, 8 + lv * 0.2, 1);
                    repeat(16, 5, () -> dust(p.getLocation().add(0, 1, 0), col(160, 0, 0), 1.5f, 6, 0.4));
                    return true;
                }),
                new Skill("Earthshatter", "Slam the ground, launching every enemy around you.", 10, 25, 40, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.7f);
                    particles(p.getLocation(), Particle.EXPLOSION, 2, 0.5, 0.2, 0.5, 0);
                    p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation(), 60, 2, 0.2, 2, 0, p.getLocation().subtract(0, 1, 0).getBlock().getBlockData());
                    for (var e : enemiesNear(p, p.getLocation(), 5)) { damage(p, e, 7 + lv * 0.4); e.setVelocity(new Vector(0, 1.1, 0)); }
                    return true;
                })), "cast.generic", "ember"));

        // ------------------------------------------------------------ 4. GUARDIAN
        add(new RPGClass("guardian", "Guardian", Role.TANK, c(0x7F8C8D), Material.SHIELD,
                "An immovable bulwark. Protects the party by drawing every blow.",
                "Stalwart", "+30% knockback resistance, take 15% less damage. Blocking reflects 20%.",
                10, -0.05, 0.0, 4, List.of(
                new Skill("Taunt", "Force all nearby monsters to attack you.", 1, 12, 15, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 0.6f);
                    int n = 0;
                    for (var e : enemiesNear(p, p.getLocation(), 10)) if (e instanceof Mob m) { m.setTarget(p); n++; dust(e.getLocation().add(0, 2.2, 0), col(255, 60, 60), 1.5f, 3, 0.1); }
                    effect(p, PotionEffectType.RESISTANCE, 5, 0);
                    msg(p, "Taunted " + n + " enemies.");
                    return true;
                }),
                new Skill("Shield Wall", "Become nearly invulnerable for a few seconds, but slowed.", 5, 30, 30, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.4f);
                    effect(p, PotionEffectType.RESISTANCE, 4 + lv * 0.1, 3);
                    effect(p, PotionEffectType.SLOWNESS, 4 + lv * 0.1, 1);
                    repeat(8, 10, () -> ringDust(p.getLocation(), 1.5, col(180, 180, 200), 16));
                    return true;
                }),
                new Skill("Bastion", "Create a protective dome: allies inside take 40% less damage.", 10, 45, 50, (p, d, cl, lv) -> {
                    Location center = p.getLocation().clone();
                    sound(center, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.8f);
                    repeat(20, 10, () -> {
                        for (int i = 0; i < 20; i++) {
                            double th = Math.random() * Math.PI, ph = Math.random() * 2 * Math.PI;
                            dust(center.clone().add(5 * Math.sin(th) * Math.cos(ph), 5 * Math.cos(th) + 0.5, 5 * Math.sin(th) * Math.sin(ph)), col(120, 200, 255), 1.1f, 1, 0);
                        }
                        for (var a : alliesNear(p, 30)) if (a.getLocation().distanceSquared(center) <= 25) effect(a, PotionEffectType.RESISTANCE, 1.5, 1);
                    });
                    return true;
                })), "cast.generic", "rune"));

        // ------------------------------------------------------------ 5. RANGER
        add(new RPGClass("ranger", "Ranger", Role.RANGED_DPS, c(0x27AE60), Material.BOW,
                "Master of the bow and the wilds. Deadly at range.",
                "Eagle Eye", "+25% arrow damage. Arrows never break on hit and headshots deal double.",
                2, 0.08, 0.0, 0, List.of(
                new Skill("Multishot", "Fire a fan of 5 arrows.", 1, 6, 15, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.9f);
                    Vector dir = p.getEyeLocation().getDirection();
                    for (int i = -2; i <= 2; i++) {
                        Vector v = dir.clone().rotateAroundY(Math.toRadians(i * 7)).multiply(2.6);
                        Arrow a = p.launchProjectile(Arrow.class, v);
                        a.setDamage(4 + lv * 0.25);
                        a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        a.setCritical(true);
                    }
                    return true;
                }),
                new Skill("Piercing Shot", "A charged arrow that pierces through everything.", 5, 10, 20, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.5f);
                    Arrow a = p.launchProjectile(Arrow.class, p.getEyeLocation().getDirection().multiply(4));
                    a.setDamage(10 + lv * 0.5);
                    a.setPierceLevel(5);
                    a.setCritical(true);
                    a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    repeat(20, 1, () -> { if (!a.isDead()) particles(a.getLocation(), Particle.END_ROD, 1, 0, 0, 0, 0); });
                    return true;
                }),
                new Skill("Arrow Storm", "Call down a rain of arrows on your target location.", 10, 35, 45, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(40);
                    Location center = hit == null ? p.getLocation().add(p.getLocation().getDirection().multiply(15)) : hit.getHitPosition().toLocation(p.getWorld());
                    sound(center, Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.2f);
                    repeat(30, 2, () -> {
                        for (int i = 0; i < 3; i++) {
                            Location spawn = center.clone().add((Math.random() - 0.5) * 8, 12, (Math.random() - 0.5) * 8);
                            Arrow a = p.getWorld().spawnArrow(spawn, new Vector(0, -1, 0), 2.5f, 2f);
                            a.setShooter(p); a.setDamage(3 + lv * 0.2); a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        }
                    });
                    return true;
                })), "cast.generic", "leaf"));

        // ------------------------------------------------------------ 6. ASSASSIN
        add(new RPGClass("assassin", "Assassin", Role.MELEE_DPS, c(0x2C3E50), Material.NETHERITE_SWORD,
                "Strikes from the shadows. Backstabs and vanishes before retaliation.",
                "Backstab", "Attacks from behind deal +75% damage. Sneaking makes you immune to mob targeting for 3s after a kill.",
                0, 0.12, 0.1, 0, List.of(
                new Skill("Shadowstep", "Teleport behind your target.", 1, 8, 15, (p, d, cl, lv) -> {
                    var t = target(p, 14);
                    if (t == null) { msg(p, "No target in sight."); return false; }
                    Location behind = t.getLocation().clone().subtract(t.getLocation().getDirection().setY(0).normalize().multiply(1.5));
                    behind.setDirection(t.getLocation().toVector().subtract(behind.toVector()));
                    particles(p.getLocation().add(0, 1, 0), Particle.SMOKE, 30, 0.3, 0.6, 0.3, 0.02);
                    p.teleport(behind);
                    particles(behind.add(0, 1, 0), Particle.SMOKE, 30, 0.3, 0.6, 0.3, 0.02);
                    sound(behind, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.6f);
                    d.flag("shadowstep", 3000);
                    return true;
                }),
                new Skill("Vanish", "Become invisible and fast. Mobs lose track of you.", 5, 25, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1f, 1.2f);
                    particles(p.getLocation().add(0, 1, 0), Particle.LARGE_SMOKE, 40, 0.4, 0.6, 0.4, 0.05);
                    effect(p, PotionEffectType.INVISIBILITY, 6 + lv * 0.2, 0);
                    effect(p, PotionEffectType.SPEED, 6 + lv * 0.2, 1);
                    for (var e : enemiesNear(p, p.getLocation(), 16)) if (e instanceof Mob m && m.getTarget() == p) m.setTarget(null);
                    return true;
                }),
                new Skill("Death Mark", "Mark a target: it takes +40% damage and bleeds.", 10, 30, 35, (p, d, cl, lv) -> {
                    var t = target(p, 14);
                    if (t == null) { msg(p, "No target in sight."); return false; }
                    sound(t.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.6f, 1.8f);
                    t.setMetadata("rpg_marked", new org.bukkit.metadata.FixedMetadataValue(plugin(), System.currentTimeMillis() + 10000));
                    repeat(10, 20, () -> { if (!t.isDead()) { damage(p, t, 1.5 + lv * 0.1); dust(t.getLocation().add(0, 2.3, 0), col(120, 0, 0), 2f, 4, 0.1); } });
                    return true;
                })), "cast.dark", "skull"));

        // ------------------------------------------------------------ 7. SAMURAI
        add(new RPGClass("samurai", "Samurai", Role.MELEE_DPS, c(0xE74C3C), Material.DIAMOND_SWORD,
                "Precision and honor. Fast strikes, counters and a devastating iaijutsu.",
                "Bushido", "Consecutive hits within 2s build a combo: +8% damage per stack (max 5).",
                2, 0.06, 0.1, 1, List.of(
                new Skill("Iaijutsu", "Blink forward, slashing everything in your path.", 1, 8, 15, (p, d, cl, lv) -> {
                    Location start = p.getLocation().clone();
                    Vector dir = start.getDirection().setY(0).normalize();
                    Location end = start.clone().add(dir.clone().multiply(6));
                    for (int i = 6; i > 0 && !end.getBlock().isPassable(); i--) end = start.clone().add(dir.clone().multiply(i));
                    lineDust(start.clone().add(0, 1, 0), end.clone().add(0, 1, 0), col(255, 255, 255), 0.3);
                    for (var e : enemiesNear(p, start.clone().add(dir.clone().multiply(3)), 3.5)) damage(p, e, 8 + lv * 0.5);
                    p.teleport(end.setDirection(start.getDirection()));
                    sound(end, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.8f);
                    sound(end, Sound.ITEM_TRIDENT_RETURN, 1f, 2f);
                    return true;
                }),
                new Skill("Parry Stance", "For 3s, the next hit you take is negated and countered.", 5, 15, 20, (p, d, cl, lv) -> {
                    d.flag("parry", 3000);
                    sound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1f, 1.5f);
                    repeat(6, 10, () -> ringDust(p.getLocation().add(0, 1, 0), 0.9, col(255, 255, 200), 12));
                    return true;
                }),
                new Skill("Thousand Cuts", "Unleash a flurry of slashes on everything nearby.", 10, 28, 40, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);
                    repeat(10, 3, () -> {
                        p.swingMainHand();
                        particles(p.getLocation().add(0, 1, 0), Particle.SWEEP_ATTACK, 3, 1.5, 0.5, 1.5, 0);
                        for (var e : enemiesNear(p, p.getLocation(), 3.5)) damage(p, e, 2.5 + lv * 0.15);
                    });
                    return true;
                })), "cast.generic", "star"));

        // ------------------------------------------------------------ 8. MAGE
        add(new RPGClass("mage", "Mage", Role.CASTER, c(0x3498DB), Material.BLAZE_ROD,
                "Master of the arcane. Raw magical power at range.",
                "Arcane Mind", "+50% max mana and +50% mana regeneration.",
                -2, 0.0, 0.0, 0, List.of(
                new Skill("Arcane Missile", "Fire a bolt of pure magic.", 1, 2.5, 12, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1f, 1.5f);
                    bolt(p, 1.0, 30, l -> dust(l, col(150, 80, 255), 1.3f, 3, 0.1),
                            e -> { damage(p, e, 6 + lv * 0.45); particles(e.getLocation().add(0, 1, 0), Particle.WITCH, 20, 0.3, 0.3, 0.3, 0.1); }, null);
                    return true;
                }),
                new Skill("Blink", "Teleport a short distance forward.", 5, 6, 15, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(10 + lv * 0.2);
                    Location dest = hit == null ? p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(10 + lv * 0.2)) : hit.getHitPosition().toLocation(p.getWorld()).subtract(p.getEyeLocation().getDirection().multiply(1));
                    dest.setDirection(p.getLocation().getDirection());
                    while (!dest.getBlock().isPassable() && dest.getY() < p.getWorld().getMaxHeight()) dest.add(0, 1, 0);
                    particles(p.getLocation().add(0, 1, 0), Particle.PORTAL, 40, 0.3, 0.6, 0.3, 0.5);
                    p.teleport(dest);
                    sound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                    return true;
                }),
                new Skill("Meteor", "Call a meteor down on your target.", 10, 30, 50, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(40);
                    Location target = hit == null ? p.getLocation().add(p.getLocation().getDirection().multiply(20)) : hit.getHitPosition().toLocation(p.getWorld());
                    Location from = target.clone().add(6, 18, 6);
                    sound(target, Sound.ENTITY_WITHER_SHOOT, 1f, 0.5f);
                    final int steps = 25;
                    final int[] i = {0};
                    repeat(steps + 1, 1, () -> {
                        double f = i[0] / (double) steps;
                        Location cur = from.clone().add(target.clone().subtract(from).toVector().multiply(f));
                        particles(cur, Particle.FLAME, 15, 0.4, 0.4, 0.4, 0.02);
                        particles(cur, Particle.LAVA, 2, 0.2, 0.2, 0.2, 0);
                        if (i[0] == steps) {
                            particles(target, Particle.EXPLOSION_EMITTER, 2, 0, 0, 0, 0);
                            sound(target, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
                            for (var e : enemiesNear(p, target, 5)) { damage(p, e, 14 + lv * 0.7); e.setFireTicks(80); knockback(p, e, 0.8, 0.6); }
                        }
                        i[0]++;
                    });
                    return true;
                })), "cast.generic", "rune"));

        // ------------------------------------------------------------ 9. PYROMANCER
        add(new RPGClass("pyromancer", "Pyromancer", Role.CASTER, c(0xE67E22), Material.FIRE_CHARGE,
                "Wields living flame. Burns everything.",
                "Ember Heart", "Immune to fire and lava. Your attacks ignite enemies.",
                0, 0.0, 0.0, 0, List.of(
                new Skill("Fireball", "Hurl an explosive ball of fire.", 1, 4, 15, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 1f);
                    bolt(p, 0.9, 35, l -> { particles(l, Particle.FLAME, 6, 0.1, 0.1, 0.1, 0.01); particles(l, Particle.SMOKE, 2, 0.05, 0.05, 0.05, 0); },
                            e -> { for (var x : enemiesNear(p, e.getLocation(), 3)) { damage(p, x, 7 + lv * 0.45); x.setFireTicks(80); } particles(e.getLocation(), Particle.EXPLOSION, 1, 0, 0, 0, 0); sound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f); }, null);
                    return true;
                }),
                new Skill("Flame Wave", "A wave of fire erupts in a cone in front of you.", 5, 12, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 0.8f);
                    Vector dir = p.getLocation().getDirection().setY(0).normalize();
                    final int[] step = {1};
                    repeat(7, 2, () -> {
                        Location c = p.getLocation().add(dir.clone().multiply(step[0]));
                        for (int k = -step[0]; k <= step[0]; k++) {
                            Location l = c.clone().add(dir.clone().rotateAroundY(Math.PI / 2).multiply(k * 0.7));
                            particles(l, Particle.FLAME, 8, 0.2, 0.3, 0.2, 0.02);
                        }
                        for (var e : enemiesNear(p, c, 1.6 + step[0] * 0.4)) { damage(p, e, 3 + lv * 0.2); e.setFireTicks(100); }
                        step[0]++;
                    });
                    return true;
                }),
                new Skill("Inferno", "Become a pillar of fire, burning everything around you.", 10, 40, 50, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.5f);
                    effect(p, PotionEffectType.FIRE_RESISTANCE, 12, 0);
                    repeat(20, 5, () -> {
                        particles(p.getLocation(), Particle.FLAME, 40, 1.5, 1.5, 1.5, 0.05);
                        particles(p.getLocation(), Particle.LAVA, 5, 1.5, 0.5, 1.5, 0);
                        for (var e : enemiesNear(p, p.getLocation(), 5)) { damage(p, e, 2 + lv * 0.15); e.setFireTicks(60); }
                    });
                    return true;
                })), "cast.fire", "ember"));

        // ------------------------------------------------------------ 10. CRYOMANCER
        add(new RPGClass("cryomancer", "Cryomancer", Role.CASTER, c(0x5DADE2), Material.PACKED_ICE,
                "Commands the cold. Freezes and shatters foes.",
                "Frostbite", "Your hits slow enemies. You walk on water (frost walker) and ignore freezing.",
                0, 0.0, 0.0, 1, List.of(
                new Skill("Ice Lance", "A piercing shard of ice that slows.", 1, 3, 12, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.8f);
                    bolt(p, 1.2, 35, l -> particles(l, Particle.SNOWFLAKE, 4, 0.1, 0.1, 0.1, 0.01),
                            e -> { damage(p, e, 6 + lv * 0.4); effect(e, PotionEffectType.SLOWNESS, 4, 2); e.setFreezeTicks(140); }, null);
                    return true;
                }),
                new Skill("Frost Nova", "Freeze all enemies around you in place.", 5, 15, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.BLOCK_POWDER_SNOW_BREAK, 1f, 0.5f);
                    for (double r = 1; r <= 6; r += 1) { final double rr = r; later((long) r, () -> ring(p.getLocation(), rr, Particle.SNOWFLAKE, (int) (rr * 8))); }
                    for (var e : enemiesNear(p, p.getLocation(), 6)) { damage(p, e, 4 + lv * 0.25); effect(e, PotionEffectType.SLOWNESS, 4 + lv * 0.1, 6); effect(e, PotionEffectType.MINING_FATIGUE, 4, 2); e.setFreezeTicks(200); }
                    return true;
                }),
                new Skill("Blizzard", "Summon a blizzard over an area.", 10, 40, 50, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(30);
                    Location center = hit == null ? p.getLocation().add(p.getLocation().getDirection().multiply(12)) : hit.getHitPosition().toLocation(p.getWorld());
                    sound(center, Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.5f);
                    repeat(24, 5, () -> {
                        particles(center.clone().add(0, 4, 0), Particle.SNOWFLAKE, 60, 4, 3, 4, 0.1);
                        particles(center.clone().add(0, 3, 0), Particle.CLOUD, 6, 4, 1, 4, 0);
                        for (var e : enemiesNear(p, center, 6)) { damage(p, e, 1.5 + lv * 0.12); effect(e, PotionEffectType.SLOWNESS, 2, 2); e.setFreezeTicks(e.getFreezeTicks() + 30); }
                    });
                    return true;
                })), "cast.ice", "snow"));

        // ------------------------------------------------------------ 11. STORMCALLER
        add(new RPGClass("stormcaller", "Stormcaller", Role.CASTER, c(0xF4D03F), Material.LIGHTNING_ROD,
                "Channels thunder and wind. Fast, loud, electrifying.",
                "Static Charge", "Every 5th hit calls a lightning strike. Immune to lightning damage.",
                0, 0.05, 0.0, 0, List.of(
                new Skill("Chain Lightning", "Lightning arcs between up to 5 enemies.", 1, 7, 18, (p, d, cl, lv) -> {
                    var first = target(p, 18);
                    if (first == null) { msg(p, "No target in sight."); return false; }
                    Set<LivingEntity> hitSet = new HashSet<>();
                    LivingEntity cur = first; Location prev = p.getEyeLocation();
                    for (int i = 0; i < 5 && cur != null; i++) {
                        hitSet.add(cur);
                        lineDust(prev, cur.getLocation().add(0, 1, 0), col(255, 255, 120), 0.25);
                        damage(p, cur, (7 + lv * 0.4) * Math.pow(0.85, i));
                        sound(cur.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.4f, 1.8f);
                        prev = cur.getLocation().add(0, 1, 0);
                        LivingEntity next = null; double best = 36;
                        for (var e : enemiesNear(p, cur.getLocation(), 6)) { double ds = e.getLocation().distanceSquared(cur.getLocation()); if (!hitSet.contains(e) && ds < best) { best = ds; next = e; } }
                        cur = next;
                    }
                    return true;
                }),
                new Skill("Gale Leap", "A gust launches you high into the air. No fall damage.", 5, 8, 15, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 1f);
                    particles(p.getLocation(), Particle.GUST, 1, 0, 0, 0, 0);
                    p.setVelocity(p.getLocation().getDirection().multiply(1.2).setY(1.4));
                    d.flag("nofall", 8000);
                    for (var e : enemiesNear(p, p.getLocation(), 3)) knockback(p, e, 1.5, 0.4);
                    return true;
                }),
                new Skill("Tempest", "Lightning rains down on every enemy nearby.", 10, 35, 50, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f);
                    final int[] n = {0};
                    repeat(12, 5, () -> {
                        var list = enemiesNear(p, p.getLocation(), 12);
                        if (list.isEmpty()) return;
                        var e = list.get(n[0]++ % list.size());
                        p.getWorld().strikeLightningEffect(e.getLocation());
                        damage(p, e, 6 + lv * 0.35);
                        e.setFireTicks(40);
                    });
                    return true;
                })), "cast.thunder", "star"));

        // ------------------------------------------------------------ 12. NECROMANCER
        add(new RPGClass("necromancer", "Necromancer", Role.CASTER, c(0x6C3483), Material.WITHER_SKELETON_SKULL,
                "Commands the dead. Drains life and raises minions.",
                "Soul Harvest", "Kills restore 20 mana and 2 hearts. Undead never target you.",
                0, 0.0, 0.0, 0, List.of(
                new Skill("Life Drain", "Siphon health from a target.", 1, 6, 15, (p, d, cl, lv) -> {
                    var t = target(p, 12);
                    if (t == null) { msg(p, "No target in sight."); return false; }
                    sound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.6f);
                    final int[] i = {0};
                    repeat(6, 4, () -> { if (t.isDead()) return; lineDust(t.getLocation().add(0, 1, 0), p.getEyeLocation(), col(80, 0, 120), 0.4); damage(p, t, 1.5 + lv * 0.12); heal(p, 1 + lv * 0.06); i[0]++; });
                    return true;
                }),
                new Skill("Raise Dead", "Summon skeleton minions that fight for you.", 5, 40, 35, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.6f, 0.5f);
                    int count = 2 + lv / 10;
                    for (int i = 0; i < count; i++) {
                        Location l = p.getLocation().add((Math.random() - 0.5) * 4, 0, (Math.random() - 0.5) * 4);
                        Skeleton s = p.getWorld().spawn(l, Skeleton.class, sk -> {
                            sk.customName(text("Risen Minion", c(0x9B59B6))); sk.setCustomNameVisible(true);
                            sk.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.IRON_SWORD));
                            sk.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.CHAINMAIL_HELMET));
                            sk.setMetadata("rpg_minion", new org.bukkit.metadata.FixedMetadataValue(plugin(), p.getUniqueId().toString()));
                            sk.setRemoveWhenFarAway(true);
                        });
                        particles(l.add(0, 1, 0), Particle.SOUL, 20, 0.3, 0.5, 0.3, 0.02);
                        var t = target(p, 20); if (t != null) s.setTarget(t);
                        later(20L * 45, () -> { if (!s.isDead()) { particles(s.getLocation().add(0, 1, 0), Particle.SOUL, 15, 0.3, 0.5, 0.3, 0.02); s.remove(); } });
                    }
                    return true;
                }),
                new Skill("Plague", "Spread a withering plague that jumps between enemies.", 10, 35, 45, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.8f);
                    for (var e : enemiesNear(p, p.getLocation(), 10)) { effect(e, PotionEffectType.WITHER, 8 + lv * 0.2, 1); effect(e, PotionEffectType.SLOWNESS, 8, 0); particles(e.getLocation().add(0, 1, 0), Particle.SCULK_SOUL, 20, 0.3, 0.5, 0.3, 0.02); }
                    return true;
                })), "cast.dark", "skull"));

        // ------------------------------------------------------------ 13. WARLOCK
        add(new RPGClass("warlock", "Warlock", Role.CASTER, c(0x9B59B6), Material.ENDER_EYE,
                "Bargains with the void. Curses and dark bolts.",
                "Dark Pact", "Casting costs health instead of mana when mana is empty. +20% spell damage below half health.",
                -2, 0.0, 0.0, 0, List.of(
                new Skill("Shadow Bolt", "A bolt of darkness that blinds.", 1, 3, 12, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_SHULKER_SHOOT, 1f, 0.6f);
                    bolt(p, 1.0, 30, l -> dust(l, col(40, 0, 60), 1.5f, 4, 0.12),
                            e -> { damage(p, e, 6 + lv * 0.45); effect(e, PotionEffectType.BLINDNESS, 3, 0); effect(e, PotionEffectType.DARKNESS, 3, 0); }, null);
                    return true;
                }),
                new Skill("Curse of Agony", "Curse enemies around you with lasting pain.", 5, 15, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.6f, 1.5f);
                    for (var e : enemiesNear(p, p.getLocation(), 8)) { effect(e, PotionEffectType.POISON, 6 + lv * 0.2, 1); effect(e, PotionEffectType.WEAKNESS, 8, 1); particles(e.getLocation().add(0, 2, 0), Particle.WITCH, 15, 0.3, 0.3, 0.3, 0); }
                    return true;
                }),
                new Skill("Void Rift", "Open a rift that pulls in and crushes enemies.", 10, 35, 50, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(25);
                    Location center = hit == null ? p.getLocation().add(p.getLocation().getDirection().multiply(10)) : hit.getHitPosition().toLocation(p.getWorld()).add(0, 1, 0);
                    sound(center, Sound.BLOCK_END_PORTAL_SPAWN, 0.8f, 0.5f);
                    repeat(16, 5, () -> {
                        particles(center, Particle.PORTAL, 60, 0.5, 0.5, 0.5, 1.5);
                        particles(center, Particle.REVERSE_PORTAL, 30, 2, 2, 2, 0.1);
                        for (var e : enemiesNear(p, center, 8)) { pullTo(center, e, 0.5); damage(p, e, 1.5 + lv * 0.15); }
                    });
                    return true;
                })), "cast.dark", "rune"));

        // ------------------------------------------------------------ 14. DRUID
        add(new RPGClass("druid", "Druid", Role.HYBRID, c(0x196F3D), Material.OAK_SAPLING,
                "One with nature. Roots, healing rain and the fury of the beast.",
                "Wild Growth", "Regenerate health while standing on grass or leaves. Animals never flee you.",
                2, 0.03, 0.0, 1, List.of(
                new Skill("Entangle", "Roots burst from the earth, rooting enemies.", 1, 8, 15, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.BLOCK_GRASS_BREAK, 1f, 0.5f);
                    for (var e : cone(p, 8, 45)) { effect(e, PotionEffectType.SLOWNESS, 3 + lv * 0.1, 10); effect(e, PotionEffectType.JUMP_BOOST, 3, 250); damage(p, e, 2 + lv * 0.15); p.getWorld().spawnParticle(Particle.BLOCK, e.getLocation().add(0, 0.5, 0), 30, 0.4, 0.5, 0.4, 0, Material.OAK_LEAVES.createBlockData()); }
                    return true;
                }),
                new Skill("Healing Rain", "A gentle rain heals allies over time.", 5, 25, 30, (p, d, cl, lv) -> {
                    Location center = p.getLocation().clone();
                    sound(center, Sound.WEATHER_RAIN, 0.8f, 1.2f);
                    repeat(10, 20, () -> {
                        particles(center.clone().add(0, 4, 0), Particle.FALLING_WATER, 40, 4, 0.5, 4, 0);
                        particles(center, Particle.HAPPY_VILLAGER, 10, 4, 1, 4, 0);
                        for (var a : alliesNear(p, 30)) if (a.getLocation().distanceSquared(center) <= 25) heal(a, 1.5 + lv * 0.1);
                    });
                    return true;
                }),
                new Skill("Bear Form", "Take the form of a bear: massive health, strength and slowness.", 10, 60, 40, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_POLAR_BEAR_WARNING, 1f, 0.8f);
                    effect(p, PotionEffectType.ABSORPTION, 15 + lv * 0.3, 3);
                    effect(p, PotionEffectType.STRENGTH, 15 + lv * 0.3, 1);
                    effect(p, PotionEffectType.RESISTANCE, 15 + lv * 0.3, 0);
                    effect(p, PotionEffectType.SLOWNESS, 15 + lv * 0.3, 0);
                    p.getWorld().spawnParticle(Particle.BLOCK, p.getLocation().add(0, 1, 0), 60, 0.5, 0.8, 0.5, 0, Material.BROWN_WOOL.createBlockData());
                    return true;
                })), "cast.generic", "leaf"));

        // ------------------------------------------------------------ 15. CLERIC
        add(new RPGClass("cleric", "Cleric", Role.SUPPORT, c(0xFDFEFE), Material.TOTEM_OF_UNDYING,
                "Devoted healer. Keeps the party alive and banishes the undead.",
                "Sanctity", "Healing you do is +30%. Undead within 6 blocks take holy damage every second.",
                2, 0.0, -0.1, 1, List.of(
                new Skill("Heal", "Heal the ally you look at (or yourself).", 1, 4, 15, (p, d, cl, lv) -> {
                    var r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), 15, 1, e -> e instanceof Player o && o != p);
                    LivingEntity t = r == null ? p : (LivingEntity) r.getHitEntity();
                    heal(t, (7 + lv * 0.45) * 1.3);
                    particles(t.getLocation().add(0, 1, 0), Particle.END_ROD, 15, 0.3, 0.6, 0.3, 0.03);
                    sound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
                    return true;
                }),
                new Skill("Smite", "Holy light strikes a target. Devastating against undead.", 5, 8, 20, (p, d, cl, lv) -> {
                    var t = target(p, 20);
                    if (t == null) { msg(p, "No target in sight."); return false; }
                    boolean undead = isUndead(t);
                    lineDust(t.getLocation().add(0, 12, 0), t.getLocation(), col(255, 255, 180), 0.4);
                    sound(t.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.5f, 2f);
                    damage(p, t, (6 + lv * 0.4) * (undead ? 2.0 : 1.0));
                    return true;
                }),
                new Skill("Divine Shield", "Grant allies a shield of absorption and remove debuffs.", 10, 40, 45, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.6f, 1.5f);
                    for (var a : alliesNear(p, 10)) {
                        effect(a, PotionEffectType.ABSORPTION, 12, 2);
                        for (var t : List.of(PotionEffectType.POISON, PotionEffectType.WITHER, PotionEffectType.SLOWNESS, PotionEffectType.WEAKNESS, PotionEffectType.BLINDNESS, PotionEffectType.DARKNESS)) a.removePotionEffect(t);
                        a.setFireTicks(0);
                        helix(a.getLocation(), 2.5, col(255, 255, 200), 25);
                    }
                    return true;
                })), "cast.holy", "star"));

        // ------------------------------------------------------------ 16. SHAMAN
        add(new RPGClass("shaman", "Shaman", Role.SUPPORT, c(0x1ABC9C), Material.TURTLE_EGG,
                "Speaks with spirits. Totems, elemental fury and ancestral wisdom.",
                "Spirit Link", "10% of damage you take is shared with nearby enemies. +2 hearts per nearby ally (max 3).",
                2, 0.0, 0.0, 1, List.of(
                new Skill("Lightning Totem", "Plant a totem that zaps nearby enemies.", 1, 20, 20, (p, d, cl, lv) -> {
                    Location l = p.getLocation().clone();
                    ArmorStand as = p.getWorld().spawn(l, ArmorStand.class, a -> { a.setInvisible(true); a.setMarker(true); a.setInvulnerable(true); a.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.LIGHTNING_ROD)); a.customName(text("⚡ Totem", c(0xF4D03F))); a.setCustomNameVisible(true); });
                    sound(l, Sound.BLOCK_WOOD_PLACE, 1f, 0.6f);
                    repeat(12, 20, () -> { var list = enemiesNear(p, l, 7); if (list.isEmpty()) return; var e = list.get((int) (Math.random() * list.size())); lineDust(l.clone().add(0, 2, 0), e.getLocation().add(0, 1, 0), col(255, 255, 100), 0.3); damage(p, e, 3 + lv * 0.25); sound(e.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.3f, 2f); });
                    later(12 * 20 + 5, as::remove);
                    return true;
                }),
                new Skill("Healing Totem", "Plant a totem that heals nearby allies.", 5, 30, 30, (p, d, cl, lv) -> {
                    Location l = p.getLocation().clone();
                    ArmorStand as = p.getWorld().spawn(l, ArmorStand.class, a -> { a.setInvisible(true); a.setMarker(true); a.setInvulnerable(true); a.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.TURTLE_EGG)); a.customName(text("✚ Totem", c(0x1ABC9C))); a.setCustomNameVisible(true); });
                    sound(l, Sound.BLOCK_WOOD_PLACE, 1f, 0.6f);
                    repeat(15, 20, () -> { ringDust(l, 5, col(30, 200, 150), 24); for (var a : alliesNear(p, 30)) if (a.getLocation().distanceSquared(l) <= 25) heal(a, 1.5 + lv * 0.1); });
                    later(15 * 20 + 5, as::remove);
                    return true;
                }),
                new Skill("Ancestral Fury", "Spirits empower your allies: Strength, Speed, Regen.", 10, 50, 50, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1f, 1.2f);
                    for (var a : alliesNear(p, 12)) { effect(a, PotionEffectType.STRENGTH, 12, 0); effect(a, PotionEffectType.SPEED, 12, 0); effect(a, PotionEffectType.REGENERATION, 12, 0); particles(a.getLocation().add(0, 1, 0), Particle.SOUL_FIRE_FLAME, 25, 0.4, 0.6, 0.4, 0.03); }
                    return true;
                })), "cast.generic", "rune"));

        // ------------------------------------------------------------ 17. BARD
        add(new RPGClass("bard", "Bard", Role.SUPPORT, c(0xF39C12), Material.GOAT_HORN,
                "Inspires allies and demoralizes enemies with song.",
                "Inspiration", "Allies within 10 blocks gain +10% speed. Your XP gain is +25%.",
                0, 0.05, 0.0, 0, List.of(
                new Skill("Song of Courage", "Allies gain Strength and Resistance.", 1, 25, 20, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 1f, 1.2f);
                    for (var a : alliesNear(p, 12)) { effect(a, PotionEffectType.STRENGTH, 10 + lv * 0.25, 0); effect(a, PotionEffectType.RESISTANCE, 10 + lv * 0.25, 0); particles(a.getLocation().add(0, 1.5, 0), Particle.NOTE, 8, 0.4, 0.4, 0.4, 1); }
                    return true;
                }),
                new Skill("Dissonance", "A screeching chord that confuses and slows enemies.", 5, 15, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 0.6f, 1.8f);
                    for (var e : enemiesNear(p, p.getLocation(), 8)) { effect(e, PotionEffectType.NAUSEA, 8, 0); effect(e, PotionEffectType.SLOWNESS, 6, 1); effect(e, PotionEffectType.WEAKNESS, 6, 0); damage(p, e, 3 + lv * 0.2); particles(e.getLocation().add(0, 1.5, 0), Particle.NOTE, 10, 0.4, 0.4, 0.4, 1); }
                    return true;
                }),
                new Skill("Encore", "Reset your allies' skill cooldowns and refill their mana.", 10, 90, 60, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.5f);
                    for (var a : alliesNear(p, 12)) { if (a == p) continue; var pd = com.arena.rpgclasses.RPGClassesPlugin.get().data().get(a); pd.clearCooldowns(); pd.setMana(com.arena.rpgclasses.RPGClassesPlugin.get().mana().maxMana(a, pd)); helix(a.getLocation(), 2.5, col(255, 180, 60), 30); msg(a, p.getName() + " played an Encore! Cooldowns reset."); }
                    return true;
                })), "cast.generic", "star"));

        // ------------------------------------------------------------ 18. MONK
        add(new RPGClass("monk", "Monk", Role.MELEE_DPS, c(0xD4AC0D), Material.STICK,
                "Fights with fists and inner chi. Fast, evasive, unarmored.",
                "Inner Peace", "Unarmed strikes deal +6 damage. No fall damage. Dodge 15% of attacks.",
                2, 0.15, 0.0, 0, List.of(
                new Skill("Palm Strike", "A chi-charged palm that sends the target flying.", 1, 5, 10, (p, d, cl, lv) -> {
                    var t = target(p, 4);
                    if (t == null) { msg(p, "No target in reach."); return false; }
                    sound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1.5f);
                    damage(p, t, 6 + lv * 0.4);
                    knockback(p, t, 2.2, 0.6);
                    particles(t.getLocation().add(0, 1, 0), Particle.EXPLOSION, 1, 0, 0, 0, 0);
                    return true;
                }),
                new Skill("Chi Burst", "Release stored chi: Speed, Haste, Jump for 10s and dash.", 5, 20, 20, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1f, 1.5f);
                    effect(p, PotionEffectType.SPEED, 10, 2); effect(p, PotionEffectType.HASTE, 10, 2); effect(p, PotionEffectType.JUMP_BOOST, 10, 1);
                    p.setVelocity(p.getLocation().getDirection().multiply(1.5).setY(0.4));
                    d.flag("nofall", 5000);
                    return true;
                }),
                new Skill("Fists of Fury", "A flurry of lightning-fast punches on all nearby enemies.", 10, 30, 40, (p, d, cl, lv) -> {
                    repeat(12, 2, () -> { p.swingMainHand(); sound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 1.6f); for (var e : enemiesNear(p, p.getLocation(), 3)) { damage(p, e, 2 + lv * 0.15); particles(e.getLocation().add(0, 1, 0), Particle.CRIT, 6, 0.3, 0.3, 0.3, 0.2); } });
                    return true;
                })), "cast.generic", "star"));

        // ------------------------------------------------------------ 19. ALCHEMIST
        add(new RPGClass("alchemist", "Alchemist", Role.RANGED_DPS, c(0x58D68D), Material.SPLASH_POTION,
                "Brews volatile concoctions. Throws acid, gas and elixirs.",
                "Chemist", "Potions you drink last 50% longer. Immune to poison.",
                0, 0.0, 0.0, 0, List.of(
                new Skill("Acid Flask", "Throw a flask of corrosive acid.", 1, 4, 12, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_SPLASH_POTION_THROW, 1f, 0.8f);
                    ThrownPotion pot = p.launchProjectile(ThrownPotion.class, p.getEyeLocation().getDirection().multiply(1.3));
                    var item = new org.bukkit.inventory.ItemStack(Material.SPLASH_POTION);
                    var meta = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
                    meta.setColor(col(80, 255, 60));
                    meta.addCustomEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.POISON, 100 + lv * 4, 1), true);
                    meta.addCustomEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, lv >= 15 ? 1 : 0), true);
                    item.setItemMeta(meta); pot.setItem(item);
                    return true;
                }),
                new Skill("Elixir", "Drink a powerful elixir: Regen, Speed, Strength, Fire Res.", 5, 30, 25, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1f, 1f);
                    effect(p, PotionEffectType.REGENERATION, 8 + lv * 0.2, 1); effect(p, PotionEffectType.SPEED, 12, 0); effect(p, PotionEffectType.STRENGTH, 12, 0); effect(p, PotionEffectType.FIRE_RESISTANCE, 30, 0);
                    p.getWorld().spawnParticle(Particle.ENTITY_EFFECT, p.getLocation().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0, col(80, 255, 120));
                    return true;
                }),
                new Skill("Toxic Cloud", "Release a lingering toxic gas cloud.", 10, 35, 45, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(20);
                    Location center = hit == null ? p.getLocation().add(p.getLocation().getDirection().multiply(8)) : hit.getHitPosition().toLocation(p.getWorld()).add(0, 0.5, 0);
                    sound(center, Sound.ENTITY_SPLASH_POTION_BREAK, 1f, 0.6f);
                    AreaEffectCloud cloud = p.getWorld().spawn(center, AreaEffectCloud.class, cl2 -> {
                        cl2.setRadius(4.5f); cl2.setDuration(200 + lv * 4); cl2.setColor(col(90, 200, 40)); cl2.setSource(p);
                        cl2.addCustomEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.POISON, 60, 1), true);
                        cl2.addCustomEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.WEAKNESS, 80, 1), true);
                        cl2.addCustomEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.NAUSEA, 80, 0), true);
                        cl2.setRadiusPerTick(-0.01f);
                    });
                    return cloud != null;
                })), "cast.generic", "leaf"));

        // ------------------------------------------------------------ 20. ARTIFICER
        add(new RPGClass("artificer", "Artificer", Role.RANGED_DPS, c(0xAAB7B8), Material.REDSTONE,
                "Engineer of war machines. Turrets, grappling hooks and explosives.",
                "Tinkerer", "Tools and weapons you use lose durability 50% slower. +20% mining speed.",
                2, 0.0, 0.0, 2, List.of(
                new Skill("Grapple", "Fire a grappling hook and pull yourself to it.", 1, 6, 10, (p, d, cl, lv) -> {
                    var hit = p.rayTraceBlocks(30);
                    if (hit == null) { msg(p, "Nothing to grapple onto."); return false; }
                    Location to = hit.getHitPosition().toLocation(p.getWorld());
                    lineDust(p.getEyeLocation(), to, col(120, 120, 120), 0.5);
                    sound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_THROW, 1f, 0.6f);
                    Vector v = to.toVector().subtract(p.getLocation().toVector());
                    double dist = v.length();
                    p.setVelocity(v.normalize().multiply(Math.min(3.0, 0.8 + dist * 0.12)).add(new Vector(0, 0.35, 0)));
                    d.flag("nofall", 6000);
                    return true;
                }),
                new Skill("Deploy Turret", "Build an auto-turret that shoots arrows at enemies.", 5, 30, 30, (p, d, cl, lv) -> {
                    Location l = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(1.5));
                    ArmorStand as = p.getWorld().spawn(l, ArmorStand.class, a -> { a.setInvulnerable(true); a.setGravity(false); a.setBasePlate(false); a.setArms(true); a.getEquipment().setHelmet(new org.bukkit.inventory.ItemStack(Material.DISPENSER)); a.getEquipment().setItemInMainHand(new org.bukkit.inventory.ItemStack(Material.CROSSBOW)); a.customName(text("⚙ Turret", c(0xAAB7B8))); a.setCustomNameVisible(true); });
                    sound(l, Sound.BLOCK_ANVIL_USE, 0.7f, 1.5f);
                    int shots = 20 + lv;
                    repeat(shots, 10, () -> {
                        var list = enemiesNear(p, l, 14); if (list.isEmpty() || as.isDead()) return;
                        LivingEntity t = list.get(0);
                        Location eye = l.clone().add(0, 1.8, 0);
                        Vector dir = t.getLocation().add(0, 1, 0).subtract(eye).toVector().normalize();
                        as.setRotation((float) Math.toDegrees(Math.atan2(-dir.getX(), dir.getZ())), 0);
                        Arrow a = p.getWorld().spawnArrow(eye.add(dir), dir, 2.5f, 1f);
                        a.setShooter(p); a.setDamage(4 + lv * 0.25); a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                        sound(eye, Sound.ITEM_CROSSBOW_SHOOT, 0.6f, 1.2f);
                    });
                    later(shots * 10L + 10, () -> { particles(as.getLocation().add(0, 1, 0), Particle.LARGE_SMOKE, 20, 0.3, 0.5, 0.3, 0.02); as.remove(); });
                    return true;
                }),
                new Skill("Cluster Bomb", "Launch a bomb that splits into several explosions.", 10, 30, 45, (p, d, cl, lv) -> {
                    sound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 1.5f);
                    Snowball ball = p.launchProjectile(Snowball.class, p.getEyeLocation().getDirection().multiply(1.6));
                    ball.setItem(new org.bukkit.inventory.ItemStack(Material.TNT));
                    ball.setMetadata("rpg_cluster", new org.bukkit.metadata.FixedMetadataValue(plugin(), lv));
                    repeat(60, 1, () -> { if (!ball.isDead()) particles(ball.getLocation(), Particle.SMOKE, 2, 0, 0, 0, 0); });
                    return true;
                })), "cast.thunder", "rune"));
    }
}
