package com.github.kristianvld.angeltrophies;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHandler implements CommandExecutor, TabCompleter {

    public CommandHandler() {
        PluginCommand cmd = Main.getInstance().getCommand("skin");
        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            C.main(sender, "Commands:\n" +
                    "  {0} - Apply a skin to an item\n" +
                    "  {1} - Remove a skin from an item\n" +
                    "  {2} - Retrieve your skins if you die while carrying them", "merge", "split", "lost");
            return true;
        } else if (!(sender instanceof Player)) {
            C.error(sender, "You need to be a player to use that command.");
            return true;
        }
        Player player = (Player) sender;
        if (args[0].equalsIgnoreCase("merge")) {
            Main.getInstance().getStickerManager().merge(player);
        } else if (args[0].equalsIgnoreCase("split")) {
            Main.getInstance().getStickerManager().split(player);
        } else if (args[0].equalsIgnoreCase("lost")) {
            Main.getInstance().getStickerManager().claimLostSkins(player);
        } else {
            C.error(player, "Unknown subcommand {0}", args[0]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Stream<String> out = null;
        if (args.length == 1) {
            out = Stream.of("merge", "split", "lost");
        }
        if (out == null) {
            return Collections.emptyList();
        }
        String pref = args[args.length - 1].toLowerCase();
        return out.filter(s -> s.toLowerCase().startsWith(pref)).sorted().collect(Collectors.toList());
    }
}
