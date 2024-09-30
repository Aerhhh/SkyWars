package net.aerh.skywars.util.menu;

import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public abstract class CustomMenu {

    private final Map<Integer, InventoryItem> elements = new HashMap<>();
    private final Map<Integer, PlayerTaskWrapper> wrappedTasks = new HashMap<>();
    private final Map<Integer, BukkitTask> tasks = new HashMap<>();
    private final Inventory inventory;

    protected CustomMenu(String title, int rows) {
        this.inventory = Bukkit.createInventory(null, 9 * rows, title);
    }

    public void initializeElements(Player player) {
        inventory.clear();

        elements.forEach((integer, inventoryItem) -> {
            inventory.setItem(integer, inventoryItem.getItemStack());

            PlayerTaskWrapper task = wrappedTasks.get(integer);
            task.setPlayer(player);
            task.setInventoryItem(inventoryItem);
            task.startTask(1);

            BukkitTask bukkitTask = SkyWarsPlugin.getInstance().getServer().getScheduler().runTaskTimer(SkyWarsPlugin.getInstance(), () -> {
                if (player != null) {
                    updateElement(integer, inventoryItem.getItemStack());
                }
            }, 0L, 1L);

            tasks.put(integer, bukkitTask);
        });
    }

    public void addElement(ItemStack element, ClickHandler clickHandler) {
        setElement(elements.size(), element, clickHandler);
    }

    public void addAnimatedElement(ItemStack element, ClickHandler clickHandler, PlayerTaskWrapper taskWrapper) {
        setAnimatedElement(elements.size(), element, clickHandler, taskWrapper);
    }

    public void setElement(int slot, ItemStack element, ClickHandler clickHandler) {
        elements.put(slot, new InventoryItem(element, clickHandler));
        inventory.setItem(slot, element);
    }

    public void setAnimatedElement(int slot, ItemStack element, ClickHandler clickHandler, PlayerTaskWrapper taskWrapper) {
        setElement(slot, element, clickHandler);
        wrappedTasks.put(slot, taskWrapper);
    }

    public void updateElement(int slot, ItemStack element) {
        if (inventory.getItem(slot) != null && inventory.getItem(slot).equals(element)) {
            Bukkit.broadcastMessage("Element is the same:\n" + element + "\nvs.\n" + inventory.getItem(slot).toString());
            return;
        }

        elements.get(slot).setItemStack(element);
        inventory.setItem(slot, element);
    }

    public void removeElement(int slot) {
        elements.remove(slot);
        wrappedTasks.remove(slot).stopTask();
        tasks.remove(slot).cancel();
        inventory.setItem(slot, null);
    }

    public void removeElement(InventoryItem element) {
        elements.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(element))
            .findFirst()
            .ifPresent(entry -> removeElement(entry.getKey()));
    }

    public void removeElement(ItemStack element) {
        elements.entrySet()
            .stream()
            .filter(entry -> entry.getValue().getItemStack().equals(element))
            .findFirst()
            .ifPresent(entry -> removeElement(entry.getKey()));
    }

    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        initializeElements(player);
    }

    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        InventoryItem element = elements.get(event.getSlot());

        if (element == null) {
            return;
        }

        element.getClickHandler().onClick(event);
        event.setCancelled(true);
    }

    public void onClose() {
        wrappedTasks.values().forEach(PlayerTaskWrapper::stopTask);
        wrappedTasks.clear();
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
    }

    public Map<Integer, InventoryItem> getElements() {
        return elements;
    }

    public Inventory getInventory() {
        return inventory;
    }
}
