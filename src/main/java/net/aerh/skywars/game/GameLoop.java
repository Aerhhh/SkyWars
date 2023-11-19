package net.aerh.skywars.game;

import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Optional;
import java.util.logging.Level;

public class GameLoop {

    private final SkyWarsGame game;
    private GameEvent currentEvent;
    private BukkitTask eventTask;
    private BukkitTask gameEndTask;
    private int secondsToNextEvent;

    /**
     * Represents the game loop of a {@link SkyWarsGame}.
     *
     * @param game the {@link SkyWarsGame} to create the game loop for
     */
    public GameLoop(SkyWarsGame game) {
        this.game = game;
    }

    /**
     * Stops the game loop.
     */
    public void stop() {
        if (currentEvent != null) {
            currentEvent.onEnd();
            currentEvent = null;
        }

        cancelTasks();
    }

    /**
     * Starts the game loop.
     */
    public void next(boolean skipped) {
        cancelTasks();

        if (game.getState() == GameState.ENDING) {
            return;
        }

        GameEvent gameEvent = game.getGameEvents().poll();

        if (gameEvent == null) {
            game.log(Level.INFO, "No more events left!");
            game.end();
            return;
        }

        currentEvent = gameEvent;

        game.log(Level.INFO, "Next event: " + gameEvent.getDisplayName() + " in " + gameEvent.getDelay() + " ticks (" + game.getGameEvents().size() + " events left)");
        secondsToNextEvent = (int) gameEvent.getDelay() / 20;

        if (gameEvent.getDelay() <= 0 || skipped) {
            currentEvent.onEnd();
            startEvent(gameEvent);
            return;
        }

        gameEndTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getAlivePlayers().size() <= 1) {
                    cancel();
                    game.end();
                    game.log(Level.INFO, "Game ended because there are no more players left!");
                    return;
                }

                if (secondsToNextEvent <= 0) {
                    cancel();
                }

                String timeUntilNextEvent = Utils.formatTime(secondsToNextEvent);

                game.getOnlinePlayers().forEach(skyWarsPlayer -> {
                    skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + gameEvent.getDisplayName() + " " + timeUntilNextEvent);
                    skyWarsPlayer.getScoreboard().update();
                });

                gameEvent.tick();

                secondsToNextEvent--;
            }
        }.runTaskTimer(game.getPlugin(), 0, 20L);

        eventTask = new BukkitRunnable() {
            @Override
            public void run() {
                startEvent(gameEvent);
            }
        }.runTaskLater(game.getPlugin(), gameEvent.getDelay());
    }

    /**
     * Executes a {@link GameEvent}.
     *
     * @param gameEvent the {@link GameEvent} to execute
     */
    public void startEvent(GameEvent gameEvent) {
        if (currentEvent != null && !currentEvent.equals(gameEvent)) {
            currentEvent.onEnd();
        }

        currentEvent = gameEvent;
        currentEvent.onStart();
        game.log(Level.INFO, "Executing event: " + gameEvent.getDisplayName() + " (" + gameEvent.getClass().getSimpleName() + ")" + " - " + game.getGameEvents().size() + " events left");

        next(false);
    }

    private void cancelTasks() {
        Optional.ofNullable(eventTask).ifPresent(BukkitTask::cancel);
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

    /**
     * Gets the amount of seconds until the next {@link GameEvent}.
     *
     * @return the amount of seconds until the next {@link GameEvent}
     */
    public int getSecondsToNextEvent() {
        return secondsToNextEvent;
    }

    /**
     * Gets the current {@link GameEvent}.
     *
     * @return the current {@link GameEvent}
     */
    public Optional<GameEvent> getCurrentEvent() {
        return Optional.ofNullable(currentEvent);
    }
}
