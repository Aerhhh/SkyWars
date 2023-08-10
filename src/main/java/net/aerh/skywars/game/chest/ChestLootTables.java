package net.aerh.skywars.game.chest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ChestLootTables {

    // TODO implement percentage chances for items to spawn
    protected static final List<ItemStack> ISLAND_CHEST_LOOT = new ArrayList<>();
    protected static final List<ItemStack> MIDDLE_CHEST_LOOT = new ArrayList<>();

    static {
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.OAK_PLANKS, 16));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.STONE_SWORD));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.STONE_PICKAXE));
        ISLAND_CHEST_LOOT.add(new ItemStack(Material.STONE_AXE));

        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.DIAMOND, 2));
        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.DIAMOND_SWORD));
        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.DIAMOND_PICKAXE));
        MIDDLE_CHEST_LOOT.add(new ItemStack(Material.DIAMOND_AXE));
    }

    public static List<ItemStack> getLootForChestType(ChestType chestType) {
        switch (chestType) {
            case ISLAND:
                return ISLAND_CHEST_LOOT;
            case MIDDLE:
                return MIDDLE_CHEST_LOOT;
            default:
                throw new IllegalArgumentException("Unknown chest type " + chestType);
        }
    }
}
