package net.aerh.skywars.game;

public abstract class GameEvent {

    protected final SkyWarsGame game;
    private final long delay;

    protected GameEvent(SkyWarsGame game, long delay) {
        this.game = game;
        this.delay = delay;
    }

    public SkyWarsGame getGame() {
        return game;
    }

    public long getDelay() {
        return delay;
    }

    public abstract void execute();
}

