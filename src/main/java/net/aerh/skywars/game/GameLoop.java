package net.aerh.skywars.game;

import net.aerh.skywars.game.chest.RefillableChest;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.event.impl.ChestRefillEvent;
import net.aerh.skywars.util.Hologram;
import net.aerh.skywars.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class GameLoop {

    private final SkyWarsGame game;
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

        GameEvent gameEvent = game.getGameEvents().peek();

        if (gameEvent == null) {
            game.log(Level.INFO, "No more events left!");
            game.end();
            return;
        }

        game.log(Level.INFO, "Next event: " + gameEvent.getDisplayName() + " in " + gameEvent.getDelay() + " ticks (" + game.getGameEvents().size() + " events left)");
        secondsToNextEvent = (int) gameEvent.getDelay() / 20;

        if (gameEvent.getDelay() <= 0) {
            executeEvent(gameEvent);
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

                if (gameEvent instanceof ChestRefillEvent) {
                    game.getRefillableChests().forEach(refillableChest -> {
                        if (refillableChest.getTimerHologram() == null) {
                            refillableChest.setTimerHologram(new Hologram(refillableChest.getLocation().clone().add(0.5, 1, 0.5), ChatColor.GREEN + timeUntilNextEvent));
                            refillableChest.getTimerHologram().spawn();
                        }

                        refillableChest.getTimerHologram().updateText(ChatColor.GREEN + timeUntilNextEvent);
                    });
                } else {
                    game.getRefillableChests().stream()
                        .filter(refillableChest -> refillableChest.getTimerHologram() != null)
                        .forEach(RefillableChest::removeTimerHologram);
                }

                secondsToNextEvent--;
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
        secondsToNextEvent = 0;
        game.log(Level.INFO, "Executing event: " + gameEvent.getDisplayName() + " (" + gameEvent.getClass().getSimpleName() + ")" + " - " + game.getGameEvents().size() + " events left");
        gameEvent.execute();
        game.getGameEvents().remove();
        next();
    }

    /**
     * Gets the next {@link GameEvent} in the queue. Can be null.
     *
     * @return the next {@link GameEvent} in the queue or null if there are no more events left
     */
    @Nullable
    public GameEvent getNextEvent() {
        return game.getGameEvents().peek();
    }

    /**
     * Gets the names of the {@link GameEvent}s in the queue as a {@link List} of strings.
     *
     * @return the names of the {@link GameEvent}s in the queue as a {@link List} of strings
     */
    public List<String> getGameEventNames() {
        return game.getGameEvents().stream().map(gameEvent -> gameEvent.getClass().getSimpleName()).collect(Collectors.toList());
    }

    public int getSecondsToNextEvent() {
        return secondsToNextEvent;
    }
}
