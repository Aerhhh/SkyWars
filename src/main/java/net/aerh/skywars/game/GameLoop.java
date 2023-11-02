package net.aerh.skywars.game;

import net.aerh.skywars.game.event.GameEvent;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.stream.Stream;

public class GameLoop {

    private final SkyWarsGame game;
    private final Queue<GameEvent> gameEvents;
    private BukkitTask eventTask;
    private BukkitTask gameEndTask;
    private int countdownTilNextEvent;

    /**
     * Represents the game loop of a {@link SkyWarsGame}.
     *
     * @param game       the {@link SkyWarsGame} to create the game loop for
     * @param gameEvents the {@link Queue} of {@link GameEvent}s to execute
     */
    public GameLoop(SkyWarsGame game, Queue<GameEvent> gameEvents) {
        this.game = game;
        this.gameEvents = new LinkedList<>(gameEvents);
    }

    /**
     * Stops the game loop.
     */
    public void stop() {
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }

        if (gameEndTask != null) {
            gameEndTask.cancel();
            gameEndTask = null;
        }
    }

    /**
     * Starts the game loop.
     */
    public void next() {
        if (eventTask != null) {
            eventTask.cancel();
        }

        if (gameEndTask != null) {
            gameEndTask.cancel();
        }

        if (game.getState() == GameState.ENDING) {
            return;
        }

        GameEvent gameEvent = gameEvents.poll();

        if (gameEvent == null) {
            game.log(Level.INFO, "No more events left!");
            game.end();
            return;
        }

        game.log(Level.INFO, "Next event: " + gameEvent.getDisplayName() + " in " + gameEvent.getDelay() + " ticks (" + gameEvents.size() + " events left)");
        countdownTilNextEvent = (int) gameEvent.getDelay() / 20;

        if (gameEvent.getDelay() <= 0) {
            executeEvent(gameEvent);
            return;
        }

        gameEndTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getBukkitPlayers().size() <= 1) {
                    cancel();
                    game.end();
                    game.log(Level.INFO, "Game ended because there are no more players left!");
                    return;
                }

                if (countdownTilNextEvent <= 0) {
                    cancel();
                }

                Stream.concat(game.getPlayers().stream(), game.getSpectators().stream()).forEach(skyWarsPlayer -> {
                    skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + gameEvent.getDisplayName() + " " + getTimeUntilNextEvent());
                    skyWarsPlayer.getScoreboard().update();
                });

                countdownTilNextEvent--;
            }
        }.runTaskTimer(game.getPlugin(), 0, 20L);

        eventTask = new BukkitRunnable() {
            @Override
            public void run() {
                executeEvent(gameEvent);
            }
        }.runTaskLater(game.getPlugin(), gameEvent.getDelay());
    }

    /**
     * Executes a {@link GameEvent}.
     *
     * @param gameEvent the {@link GameEvent} to execute
     */
    public void executeEvent(GameEvent gameEvent) {
        countdownTilNextEvent = 0;
        game.log(Level.INFO, "Executing event: " + gameEvent.getDisplayName() + " (" + gameEvent.getClass().getSimpleName() + ")" + " - " + gameEvents.size() + " events left");
        gameEvent.execute();
        next();
    }

    /**
     * Gets the time until the next event.
     *
     * @return the time until the next event formatted as a string (mm:ss)
     */
    public String getTimeUntilNextEvent() {
        int minutes = countdownTilNextEvent / 60;
        int seconds = countdownTilNextEvent % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Gets the next {@link GameEvent} in the queue. Can be null.
     *
     * @return the next {@link GameEvent} in the queue or null if there are no more events left
     */
    @Nullable
    public GameEvent getNextEvent() {
        return gameEvents.peek();
    }

    /**
     * Gets the {@link Queue} of {@link GameEvent}s.
     *
     * @return the {@link Queue} of {@link GameEvent}s
     */
    public Queue<GameEvent> getGameEvents() {
        return gameEvents;
    }
}
