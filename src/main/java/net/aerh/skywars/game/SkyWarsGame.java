package net.aerh.skywars.game;

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
import net.aerh.skywars.player.PlayerManager;
import net.aerh.skywars.player.SkyWarsPlayer;
import net.aerh.skywars.util.CenteredMessage;
import net.aerh.skywars.util.ItemBuilder;
import net.aerh.skywars.util.Utils;
import net.aerh.skywars.util.WorldSignParser;
import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SkyWarsGame {

    public static final int MIN_PLAYER_COUNT = 2;
    private final GameLoop gameLoop;
    private final GameSettings settings;
    private final PlayerManager playerManager;
    private final Queue<GameEvent> gameEvents;
    private final Set<RefillableChest> refillableChests;
    private final Map<String, Integer> kills;
    private final String mapName;
    private final List<Island> islands;
    private final World world;
    private Location pregameSpawn;
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

        this.playerManager = new PlayerManager(this);
        this.islands = new ArrayList<>();
        this.gameEvents = new LinkedList<>();
        this.refillableChests = new HashSet<>();
        this.kills = new HashMap<>();

        this.settings = new GameSettings();
        settings.setInteractable(false);

        addGameEvents(new CageOpenEvent(this), new ChestRefillEvent(this), new ChestRefillEvent(this), new DragonSpawnEvent(this), new GameEndEvent(this));
        this.gameLoop = new GameLoop(this);

        WorldSignParser signParser = new WorldSignParser(world, true);

        signParser.getParsedSigns("chest").forEach(sign -> {
            ChestType chestType = Utils.parseEnum(ChestType.class, sign.getOptions().get(0)).orElse(ChestType.ISLAND);
            RefillableChest refillableChest = new RefillableChest(sign.getLocation(), chestType);
            refillableChests.add(refillableChest);
            refillableChest.spawn(true, sign.getRotation());
            log(Level.INFO, "Registered refillable " + chestType + " chest at " + Utils.parseLocationToString(sign.getLocation()));
        });

        if (refillableChests.isEmpty()) {
            // We'll remove chest refill events if there are no chests instead of stopping the game
            gameEvents.removeIf(ChestRefillEvent.class::isInstance);
        }
    }

    /**
     * Starts the game.
     */
    public void start() {
        if (countdownTask != null) {
            if (!countdownTask.isCancelled()) {
                countdownTask.cancel();
            }
            countdownTask = null;
        }

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

        playerManager.getOnlinePlayers().stream()
            .filter(Objects::nonNull)
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .forEach(skyWarsPlayer -> {
                skyWarsPlayer.setupScoreboard(this);
                skyWarsPlayer.getBukkitPlayer().get().setGameMode(GameMode.SURVIVAL);
            });

        gameLoop.next();
    }

    /**
     * Ends the game.
     */
    public void end() {
        state = GameState.ENDING;

        gameLoop.cancelTasks();

        playerManager.getOnlinePlayers().forEach(skyWarsPlayer -> {
            skyWarsPlayer.getScoreboard().add(8, ChatColor.GREEN + "Game over!");
            skyWarsPlayer.getScoreboard().update();
        });

        if (playerManager.getAlivePlayers().size() == 1) {
            winner = playerManager.getAlivePlayers().iterator().next();
        }

        sendGameEndMessage();

        playerManager.getPlayers().stream()
            .filter(skyWarsPlayer -> getWinner().isEmpty() || !winner.getUuid().equals(skyWarsPlayer.getUuid()))
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .filter(skyWarsPlayer -> playerManager.getSpectator(skyWarsPlayer.getUuid()).isEmpty())
            .forEach(this::setSpectator);

        scheduleGameShutdown();
    }

    /**
     * Checks the player count to start the countdown.
     */
    public void checkPlayerCountForCountdown() {
        if (playerManager.getOnlinePlayers().size() >= MIN_PLAYER_COUNT && (countdownTask == null)) {
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
                if (playerManager.getOnlinePlayers().size() < MIN_PLAYER_COUNT) {
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
        playerManager.getSpectators().add(skyWarsPlayer);

        skyWarsPlayer.getBukkitPlayer().ifPresentOrElse(spectator -> {
            log(Level.INFO, "Setting " + spectator.getName() + " to spectator mode!");

            spectator.setGameMode(GameMode.ADVENTURE);
            spectator.setHealth(20.0D);
            spectator.setFoodLevel(20);
            spectator.setAllowFlight(true);
            spectator.setFlying(true);
            spectator.getInventory().clear();
            spectator.getInventory().setArmorContents(null);
            spectator.getScoreboard().getTeam("gray").addEntry(spectator.getName());
            spectator.teleport(pregameSpawn);
            spectator.getInventory().clear();

            ItemStack teleporterItem = new ItemBuilder(Material.COMPASS).setDisplayName(ChatColor.GREEN + "Teleporter").build();
            ItemStack settingsItem = new ItemBuilder(Material.COMPARATOR).setDisplayName(ChatColor.GREEN + "Spectator Settings").build();

            spectator.getInventory().setItem(0, teleporterItem);
            spectator.getInventory().setItem(4, settingsItem);

            playerManager.getOnlinePlayers().stream()
                .filter(p -> p.getBukkitPlayer().isPresent())
                .forEach(swPlayer -> {
                    if (swPlayer.canSeeSpectators()) {
                        swPlayer.getBukkitPlayer().get().showPlayer(SkyWarsPlugin.getInstance(), spectator);
                    } else {
                        swPlayer.getBukkitPlayer().get().hidePlayer(SkyWarsPlugin.getInstance(), spectator);
                    }

                    swPlayer.getScoreboard().getScoreboard().getTeam("gray").addEntry(spectator.getName());
                    swPlayer.getScoreboard().add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + playerManager.getAlivePlayers().size());
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
            playerManager.getSpectatorsBukkit().forEach(spectator -> spectator.kickPlayer(ChatColor.RED + "Game ended!"));

            if (winner != null && playerManager.getOnlinePlayers().contains(winner)) {
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
     * Teleports all players to their island spawn locations.
     */
    public void teleportPlayers() {
        playerManager.getPlayers().stream()
            .filter(skyWarsPlayer -> skyWarsPlayer.getBukkitPlayer().isPresent())
            .forEach(player -> {
                getIsland(player).ifPresentOrElse(i -> {
                    Location playerIslandSpawn = i.getSpawnLocation().clone().add(0.5, 0, 0.5);
                    double x = pregameSpawn.getX() - playerIslandSpawn.getX();
                    double z = pregameSpawn.getZ() - playerIslandSpawn.getZ();
                    double theta = Math.atan2(-x, z);
                    double yaw = Math.toDegrees(theta);
                    playerIslandSpawn.setYaw((float) yaw);

                    player.getBukkitPlayer().get().teleport(playerIslandSpawn);
                }, () -> setSpectator(player));
            });
    }

    /**
     * Adds a kill to the specified {@link SkyWarsPlayer}.
     *
     * @param skyWarsPlayer The {@link SkyWarsPlayer} to add the kill to
     */
    public void addKill(SkyWarsPlayer skyWarsPlayer) {
        kills.put(skyWarsPlayer.getDisplayName(), kills.getOrDefault(skyWarsPlayer.getDisplayName(), 0) + 1);

        skyWarsPlayer.getScoreboard().add(5, ChatColor.RESET + "Kills: " + ChatColor.GREEN + getKills(skyWarsPlayer));
        skyWarsPlayer.getScoreboard().update();
    }

    /**
     * Gets the amount of kills a player has
     *
     * @param player The player to get the kills of
     * @return The amount of kills the player has
     */
    public int getKills(SkyWarsPlayer player) {
        return kills.getOrDefault(player.getDisplayName(), 0);
    }

    /**
     * Gets the map of kills.
     *
     * @return The map of kills
     */
    public Map<String, Integer> getKills() {
        return kills;
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
     * Get the maximum amount of players this game can hold.
     *
     * @return The maximum amount of players this game can hold.
     */
    public int getMaxPlayers() {
        return islands.size();
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
     * Gets the {@link PlayerManager} of this game.
     *
     * @return the {@link PlayerManager} of this game.
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * Broadcasts a message to all players and spectators.
     *
     * @param message the message
     */
    public void broadcast(String message) {
        playerManager.getOnlinePlayers().stream()
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
        playerManager.getOnlinePlayers().stream()
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
     * Sets the pregame spawn {@link Location location}.
     *
     * @param pregameSpawn the pregame spawn location
     */
    public void setPregameSpawn(Location pregameSpawn) {
        this.pregameSpawn = pregameSpawn;
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
     * Gets the {@link Queue} of {@link GameEvent events} for this game.
     *
     * @return the {@link Queue} of {@link GameEvent events}
     */
    public Queue<GameEvent> getGameEvents() {
        return gameEvents;
    }

    /**
     * Adds {@link GameEvent events} to this game.
     *
     * @param gameEvents the {@link GameEvent events} to add
     */
    public void addGameEvents(GameEvent... gameEvents) {
        this.gameEvents.addAll(Arrays.asList(gameEvents));
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
     * Gets the name of the map.
     *
     * @return The name of the map.
     */
    public String getMapName() {
        return mapName;
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
}
