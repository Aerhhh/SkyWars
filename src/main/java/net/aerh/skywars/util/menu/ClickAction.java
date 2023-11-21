package net.aerh.skywars.util.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface ClickAction {
    /**
     * Executes when a player clicks an inventory item
     *
     * @param clicker The player who clicked the item
     * @param event   The event
     */
    void onClick(Player clicker, InventoryClickEvent event);
}
