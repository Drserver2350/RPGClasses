package com.arena.rpgclasses.manager;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.model.RPGClass;
import com.arena.rpgclasses.model.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Mana regeneration + action-bar HUD, ticking twice per second. */
public final class ManaManager {

    private final RPGClassesPlugin plugin;
    private BukkitTask task;

    public ManaManager(RPGClassesPlugin plugin) { this.plugin = plugin; }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() { if (task != null) task.cancel(); }

    public double maxMana(Player p, PlayerData d) {
        double base = plugin.getConfig().getDouble("mana.base", 100) + d.level() * plugin.getConfig().getDouble("mana.per-level", 5);
        if ("mage".equals(d.classId())) base *= 1.5;
        return base;
    }

    public double regenPerSecond(PlayerData d) {
        double r = plugin.getConfig().getDouble("mana.regen-per-second", 4);
        if ("mage".equals(d.classId())) r *= 1.5;
        return r;
    }

    private void tick() {
        boolean hud = plugin.getConfig().getBoolean("casting.hud", true);
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = plugin.data().get(p);
            if (!d.hasClass()) continue;
            double max = maxMana(p, d);
            d.setMana(Math.min(max, d.mana() + regenPerSecond(d) / 2.0));
            if (hud) p.sendActionBar(hud(p, d, max));
        }
    }

    private Component hud(Player p, PlayerData d, double maxMana) {
        RPGClass c = plugin.classes().get(d.classId());
        int lv = d.level();
        double need = plugin.classManager().xpForNext(lv);
        double xpFrac = lv >= plugin.classManager().maxLevel() ? 1 : Math.min(1, d.xp() / need);
        double manaFrac = Math.min(1, d.mana() / maxMana);

        Component out = Component.text(c.displayName(), c.color(), TextDecoration.BOLD)
                .append(Component.text(" Lv" + lv + " ", NamedTextColor.WHITE))
                .append(bar(xpFrac, 10, NamedTextColor.GREEN, NamedTextColor.DARK_GRAY))
                .append(Component.text("  ✦ ", TextColor.color(0x55AAFF)))
                .append(bar(manaFrac, 10, TextColor.color(0x55AAFF), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + (int) d.mana() + "/" + (int) maxMana + "  ", TextColor.color(0x55AAFF)));

        Skill s = c.skills().get(d.selectedSkill());
        long cd = d.cooldownRemaining(d.selectedSkill());
        boolean locked = lv < s.unlockLevel();
        Component skill;
        if (locked) skill = Component.text("🔒 " + s.name() + " (Lv" + s.unlockLevel() + ")", NamedTextColor.DARK_GRAY);
        else if (cd > 0) skill = Component.text("⌛ " + s.name() + " " + String.format("%.1fs", cd / 1000.0), NamedTextColor.RED);
        else skill = Component.text("➤ " + s.name(), NamedTextColor.GOLD, TextDecoration.BOLD);
        return out.append(Component.text("[" + (d.selectedSkill() + 1) + "] ", NamedTextColor.GRAY)).append(skill);
    }

    private Component bar(double frac, int len, TextColor on, TextColor off) {
        int n = (int) Math.round(frac * len);
        return Component.text("▮".repeat(Math.max(0, n)), on).append(Component.text("▯".repeat(Math.max(0, len - n)), off));
    }
}
