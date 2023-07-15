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
    private BukkitTask currentTask;
    private int countdownTilNextEvent;

    public GameLoop(SkyWarsGame game, Queue<GameEvent> gameEvents) {
        this.game = game;
        this.gameEvents = new LinkedList<>(gameEvents);
    }

    public void start() {
        next();
    }

    public void stop() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    private void next() {
        if (currentTask != null) {
            currentTask.cancel();
        }

        if (gameEvents.isEmpty()) {
            game.log(Level.INFO, "No more events left!");
            game.end();
            return;
        }

        GameEvent gameEvent = gameEvents.poll();
        game.log(Level.INFO, "Next event: " + gameEvent.getClass().getSimpleName() + " in " + gameEvent.getDelay() + " ticks (" + gameEvents.size() + " events left)");
        countdownTilNextEvent = (int) gameEvent.getDelay() / 20;

        if (gameEvent.getDelay() <= 0) {
            executeEvent(gameEvent);
            return;
        }

        new BukkitRunnable() {
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

        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                executeEvent(gameEvent);
            }
        }.runTaskLater(game.getPlugin(), gameEvent.getDelay());
    }

    private void executeEvent(GameEvent gameEvent) {
        countdownTilNextEvent = 0;
        game.log(Level.INFO, "Executing event: " + gameEvent.getClass().getSimpleName());
        gameEvent.execute();
        next();
    }

    public int getTimeUntilNextEvent() {
        return countdownTilNextEvent;
    }
}
