package net.aerh.skywars.game;

import net.aerh.skywars.game.event.GameEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;

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
            game.getPlugin().getLogger().info("No more events left!");
            return;
        }

        GameEvent gameEvent = gameEvents.poll();
        game.getPlugin().getLogger().info("Next event: " + gameEvent.getClass().getSimpleName() + " in " + gameEvent.getDelay() + " ticks (" + gameEvents.size() + " events left)");
        countdownTilNextEvent = (int) gameEvent.getDelay() / 20;
        game.getPlugin().getLogger().info("Time set: " + countdownTilNextEvent);

        if (gameEvent.getDelay() <= 0) {
            game.getPlugin().getLogger().info("Executing event: " + gameEvent.getClass().getSimpleName());
            gameEvent.execute();
            next();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                game.getPlugin().getLogger().info("Countdown: " + countdownTilNextEvent);
                if (countdownTilNextEvent <= 0) {
                    Bukkit.broadcastMessage("cancelled");
                    cancel();
                }

                countdownTilNextEvent--;
            }
        }.runTaskTimer(game.getPlugin(), 0, 20L);

        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                countdownTilNextEvent = 0;
                game.getPlugin().getLogger().info("Executing event: " + gameEvent.getClass().getSimpleName());
                gameEvent.execute();
                next();
            }
        }.runTaskLater(game.getPlugin(), gameEvent.getDelay());
    }

    public int getTimeUntilNextEvent() {
        return countdownTilNextEvent;
    }
}
