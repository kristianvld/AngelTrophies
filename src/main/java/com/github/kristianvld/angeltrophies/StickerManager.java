package com.github.kristianvld.angeltrophies;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.SmithingInventory;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class StickerManager implements Listener {

    private final List<Skin> skins;
    private final Set<String> stickerItems = new HashSet<>();

    private final Map<UUID, List<String>> lostSkins = new HashMap<>();
    private final File lostSkinsFile;

    private final Map<UUID, Long> grinding = new HashMap<>();
    private final long GRINDING_TIME = 1000;

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
            C.error(player, "You need to hold a skin in your offhand, and an\nitem to apply it to in your other hand.");
            return;
        }
        ItemStack[] items = skin.apply(item, sticker);
        player.getInventory().setItemInMainHand(items[0]);
        player.getInventory().setItemInOffHand(items[1]);
        player.playSound(player.getEyeLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 0.6f);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMerge(PrepareSmithingEvent event) {
        ItemStack item = event.getInventory().getItem(0);
        ItemStack sticker = event.getInventory().getItem(1);
        Skin skin = getSkin(item, sticker);
        if (skin == null) {
            return;
        }
        ItemStack[] items = skin.apply(item.clone(), sticker.clone());
        event.setResult(items[0]);
    }

    private ItemStack canStack(ItemStack a, ItemStack b) {
        if (b == null || b.getType() == Material.AIR) {
            return a;
        }
        ItemStack A = a.clone();
        ItemStack B = b.clone();
        A.setAmount(1);
        B.setAmount(1);
        if (A.equals(B)) {
            if (a.getAmount() + b.getAmount() <= a.getMaxStackSize()) {
                A.setAmount(a.getAmount() + b.getAmount());
                return A;
            }
        }
        return null;
    }

    @EventHandler
    public void onMerge(InventoryClickEvent event) {
        if (!(event.getClickedInventory() instanceof SmithingInventory)) {
            return;
        }
        if (event.getSlot() != 2) {
            return;
        }
        SmithingInventory inv = (SmithingInventory) event.getClickedInventory();
        ItemStack item = inv.getItem(0);
        ItemStack sticker = inv.getItem(1);
        Skin skin = getSkin(item, sticker);
        if (skin == null) {
            return;
        }
        ItemStack[] items = skin.apply(item.clone(), sticker.clone());
        ItemStack target;
        switch (event.getClick()) {
            case LEFT:
            case RIGHT:
                target = canStack(items[0], event.getCursor());
                if (target == null) {
                    return;
                }
                inv.setItem(0, null);
                inv.setItem(1, items[1]);
                event.setCursor(target);
                inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1, 1);
                event.setResult(Event.Result.ALLOW);
                break;
            case DROP:
            case CONTROL_DROP:
                target = canStack(items[0], event.getCursor());
                if (target == null) {
                    return;
                }
                inv.setItem(0, null);
                inv.setItem(1, items[1]);
                Item drop = event.getWhoClicked().getWorld().dropItem(event.getWhoClicked().getEyeLocation(), target);
                drop.setVelocity(event.getWhoClicked().getEyeLocation().getDirection().normalize().multiply(0.3));
                inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1, 1);
                event.setResult(Event.Result.ALLOW);
                break;
            case MIDDLE:
            case UNKNOWN:
            case DOUBLE_CLICK:
            case WINDOW_BORDER_LEFT:
            case WINDOW_BORDER_RIGHT:
                return;
            case CREATIVE:
                target = canStack(items[0], event.getCursor());
                if (target == null) {
                    return;
                }
                event.setCursor(target);
                inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1, 1);
                event.setResult(Event.Result.ALLOW);
                break;
            case SWAP_OFFHAND:
                target = canStack(items[0], event.getWhoClicked().getEquipment().getItemInOffHand());
                if (target == null) {
                    return;
                }
                inv.setItem(0, null);
                inv.setItem(1, items[1]);
                event.getWhoClicked().getEquipment().setItemInOffHand(target);
                inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1, 1);
                event.setResult(Event.Result.ALLOW);
                break;
            case NUMBER_KEY:
                int playerSlot = event.getHotbarButton();
                target = canStack(items[0], event.getWhoClicked().getInventory().getItem(playerSlot));
                if (target == null) {
                    return;
                }
                inv.setItem(0, null);
                inv.setItem(1, items[1]);
                event.getWhoClicked().getInventory().setItem(playerSlot, target);
                inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1, 1);
                event.setResult(Event.Result.ALLOW);
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                for (int i = 0; i < event.getWhoClicked().getInventory().getSize(); i++) {
                    target = canStack(items[0], event.getWhoClicked().getInventory().getItem(i));
                    if (target != null) {
                        inv.setItem(0, null);
                        inv.setItem(1, items[1]);
                        event.getWhoClicked().getInventory().setItem(i, target);
                        inv.getLocation().getWorld().playSound(inv.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1, 1);
                        event.setResult(Event.Result.ALLOW);
                        if (i >= 9 && event.getWhoClicked() instanceof Player) {
                            ((Player) event.getWhoClicked()).updateInventory(); //TODO: For some reason items within the inventory (not hotbar) will not update unless we do it manually. This should be removed if we can to reduce unnecessary traffic towards the client.
                        }
                        break;
                    }
                }
                break;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClickSplitGrindstone(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !event.getPlayer().isSneaking() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        Skin skin = getSkin(item);
        if (skin == null) {
            return;
        }
        ItemStack[] items = skin.split(item.clone());
        ItemStack offhand = canStack(items[1], event.getPlayer().getInventory().getItemInOffHand());
        if (offhand == null) {
            C.error(event.getPlayer(), "You need to clear your offhand first to grind your\nskinned item.");
            return;
        }
        long grindingUntil = grinding.getOrDefault(event.getPlayer().getUniqueId(), 0L);
        grinding.put(event.getPlayer().getUniqueId(), System.currentTimeMillis() + GRINDING_TIME);
        Location loc = event.getClickedBlock().getLocation();
        loc.add(0.5, 1.1, 0.5);
        int amount = 5;
        double xz = 0.3;
        double y = 0;
        double speed = 0;
        if (grindingUntil >= System.currentTimeMillis()) {
            amount = 2;
            loc.getWorld().spawnParticle(Particle.CRIT, loc, amount, xz, y, xz, speed);
            return;
        }

        loc.getWorld().playSound(loc, Sound.BLOCK_GRINDSTONE_USE, 0.7f, 1f);
        loc.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, loc, amount, xz, y, xz, speed);

        double percentage = 0.05;
        if (ThreadLocalRandom.current().nextDouble() <= percentage) {
            split(event.getPlayer());
        }
    }

    public void split(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        Skin skin = getSkin(item);
        if (skin == null) {
            C.error(player, "The item in your main hand is not skinned.");
            return;
        }
        ItemStack[] items = skin.split(item.clone());
        ItemStack offhand = canStack(items[1], player.getInventory().getItemInOffHand());
        if (offhand == null) {
            C.error(player, "You need to clear your offhand first.");
            return;
        }
        player.getInventory().setItemInMainHand(items[0]);
        player.getInventory().setItemInOffHand(offhand);
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
        if (!player.hasPermission(CommandHandler.lostPermission)) {
            return;
        }
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
