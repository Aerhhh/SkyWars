package net.aerh.skywars.util.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class MenuManager {

    private static MenuManager instance;
    private final Map<Inventory, CustomMenu> menus = new HashMap<>();

    private MenuManager() {
    }

    public static MenuManager getInstance() {
        if (instance == null) {
            instance = new MenuManager();
        }

        return instance;
    }

    public void register(Inventory inventory, CustomMenu handler) {
        menus.put(inventory, handler);
    }

    public void unregister(Inventory inventory) {
        menus.remove(inventory);
    }

    public void openInventory(Player player, CustomMenu customMenu) {
        register(customMenu.getInventory(), customMenu);
        player.openInventory(customMenu.getInventory());
    }

    public void handleOpen(InventoryOpenEvent event) {
        getHandler(event.getInventory()).onOpen(event);
    }

    public void handleClose(InventoryCloseEvent event) {
        getHandler(event.getInventory()).onClose();
        unregister(event.getInventory());
    }

    public void handleClick(InventoryClickEvent event) {
        getHandler(event.getInventory()).onClick(event);
    }

    public CustomMenu getHandler(Inventory inventory) {
        return menus.get(inventory);
    }
}
