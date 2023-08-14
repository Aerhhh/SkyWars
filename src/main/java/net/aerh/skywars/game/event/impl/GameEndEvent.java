package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;

public class GameEndEvent extends GameEvent {

    public GameEndEvent(SkyWarsGame game) {
        super(game, 20L * 60L * 5L);
    }

    @Override
    public void execute() {
        game.end();
    }
}
