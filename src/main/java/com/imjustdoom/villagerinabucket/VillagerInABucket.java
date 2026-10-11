package com.imjustdoom.villagerinabucket;

import com.imjustdoom.villagerinabucket.listener.DispenserListener;
import com.imjustdoom.villagerinabucket.listener.InteractListener;
import com.imjustdoom.villagerinabucket.listener.ReloadListener;
import com.imjustdoom.villagerinabucket.listener.protection.WorldGuardProtectionListener;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
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
                    })).then(Commands.literal("forceplace").executes(ctx -> {
                        if (!(ctx.getSource().getSender() instanceof Player player)) {
                            ctx.getSource().getSender().sendMessage(Component.text(PREFIX + " You must be a player to run this command", TEXT_COLOR));
                            return Command.SINGLE_SUCCESS;
                        }

                        return forceSpawnLogic(ctx.getSource().getSender(), player);
                    }).then(Commands.argument("target", ArgumentTypes.player()).executes(ctx -> {
                        final PlayerSelectorArgumentResolver resolver = ctx.getArgument("target", PlayerSelectorArgumentResolver.class);
                        final Player player = resolver.resolve(ctx.getSource()).getFirst();
                        return forceSpawnLogic(ctx.getSource().getSender(), player);
                    })))
                    .build();
            commands.registrar().register(buildCommand, List.of("viab"));
        });

        PluginManager pluginManager = getServer().getPluginManager();

        if (pluginManager.isPluginEnabled("WorldGuard")) {
            pluginManager.registerEvents(new WorldGuardProtectionListener(), this);
            getLogger().info("Detected WorldGuard, registered listener integration");
        }
        if (pluginManager.isPluginEnabled("GriefPrevention")) {
            pluginManager.registerEvents(new WorldGuardProtectionListener(), this);
            getLogger().info("Detected GriefPrevention, registered listener integration");
        }

        pluginManager.registerEvents(this, this);
        pluginManager.registerEvents(new InteractListener(), this);
        pluginManager.registerEvents(new DispenserListener(), this);

        if (pluginManager.isPluginEnabled("BetterReload")) {
            pluginManager.registerEvents(new ReloadListener(), this);
            getLogger().info("Detected BetterReload, registered listener integration");
        }

        Metrics metrics = new Metrics(this, 25722);
        metrics.addCustomChart(new SimplePie("updated_to_new_settings", () -> "true"));
        metrics.addCustomChart(new SimplePie("usingResourcepack", () -> String.valueOf(Config.RESOURCE_PACK)));
    }

    @Override
    public void onDisable() {
        synchronized (VLog.LOG_LOCK) {
            if (VLog.LOG_FILE_WRITER != null) {
                try {
                    VLog.LOG_FILE_WRITER.close();
                    VLog.LOG_FILE_WRITER = null;
                } catch (IOException e) {
                    getLogger().severe("Unable to close villager action file logger");
                }
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

    private int forceSpawnLogic(CommandSender sender, Player player) {
        ItemStack itemStack = player.getInventory().getItem(player.getActiveItemHand());

        if (!BucketUtil.hasEntityData(itemStack)) {
            sender.sendMessage(Component.text(PREFIX + " No entity data found on item", TEXT_COLOR));
            return Command.SINGLE_SUCCESS;
        }

        LivingEntity entity = BucketUtil.entityFromBucket(itemStack, player.getWorld());
        Location location = player.getLocation();

        if (BucketUtil.spawnVillagerFromBucket(entity, player, itemStack, location)) {
            sender.sendMessage(Component.text(PREFIX + " Successfully force spawned entity", TEXT_COLOR));
        } else {
            sender.sendMessage(Component.text(PREFIX + " Force spawn was cancelled by something, possibly a land protection or other plugin", TEXT_COLOR));
        }

        return Command.SINGLE_SUCCESS;
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