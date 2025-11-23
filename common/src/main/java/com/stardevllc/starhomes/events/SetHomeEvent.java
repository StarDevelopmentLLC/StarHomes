package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starmclib.actors.Actor;
import org.bukkit.Location;
import org.bukkit.event.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Called when a set home request is initiated <br>
 * This is called before the home is applied to the owner <br>
 * Cancel this event to prevent the set home action from occuring
 *
 * @see com.stardevllc.starhomes.StarHomes#setHome(UUID, String, Location)
 * @see com.stardevllc.starhomes.StarHomes#setHome(UUID, String, Location, Actor)
 */
public class SetHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private final Home home;
    private final Actor actor;
    
    /**
     * Creates a new set home event
     *
     * @param home  The home that is to be applied
     * @param actor The actor that performed the request (Can be null)
     */
    public SetHomeEvent(Home home, Actor actor) {
        this.home = home;
        this.actor = actor;
    }
    
    /**
     * The home for the request
     *
     * @return The home
     */
    public Home getHome() {
        return home;
    }
    
    /**
     * The actor that requested the action
     *
     * @return The actor as an optional
     */
    public Optional<Actor> getActor() {
        return Optional.ofNullable(actor);
    }
    
    /**
     * Bukkit event things
     *
     * @return The handler list
     */
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
