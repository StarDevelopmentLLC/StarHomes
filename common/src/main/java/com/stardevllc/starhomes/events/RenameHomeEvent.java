package com.stardevllc.starhomes.events;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starmclib.actors.Actor;
import org.bukkit.event.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Called when a rename home request is initiated <br>
 * This event is called before the actual rename occurs <br>
 * Cancel this event to stop the rename
 *
 * @see com.stardevllc.starhomes.StarHomes#renameHome(UUID, String, String)
 * @see com.stardevllc.starhomes.StarHomes#renameHome(UUID, String, String, Actor)
 */
public class RenameHomeEvent extends Event implements Cancellable {
    
    private static HandlerList handlerList = new HandlerList();
    
    private boolean cancelled;
    
    private final Home home;
    private final String newName;
    private final Actor actor;
    
    /**
     * Creates a new rename home event
     *
     * @param home    The home that the request is targetting
     * @param newName The new name for the home
     * @param actor   The actor that performed the request (Can be null)
     */
    public RenameHomeEvent(Home home, String newName, Actor actor) {
        this.home = home;
        this.newName = newName;
        this.actor = actor;
    }
    
    /**
     * The home of the rename request
     *
     * @return The home to be renamed
     */
    public Home getHome() {
        return home;
    }
    
    /**
     * The new name for the home
     *
     * @return The new name
     */
    public String getNewName() {
        return newName;
    }
    
    /**
     * The actor of the request (Can be null)
     *
     * @return The actor as an optional
     */
    public Optional<Actor> getActor() {
        return Optional.ofNullable(actor);
    }
    
    /**
     * Bukkit Event stuff
     *
     * @return HandlerList
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
