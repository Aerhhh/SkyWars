package net.aerh.skywars.game;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.chest.ChestType;
import net.aerh.skywars.game.chest.RefillableChest;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.event.impl.CageOpenEvent;
import net.aerh.skywars.game.event.impl.ChestRefillEvent;
import net.aerh.skywars.game.event.impl.DragonSpawnEvent;
import net.aerh.skywars.game.event.impl.GameEndEvent;
import net.aerh.skywars.game.island.Island;
import net.aerh.skywars.player.PlayerScoreboard;
import net.aerh.skywars.player.SkyWarsPlayer;
import net.aerh.skywars.util.CenteredMessage;
import net.aerh.skywars.util.Utils;
import net.aerh.skywars.util.WorldSignParser;
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
    private final Set<RefillableChest> refillableChests;
    private GameState state = GameState.PRE_GAME;
    private List<Island> islands;
    private BukkitTask countdownTask;
    private SkyWarsPlayer winner;
    private final String mapName;

    /**
     * Represents a game of SkyWars.
     *
     * @param plugin the {@link SkyWarsPlugin} instance
     * @param world  the {@link World} of this game
     * @param config the {@link JsonObject} to parse things from
     */
    public SkyWarsGame(SkyWarsPlugin plugin, World world, JsonObject config) {
        this.plugin = plugin;
        this.world = world;
        this.players = new HashSet<>();
        this.spectators = new HashSet<>();
        this.gameEvents = new LinkedList<>();
        this.refillableChests = new HashSet<>();
        this.mapName = config.get("name").getAsString();
        this.pregameSpawn = parseLocation(config, "pregame");

        gameEvents.add(new CageOpenEvent(this));
        gameEvents.add(new ChestRefillEvent(this));
        gameEvents.add(new ChestRefillEvent(this));
        gameEvents.add(new DragonSpawnEvent(this));
        gameEvents.add(new GameEndEvent(this));

        this.gameLoop = new GameLoop(this, gameEvents);

        try {
            this.islands = parseIslands(config.get("islands").getAsJsonArray());
        } catch (IllegalStateException exception) {
            log(Level.SEVERE, "Failed to parse islands!");
            exception.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        // I made this because the map I'm using to test already has chest signs so it'll be quicker to just use them
        WorldSignParser signParser = new WorldSignParser(plugin, world, true);

        signParser.getParsedSigns("chest").forEach(sign -> {
            Location location = sign.getLocation();
            ChestType chestType = ChestType.valueOf(sign.getOptions().get(0).toUpperCase());
            RefillableChest refillableChest = new RefillableChest(location, chestType);
            refillableChests.add(refillableChest);
            refillableChest.spawn(true, sign.getRotation());
            log(Level.INFO, "Registered refillable " + chestType + " chest at " + location);
        });
    }

    /**
     * Starts the game.
     */
    public void start() {
        state = GameState.IN_GAME;
        broadcast(ChatColor.GREEN + "Game started!");

        broadcast(Utils.SEPARATOR);
        broadcast(CenteredMessage.generate(ChatColor.RESET + ChatColor.BOLD.toString() + "SkyWars"));
        broadcast("\n");
        broadcast(CenteredMessage.generate(ChatColor.YELLOW + "Gather resources and equipment on your"));
        broadcast(CenteredMessage.generate(ChatColor.YELLOW + "island in order to eliminate every other player."));
        broadcast(CenteredMessage.generate(ChatColor.YELLOW + "Go to the center island for special chests"));
        broadcast(CenteredMessage.generate(ChatColor.YELLOW + "with special items!"));
        broadcast("\n");
        broadcast(Utils.SEPARATOR);

        getPlayers().forEach(skyWarsPlayer -> {
            log(Level.INFO, "Setting scoreboard for " + skyWarsPlayer.getUuid() + "!");

            Player player = skyWarsPlayer.getBukkitPlayer();

            setupScoreboard(player, skyWarsPlayer.getScoreboard());
            setupPlayerNameColors(player);
            player.setGameMode(GameMode.SURVIVAL);
        });

        gameLoop.next();
    }

    /**
     * Ends the game.
     */
    public void end() {
        gameLoop.stop();
        state = GameState.ENDING;

        broadcast(ChatColor.RED + "Game ended!");

        getPlayers().forEach(skyWarsPlayer -> {
            skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + "Game over!");
            skyWarsPlayer.getScoreboard().update();
        });

        if (players.size() == 1) {
            winner = players.iterator().next();
        }

        broadcast(Utils.SEPARATOR);
        broadcast("\n");

        if (winner != null) {
            broadcast("\n");
            broadcast(CenteredMessage.generate(ChatColor.RESET + ChatColor.BOLD.toString() + "Winner: " + ChatColor.GOLD + winner.getBukkitPlayer().getName()));
        } else {
            broadcast("\n");
            broadcast(CenteredMessage.generate(ChatColor.RED + "Nobody won the game!"));
        }

        broadcast("\n");

        List<SkyWarsPlayer> topPlayers = getTopPlayers();

        if (!topPlayers.isEmpty()) {
            broadcast(CenteredMessage.generate(ChatColor.RESET + ChatColor.BOLD.toString() + "Top Kills:"));
            for (SkyWarsPlayer topPlayer : topPlayers) {
                broadcast(CenteredMessage.generate(ChatColor.GOLD + topPlayer.getBukkitPlayer().getName() + ChatColor.RESET + " - " + topPlayer.getKills() + " kill" + (topPlayer.getKills() == 1 ? "" : "s")));
            }
        }

        broadcast("\n");
        broadcast(Utils.SEPARATOR);

        for (SkyWarsPlayer skyWarsPlayer : players) {
            if (winner != null && winner.getUuid().equals(skyWarsPlayer.getUuid())) {
                continue;
            }

            setSpectator(skyWarsPlayer);
        }

        players.clear();
        islands.clear();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player spectator : getBukkitSpectators()) {
                spectator.kickPlayer(ChatColor.RED + "Game ended!");
            }

            if (winner != null && winner.getBukkitPlayer() != null) {
                winner.getBukkitPlayer().kickPlayer(ChatColor.GREEN + "You won!");
            }

            spectators.clear();
            plugin.getGameManager().getGames().remove(this);
        }, 20L * 30L);
    }

    private void setupScoreboard(Player player, PlayerScoreboard scoreboard) {
        scoreboard.add(10, " ");
        scoreboard.add(9, ChatColor.RESET + "Next Event:");
        scoreboard.add(8, ChatColor.GRAY + "???");
        scoreboard.add(7, "  ");
        scoreboard.add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getPlayers().size());
        scoreboard.add(5, ChatColor.RESET + "Kills: " + ChatColor.GREEN + "0");
        scoreboard.add(4, "   ");
        scoreboard.add(3, ChatColor.RESET + "Map: " + ChatColor.GREEN + mapName);
        scoreboard.add(2, "    ");
        scoreboard.add(1, ChatColor.YELLOW + "www.aerh.net");
        scoreboard.update();

        scoreboard.send(player);
    }

    /**
     * Sets up player name colors on the scoreboard. Players will see other players as red and themselves as green.
     *
     * @param player the {@link Player} to set up the colors for
     */
    private void setupPlayerNameColors(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Team green = scoreboard.registerNewTeam("green");
        Team gray = scoreboard.registerNewTeam("gray");
        Team red = scoreboard.registerNewTeam("red");

        green.setColor(ChatColor.GREEN);
        red.setColor(ChatColor.RED);
        gray.setColor(ChatColor.GRAY);

        green.addEntry(player.getName());
        getBukkitPlayers().stream()
            .filter(otherPlayer -> !otherPlayer.getUniqueId().equals(player.getUniqueId()))
            .forEach(otherPlayer -> red.addEntry(otherPlayer.getName()));
    }

    /**
     * Checks the player count to start the countdown.
     */
    private void checkPlayerCountForCountdown() {
        if (players.size() >= MIN_PLAYER_COUNT && (countdownTask == null)) {
            startCountdown();
        }
    }

    /**
     * Starts the countdown to start the game.
     */
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

    /**
     * Sets a {@link SkyWarsPlayer} to a spectator.
     *
     * @param skyWarsPlayer the {@link SkyWarsPlayer} to set
     */
    public void setSpectator(SkyWarsPlayer skyWarsPlayer) {
        spectators.add(skyWarsPlayer);

        Player player = skyWarsPlayer.getBukkitPlayer();

        if (player == null) {
            log(Level.SEVERE, "Failed to set " + skyWarsPlayer.getUuid() + " to spectator mode!");
            return;
        }

        log(Level.INFO, "Setting " + player.getName() + " to spectator mode!");

        player.setHealth(20.0D);
        player.setFoodLevel(20);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getScoreboard().getTeam("gray").addEntry(player.getName());
        player.teleport(pregameSpawn);

        Stream.concat(players.stream(), spectators.stream()).forEach(swPlayer -> {
            Player bukkitPlayer = swPlayer.getBukkitPlayer();

            if (bukkitPlayer != null) {
                bukkitPlayer.hidePlayer(plugin, player);
            }

            swPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getPlayers().size());
            swPlayer.getScoreboard().update();
        });
    }

    /**
     * Adds a {@link SkyWarsPlayer} to the game. Returns false if the game is already running or if there are no islands left.
     *
     * @param player the {@link SkyWarsPlayer} to add
     * @return true if the {@link SkyWarsPlayer} was added, false otherwise
     */
    public boolean addPlayer(SkyWarsPlayer player) {
        log(Level.INFO, "Adding player " + player.getUuid());

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
        log(Level.INFO, "Added player " + player.getUuid() + " to island " + Utils.parseLocationToString(island.getSpawnLocation()) + "!");

        if (state != GameState.STARTING) {
            Stream.concat(players.stream(), spectators.stream())
                .filter(skyWarsPlayer -> skyWarsPlayer.getScoreboard() != null)
                .forEach(skyWarsPlayer -> {
                    skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getPlayers().size());
                    skyWarsPlayer.getScoreboard().update();
                });
        }

        return true;
    }

    /**
     * Removes a {@link SkyWarsPlayer} from the game.
     *
     * @param player the {@link SkyWarsPlayer} to remove
     */
    public void removePlayer(SkyWarsPlayer player) {
        players.remove(player);
        spectators.remove(player);

        if (state == GameState.PRE_GAME) {
            checkPlayerCountForCountdown();
        }

        Island island = getIsland(player);

        if (island != null) {
            island.setAssignedPlayer(null);
        }

        if (state != GameState.STARTING) {
            Stream.concat(players.stream(), spectators.stream()).forEach(skyWarsPlayer -> {
                skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getPlayers().size());
                skyWarsPlayer.getScoreboard().update();
            });
        }
    }

    /**
     * Teleports all players to their island spawn locations.
     */
    public void teleportPlayers() {
        players.forEach(player -> {
            Island island = getIsland(player);

            if (island == null || player.getBukkitPlayer() == null) {
                return;
            }

            player.getBukkitPlayer().teleport(island.getSpawnLocation().clone().add(0.5, 0, 0.5));
        });
    }

    /**
     * Gets a {@link SkyWarsPlayer} by their {@link Player} object.
     *
     * @param player the {@link Player} to get
     * @return the {@link SkyWarsPlayer} with the {@link Player}. Can be null
     */
    @Nullable
    public SkyWarsPlayer getPlayer(Player player) {
        return players.stream().filter(p -> p.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    /**
     * Gets a {@link SkyWarsPlayer} by their {@link UUID}.
     *
     * @param uuid the {@link UUID} to get
     * @return the {@link SkyWarsPlayer} with the {@link UUID}. Can be null
     */
    @Nullable
    public SkyWarsPlayer getPlayer(UUID uuid) {
        return players.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    /**
     * Gets a spectator by their {@link Player} object.
     *
     * @param player the {@link Player} to get
     * @return the {@link SkyWarsPlayer} who is a spectator. Returns null if the {@link SkyWarsPlayer} is not a spectator
     */
    @Nullable
    public SkyWarsPlayer getSpectator(Player player) {
        return spectators.stream().filter(p -> p.getUuid().equals(player.getUniqueId())).findFirst().orElse(null);
    }

    /**
     * Gets a {@link SkyWarsPlayer} who is a spectator.
     *
     * @param uuid the {@link UUID} of the {@link SkyWarsPlayer} to get
     * @return the {@link SkyWarsPlayer} who is a spectator. Returns null if the {@link SkyWarsPlayer} is not a spectator
     */
    @Nullable
    public SkyWarsPlayer getSpectator(UUID uuid) {
        return spectators.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    /**
     * Gets the {@link Island} of a {@link Player}. Can be null.
     *
     * @param player the {@link Player} to get the {@link Island} of
     * @return the {@link Island} of the {@link Player}. Can be null
     */
    @Nullable
    public Island getIsland(Player player) {
        return getIsland(getPlayer(player));
    }

    /**
     * Gets a {@link Player} in the game whether they are a player or a spectator.
     *
     * @param uuid the {@link UUID} of the {@link Player} to get
     * @return the {@link Player} in the game. Can be null if the {@link Player} is not in the game
     */
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

    /**
     * Gets a {@link Player} in the game whether they are a player or a spectator.
     *
     * @param player the {@link Player} to get
     * @return the {@link Player} in the game. Can be null
     */
    @Nullable
    public Player getPlayerOrSpectator(Player player) {
        return getPlayerOrSpectator(player.getUniqueId());
    }

    /**
     * Gets a {@link List} of the {@link SkyWarsPlayer players} in this game.
     *
     * @return the {@link List} of the {@link SkyWarsPlayer players} in this game
     */
    public Set<SkyWarsPlayer> getPlayers() {
        return players;
    }

    /**
     * Gets a {@link List} of the {@link SkyWarsPlayer spectators} in this game.
     *
     * @return the {@link List} of the {@link SkyWarsPlayer spectators} in this game
     */
    public Set<SkyWarsPlayer> getSpectators() {
        return spectators;
    }

    /**
     * Gets a {@link List} of the {@link Player players} in this game.
     *
     * @return the {@link List} of the {@link Player players} in this game
     */
    public List<Player> getBukkitPlayers() {
        return players.stream().map(SkyWarsPlayer::getBukkitPlayer).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Gets a {@link List} of the top 3 {@link SkyWarsPlayer players} based on kills.
     *
     * @return the {@link List} of the top 3 {@link SkyWarsPlayer players} based on kills
     */
    private List<SkyWarsPlayer> getTopPlayers() {
        return Stream.concat(players.stream(), spectators.stream()).collect(Collectors.toList())
            .stream()
            .sorted(Comparator.comparingInt(SkyWarsPlayer::getKills).reversed())
            .limit(3)
            .collect(Collectors.toList());
    }

    /**
     * Parses the islands from a {@link JsonArray}.
     *
     * @param islandsArray the {@link JsonArray} to parse from
     * @return the parsed {@link List} of {@link Island islands}
     */
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

    /**
     * Gets the {@link Island} of a {@link SkyWarsPlayer}. Can be null.
     *
     * @param skyWarsPlayer the {@link SkyWarsPlayer} to get the {@link Island} of
     * @return the {@link Island} of the {@link SkyWarsPlayer}. Can be null
     */
    @Nullable
    public Island getIsland(SkyWarsPlayer skyWarsPlayer) {
        return islands.stream().filter(island -> island.getAssignedPlayer() != null && island.getAssignedPlayer().equals(skyWarsPlayer)).findFirst().orElse(null);
    }

    /**
     * Parses a {@link Location} from a {@link JsonObject}.
     *
     * @param config the {@link JsonObject} to parse from
     * @param field  the field to parse
     * @return the parsed {@link Location}
     */
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

    /**
     * Broadcasts a message to all players and spectators.
     *
     * @param message the message
     */
    public void broadcast(String message) {
        Stream.concat(players.stream(), spectators.stream())
            .filter(player -> player.getBukkitPlayer() != null)
            .forEach(player -> player.getBukkitPlayer().sendMessage(message));
    }

    /**
     * Broadcasts a title to all players and spectators.
     *
     * @param title    the title
     * @param subtitle the subtitle
     * @param fadeIn   the fade in time in ticks
     * @param stay     the stay time in ticks
     * @param fadeOut  the fade out time in ticks
     */
    public void broadcastTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Stream.concat(players.stream(), spectators.stream())
            .filter(player -> player.getBukkitPlayer() != null)
            .forEach(player -> player.getBukkitPlayer().sendTitle(title, subtitle, fadeIn, stay, fadeOut));
    }

    /**
     * Gets the {@link SkyWarsPlugin} instance.
     *
     * @return the {@link SkyWarsPlugin} instance
     */
    public SkyWarsPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the pregame spawn {@link Location location}.
     *
     * @return the pregame spawn location
     */
    public Location getPregameSpawn() {
        return pregameSpawn;
    }

    /**
     * Gets the {@link World} of this game.
     *
     * @return the {@link World} of this game
     */
    public World getWorld() {
        return world;
    }

    /**
     * Gets the {@link GameLoop} for this game.
     *
     * @return the {@link GameLoop} for this game
     */
    public GameLoop getGameLoop() {
        return gameLoop;
    }

    /**
     * Gets the {@link List} of {@link Island islands} in this game.
     *
     * @return the {@link List} of {@link Island islands}
     */
    public List<Island> getIslands() {
        return islands;
    }

    /**
     * Gets the {@link Set} of {@link RefillableChest chests} in this game.
     *
     * @return the {@link Set} of {@link RefillableChest chests}
     */
    public Set<RefillableChest> getRefillableChests() {
        return refillableChests;
    }

    /**
     * Checks if a {@link Location} is a refillable chest.
     *
     * @param location the {@link Location} to check
     * @return true if the {@link Location} is a {@link RefillableChest}, false otherwise
     */
    public boolean isRefillableChest(Location location) {
        return refillableChests.stream().anyMatch(refillableChest -> Utils.locationsMatch(refillableChest.getLocation(), location));
    }

    /**
     * Removes a {@link RefillableChest} from this game.
     *
     * @param location the {@link Location} of the {@link RefillableChest}
     */
    public void removeRefillableChest(Location location) {
        refillableChests.removeIf(refillableChest -> Utils.locationsMatch(refillableChest.getLocation(), location));
    }

    /**
     * Gets the {@link GameState} of this game.
     *
     * @return the {@link GameState} of this game
     */
    public GameState getState() {
        return state;
    }

    /**
     * Gets the {@link GameSettings} for this game.
     *
     * @return the {@link GameSettings} for this game
     */
    public GameSettings getSettings() {
        return settings;
    }

    /**
     * Gets the {@link BukkitTask} for the countdown.
     *
     * @return the {@link BukkitTask} for the countdown
     */
    public BukkitTask getCountdownTask() {
        return countdownTask;
    }

    /**
     * Sets the {@link BukkitTask} for the countdown and cancels the previous one if it exists.
     *
     * @param countdownTask the {@link BukkitTask} for the countdown
     */
    public void setCountdownTask(BukkitTask countdownTask) {
        if (!this.countdownTask.isCancelled()) {
            this.countdownTask.cancel();
        }
        this.countdownTask = countdownTask;
    }

    /**
     * Gets the {@link Queue} of {@link GameEvent events} for this game.
     *
     * @return the {@link Queue} of {@link GameEvent events}
     */
    public Queue<GameEvent> getGameEvents() {
        return gameEvents;
    }

    /**
     * Gets the winner of this game. Can be null.
     *
     * @return the {@link SkyWarsPlayer player} who won. Can be null
     */
    @Nullable
    public SkyWarsPlayer getWinner() {
        return winner;
    }

    /**
     * Sets the winner of this game. Can be null.
     *
     * @param winner the {@link SkyWarsPlayer player} who won. Can be null
     */
    public void setWinner(@Nullable SkyWarsPlayer winner) {
        this.winner = winner;
    }

    /**
     * Gets the spectators in this game. Can be empty.
     *
     * @return a {@link List} of {@link SkyWarsPlayer players} who are spectating
     */
    public Set<Player> getBukkitSpectators() {
        return spectators.stream().map(SkyWarsPlayer::getBukkitPlayer).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    /**
     * Logs a message to the console prefixed with the world name.
     *
     * @param level   the {@link Level} of the message
     * @param message the message to log
     */
    public void log(Level level, String message) {
        plugin.getLogger().log(level, "[" + world.getName() + "] " + message);
    }
}
