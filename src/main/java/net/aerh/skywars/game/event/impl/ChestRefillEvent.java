package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;

public class ChestRefillEvent extends GameEvent {

    public ChestRefillEvent(SkyWarsGame game) {
        super(game, 20L * 60 * 5);
    }

    @Override
    public void execute() {
        // Refill chests around the map

        game.broadcast("[TODO] Chest refilled!");
    }
}
