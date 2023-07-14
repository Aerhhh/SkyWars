package net.aerh.skywars.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.event.impl.CageOpenEvent;
import net.aerh.skywars.game.island.Island;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class SkyWarsGame {

    // TODO could be configured per map
    public static final int MIN_PLAYER_COUNT = 2;
    public static final int MAX_PLAYER_COUNT = 12;

    private final SkyWarsPlugin plugin;
    private GameState state;
    private final World world;
    private final Location pregameSpawn;
    private final GameLoop gameLoop;
    private List<Island> islands;
    private BukkitTask countdownTask;
    private final Set<Player> players = new HashSet<>();

    public SkyWarsGame(SkyWarsPlugin plugin, World world, JsonObject config) {
        this.plugin = plugin;
        this.world = world;

        Queue<GameEvent> gameEvents = new LinkedList<>();
        gameEvents.add(new CageOpenEvent(this));
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

        this.pregameSpawn = parseLocation(config, "pregame");
        this.gameLoop = new GameLoop(plugin, gameEvents);
    }

    public void start() {
        state = GameState.IN_GAME;
        gameLoop.start();
        broadcast(ChatColor.GREEN + "Game started!");
    }

    public void end() {
        state = GameState.ENDING;
        gameLoop.stop();
        broadcast(ChatColor.RED + "Game ended!");
    }

    private void checkPlayerCountForCountdown() {

        if (players.size() >= MIN_PLAYER_COUNT && (countdownTask == null)) {
            startCountdown();
        }
    }

    private void startCountdown() {
        int countdownSeconds = 10;
        countdownTask = new BukkitRunnable() {
            int countdown = countdownSeconds;

            @Override
            public void run() {
                if (players.size() < MIN_PLAYER_COUNT) {
                    cancel();
                    countdownTask = null;
                    broadcast(ChatColor.RED + "Not enough players to start the game!");
                    return;
                }

                if (countdown <= 0) {
                    start();
                    cancel();
                    countdownTask = null;
                } else {
                    broadcast(ChatColor.YELLOW + "Game starting in " + ChatColor.RED + countdown + ChatColor.YELLOW + " second" + (countdown == 1 ? "" : "s") + "!");
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0, 20L);
    }

    public boolean addPlayer(Player player) {
        Island island = islands.stream().filter(i -> i.getAssignedPlayer() == null).findFirst().orElse(null);

        if (island == null) {
            return false;
        }

        island.assignPlayer(player);
        players.add(player);
        plugin.getLogger().info("Assigned player " + player.getName() + " to island " + island.getSpawnLocation() + "!");
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.teleport(island.getSpawnLocation().clone().add(0.5, 0, 0.5)), 1L);

        checkPlayerCountForCountdown();

        return true;
    }

    public void removePlayer(Player player) {
        islands.stream().filter(island -> island.getAssignedPlayer() != null && island.getAssignedPlayer().equals(player)).findFirst().ifPresent(island -> island.setAssignedPlayer(null));
        players.remove(player);
        checkPlayerCountForCountdown();
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

    private Location parseLocation(JsonObject config, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");
        JsonObject desiredLocation = locationsObject.getAsJsonObject(field);

        Location location = new Location(world, desiredLocation.get("x").getAsDouble(), desiredLocation.get("y").getAsDouble(), desiredLocation.get("z").getAsDouble());

        if (desiredLocation.has("yaw")) {
            location.setYaw(desiredLocation.get("yaw").getAsFloat());
        }

        if (desiredLocation.has("pitch")) {
            location.setPitch(desiredLocation.get("pitch").getAsFloat());
        }

        return location;
    }

    public void broadcast(String message) {
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    public SkyWarsPlugin getPlugin() {
        return plugin;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Location getPregameSpawn() {
        return pregameSpawn;
    }

    public World getWorld() {
        return world;
    }

    public GameLoop getGameLoop() {
        return gameLoop;
    }

    public List<Island> getIslands() {
        return islands;
    }

    public GameState getState() {
        return state;
    }
}
