package com.stardevllc.starhomes.commands;

import com.stardevllc.starhomes.Home;
import com.stardevllc.starhomes.StarHomes;
import com.stardevllc.starhomes.StarHomes.RenameHomeInfo;
import com.stardevllc.starhomes.StarHomes.RenameHomeStatus;
import com.stardevllc.starhomes.events.RenameHomeEvent;
import com.stardevllc.starmclib.actors.Actors;
import com.stardevllc.starmclib.command.flags.FlagResult;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class RenameHomeCommand extends BaseCommand {
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
            OtherInfo otherInfo = getOtherPlayerHome(player, args[0], "starhomes.command.renamehome.others", "&cYou do not have permission to rename homes of other players.");
            if (otherInfo == null) {
                return true;
            }
            
            MojangProfile profile = otherInfo.profile();
            UUID uuid = profile.getUniqueId();
            
            RenameHomeInfo renameHomeInfo = StarHomes.renameHome(uuid, otherInfo.homeName(), args[1], Actors.of(player));
            
            if (renameHomeInfo.status() == RenameHomeStatus.NO_HOME) {
                plugin.getColors().coloredLegacy(player, "&cNo home by the name " + renameHomeInfo.oldName() + " exists for " + profile.getName() + ".");
                return true;
            }
            
            if (renameHomeInfo.status() == RenameHomeStatus.EVENT_CANCELLED) {
                plugin.getColors().coloredLegacy(player, "&cRenaming the home " + renameHomeInfo.oldName() + " was cancelled.");
                return true;
            }
            
            Optional<Home> homeOpt = renameHomeInfo.home();
            if (homeOpt.isEmpty()) {
                plugin.getLogger().severe("Rename Home returned success, but the home value was not present. Please report as a bug");
                return true;
            }
            
            Home home = homeOpt.get();
            
            plugin.getColors().coloredLegacy(player, "&eYou renamed the home &b" + renameHomeInfo.oldName() + " &eto &b" + home.getName() + " &efor &b" + profile.getName() + "&e.");
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                plugin.getColors().coloredLegacy(p, "&b" + player.getName() + " &erenamed the home &b" + renameHomeInfo.oldName()+ " &eto &b" + home.getName() + "&e.");
            }
            return true;
        }
        RenameHomeInfo renameHomeInfo = StarHomes.renameHome(player.getUniqueId(), args[0], args[1], Actors.of(player));
        
        if (renameHomeInfo.status() == RenameHomeStatus.NO_HOME) {
            plugin.getColors().coloredLegacy(player, "&cYou do not have a home named " + renameHomeInfo.oldName() + ".");
            return true;
        }
        
        if (renameHomeInfo.status() == RenameHomeStatus.EVENT_CANCELLED) {
            plugin.getColors().coloredLegacy(player, "&cRenaming the home " + renameHomeInfo.oldName() + " was cancelled.");
            return true;
        }
        
        Optional<Home> homeOpt = renameHomeInfo.home();
        if (homeOpt.isEmpty()) {
            plugin.getLogger().severe("Rename Home returned success, but the home value was not present. Please report as a bug");
            return true;
        }
        
        Home home = homeOpt.get();
        
        plugin.getColors().coloredLegacy(player, "&eYou renamed the home &b" + renameHomeInfo.oldName() + " &eto &b" + home.getName() + "&e.");
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