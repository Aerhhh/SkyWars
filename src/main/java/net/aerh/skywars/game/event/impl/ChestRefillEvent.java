package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.chest.ChestLootTables;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

public class ChestRefillEvent extends GameEvent {

    public ChestRefillEvent(SkyWarsGame game) {
        super(game, 20L * 60L * 5L);
    }

    @Override
    public void execute() {
        game.getRefillableChests().forEach(refillableChest -> {
            // Guarantee ender pearls on the second refill
            if (refillableChest.getTimesRefilled() + 1 == 3) {
                ChestLootTables.getLootForChestType(refillableChest.getType()).add(new ItemStack(Material.ENDER_PEARL, 2));
            }

            refillableChest.refillChest();
            game.log(Level.INFO, "Refilled " + refillableChest.getType() + " chest at " + Utils.parseLocationToString(refillableChest.getLocation()));
        });

        game.broadcastTitle("", ChatColor.YELLOW + "Chests have been refilled!", 10, 20 * 3, 10);
        game.getBukkitPlayers().forEach(bukkitPlayer -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F));
        game.log(Level.INFO, "Refilled all chests!");
    }
}
