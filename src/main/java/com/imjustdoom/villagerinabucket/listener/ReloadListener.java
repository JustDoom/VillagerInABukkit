package com.imjustdoom.villagerinabucket.listener;

/*
 * We need to separate this because any usage of this will throw class not found errors if BetterReload isnt installed.
 */

import better.reload.api.ReloadEvent;
import com.imjustdoom.villagerinabucket.Config;
import com.imjustdoom.villagerinabucket.VillagerInABucket;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ReloadListener implements Listener {

    @EventHandler
    public void onReloadEvent(ReloadEvent event) {
        Config.init();
        event.getCommandSender().sendMessage(VillagerInABucket.PREFIX + " VillagerInABucket has been reloaded!");
    }
}
