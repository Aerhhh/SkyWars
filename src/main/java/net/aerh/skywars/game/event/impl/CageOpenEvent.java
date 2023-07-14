package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import org.bukkit.Material;

public class CageOpenEvent extends GameEvent {

    public CageOpenEvent(SkyWarsGame game) {
        super(game, 0);
    }

    @Override
    public void execute() {
        game.getPlugin().getLogger().info("Opening cages for game " + game.getWorld());

        for (Island island : getGame().getIslands()) {
            // TODO handle cages more elegantly
            island.getSpawnLocation().getBlock().getRelative(0, -1, 0).setType(Material.AIR);
        }
    }
}
