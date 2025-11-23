package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import org.bukkit.entity.Player;
import org.bukkit.event.*;

public class RenameHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private Home home;
    private String oldName;
    private Player actor;
    
    public RenameHomeEvent(Home home, String oldName, Player actor) {
        this.home = home;
        this.oldName = oldName;
        this.actor = actor;
    }
    
    public Home getHome() {
        return home;
    }
    
    public String getOldName() {
        return oldName;
    }
    
    public Player getActor() {
        return actor;
    }
    
    public static HandlerList getHandlerList() {
        return handlerList;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
