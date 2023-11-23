package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.GameSettings;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.island.Island;
import org.bukkit.ChatColor;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CageOpenEvent extends GameEvent {

    private final GameSettings settings = game.getSettings();

    public CageOpenEvent(SkyWarsGame game) {
        super(game, "Cages Open", 15L, TimeUnit.SECONDS);
    }

    @Override
    public void onSchedule() {
        game.teleportPlayers();
        game.removePregameSpawn(10);
    }

    @Override
    public void onTrigger() {
        game.log(Level.INFO, "Opening cages!");

        game.getIslands().forEach(Island::removeCage);

        game.broadcast(ChatColor.YELLOW + "Cages opened! " + ChatColor.RED + "FIGHT!");
        settings.setInteractable(true);
        settings.allowDamage(true);
        settings.setHunger(true);
        settings.allowItemDrops(true);
        settings.allowItemPickup(true);
        settings.allowBlockBreaking(true);
        settings.allowBlockPlacing(true);
    }

    @Override
    public void onTick() {
        // Not needed
    }
}
