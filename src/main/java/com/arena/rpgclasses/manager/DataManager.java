package com.arena.rpgclasses.manager;

import com.arena.rpgclasses.model.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

/** YAML persistence for player data (one file per player). */
public final class DataManager {

    private final JavaPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "players");
        if (!dir.exists()) dir.mkdirs();
    }

    public PlayerData get(Player p) { return get(p.getUniqueId()); }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public Collection<PlayerData> loaded() { return cache.values(); }

    private PlayerData load(UUID uuid) {
        PlayerData d = new PlayerData(uuid);
        File f = new File(dir, uuid + ".yml");
        if (!f.exists()) return d;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        d.setClassId(y.getString("class", null));
        d.setMana(y.getDouble("mana", 0));
        d.setLastClassChange(y.getLong("last-class-change", 0));
        var lv = y.getConfigurationSection("levels");
        if (lv != null) for (String k : lv.getKeys(false)) d.setLevel(k, lv.getInt(k));
        var xp = y.getConfigurationSection("xp");
        if (xp != null) for (String k : xp.getKeys(false)) d.setXp(k, xp.getDouble(k));
        return d;
    }

    public void save(PlayerData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("class", d.classId());
        y.set("mana", d.mana());
        y.set("last-class-change", d.lastClassChange());
        for (var e : d.allLevels().entrySet()) y.set("levels." + e.getKey(), e.getValue());
        for (var e : d.allXp().entrySet()) y.set("xp." + e.getKey(), e.getValue());
        try {
            y.save(new File(dir, d.uuid() + ".yml"));
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save data for " + d.uuid() + ": " + ex.getMessage());
        }
    }

    public void unload(UUID uuid) {
        PlayerData d = cache.remove(uuid);
        if (d != null) save(d);
    }

    public void saveAll() { cache.values().forEach(this::save); }
}
