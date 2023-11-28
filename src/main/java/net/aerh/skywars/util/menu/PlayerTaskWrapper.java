package net.aerh.skywars.util.menu;

import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.BiConsumer;

public class PlayerTaskWrapper {

    private Player player;
    private InventoryItem inventoryItem;
    private final BiConsumer<Player, InventoryItem> playerAction;
    private BukkitTask task;

    public PlayerTaskWrapper(BiConsumer<Player, InventoryItem> playerAction) {
        this.playerAction = playerAction;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public void startTask(int period) {
        this.task = Bukkit.getScheduler().runTaskTimer(SkyWarsPlugin.getInstance(), () -> {
            if (player != null) {
                playerAction.accept(player, inventoryItem);
            }
        }, 0L, period);
    }

    public void stopTask() {
        if (task != null) {
            task.cancel();
        }
    }
}