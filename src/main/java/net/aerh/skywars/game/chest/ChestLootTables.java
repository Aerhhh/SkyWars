package net.aerh.skywars.game.chest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChestLootTables {

    // TODO implement percentage chances for items to spawn
    private static final List<ItemStack> ISLAND_CHEST_LOOT = new ArrayList<>();
    private static final List<ItemStack> MIDDLE_CHEST_LOOT = new ArrayList<>();

    static {
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.LEATHER_HELMET));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.LEATHER_CHESTPLATE));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.LEATHER_LEGGINGS));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.LEATHER_BOOTS));

        ISLAND_CHEST_LOOT.add(new ItemStack(Material.CHAINMAIL_HELMET));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.CHAINMAIL_BOOTS));

        ISLAND_CHEST_LOOT.add(new ItemStack(Material.GOLDEN_HELMET));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.GOLDEN_CHESTPLATE));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.GOLDEN_LEGGINGS));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.GOLDEN_BOOTS));

        ISLAND_CHEST_LOOT.add(new ItemStack(Material.IRON_HELMET));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.IRON_CHESTPLATE));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.IRON_LEGGINGS));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.IRON_BOOTS));

        ISLAND_CHEST_LOOT.add(new ItemStack(Material.STONE_SWORD));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.IRON_SWORD));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.BOW));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.ARROW, 8));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.OAK_PLANKS, 16));

        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.DIAMOND_SWORD));
        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.TNT, 4));
        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.FLINT_AND_STEEL));
        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.LAVA_BUCKET));

        for (int i = 0; i < 2; i++) {
            MIDDLE_CHEST_LOOT.add(new ItemStack(Material.ENDER_PEARL, i));
        }

        for (int i = 0; i < 3; i++) {
            MIDDLE_CHEST_LOOT.add(new ItemStack(Material.GOLDEN_APPLE, i));
        }
    }

    public static List<ItemStack> getLootForChestType(@NotNull ChestType chestType) {
        if (chestType == ChestType.MIDDLE) {
            return MIDDLE_CHEST_LOOT;
        } else {
            return ISLAND_CHEST_LOOT;
        }
    }
}
