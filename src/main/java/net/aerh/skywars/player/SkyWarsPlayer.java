package net.aerh.skywars.player;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SkyWarsPlayer {

    private final UUID uuid;
    private final String displayName;
    private PlayerScoreboard scoreboard;
    private int kills;

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
     * Gets the amount of kills the player has.
     *
     * @return the amount of kills the player has
     */
    public int getKills() {
        return kills;
    }

    /**
     * Sets the amount of kills the player has.
     *
     * @param kills the amount of kills the player should have
     */
    public void setKills(int kills) {
        this.kills = kills;

        if (scoreboard != null) {
            scoreboard.add(5, ChatColor.RESET + "Kills: " + ChatColor.GREEN + this.kills);
            scoreboard.update();
        }
    }

    /**
     * Adds a kill to the player.
     */
    public void addKill() {
        setKills(++kills);
    }

    /**
     * Removes a kill from the player.
     */
    public void removeKill() {
        setKills(--kills);
    }

    /**
     * Resets the amount of kills the player has.
     */
    public void resetKills() {
        setKills(0);
    }

    /**
     * Gets the {@link Player} object of the player. Can be null.
     *
     * @return the {@link Player} object of the player
     */
    @Nullable
    public Player getBukkitPlayer() {
        return Bukkit.getPlayer(uuid);
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

    @Override
    public String toString() {
        return "SkyWarsPlayer{" +
            "uuid=" + uuid +
            '}';
    }
}
