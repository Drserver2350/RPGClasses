package com.arena.rpgclasses.command;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.model.RPGClass;
import com.arena.rpgclasses.model.Skill;
import com.arena.rpgclasses.util.FX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Commands implements TabExecutor {

    private final RPGClassesPlugin plugin;

    public Commands(RPGClassesPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);
        switch (name) {
            case "class" -> classCmd(sender, a);
            case "cast" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                int idx = a.length > 0 ? parse(a[0], 1) - 1 : plugin.data().get(p).selectedSkill();
                plugin.skills().cast(p, idx);
            }
            case "skills" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                skills(p);
            }
            case "rpgadmin" -> admin(sender, a);
        }
        return true;
    }

    private void classCmd(CommandSender sender, String[] a) {
        if (a.length == 0 || a[0].equalsIgnoreCase("menu")) {
            if (sender instanceof Player p) plugin.gui().openMain(p); else sender.sendMessage("Players only.");
            return;
        }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "list" -> {
                sender.sendMessage(Component.text("Available classes (" + plugin.classes().all().size() + "):", NamedTextColor.GOLD));
                for (RPGClass c : plugin.classes().all())
                    sender.sendMessage(Component.text(" • ", NamedTextColor.DARK_GRAY).append(Component.text(c.displayName(), c.color())).append(Component.text(" - " + c.roleName(), NamedTextColor.GRAY)));
            }
            case "info" -> {
                if (!(sender instanceof Player p)) return;
                PlayerData d = plugin.data().get(p);
                RPGClass c = plugin.classes().get(d.classId());
                if (c == null) { FX.msg(p, "You have no class yet. Use /class to choose one."); return; }
                FX.msg(p, Component.text(c.displayName(), c.color(), TextDecoration.BOLD).append(Component.text("  Level " + d.level() + "  XP " + (int) d.xp() + "/" + (int) plugin.classManager().xpForNext(d.level()), NamedTextColor.GRAY)));
                FX.msg(p, Component.text("Passive: ", NamedTextColor.LIGHT_PURPLE).append(Component.text(c.passiveName() + " - " + c.passiveDescription(), NamedTextColor.GRAY)));
            }
            case "reset" -> {
                if (!(sender instanceof Player p)) return;
                if (!plugin.getConfig().getBoolean("class-change.allow-reset")) { FX.msg(p, Component.text("Class changes are disabled.", NamedTextColor.RED)); return; }
                plugin.gui().openMain(p);
            }
            case "set" -> {
                if (!sender.hasPermission("rpgclasses.admin")) { sender.sendMessage(Component.text("No permission.", NamedTextColor.RED)); return; }
                if (a.length < 3) { sender.sendMessage("/class set <player> <class>"); return; }
                Player t = Bukkit.getPlayerExact(a[1]);
                RPGClass c = plugin.classes().get(a[2]);
                if (t == null || c == null) { sender.sendMessage(Component.text("Unknown player or class.", NamedTextColor.RED)); return; }
                plugin.classManager().choose(t, c, true);
                sender.sendMessage(Component.text("Set " + t.getName() + " to " + c.displayName(), NamedTextColor.GREEN));
            }
            default -> {
                // /class <classname> shortcut
                RPGClass c = plugin.classes().get(a[0]);
                if (c != null && sender instanceof Player p) plugin.gui().openConfirm(p, c);
                else sender.sendMessage(Component.text("Usage: /class [menu|info|list|reset|set]", NamedTextColor.RED));
            }
        }
    }

    private void skills(Player p) {
        PlayerData d = plugin.data().get(p);
        RPGClass c = plugin.classes().get(d.classId());
        if (c == null) { FX.msg(p, "Choose a class first with /class"); return; }
        p.sendMessage(Component.text("━━━━━ ", NamedTextColor.DARK_GRAY).append(Component.text(c.displayName() + " Skills", c.color(), TextDecoration.BOLD)).append(Component.text(" ━━━━━", NamedTextColor.DARK_GRAY)));
        p.sendMessage(Component.text("✦ Passive: ", NamedTextColor.LIGHT_PURPLE).append(Component.text(c.passiveName(), NamedTextColor.WHITE, TextDecoration.BOLD)).append(Component.text(" - " + c.passiveDescription(), NamedTextColor.GRAY)));
        for (int i = 0; i < c.skills().size(); i++) {
            Skill s = c.skills().get(i);
            boolean un = d.level() >= s.unlockLevel();
            p.sendMessage(Component.text("[" + (i + 1) + "] ", NamedTextColor.GRAY)
                    .append(Component.text(s.name(), un ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY, TextDecoration.BOLD))
                    .append(Component.text(un ? "" : "  (unlocks Lv" + s.unlockLevel() + ")", NamedTextColor.RED))
                    .append(Component.text("  " + (int) s.cooldownSeconds() + "s cd • " + (int) s.manaCost() + " mana", NamedTextColor.DARK_AQUA)));
            p.sendMessage(Component.text("     " + s.description(), NamedTextColor.GRAY));
        }
        p.sendMessage(Component.text("Sneak + F to cycle, Sneak + Right-Click to cast, or /cast <1-3>", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC));
    }

    private void admin(CommandSender s, String[] a) {
        if (a.length == 0) { s.sendMessage("/rpgadmin <reload|setlevel <player> <lvl>|addxp <player> <xp>|pack <player>|packinfo>"); return; }
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> { plugin.reloadConfig(); s.sendMessage(Component.text("RPGClasses config reloaded.", NamedTextColor.GREEN)); }
            case "setlevel" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null || a.length < 3) { s.sendMessage("/rpgadmin setlevel <player> <level>"); return; }
                PlayerData d = plugin.data().get(t);
                if (!d.hasClass()) { s.sendMessage("Player has no class."); return; }
                d.setLevel(d.classId(), Math.min(plugin.classManager().maxLevel(), parse(a[2], 1)));
                d.setXp(d.classId(), 0);
                plugin.classManager().applyStats(t);
                plugin.data().save(d);
                s.sendMessage(Component.text(t.getName() + " is now level " + d.level(), NamedTextColor.GREEN));
            }
            case "addxp" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : null;
                if (t == null || a.length < 3) { s.sendMessage("/rpgadmin addxp <player> <xp>"); return; }
                plugin.classManager().addXp(t, parse(a[2], 0));
                s.sendMessage(Component.text("Added XP.", NamedTextColor.GREEN));
            }
            case "pack" -> {
                Player t = a.length > 1 ? Bukkit.getPlayerExact(a[1]) : (s instanceof Player p ? p : null);
                if (t == null) { s.sendMessage("Player not found."); return; }
                plugin.pack().send(t);
                s.sendMessage(Component.text("Resource pack re-sent to " + t.getName(), NamedTextColor.GREEN));
            }
            case "packinfo" -> s.sendMessage(Component.text("Pack URL: " + plugin.pack().url() + "\nSHA-1: " + plugin.pack().sha1Hex(), NamedTextColor.AQUA));
            default -> s.sendMessage("Unknown subcommand.");
        }
    }

    private int parse(String s, int def) { try { return Integer.parseInt(s); } catch (Exception e) { return def; } }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] a) {
        List<String> out = new ArrayList<>();
        String n = cmd.getName().toLowerCase(Locale.ROOT);
        if (n.equals("class")) {
            if (a.length == 1) { out.addAll(List.of("menu", "info", "list", "reset")); if (sender.hasPermission("rpgclasses.admin")) out.add("set"); plugin.classes().all().forEach(c -> out.add(c.id())); }
            else if (a.length == 2 && a[0].equalsIgnoreCase("set")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            else if (a.length == 3 && a[0].equalsIgnoreCase("set")) plugin.classes().all().forEach(c -> out.add(c.id()));
        } else if (n.equals("cast")) { if (a.length == 1) out.addAll(List.of("1", "2", "3")); }
        else if (n.equals("rpgadmin")) {
            if (a.length == 1) out.addAll(List.of("reload", "setlevel", "addxp", "pack", "packinfo"));
            else if (a.length == 2) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
        }
        String last = a.length > 0 ? a[a.length - 1].toLowerCase(Locale.ROOT) : "";
        out.removeIf(x -> !x.toLowerCase(Locale.ROOT).startsWith(last));
        return out;
    }
}
