package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.events.SetHomeEvent;
import com.stardevllc.starmclib.command.StarCommand;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangAPI;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SethomeCommand extends StarCommand<ExtendedJavaPlugin> {
    public SethomeCommand(ExtendedJavaPlugin plugin) {
        super(plugin, "sethome", "Sets a home for yourself or another player", "starhomes.command.sethome");
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
            if (!player.hasPermission("starhomes.command.sethome.others")) {
                plugin.getColors().coloredLegacy(player, "&cYou do not have permission to set homes of other players.");
                return true;
            }
            
            String[] split = args[0].split(":");
            if (split.length != 2) {
                plugin.getColors().coloredLegacy(sender, "&cInvalid format for setting another player's home.");
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
            Home home = StarHomes.setHome(uuid, homeName, player.getLocation());
            
            SetHomeEvent event = new SetHomeEvent(home, player);
            Bukkit.getPluginManager().callEvent(event);
            
            if (event.isCancelled()) {
                StarHomes.deleteHome(uuid, home.getName());
                plugin.getColors().coloredLegacy(player, "&cSetting the home " + home.getName() + " was cancelled.");
                return true;
            }
            
            plugin.getColors().coloredLegacy(player, "&eYou set a home named &b" + home.getName() + " &efor &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &eset a home &b" + home.getName() + "&e.");
            }
            return true;
        }
        String homeName = args[0];
        Home home = StarHomes.setHome(player.getUniqueId(), homeName, player.getLocation());
        
        SetHomeEvent event = new SetHomeEvent(home, player);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            StarHomes.deleteHome(player.getUniqueId(), home.getName());
            plugin.getColors().coloredLegacy(player, "&cSetting the home " + home.getName() + " was cancelled.");
            return true;
        }
        
        plugin.getColors().coloredLegacy(player, "&eYou set a home named &b" + home.getName() + "&e.");
        return true;
    }
}