package net.aerh.skywars.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.aerh.skywars.SkyWarsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class SkyWarsGame {

    private final World world;
    private final GameLoop gameLoop;
    private List<Island> islands;
    private int nextIsland;
    private final Set<Player> players = new HashSet<>();

    public SkyWarsGame(SkyWarsPlugin plugin, World world, JsonObject config) {
        this.world = world;

        Queue<GameEvent> gameEvents = new LinkedList<>();
        //gameEvents.add(new ChestRefillEvent(this, 20 * 60 * 5));  // 5 minutes delay
        //gameEvents.add(new ChestRefillEvent(this, 20 * 60 * 5));  // 5 minutes delay
        //gameEvents.add(new DragonSpawnEvent(this, 20 * 60 * 5));  // 5 minutes delay
        //gameEvents.add(new GameEndEvent(this, 20 * 60 * 5));  // 5 minutes delay

        try {
            this.islands = parseIslands(config.get("islands").getAsJsonArray());
        } catch (IllegalStateException exception) {
            plugin.getLogger().severe("Failed to parse islands!");
            exception.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        this.gameLoop = new GameLoop(plugin, gameEvents);
    }

    public void start() {
        gameLoop.start();
    }

    public void end() {
        gameLoop.stop();
    }

    public void addPlayer(Player player) {
        if (nextIsland < islands.size()) {
            Island island = islands.get(nextIsland++);
            island.assignPlayer(player);
            players.add(player);
        } else {
            player.sendMessage("The game is full!");
        }
    }

    public void removePlayer(Player player) {
        Island island = islands.stream().filter(i -> i.getAssignedPlayer() != null && i.getAssignedPlayer().equals(player)).findFirst().orElse(null);

        if (island != null) {
            island.setAssignedPlayer(null);
        }

        players.remove(player);
    }

    private List<Island> parseIslands(JsonArray islandsArray) {
        List<Island> islands = new ArrayList<>();

        for (JsonElement islandElement : islandsArray) {
            JsonObject island = islandElement.getAsJsonObject();
            long x = island.get("x").getAsLong();
            long y = island.get("y").getAsLong();
            long z = island.get("z").getAsLong();

            Location location = new Location(world, x, y, z);

            islands.add(new Island(location));
        }

        if (islands.isEmpty()) {
            throw new IllegalStateException("No islands found!");
        }

        return islands;
    }

    public Set<Player> getPlayers() {
        return players;
    }
}
