package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starmclib.actors.Actor;
import org.bukkit.event.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Called when a home is in the process of being deleted <br>
 * This is called before the deletion actually occurs <br>
 * Cancel this event to prevent the deletion to occur
 *
 * @see com.stardevllc.starhomes.StarHomes#deleteHome(UUID, String)
 * @see com.stardevllc.starhomes.StarHomes#deleteHome(UUID, String, Actor)
 */
public class DeleteHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private final Home home;
    private final Actor actor;
    
    /**
     * Creates a new delete home event
     *
     * @param home  The home that is to be deleted
     * @param actor The actor that performed the deletion request (Can be null)
     */
    public DeleteHomeEvent(Home home, Actor actor) {
        this.home = home;
        this.actor = actor;
    }
    
    /**
     * The home of the deletion request
     *
     * @return The home
     */
    public Home getHome() {
        return home;
    }
    
    /**
     * The actor that performed the deletion request (Can be null)
     *
     * @return The actor as an optional
     */
    public Optional<Actor> getActor() {
        return Optional.ofNullable(actor);
    }
    
    /**
     * Used for the Bukkit Events System
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
