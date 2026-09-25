package com.imjustdoom.villagerinabucket.listener.protection;

import com.imjustdoom.villagerinabucket.event.PreVillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.PreVillagerPlaceEvent;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardProtectionListener implements Listener {
    private final RegionContainer container;

    public WorldGuardProtectionListener() {
        this.container = WorldGuard.getInstance().getPlatform().getRegionContainer();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreVillagerPickupEvent(PreVillagerPickupEvent event) {
        checkRegionFlags(event.getPlayer(), event.getLocation(), event, Flags.INTERACT);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreVillagerPlaceEvent(PreVillagerPlaceEvent event) {
        checkRegionFlags(event.getPlayer(), event.getLocation(), event, Flags.MOB_SPAWNING);
    }

    private void checkRegionFlags(Player player, Location location, Cancellable event, StateFlag flag) {
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (this.container.createQuery().queryState(BukkitAdapter.adapt(location), localPlayer, flag) == StateFlag.State.DENY) {
            event.setCancelled(true);
        }
    }
}
