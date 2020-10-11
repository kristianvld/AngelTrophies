package com.github.kristianvld.angeltrophies;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class StickerManager implements Listener {

    private final List<Skin> skins;
    private final Set<String> stickerItems = new HashSet<>();

    private final Map<UUID, List<String>> lostSkins = new HashMap<>();

    private final File lostSkinsFile;

    public StickerManager(List<Skin> skins) {
        this.skins = new ArrayList<>(skins);
        for (Skin skin : skins) {
            stickerItems.add(skin.getStickerName());
        }
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        Main.getInstance().getDataFolder().mkdirs();
        lostSkinsFile = new File(Main.getInstance().getDataFolder(), "LostSkins.txt");
        if (lostSkinsFile.exists()) {
            try {
                Scanner scanner = new Scanner(lostSkinsFile);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.matches("[0-9a-fA-F-]{36}: [^#]+ #.*")) {
                        String[] args = line.split(" ");
                        UUID uuid = UUID.fromString(args[0].substring(0, 36));
                        List<String> lost = new ArrayList<>(Arrays.asList(args[1].split(",")));
                        lostSkins.put(uuid, lost);
                    }
                }
            } catch (FileNotFoundException e) {
                Main.getInstance().getLogger().log(Level.SEVERE, "Error loading LostSkins file!", e);
            }
        }
    }

    public Skin getSkin(ItemStack item, ItemStack sticker) {
        if (item == null || sticker == null) {
            return null;
        }
        for (Skin skin : skins) {
            if (skin.matchesSource(item) && skin.matchesSticker(sticker)) {
                return skin;
            }
        }
        return null;
    }

    public Skin getSkin(ItemStack target) {
        if (target == null) {
            return null;
        }
        for (Skin skin : skins) {
            if (skin.matchesTarget(target)) {
                return skin;
            }
        }
        return null;
    }

    public void merge(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemStack sticker = player.getInventory().getItemInOffHand();
        Skin skin = getSkin(item, sticker);
        if (skin == null) {
            C.error(player, "You need to hold a skin in your offhand, \nand an item to apply it to in your other hand.");
            return;
        }
        ItemStack[] items = skin.apply(item, sticker);
        player.getInventory().setItemInMainHand(items[0]);
        player.getInventory().setItemInOffHand(items[1]);
        player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 0.6f);
    }

    public void split(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (player.getInventory().getItemInOffHand().getType() != Material.AIR) {
            C.error(player, "You need to clear your offhand first.");
            return;
        }
        Skin skin = getSkin(item);
        if (skin == null) {
            C.error(player, "The item in your main hand is not skinned.");
            return;
        }
        ItemStack[] items = skin.split(item);
        player.getInventory().setItemInMainHand(items[0]);
        player.getInventory().setItemInOffHand(items[1]);
        player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 0.6f);
    }

    private void saveLostItems() {
        Map<String[], List<String>> map = new HashMap<>();
        for (Map.Entry<UUID, List<String>> e : lostSkins.entrySet()) {
            map.put(new String[]{e.getKey() + "", Bukkit.getOfflinePlayer(e.getKey()).getName()}, new ArrayList<>(e.getValue()));
        }
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            synchronized (lostSkinsFile) {
                try {
                    PrintWriter writer = new PrintWriter(lostSkinsFile);
                    for (Map.Entry<String[], List<String>> e : map.entrySet()) {
                        String skins = String.join(",", e.getValue());
                        writer.println(e.getKey()[0] + ": " + skins + " #" + e.getKey()[1]);
                    }
                    writer.flush();
                    writer.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeath(PlayerDeathEvent event) {
        boolean save = false;
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            String stickerName = OraxenItems.getIdByItem(drops.get(i));
            if (stickerItems.contains(stickerName)) {
                List<String> lost = lostSkins.computeIfAbsent(event.getEntity().getUniqueId(), u -> new ArrayList<>());
                for (int ii = 0; ii < drops.get(i).getAmount(); ii++) {
                    lost.add(stickerName);
                }
                save = true;
                drops.remove(i);
                i--;
                continue;
            }

            Skin skin;
            ItemStack item;
            int maxLoop = 1000;
            while ((skin = getSkin(item = drops.get(i))) != null) {
                List<String> lost = lostSkins.computeIfAbsent(event.getEntity().getUniqueId(), u -> new ArrayList<>());
                ItemStack[] items = skin.split(item);
                drops.set(i, items[0]);
                lost.add(skin.getStickerName());
                save = true;
                maxLoop--;
                if (maxLoop == 0) {
                    break;
                }
            }
        }
        if (save) {
            saveLostItems();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        notifyLostItems(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        notifyLostItems(event.getPlayer());
    }

    public void notifyLostItems(Player player) {
        List<String> skins = lostSkins.get(player.getUniqueId());
        if (skins == null || skins.isEmpty()) {
            return;
        }
        C.main(player, "It seems you were unfortunate enough to die while\n" +
                "carrying some skinned items. I have taken care of\n" +
                "the items for you in the mean time.\n" +
                "Use {0} to get your skins back.", "/skin lost");
    }

    public void claimLostSkins(Player player) {
        List<String> skins = lostSkins.get(player.getUniqueId());
        if (skins == null || skins.isEmpty()) {
            C.error(player, "You have not lost any skins lately.");
            return;
        }
        boolean given = false;
        while (!skins.isEmpty()) {
            if (!player.getInventory().addItem(OraxenItems.getItemById(skins.get(0)).build()).isEmpty()) {
                break;
            }
            given = true;
            skins.remove(0);
        }
        if (skins.isEmpty()) {
            lostSkins.remove(player.getUniqueId());
        } else {
            C.error(player, "Unable to give you back all your items. Please\n" +
                    "clear some more space in your inventory first.");
        }
        if (given) {
            player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 0.7f);
            saveLostItems();
        }
    }

}
