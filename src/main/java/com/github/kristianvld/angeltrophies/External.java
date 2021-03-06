package com.github.kristianvld.angeltrophies;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.vergilprime.angelprotect.AngelProtect;
import com.vergilprime.angelprotect.datamodels.APChunk;
import com.vergilprime.angelprotect.datamodels.APClaim;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class External {

    private final boolean worldGuard;
    private final boolean angelProtect;

    public External() {
        worldGuard = Bukkit.getPluginManager().isPluginEnabled("WorldGuard");
        if (worldGuard) {
            Main.getInstance().getLogger().info("Found WorldGuard, will be using hook to check for external permissions.");
        }
        angelProtect = Bukkit.getPluginManager().isPluginEnabled("AngelProtect");
        if (angelProtect) {
            Main.getInstance().getLogger().info("Found AngelProtect, will be using hook to check for external permissions.");
        }
    }

    public boolean canBuild(Player player, Block block) {
        if (!canBuildAngelProtect(player, block)) {
            return false;
        }
        return canBuildWorldGuard(player, block);
    }

    public boolean canBuildWorldGuard(Player player, Block block) {
        if (worldGuard) {

            LocalPlayer wgPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
            WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();

            if (platform.getSessionManager().hasBypass(wgPlayer, wgPlayer.getWorld())) {
                return true; // has bypass permission
            }

            com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(block.getLocation());
            RegionContainer container = platform.getRegionContainer();
            RegionQuery query = container.createQuery();

            return query.testState(loc, wgPlayer, Flags.BUILD);
        }
        return true;
    }

    public boolean canBuildAngelProtect(Player player, Block block) {
        if (angelProtect) {
            APClaim chunk = AngelProtect.getInstance().getStorageManager().getClaim(new APChunk(block));
            if (chunk != null) {
                return chunk.canBuild(player);
            }
        }
        return true;
    }
}
