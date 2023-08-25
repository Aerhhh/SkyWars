package net.aerh.skywars.game.chest;

import net.aerh.skywars.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RefillableChest {

    private final Location location;
    private final List<ItemStack> loot;
    private final ChestType type;
    private int timesRefilled;

    public RefillableChest(Location location, ChestType type) {
        this.location = location;
        this.type = type;
        this.loot = ChestLootTables.getLootForChestType(type);
    }

    public void spawn(boolean refill) {
        location.getBlock().setType(Material.CHEST);

        if (refill) {
            refillChest();
        }
    }

    public void refillChest() {
        if (loot.isEmpty()) {
            throw new IllegalStateException("Cannot refill chest at " + Utils.parseLocationToString(location) + " with empty loot pool!");
        }

        Collections.shuffle(loot);

        BlockState blockState = location.getBlock().getState();
        if (!(blockState instanceof Chest)) {
            throw new IllegalStateException("Block at " + Utils.parseLocationToString(location) + " is not a chest!");
        }

        Inventory chestInventory = ((Chest) blockState).getBlockInventory();

        for (int i = 0; i < type.getMaxRefillItems(); i++) {
            int randomSlot = ThreadLocalRandom.current().nextInt(((Chest) blockState).getBlockInventory().getSize());
            ItemStack randomItem = loot.get(ThreadLocalRandom.current().nextInt(loot.size()));

            chestInventory.setItem(randomSlot, randomItem);
        }

        timesRefilled++;
    }

    public Location getLocation() {
        return location;
    }

    public List<ItemStack> getLoot() {
        return loot;
    }

    public ChestType getType() {
        return type;
    }

    public int getTimesRefilled() {
        return timesRefilled;
    }
}
