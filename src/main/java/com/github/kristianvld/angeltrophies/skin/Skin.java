package com.github.kristianvld.angeltrophies.skin;

import com.github.kristianvld.angeltrophies.Main;
import io.th0rgal.oraxen.items.OraxenItems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class Skin {

    private static NamespacedKey stickerKey;

    private Material material;

    private int sourceID;
    private String sourceName;
    private String sourceDisplayName;

    private int targetID;
    private String targetName;
    private String targetDisplayName;

    private String stickerName;
    private ItemStack stickerItem;

    public Skin(Material sourceType, int sourceID, String target, String sticker) {
        init(sourceType, sourceID, target, sticker);
    }

    public Skin(String source, String target, String sticker) {
        if (!OraxenItems.exists(source)) {
            throw new IllegalArgumentException("Source is not a valid Oraxen item for Skin '" + sticker + "'. '" + source + "' was not found.");
        }
        ItemStack sourceItem = OraxenItems.getItemById(source).build();
        init(sourceItem.getType(), getModelData(sourceItem), target, sticker);
        sourceName = source;
        sourceDisplayName = getDisplayName(sourceItem);
    }

    private void init(Material sourceType, int sourceID, String target, String sticker) {
        material = sourceType;
        this.sourceID = sourceID;
        if (!OraxenItems.exists(target)) {
            throw new IllegalArgumentException("Target is not a valid Oraxen item for the Skin '" + sticker + "', target: '" + target + "'!");
        }
        if (!OraxenItems.exists(sticker)) {
            throw new IllegalArgumentException("Skin is not a valid Oraxen item for the Skin '" + sticker + "'!");
        }
        ItemStack targetItem = OraxenItems.getItemById(target).build();
        if (targetItem.getType() != sourceType) {
            throw new IllegalArgumentException("Source and Target needs to be of the same material for the Skin '" + sticker + "'. Provided target: " + targetItem.getType() + ", provided source: " + sourceType + "!");
        }
        targetID = getModelData(targetItem);
        targetName = target;
        targetDisplayName = getDisplayName(targetItem);
        stickerName = sticker;
        stickerItem = OraxenItems.getItemById(sticker).build();
    }

    public String getStickerName() {
        return stickerName;
    }

    private int getModelData(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 0;
    }

    private ItemStack reduceAmount(ItemStack item, int amount) {
        if (amount >= item.getAmount()) {
            return null;
        }
        item.setAmount(item.getAmount() - amount);
        return item;
    }

    private String getDisplayName(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
    }

    public boolean matchesSource(ItemStack item) {
        return item.getType() == material && getModelData(item) == sourceID;
    }

    public boolean matchesTarget(ItemStack item) {
        if (item.getType() != material || getModelData(item) != targetID) {
            return false;
        }
        String stickers = item.getItemMeta().getPersistentDataContainer().get(stickerKey, PersistentDataType.STRING);
        return stickers != null && (stickers.equals(stickerName) || stickers.endsWith("," + stickerName));
    }

    public boolean matchesSticker(ItemStack item) {
        return stickerName.equals(OraxenItems.getIdByItem(item)) && item.getAmount() >= stickerItem.getAmount();
    }

    /**
     * @return Two items, first the item with the skin applied. Second the remaining stickers. Second sticker might be null.
     */
    public ItemStack[] apply(ItemStack item, ItemStack sticker) {
        if (!matchesSource(item) || !matchesSticker(sticker)) {
            return new ItemStack[]{item, sticker};
        }

        ItemMeta im = item.getItemMeta();
        im.setCustomModelData(targetID);
        if (Objects.equals(getDisplayName(item), sourceDisplayName)) {
            im.setDisplayName(targetDisplayName);
        }
        String stickers = stickerName;
        PersistentDataContainer storage = im.getPersistentDataContainer();
        if (storage.has(stickerKey, PersistentDataType.STRING)) {
            stickers = storage.get(stickerKey, PersistentDataType.STRING) + "," + stickers;
        }
        storage.set(stickerKey, PersistentDataType.STRING, stickers);
        storage.set(OraxenItems.ITEM_ID, PersistentDataType.STRING, targetName);
        item.setItemMeta(im);

        sticker = reduceAmount(sticker, stickerItem.getAmount());
        return new ItemStack[]{item, sticker};
    }

    /**
     * @return Two items, first the item with the skin removed. Second a sticker. Second sticker might be null.
     */
    public ItemStack[] split(ItemStack item) {
        if (!matchesTarget(item)) {
            return new ItemStack[]{item, null};
        }

        ItemMeta im = item.getItemMeta();
        im.setCustomModelData(sourceID);
        if (Objects.equals(getDisplayName(item), targetDisplayName)) {
            im.setDisplayName(sourceDisplayName);
        }
        PersistentDataContainer storage = im.getPersistentDataContainer();
        String stickers = storage.get(stickerKey, PersistentDataType.STRING);
        if (stickerName.equals(stickers)) {
            storage.remove(stickerKey);
        } else {
            storage.set(stickerKey, PersistentDataType.STRING, stickers.substring(0, stickers.length() - 1 - stickerName.length()));
        }
        if (sourceName == null) {
            storage.remove(OraxenItems.ITEM_ID);
        } else {
            storage.set(OraxenItems.ITEM_ID, PersistentDataType.STRING, sourceName);
        }
        item.setItemMeta(im);

        return new ItemStack[]{item, stickerItem.clone()};
    }

    public static void init(Main main) {
        Skin.stickerKey = new NamespacedKey(main, "stickers");
    }
}
