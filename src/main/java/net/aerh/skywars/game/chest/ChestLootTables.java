package net.aerh.skywars.game.chest;

import net.aerh.skywars.game.loottable.LootTable;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ChestLootTables {

    private static final LootTable<ItemStack> ISLAND_CHEST_LOOT = new LootTable<>();
    private static final LootTable<ItemStack> MIDDLE_CHEST_LOOT = new LootTable<>();

    static {
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.STONE_SWORD), 20);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.IRON_SWORD), 5);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.BOW), 10);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.ARROW, 8), 10);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.OAK_PLANKS, 16), 50);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.WATER_BUCKET), 5);

        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.LEATHER_HELMET), 10);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.LEATHER_CHESTPLATE), 10);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.LEATHER_LEGGINGS), 10);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.LEATHER_BOOTS), 10);

        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.CHAINMAIL_HELMET), 5);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.CHAINMAIL_CHESTPLATE), 5);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.CHAINMAIL_LEGGINGS), 5);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.CHAINMAIL_BOOTS), 5);

        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.GOLDEN_HELMET), 3);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.GOLDEN_CHESTPLATE), 3);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.GOLDEN_LEGGINGS), 3);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.GOLDEN_BOOTS), 3);

        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.IRON_HELMET), 2);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.IRON_CHESTPLATE), 2);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.IRON_LEGGINGS), 2);
        ISLAND_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.IRON_BOOTS), 2);

        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.DIAMOND_SWORD), 5);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.IRON_SWORD), 15);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.BOW), 10);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.ARROW, 8), 10);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.OAK_PLANKS, 16), 15);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.TNT, 4), 8);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.FLINT_AND_STEEL), 8);
        MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.LAVA_BUCKET), 5);

        for (int i = 0; i < 2; i++) {
            MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.ENDER_PEARL, i), i * 3);
        }

        for (int i = 0; i < 3; i++) {
            MIDDLE_CHEST_LOOT.addItemWithWeight(new ItemStack(Material.GOLDEN_APPLE, i), 5);
        }
    }

    private ChestLootTables() {
        throw new UnsupportedOperationException("Cannot instantiate utility class.");
    }

    /**
     * Returns the {@link LootTable} for the given {@link ChestType}.
     *
     * @param chestType the {@link ChestType} to get the {@link LootTable} for
     * @return the {@link LootTable} for the given {@link ChestType}
     */
    public static LootTable<ItemStack> getLootForChestType(@NotNull ChestType chestType) {
        if (chestType == ChestType.MIDDLE) {
            return MIDDLE_CHEST_LOOT;
        } else {
            return ISLAND_CHEST_LOOT;
        }
    }
}
