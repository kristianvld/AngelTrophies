package com.github.kristianvld.angeltrophies.trophy;

import com.github.kristianvld.angeltrophies.Main;
import com.github.kristianvld.angeltrophies.couch.CouchUtil;
import com.github.kristianvld.angeltrophies.util.C;
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
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.ArrayList;
import java.util.HashMap;
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

        Trophy trophyPlace = CouchUtil.getTrophy(trophy, block, player.getLocation().getYaw());
        if (trophyPlace != trophy) {
            item = trophyPlace.getExampleItem();
        }

        if (trophyPlace.place(player, block, face, event.getHand(), item) != null) {
            justPlacedTrophy.put(player.getUniqueId(), player.getTicksLived());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (!Trophy.isTrophy(event.getRightClicked())) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().isSneaking()) {
            if (event.getPlayer().isInsideVehicle()) {
                return;
            }
            Entity seat = Trophy.getSeat(event.getRightClicked());
            if (seat != null) {
                if (seat.getPassengers().isEmpty()) {
                    seat.addPassenger(event.getPlayer());
                }
                event.setCancelled(true);
            }
        } else {
            Block block = event.getRightClicked().getLocation().getBlock();
            if (!Main.getInstance().getExternal().canBuild(event.getPlayer(), block)) {
                C.error(event.getPlayer(), "You can not pickup that trophy.");
                return;
            }
            Trophy.pickup(event.getPlayer(), event.getRightClicked());
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (Trophy.getTrophy(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    private void onExplosion(List<Block> blocks) {
        blocks.removeIf(b -> Trophy.getTrophy(b) != null);
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
        if (Trophy.getTrophy(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFire(BlockBurnEvent event) {
        if (Trophy.getTrophy(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    private void onPiston(List<Block> blocks, Cancellable event) {
        for (Block b : blocks) {
            if (Trophy.getTrophy(b) != null) {
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
        if (Trophy.getTrophy(event.getBlock()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long time = event.getPlayer().getTicksLived();
        if (!event.getPlayer().isInsideVehicle() && dismount.getOrDefault(uuid, 0L) < time) {
            Entity trophy = Trophy.getTrophy(event.getPlayer().getLocation().getBlock());
            if (trophy != null) {
                Entity seat = Trophy.getSeat(trophy);
                if (seat != null) {
                    if (event.isSneaking()) {
                        sneaking.put(uuid, time + SITTING_TIMEOUT);
                    } else if (sneaking.getOrDefault(uuid, 0L) > time) {
                        if (seat.getPassengers().isEmpty()) {
                            seat.addPassenger(event.getPlayer());
                        }
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
            Entity trophy = Trophy.getTrophy(event.getDismounted());
            if (trophy != null) {
                Entity seat = Trophy.getSeat(trophy);
                if (seat != null) {
                    Location pLoc = player.getLocation();
                    BlockVector bv = Trophy.getBlockVector(trophy);
                    Location loc;
                    if (bv != null) {
                        loc = bv.toLocation(pLoc.getWorld(), pLoc.getYaw(), pLoc.getPitch());
                        loc.add(0.5, 0.5, 0.5);
                    } else {
                        loc = pLoc.add(0, 0.55, 0); // old legacy before we started storing block vector
                    }
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        player.teleport(loc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
                    });
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
