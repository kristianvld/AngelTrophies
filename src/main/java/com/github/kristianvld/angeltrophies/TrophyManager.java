package com.github.kristianvld.angeltrophies;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TrophyManager implements Listener {

    private final List<Trophy> trophies;

    private final Map<UUID, Long> sneaking = new HashMap<>();
    private static final long SITTING_TIMEOUT = 10;

    private final Map<UUID, Long> dismount = new HashMap<>();
    private static final long DISMOUNT_TIMEOUT = 20;

    private final Map<UUID, Integer> justPlacedTrophy = new HashMap<>();

    public TrophyManager(List<Trophy> trophies) {
        this.trophies = new ArrayList<>(trophies);
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    public Trophy getTrophy(ItemStack item) {
        for (Trophy trophy : trophies) {
            if (trophy.matches(item)) {
                return trophy;
            }
        }
        return null;
    }

    public Trophy getTrophy(Entity entity) {
        if (entity != null && entity.isValid()) {
            for (Trophy trophy : trophies) {
                if (trophy.matches(entity)) {
                    return trophy;
                }
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || !player.isSneaking()) {
            return;
        }

        if (justPlacedTrophy.getOrDefault(player.getUniqueId(), 0) == player.getTicksLived()) {
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            return;
        }

        EquipmentSlot otherHand = event.getHand() == EquipmentSlot.HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        if (getTrophy(player.getEquipment().getItem(otherHand)) != null) {
            return;
        }

        ItemStack item = event.getItem();
        Trophy trophy = getTrophy(item);
        if (trophy == null) {
            return;
        }
        Block block = event.getClickedBlock().getRelative(event.getBlockFace());

        if (!Main.getInstance().getExternal().canBuild(player, block)) {
            C.error(player, "You can not place a trophy here.");
            return;
        }

        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        BlockFace face = event.getBlockFace().getOppositeFace();
        if (trophy.place(player, block, face, event.getHand()) != null) {
            justPlacedTrophy.put(player.getUniqueId(), player.getTicksLived());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        Trophy trophy = getTrophy(event.getRightClicked());
        if (trophy == null) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().isSneaking()) {
            if (event.getPlayer().isInsideVehicle()) {
                return;
            }
            Entity seat = trophy.getSeat(event.getRightClicked());
            if (seat != null) {
                if (seat.getPassengers().isEmpty()) {
                    seat.addPassenger(event.getPlayer());
                }
            }
        } else {
            Block block = event.getRightClicked().getLocation().getBlock();
            if (!Main.getInstance().getExternal().canBuild(event.getPlayer(), block)) {
                C.error(event.getPlayer(), "You can not pickup that trophy.");
                return;
            }
            trophy.pickup(event.getPlayer(), event.getRightClicked());
        }
    }

    private boolean isTrophy(Block block) {
        if (block.getType() == Trophy.SLAB_TYPE) {
            for (Entity e : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
                if (getTrophy(e) != null && e.getPersistentDataContainer().has(Trophy.SEAT_KEY, UUIDTagType.UUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (isTrophy(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private void onExplosion(List<Block> blocks) {
        for (Iterator<Block> it = blocks.iterator(); it.hasNext(); ) {
            Block b = it.next();
            if (isTrophy(b)) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent event) {
        onExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        onExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (isTrophy(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFire(BlockBurnEvent event) {
        if (isTrophy(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    private void onPiston(List<Block> blocks, Cancellable event) {
        for (Block b : blocks) {
            if (isTrophy(b)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonExtendEvent event) {
        onPiston(event.getBlocks(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPiston(BlockPistonRetractEvent event) {
        onPiston(event.getBlocks(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (isTrophy(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long time = event.getPlayer().getTicksLived();
        if (!event.getPlayer().isInsideVehicle() && dismount.getOrDefault(uuid, 0L) < time) {
            if (event.getPlayer().getLocation().getBlock().getType() == Trophy.SLAB_TYPE) {
                for (Entity e : event.getPlayer().getLocation().getBlock().getWorld().getNearbyEntities(event.getPlayer().getLocation().getBlock().getLocation().add(0.5, 0.5, 0.5), 0.5, 2.5, 0.5)) {
                    if (e.getPersistentDataContainer().has(Trophy.TROPHY_PARENT_KEY, UUIDTagType.UUID)) {
                        if (event.isSneaking()) {
                            sneaking.put(uuid, time + SITTING_TIMEOUT);
                        } else if (sneaking.getOrDefault(uuid, 0L) > time) {
                            if (e.getPassengers().isEmpty()) {
                                e.addPassenger(event.getPlayer());
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            dismount.put(player.getUniqueId(), event.getEntity().getTicksLived() + DISMOUNT_TIMEOUT);
            if (player.getLocation().add(0, 1, 0).getBlock().getType() == Trophy.SLAB_TYPE) {
                for (Entity e : player.getLocation().getBlock().getWorld().getNearbyEntities(player.getLocation().getBlock().getLocation().add(0.5, 0.5, 0.5), 0.5, 2.5, 0.5)) {
                    if (e.getPersistentDataContainer().has(Trophy.TROPHY_PARENT_KEY, UUIDTagType.UUID)) {
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                            Location loc = event.getDismounted().getLocation().getBlock().getLocation().add(0.5, 0.5, 0.5);
                            loc.setDirection(player.getLocation().getDirection());
                            player.teleport(loc);
                        });
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sneaking.remove(event.getPlayer().getUniqueId());
        dismount.remove(event.getPlayer().getUniqueId());
        justPlacedTrophy.remove(event.getPlayer().getUniqueId());
    }


}
