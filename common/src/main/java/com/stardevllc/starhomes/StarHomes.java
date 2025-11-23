package com.stardevllc.starhomes;

import com.stardevllc.config.Section;
import com.stardevllc.config.file.FileConfig;
import com.stardevllc.config.file.yaml.YamlConfig;
import com.stardevllc.starhomes.events.*;
import com.stardevllc.starlib.observable.collections.list.ObservableArrayList;
import com.stardevllc.starlib.observable.collections.list.ObservableList;
import com.stardevllc.starlib.observable.collections.map.ObservableHashMap;
import com.stardevllc.starlib.observable.collections.map.ObservableMap;
import com.stardevllc.starmclib.Position;
import com.stardevllc.starmclib.actors.Actor;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * <p>
 * Main class for the StarHomes logic.
 * This class must be initialized by a plugin by calling the {@link #init(ExtendedJavaPlugin)} method
 * This also has pretty much all logic needed for homes for the most part.
 * The homes are stored in an {@link ObservableMap} with UUIDs for the keys and an {@link ObservableList} for the homes list
 * Using the observable map and list allow for the ability to listen for when things are changed and removed at will
 * </p>
 */
public final class StarHomes {
    private static ExtendedJavaPlugin plugin;
    
    private static final ObservableMap<UUID, ObservableList<Home>> homes = new ObservableHashMap<>();
    
    public static void init(ExtendedJavaPlugin plugin) {
        if (StarHomes.plugin != null) {
            plugin.getLogger().warning("StarHomes has already been initialized by the plugin: " + StarHomes.plugin.getName());
            return;
        }
        
        StarHomes.plugin = plugin;
        
        plugin.getMainConfig().addDefault("homes.storage", "SINGLEFILE", "This controls how homes are stored.", "SINGLEFILE means that all homes are stored in a single file", "SEPARATEFILES means that homes are stored in a file per player using {playeruuid}.yml");
        plugin.getMainConfig().addDefault("homes.singlefile.name", "homes.yml", "This is just the name of the file when using SINGLEFILE storage mode", "If you change this after homes are created, you will need to rename the old file to the new name. Otherwise homes will not be loaded");
        plugin.getMainConfig().addDefault("homes.separatefiles.foldername", "homes", "This is the name of the folder for the home file locations when using SEPARATEFILES mode", "If you change this after homes are created, you will need to rename the old folder to the new name. Otherwise homes will not be loaded");
        plugin.getMainConfig().save();
        loadHomes();
        
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, StarHomes::saveHomes, 1L, 6000L);
    }
    
    public static Optional<File> getSingleFile() {
        if (!"singlefile".equalsIgnoreCase(plugin.getMainConfig().getString("homes.storage"))) {
            return Optional.empty();
        }
        
        File file = new File(plugin.getDataFolder(), plugin.getMainConfig().getString("homes.singlefile.name"));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Error while creating " + plugin.getMainConfig().getString("homes.singlefile.name"));
                return Optional.empty();
            }
        }
        return Optional.of(file);
    }
    
    public static Optional<FileConfig> getSingleConfig() {
        Optional<File> singleFileOpt = getSingleFile();
        if (singleFileOpt.isEmpty()) {
            return Optional.empty();
        }
        
        FileConfig config;
        try {
            config = YamlConfig.loadConfiguration(singleFileOpt.get());
        } catch (Throwable t) {
            return Optional.empty();
        }
        
        return Optional.of(config);
    }
    
    public static Optional<File> getHomesFolder() {
        if (!"separatefiles".equalsIgnoreCase(plugin.getMainConfig().getString("homes.storage"))) {
            return Optional.empty();
        }
        
        File folder = new File(plugin.getDataFolder(), plugin.getMainConfig().getString("homes.separatefiles.folder"));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        return Optional.of(folder);
    }
    
    public static void loadHomes() {
        String storageMode = plugin.getMainConfig().getString("homes.storage");
        if (storageMode.equalsIgnoreCase("singlefile")) {
            Optional<FileConfig> singleConfigOpt = getSingleConfig();
            if (singleConfigOpt.isEmpty()) {
                return;
            }
            
            FileConfig config = singleConfigOpt.get();
            for (String rawUUID : config.getKeys()) {
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUUID);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error while parsing homes for " + rawUUID, e);
                    continue;
                }
                
                Section playerHomesSection = config.getSection(rawUUID);
                if (playerHomesSection == null) {
                    continue;
                }
                
                ObservableList<Home> playerHomes = loadHomes(uuid, playerHomesSection);
                homes.put(uuid, playerHomes);
            }
        } else if (storageMode.equalsIgnoreCase("separatefiles")) {
            Optional<File> homesFolderOpt = getHomesFolder();
            if (homesFolderOpt.isEmpty()) {
                return;
            }
            
            File folder = homesFolderOpt.get();
            
            for (File file : folder.listFiles()) {
                if (file.isDirectory()) {
                    continue;
                }
                
                String fileName = file.getName();
                if (!fileName.endsWith(".yml")) {
                    continue;
                }
                
                int periodIndex = fileName.lastIndexOf('.');
                String rawUUID = fileName.substring(0, periodIndex);
                UUID uuid;
                try {
                    uuid = UUID.fromString(rawUUID);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error while parsing homes for " + rawUUID, e);
                    continue;
                }
                
                FileConfig config = YamlConfig.loadConfiguration(file);
                ObservableList<Home> playerHomes = loadHomes(uuid, config);
                homes.put(uuid, playerHomes);
            }
            
        } else {
            throw new IllegalArgumentException("Invalid storage mode provided");
        }
    }
    
    public static ObservableList<Home> loadHomes(UUID owner, Section section) {
        ObservableList<Home> homes = new ObservableArrayList<>();
        for (String name : section.getKeys()) {
            double x = section.getDouble(name + ".x");
            double y = section.getDouble(name + ".y");
            double z = section.getDouble(name + ".z");
            float yaw = (float) section.getDouble(name + ".yaw");
            float pitch = (float) section.getDouble(name + ".pitch");
            Position position = new Position(x, y, z, yaw, pitch);
            String worldName = section.getString(name + ".worldName");
            homes.add(new Home(owner, name, worldName, position));
        }
        
        return homes;
    }
    
    public static void saveHomes() {
        try {
            String storageMode = plugin.getMainConfig().getString("homes.storage");
            if (storageMode.equalsIgnoreCase("singlefile")) {
                Optional<FileConfig> singleConfigOpt = getSingleConfig();
                if (singleConfigOpt.isEmpty()) {
                    return;
                }
                
                FileConfig config = singleConfigOpt.get();
                for (String rawUUID : config.getKeys()) {
                    config.set(rawUUID, null);
                    try {
                        UUID.fromString(rawUUID);
                    } catch (Throwable t) {
                        plugin.getLogger().log(Level.SEVERE, "Error while saving homes for " + rawUUID, t);
                    }
                }
                
                config.save();
                
                homes.forEach((owner, homes) -> {
                    Section section;
                    if (!config.contains(owner.toString())) {
                        section = config.createSection(owner.toString());
                    } else {
                        section = config.getSection(owner.toString());
                    }
                    
                    saveHomes(owner, homes, section);
                });
                
                config.save();
            } else if (storageMode.equalsIgnoreCase("separatefiles")) {
                Optional<File> homesFolderOpt = getHomesFolder();
                if (homesFolderOpt.isEmpty()) {
                    return;
                }
                
                File folder = homesFolderOpt.get();
                
                homes.forEach((owner, homes) -> {
                    File file = new File(folder, owner.toString() + ".yml");
                    if (file.exists()) {
                        file.delete();
                    }
                    
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        plugin.getLogger().severe("Could not create the file " + file.getName());
                        return;
                    }
                    
                    FileConfig config = YamlConfig.loadConfiguration(file);
                    saveHomes(owner, homes, config);
                    config.save();
                });
            } else {
                throw new IllegalArgumentException("Invalid storage mode provided");
            }
        } catch (ConcurrentModificationException e) {}
    }
    
    public static void saveHomes(UUID owner, ObservableList<Home> homes, Section section) {
        for (Home home : homes) {
            String name = home.getName();
            Position position = home.getPosition();
            section.set(name + ".x", position.getX());
            section.set(name + ".y", position.getY());
            section.set(name + ".z", position.getZ());
            section.set(name + ".yaw", position.getYaw());
            section.set(name + ".pitch", position.getPitch());
            section.set(name + ".worldName", home.getWorldName());
        }
    }
    
    public static ObservableMap<UUID, ObservableList<Home>> getHomes() {
        return homes;
    }
    
    public static ObservableList<Home> getHomes(UUID owner) {
        if (homes.containsKey(owner)) {
            return homes.get(owner);
        }
        
        ObservableList<Home> playerHomes = new ObservableArrayList<>();
        homes.put(owner, playerHomes);
        return playerHomes;
    }
    
    public static Optional<Home> getHome(UUID owner, String name) {
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            return Optional.empty();
        }
        
        for (Home home : homes) {
            if (home.getName().equalsIgnoreCase(name)) {
                return Optional.of(home);
            }
        }
        
        return Optional.empty();
    }
    
    public enum SetHomeStatus {
        SUCCESS, EVENT_CANCELLED
    }
    
    public record SetHomeInfo(Home home, SetHomeStatus status) {}
    
    public static SetHomeInfo setHome(UUID owner, String name, Location location) {
        return setHome(owner, name, location, null);
    }
    
    public static SetHomeInfo setHome(UUID owner, String name, Location location, Actor actor) {
        ObservableList<Home> homes = getHomes(owner);
        
        Home home = null;
        boolean add = false;
        
        if (homes.isEmpty()) {
            home = new Home(owner, name, location);
            add = true;
        }
        
        if (home == null) {
            for (Home h : homes) {
                if (h.getName().equalsIgnoreCase(name)) {
                    h.setPosition(location);
                    home = h;
                    break;
                }
            }
        }
        
        if (home == null) {
            home = new Home(owner, name, location);
            add = true;
        }
        
        SetHomeEvent setHomeEvent = new SetHomeEvent(home, actor);
        Bukkit.getPluginManager().callEvent(setHomeEvent);
        
        if (setHomeEvent.isCancelled()) {
            return new SetHomeInfo(home, SetHomeStatus.EVENT_CANCELLED);
        }
        
        if (add) {
            homes.add(home);
        }
        
        return new SetHomeInfo(home, SetHomeStatus.SUCCESS);
    }
    
    public enum DeleteHomeStatus {
        SUCCESS, EVENT_CANCELLED, NO_HOME
    }
    
    public record DeleteHomeInfo(Optional<Home> home, String name, DeleteHomeStatus status) {}
    
    public static DeleteHomeInfo deleteHome(UUID owner, String name) {
        return deleteHome(owner, name, null);
    }
    
    public static DeleteHomeInfo deleteHome(UUID owner, String name, Actor actor) {
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            return new DeleteHomeInfo(Optional.empty(), name, DeleteHomeStatus.NO_HOME);
        }
        
        Iterator<Home> iterator = homes.iterator();
        while (iterator.hasNext()) {
            Home home = iterator.next();
            if (home.getName().equalsIgnoreCase(name)) {
                DeleteHomeEvent deleteHomeEvent = new DeleteHomeEvent(home, actor);
                Bukkit.getPluginManager().callEvent(deleteHomeEvent);
                
                if (deleteHomeEvent.isCancelled()) {
                    return new DeleteHomeInfo(Optional.of(home), name, DeleteHomeStatus.EVENT_CANCELLED);
                }
                
                iterator.remove();
                return new DeleteHomeInfo(Optional.of(home), name, DeleteHomeStatus.SUCCESS);
            }
        }
        
        return new DeleteHomeInfo(Optional.empty(), name, DeleteHomeStatus.NO_HOME);
    }
    
    public enum RenameHomeStatus {
        SUCCESS, EVENT_CANCELLED, NO_HOME
    }
    
    public record RenameHomeInfo(Optional<Home> home, String oldName, String newName, RenameHomeStatus status) {}
    
    public static RenameHomeInfo renameHome(UUID owner, String oldName, String newName, Actor actor) {
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            return new RenameHomeInfo(Optional.empty(), oldName, newName, RenameHomeStatus.NO_HOME);
        }
        
        for (Home home : homes) {
            if (home.getName().equalsIgnoreCase(oldName)) {
                String existingName = home.getName();
                
                RenameHomeEvent renameHomeEvent = new RenameHomeEvent(home, newName, actor);
                Bukkit.getPluginManager().callEvent(renameHomeEvent);
                
                if (renameHomeEvent.isCancelled()) {
                    return new RenameHomeInfo(Optional.of(home), existingName, newName, RenameHomeStatus.EVENT_CANCELLED);
                }
                
                home.setName(newName);
                return new RenameHomeInfo(Optional.of(home), existingName, newName, RenameHomeStatus.SUCCESS);
            }
        }
        
        return new RenameHomeInfo(Optional.empty(), oldName, newName, RenameHomeStatus.NO_HOME);
    }
    
    public static RenameHomeInfo renameHome(UUID owner, String oldName, String newName) {
        return renameHome(owner, oldName, newName, null);
    }
}