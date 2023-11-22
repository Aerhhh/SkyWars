package net.aerh.skywars.util.menu;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.util.ItemBuilder;
import net.aerh.skywars.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CustomMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Map<Integer, ClickAction> actions;
    private final List<BukkitTask> tasks;
    private final boolean closeOnInteract;
    private final boolean addCloseButton;

    protected CustomMenu(String title, int size, boolean closeOnInteract, boolean addCloseButton) {
        this.closeOnInteract = closeOnInteract;
        this.addCloseButton = addCloseButton;
        this.inventory = Bukkit.createInventory(this, size, title);
        this.actions = new HashMap<>();
        this.tasks = new ArrayList<>();
    }

    /**
     * Initializes the items in the inventory.
     */
    protected abstract void initializeItems(Player player);

    /**
     * Opens this menu for the specified player after initializing items.
     *
     * @param player The player for whom to open the menu.
     */
    public void displayTo(Player player) {
        tasks.add(Bukkit.getScheduler().runTaskTimer(SkyWarsPlugin.getInstance(), () -> {
            // I hate this but it works
            actions.clear();
            inventory.clear();
            initializeItems(player);

            if (addCloseButton) {
                addCloseButton();
            }
        }, 0L, Utils.TICKS_PER_SECOND));

        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    /**
     * Sets an item in the inventory at the specified slot.
     *
     * @param slot   The slot to set the item in.
     * @param item   The item to set.
     * @param action The ClickAction associated with the item.
     */
    protected void setItem(int slot, ItemStack item, ClickAction action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    /**
     * Adds an item to the next available slot in the inventory.
     *
     * @param item   The item to add.
     * @param action The ClickAction associated with the item.
     */
    public void addItem(ItemStack item, ClickAction action) {
        int slot = inventory.firstEmpty();

        if (slot != -1) {
            setItem(slot, item, action);
        }
    }

    /**
     * Adds a close button to the inventory at the bottom-middle slot.
     */
    private void addCloseButton() {
        ItemStack closeButton = new ItemBuilder(Material.BARRIER).setDisplayName(ChatColor.RED + "Close").build();
        setItem(inventory.getSize() - 5, closeButton, (player, event) -> player.closeInventory());
    }

    public Map<Integer, ClickAction> getActions() {
        return actions;
    }

    public boolean shouldCloseOnInteract() {
        return closeOnInteract;
    }

    public List<BukkitTask> getTasks() {
        return tasks;
    }
}