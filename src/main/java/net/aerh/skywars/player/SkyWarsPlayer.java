package net.aerh.skywars.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

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
