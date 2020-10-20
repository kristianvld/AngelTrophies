package com.github.kristianvld.angeltrophies;

import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.UUID;

public class Trophy {

    public final static Material SLAB_TYPE = Material.BIRCH_SLAB;

    static NamespacedKey OWNER_KEY;
    static NamespacedKey DIRECTION_KEY;
    static NamespacedKey SEAT_KEY;
    static NamespacedKey TROPHY_PARENT_KEY;

    private final String itemName;

    private final boolean floor;
    private final boolean floorSmall;
    private final double floorOffset;
    private final boolean floorPlaceSlab;
    private final float floorRotationResolution;

    private final boolean wall;
    private final boolean wallSmall;
    private final double wallOffset;

    public Trophy(String item, boolean floor, boolean floorSmall, double floorOffset, boolean wall, boolean wallSmall, double wallOffset, boolean floorPlaceSlab, float floorRotationResolution) {
        itemName = item;
        if (!OraxenItems.exists(item)) {
            throw new IllegalArgumentException("Invalid Oraxen item provided for trophy.");
        }
        this.floor = floor;
        this.floorSmall = floorSmall;
        this.floorOffset = floorOffset;
        this.wall = wall;
        this.wallSmall = wallSmall;
        this.wallOffset = wallOffset;
        this.floorPlaceSlab = floorPlaceSlab;
        this.floorRotationResolution = floorRotationResolution;
    }

    public ArmorStand place(UUID owner, ItemStack itemstack, Block block, boolean small, BlockFace blockFace, float yaw, Vector offset, boolean placeSlab) {
        for (Entity e : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)) {
            if (e instanceof Hanging) {
                if (((Hanging) e).getAttachedFace() == blockFace) {
                    return null;
                }
            }
            if (blockFace == getFace(e)) {
                return null;
            }
        }
        if (placeSlab && block.getType() != Material.AIR) {
            return null;
        }
        Location loc = block.getLocation().add(0.5, 0, 0.5).add(offset);
        loc.setYaw(yaw);
        ArmorStand stand = loc.getWorld().spawn(loc, ArmorStand.class);
        stand.getEquipment().setHelmet(itemstack);
        stand.setInvulnerable(true);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setCollidable(false);
        stand.setSmall(small);
        stand.setMarker(false); // turn to true to disable hitbox, would be hard to detect pickup
        stand.getPersistentDataContainer().set(OWNER_KEY, UUIDTagType.UUID, owner);
        stand.getPersistentDataContainer().set(DIRECTION_KEY, UUIDTagType.INTEGER, blockFace.ordinal());
        loc.getWorld().playSound(loc, Sound.ENTITY_ARMOR_STAND_PLACE, 0.7f, 0.7f);
        if (placeSlab) {
            block.setType(SLAB_TYPE);
            ArmorStand seat = loc.getWorld().spawn(loc.clone().add(0, 0.3, 0), ArmorStand.class);
            seat.setMarker(true);
            seat.setGravity(false);
            seat.setInvulnerable(true);
            seat.setVisible(false);
            seat.getPersistentDataContainer().set(TROPHY_PARENT_KEY, UUIDTagType.UUID, stand.getUniqueId());
            stand.getPersistentDataContainer().set(SEAT_KEY, UUIDTagType.UUID, seat.getUniqueId());
        }
        return stand;
    }

    public boolean matches(ItemStack item) {
        return itemName.equals(OraxenItems.getIdByItem(item));
    }

    public boolean matches(Entity entity) {
        return entity instanceof ArmorStand
                && entity.isValid()
                && entity.getPersistentDataContainer().has(OWNER_KEY, UUIDTagType.UUID)
                && entity.getPersistentDataContainer().has(DIRECTION_KEY, UUIDTagType.INTEGER);
    }

    public BlockFace getFace(Entity entity) {
        return matches(entity) ? BlockFace.values()[entity.getPersistentDataContainer().get(DIRECTION_KEY, UUIDTagType.INTEGER)] : null;
    }


    public ArmorStand place(Player player, Block block, BlockFace face, EquipmentSlot hand) {
        ItemStack item = player.getEquipment().getItem(hand);
        if (!matches(item)) {
            return null;
        }
        Vector offset;
        boolean small;
        float yaw;
        boolean placeSlab = false;
        if (face == BlockFace.DOWN && floor) {
            offset = face.getDirection().multiply(-floorOffset);
            small = floorSmall;
            yaw = 180 + Math.round(player.getLocation().getYaw() / floorRotationResolution) * floorRotationResolution;
            placeSlab = floorPlaceSlab;
        } else if (wall && (face == BlockFace.EAST || face == BlockFace.NORTH || face == BlockFace.SOUTH || face == BlockFace.WEST)) {
            offset = face.getDirection().multiply(wallOffset);
            small = wallSmall;
            yaw = (float) Math.toDegrees(face.getDirection().angle(BlockFace.SOUTH.getDirection()));
        } else {
            return null;
        }
        ItemStack trophy = item.clone();
        trophy.setAmount(1);
        ArmorStand stand = place(player.getUniqueId(), trophy, block, small, face, yaw, offset, placeSlab);
        if (stand != null) {
            item.setAmount(item.getAmount() - 1);
            item = item.getAmount() > 0 ? item : null;
            player.getEquipment().setItem(hand, item);
        }
        return stand;
    }

    public Entity getSeat(Entity trophy) {
        if (!matches(trophy)) {
            return null;
        }
        if (trophy.getPersistentDataContainer().has(SEAT_KEY, UUIDTagType.UUID)) {
            UUID uuid = trophy.getPersistentDataContainer().get(SEAT_KEY, UUIDTagType.UUID);
            for (Entity e : trophy.getLocation().getChunk().getEntities()) {
                if (e.getUniqueId().equals(uuid)) {
                    return e;
                }
            }
        }
        return null;
    }

    public boolean pickup(Player player, Entity entity) {
        if (!matches(entity)) {
            return false;
        }
        for (ItemStack item : player.getInventory().addItem(((ArmorStand) entity).getEquipment().getHelmet()).values()) {
            player.getWorld().dropItem(player.getEyeLocation(), item);
        }
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 0.7f, 0.7f);
        if (entity.getPersistentDataContainer().has(SEAT_KEY, UUIDTagType.UUID)) {
            Entity seat = getSeat(entity);
            if (seat != null) {
                seat.remove();
            }
            entity.getLocation().add(0, -floorOffset, 0).getBlock().setType(Material.AIR);
        }
        entity.remove();
        return true;
    }

    public static void init(Main main) {
        OWNER_KEY = new NamespacedKey(main, "trophy_owner");
        DIRECTION_KEY = new NamespacedKey(main, "trophy_direction");
        SEAT_KEY = new NamespacedKey(main, "trophy_seat");
        TROPHY_PARENT_KEY = new NamespacedKey(main, "trophy_parent");
    }
}
