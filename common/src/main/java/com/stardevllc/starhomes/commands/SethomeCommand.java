package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.StarHomes.SetHomeInfo;
import com.stardevllc.starhomes.StarHomes.SetHomeStatus;
import com.stardevllc.starmclib.actors.Actors;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SethomeCommand extends BaseCommand {
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
            OtherInfo otherInfo = getOtherPlayerHome(player, args[0], "starhomes.command.sethome.others", "&cYou do not have permission to set homes of other players.");
            if (otherInfo == null) {
                return true;
            }
            
            MojangProfile profile = otherInfo.profile();
            UUID uuid = profile.getUniqueId();
            
            SetHomeInfo setHomeInfo = StarHomes.setHome(uuid, otherInfo.homeName(), player.getLocation(), Actors.of(player));
            
            if (setHomeInfo.status() == SetHomeStatus.EVENT_CANCELLED) {
                plugin.getColors().coloredLegacy(player, "&cSetting the home " + otherInfo.homeName() + " was cancelled.");
                return true;
            }
            
            Home home = setHomeInfo.home();
            
            plugin.getColors().coloredLegacy(player, "&eYou set a home named &b" + home.getName() + " &efor &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &eset a home &b" + home.getName() + "&e.");
            }
            return true;
        }
        
        SetHomeInfo setHomeInfo = StarHomes.setHome(player.getUniqueId(), args[0], player.getLocation(), Actors.of(player));
        
        if (setHomeInfo.status() == SetHomeStatus.EVENT_CANCELLED) {
            plugin.getColors().coloredLegacy(player, "&cSetting the home " + args[0] + " was cancelled.");
            return true;
        }
        
        Home home = setHomeInfo.home();
        
        plugin.getColors().coloredLegacy(player, "&eYou set a home named &b" + home.getName() + "&e.");
        return true;
    }
}