package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameSettings;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CageOpenEvent extends GameEvent {

    private final GameSettings settings = game.getSettings();

    public CageOpenEvent(SkyWarsGame game) {
        super(game, "Game Start", 15L, TimeUnit.SECONDS);
    }

    @Override
    public void onSchedule() {
        game.teleportPlayers();
    }

    @Override
    public void onTrigger() {
        game.log(Level.INFO, "Opening cages!");

        for (Island island : game.getIslands()) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 3; y++) {
                    for (int z = -2; z <= 2; z++) {
                        island.getSpawnLocation().clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        game.broadcast(ChatColor.YELLOW + "Cages opened! " + ChatColor.RED + "FIGHT!");

        SkyWarsPlugin.getInstance().getServer().getScheduler().runTaskLater(SkyWarsPlugin.getInstance(), () -> {
            settings.allowDamage(true);
            settings.setHunger(true);
            settings.allowItemDrops(true);
            settings.allowItemPickup(true);
            settings.allowBlockBreaking(true);
            settings.allowBlockPlacing(true);
            settings.setInteractable(true);
        }, Utils.TICKS_PER_SECOND);
    }

    @Override
    public void onTick() {
        // Not needed
    }
}
