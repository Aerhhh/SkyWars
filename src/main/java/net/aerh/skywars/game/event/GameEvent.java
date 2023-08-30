package net.aerh.skywars.game.event;

import net.aerh.skywars.game.SkyWarsGame;

import java.util.concurrent.TimeUnit;

public abstract class GameEvent {

    protected final SkyWarsGame game;
    private final long delay;

    protected GameEvent(SkyWarsGame game, long delay) {
        this.game = game;
        this.delay = delay;
    }

    protected GameEvent(SkyWarsGame game, long delay, TimeUnit unit) {
        this(game, unit.toMillis(delay) / 50L);
    }

    public SkyWarsGame getGame() {
        return game;
    }

    public long getDelay() {
        return delay;
    }

    public abstract void execute();
}

