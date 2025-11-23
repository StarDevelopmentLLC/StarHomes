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
 * This also allows you to modify the homes as well, use with caution
 * </p>
 */
public final class StarHomes {
    private StarHomes() {}
    
    private static ExtendedJavaPlugin plugin;
    
    private static final ObservableMap<UUID, ObservableList<Home>> homes = new ObservableHashMap<>();
    
    /**
     * Initalilzes StarHomes
     *
     * @param plugin The holder plugin
     */
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
    
    private static Optional<File> getSingleFile() {
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
    
    private static Optional<FileConfig> getSingleConfig() {
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
    
    private static Optional<File> getHomesFolder() {
        if (!"separatefiles".equalsIgnoreCase(plugin.getMainConfig().getString("homes.storage"))) {
            return Optional.empty();
        }
        
        File folder = new File(plugin.getDataFolder(), plugin.getMainConfig().getString("homes.separatefiles.folder"));
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        return Optional.of(folder);
    }
    
    /**
     * Loads all homes from storage
     *
     * @throws IllegalArgumentException If the storage mode from the config if it is invalid
     */
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
    
    /**
     * Loads all homes for a home owner
     *
     * @param owner   The owner to load from (Needed for home instantiation)
     * @param section The config section that contains the homes
     * @return The ObservableList of all loaded homes
     */
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
    
    /**
     * Saves all homes to storage
     *
     * @throws IllegalArgumentException If the storage mode from the config is invalid
     */
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
        } catch (ConcurrentModificationException e) {
        }
    }
    
    /**
     * Saves all homes for an owner
     *
     * @param owner   The owner of the homes
     * @param homes   The list of homes to save
     * @param section The section to save the homes in
     */
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
    
    /**
     * Gets a mapping of all homes to their owners
     *
     * @return The ObservableMap of homes and uuids
     */
    public static ObservableMap<UUID, ObservableList<Home>> getHomes() {
        return homes;
    }
    
    /**
     * Gets all homes for a home owner
     *
     * @param owner The owner
     * @return The list of homes for that owner
     */
    public static ObservableList<Home> getHomes(UUID owner) {
        if (homes.containsKey(owner)) {
            return homes.get(owner);
        }
        
        ObservableList<Home> playerHomes = new ObservableArrayList<>();
        homes.put(owner, playerHomes);
        return playerHomes;
    }
    
    /**
     * Gets a home based on an owner and a name
     *
     * @param owner The owner
     * @param name  The name (case-insensitive)
     * @return An optional that is null if a home does not exist, or the home if found
     */
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
    
    /**
     * Status enum for the Set Home actions
     */
    public enum SetHomeStatus {
        /**
         * The setting of the home was successful
         */
        SUCCESS,
        
        /**
         * The {@link SetHomeEvent} was cancelled
         */
        EVENT_CANCELLED
    }
    
    /**
     * Record for the return info when using set home
     *
     * @param home   The home that is returned. This is never null
     * @param status The status of the action
     */
    public record SetHomeInfo(Home home, SetHomeStatus status) {
    }
    
    /**
     * Sets a home with provided information.
     *
     * @param owner    The owner of the home
     * @param name     The name for the home
     * @param location The location where the home is set
     * @return The information regarding the action
     */
    public static SetHomeInfo setHome(UUID owner, String name, Location location) {
        return setHome(owner, name, location, null);
    }
    
    /**
     * Sets a home with provided information
     *
     * @param owner    The owner of the home
     * @param name     The name for the home
     * @param location The location where the home is set
     * @param actor    The actor that performed the action (Can be null)
     * @return The information regarding the action
     */
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
                    h.setLocation(location);
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
    
    /**
     * Status enum for delete home actions
     */
    public enum DeleteHomeStatus {
        /**
         * The deletion of the home was successful
         */
        SUCCESS,
        
        /**
         * The {@link DeleteHomeEvent} was cancelled
         */
        EVENT_CANCELLED,
        
        /**
         * No home was found with the name provided
         */
        NO_HOME
    }
    
    /**
     * Record for the delete home action information
     *
     * @param home   The home optional. This only exists if the Status is SUCCESS
     * @param name   The name of the home to be deleted or was deleted
     * @param status The status of the action
     */
    public record DeleteHomeInfo(Optional<Home> home, String name, DeleteHomeStatus status) {
    }
    
    /**
     * Deletes a home based on provided values
     *
     * @param owner The owner of the home
     * @param name  The name of the home (case-insensitive)
     * @return The action information
     */
    public static DeleteHomeInfo deleteHome(UUID owner, String name) {
        return deleteHome(owner, name, null);
    }
    
    /**
     * Deletes a home based on provided values
     *
     * @param owner The owner of the home
     * @param name  The name of the home (case-insensitive)
     * @param actor The Actor that performed the action (can be null)
     * @return The action information
     */
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
    
    /**
     * Status enum for rename actions
     */
    public enum RenameHomeStatus {
        /**
         * The rename was successful
         */
        SUCCESS,
        
        /**
         * The {@link RenameHomeEvent} was cancelled
         */
        EVENT_CANCELLED,
        
        /**
         * No home was found with the provided name
         */
        NO_HOME
    }
    
    /**
     * Record for the information relatled to renaming a home
     *
     * @param home    The home that was renamed (This is present if the event cancelled and success status are the ones)
     * @param oldName The old name (This may not be the direct name provided, it is replaced with the actual name if a home is found)
     * @param newName The new name
     * @param status  The status of the action
     */
    public record RenameHomeInfo(Optional<Home> home, String oldName, String newName, RenameHomeStatus status) {
    }
    
    /**
     * Renames a home with provided information
     *
     * @param owner   The owner of the home
     * @param oldName The old name of the home (This might get changed in the info if a home was found and differs in case)
     * @param newName The new name for the home
     * @param actor   The actor the performed the action
     * @return The information related to the action
     */
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
    
    /**
     * Renames a home with provided information
     *
     * @param owner   The owner of the home
     * @param oldName The old name of the home (This might get changed in the info if a home was found and differs in case)
     * @param newName The new name for the home
     * @return Th
     * e information related to the action
     */
    public static RenameHomeInfo renameHome(UUID owner, String oldName, String newName) {
        return renameHome(owner, oldName, newName, null);
    }
}