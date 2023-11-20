package net.aerh.skywars.game;

import com.google.gson.JsonArray;
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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SkyWarsGame {

    public static final int MIN_PLAYER_COUNT = 2;
    private static final BlockFace[] VALID_CHEST_ROTATIONS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    private final SkyWarsPlugin plugin;
    private final World world;
    private final Location pregameSpawn;
    private final GameLoop gameLoop;
    private final GameSettings settings = new GameSettings();
    private final Set<SkyWarsPlayer> players = new HashSet<>();
    private final Set<SkyWarsPlayer> spectators = new HashSet<>();
    private final Queue<GameEvent> gameEvents = new LinkedList<>();
    private final Set<RefillableChest> refillableChests = new HashSet<>();
    private final String mapName;
    private final List<Island> islands = new ArrayList<>();
    private GameState state = GameState.PRE_GAME;
    private BukkitTask countdownTask;
    private SkyWarsPlayer winner;

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
        this.mapName = config.get("name").getAsString();
        this.pregameSpawn = Utils.parseConfigLocationObject(config, this.world, "pregame");

        settings.setInteractable(false);

        addGameEvents(new CageOpenEvent(this), new ChestRefillEvent(this), new ChestRefillEvent(this), new DragonSpawnEvent(this), new GameEndEvent(this));
        this.gameLoop = new GameLoop(this);

        try {
            parseIslands(config);
        } catch (IllegalArgumentException exception) {
            log(Level.SEVERE, "Failed to parse islands!");
            exception.printStackTrace();
            Bukkit.getServer().shutdown();
        }

        try {
            parseChests(config);
        } catch (IllegalArgumentException exception) {
            log(Level.SEVERE, "Failed to parse chests!");
            exception.printStackTrace();
        }

        // I made this because the map I'm using to test already has chest signs so it'll be quicker to just use them
        WorldSignParser signParser = new WorldSignParser(plugin, world, true);

        signParser.getParsedSigns("chest").forEach(sign -> {
            ChestType chestType = Utils.parseEnum(ChestType.class, sign.getOptions().get(0)).orElse(ChestType.ISLAND);
            RefillableChest refillableChest = new RefillableChest(sign.getLocation(), chestType);
            refillableChests.add(refillableChest);
            refillableChest.spawn(true, sign.getRotation());
            log(Level.INFO, "Registered refillable " + chestType + " chest at " + Utils.parseLocationToString(sign.getLocation()));
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

        getOnlinePlayers().stream()
            .filter(Objects::nonNull)
            .forEach(skyWarsPlayer -> {
                Player player = skyWarsPlayer.getBukkitPlayer();

                if (player == null) {
                    log(Level.SEVERE, "Failed to set scoreboard for " + skyWarsPlayer.getUuid() + "!");
                    return;
                }

                setupScoreboard(player, skyWarsPlayer.getScoreboard());
                player.setGameMode(GameMode.SURVIVAL);
            });

        gameLoop.next();
    }

    /**
     * Ends the game.
     */
    public void end() {
        state = GameState.ENDING;

        gameLoop.cancelTasks();

        getOnlinePlayers().forEach(skyWarsPlayer -> {
            skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + "Game over!");
            skyWarsPlayer.getScoreboard().update();
        });

        if (getAlivePlayers().size() == 1) {
            winner = getAlivePlayers().iterator().next();
        }

        broadcast(Utils.SEPARATOR);
        broadcast(CenteredMessage.generate(ChatColor.RESET + ChatColor.BOLD.toString() + "SkyWars"));
        broadcast(CenteredMessage.generate(ChatColor.RED + "GAME OVER!"));
        broadcast("\n");

        if (winner != null) {
            broadcast(CenteredMessage.generate(ChatColor.RESET + ChatColor.BOLD.toString() + "Winner: " + ChatColor.GOLD + winner.getDisplayName()));
        } else {
            broadcast(CenteredMessage.generate(ChatColor.RED + "Nobody won the game!"));
        }

        broadcast("\n");
        broadcast(CenteredMessage.generate(ChatColor.RESET + ChatColor.BOLD.toString() + "Top Players"));

        if (!getTopPlayers().isEmpty()) {
            getTopPlayers().forEach(skyWarsPlayer -> {
                broadcast(CenteredMessage.generate(ChatColor.GOLD + skyWarsPlayer.getDisplayName() + ChatColor.RESET + ": "
                    + ChatColor.YELLOW + skyWarsPlayer.getKills() + " kill" + (skyWarsPlayer.getKills() == 1 ? "" : "s")));
            });
        } else {
            broadcast(CenteredMessage.generate(ChatColor.RED + "Nobody!"));
        }

        broadcast("\n");
        broadcast(Utils.SEPARATOR);

        players.stream()
            .filter(skyWarsPlayer -> getWinner().isEmpty() || !winner.getUuid().equals(skyWarsPlayer.getUuid()))
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer() != null)
            .forEach(this::setSpectator);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            getBukkitSpectators().forEach(spectator -> spectator.kickPlayer(ChatColor.RED + "Game ended!"));

            if (winner != null && winner.getBukkitPlayer() != null) {
                winner.getBukkitPlayer().kickPlayer(ChatColor.GREEN + "You won!");
            }

            spectators.clear();
            players.clear();
            islands.clear();
            refillableChests.clear();
            gameEvents.clear();
            plugin.getGameManager().removeGame(this);
        }, Utils.TICKS_PER_SECOND * 30L);
    }

    /**
     * Set up the scoreboard for a {@link Player}.
     *
     * @param player     the {@link Player} to set up the scoreboard for
     * @param scoreboard the {@link PlayerScoreboard} to set up
     */
    private void setupScoreboard(Player player, PlayerScoreboard scoreboard) {
        scoreboard.add(10, " ");
        scoreboard.add(9, ChatColor.RESET + "Next Event:");
        scoreboard.add(8, ChatColor.GRAY + "???");
        scoreboard.add(7, "  ");
        scoreboard.add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getOnlinePlayers().size());
        scoreboard.add(5, ChatColor.RESET + "Kills: " + ChatColor.GREEN + "0");
        scoreboard.add(4, "   ");
        scoreboard.add(3, ChatColor.RESET + "Map: " + ChatColor.GREEN + mapName);
        scoreboard.add(2, "    ");
        scoreboard.add(1, ChatColor.YELLOW + "www.aerh.net");
        scoreboard.update();
        scoreboard.send(player);

        setupPlayerNameColors(player, scoreboard.getScoreboard());
    }

    /**
     * Sets up player name colors on the scoreboard. Players will see other players as red and themselves as green.
     *
     * @param player the {@link Player} to set up the colors for
     */
    private void setupPlayerNameColors(Player player, Scoreboard scoreboard) {
        Team green;
        Team gray;
        Team red;

        if (scoreboard.getTeam("green") == null) {
            green = scoreboard.registerNewTeam("green");
        } else {
            green = scoreboard.getTeam("green");
        }

        if (scoreboard.getTeam("gray") == null) {
            gray = scoreboard.registerNewTeam("gray");
        } else {
            gray = scoreboard.getTeam("gray");
        }

        if (scoreboard.getTeam("red") == null) {
            red = scoreboard.registerNewTeam("red");
        } else {
            red = scoreboard.getTeam("red");
        }

        green.setColor(ChatColor.GREEN);
        green.setAllowFriendlyFire(false);
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
        }.runTaskTimer(plugin, 10L, Utils.TICKS_PER_SECOND);
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
            log(Level.SEVERE, "Failed to set " + skyWarsPlayer.getUuid() + " to spectator mode as they could not be found!");
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

        getOnlinePlayers().forEach(swPlayer -> {
            Player bukkitPlayer = swPlayer.getBukkitPlayer();

            if (bukkitPlayer != null) {
                bukkitPlayer.hidePlayer(plugin, player);
            }

            swPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getAlivePlayers().size());
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

        Optional<Island> island = islands.stream()
            .filter(i -> i.getAssignedPlayer() == null)
            .findFirst();

        if (island.isEmpty()) {
            return false;
        }

        if (state == GameState.IN_GAME || state == GameState.ENDING) {
            log(Level.INFO, "Player " + player.getUuid() + " tried to join but the game is already running!");
            return false;
        }

        players.add(player);
        island.get().assignPlayer(player);
        Bukkit.getScheduler().runTask(plugin, () -> island.get().spawnCage());

        checkPlayerCountForCountdown();
        log(Level.INFO, "Added player " + player.getUuid() + " to island " + Utils.parseLocationToString(island.get().getSpawnLocation()) + "!");

        if (state != GameState.STARTING) {
            getOnlinePlayers().stream()
                .filter(skyWarsPlayer -> skyWarsPlayer.getScoreboard() != null)
                .forEach(skyWarsPlayer -> {
                    skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getAlivePlayers().size());
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
        spectators.remove(player);

        if (state == GameState.PRE_GAME) {
            checkPlayerCountForCountdown();
        }

        getIsland(player).ifPresent(Island::removePlayer);

        if (state != GameState.STARTING) {
            getOnlinePlayers().forEach(skyWarsPlayer -> {
                skyWarsPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getAlivePlayers().size());
                skyWarsPlayer.getScoreboard().update();
            });
        }
    }

    /**
     * Teleports all players to their island spawn locations.
     */
    public void teleportPlayers() {
        players.stream()
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer() != null)
            .forEach(player -> {
                getIsland(player).ifPresentOrElse(i -> player.getBukkitPlayer().teleport(i.getSpawnLocation().clone().add(0.5, 0, 0.5)),
                    () -> setSpectator(player));
            });
    }

    /**
     * Gets a {@link SkyWarsPlayer} by their {@link Player} object.
     *
     * @param player the {@link Player} to get
     * @return the {@link SkyWarsPlayer} with the {@link Player}. Can be null
     */
    public Optional<SkyWarsPlayer> getPlayer(Player player) {
        return players.stream()
            .filter(p -> p.getUuid().equals(player.getUniqueId()))
            .findFirst();
    }

    /**
     * Gets a {@link SkyWarsPlayer} by their {@link UUID}.
     *
     * @param uuid the {@link UUID} to get
     * @return the {@link SkyWarsPlayer} with the {@link UUID}. Can be null
     */
    public Optional<SkyWarsPlayer> getPlayer(UUID uuid) {
        return players.stream()
            .filter(p -> p.getUuid().equals(uuid))
            .findFirst();
    }

    /**
     * Gets a {@link SkyWarsPlayer} who is a spectator.
     *
     * @param uuid the {@link UUID} of the {@link SkyWarsPlayer} to get
     * @return the {@link SkyWarsPlayer} who is a spectator. Returns null if the {@link SkyWarsPlayer} is not a spectator
     */
    public Optional<SkyWarsPlayer> getSpectator(UUID uuid) {
        return spectators.stream()
            .filter(p -> p.getUuid().equals(uuid))
            .findFirst();
    }

    public Set<SkyWarsPlayer> getOnlinePlayers() {
        return Stream.concat(players.stream(), spectators.stream())
            .collect(Collectors.toSet());
    }

    /**
     * Gets a {@link List} of all alive {@link SkyWarsPlayer players} in this game.
     *
     * @return the {@link List} of the alive {@link SkyWarsPlayer players} in this game
     */
    public Set<SkyWarsPlayer> getAlivePlayers() {
        return players.stream()
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer() != null && !spectators.contains(skyWarsPlayer))
            .collect(Collectors.toSet());
    }

    /**
     * Gets a {@link List} of the {@link Player players} in this game.
     *
     * @return the {@link List} of the {@link Player players} in this game
     */
    public List<Player> getBukkitPlayers() {
        return players.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Gets a {@link List} of the top 3 {@link SkyWarsPlayer players} based on kills.
     *
     * @return the {@link List} of the top 3 {@link SkyWarsPlayer players} based on kills
     */
    private List<SkyWarsPlayer> getTopPlayers() {
        return players.stream()
            .sorted(Comparator.comparingInt(SkyWarsPlayer::getKills).reversed())
            .limit(3)
            .toList();
    }

    /**
     * Parses the islands from a {@link JsonArray}.
     *
     * @param config the {@link JsonObject} to parse from
     */
    private void parseIslands(JsonObject config) {
        Utils.parseConfigLocationArray(config, "islands").forEach(island -> {
            try {
                double x = island.get("x").getAsDouble();
                double y = island.get("y").getAsDouble();
                double z = island.get("z").getAsDouble();
                Location location = new Location(world, x, y, z);
                islands.add(new Island(location));
                log(Level.INFO, "Registered island: " + island);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Failed to parse island: " + island);
            }
        });
    }

    /**
     * Parses the chests from a {@link JsonObject}.
     *
     * @param config the {@link JsonObject} to parse from
     */
    private void parseChests(JsonObject config) {
        log(Level.INFO, "Parsing chest locations from map config...");

        Utils.parseConfigLocationArray(config, "chests").forEach(chest -> {
            try {
                double x = chest.get("x").getAsDouble();
                double y = chest.get("y").getAsDouble();
                double z = chest.get("z").getAsDouble();
                Optional<BlockFace> rotation = Utils.parseEnum(BlockFace.class, chest.get("rotation").getAsString());
                Optional<ChestType> chestType = Utils.parseEnum(ChestType.class, chest.get("type").getAsString());

                if (rotation.isEmpty() || chestType.isEmpty()) {
                    return;
                }

                if (Arrays.stream(VALID_CHEST_ROTATIONS).noneMatch(face -> face.equals(rotation.get()))) {
                    throw new IllegalArgumentException("Invalid chest rotation: " + rotation.get() + " for chest: " + chest);
                }

                RefillableChest refillableChest = new RefillableChest(new Location(world, x, y, z), chestType.get());
                refillableChests.add(refillableChest);
                refillableChest.spawn(true, rotation.get());
                log(Level.INFO, "Registered refillable chest: " + chest);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Failed to parse chest: " + chest);
            }
        });
    }

    /**
     * Gets the {@link Island} of a {@link SkyWarsPlayer}. Can be null.
     *
     * @param skyWarsPlayer the {@link SkyWarsPlayer} to get the {@link Island} of
     * @return the {@link Island} of the {@link SkyWarsPlayer}. Can be null
     */
    public Optional<Island> getIsland(SkyWarsPlayer skyWarsPlayer) {
        return islands.stream()
            .filter(island -> island.getAssignedPlayer() != null && island.getAssignedPlayer().equals(skyWarsPlayer))
            .findFirst();
    }

    /**
     * Broadcasts a message to all players and spectators.
     *
     * @param message the message
     */
    public void broadcast(String message) {
        getOnlinePlayers().stream()
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
        getOnlinePlayers().stream()
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

    public int getMaxPlayers() {
        return islands.size();
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
     * Removes a {@link RefillableChest} from this game.
     *
     * @param location the {@link Location} of the {@link RefillableChest}
     */
    public void removeRefillableChest(Location location) {
        refillableChests.stream()
            .filter(chest -> Utils.locationsMatch(chest.getLocation(), location))
            .findFirst().ifPresent(refillableChest -> {
                refillableChest.removeTimerHologram();
                refillableChests.remove(refillableChest);
            });
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
    public Optional<SkyWarsPlayer> getWinner() {
        return Optional.ofNullable(winner);
    }

    /**
     * Gets the spectators in this game. Can be empty.
     *
     * @return a {@link List} of {@link SkyWarsPlayer players} who are spectating
     */
    public Set<Player> getBukkitSpectators() {
        return spectators.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
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

    private void addGameEvents(GameEvent... events) {
        gameEvents.addAll(Arrays.asList(events));
    }
}
