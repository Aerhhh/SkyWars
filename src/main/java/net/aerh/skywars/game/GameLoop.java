package net.aerh.skywars.game;

import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.Queue;

public class GameLoop {

    private final SkyWarsPlugin plugin;
    private final Queue<GameEvent> gameEvents;
    private BukkitTask currentTask;
    private int countdownTilNextEvent;

    public GameLoop(SkyWarsPlugin plugin, Queue<GameEvent> gameEvents) {
        this.plugin = plugin;
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
            return;
        }

        GameEvent gameEvent = gameEvents.poll();
        countdownTilNextEvent = (int) gameEvent.getDelay() / 20;
        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameEvent.execute();
                countdownTilNextEvent = 0;
                next();
            }
        }.runTaskLater(plugin, gameEvent.getDelay());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (countdownTilNextEvent <= 0) {
                    this.cancel();
                } else {
                    countdownTilNextEvent--;
                }
            }
        }.runTaskTimer(plugin, 20, 20);  // Run every second
    }
}
