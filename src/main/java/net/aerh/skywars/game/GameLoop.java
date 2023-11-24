package net.aerh.skywars.game;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.state.GameState;
import net.aerh.skywars.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GameLoop {

    private final SkyWarsGame game;
    private GameEvent currentEvent;
    private long nextEventTime;
    private int gameEndTaskId;

    /**
     * Represents the game loop of a {@link SkyWarsGame}.
     *
     * @param game the {@link SkyWarsGame} to create the game loop for
     */
    public GameLoop(SkyWarsGame game) {
        this.game = game;
    }

    /**
     * Starts the game loop.
     */
    public void next() {
        cancelTasks();

        if (game.getState() == GameState.ENDING) {
            return;
        }

        getNextEventAndRemove().ifPresentOrElse(
            this::startEvent,
            () -> {
                game.log(Level.INFO, "No more events left!");
                game.end();
            });
    }

    /**
     * Schedules the given {@link GameEvent} to be executed.
     *
     * @param gameEvent the {@link GameEvent} to execute
     */
    public void startEvent(GameEvent gameEvent) {
        getCurrentEvent().ifPresent(GameEvent::onTrigger);

        currentEvent = gameEvent;
        currentEvent.onSchedule();

        long delayInMillis = TimeUnit.SECONDS.toMillis(currentEvent.getDelay() / Utils.TICKS_PER_SECOND);
        nextEventTime = System.currentTimeMillis() + delayInMillis;

        Bukkit.getScheduler().runTaskTimer(SkyWarsPlugin.getInstance(), task -> {
            gameEndTaskId = task.getTaskId();

            if (game.getPlayerManager().getAlivePlayers().size() <= 1) {
                game.end();
                task.cancel();
                return;
            }

            if (System.currentTimeMillis() >= nextEventTime) {
                next();
                return;
            }

            game.getPlayerManager().getOnlinePlayers().forEach(skyWarsPlayer -> {
                getNextEvent().ifPresentOrElse(event -> {
                    skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + currentEvent.getDisplayName()
                        + " " + Utils.formatTimeMillis(nextEventTime - System.currentTimeMillis()));
                }, () -> {
                    // If there is no next event, we can assume that the game is ending, so display the game end event name
                    getCurrentEvent().ifPresentOrElse(event -> {
                        skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + event.getDisplayName()
                            + " " + Utils.formatTimeMillis(nextEventTime - System.currentTimeMillis()));
                    }, () -> skyWarsPlayer.getScoreboard().add(8, ChatColor.GRAY + "???"));
                });

                skyWarsPlayer.getScoreboard().update();
            });

            currentEvent.onTick();
        }, 0L, Utils.TICKS_PER_SECOND);


    }

    /**
     * Cancels all tasks.
     */
    void cancelTasks() {
        Bukkit.getScheduler().cancelTask(gameEndTaskId);
        gameEndTaskId = -1;
    }

    /**
     * Gets the next {@link GameEvent} in the queue. Can be null.
     *
     * @return the next {@link GameEvent} in the queue or null if there are no more events left
     */
    public Optional<GameEvent> getNextEvent() {
        return Optional.ofNullable(game.getGameEvents().peek());
    }

    /**
     * Gets the next {@link GameEvent} in the queue and removes it from the queue. Can be null.
     *
     * @return the next {@link GameEvent} in the queue or null if there are no more events left
     */
    public Optional<GameEvent> getNextEventAndRemove() {
        return Optional.ofNullable(game.getGameEvents().poll());
    }

    /**
     * Gets the current {@link GameEvent}.
     *
     * @return the current {@link GameEvent}
     */
    public Optional<GameEvent> getCurrentEvent() {
        return Optional.ofNullable(currentEvent);
    }

    /**
     * Gets the time in milliseconds until the next event.
     *
     * @return the time in milliseconds until the next event
     */
    public long getNextEventTime() {
        return nextEventTime;
    }
}
