package com.stardevllc.starhomes;

import com.stardevllc.starmclib.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a home
 */
public class Home {
    private final UUID owner;
    private String name;
    private String worldName;
    private Position position;
    
    private Location locationCache;
    
    /**
     * Constructs a new home
     *
     * @param owner     The owner of the home
     * @param name      The name of the home
     * @param worldName The world name of the home
     * @param position  The position of the home
     */
    public Home(UUID owner, String name, String worldName, Position position) {
        this.owner = owner;
        this.name = name;
        this.worldName = worldName;
        this.position = position;
    }
    
    /**
     * Constructs a new home
     *
     * @param owner    The owner of the home
     * @param name     The name of the home
     * @param location The location of the home
     */
    public Home(UUID owner, String name, Location location) {
        this.owner = owner;
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.position = Position.fromLocation(location);
    }
    
    /**
     * Sets the name of this home
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Sets the world name of this home
     *
     * @param worldName The new world name
     */
    public void setWorldName(String worldName) {
        this.locationCache = null;
        this.worldName = worldName;
    }
    
    /**
     * Sets the position of this home
     *
     * @param position The new position
     */
    public void setPosition(Position position) {
        this.locationCache = null;
        this.position = position;
    }
    
    /**
     * Sets the location of this home
     *
     * @param location The new locatioin
     */
    public void setLocation(Location location) {
        setWorldName(location.getWorld().getName());
        setPosition(Position.fromLocation(location));
    }
    
    /**
     * Teleports an entity to this home
     *
     * @param entity The entity
     */
    public void teleport(Entity entity) {
        if (this.locationCache == null) {
            this.locationCache = this.position.toLocation(Bukkit.getWorld(this.worldName));
        }
        
        entity.teleport(locationCache);
    }
    
    /**
     * The owner of the home
     *
     * @return The owner
     */
    public UUID getOwner() {
        return owner;
    }
    
    /**
     * The name of the home
     *
     * @return The name
     */
    public String getName() {
        return name;
    }
    
    /**
     * The world name of the home
     *
     * @return The world name
     */
    public String getWorldName() {
        return worldName;
    }
    
    /**
     * The position of the home
     *
     * @return The position
     */
    public Position getPosition() {
        return position;
    }
    
    @Override
    public final boolean equals(Object object) {
        if (!(object instanceof Home home)) {
            return false;
        }
        
        return Objects.equals(owner, home.owner) && Objects.equals(name.toLowerCase(), home.name.toLowerCase());
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hashCode(owner);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }
}