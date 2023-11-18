package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;

import java.util.concurrent.TimeUnit;

public class GameEndEvent extends GameEvent {

    public GameEndEvent(SkyWarsGame game) {
        super(game, "Game End", 5L, TimeUnit.MINUTES);
    }

    @Override
    public void onStart() {
        game.end();
    }

    @Override
    public void onEnd() {
        // Not needed
    }

    @Override
    public void tick() {
        // Not needed
    }
}
