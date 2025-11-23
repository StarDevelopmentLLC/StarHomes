package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starmclib.actors.Actor;
import org.bukkit.event.*;

public class RenameHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private Home home;
    private String newName;
    private Actor actor;
    
    public RenameHomeEvent(Home home, String newName, Actor actor) {
        this.home = home;
        this.newName = newName;
        this.actor = actor;
    }
    
    public Home getHome() {
        return home;
    }
    
    public String getNewName() {
        return newName;
    }
    
    public Actor getActor() {
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
