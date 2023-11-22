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
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    private final Location pregameSpawn;
    private final GameLoop gameLoop;
    private final GameSettings settings;
    private final Set<SkyWarsPlayer> players;
    private final Set<SkyWarsPlayer> spectators;
    private final Queue<GameEvent> gameEvents;
    private final Set<RefillableChest> refillableChests;
    private final Map<String, Integer> kills;
    private final String mapName;
    private final List<Island> islands;
    private final World world;
    private GameState state = GameState.PRE_GAME;
    private BukkitTask countdownTask;
    private SkyWarsPlayer winner;

    /**
     * Represents a game of SkyWars.
     *
     * @param world  the {@link World} of this game
     * @param config the {@link JsonObject} to parse things from
     */
    public SkyWarsGame(World world, JsonObject config) {
        this.world = world;
        this.mapName = config.get("name").getAsString();
        this.pregameSpawn = parseConfigLocationObject(config, this.world, "pregame");

        this.islands = new ArrayList<>();
        this.players = new HashSet<>();
        this.spectators = new HashSet<>();
        this.gameEvents = new LinkedList<>();
        this.refillableChests = new HashSet<>();
        this.kills = new HashMap<>();

        this.settings = new GameSettings();
        settings.setInteractable(false);

        addGameEvents(new CageOpenEvent(this), new ChestRefillEvent(this), new ChestRefillEvent(this), new DragonSpawnEvent(this), new GameEndEvent(this));
        this.gameLoop = new GameLoop(this);

        try {
            parseIslands(config);
        } catch (IllegalArgumentException exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to parse islands!", exception);
            Bukkit.getServer().shutdown();
        }

        try {
            parseChests(config);
        } catch (IllegalArgumentException exception) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to parse chests!", exception);
        }

        // I made this because the map I'm using to test already has chest signs so it'll be quicker to just use them
        WorldSignParser signParser = new WorldSignParser(world, true);

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
                skyWarsPlayer.getBukkitPlayer().ifPresent(player -> {
                    setupScoreboard(player, skyWarsPlayer.getScoreboard());
                    player.setGameMode(GameMode.SURVIVAL);
                });
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

        sendGameEndMessage();

        players.stream()
            .filter(skyWarsPlayer -> getWinner().isEmpty() || !winner.getUuid().equals(skyWarsPlayer.getUuid()))
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .filter(skyWarsPlayer -> getSpectator(skyWarsPlayer.getUuid()).isEmpty())
            .forEach(this::setSpectator);

        scheduleGameShutdown();
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
    public void checkPlayerCountForCountdown() {
        if (getOnlinePlayers().size() >= MIN_PLAYER_COUNT && (countdownTask == null)) {
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
                if (getOnlinePlayers().size() < MIN_PLAYER_COUNT) {
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
        }.runTaskTimer(SkyWarsPlugin.getInstance(), 10L, Utils.TICKS_PER_SECOND);
    }

    /**
     * Sets a {@link SkyWarsPlayer} to a spectator.
     *
     * @param skyWarsPlayer the {@link SkyWarsPlayer} to set
     */
    public void setSpectator(SkyWarsPlayer skyWarsPlayer) {
        spectators.add(skyWarsPlayer);

        skyWarsPlayer.getBukkitPlayer().ifPresentOrElse(spectator -> {
            log(Level.INFO, "Setting " + spectator.getName() + " to spectator mode!");

            spectator.setHealth(20.0D);
            spectator.setFoodLevel(20);
            spectator.setAllowFlight(true);
            spectator.setFlying(true);
            spectator.getInventory().clear();
            spectator.getInventory().setArmorContents(null);
            spectator.getScoreboard().getTeam("gray").addEntry(spectator.getName());
            spectator.teleport(pregameSpawn);
            spectator.getInventory().clear();

            ItemStack compass = new ItemStack(Material.COMPASS);
            ItemMeta compassMeta = compass.getItemMeta();
            compassMeta.setDisplayName(ChatColor.GREEN + "Teleporter");
            compass.setItemMeta(compassMeta);

            ItemStack settings = new ItemStack(Material.COMPARATOR);
            ItemMeta settingsMeta = settings.getItemMeta();
            settingsMeta.setDisplayName(ChatColor.GREEN + "Spectator Settings");
            settings.setItemMeta(settingsMeta);

            spectator.getInventory().setItem(0, compass);
            spectator.getInventory().setItem(4, settings);

            getOnlinePlayers().stream()
                .filter(p -> p.getBukkitPlayer().isPresent())
                .forEach(swPlayer -> {
                    if (swPlayer.canSeeSpectators()) {
                        swPlayer.getBukkitPlayer().get().showPlayer(SkyWarsPlugin.getInstance(), spectator);
                    } else {
                        swPlayer.getBukkitPlayer().get().hidePlayer(SkyWarsPlugin.getInstance(), spectator);
                    }

                    swPlayer.getScoreboard().getScoreboard().getTeam("gray").addEntry(spectator.getName());
                    swPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + getAlivePlayers().size());
                    swPlayer.getScoreboard().update();
                });
        }, () -> log(Level.SEVERE, "Failed to set " + skyWarsPlayer.getUuid() + " to spectator mode as they could not be found!"));
    }

    /**
     * Sends the giant GAME OVER message when the game ends.
     */
    private void sendGameEndMessage() {
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
            getTopPlayers().forEach((string, integer) -> {
                broadcast(CenteredMessage.generate(ChatColor.GOLD + string + ChatColor.RESET + ": "
                    + ChatColor.YELLOW + integer + " kill" + (integer == 1 ? "" : "s")));
            });
        } else {
            broadcast(CenteredMessage.generate(ChatColor.RED + "Nobody!"));
        }

        broadcast("\n");
        broadcast(Utils.SEPARATOR);
    }

    /**
     * Schedules the game being closed and cleared.
     */
    private void scheduleGameShutdown() {
        SkyWarsPlugin.getInstance().getServer().getScheduler().runTaskLater(SkyWarsPlugin.getInstance(), () -> {
            getBukkitSpectators().forEach(spectator -> spectator.kickPlayer(ChatColor.RED + "Game ended!"));

            if (winner != null && getOnlinePlayers().contains(winner)) {
                winner.getBukkitPlayer().ifPresent(player -> player.kickPlayer(ChatColor.GREEN + "You won!"));
            }

            if (SkyWarsPlugin.getInstance().getServer().unloadWorld(world, false)) {
                log(Level.INFO, "Unloaded world " + world.getName() + " successfully!");
                Utils.deleteFolder(world.getWorldFolder().toPath());
            } else {
                throw new IllegalStateException("Failed to unload world " + world.getName() + "!");
            }

            SkyWarsPlugin.getInstance().getGameManager().removeGame(this);
        }, Utils.TICKS_PER_SECOND * 15L);
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
        SkyWarsPlugin.getInstance().getServer().getScheduler().runTask(SkyWarsPlugin.getInstance(), () -> island.get().spawnCage());

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
        players.remove(player);
        spectators.remove(player);

        /*if (state == GameState.PRE_GAME) {
            checkPlayerCountForCountdown();
        }*/

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
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .forEach(player -> {
                getIsland(player).ifPresentOrElse(i -> player.getBukkitPlayer().get().teleport(i.getSpawnLocation().clone().add(0.5, 0, 0.5)),
                    () -> setSpectator(player));
            });
    }

    public void addKill(String username) {
        kills.put(username, kills.getOrDefault(username, 0) + 1);

        getPlayer(username).ifPresent(skyWarsPlayer -> {
            skyWarsPlayer.getScoreboard().add(5, ChatColor.RESET + "Kills: " + ChatColor.GREEN + getKills(getPlayer(username).get()));
            skyWarsPlayer.getScoreboard().update();
        });
    }

    public int getKills(SkyWarsPlayer player) {
        return kills.getOrDefault(player.getDisplayName(), 0);
    }

    public Map<String, Integer> getKills() {
        return kills;
    }

    /**
     * Gets a {@link SkyWarsPlayer} by their {@link Player} object.
     *
     * @param player the {@link Player} to get
     * @return the {@link SkyWarsPlayer} with the {@link Player}. Can be null
     */
    public Optional<SkyWarsPlayer> getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    public Optional<SkyWarsPlayer> getSpectator(Player player) {
        return getSpectator(player.getUniqueId());
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
     * Gets a {@link SkyWarsPlayer} by their display name.
     *
     * @param username the display name to get
     * @return the {@link SkyWarsPlayer} with the display name. Can be null
     */
    public Optional<SkyWarsPlayer> getPlayer(String username) {
        return players.stream()
            .filter(p -> p.getDisplayName().equals(username))
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
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .collect(Collectors.toSet());
    }

    /**
     * Gets a {@link List} of all alive {@link SkyWarsPlayer players} in this game.
     *
     * @return the {@link List} of the alive {@link SkyWarsPlayer players} in this game
     */
    public List<SkyWarsPlayer> getAlivePlayers() {
        return players.stream()
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent() && !spectators.contains(skyWarsPlayer))
            .toList();
    }

    /**
     * Gets a {@link List} of the {@link Player players} in this game.
     *
     * @return the {@link List} of the {@link Player players} in this game
     */
    public List<Player> getBukkitPlayers() {
        return players.stream()
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();
    }

    /**
     * Gets a {@link List} of the top 3 players by display name in this game.
     *
     * @return the {@link List} of the top 3 players by display name in this game
     */
    private Map<String, Integer> getTopPlayers() {
        return kills.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(3)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                (oldValue, newValue) -> oldValue, LinkedHashMap::new)
            );
    }

    /**
     * Returns a list of spectators in the game.
     *
     * @return A list of spectators.
     */
    public Set<SkyWarsPlayer> getSpectators() {
        return spectators;
    }

    /**
     * Parses the islands from a {@link JsonArray}.
     *
     * @param config the {@link JsonObject} to parse from
     */
    private void parseIslands(JsonObject config) {
        parseConfigLocationArray(config, "islands").forEach(island -> {
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

        parseConfigLocationArray(config, "chests").forEach(chest -> {
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
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(player -> player.sendMessage(message));
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
            .map(SkyWarsPlayer::getBukkitPlayer)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(player -> player.sendTitle(title, subtitle, fadeIn, stay, fadeOut));
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
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    /**
     * Gets the {@link Set} of {@link SkyWarsPlayer players} in this game.
     *
     * @return the {@link Set} of {@link SkyWarsPlayer players}
     */
    public Set<SkyWarsPlayer> getPlayers() {
        return players;
    }

    /**
     * Logs a message to the console prefixed with the world name.
     *
     * @param level   the {@link Level} of the message
     * @param message the message to log
     */
    public void log(Level level, String message) {
        SkyWarsPlugin.getInstance().getLogger().log(level, "[" + world.getName() + "] " + message);
    }

    private void addGameEvents(GameEvent... events) {
        gameEvents.addAll(Arrays.asList(events));
    }

    /**
     * Parses a {@link Location} from a {@link JsonObject}.
     *
     * @param config the {@link JsonObject} to parse from
     * @param field  the field to parse
     * @return the parsed {@link Location}
     */
    private Location parseConfigLocationObject(JsonObject config, World world, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");
        JsonObject desiredLocation = locationsObject.getAsJsonObject(field);

        if (!desiredLocation.has("x") || !desiredLocation.has("y") || !desiredLocation.has("z")) {
            throw new IllegalStateException("Location is missing coordinates! " + desiredLocation);
        }

        Location location = new Location(world, desiredLocation.get("x").getAsDouble(), desiredLocation.get("y").getAsDouble(), desiredLocation.get("z").getAsDouble());

        if (desiredLocation.has("yaw")) {
            location.setYaw(desiredLocation.get("yaw").getAsFloat());
        }

        if (desiredLocation.has("pitch")) {
            location.setPitch(desiredLocation.get("pitch").getAsFloat());
        }

        return location;
    }

    private List<JsonObject> parseConfigLocationArray(JsonObject config, String field) {
        JsonObject locationsObject = config.getAsJsonObject("locations");
        List<JsonObject> locations = new ArrayList<>();

        for (JsonElement locationElement : locationsObject.getAsJsonArray(field)) {
            JsonObject locationObject = locationElement.getAsJsonObject();

            if (!locationObject.has("x") || !locationObject.has("y") || !locationObject.has("z")) {
                throw new IllegalStateException("Location is missing coordinates! " + locationObject);
            }

            locations.add(locationObject);
        }

        return locations;
    }
}
