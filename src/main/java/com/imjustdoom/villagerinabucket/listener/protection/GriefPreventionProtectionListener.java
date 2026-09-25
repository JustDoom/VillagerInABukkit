package com.imjustdoom.villagerinabucket.listener.protection;

import com.imjustdoom.villagerinabucket.event.PreVillagerPickupEvent;
import com.imjustdoom.villagerinabucket.event.PreVillagerPlaceEvent;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class GriefPreventionProtectionListener implements Listener {
    private final GriefPrevention griefPrevention;

    public GriefPreventionProtectionListener() {
        this.griefPrevention = (GriefPrevention) Bukkit.getPluginManager().getPlugin("GriefPrevention");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreVillagerPickupEvent(PreVillagerPickupEvent event) {
        checkLocationPermission(event.getPlayer(), event.getLocation(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreVillagerPlaceEvent(PreVillagerPlaceEvent event) {
        checkLocationPermission(event.getPlayer(), event.getLocation(), event);
    }

    private void checkLocationPermission(Player player, Location location, Cancellable event) {
        if (this.griefPrevention.allowBuild(player, location) != null) {
            event.setCancelled(true);
        }
    }
}
