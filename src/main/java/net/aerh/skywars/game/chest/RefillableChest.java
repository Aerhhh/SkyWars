package net.aerh.skywars.game.chest;

import net.aerh.skywars.util.Hologram;
import net.aerh.skywars.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Directional;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class RefillableChest {

    private final Location location;
    private final ChestType type;
    private int timesRefilled;
    private Hologram timerHologram;

    /**
     * Represents a chest that can be refilled with loot.
     *
     * @param location the {@link Location} of the chest
     * @param type     the {@link ChestType} of the chest
     */
    public RefillableChest(Location location, ChestType type) {
        this.location = location;
        this.type = type;
    }

    /**
     * Spawns the chest at the defined {@link Location}.
     *
     * @param refill   If the chest should be refilled
     * @param rotation the {@link BlockFace rotation} of the chest
     */
    public void spawn(boolean refill, BlockFace rotation) {
        location.getBlock().setType(Material.CHEST);

        BlockState blockState = location.getBlock().getState();
        Directional directional = (Directional) blockState.getBlockData();

        directional.setFacing(rotation);
        location.getBlock().setBlockData(directional);

        if (refill) {
            refillChest();
        }
    }

    /**
     * Refills the chest with loot.
     */
    public void refillChest() {
        if (ChestLootTables.getLootForChestType(type).isEmpty()) {
            throw new IllegalStateException("Cannot refill chest at " + Utils.parseLocationToString(location) + " with empty loot pool!");
        }

        BlockState blockState = location.getBlock().getState();

        if (!(blockState instanceof Chest)) {
            throw new IllegalStateException("Block at " + Utils.parseLocationToString(location) + " is not a chest!");
        }

        Inventory chestInventory = ((Chest) blockState).getBlockInventory();

        if (chestInventory.firstEmpty() == -1) {
            return;
        }

        for (int i = 0; i < type.getMaxRefillItems(); i++) {
            int randomSlot = ThreadLocalRandom.current().nextInt(chestInventory.getSize());

            while (chestInventory.getItem(randomSlot) != null) {
                randomSlot = ThreadLocalRandom.current().nextInt(chestInventory.getSize());
            }

            ItemStack randomItem = ChestLootTables.getLootForChestType(type).getRandomItem().getObject();
            chestInventory.setItem(randomSlot, randomItem);
        }

        timesRefilled++;
    }

    /**
     * Gets the location of this chest.
     *
     * @return the location of this chest
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Gets the type of this chest.
     *
     * @return the type of this chest
     */
    public ChestType getType() {
        return type;
    }

    /**
     * Gets the {@link Inventory} of this chest.
     *
     * @return the {@link Inventory} of this chest
     */
    public Inventory getInventory() {
        return ((Chest) location.getBlock().getState()).getBlockInventory();
    }

    /**
     * Adds an {@link ItemStack item} to a random slot in the chest.
     *
     * @param itemStack the {@link ItemStack} to add
     */
    public void addItemToRandomSlot(ItemStack itemStack) {
        Inventory inventory = getInventory();

        if (inventory.firstEmpty() == -1) {
            return;
        }

        int randomSlot = ThreadLocalRandom.current().nextInt(inventory.getSize());

        while (inventory.getItem(randomSlot) != null) {
            randomSlot = ThreadLocalRandom.current().nextInt(inventory.getSize());
        }

        inventory.setItem(randomSlot, itemStack);
    }

    /**
     * Gets the amount of times this chest has been refilled.
     *
     * @return the amount of times this chest has been refilled
     */
    public int getTimesRefilled() {
        return timesRefilled;
    }

    /**
     * Gets the {@link Hologram} that displays the refill timer.
     *
     * @return the {@link Hologram} that displays the refill timer
     */
    public Optional<Hologram> getTimerHologram() {
        return Optional.ofNullable(timerHologram);
    }

    /**
     * Sets the {@link Hologram} that displays the refill timer.
     *
     * @param timerHologram the {@link Hologram} that displays the refill timer
     */
    public void setTimerHologram(Hologram timerHologram) {
        this.timerHologram = timerHologram;
    }

    /**
     * Removes the {@link Hologram} that displays the refill timer.
     */
    public void removeTimerHologram() {
        getTimerHologram().ifPresent(hologram -> {
            hologram.remove();
            timerHologram = null;
        });
    }
}
