package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starmclib.actors.Actor;
import org.bukkit.event.*;

import java.util.Optional;
public class DeleteHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private final Home home;
    private final Actor actor;
    
    public DeleteHomeEvent(Home home, Actor actor) {
        this.home = home;
        this.actor = actor;
    }
    
    public Home getHome() {
        return home;
    }
    
    public Actor getActor() {
        return actor;
    public Optional<Actor> getActor() {
        return Optional.ofNullable(actor);
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
