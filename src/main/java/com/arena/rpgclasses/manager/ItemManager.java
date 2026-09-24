package com.arena.rpgclasses.manager;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.RPGClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/**
 * Creates and identifies the custom class items: the Power Orb (casting item) and the
 * signature class weapon. Both use custom models from the bundled resource pack.
 */
public final class ItemManager {

    public static final String NS = "rpgclasses";

    /** weapon model, base material, display name, damage, attack speed */
    public record WeaponDef(String model, Material base, String name, double damage, double speed) {}

    private static final Map<String, WeaponDef> WEAPONS = Map.ofEntries(
            Map.entry("warrior",     new WeaponDef("longsword", Material.IRON_SWORD,    "Vanguard Longsword", 7, -2.4)),
            Map.entry("paladin",     new WeaponDef("longsword", Material.GOLDEN_SWORD,  "Blade of Dawn", 6.5, -2.4)),
            Map.entry("guardian",    new WeaponDef("warhammer", Material.IRON_AXE,      "Bulwark Maul", 8, -3.0)),
            Map.entry("samurai",     new WeaponDef("katana",    Material.DIAMOND_SWORD, "Masamune Katana", 6.5, -1.8)),
            Map.entry("berserker",   new WeaponDef("greataxe",  Material.DIAMOND_AXE,   "Bloodreaver Greataxe", 10, -3.2)),
            Map.entry("ranger",      new WeaponDef("longbow",   Material.BOW,           "Hunter's Longbow", 0, 0)),
            Map.entry("assassin",    new WeaponDef("dagger",    Material.NETHERITE_SWORD,"Nightfang Dagger", 5, -1.2)),
            Map.entry("alchemist",   new WeaponDef("dagger",    Material.IRON_SWORD,    "Vivisection Scalpel", 4.5, -1.4)),
            Map.entry("mage",        new WeaponDef("staff",     Material.WOODEN_SWORD,  "Arcane Staff", 4, -2.6)),
            Map.entry("pyromancer",  new WeaponDef("staff",     Material.WOODEN_SWORD,  "Cinder Staff", 4, -2.6)),
            Map.entry("cryomancer",  new WeaponDef("staff",     Material.WOODEN_SWORD,  "Glacier Staff", 4, -2.6)),
            Map.entry("stormcaller", new WeaponDef("staff",     Material.WOODEN_SWORD,  "Tempest Rod", 4, -2.6)),
            Map.entry("necromancer", new WeaponDef("staff",     Material.WOODEN_SWORD,  "Gravebound Staff", 4, -2.6)),
            Map.entry("warlock",     new WeaponDef("staff",     Material.WOODEN_SWORD,  "Voidcaller Staff", 4, -2.6)),
            Map.entry("druid",       new WeaponDef("staff",     Material.WOODEN_SWORD,  "Wildwood Staff", 4.5, -2.6)),
            Map.entry("cleric",      new WeaponDef("warhammer", Material.GOLDEN_AXE,    "Hallowed Hammer", 6, -3.0)),
            Map.entry("shaman",      new WeaponDef("staff",     Material.WOODEN_SWORD,  "Spirit Totem Staff", 4.5, -2.6)),
            Map.entry("bard",        new WeaponDef("lute",      Material.WOODEN_SWORD,  "Lute of Legends", 3, -2.2)),
            Map.entry("monk",        new WeaponDef("staff",     Material.STICK,         "Bo Staff", 5, -1.6)),
            Map.entry("artificer",   new WeaponDef("warhammer", Material.IRON_AXE,      "Piston Hammer", 7, -3.0))
    );

    private final RPGClassesPlugin plugin;
    private final NamespacedKey keyType, keyClass;

    public ItemManager(RPGClassesPlugin plugin) {
        this.plugin = plugin;
        keyType = new NamespacedKey(plugin, "item_type");
        keyClass = new NamespacedKey(plugin, "item_class");
    }

    // ------------------------------------------------------------ creation

