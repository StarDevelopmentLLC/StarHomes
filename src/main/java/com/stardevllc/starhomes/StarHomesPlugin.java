package com.stardevllc.starhomes;

import com.stardevllc.plugin.ExtendedJavaPlugin;
import com.stardevllc.starhomes.commands.*;

public class StarHomesPlugin extends ExtendedJavaPlugin {
    @Override
    public void onEnable() {
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