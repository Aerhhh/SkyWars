package net.aerh.skywars.player;

import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Optional;
import java.util.UUID;

public class SkyWarsPlayer {

    private final UUID uuid;
    private final String displayName;
    private PlayerScoreboard scoreboard;
    private boolean canSeeSpectators;

    /**
     * Represents a player in the game.
     *
     * @param uuid the {@link UUID} of the player
     */
    public SkyWarsPlayer(UUID uuid, String displayName) {
        this.uuid = uuid;
        this.displayName = displayName;
    }

    /**
     * Set up the scoreboard for a {@link Player}.
     */
    public void setupScoreboard(SkyWarsGame skyWarsGame) {
        scoreboard.add(10, " ");
        scoreboard.add(9, ChatColor.RESET + "Next Event:");
        scoreboard.add(8, ChatColor.GRAY + "???");
        scoreboard.add(7, "  ");
        scoreboard.add(6, ChatColor.RESET + "Players: " + ChatColor.GREEN + skyWarsGame.getPlayerManager().getOnlinePlayers().size());
        scoreboard.add(5, ChatColor.RESET + "Kills: " + ChatColor.GREEN + "0");
        scoreboard.add(4, "   ");
        scoreboard.add(3, ChatColor.RESET + "Map: " + ChatColor.GREEN + skyWarsGame.getMapName());
        scoreboard.add(2, "    ");
        scoreboard.add(1, ChatColor.YELLOW + "www.aerh.net");
        scoreboard.update();

        getBukkitPlayer().ifPresent(player -> {
            setupPlayerNameColors(skyWarsGame);
            scoreboard.send(player);
        });
    }

    /**
     * Sets up player name colors on the scoreboard. Players will see other players as red and themselves as green.
     */
    private void setupPlayerNameColors(SkyWarsGame skyWarsGame) {
        Team green;
        Team gray;
        Team red;

        Scoreboard bukkitScoreboard = scoreboard.getScoreboard();

        if (bukkitScoreboard.getTeam("green") == null) {
            green = bukkitScoreboard.registerNewTeam("green");
        } else {
            green = bukkitScoreboard.getTeam("green");
        }

        if (bukkitScoreboard.getTeam("gray") == null) {
            gray = bukkitScoreboard.registerNewTeam("gray");
        } else {
            gray = bukkitScoreboard.getTeam("gray");
        }

        if (bukkitScoreboard.getTeam("red") == null) {
            red = bukkitScoreboard.registerNewTeam("red");
        } else {
            red = bukkitScoreboard.getTeam("red");
        }

        green.setColor(ChatColor.GREEN);
        green.setAllowFriendlyFire(false);
        red.setColor(ChatColor.RED);
        gray.setColor(ChatColor.GRAY);

        getBukkitPlayer().ifPresent(player -> green.addEntry(player.getName()));

        skyWarsGame.getPlayerManager().getPlayers().stream()
            .filter(skyWarsPlayer -> !skyWarsPlayer.getUuid().equals(uuid))
            .forEach(skyWarsPlayer -> {
                skyWarsPlayer.getBukkitPlayer().ifPresent(player -> {
                    red.addEntry(player.getName());
                });
            });
    }

    /**
     * Gets the {@link UUID} of the player.
     *
     * @return the {@link UUID} of the player
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the display name of the player.
     *
     * @return the display name of the player
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the {@link Player} object of the player. Can be null.
     *
     * @return the {@link Player} object of the player
     */
    public Optional<Player> getBukkitPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(uuid));
    }

    /**
     * Gets the {@link Scoreboard} of the player. Can be null.
     *
     * @return the {@link Scoreboard} of the player
     */
    public PlayerScoreboard getScoreboard() {
        return scoreboard;
    }

    /**
     * Sets the {@link Scoreboard} of the player.
     *
     * @param scoreboard the {@link Scoreboard} to set
     */
    public void setScoreboard(PlayerScoreboard scoreboard) {
        this.scoreboard = scoreboard;
    }

    public boolean canSeeSpectators() {
        return canSeeSpectators;
    }

    public void setCanSeeSpectators(boolean seeSpectators) {
        this.canSeeSpectators = seeSpectators;
    }
}
