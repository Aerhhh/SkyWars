package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.GameSettings;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import org.bukkit.Material;

public class CageOpenEvent extends GameEvent {

    public CageOpenEvent(SkyWarsGame game) {
        super(game, 0L);
    }

    @Override
    public void execute() {
        game.getPlugin().getLogger().info("Opening cages for game " + game.getWorld());

        GameSettings settings = game.getSettings();
        settings.setHunger(false);
        settings.setDropItem(false);

        for (Island island : getGame().getIslands()) {
            // TODO handle cages more elegantly
            island.getSpawnLocation().getBlock().getRelative(0, -1, 0).setType(Material.AIR);
        }

        game.getPlugin().getServer().getScheduler().runTaskLater(game.getPlugin(), () -> {
            settings.setDamage(true);
            settings.setHunger(true);
            settings.setDropItem(true);
        }, 20L);
    }
}
