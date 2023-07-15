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
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkyWarsGame {

    // TODO could be configured per map
    public static final int MIN_PLAYER_COUNT = 2;
    public static final int MAX_PLAYER_COUNT = 12;

    private final SkyWarsPlugin plugin;
    private final World world;
    private final Location pregameSpawn;
    private final GameLoop gameLoop;
    private final GameSettings settings = new GameSettings();
    private final Set<SkyWarsPlayer> players;
    private final Set<SkyWarsPlayer> spectators;
    private final Queue<GameEvent> gameEvents;
    private GameState state = GameState.PRE_GAME;
    private List<Island> islands;
    private BukkitTask countdownTask;
    private SkyWarsPlayer winner;

    public SkyWarsGame(SkyWarsPlugin plugin, World world, JsonObject config) {
        this.plugin = plugin;
        this.world = world;
        this.players = new HashSet<>();
        this.spectators = new HashSet<>();
        this.gameEvents = new LinkedList<>();

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
            log(Level.SEVERE, "Failed to parse islands!");
            exception.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        this.pregameSpawn = parseLocation(config, "pregame");
        this.gameLoop = new GameLoop(this, gameEvents);
    }

    public void start() {
        state = GameState.IN_GAME;
        broadcast(ChatColor.GREEN + "Game started!");

        getBukkitPlayers().forEach(player -> {
            setupPlayerNameColors(player);
            player.setGameMode(GameMode.SURVIVAL);
        });

        gameLoop.start();
    }

    public void end() {
        gameLoop.stop();
        state = GameState.ENDING;
        broadcast(ChatColor.RED + "Game ended!");

        if (players.size() == 1) {
            winner = players.iterator().next();
            broadcast(ChatColor.GREEN + "Winner: " + winner.getBukkitPlayer().getName());
        } else {
            broadcast(ChatColor.GREEN + "No winner!");
        }

        players.forEach(this::setSpectator);
        players.clear();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player spectator : getBukkitSpectators()) {
                spectator.kickPlayer(ChatColor.RED + "Game ended!");
            }
        }, 20L * 10L);
    }

    private void setupPlayerNameColors(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Team green = scoreboard.registerNewTeam("green");
        Team gray = scoreboard.registerNewTeam("gray");
        Team red = scoreboard.registerNewTeam("red");

        green.setColor(ChatColor.GREEN);
        red.setColor(ChatColor.RED);
        gray.setColor(ChatColor.GRAY);

        green.addEntry(player.getName());
        getBukkitPlayers().stream().filter(otherPlayer -> !otherPlayer.equals(player)).forEach(otherPlayer -> red.addEntry(otherPlayer.getName()));
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

    public void setSpectator(SkyWarsPlayer skyWarsPlayer) {
        removePlayer(skyWarsPlayer);
        spectators.add(skyWarsPlayer);

        Player player = skyWarsPlayer.getBukkitPlayer();

        if (player == null) {
            log(Level.SEVERE, "Failed to set " + skyWarsPlayer.getUuid() + " to spectator mode!");
            return;
        }

        log(Level.INFO, "Setting " + player.getName() + " to spectator mode!");

        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getScoreboard().getTeam("gray").addEntry(player.getName());
        player.teleport(pregameSpawn);

        for (Player otherPlayer : getBukkitPlayers()) {
            otherPlayer.hidePlayer(plugin, player);
            otherPlayer.getScoreboard().getTeam("gray").addEntry(player.getName());
        }

        for (Player spectator : getBukkitSpectators()) {
            if (!spectator.equals(player)) {
                player.hidePlayer(plugin, spectator);
            }
        }
    }

    public boolean addPlayer(SkyWarsPlayer player) {
        Island island = islands.stream().filter(i -> i.getAssignedPlayer() == null).findFirst().orElse(null);

        if (island == null) {
            return false;
        }

        if (state == GameState.IN_GAME || state == GameState.ENDING) {
            log(Level.INFO, "Player " + player.getUuid() + " tried to join but the game is already running!");
            return false;
        }

        players.add(player);
        island.assignPlayer(player);

        checkPlayerCountForCountdown();
        log(Level.INFO, "Added player " + player.getUuid() + " to island " + island.getSpawnLocation() + "!");

        return true;
    }

    public void removePlayer(SkyWarsPlayer player) {
        if (state == GameState.PRE_GAME) {
            checkPlayerCountForCountdown();
        }

        Island island = getIsland(player);

        if (island == null) {
            return;
        }

        island.setAssignedPlayer(null);
        players.remove(player);
        spectators.remove(player);
    }

    public void removePlayerFromPlayersOrSpectators(Player player) {
        if (getPlayer(player) != null) {
            players.remove(getPlayer(player));
        }

        if (getSpectator(player) != null) {
            spectators.remove(getSpectator(player));
        }
    }

    public void teleportPlayers() {
        players.forEach(player -> {
            Island island = getIsland(player);

            if (island == null || player.getBukkitPlayer() == null) {
                return;
            }

            player.getBukkitPlayer().teleport(island.getSpawnLocation().clone().add(0.5, 0, 0.5));
        });
    }

    @Nullable
    public SkyWarsPlayer getPlayer(Player player) {
        return players.stream().filter(p -> p.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    @Nullable
    public SkyWarsPlayer getPlayer(UUID uuid) {
        return players.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    @Nullable
    public SkyWarsPlayer getSpectator(Player player) {
        return spectators.stream().filter(p -> p.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    @Nullable
    public SkyWarsPlayer getSpectator(UUID uuid) {
        return spectators.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    @Nullable
    public Island getIsland(Player player) {
        return getIsland(getPlayer(player));
    }

    @Nullable
    public Player getPlayerOrSpectator(UUID uuid) {
        SkyWarsPlayer player = getPlayer(uuid);

        if (player != null) {
            return player.getBukkitPlayer();
        }

        SkyWarsPlayer spectator = getSpectator(uuid);

        if (spectator != null) {
            return spectator.getBukkitPlayer();
        }

        return null;
    }

    @Nullable
    public Player getPlayerOrSpectator(Player player) {
        return getPlayerOrSpectator(player.getUniqueId());
    }

    public List<Player> getBukkitPlayers() {
        return players.stream().map(SkyWarsPlayer::getBukkitPlayer).filter(Objects::nonNull).collect(Collectors.toList());
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

    @Nullable
    private Island getIsland(SkyWarsPlayer skyWarsPlayer) {
        return islands.stream().filter(island -> island.getAssignedPlayer() != null && island.getAssignedPlayer().equals(skyWarsPlayer)).findFirst().orElse(null);
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
        Stream.concat(players.stream(), spectators.stream())
            .filter(player -> player.getBukkitPlayer() != null)
            .forEach(player -> player.getBukkitPlayer().sendMessage(message));
    }

    public SkyWarsPlugin getPlugin() {
        return plugin;
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

    public Set<Player> getBukkitSpectators() {
        return spectators.stream().map(SkyWarsPlayer::getBukkitPlayer).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public void log(Level level, String message) {
        plugin.getLogger().log(level, "[" + world.getName() + "] " + message);
    }
}
