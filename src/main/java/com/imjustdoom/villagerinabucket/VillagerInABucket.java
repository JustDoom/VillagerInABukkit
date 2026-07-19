package com.imjustdoom.villagerinabucket;

import com.imjustdoom.villagerinabucket.listener.DispenserListener;
import com.imjustdoom.villagerinabucket.listener.InteractListener;
import com.imjustdoom.villagerinabucket.listener.ReloadListener;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public class VillagerInABucket extends JavaPlugin implements Listener {
    public static String PREFIX = "[VIAB]";
    public static TextColor TEXT_COLOR = TextColor.color(2, 220, 5);

    public NamespacedKey key = new NamespacedKey(this, "villager_data");

    public VillagerInABucket() {
        INSTANCE = this;
    }

    @Override
    public void onEnable() {
        Config.init();

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            LiteralCommandNode<CommandSourceStack> buildCommand = Commands.literal("villagerinabucket")
                    .requires(sender -> sender.getSender().hasPermission("villagerinabucket.commands"))
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(Component.text(PREFIX + " VillagerInABucket version " + getPluginMeta().getVersion(), TEXT_COLOR));
                        return Command.SINGLE_SUCCESS;
                    }).then(Commands.literal("reload").executes(ctx -> {
                        Config.init();
                        ctx.getSource().getSender().sendMessage(Component.text(PREFIX + " VillagerInABucket has been reloaded!", TEXT_COLOR));
                        return Command.SINGLE_SUCCESS;
                    }))
                    .build();
            commands.registrar().register(buildCommand, List.of("viab"));
        });
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new InteractListener(), this);
        getServer().getPluginManager().registerEvents(new DispenserListener(), this);

        if (getServer().getPluginManager().getPlugin("BetterReload") != null) {
            getServer().getPluginManager().registerEvents(new ReloadListener(), this);
        }

        Metrics metrics = new Metrics(this, 25722);
        metrics.addCustomChart(new SimplePie("updated_to_new_settings", () -> "true"));
        metrics.addCustomChart(new SimplePie("usingResourcepack", () -> String.valueOf(Config.RESOURCE_PACK)));
    }

    @Override
    public void onDisable() {
        if (VLog.LOG_FILE_WRITER != null) {
            try {
                VLog.LOG_FILE_WRITER.close();
            } catch (IOException e) {
                getLogger().severe("Unable to close villager action file logger");
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!Config.RESOURCE_PACK) {
            return;
        }
        try {
            event.getPlayer().sendResourcePacks(ResourcePackInfo.resourcePackInfo(UUID.fromString(Config.RESOURCE_PACK_ID), URI.create(Config.RESOURCE_PACK_URL), Config.RESOURCE_PACK_HASH));
        } catch (IllegalArgumentException exception) {
            getLogger().severe("The UUID '" + Config.RESOURCE_PACK_ID + "' is invalid");
        }
    }

    private static VillagerInABucket INSTANCE;

    /**
     * Gets the Villager In A Bucket instance
     *
     * @return the instance
     */
    public static VillagerInABucket get() {
        return INSTANCE;
    }
}