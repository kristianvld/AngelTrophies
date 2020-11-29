package com.github.kristianvld.angeltrophies;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

public class Main extends JavaPlugin {

    private static Main instance;

    private StickerManager stickerManager;
    private TrophyManager trophyManager;
    private CommandHandler cmdHandler;
    private External external;

    public StickerManager getStickerManager() {
        return stickerManager;
    }

    @Override
    public void onEnable() {
        instance = this;
        Skin.init(this);
        Trophy.init(this);

        cmdHandler = new CommandHandler();
        external = new External();

        loadManagers();
    }

    public void reload() {
        HandlerList.unregisterAll(stickerManager);
        HandlerList.unregisterAll(trophyManager);
        getLogger().info("Reloading...");
        loadManagers();
        getLogger().info("Done Reloading.");
    }

    private void loadManagers() {
        List<Skin> skins = new ArrayList<>();
        List<Trophy> trophies = new ArrayList<>();

        for (File file : new File(getDataFolder().getParent(), "Oraxen/items").listFiles(f -> f.getName().endsWith(".yml"))) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                for (String key : yaml.getKeys(false)) {
                    if (!yaml.isConfigurationSection(key)) {
                        continue;
                    }
                    if (yaml.isConfigurationSection(key + ".skin")) {
                        ConfigurationSection skin = yaml.getConfigurationSection(key + ".skin");
                        for (String source : skin.getKeys(false)) {
                            try {
                                if (skin.isString(source)) {
                                    String target = skin.getString(source);
                                    if (source.toLowerCase().startsWith("minecraft_")) {
                                        String material = source;
                                        String[] split = material.split("_");
                                        int id = 0;
                                        try {
                                            id = Integer.parseInt(split[split.length - 1]);
                                            material = String.join("_", Arrays.asList(split).subList(0, split.length - 1));
                                        } catch (NumberFormatException e) {
                                        }
                                        material = material.replaceFirst("_", ":");
                                        skins.add(new Skin(Material.matchMaterial(material), id, target, key));
                                    } else {
                                        skins.add(new Skin(source, target, key));
                                    }
                                }
                            } catch (Exception e) {
                                getLogger().log(Level.SEVERE, "Error while parsing skin " + key + ".skin." + source, e);
                            }
                        }
                    } else if (yaml.isConfigurationSection(key + ".trophies")) {
                        try {
                            ConfigurationSection t = yaml.getConfigurationSection(key + ".trophies");
                            boolean floor = false;
                            boolean floorSmall = false;
                            double floorOffset = 0.0;
                            boolean wall = false;
                            boolean wallSmall = false;
                            double wallOffset = 0.0;
                            boolean floorPlaceSlab = false;
                            float floorRotationResolution = 45;
                            if (t.isConfigurationSection("floor")) {
                                floor = true;
                                floorSmall = t.getBoolean("floor.small", floorSmall);
                                floorOffset = t.getDouble("floor.offset", floorOffset);
                                floorPlaceSlab = t.getBoolean("floor.place_slab", floorPlaceSlab);
                                floorRotationResolution = (float) t.getDouble("floor.rotation_resolution", floorRotationResolution);
                            }
                            if (t.isConfigurationSection("wall")) {
                                wall = true;
                                wallSmall = t.getBoolean("wall.small", wallSmall);
                                wallOffset = t.getDouble("wall.offset", wallOffset);
                            }
                            if (floor || wall) {
                                trophies.add(new Trophy(key, floor, floorSmall, floorOffset, wall, wallSmall, wallOffset, floorPlaceSlab, floorRotationResolution));
                            }
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Error while parsing trophy " + key + ".trophies", e);
                        }
                    }
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Error while parsing config file " + file.getPath(), e);
            }
        }
        getLogger().info("Loaded " + skins.size() + " skins.");
        getLogger().info("Loaded " + trophies.size() + " trophies.");

        stickerManager = new StickerManager(skins);
        trophyManager = new TrophyManager(trophies);
    }

    public External getExternal() {
        return external;
    }

    public static Main getInstance() {
        return instance;
    }
}
