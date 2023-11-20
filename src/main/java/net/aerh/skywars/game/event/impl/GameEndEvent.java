package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;

import java.util.concurrent.TimeUnit;

public class GameEndEvent extends GameEvent {

    public GameEndEvent(SkyWarsGame game) {
        super(game, "Game End", 5L, TimeUnit.MINUTES);
    }

    @Override
    public void onSchedule() {
        // Not needed
    }

    @Override
    public void onTrigger() {
        game.end();
    }

    @Override
    public void onTick() {
        // Not needed
    }
}
