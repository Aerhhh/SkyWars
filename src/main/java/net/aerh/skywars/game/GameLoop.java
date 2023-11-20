package net.aerh.skywars.game;

import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class GameLoop {

    private final SkyWarsGame game;
    private GameEvent currentEvent;
    private long nextEventTime;
    private BukkitTask gameEndTask;

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
     * Executes a {@link GameEvent}.
     *
     * @param gameEvent the {@link GameEvent} to execute
     */
    public void startEvent(GameEvent gameEvent) {
        if (currentEvent != null) {
            currentEvent.onTrigger();
        }

        currentEvent = gameEvent;
        currentEvent.onSchedule();

        long delayInMillis = TimeUnit.SECONDS.toMillis(currentEvent.getDelay() / 20);
        nextEventTime = System.currentTimeMillis() + delayInMillis;

        gameEndTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getAlivePlayers().size() <= 1) {
                    game.end();
                    cancel();
                    return;
                }

                if (System.currentTimeMillis() >= nextEventTime) {
                    next();
                    return;
                }

                game.getOnlinePlayers().forEach(skyWarsPlayer -> {
                    getNextEvent().ifPresentOrElse(event -> {
                        skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + currentEvent.getDisplayName()
                            + " " + Utils.formatTimeMillis(nextEventTime - System.currentTimeMillis()));
                    }, () -> {
                        getCurrentEvent().ifPresentOrElse(event -> {
                            skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + event.getDisplayName()
                                + " " + Utils.formatTimeMillis(nextEventTime - System.currentTimeMillis()));
                        }, () -> skyWarsPlayer.getScoreboard().add(8, ChatColor.GRAY + "???"));
                    });

                    skyWarsPlayer.getScoreboard().update();
                });

                currentEvent.onTick();
            }
        }.runTaskTimer(game.getPlugin(), 0, 20L);
    }

    void cancelTasks() {
        Optional.ofNullable(gameEndTask).ifPresent(BukkitTask::cancel);
    }

    /**
     * Gets the next {@link GameEvent} in the queue. Can be null.
     *
     * @return the next {@link GameEvent} in the queue or null if there are no more events left
     */
    public Optional<GameEvent> getNextEvent() {
        return Optional.ofNullable(game.getGameEvents().peek());
    }

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

    public long getNextEventTime() {
        return nextEventTime;
    }
}
