package com.github.kristianvld.angeltrophies;

import com.github.kristianvld.angeltrophies.util.C;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    public final static String reloadPermission = "angeltrophies.reload";
    public final static String mergePermission = "angeltrophies.merge";
    public final static String splitPermission = "angeltrophies.merge";
    public final static String lostPermission = "angeltrophies.merge";
    public final static String permissionDeniedMessage = "You do not have permission to use that command.";

    public CommandHandler() {
        PluginCommand cmd = Main.getInstance().getCommand("skin");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            C.main(sender, "Commands:\n" +
                    " {0} - Apply a skin to an item\n" +
                    " {1} - Remove a skin from an item\n" +
                    " {2} - Retrieve your skins if you die while\ncarrying them", "merge", "split", "lost");
            if (sender.hasPermission(reloadPermission)) {
                C.main(sender, " {0} - Reloads the config files", "reload");
            }
            return true;
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (hasPermission(sender, reloadPermission)) {
                Main.getInstance().reload(sender);
            }
            return true;
        } else if (!(sender instanceof Player)) {
            C.error(sender, "You need to be a player to use that command.");
            return true;
        }
        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("merge")) {
            if (hasPermission(sender, mergePermission)) {
                Main.getInstance().getStickerManager().merge(player);
            }
        } else if (args[0].equalsIgnoreCase("split")) {
            if (hasPermission(sender, splitPermission)) {
                Main.getInstance().getStickerManager().split(player);
            }
        } else if (args[0].equalsIgnoreCase("lost")) {
            if (hasPermission(sender, lostPermission)) {
                Main.getInstance().getStickerManager().claimLostSkins(player);
            }
        } else {
            C.error(player, "Unknown subcommand {0}", args[0]);
        }
        return true;
    }

    public static boolean hasPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            C.error(sender, permissionDeniedMessage);
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = null;
        if (args.length == 1) {
            out = new ArrayList<>(Arrays.asList("merge", "split", "lost"));
            if (sender.hasPermission(reloadPermission)) {
                out.add("reload");
            }
        }
        if (out == null) {
            return Collections.emptyList();
        }
        String pref = args[args.length - 1].toLowerCase();
        return out.stream().filter(s -> s.toLowerCase().startsWith(pref)).sorted().collect(Collectors.toList());
    }
}
