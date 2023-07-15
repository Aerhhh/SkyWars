package net.aerh.skywars.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.event.impl.CageOpenEvent;
import net.aerh.skywars.game.event.impl.ChestRefillEvent;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class SkyWarsGame {

    // TODO could be configured per map
    public static final int MIN_PLAYER_COUNT = 2;
    public static final int MAX_PLAYER_COUNT = 12;

    private final SkyWarsPlugin plugin;
    private GameState state = GameState.PRE_GAME;
    private final World world;
    private final Location pregameSpawn;
    private final GameLoop gameLoop;
    private List<Island> islands;
    private BukkitTask countdownTask;
    private final GameSettings settings = new GameSettings();
    private final Set<SkyWarsPlayer> players = new HashSet<>();
    private final Set<Player> spectators = new HashSet<>();
    private SkyWarsPlayer winner;
    private final Queue<GameEvent> gameEvents = new LinkedList<>();

    public SkyWarsGame(SkyWarsPlugin plugin, World world, JsonObject config) {
        this.plugin = plugin;
        this.world = world;

        gameEvents.add(new CageOpenEvent(this));
        gameEvents.add(new ChestRefillEvent(this));
        gameEvents.add(new ChestRefillEvent(this));
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
        this.gameLoop = new GameLoop(this, gameEvents);
    }

    public void start() {
        state = GameState.IN_GAME;
        broadcast(ChatColor.GREEN + "Game started!");
        players.forEach((player) -> {
            setupPlayerNameColors(player);
            player.getBukkitPlayer().setGameMode(GameMode.SURVIVAL);
        });
        gameLoop.start();
    }

    public void end() {
        state = GameState.ENDING;
        gameLoop.stop();
        broadcast(ChatColor.RED + "Game ended!");

        if (players.size() == 1) {
            winner = players.iterator().next();
            broadcast(ChatColor.GREEN + "Winner: " + winner.getBukkitPlayer().getName());
        } else {
            broadcast(ChatColor.GREEN + "No winner!");
        }

        players.forEach(skyWarsPlayer -> setSpectator(skyWarsPlayer.getBukkitPlayer()));
        players.clear();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player spectator : spectators) {
                spectator.kickPlayer(ChatColor.RED + "Game ended!");
            }
        }, 20L * 10L);
    }

    private void setupPlayerNameColors(SkyWarsPlayer skyWarsPlayer) {
        Scoreboard scoreboard = skyWarsPlayer.getScoreboard();
        Team green = scoreboard.registerNewTeam("green");
        Team gray = scoreboard.registerNewTeam("gray");
        Team red = scoreboard.registerNewTeam("red");

        green.setColor(ChatColor.GREEN);
        red.setColor(ChatColor.RED);
        gray.setColor(ChatColor.GRAY);

        green.addEntry(skyWarsPlayer.getBukkitPlayer().getName());
        players.stream()
            .filter(otherPlayer -> !otherPlayer.equals(skyWarsPlayer))
            .forEach(otherPlayer -> red.addEntry(otherPlayer.getBukkitPlayer().getName()));
    }

    private void checkPlayerCountForCountdown() {
        if (players.size() >= MIN_PLAYER_COUNT && (countdownTask == null)) {
            startCountdown();
        }
    }

    private void startCountdown() {
        state = GameState.STARTING;
        plugin.getServer().getScheduler().runTaskLater(plugin, this::teleportPlayers, 10L);

        countdownTask = new BukkitRunnable() {
            int countdown = 10;

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
        }.runTaskTimer(plugin, 10L, 20L);
    }

    public void setSpectator(Player player) {
        removePlayer(player);
        spectators.add(player);

        plugin.getLogger().info("Setting " + player.getName() + " to spectator mode in world " + world.getName());

        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getScoreboard().getTeam("gray").addEntry(player.getName());
        player.teleport(pregameSpawn);

        for (Player otherPlayer : players.stream().map(SkyWarsPlayer::getBukkitPlayer).collect(Collectors.toList())) {
            otherPlayer.hidePlayer(plugin, player);
            otherPlayer.getScoreboard().getTeam("gray").addEntry(player.getName());
        }
    }

    public boolean addPlayer(SkyWarsPlayer player) {
        Island island = islands.stream().filter(i -> i.getAssignedPlayer() == null).findFirst().orElse(null);

        if (island == null) {
            return false;
        }

        if (state == GameState.PRE_GAME || state == GameState.STARTING) {
            island.assignPlayer(player);
            players.add(player);

            checkPlayerCountForCountdown();
            plugin.getLogger().info("Added player " + player.getUuid() + " to island " + island.getSpawnLocation() + "!");
            return true;
        }

        return false;
    }

    public void teleportPlayers() {
        players.forEach(player -> {
            Island island = islands.stream().filter(i -> i.getAssignedPlayer() != null && i.getAssignedPlayer().equals(player)).findFirst().orElse(null);

            if (island == null || player.getBukkitPlayer() == null) {
                return;
            }

            player.getBukkitPlayer().teleport(island.getSpawnLocation().clone().add(0.5, 0, 0.5));
        });
    }

    public void removePlayer(Player player) {
        SkyWarsPlayer skyWarsPlayer = getPlayer(player);
        islands.stream().filter(island -> island.getAssignedPlayer() != null && island.getAssignedPlayer().equals(skyWarsPlayer)).findFirst().ifPresent(island -> island.setAssignedPlayer(null));
        players.remove(skyWarsPlayer);
        plugin.getLogger().info("Removed player " + player.getName() + " from island!");

        if (state == GameState.PRE_GAME) {
            checkPlayerCountForCountdown();
        }
    }

    public SkyWarsPlayer getPlayer(Player player) {
        return players.stream().filter(p -> p.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    public SkyWarsPlayer getPlayer(UUID uuid) {
        return players.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
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
        for (SkyWarsPlayer player : players) {
            if (player.getBukkitPlayer() != null) {
                player.getBukkitPlayer().sendMessage(message);
            }
        }

        for (Player spectator : spectators) {
            spectator.sendMessage(message);
        }
    }

    public SkyWarsPlugin getPlugin() {
        return plugin;
    }

    public Set<SkyWarsPlayer> getPlayers() {
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

    public GameSettings getSettings() {
        return settings;
    }

    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(BukkitTask countdownTask) {
        if (!this.countdownTask.isCancelled()) {
            this.countdownTask.cancel();
        }
        this.countdownTask = countdownTask;
    }

    public Queue<GameEvent> getGameEvents() {
        return gameEvents;
    }

    @Nullable
    public SkyWarsPlayer getWinner() {
        return winner;
    }

    public void setWinner(@Nullable SkyWarsPlayer winner) {
        this.winner = winner;
    }

    public Set<Player> getSpectators() {
        return spectators;
    }
}
