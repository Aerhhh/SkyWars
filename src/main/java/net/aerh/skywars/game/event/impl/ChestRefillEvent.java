package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.util.Hologram;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ChestRefillEvent extends GameEvent {

    private static final int[] ENDER_PEARL_AMOUNTS = {2, 4, 8};

    public ChestRefillEvent(SkyWarsGame game) {
        super(game, "Chest Refill", 5L, TimeUnit.MINUTES);
    }

    @Override
    public void onSchedule() {
        boolean nextRefill = game.getGameLoop().getCurrentEvent().isPresent() && game.getGameLoop().getCurrentEvent().get() instanceof ChestRefillEvent;

        game.getRefillableChests().forEach(refillableChest -> {
            if (nextRefill && refillableChest.getTimerHologram() == null) {
                refillableChest.setTimerHologram(new Hologram(refillableChest.getLocation().clone().add(0.5, 1, 0.5), ""));
                refillableChest.getTimerHologram().spawn();
            }
        });
    }

    @Override
    public void onTick() {
        String timeUntilNextEvent = Utils.formatTimeMillis(game.getGameLoop().getNextEventTime() - System.currentTimeMillis());

        game.getRefillableChests().forEach(refillableChest -> {
            if (refillableChest.getTimerHologram() != null) {
                refillableChest.getTimerHologram().updateText(ChatColor.GREEN + timeUntilNextEvent);
            }
        });
    }

    @Override
    public void onTrigger() {
        game.getRefillableChests().forEach(refillableChest -> {
            if (refillableChest.getTimesRefilled() == 2 && ThreadLocalRandom.current().nextBoolean()) {
                int amount = ENDER_PEARL_AMOUNTS[ThreadLocalRandom.current().nextInt(ENDER_PEARL_AMOUNTS.length)];
                refillableChest.addItemToRandomSlot(new ItemStack(Material.ENDER_PEARL, amount));
            }

            refillableChest.refillChest();
            refillableChest.removeTimerHologram();
        });

        game.broadcastTitle("", ChatColor.YELLOW + "Chests have been refilled!", 10, Utils.TICKS_PER_SECOND * 3, 10);
        game.getBukkitPlayers().forEach(bukkitPlayer -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F));
        game.log(Level.INFO, "Refilled all chests!");
    }
}
