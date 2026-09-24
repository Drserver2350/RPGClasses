package com.arena.rpgclasses.manager;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.model.RPGClass;
import com.arena.rpgclasses.model.Skill;
import com.arena.rpgclasses.util.FX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.time.Duration;

/** Applies class stats/attributes, handles leveling & XP. */
public final class ClassManager {

    private final RPGClassesPlugin plugin;
    private final NamespacedKey keyHealth, keySpeed, keyDamage, keyArmor, keyKb;

    public ClassManager(RPGClassesPlugin plugin) {
        this.plugin = plugin;
        keyHealth = new NamespacedKey(plugin, "class_health");
        keySpeed = new NamespacedKey(plugin, "class_speed");
        keyDamage = new NamespacedKey(plugin, "class_damage");
        keyArmor = new NamespacedKey(plugin, "class_armor");
        keyKb = new NamespacedKey(plugin, "class_kb");
    }

    // ---------------- stats ----------------

    public void applyStats(Player p) {
        PlayerData d = plugin.data().get(p);
        RPGClass c = plugin.classes().get(d.classId());
        clearStats(p);
        if (c == null) return;
        int lv = d.level();
        double lvHealth = Math.floor(lv / 5.0) * 2; // +1 heart every 5 levels
        set(p, Attribute.MAX_HEALTH, keyHealth, c.bonusHealth() + lvHealth, AttributeModifier.Operation.ADD_NUMBER);
        set(p, Attribute.MOVEMENT_SPEED, keySpeed, c.bonusSpeed(), AttributeModifier.Operation.ADD_SCALAR);
        set(p, Attribute.ATTACK_DAMAGE, keyDamage, c.bonusDamage() + lv * 0.005, AttributeModifier.Operation.ADD_SCALAR);
        set(p, Attribute.ARMOR, keyArmor, c.bonusArmor(), AttributeModifier.Operation.ADD_NUMBER);
        if (c.id().equals("guardian")) set(p, Attribute.KNOCKBACK_RESISTANCE, keyKb, 0.3, AttributeModifier.Operation.ADD_NUMBER);
        if (c.id().equals("artificer")) set(p, Attribute.MINING_EFFICIENCY, keyKb, 0.2, AttributeModifier.Operation.ADD_SCALAR);
        var mh = p.getAttribute(Attribute.MAX_HEALTH);
        if (mh != null && p.getHealth() > mh.getValue()) p.setHealth(mh.getValue());
    }

    public void clearStats(Player p) {
        for (Attribute a : new Attribute[]{Attribute.MAX_HEALTH, Attribute.MOVEMENT_SPEED, Attribute.ATTACK_DAMAGE, Attribute.ARMOR, Attribute.KNOCKBACK_RESISTANCE, Attribute.MINING_EFFICIENCY}) {
            AttributeInstance inst = p.getAttribute(a);
            if (inst == null) continue;
            for (AttributeModifier m : inst.getModifiers().toArray(new AttributeModifier[0]))
                if (m.getKey().getNamespace().equals(plugin.getName().toLowerCase())) inst.removeModifier(m);
        }
    }

    private void set(Player p, Attribute a, NamespacedKey key, double amount, AttributeModifier.Operation op) {
        if (amount == 0) return;
        AttributeInstance inst = p.getAttribute(a);
        if (inst == null) return;
        inst.removeModifier(key);
        inst.addModifier(new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY));
    }

    // ---------------- class selection ----------------

    public boolean choose(Player p, RPGClass c, boolean force) {
        PlayerData d = plugin.data().get(p);
        if (!force && d.hasClass()) {
            long cd = plugin.getConfig().getLong("class-change.reset-cooldown-seconds") * 1000L;
            long left = d.lastClassChange() + cd - System.currentTimeMillis();
            if (!plugin.getConfig().getBoolean("class-change.allow-reset")) { FX.msg(p, Component.text("Class changes are disabled.", NamedTextColor.RED)); return false; }
            if (left > 0) { FX.msg(p, Component.text("You can change class again in " + (left / 1000) + "s.", NamedTextColor.RED)); return false; }
        }
        d.setClassId(c.id());
        d.setLastClassChange(System.currentTimeMillis());
        d.clearCooldowns();
        applyStats(p);
        d.setMana(plugin.mana().maxMana(p, d));
        plugin.data().save(d);
        plugin.items().giveKit(p, c);

        p.showTitle(Title.title(
                Component.text(c.displayName(), c.color(), TextDecoration.BOLD),
                Component.text(c.roleName() + "  •  " + c.passiveName(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700))));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
        FX.helix(p.getLocation(), 2.5, org.bukkit.Color.fromRGB(c.color().value()), 40);
        FX.msg(p, Component.text("You are now a ", NamedTextColor.GRAY).append(Component.text(c.displayName(), c.color(), TextDecoration.BOLD))
                .append(Component.text("! You received your Power Orb and weapon. Right-Click them to cast, press F to switch skills. /skills for details.", NamedTextColor.GRAY)));
        return true;
    }

    // ---------------- leveling ----------------

    public int maxLevel() { return plugin.getConfig().getInt("leveling.max-level", 30); }

    public double xpForNext(int level) {
        return plugin.getConfig().getDouble("leveling.base-xp", 100) + level * plugin.getConfig().getDouble("leveling.per-level-xp", 45);
    }

    public void addXp(Player p, double amount) {
        PlayerData d = plugin.data().get(p);
        if (!d.hasClass() || amount <= 0) return;
        if ("bard".equals(d.classId())) amount *= 1.25;
        int lv = d.level();
        if (lv >= maxLevel()) return;
        double xp = d.xp() + amount;
        boolean leveled = false;
        while (lv < maxLevel() && xp >= xpForNext(lv)) {
            xp -= xpForNext(lv);
            lv++;
            leveled = true;
        }
        d.setLevel(d.classId(), lv);
        d.setXp(d.classId(), lv >= maxLevel() ? 0 : xp);
        if (leveled) onLevelUp(p, d, lv);
    }

    private void onLevelUp(Player p, PlayerData d, int lv) {
        RPGClass c = plugin.classes().get(d.classId());
        applyStats(p);
        d.setMana(plugin.mana().maxMana(p, d));
        FX.customSound(p.getLocation(), "levelup", 1f, 1f);
        FX.customRing(p.getLocation(), 1.5, c.particle(), 16);
        FX.particles(p.getLocation().add(0, 1, 0), Particle.TOTEM_OF_UNDYING, 60, 0.5, 0.8, 0.5, 0.3);
        p.showTitle(Title.title(
                Component.text("LEVEL UP", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text(c.displayName() + " Level " + lv, c.color()),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))));
        for (int i = 0; i < c.skills().size(); i++) {
            Skill s = c.skills().get(i);
            if (s.unlockLevel() == lv)
                FX.msg(p, Component.text("New skill unlocked: ", NamedTextColor.GREEN).append(Component.text(s.name(), NamedTextColor.AQUA, TextDecoration.BOLD)).append(Component.text(" (slot " + (i + 1) + ")", NamedTextColor.GRAY)));
        }
        plugin.data().save(d);
    }
}
