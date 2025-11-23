package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.events.DeleteHomeEvent;
import com.stardevllc.starmclib.command.StarCommand;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangAPI;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class DelhomeCommand extends StarCommand<ExtendedJavaPlugin> {
    public DelhomeCommand(ExtendedJavaPlugin plugin) {
        super(plugin, "delhome", "Deletes a home from yourself or another player", "starhomes.command.delhome");
        this.playerOnly = true;
        this.playerOnlyMessage = plugin.getColors().colorLegacy("&cOnly players can use that command");
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args, FlagResult flagResults) {
        if (!(args.length > 0)) {
            plugin.getColors().coloredLegacy(sender, "&cYou must provide a home name.");
            return true;
        }
        
        Player player = (Player) sender;
        if (args[0].contains(":")) {
            if (!player.hasPermission("starhomes.command.delhome.others")) {
                plugin.getColors().coloredLegacy(player, "&cYou do not have permission to delete homes of other players.");
                return true;
            }
            
            String[] split = args[0].split(":");
            if (split.length != 2) {
                plugin.getColors().coloredLegacy(sender, "&cInvalid format for deleting another player's home.");
                return true;
            }
            String playerName = split[0];
            MojangProfile profile = MojangAPI.getProfile(playerName);
            if (profile == null) {
                plugin.getColors().coloredLegacy(player, "&cInvalid player name");
                return true;
            }
            
            UUID uuid = profile.getUniqueId();
            String homeName = split[1];
            Optional<Home> homeOpt = StarHomes.deleteHome(uuid, homeName);
            
            if (homeOpt.isEmpty()) {
                plugin.getColors().coloredLegacy(player, "&cNo home by the name " + homeName + " exists for player " + playerName + ".");
                return true;
            }
            
            Home home = homeOpt.get();
            
            DeleteHomeEvent event = new DeleteHomeEvent(home, player);
            Bukkit.getPluginManager().callEvent(event);
            
            if (event.isCancelled()) {
                StarHomes.setHome(home);
                plugin.getColors().coloredLegacy(player, "&cDeleting the home " + home.getName() + " was cancelled.");
                return true;
            }
            
            plugin.getColors().coloredLegacy(player, "&eYou deleted the home named &b" + home.getName() + " &efrom &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &edeleted the home &b" + home.getName() + "&e.");
            }
            return true;
        }
        
        String homeName = args[0];
        Optional<Home> homeOpt = StarHomes.deleteHome(player.getUniqueId(), homeName);
        
        if (homeOpt.isEmpty()) {
            plugin.getColors().coloredLegacy(player, "&cYou don't have a home named " + homeName + ".");
            return true;
        }
        
        Home home = homeOpt.get();
        
        DeleteHomeEvent event = new DeleteHomeEvent(home, player);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            StarHomes.setHome(home);
            plugin.getColors().coloredLegacy(player, "&cDeleting the home " + home.getName() + " was cancelled.");
            return true;
        }
        
        plugin.getColors().coloredLegacy(player, "&eYou deleted the home named &b" + home.getName() + "&e.");
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