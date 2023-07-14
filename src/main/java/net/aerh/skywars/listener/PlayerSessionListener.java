package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerSessionListener implements Listener {

    private final SkyWarsPlugin plugin;

    public PlayerSessionListener(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        SkyWarsGame game = plugin.getGames().values().iterator().next();
        game.addPlayer(player);
    }

}
