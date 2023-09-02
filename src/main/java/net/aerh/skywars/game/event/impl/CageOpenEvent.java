package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.GameSettings;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.island.Island;
import org.bukkit.ChatColor;
import org.bukkit.Material;

import java.util.logging.Level;

public class CageOpenEvent extends GameEvent {

    public CageOpenEvent(SkyWarsGame game) {
        super(game, "Game Start", 0L);
    }

    @Override
    public void execute() {
        game.log(Level.INFO, "Opening cages!");

        GameSettings settings = game.getSettings();
        settings.setHunger(false);
        settings.allowItemDrops(false);
        settings.allowBlockBreaking(false);
        settings.allowBlockPlacing(false);

        for (Island island : game.getIslands()) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 3; y++) {
                    for (int z = -2; z <= 2; z++) {
                        island.getSpawnLocation().clone().add(x, y, z).getBlock().setType(Material.AIR);
                    }
                }
            }
        }

        game.getPlugin().getServer().getScheduler().runTaskLater(game.getPlugin(), () -> {
            settings.allowDamage(true);
            settings.setHunger(true);
            settings.allowItemDrops(true);
            settings.allowBlockBreaking(true);
            settings.allowBlockPlacing(true);
        }, 20L);

        game.broadcast(ChatColor.YELLOW + "Cages opened! " + ChatColor.RED + "FIGHT!");
    }
}
