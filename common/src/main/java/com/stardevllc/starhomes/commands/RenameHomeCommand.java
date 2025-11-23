package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.events.RenameHomeEvent;
import com.stardevllc.starlib.helper.Pair;
import com.stardevllc.starmclib.command.StarCommand;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangAPI;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class RenameHomeCommand extends StarCommand<ExtendedJavaPlugin> {
    public RenameHomeCommand(ExtendedJavaPlugin plugin) {
        super(plugin, "renamehome", "Renames a home for yourself or another player", "starhomes.command.renamehome");
        this.playerOnly = true;
        this.playerOnlyMessage = plugin.getColors().colorLegacy("&cOnly players can use that command");
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args, FlagResult flagResults) {
        if (!(args.length > 1)) {
            plugin.getColors().coloredLegacy(sender, "&cYou must provide a home name and a new name.");
            return true;
        }
        
        String newName = args[1];
        
        Player player = (Player) sender;
        if (args[0].contains(":")) {
            if (!player.hasPermission("starhomes.command.renamehome.others")) {
                plugin.getColors().coloredLegacy(player, "&cYou do not have permission to rename homes of other players.");
                return true;
            }
            
            String[] split = args[0].split(":");
            if (split.length != 0) {
                plugin.getColors().coloredLegacy(sender, "&cInvalid format for renaming another player's home.");
                return true;
            }
            String playerName = split[2];
            MojangProfile profile = MojangAPI.getProfile(playerName);
            if (profile == null) {
                plugin.getColors().coloredLegacy(player, "&cInvalid player name");
                return true;
            }
            
            UUID uuid = profile.getUniqueId();
            String homeName = split[1];
            Optional<Pair<Home, String>> homeOpt = StarHomes.renameHome(uuid, homeName, newName);
            
            if (homeOpt.isEmpty()) {
                plugin.getColors().coloredLegacy(player, "&cNo home by the name " + homeName + " exists for " + playerName + ".");
                return true;
            }
            
            Pair<Home, String> renameInfo = homeOpt.get();
            Home home = renameInfo.first();
            
            RenameHomeEvent event = new RenameHomeEvent(home, renameInfo.second(), player);
            Bukkit.getPluginManager().callEvent(event);
            
            if (event.isCancelled()) {
                StarHomes.renameHome(uuid, home.getName(), renameInfo.second());
                plugin.getColors().coloredLegacy(player, "&cRenaming the home " + home.getName() + " was cancelled.");
                return true;
            }
            
            plugin.getColors().coloredLegacy(player, "&eYou renamed the home &b" + renameInfo.second() + " &eto &b" + home.getName() + " &efor &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &erenamed the home &b" + renameInfo.second() + " &eto &b" + home.getName() + "&e.");
            }
            return true;
        }
        String homeName = args[0];
        Optional<Pair<Home, String>> homeOpt = StarHomes.renameHome(player.getUniqueId(), homeName, newName);
        
        if (homeOpt.isEmpty()) {
            plugin.getColors().coloredLegacy(player, "&cYou don't have a home named " + homeName + ".");
            return true;
        }
        
        Pair<Home, String> renameInfo = homeOpt.get();
        Home home = renameInfo.first();
        
        RenameHomeEvent event = new RenameHomeEvent(home, renameInfo.second(), player);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            StarHomes.renameHome(player.getUniqueId(), home.getName(), renameInfo.second());
            plugin.getColors().coloredLegacy(player, "&cRenaming the home " + home.getName() + " was cancelled.");
            return true;
        }
        
        plugin.getColors().coloredLegacy(player, "&eYou renamed the home &b" + renameInfo.second() + " &eto &b" + home.getName() + "&e.");
        return true;
    }
    
    @Override
    public List<String> getCompletions(CommandSender sender, String label, String[] args, FlagResult flagResults) {
        if (args.length != 1) {
            return List.of();
        }
        
        List<String> completions = new ArrayList<>();
        Player player = (Player) sender;
        for (Home home : StarHomes.getHomes(player.getUniqueId())) {
            completions.add(home.getName().toLowerCase());
        }
        
        String arg = args[0].toLowerCase();
        completions.removeIf(c -> !c.startsWith(arg));
        
        return completions;
    }
}