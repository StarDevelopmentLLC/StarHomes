package com.stardevllc.starhomes.commands;

import com.stardevllc.starmclib.command.StarCommand;
import com.stardevllc.starmclib.mojang.MojangAPI;
import com.stardevllc.starmclib.mojang.MojangProfile;
import com.stardevllc.starmclib.plugin.ExtendedJavaPlugin;
import org.bukkit.entity.Player;

public abstract class BaseCommand extends StarCommand<ExtendedJavaPlugin> {
    public BaseCommand(ExtendedJavaPlugin plugin, String name, String description, String permission, String... aliases) {
        super(plugin, name, description, permission, aliases);
    }
    
    protected OtherInfo getOtherPlayerHome(Player player, String arg, String othersPermission, String noPermMessage) {
        if (!player.hasPermission(othersPermission)) {
            plugin.getColors().coloredLegacy(player, noPermMessage);
            return null;
        }
        
        String[] split = arg.split(":");
        if (split.length != 2) {
            plugin.getColors().coloredLegacy(player, "&cInvalid format for deleting another player's home.");
            return null;
        }
        String playerName = split[0];
        MojangProfile profile = MojangAPI.getProfile(playerName);
        if (profile == null) {
            plugin.getColors().coloredLegacy(player, "&cInvalid player name");
            return null;
        }
        
        return new OtherInfo(profile, split[1]);
    }
    
    protected record OtherInfo(MojangProfile profile, String homeName) {
    }
}
