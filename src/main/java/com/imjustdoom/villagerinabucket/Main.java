package com.imjustdoom.villagerinabucket;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    public NamespacedKey key = new NamespacedKey(this, "villager_data");

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public boolean isVillagerBucket(ItemStack itemStack) {
        if (itemStack.getType() != Material.BUCKET || itemStack.getItemMeta() == null) {
            return false;
        }

        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        return dataContainer.has(this.key) && dataContainer.get(this.key, PersistentDataType.BYTE_ARRAY) != null;
    }

    @EventHandler
    public void villagerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        Entity clicked = event.getRightClicked();
        if (clicked.getType() != EntityType.VILLAGER || itemStack.getType() != Material.BUCKET || isVillagerBucket(itemStack)) {
            return;
        }
        if (itemStack.getAmount() > 1) {
            ItemStack newStack = new ItemStack(Material.BUCKET);
            newStack.editMeta(meta -> {
                meta.customName(Component.text("Villager In A Bucket"));
                meta.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE_ARRAY, Bukkit.getUnsafe().serializeEntity(clicked));
                meta.setMaxStackSize(1);
            });
            itemStack.setAmount(itemStack.getAmount() - 1);
            player.getInventory().addItem(newStack);
        } else {
            itemStack.editMeta(meta -> {
                meta.customName(Component.text("Villager In A Bucket"));
                meta.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE_ARRAY, Bukkit.getUnsafe().serializeEntity(clicked));
                meta.setMaxStackSize(1);
            });
        }
        clicked.remove();
        event.setCancelled(true);
    }

    @EventHandler
    public void bucketInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItem(event.getHand());

        if (!event.getAction().isRightClick() || event.getClickedBlock() == null || !isVillagerBucket(itemStack)) {
            return;
        }

        PersistentDataContainer dataContainer = itemStack.getItemMeta().getPersistentDataContainer();
        Bukkit.getUnsafe().deserializeEntity(dataContainer.get(this.key, PersistentDataType.BYTE_ARRAY), player.getWorld()).spawnAt(event.getInteractionPoint());
        itemStack.editMeta(meta -> {
            meta.customName(null);
            meta.getPersistentDataContainer().remove(this.key);
            meta.setMaxStackSize(null);
        });
        event.setCancelled(true);
    }
}