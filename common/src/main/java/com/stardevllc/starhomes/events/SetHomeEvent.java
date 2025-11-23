package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starmclib.actors.Actor;
import org.bukkit.event.*;

public class SetHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private Home home;
    private Actor actor;
    
    public SetHomeEvent(Home home, Actor actor) {
        this.home = home;
        this.actor = actor;
    }
    
    public Home getHome() {
        return home;
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
