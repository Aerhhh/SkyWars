package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import net.aerh.skywars.game.island.Island;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class DragonSpawnEvent extends GameEvent {

    private static final int MAX_DRAGONS = 3;

    public DragonSpawnEvent(SkyWarsGame game) {
        super(game, "Doom", 5L, TimeUnit.MINUTES);
    }

    @Override
    public void onStart() {
        for (int i = 0; i < MAX_DRAGONS; i++) {
            Island randomIsland = game.getIslands().get(ThreadLocalRandom.current().nextInt(game.getIslands().size()));
            EnderDragon dragon = game.getWorld().spawn(randomIsland.getSpawnLocation().add(0, 10, 0), EnderDragon.class);

            dragon.setAware(true);
            dragon.setPhase(EnderDragon.Phase.SEARCH_FOR_BREATH_ATTACK_TARGET);
            dragon.setRemoveWhenFarAway(false);
        }

        game.getBukkitPlayers().forEach(bukkitPlayer -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F));
    }

    @Override
    public void onEnd() {
        // Not needed
    }

    @Override
    public void tick() {
        // Not needed
    }
}
