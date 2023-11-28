package net.aerh.skywars.util.menu;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

public class MenuListener implements Listener {

    @EventHandler
    public void onCustomMenuOpen(InventoryOpenEvent event) {
        if (MenuManager.getInstance().getHandler(event.getInventory()) != null) {
            MenuManager.getInstance().handleOpen(event);
        }
    }

    @EventHandler
    public void onCustomMenuClose(InventoryCloseEvent event) {
        if (MenuManager.getInstance().getHandler(event.getInventory()) != null) {
            MenuManager.getInstance().handleClose(event);
        }
    }

    @EventHandler
    public void onCustomMenuClick(InventoryClickEvent event) {
        if (MenuManager.getInstance().getHandler(event.getInventory()) != null) {
            MenuManager.getInstance().handleClick(event);
        }
    }
}
