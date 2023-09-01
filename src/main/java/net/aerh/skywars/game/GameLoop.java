package net.aerh.skywars.game;

import net.aerh.skywars.game.event.GameEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;

public class GameLoop {

    private final SkyWarsGame game;
    private final Queue<GameEvent> gameEvents;
    private BukkitTask eventTask;
    private BukkitTask gameEndTask;
    private int countdownTilNextEvent;

    public GameLoop(SkyWarsGame game, Queue<GameEvent> gameEvents) {
        this.game = game;
        this.gameEvents = new LinkedList<>(gameEvents);
    }

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

    public void executeEvent(GameEvent gameEvent) {
        countdownTilNextEvent = 0;
        game.log(Level.INFO, "Executing event: " + gameEvent.getDisplayName() + " (" + gameEvent.getClass().getSimpleName() + ")" + " - " + gameEvents.size() + " events left");
        gameEvent.execute();
        next();
    }

    public String getTimeUntilNextEvent() {
        int minutes = countdownTilNextEvent / 60;
        int seconds = countdownTilNextEvent % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    public GameEvent getNextEvent() {
        return gameEvents.peek();
    }

    public Queue<GameEvent> getGameEvents() {
        return gameEvents;
    }
}