    public ItemStack powerOrb(RPGClass c) {
        ItemStack it = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta m = it.getItemMeta();
        m.setItemModel(new NamespacedKey(NS, "orb_" + c.id()));
        m.displayName(Component.text("✦ ", NamedTextColor.WHITE).append(Component.text(c.displayName() + " Power Orb", c.color(), TextDecoration.BOLD)).decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                gray("Channel your class powers through this orb."),
                Component.empty(),
                Component.text("Right-Click ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(gray("→ cast selected skill")),
                Component.text("Press F ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(gray("(swap hands) → next skill")),
                Component.text("Sneak + F ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(gray("→ previous skill")),
                Component.empty(),
                Component.text("Soulbound • cannot be dropped, kept on death", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, true)));
        m.setEnchantmentGlintOverride(true);
        m.addItemFlags(ItemFlag.values());
        m.setMaxStackSize(1);
        m.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, "orb");
        m.getPersistentDataContainer().set(keyClass, PersistentDataType.STRING, c.id());
        it.setItemMeta(m);
        return it;
    }

    public ItemStack weapon(RPGClass c) {
        WeaponDef w = WEAPONS.get(c.id());
        if (w == null) return null;
        ItemStack it = new ItemStack(w.base());
        ItemMeta m = it.getItemMeta();
        m.setItemModel(new NamespacedKey(NS, "weapon_" + w.model()));
        m.displayName(Component.text(w.name(), c.color(), TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(
                gray(c.displayName() + " signature weapon"),
                Component.empty(),
                w.base() == Material.BOW ? gray("Draw to shoot • use the orb to cast") : Component.text("Right-Click ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(gray("→ cast selected skill")),
                Component.text("Press F ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(gray("→ next skill")),
                Component.empty(),
                Component.text("Unbreakable • Soulbound", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, true)));
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        if (w.base() != Material.BOW) {
            m.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(new NamespacedKey(plugin, "weapon_damage"), w.damage(), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            m.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(new NamespacedKey(plugin, "weapon_speed"), w.speed(), AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
            m.lore(appendStats(m.lore(), w));
        }
        m.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, "weapon");
        m.getPersistentDataContainer().set(keyClass, PersistentDataType.STRING, c.id());
        it.setItemMeta(m);
        return it;
    }

    private List<Component> appendStats(List<Component> lore, WeaponDef w) {
        var l = new java.util.ArrayList<>(lore);
        l.add(Component.empty());
        l.add(Component.text("⚔ " + (w.damage() + 1) + " Attack Damage", NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false));
        l.add(Component.text("⚡ " + String.format("%.1f", 4 + w.speed()) + " Attack Speed", NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false));
        return l;
    }

    /** Item used purely as a particle texture (never given to players). */
    public ItemStack particleItem(String kind) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta m = it.getItemMeta();
        m.setItemModel(new NamespacedKey(NS, "particle_" + kind));
        it.setItemMeta(m);
        return it;
    }

    private static Component gray(String s) { return Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false); }

    // ------------------------------------------------------------ identification

    public String type(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(keyType, PersistentDataType.STRING);
    }

    public String classOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(keyClass, PersistentDataType.STRING);
    }

    public boolean isOrb(ItemStack it) { return "orb".equals(type(it)); }
    public boolean isWeapon(ItemStack it) { return "weapon".equals(type(it)); }
    public boolean isClassItem(ItemStack it) { return type(it) != null; }

    /** Can this item trigger a cast on right click? (orb, or non-bow weapon) */
    public boolean isCaster(ItemStack it) {
        String t = type(it);
        if (t == null) return false;
        if (t.equals("orb")) return true;
        return t.equals("weapon") && it.getType() != Material.BOW;
    }

    // ------------------------------------------------------------ giving

    /** Removes old class items and gives the orb + weapon for the given class. */
    public void giveKit(Player p, RPGClass c) {
        removeClassItems(p);
        give(p, powerOrb(c));
        ItemStack w = weapon(c);
        if (w != null) give(p, w);
    }

    public void removeClassItems(Player p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) if (isClassItem(inv.getItem(i))) inv.setItem(i, null);
        if (isClassItem(inv.getItemInOffHand())) inv.setItemInOffHand(null);
    }

    public boolean hasOrb(Player p) {
        for (ItemStack it : p.getInventory().getContents()) if (isOrb(it)) return true;
        return false;
    }

    private void give(Player p, ItemStack it) {
        var left = p.getInventory().addItem(it);
        left.values().forEach(rest -> p.getWorld().dropItemNaturally(p.getLocation(), rest));
    }
}
