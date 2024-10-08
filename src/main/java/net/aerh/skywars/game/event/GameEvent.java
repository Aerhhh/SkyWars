package net.aerh.skywars.game.event;

import net.aerh.skywars.game.SkyWarsGame;

import java.util.concurrent.TimeUnit;

public abstract class GameEvent {

    protected final SkyWarsGame game;
    private final String displayName;
    private final long delay;

    /**
     * Creates a new game event.
     *
     * @param game        the game
     * @param displayName the display name of the event
     * @param delay       the delay of the event in ticks
     */
    protected GameEvent(SkyWarsGame game, String displayName, long delay) {
        this.game = game;
        this.displayName = displayName;
        this.delay = delay;
    }

    /**
     * Creates a new game event.
     *
     * @param game        the game
     * @param displayName the display name of the event
     * @param delay       the delay of the event
     * @param unit        the {@link TimeUnit} of the delay
     */
    protected GameEvent(SkyWarsGame game, String displayName, long delay, TimeUnit unit) {
        this(game, displayName, unit.toMillis(delay) / 50L);
    }

    /**
     * Gets the display name of the event.
     *
     * @return the display name of the event
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the delay of the event in ticks.
     *
     * @return the delay of the event in ticks
     */
    public long getDelay() {
        return delay;
    }

    /**
     * Executes when the event is scheduled to start.
     */
    public abstract void onSchedule();

    /**
     * Executes when the event is triggered.
     */
    public abstract void onTrigger();

    /**
     * Triggered every second while the event is running.
     */
    public abstract void onTick();
}

