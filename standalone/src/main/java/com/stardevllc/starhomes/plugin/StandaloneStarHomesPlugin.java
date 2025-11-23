package com.stardevllc.starhomes.plugin;

import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.commands.*;
import com.stardevllc.starmclib.StarMCLib;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class StandaloneStarHomesPlugin extends ExtendedJavaPlugin {
    @Override
    public void onEnable() {
        Plugin starmclibPlugin = Bukkit.getPluginManager().getPlugin("StarMCLib");
        if (starmclibPlugin != null) {
            getLogger().severe("StarMCLib plugin detected with the StarHomes-Standalone plugin");
            getLogger().severe("Please either replace StarHomes-Standalone with StarHomes Plugin or remove StarMCLib plugin");
            getLogger().severe("Please see the wiki page for more information");
            getLogger().severe("https://github.com/StarDevelopmentLLC/StarHomes/wiki/Available-Binaries");
        }
        
        Plugin starcorePlugin = Bukkit.getPluginManager().getPlugin("StarCore");
        if (starcorePlugin != null) {
            getLogger().severe("StarCore plugin detected with the StarHomes-Standalone plugin");
            getLogger().severe("Please either replace StarHomes-Standalone with StarHomes Plugin or remove StarCore");
            getLogger().severe("Please see the wiki page for more information");
            getLogger().severe("https://github.com/StarDevelopmentLLC/StarHomes/wiki/Available-Binaries");
        }
        
        StarMCLib.init(this);
        super.onEnable();
        StarHomes.init(this);
        registerCommand(new HomeCommand(this), new DelhomeCommand(this), new RenameHomeCommand(this), new SethomeCommand(this));
    }
    
    @Override
    public void onDisable() {
        super.onDisable();
        StarHomes.saveHomes();
    }
}
