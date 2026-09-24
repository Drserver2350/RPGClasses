package com.arena.rpgclasses;

import com.arena.rpgclasses.command.Commands;
import com.arena.rpgclasses.gui.ClassGUI;
import com.arena.rpgclasses.listener.CombatListener;
import com.arena.rpgclasses.listener.PlayerListener;
import com.arena.rpgclasses.manager.ClassManager;
import com.arena.rpgclasses.manager.DataManager;
import com.arena.rpgclasses.manager.ManaManager;
import com.arena.rpgclasses.manager.SkillManager;
import com.arena.rpgclasses.pack.PackManager;
import com.arena.rpgclasses.skills.ClassRegistry;
import com.arena.rpgclasses.util.FX;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RPGClassesPlugin extends JavaPlugin {

    private static RPGClassesPlugin instance;

    private ClassRegistry classes;
    private DataManager data;
    private ClassManager classManager;
    private ManaManager mana;
    private SkillManager skills;
    private PackManager pack;
    private ClassGUI gui;

    public static RPGClassesPlugin get() { return instance; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        FX.init(this);

        classes = new ClassRegistry();
        data = new DataManager(this);
        classManager = new ClassManager(this);
        mana = new ManaManager(this);
        skills = new SkillManager(this);
        pack = new PackManager(this);
        gui = new ClassGUI(this);

        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);

        Commands cmds = new Commands(this);
        for (String c : new String[]{"class", "cast", "skills", "rpgadmin"}) {
            var pc = getCommand(c);
            if (pc != null) { pc.setExecutor(cmds); pc.setTabCompleter(cmds); }
        }

        mana.start();
        pack.start();

        // handle /reload: re-apply stats to online players
        for (Player p : Bukkit.getOnlinePlayers()) classManager.applyStats(p);

        // autosave every 5 minutes
        Bukkit.getScheduler().runTaskTimer(this, () -> data.saveAll(), 20L * 300, 20L * 300);

        getLogger().info("RPGClasses enabled with " + classes.all().size() + " classes.");
    }

    @Override
    public void onDisable() {
        if (mana != null) mana.stop();
        if (pack != null) pack.stop();
        if (data != null) data.saveAll();
        if (classManager != null) for (Player p : Bukkit.getOnlinePlayers()) classManager.clearStats(p);
    }

    public ClassRegistry classes() { return classes; }
    public DataManager data() { return data; }
    public ClassManager classManager() { return classManager; }
    public ManaManager mana() { return mana; }
    public SkillManager skills() { return skills; }
    public PackManager pack() { return pack; }
    public ClassGUI gui() { return gui; }
}
