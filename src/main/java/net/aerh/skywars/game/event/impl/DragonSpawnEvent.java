package net.aerh.skywars.game.event.impl;

import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.game.event.GameEvent;
import org.bukkit.Sound;
import org.bukkit.entity.EnderDragon;

public class DragonSpawnEvent extends GameEvent {

    private static final int MAX_DRAGONS = 3;

    public DragonSpawnEvent(SkyWarsGame game) {
        super(game, 20L * 60L * 5L);
    }

    @Override
    public void execute() {
        for (int i = 0; i < MAX_DRAGONS; i++) {
            EnderDragon dragon = game.getWorld().spawn(game.getPregameSpawn(), EnderDragon.class);

            dragon.setAware(true);
            dragon.setPhase(EnderDragon.Phase.SEARCH_FOR_BREATH_ATTACK_TARGET);
            dragon.setRemoveWhenFarAway(false);
        }

        game.getBukkitPlayers().forEach(bukkitPlayer -> bukkitPlayer.playSound(bukkitPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.0F));
    }
}
