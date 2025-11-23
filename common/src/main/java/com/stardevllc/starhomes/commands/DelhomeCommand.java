package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.StarHomes.DeleteHomeInfo;
import com.stardevllc.starhomes.StarHomes.DeleteHomeStatus;
import com.stardevllc.starmclib.actors.Actors;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class DelhomeCommand extends BaseCommand {
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
            OtherInfo otherInfo = getOtherPlayerHome(player, args[0], "starhomes.command.delhome.others", "&cYou do not have permission to delete homes of other players.");
            if (otherInfo == null) {
                return true;
            }
            
            MojangProfile profile = otherInfo.profile();
            
            UUID uuid = profile.getUniqueId();
            String homeName = otherInfo.homeName();
            
            DeleteHomeInfo deleteHomeInfo = StarHomes.deleteHome(uuid, homeName, Actors.of(player));
            
            if (deleteHomeInfo.status() == DeleteHomeStatus.NO_HOME) {
                plugin.getColors().coloredLegacy(player, "&cNo home by the name " + homeName + " exists for player " + profile.getName() + ".");
                return true;
            }
            
            if (deleteHomeInfo.status() == DeleteHomeStatus.EVENT_CANCELLED) {
                plugin.getColors().coloredLegacy(player, "&cDeleting the home " + deleteHomeInfo.name() + " was cancelled.");
                return true;
            }
            
            Optional<Home> homeOpt = deleteHomeInfo.home();
            
            if (homeOpt.isEmpty()) {
                plugin.getColors().coloredLegacy(player, "&cDeleting the home " + deleteHomeInfo.name() + " failed. Please report to the plugin author as a bug.");
                return true;
            }
            
            Home home = homeOpt.get();
            
            plugin.getColors().coloredLegacy(player, "&eYou deleted the home named &b" + home.getName() + " &efrom &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &edeleted the home &b" + home.getName() + "&e.");
            }
            return true;
        }
        
        String homeName = args[0];
        DeleteHomeInfo deleteHomeInfo = StarHomes.deleteHome(player.getUniqueId(), homeName, Actors.of(player));
        
        if (deleteHomeInfo.status() == DeleteHomeStatus.NO_HOME) {
            plugin.getColors().coloredLegacy(player, "&cYou do not have a home named " + homeName + ".");
            return true;
        }
        
        if (deleteHomeInfo.status() == DeleteHomeStatus.EVENT_CANCELLED) {
            plugin.getColors().coloredLegacy(player, "&cDeleting the home " + deleteHomeInfo.name() + " was cancelled.");
            return true;
        }
        
        Optional<Home> homeOpt = deleteHomeInfo.home();
        
        if (homeOpt.isEmpty()) {
            plugin.getColors().coloredLegacy(player, "&cDeleting the home " + deleteHomeInfo.name() + " failed. Please report to the plugin author as a bug.");
            return true;
        }
        
        Home home = homeOpt.get();
        
        plugin.getColors().coloredLegacy(player, "&eYou deleted the home &b" + home.getName() + "&e.");
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