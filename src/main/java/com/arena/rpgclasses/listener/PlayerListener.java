package com.arena.rpgclasses.listener;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.arena.rpgclasses.model.PlayerData;
import com.arena.rpgclasses.util.FX;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    /** Sneak + F (swap hands) cycles the selected skill. */
    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        var p = e.getPlayer();
        if (!p.isSneaking()) return;
        PlayerData d = plugin.data().get(p);
        if (!d.hasClass()) return;
        e.setCancelled(true);
        plugin.skills().cycle(p, 1);
    }

    /** Sneak + right click casts the selected skill. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        var p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!p.isSneaking()) return;
        PlayerData d = plugin.data().get(p);
        if (!d.hasClass()) return;
        // don't hijack container/interactable block use
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable() && !p.isSneaking()) return;
        var item = p.getInventory().getItemInMainHand();
        if (item.getType().isEdible() || item.getType().name().endsWith("BUCKET") || item.getType().isBlock() && !item.getType().isAir()) return;
        if (plugin.skills().cast(p, d.selectedSkill())) e.setCancelled(true);
    }
}
