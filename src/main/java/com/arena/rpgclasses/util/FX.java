package com.arena.rpgclasses.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Helper toolbox shared by all skills: targeting, particles, damage, healing, timers.
 */
public final class FX {

    private static Plugin plugin;
    public static void init(Plugin p) { plugin = p; }
    public static Plugin plugin() { return plugin; }

    // ---------- targeting ----------

    public static List<LivingEntity> enemiesNear(Player p, Location center, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (Entity e : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (e instanceof LivingEntity le && isEnemy(p, le)) out.add(le);
        }
        return out;
    }

    public static List<Player> alliesNear(Player p, double radius) {
        List<Player> out = new ArrayList<>();
        for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
            if (e instanceof Player o && !o.isDead()) out.add(o);
        }
        out.add(p);
        return out;
    }

    public static boolean isEnemy(Player p, LivingEntity le) {
        if (le == p || le.isDead() || le.isInvulnerable()) return false;
        if (le instanceof ArmorStand) return false;
        if (le instanceof Player o) return o.getGameMode() != GameMode.SPECTATOR && o.getGameMode() != GameMode.CREATIVE;
        if (le instanceof Tameable t && t.isTamed()) return false;
        return le instanceof Monster || le instanceof Mob || le instanceof Slime;
    }

    public static LivingEntity target(Player p, double range) {
        var r = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range, 0.8,
                e -> e instanceof LivingEntity le && isEnemy(p, le));
        return r == null ? null : (LivingEntity) r.getHitEntity();
    }

    public static List<LivingEntity> cone(Player p, double range, double angleDeg) {
        List<LivingEntity> out = new ArrayList<>();
        Vector dir = p.getEyeLocation().getDirection().normalize();
        double cos = Math.cos(Math.toRadians(angleDeg));
        for (LivingEntity le : enemiesNear(p, p.getLocation(), range)) {
            Vector to = le.getLocation().add(0, 1, 0).subtract(p.getEyeLocation()).toVector();
            if (to.lengthSquared() < 0.01) { out.add(le); continue; }
            if (to.normalize().dot(dir) >= cos) out.add(le);
        }
        return out;
    }

    // ---------- damage / heal ----------

    public static void damage(Player src, LivingEntity target, double amount) {
        if (target.isDead()) return;
        target.setNoDamageTicks(0);
        target.damage(amount, src);
    }

    public static void heal(LivingEntity le, double amount) {
        var attr = le.getAttribute(Attribute.MAX_HEALTH);
        double max = attr == null ? 20 : attr.getValue();
        le.setHealth(Math.min(max, le.getHealth() + amount));
        particles(le.getLocation().add(0, 1, 0), Particle.HEART, 3, 0.4, 0.4, 0.4, 0);
    }

    public static void effect(LivingEntity le, PotionEffectType type, double seconds, int amplifier) {
        le.addPotionEffect(new PotionEffect(type, (int) (seconds * 20), amplifier, false, true, true));
    }

    public static void knockback(Entity from, LivingEntity target, double strength, double up) {
        Vector v = target.getLocation().toVector().subtract(from.getLocation().toVector());
        if (v.lengthSquared() < 0.01) v = new Vector(0, 0, 1);
        target.setVelocity(v.normalize().multiply(strength).setY(up));
    }

    public static void pullTo(Location to, LivingEntity target, double strength) {
        Vector v = to.toVector().subtract(target.getLocation().toVector());
        if (v.lengthSquared() < 0.25) return;
        target.setVelocity(v.normalize().multiply(strength).setY(0.25));
    }

    public static boolean isUndead(LivingEntity le) {
        return le instanceof Zombie || le instanceof AbstractSkeleton || le instanceof Wither || le instanceof Phantom
                || le instanceof Zoglin || le instanceof ZombieHorse || le instanceof SkeletonHorse;
    }

    public static void pull(Entity to, LivingEntity target, double strength) {
        Vector v = to.getLocation().toVector().subtract(target.getLocation().toVector());
        target.setVelocity(v.normalize().multiply(strength).setY(0.3));
    }

    // ---------- particles / sound ----------

    public static void particles(Location loc, Particle p, int count, double dx, double dy, double dz, double speed) {
        loc.getWorld().spawnParticle(p, loc, count, dx, dy, dz, speed);
    }

    public static void dust(Location loc, Color color, float size, int count, double spread) {
        loc.getWorld().spawnParticle(Particle.DUST, loc, count, spread, spread, spread, 0, new Particle.DustOptions(color, size));
    }

    public static void sound(Location loc, Sound s, float vol, float pitch) {
        loc.getWorld().playSound(loc, s, vol, pitch);
    }

    public static void ring(Location center, double radius, Particle p, int points) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            center.getWorld().spawnParticle(p, center.clone().add(radius * Math.cos(a), 0.1, radius * Math.sin(a)), 1, 0, 0, 0, 0);
        }
    }

    public static void ringDust(Location center, double radius, Color c, int points) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            dust(center.clone().add(radius * Math.cos(a), 0.1, radius * Math.sin(a)), c, 1.4f, 1, 0);
        }
    }

    public static void line(Location from, Location to, Particle p, double step) {
        Vector dir = to.toVector().subtract(from.toVector());
        double len = dir.length();
        dir.normalize().multiply(step);
        Location cur = from.clone();
        for (double d = 0; d < len; d += step) {
            cur.add(dir);
            cur.getWorld().spawnParticle(p, cur, 1, 0, 0, 0, 0);
        }
    }

    public static void lineDust(Location from, Location to, Color c, double step) {
        Vector dir = to.toVector().subtract(from.toVector());
        double len = dir.length();
        dir.normalize().multiply(step);
        Location cur = from.clone();
        for (double d = 0; d < len; d += step) {
            cur.add(dir);
            dust(cur, c, 1.2f, 1, 0);
        }
    }

    public static void helix(Location base, double height, Color c, int ticks) {
        final double[] t = {0};
        repeat(ticks, 1, () -> {
            for (int k = 0; k < 2; k++) {
                double a = t[0] * 0.5 + k * Math.PI;
                double y = (t[0] / ticks) * height;
                dust(base.clone().add(0.8 * Math.cos(a), y, 0.8 * Math.sin(a)), c, 1.2f, 1, 0);
            }
            t[0]++;
        });
    }

    // ---------- custom resource-pack assets ----------

    /** Plays a custom sound defined in the pack's sounds.json, e.g. "cast.fire". */
    public static void customSound(Location loc, String name, float vol, float pitch) {
        loc.getWorld().playSound(loc, "rpgclasses:" + name, SoundCategory.PLAYERS, vol, pitch);
    }

    /** Spawns a custom-textured particle (rune, skull, star, leaf, ember, snow) using an item particle. */
    public static void custom(Location loc, String kind, int count, double dx, double dy, double dz, double speed) {
        loc.getWorld().spawnParticle(Particle.ITEM, loc, count, dx, dy, dz, speed,
                com.arena.rpgclasses.RPGClassesPlugin.get().items().particleItem(kind));
    }

    public static void customRing(Location center, double radius, String kind, int points) {
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            custom(center.clone().add(radius * Math.cos(a), 0.2, radius * Math.sin(a)), kind, 1, 0, 0, 0, 0);
        }
    }

    // ---------- scheduling ----------

    public static void later(long ticks, Runnable r) {
        Bukkit.getScheduler().runTaskLater(plugin, r, ticks);
    }

    /** Runs {@code r} every {@code period} ticks, {@code times} times. */
    public static void repeat(int times, long period, Runnable r) {
        final int[] n = {0};
        final int[] id = {-1};
        id[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (n[0]++ >= times) { Bukkit.getScheduler().cancelTask(id[0]); return; }
            r.run();
        }, 0L, period);
    }

    /** Fires a projectile-like ray that travels each tick until it hits something. */
    public static void bolt(Player p, double speed, double maxRange, Consumer<Location> trail, Consumer<LivingEntity> onHit, Runnable onEnd) {
        Location loc = p.getEyeLocation().clone();
        Vector dir = loc.getDirection().normalize().multiply(speed);
        final double[] travelled = {0};
        final int[] id = {-1};
        id[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (int i = 0; i < 2; i++) {
                loc.add(dir);
                travelled[0] += speed;
                trail.accept(loc);
                if (!loc.getBlock().isPassable()) { Bukkit.getScheduler().cancelTask(id[0]); if (onEnd != null) onEnd.run(); return; }
                for (Entity e : loc.getWorld().getNearbyEntities(loc, 1.2, 1.2, 1.2)) {
                    if (e instanceof LivingEntity le && isEnemy(p, le)) {
                        onHit.accept(le);
                        Bukkit.getScheduler().cancelTask(id[0]);
                        if (onEnd != null) onEnd.run();
                        return;
                    }
                }
                if (travelled[0] >= maxRange) { Bukkit.getScheduler().cancelTask(id[0]); if (onEnd != null) onEnd.run(); return; }
            }
        }, 0L, 1L);
    }

    // ---------- text ----------

    public static Component text(String s, TextColor c) { return Component.text(s, c); }
    public static Component gray(String s) { return Component.text(s, NamedTextColor.GRAY); }

    public static void actionBar(Player p, Component c) { p.sendActionBar(c); }

    public static void msg(Player p, Component c) {
        p.sendMessage(Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("RPG", NamedTextColor.GOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(c));
    }

    public static void msg(Player p, String s) { msg(p, Component.text(s, NamedTextColor.GRAY)); }
}
