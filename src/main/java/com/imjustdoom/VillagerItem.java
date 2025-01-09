package com.imjustdoom;

import net.minecraft.server.v1_6_R3.MerchantRecipe;
import net.minecraft.server.v1_6_R3.NBTBase;
import net.minecraft.server.v1_6_R3.NBTTagCompound;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_6_R3.entity.CraftVillager;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.getspout.spoutapi.block.SpoutBlock;
import org.getspout.spoutapi.material.item.GenericCustomItem;
import org.getspout.spoutapi.player.SpoutPlayer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Base64;
import java.util.List;

public class VillagerItem extends GenericCustomItem {

    // Main item initialisation
    public VillagerItem() {
        // Plugin instance, item ID, texture PNG link
        // I had trouble getting the texture working, not sure if it was a weird cache thing or what. Try delete Spout cache
        // info if you are having trouble with it not applying to the item. Or grab a new one from the creative menu
        super(Main.get(), "villagerinabucket", "https://i.ibb.co/sCFvQBD/q6wBozz.png");
        setName("Villager In A Bucket"); // Item name
        setStackable(false); // It now worky
    }

    @Override
    public boolean onItemInteract(SpoutPlayer player, SpoutBlock block, BlockFace face) {
        if (player == null || block == null || face == null) return true;

        // Fun NMS stuff to store villager recipes in the lore encoded as Base64 :)
        World world = player.getWorld();
        ItemStack item = player.getItemInHand();
        CraftVillager villager = (CraftVillager) world.spawnEntity(new Location(world, block.getX() + face.getModX(), block.getY() + face.getModY(), block.getZ() + face.getModZ()), EntityType.VILLAGER);

        // Check just in case it is pulled from the creative menu as it will have no lore data
        if (item.getItemMeta().hasLore() && !item.getItemMeta().getLore().isEmpty()) {
            List<String> lore = item.getItemMeta().getLore();
            villager.setProfession(Villager.Profession.valueOf(lore.get(0)));
            villager.setAge(Integer.parseInt(lore.get(1)));

            // Remove first 2 which are always Profession and Age. The rest is encoded data
            lore.remove(0);
            lore.remove(0);

            villager.getHandle().getOffers(((CraftPlayer) player).getHandle()).clear();
            for (String recipe : lore) {
                byte[] bytes = Base64.getDecoder().decode(recipe);
                NBTTagCompound nbt = (NBTTagCompound) NBTBase.b(new DataInputStream(new ByteArrayInputStream(bytes)), bytes.length);
                villager.getHandle().getOffers(((CraftPlayer) player).getHandle()).a(new MerchantRecipe(nbt));
            }
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            item.setAmount(item.getAmount());
        }

        return true;
    }
}
