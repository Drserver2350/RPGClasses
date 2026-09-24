package com.arena.rpgclasses.gui;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.model.RPGClass;
import com.arena.rpgclasses.model.Skill;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Chest GUI for class selection and skill overview. */
public final class ClassGUI implements Listener {

    private final RPGClassesPlugin plugin;

    public ClassGUI(RPGClassesPlugin plugin) { this.plugin = plugin; }

    /** Marker holder so we can identify our inventories. */
    private static final class Holder implements InventoryHolder {
        final String view; final String classId;
        Inventory inv;
        Holder(String view, String classId) { this.view = view; this.classId = classId; }
        @Override public Inventory getInventory() { return inv; }
    }

    // ---------------- item helpers ----------------

    public ItemStack classIcon(RPGClass c, PlayerData d, boolean detailed) {
        ItemStack it = new ItemStack(c.fallbackIcon());
        ItemMeta m = it.getItemMeta();
        m.setItemModel(new NamespacedKey("rpgclasses", c.id()));   // custom model from the resource pack
        m.displayName(Component.text(c.displayName(), c.color(), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(gray("Role: ").append(Component.text(c.roleName(), NamedTextColor.YELLOW)));
        lore.add(Component.empty());
        for (String line : wrap(c.lore(), 38)) lore.add(gray(line));
        lore.add(Component.empty());
        lore.add(Component.text("✦ Passive: ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false).append(Component.text(c.passiveName(), NamedTextColor.WHITE)));
        for (String line : wrap(c.passiveDescription(), 38)) lore.add(gray("  " + line));
        lore.add(Component.empty());
        lore.add(Component.text("Stats: ", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(stat("❤", c.bonusHealth() / 2, false) + " " + stat("⚡", c.bonusSpeed() * 100, true) + " " + stat("⚔", c.bonusDamage() * 100, true) + " " + stat("🛡", c.bonusArmor(), false), NamedTextColor.WHITE)));
        lore.add(Component.empty());
        int lv = d.level(c.id());
        for (int i = 0; i < c.skills().size(); i++) {
            Skill s = c.skills().get(i);
            boolean unlocked = lv >= s.unlockLevel();
            lore.add(Component.text((unlocked ? "◆ " : "◇ ") + s.name(), unlocked ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("  Lv" + s.unlockLevel() + " • " + (int) s.cooldownSeconds() + "s • " + (int) s.manaCost() + " mana", NamedTextColor.DARK_GRAY)));
            if (detailed) for (String line : wrap(s.description(), 38)) lore.add(gray("   " + line));
        }
        lore.add(Component.empty());
        if (d.hasClass() && d.classId().equals(c.id()))
            lore.add(Component.text("▶ CURRENT CLASS  (Level " + lv + ")", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        else {
            if (lv > 1) lore.add(Component.text("Progress saved: Level " + lv, NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to choose", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        m.lore(lore);
        m.addItemFlags(ItemFlag.values());
        if (d.hasClass() && d.classId().equals(c.id())) m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        return it;
    }

    private static String stat(String icon, double v, boolean percent) {
        if (v == 0) return "";
        String s = (v > 0 ? "+" : "") + (percent ? (int) Math.round(v) + "%" : (v == Math.floor(v) ? String.valueOf((int) v) : String.format("%.1f", v)));
        return icon + s;
    }

    private static Component gray(String s) { return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false); }

    private static List<String> wrap(String text, int width) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() + w.length() + 1 > width) { out.add(cur.toString()); cur = new StringBuilder(); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    private ItemStack filler(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.empty());
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    // ---------------- menus ----------------

    public void openMain(Player p) {
        PlayerData d = plugin.data().get(p);
        Holder h = new Holder("main", null);
        Inventory inv = Bukkit.createInventory(h, 54, Component.text("✦ Choose Your Class ✦", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));
        h.inv = inv;
        ItemStack border = filler(Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack inner = filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) inv.setItem(i, (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) ? border : inner);
        List<RPGClass> list = plugin.classes().list();
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};
        for (int i = 0; i < list.size() && i < slots.length; i++) inv.setItem(slots[i], classIcon(list.get(i), d, false));

        // info item
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta im = info.getItemMeta();
        im.displayName(Component.text("How to play", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        im.lore(List.of(
                gray("Sneak + F  → cycle selected skill"),
                gray("Sneak + Right-Click → cast skill"),
                gray("/cast 1|2|3 → cast directly"),
                gray("/skills → detailed skill info"),
                Component.empty(),
                gray("Gain XP by killing mobs & mining."),
                gray("Skills unlock at level 1, 5 and 10."),
                gray("Level 30 is the cap.")));
        info.setItemMeta(im);
        inv.setItem(49, info);
        if (d.hasClass()) {
            RPGClass cur = plugin.classes().get(d.classId());
            ItemStack you = classIcon(cur, d, true);
            inv.setItem(4, you);
        }
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.6f, 1.4f);
    }

    public void openConfirm(Player p, RPGClass c) {
        PlayerData d = plugin.data().get(p);
        Holder h = new Holder("confirm", c.id());
        Inventory inv = Bukkit.createInventory(h, 27, Component.text("Become a " + c.displayName() + "?", c.color(), TextDecoration.BOLD));
        h.inv = inv;
        ItemStack bg = filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);
        inv.setItem(13, classIcon(c, d, true));
        ItemStack yes = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta ym = yes.getItemMeta(); ym.displayName(Component.text("✔ CONFIRM", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)); yes.setItemMeta(ym);
        ItemStack no = new ItemStack(Material.RED_CONCRETE);
        ItemMeta nm = no.getItemMeta(); nm.displayName(Component.text("✖ BACK", NamedTextColor.RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false)); no.setItemMeta(nm);
        inv.setItem(11, yes); inv.setItem(15, no);
        p.openInventory(inv);
    }

    // ---------------- events ----------------

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || it.getType().isAir()) return;

        if (h.view.equals("main")) {
            ItemMeta m = it.getItemMeta();
            if (m == null || m.getItemModel() == null || !m.getItemModel().getNamespace().equals("rpgclasses")) return;
            RPGClass c = plugin.classes().get(m.getItemModel().getKey());
            if (c == null) return;
            PlayerData d = plugin.data().get(p);
            if (d.hasClass() && d.classId().equals(c.id())) { p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f); return; }
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.3f);
            openConfirm(p, c);
        } else if (h.view.equals("confirm")) {
            if (e.getSlot() == 11) {
                RPGClass c = plugin.classes().get(h.classId);
                p.closeInventory();
                plugin.classManager().choose(p, c, false);
            } else if (e.getSlot() == 15) {
                openMain(p);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }
}
