package com.stardevllc.starhomes;

import com.stardevllc.config.Section;
import com.stardevllc.config.file.FileConfig;
import com.stardevllc.config.file.yaml.YamlConfig;
import com.stardevllc.starlib.helper.Pair;
import com.stardevllc.starlib.observable.collections.list.ObservableArrayList;
import com.stardevllc.starlib.observable.collections.list.ObservableList;
import com.stardevllc.starlib.observable.collections.map.ObservableHashMap;
import com.stardevllc.starlib.observable.collections.map.ObservableMap;
import com.stardevllc.starmclib.Position;
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
        plugin.getMainConfig().addDefault("homes.singlefile.name", "homes.yml", "This is just the name of the file when using SINGLEFILE storage mode");
        plugin.getMainConfig().addDefault("homes.separatefiles.foldername", "homes", "This is the name of the folder for the home file locations when using SEPARATEFILES mode");
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
    
    public static Home setHome(Home home) {
        UUID owner = home.getOwner();
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            homes.add(home);
            return home;
        }
        
        for (Home h : homes) {
            if (h.getName().equalsIgnoreCase(home.getName())) {
                h.setPosition(home.getPosition());
                h.setWorldName(home.getWorldName());
                return h;
            }
        }
        
        homes.add(home);
        return home;
    }
    
    public static Home setHome(UUID owner, String name, Location location) {
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            Home home = new Home(owner, name, location);
            homes.add(home);
            return home;
        }
        
        for (Home home : homes) {
            if (home.getName().equalsIgnoreCase(name)) {
                home.setPosition(location);
                return home;
            }
        }
        
        Home home = new Home(owner, name, location);
        homes.add(home);
        return home;
    }
    
    public static Optional<Home> deleteHome(UUID owner, String name) {
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            return Optional.empty();
        }
        
        Iterator<Home> iterator = homes.iterator();
        while (iterator.hasNext()) {
            Home home = iterator.next();
            if (home.getName().equalsIgnoreCase(name)) {
                iterator.remove();
                return Optional.of(home);
            }
        }
        
        return Optional.empty();
    }
    
    public static Optional<Pair<Home, String>> renameHome(UUID owner, String oldName, String newName) {
        ObservableList<Home> homes = getHomes(owner);
        if (homes.isEmpty()) {
            return Optional.empty();
        }
        
        for (Home home : homes) {
            if (home.getName().equalsIgnoreCase(oldName)) {
                String existingName = home.getName();
                home.setName(newName);
                return Optional.of(new Pair<>(home, existingName));
            }
        }
        
        return Optional.empty();
    }
}