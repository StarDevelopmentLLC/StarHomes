package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starlib.observable.collections.list.ObservableList;
import com.stardevllc.starmclib.command.StarCommand;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangAPI;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class HomeCommand extends StarCommand<ExtendedJavaPlugin> {
    public HomeCommand(ExtendedJavaPlugin plugin) {
        super(plugin, "home", "Teleports a home that you have or another player has", "starhomes.command.home");
        this.playerOnly = true;
        this.playerOnlyMessage = plugin.getColors().colorLegacy("&cOnly players can use that command");
    }
    
    @Override
    public boolean execute(CommandSender sender, String label, String[] args, FlagResult flagResults) {
        Player player = (Player) sender;
        if (args.length == 0) {
            ObservableList<Home> homes = StarHomes.getHomes(player.getUniqueId());
            StringBuilder sb = new StringBuilder();
            for (Home home : homes) {
                sb.append("&b").append(home.getName().toLowerCase()).append(" ");
            }
            
            plugin.getColors().coloredLegacy(player, "&eHomes: " + sb.toString().trim());
            return true;
        }
        
        if (args[0].contains(":")) {
            if (!player.hasPermission("starhomes.command.home.others")) {
                plugin.getColors().coloredLegacy(player, "&cYou do not have permission to teleport to other players homes.");
                return true;
            }
            
            String[] split = args[0].split(":");
            if (split.length != 2) {
                plugin.getColors().coloredLegacy(sender, "&cInvalid format for teleport to another player's home.");
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
            Optional<Home> homeOpt = StarHomes.getHome(uuid, homeName);
            
            if (homeOpt.isEmpty()) {
                plugin.getColors().coloredLegacy(player, "&cNo home by the name " + homeName + " exists for player " + playerName + ".");
                return true;
            }
            
            Home home = homeOpt.get();
            home.teleport(player);
            
            plugin.getColors().coloredLegacy(player, "&eYou teleported to the home named &b" + home.getName() + " &eowned by &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (p.hasPermission("starhomes.command.home.others.notify")) {
                    plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &eteleported to your home &b" + home.getName() + "&e.");
                }
            }
            return true;
        }
        
        String homeName = args[0];
        Optional<Home> homeOpt = StarHomes.getHome(player.getUniqueId(), homeName);
        
        if (homeOpt.isEmpty()) {
            plugin.getColors().coloredLegacy(player, "&cYou don't have a home named " + homeName + ".");
            return true;
        }
        
        Home home = homeOpt.get();
        home.teleport(player);
        
        plugin.getColors().coloredLegacy(player, "&eYou teleported to your home named &b" + home.getName() + "&e.");
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