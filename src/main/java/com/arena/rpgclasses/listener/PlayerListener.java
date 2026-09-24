package com.arena.rpgclasses.listener;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.util.FX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

/** Join/quit, resource pack delivery, casting input. */
public final class PlayerListener implements Listener {

    private final RPGClassesPlugin plugin;

    public PlayerListener(RPGClassesPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        var p = e.getPlayer();
        PlayerData d = plugin.data().get(p);
        plugin.classManager().applyStats(p);
        // send pack a moment after join so the client is ready
        FX.later(20L, () -> { if (p.isOnline()) plugin.pack().send(p); });
        if (!d.hasClass()) {
            FX.later(60L, () -> {
                if (!p.isOnline()) return;
                FX.msg(p, Component.text("Welcome, adventurer! ", NamedTextColor.GOLD)
                        .append(Component.text("Choose your class: ", NamedTextColor.GRAY))
                        .append(Component.text("[Open Class Menu]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/class"))));
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.data().unload(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        FX.later(1L, () -> plugin.classManager().applyStats(e.getPlayer()));
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent e) {
        var p = e.getPlayer();
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> FX.msg(p, Component.text("Resource pack loaded ✔", NamedTextColor.GREEN));
            case DECLINED -> FX.msg(p, Component.text("You declined the resource pack. Class icons will use vanilla items. Enable server packs in the server list to see custom art.", NamedTextColor.YELLOW));
            case FAILED_DOWNLOAD -> FX.msg(p, Component.text("Resource pack download failed. Ask an admin to check resource-pack settings.", NamedTextColor.RED));
            default -> {}
        }
    }

    private boolean itemMatches(Player p, PlayerData d, org.bukkit.inventory.ItemStack it) {
        String cls = plugin.items().classOf(it);
        if (cls == null) return false;
        if (!cls.equals(d.classId())) {
            FX.actionBar(p, Component.text("This item belongs to the " + plugin.classes().get(cls).displayName() + " class. Use /class items.", NamedTextColor.RED));
            return false;
        }
        return true;
    }

    /** F (swap hands) while holding the orb/weapon cycles the selected skill (sneak = backwards). */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        var p = e.getPlayer();
        var it = p.getInventory().getItemInMainHand();
        if (!plugin.items().isClassItem(it)) return;
        e.setCancelled(true);                       // never move the soulbound item to the offhand
        PlayerData d = plugin.data().get(p);
        if (!d.hasClass() || !itemMatches(p, d, it)) return;
        plugin.skills().cycle(p, p.isSneaking() ? -1 : 1);
    }

    /** Right click with the Power Orb or the class weapon casts the selected skill. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        var p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND) return;
        var it = e.getItem();
        if (!plugin.items().isClassItem(it)) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!plugin.items().isCaster(it)) return;   // bow: vanilla draw, cast with orb instead
        // let players still open chests/doors etc. unless sneaking
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable() && !p.isSneaking()) return;
        e.setCancelled(true);
        PlayerData d = plugin.data().get(p);
        if (!d.hasClass()) { FX.msg(p, Component.text("Choose a class first with /class", NamedTextColor.RED)); return; }
        if (!itemMatches(p, d, it)) return;
        plugin.skills().cast(p, d.selectedSkill());
    }

    // ---------------- soulbound protection ----------------

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.items().isClassItem(e.getItemDrop().getItemStack())) {
            e.setCancelled(true);
            FX.actionBar(e.getPlayer(), Component.text("This item is soulbound.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        e.getDrops().removeIf(plugin.items()::isClassItem);
        e.getItemsToKeep().removeIf(plugin.items()::isClassItem);
        var p = e.getPlayer();
        var c = plugin.classes().get(plugin.data().get(p).classId());
        if (c == null) return;
        // re-give a fresh kit on respawn
        FX.later(2L, () -> {
            if (p.isOnline() && p.isDead()) {
                var l = new org.bukkit.event.Listener() {};
                plugin.getServer().getPluginManager().registerEvent(PlayerRespawnEvent.class, l, EventPriority.MONITOR,
                        (ll, ev) -> { if (((PlayerRespawnEvent) ev).getPlayer() == p) { FX.later(1L, () -> plugin.items().giveKit(p, c)); org.bukkit.event.HandlerList.unregisterAll(ll); } }, plugin);
            } else if (p.isOnline()) plugin.items().giveKit(p, c);
        });
    }

    /** Don't let the orb be put into item frames / armor stands / containers via right-click. */
    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (plugin.items().isClassItem(e.getPlayer().getInventory().getItem(e.getHand()))) e.setCancelled(true);
    }

    @EventHandler
    public void onInvClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        var top = e.getView().getTopInventory();
        if (top.getType() == org.bukkit.event.inventory.InventoryType.CRAFTING || top.getType() == org.bukkit.event.inventory.InventoryType.PLAYER) return;
        if (top.getHolder() instanceof org.bukkit.inventory.InventoryHolder h && h.getClass().getName().contains("ClassGUI")) return;
        var cur = e.getCurrentItem();
        var cursor = e.getCursor();
        boolean movingIn = (e.isShiftClick() && e.getClickedInventory() == e.getView().getBottomInventory() && plugin.items().isClassItem(cur))
                || (e.getClickedInventory() == top && plugin.items().isClassItem(cursor));
        if (movingIn) { e.setCancelled(true); FX.actionBar((org.bukkit.entity.Player) e.getWhoClicked(), Component.text("Soulbound items can't be stored.", NamedTextColor.RED)); }
    }

    /** Give missing kit on join (e.g. after an admin cleared inventories). */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinKit(PlayerJoinEvent e) {
        var p = e.getPlayer();
        FX.later(40L, () -> {
            if (!p.isOnline()) return;
            var d = plugin.data().get(p);
            var c = plugin.classes().get(d.classId());
            if (c != null && !plugin.items().hasOrb(p)) plugin.items().giveKit(p, c);
        });
    }
}
