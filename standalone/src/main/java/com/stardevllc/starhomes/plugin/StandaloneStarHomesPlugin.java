package com.stardevllc.starhomes.plugin;

import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.commands.*;
import com.stardevllc.starmclib.StarMCLib;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;

public class StandaloneStarHomesPlugin extends ExtendedJavaPlugin {
    @Override
    public void onEnable() {
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
