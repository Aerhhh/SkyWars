package net.aerh.skywars.game.event;

import net.aerh.skywars.game.SkyWarsGame;

import java.util.concurrent.TimeUnit;

public abstract class GameEvent {

    private final String displayName;
    protected final SkyWarsGame game;
    private final long delay;

    protected GameEvent(SkyWarsGame game, String displayName, long delay) {
        this.game = game;
        this.displayName = displayName;
        this.delay = delay;
    }

    protected GameEvent(SkyWarsGame game, String displayName, long delay, TimeUnit unit) {
        this(game, displayName, unit.toMillis(delay) / 50L);
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getDelay() {
        return delay;
    }

    public abstract void execute();
}

