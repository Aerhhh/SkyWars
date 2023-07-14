package net.aerh.skywars.game;

public abstract class GameEvent {

    private final long delay;

    protected GameEvent(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }

    public abstract void execute();
}

