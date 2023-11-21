package net.aerh.skywars.util.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.scheduler.BukkitTask;

public class CustomMenuListener implements Listener {

    @EventHandler
    public void onCustomMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomMenu customMenu)) {
            return;
        }

        int slot = event.getRawSlot();

        if (customMenu.getActions().containsKey(slot)) {
            customMenu.getActions().get(slot).onClick((Player) event.getWhoClicked(), event);

            if (customMenu.shouldCloseOnInteract()) {
                event.getWhoClicked().closeInventory();
            }
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onCustomMenuClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomMenu customMenu)) {
            return;
        }

        customMenu.getTasks().forEach(BukkitTask::cancel);
        customMenu.getTasks().clear();
    }
}
