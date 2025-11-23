package com.stardevllc.starhomes;

import com.stardevllc.starmclib.Position;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.Objects;
import java.util.UUID;

public class Home {
    private final UUID owner;
    private String name;
    private String worldName;
    private Position position;
    
    private Location locationCache;
    
    public Home(UUID owner, String name, String worldName, Position position) {
        this.owner = owner;
        this.name = name;
        this.worldName = worldName;
        this.position = position;
    }
    
    public Home(UUID owner, String name, Location location) {
        this.owner = owner;
        this.name = name;
        this.worldName = location.getWorld().getName();
        this.position = Position.fromLocation(location);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setWorldName(String worldName) {
        this.locationCache = null;
        this.worldName = worldName;
    }
    
    public void setPosition(Position position) {
        this.locationCache = null;
        this.position = position;
    }
    
    public void setPosition(Location location) {
        setWorldName(location.getWorld().getName());
        setPosition(Position.fromLocation(location));
    }
    
    public void teleport(Entity entity) {
        if (this.locationCache == null) {
            this.locationCache = this.position.toLocation(Bukkit.getWorld(this.worldName));
        }
        
        entity.teleport(locationCache);
    }
    
    public UUID getOwner() {
        return owner;
    }
    
    public String getName() {
        return name;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public Location getLocationCache() {
        return locationCache;
    }
    
    @Override
    public final boolean equals(Object object) {
        if (!(object instanceof Home home)) {
            return false;
        }
        
        return Objects.equals(owner, home.owner) && Objects.equals(name, home.name);
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hashCode(owner);
        result = 31 * result + Objects.hashCode(name);
        return result;
    }
}