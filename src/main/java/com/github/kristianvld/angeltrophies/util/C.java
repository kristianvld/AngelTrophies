package com.github.kristianvld.angeltrophies.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.text.MessageFormat;

public class C {

    public static final String colorBodyMain = ChatColor.GRAY + "";
    public static final String colorBodyError = ChatColor.GRAY + "";
    public static final String colorError = ChatColor.RED + "";
    public static final String colorItem = ChatColor.YELLOW + "";

    public static final String prefixMain = colorBodyMain + "[" + ChatColor.AQUA + "Trophies" + colorBodyMain + "] ";
    public static final String prefixError = colorError + "[" + ChatColor.AQUA + "Trophies" + colorError + "] " + colorBodyMain;

    public static String prefix(String prefix, String colorBody, String msg, Object... objs) {
        for (int i = 0; i < objs.length; i++) {
            if (objs[i] instanceof Number && ((Number) objs[i]).doubleValue() % 1 == 0) {
                objs[i] = ((Number) objs[i]).intValue();
            }
        }
        msg = msg.replace("\\\\", "\0").replace("\\n", "\n").replace("\0", "\\");
        msg = msg.replaceAll("((?:§[a-f0-9k-or])*\\{\\d+(?:|,[^}]+)})", colorItem + "$1" + colorBody);
        msg = MessageFormat.format(msg, objs);
        return prefix + msg.replaceAll("\n", "\n" + prefix);
    }

    public static void main(CommandSender receiver, String msg, Object... objs) {
        String message = prefix(prefixMain, colorBodyMain, msg, objs);
        receiver.sendMessage(message);
    }

    public static void error(CommandSender receiver, String msg, Object... objs) {
        String message = prefix(prefixError, colorBodyError, msg, objs);
        receiver.sendMessage(message);
    }
}
