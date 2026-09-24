package com.arena.rpgclasses.manager;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.model.RPGClass;
import com.arena.rpgclasses.model.Skill;
import com.arena.rpgclasses.util.FX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Casting logic: unlock checks, mana, cooldowns, execution. */
public final class SkillManager {

    private final RPGClassesPlugin plugin;

    public SkillManager(RPGClassesPlugin plugin) { this.plugin = plugin; }

    public void cycle(Player p, int dir) {
        PlayerData d = plugin.data().get(p);
        if (!d.hasClass()) return;
        d.setSelectedSkill(d.selectedSkill() + dir);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.6f);
    }

    public boolean cast(Player p, int index) {
        PlayerData d = plugin.data().get(p);
        RPGClass c = plugin.classes().get(d.classId());
        if (c == null) { FX.msg(p, Component.text("Choose a class first with /class", NamedTextColor.RED)); return false; }
        if (index < 0 || index >= c.skills().size()) return false;
        Skill s = c.skills().get(index);

        if (d.level() < s.unlockLevel()) {
            FX.msg(p, Component.text(s.name() + " unlocks at level " + s.unlockLevel() + ".", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return false;
        }
        long cd = d.cooldownRemaining(index);
        if (cd > 0) {
            FX.actionBar(p, Component.text(s.name() + " is on cooldown (" + String.format("%.1f", cd / 1000.0) + "s)", NamedTextColor.RED));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return false;
        }

        boolean useHealth = false;
        if (d.mana() < s.manaCost()) {
            // Warlock Dark Pact: pay with health
            if ("warlock".equals(c.id()) && p.getHealth() > s.manaCost() * 0.15 + 1) useHealth = true;
            else {
                FX.actionBar(p, Component.text("Not enough mana (" + (int) s.manaCost() + " needed)", NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
                return false;
            }
        }

        boolean ok;
        try {
            ok = s.executor().cast(p, d, c, d.level());
        } catch (Exception ex) {
            plugin.getLogger().warning("Skill " + s.name() + " threw: " + ex);
            ex.printStackTrace();
            return false;
        }
        if (!ok) return false;

        if (useHealth) p.setHealth(Math.max(1, p.getHealth() - s.manaCost() * 0.15));
        else d.setMana(d.mana() - s.manaCost());
        // Level slightly reduces cooldown (up to -20% at max level)
        double cdMul = 1.0 - 0.2 * Math.min(1.0, d.level() / (double) plugin.classManager().maxLevel());
        d.setCooldown(index, s.cooldownSeconds() * cdMul);
        d.lastCombatMillis = System.currentTimeMillis();
        return true;
    }
}
