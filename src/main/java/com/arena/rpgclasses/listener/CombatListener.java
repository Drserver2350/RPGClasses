package com.arena.rpgclasses.listener;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.util.FX;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Random;

/** Class passives, XP gain and misc combat hooks. */
public final class CombatListener implements Listener {

    private final RPGClassesPlugin plugin;
    private final Random rng = new Random();

    public CombatListener(RPGClassesPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::passiveTick, 20L, 20L);
    }

    private String cls(Player p) { return plugin.data().get(p).classId(); }

    // ------------------------------------------------------------ periodic passives
    private void passiveTick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String c = cls(p);
            if (c == null) continue;
            switch (c) {
                case "druid" -> {
                    Material under = p.getLocation().subtract(0, 0.2, 0).getBlock().getType();
                    if (under == Material.GRASS_BLOCK || Tag.LEAVES.isTagged(under) || under == Material.MOSS_BLOCK) FX.heal(p, 0.5);
                }
                case "cleric" -> {
                    for (var e : FX.enemiesNear(p, p.getLocation(), 6)) if (FX.isUndead(e)) { FX.damage(p, e, 1.0); FX.particles(e.getLocation().add(0, 1, 0), Particle.END_ROD, 3, 0.2, 0.4, 0.2, 0.01); }
                }
                case "bard" -> {
                    for (Player a : FX.alliesNear(p, 10)) if (a != p) FX.effect(a, PotionEffectType.SPEED, 2.5, 0);
                }
                case "cryomancer" -> { if (p.getFreezeTicks() > 0) p.setFreezeTicks(0); }
                case "shaman" -> {
                    int allies = FX.alliesNear(p, 10).size() - 1;
                    if (allies > 0) FX.effect(p, PotionEffectType.ABSORPTION, 2.5, Math.min(3, allies) - 1);
                }
                default -> {}
            }
        }
    }

    // ------------------------------------------------------------ damage dealt
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity target)) return;
        Player p = attacker(e.getDamager());
        if (p == null) return;
        PlayerData d = plugin.data().get(p);
        String c = d.classId();
        if (c == null) return;
        boolean melee = e.getDamager() == p;
        boolean arrow = e.getDamager() instanceof AbstractArrow;
        double mul = 1.0;
        d.lastCombatMillis = System.currentTimeMillis();

        // Death Mark (assassin ultimate) applies to all damage
        if (target.hasMetadata("rpg_marked") && target.getMetadata("rpg_marked").get(0).asLong() > System.currentTimeMillis()) mul += 0.4;

        switch (c) {
            case "warrior" -> { if (melee && ++d.comboCounter % 4 == 0) { FX.effect(target, PotionEffectType.SLOWNESS, 1.2, 3); FX.particles(target.getLocation().add(0, 2, 0), Particle.CRIT, 10, 0.2, 0.2, 0.2, 0.1); } }
            case "paladin" -> {
                if (FX.isUndead(target)) mul += 0.25;
                if (d.hasFlag("holystrike") && melee) { d.flag("holystrike", 0); mul += 1.0; FX.particles(target.getLocation().add(0, 1, 0), Particle.END_ROD, 30, 0.4, 0.6, 0.4, 0.1); FX.sound(target.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1.8f); for (var x : FX.enemiesNear(p, target.getLocation(), 3)) if (x != target) FX.damage(p, x, 4 + d.level() * 0.3); }
                if (melee) FX.later(1L, () -> FX.heal(p, e.getFinalDamage() * 0.08));
            }
            case "berserker" -> { double frac = p.getHealth() / p.getAttribute(Attribute.MAX_HEALTH).getValue(); mul += 0.6 * (1 - frac); }
            case "ranger" -> { if (arrow) { mul += 0.25; if (e.getDamager().getLocation().getY() > target.getEyeLocation().getY() - 0.35) { mul += 1.0; FX.sound(p.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.5f); FX.particles(target.getEyeLocation(), Particle.CRIT, 15, 0.2, 0.2, 0.2, 0.2); } } }
            case "assassin" -> {
                if (melee) {
                    var toTarget = target.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0).normalize();
                    boolean behind = toTarget.dot(target.getLocation().getDirection().setY(0).normalize()) > 0.4;
                    if (behind || d.hasFlag("shadowstep")) { mul += 0.75; FX.particles(target.getLocation().add(0, 1.2, 0), Particle.DAMAGE_INDICATOR, 8, 0.2, 0.2, 0.2, 0.2); FX.sound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.7f); }
                }
            }
            case "samurai" -> {
                if (melee) {
                    long now = System.currentTimeMillis();
                    if (now - d.tempFlagUntil > 0 && d.tempFlag != null && d.tempFlag.equals("combo")) d.comboCounter = 0;
                    if (now - d.lastCombatMillis > 2000) d.comboCounter = 0;
                    d.comboCounter = Math.min(5, d.comboCounter + 1);
                    mul += 0.08 * d.comboCounter;
                    d.flag("combo", 2000);
                    if (d.comboCounter == 5) FX.particles(p.getLocation().add(0, 1, 0), Particle.FLASH, 1, 0, 0, 0, 0);
                }
            }
            case "pyromancer" -> { if (melee || arrow) target.setFireTicks(Math.max(target.getFireTicks(), 60)); }
            case "cryomancer" -> { if (melee || arrow) { FX.effect(target, PotionEffectType.SLOWNESS, 2, 1); target.setFreezeTicks(Math.min(140, target.getFreezeTicks() + 40)); } }
            case "stormcaller" -> { if ((melee || arrow) && ++d.comboCounter % 5 == 0) { target.getWorld().strikeLightningEffect(target.getLocation()); FX.damage(p, target, 4 + d.level() * 0.2); for (var x : FX.enemiesNear(p, target.getLocation(), 2.5)) if (x != target) FX.damage(p, x, 3); } }
            case "warlock" -> { if (p.getHealth() < p.getAttribute(Attribute.MAX_HEALTH).getValue() / 2) mul += 0.2; }
            case "monk" -> { if (melee && p.getInventory().getItemInMainHand().getType().isAir()) e.setDamage(e.getDamage() + 6); }
            default -> {}
        }
        if (mul != 1.0) e.setDamage(e.getDamage() * mul);
    }

    private Player attacker(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    // ------------------------------------------------------------ damage taken
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        PlayerData d = plugin.data().get(p);
        String c = d.classId();
        if (c == null) return;
        var cause = e.getCause();

        // no-fall flags (Stormcaller Gale Leap, Monk, Artificer Grapple, Chi Burst)
        if (cause == EntityDamageEvent.DamageCause.FALL && (c.equals("monk") || d.hasFlag("nofall"))) { e.setCancelled(true); return; }

        switch (c) {
            case "guardian" -> {
                e.setDamage(e.getDamage() * 0.85);
                if (p.isBlocking() && e instanceof EntityDamageByEntityEvent be && be.getDamager() instanceof LivingEntity le) { FX.damage(p, le, e.getDamage() * 0.2); FX.sound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.2f); }
            }
            case "pyromancer" -> { if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK || cause == EntityDamageEvent.DamageCause.LAVA || cause == EntityDamageEvent.DamageCause.HOT_FLOOR) { e.setCancelled(true); p.setFireTicks(0); } }
            case "cryomancer" -> { if (cause == EntityDamageEvent.DamageCause.FREEZE) e.setCancelled(true); }
            case "stormcaller" -> { if (cause == EntityDamageEvent.DamageCause.LIGHTNING) e.setCancelled(true); }
            case "alchemist" -> { if (cause == EntityDamageEvent.DamageCause.POISON) e.setCancelled(true); }
            case "monk" -> { if (e instanceof EntityDamageByEntityEvent && rng.nextDouble() < 0.15) { e.setCancelled(true); FX.particles(p.getLocation().add(0, 1, 0), Particle.CLOUD, 10, 0.3, 0.4, 0.3, 0.02); FX.sound(p.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.5f, 1.8f); FX.actionBar(p, FX.text("Dodged!", net.kyori.adventure.text.format.NamedTextColor.AQUA)); } }
            case "samurai" -> {
                if (d.hasFlag("parry") && e instanceof EntityDamageByEntityEvent be) {
                    e.setCancelled(true); d.flag("parry", 0);
                    FX.sound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1.8f); FX.sound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
                    FX.particles(p.getLocation().add(0, 1.2, 0), Particle.FLASH, 1, 0, 0, 0, 0);
                    Entity src = be.getDamager() instanceof Projectile pr && pr.getShooter() instanceof LivingEntity s ? s : be.getDamager();
                    if (src instanceof LivingEntity le && FX.isEnemy(p, le)) { FX.damage(p, le, e.getDamage() * 2 + d.level() * 0.3); FX.knockback(p, le, 0.8, 0.3); }
                }
            }
            case "shaman" -> { if (e instanceof EntityDamageByEntityEvent) { double share = e.getDamage() * 0.1; for (var x : FX.enemiesNear(p, p.getLocation(), 6)) FX.damage(p, x, share); } }
            default -> {}
        }
    }

    // ------------------------------------------------------------ kills & XP
    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Player k = e.getEntity().getKiller();
        if (k == null || e.getEntity() == k) return;
        PlayerData d = plugin.data().get(k);
        String c = d.classId();
        if (c == null) return;
        double xp;
        if (e.getEntity() instanceof Player) xp = plugin.getConfig().getDouble("leveling.xp-per-player-kill", 120);
        else {
            var mh = e.getEntity().getAttribute(Attribute.MAX_HEALTH);
            xp = (mh == null ? 10 : mh.getValue()) * plugin.getConfig().getDouble("leveling.xp-per-mob-health", 2.0);
            if (e.getEntity() instanceof Boss) xp *= 5;
            if (e.getEntity().hasMetadata("rpg_minion")) xp = 0;
        }
        plugin.classManager().addXp(k, xp);

        switch (c) {
            case "berserker" -> FX.effect(k, PotionEffectType.SPEED, 4, 1);
            case "necromancer" -> { d.setMana(Math.min(plugin.mana().maxMana(k, d), d.mana() + 20)); FX.heal(k, 4); FX.particles(e.getEntity().getLocation().add(0, 1, 0), Particle.SOUL, 15, 0.3, 0.4, 0.3, 0.03); }
            case "assassin" -> { if (k.isSneaking()) d.flag("shadowstep", 3000); }
            default -> {}
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        var t = e.getBlock().getType();
        if (Tag.BASE_STONE_OVERWORLD.isTagged(t) || t.name().endsWith("_ORE") || Tag.LOGS.isTagged(t) || t == Material.ANCIENT_DEBRIS)
            plugin.classManager().addXp(e.getPlayer(), plugin.getConfig().getDouble("leveling.xp-per-block-broken", 0.25) * (t.name().endsWith("_ORE") ? 8 : 1));
    }

    // ------------------------------------------------------------ targeting rules
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!(e.getTarget() instanceof Player p)) return;
        String c = cls(p);
        // minions never attack players
        if (e.getEntity().hasMetadata("rpg_minion")) { e.setCancelled(true); return; }
        if ("necromancer".equals(c) && e.getEntity() instanceof LivingEntity le && FX.isUndead(le)) e.setCancelled(true);
        if ("assassin".equals(c) && plugin.data().get(p).hasFlag("shadowstep") && p.isSneaking()) e.setCancelled(true);
        if ("druid".equals(c) && e.getEntity() instanceof Animals) e.setCancelled(true);
    }

    // ------------------------------------------------------------ item durability (artificer)
    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        if ("artificer".equals(cls(e.getPlayer())) && rng.nextBoolean()) e.setCancelled(true);
    }

    // ------------------------------------------------------------ cluster bomb
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball ball) || !ball.hasMetadata("rpg_cluster")) return;
        ProjectileSource src = ball.getShooter();
        if (!(src instanceof Player p)) return;
        int lv = ball.getMetadata("rpg_cluster").get(0).asInt();
        Location center = ball.getLocation();
        explode(p, center, 6 + lv * 0.35, 3);
        for (int i = 0; i < 4; i++) {
            Location sub = center.clone().add((rng.nextDouble() - 0.5) * 6, 0.5, (rng.nextDouble() - 0.5) * 6);
            FX.later(5L + i * 4L, () -> explode(p, sub, 4 + lv * 0.25, 2.5));
        }
    }

    private void explode(Player p, Location l, double dmg, double radius) {
        FX.particles(l, Particle.EXPLOSION, 1, 0, 0, 0, 0);
        FX.particles(l, Particle.FLAME, 20, 0.5, 0.5, 0.5, 0.05);
        FX.sound(l, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.3f);
        for (var e : FX.enemiesNear(p, l, radius)) { FX.damage(p, e, dmg); FX.knockback(p, e, 0.8, 0.5); }
    }
}
