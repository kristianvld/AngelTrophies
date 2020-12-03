package com.github.kristianvld.angeltrophies;

import com.github.kristianvld.angeltrophies.couch.CouchRole;
import com.github.kristianvld.angeltrophies.couch.CouchUtil;
import com.github.kristianvld.angeltrophies.skin.Skin;
import com.github.kristianvld.angeltrophies.skin.SkinManager;
import com.github.kristianvld.angeltrophies.trophy.Trophy;
import com.github.kristianvld.angeltrophies.trophy.TrophyManager;
import com.github.kristianvld.angeltrophies.util.C;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
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

    private SkinManager skinManager;
    private TrophyManager trophyManager;
    private CommandHandler cmdHandler;
    private External external;

    public SkinManager getStickerManager() {
        return skinManager;
    }

    @Override
    public void onEnable() {
        try {
            instance = this;
            Skin.init(this);
            Trophy.init(this);

            cmdHandler = new CommandHandler();
            external = new External();

            loadManagers();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while enabling:", e);
            getLogger().severe("An error occurred while enabling AngelTrophies, disabling...");
            setEnabled(false);
        }
    }

    public void reload(CommandSender sender) {
        try {
            HandlerList.unregisterAll(skinManager);
            HandlerList.unregisterAll(trophyManager);
            getLogger().info("Reloading...");
            loadManagers();
            getLogger().info("Done Reloading.");
            C.main(sender, "Reloaded {0}.", Main.getInstance().getDescription().getName());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Error while reloading:", e);
            getLogger().severe("An error occurred while enabling AngelTrophies, disabling...");
            C.error(sender, "An error occurred while reload {0}.", Main.getInstance().getDescription().getName());
            C.error(sender, "Disabling the plugin...");
            setEnabled(false);
        }
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
                                        } catch (NumberFormatException ignored) {
                                        }
                                        material = material.replaceFirst("_", ":");
                                        skins.add(new Skin(Material.matchMaterial(material), id, target, key));
                                    } else {
                                        skins.add(new Skin(source, target, key));
                                    }
                                }
                            } catch (Exception e) {
                                throw new RuntimeException("Error while parsing skin " + key + ".skin." + source, e);
                            }
                        }
                    } else if (yaml.isConfigurationSection(key + ".trophies")) {
                        try {
                            ConfigurationSection t = yaml.getConfigurationSection(key + ".trophies");
                            boolean floor = t.isConfigurationSection("floor");
                            boolean floorSmall = t.getBoolean("floor.small", false);
                            double floorOffset = t.getDouble("floor.offset", 0.0);
                            boolean wall = t.isConfigurationSection("wall");
                            boolean wallSmall = t.getBoolean("wall.small", false);
                            double wallOffset = t.getDouble("wall.offset", 0.0);
                            boolean floorPlaceSlab = t.getBoolean("floor.place_slab", false);
                            float floorRotationResolution = (float) t.getDouble("floor.rotation_resolution", 45);
                            String cGroup = t.getString("floor.couch.group", null);
                            CouchRole cRole = CouchRole.parse(t.getString("floor.couch.role", null));
                            if (floor || wall) {
                                trophies.add(new Trophy(key, floor, floorSmall, floorOffset, wall, wallSmall, wallOffset, floorPlaceSlab, floorRotationResolution, cGroup, cRole));
                            }
                        } catch (Exception e) {
                            throw new RuntimeException("Error while parsing trophy " + key + ".trophies", e);
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Error while parsing config file " + file.getPath(), e);
            }
        }
        CouchUtil.buildCache(trophies);
        getLogger().info("Loaded " + skins.size() + " skins.");
        getLogger().info("Loaded " + trophies.size() + " trophies.");

        skinManager = new SkinManager(skins);
        trophyManager = new TrophyManager(trophies);
    }

    public External getExternal() {
        return external;
    }

    public static Main getInstance() {
        return instance;
    }
}
