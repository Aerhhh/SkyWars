package net.aerh.skywars.listener;

import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.SkyWarsGame;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;

public class GameListener implements Listener {

    private final SkyWarsPlugin plugin;

    public GameListener(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null || event.getFinalDamage() < player.getHealth()) {
            return;
        }

        if (game.getSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.getHealth() - event.getFinalDamage() > 0) {
            return;
        }

        event.setCancelled(true);
        player.sendTitle(ChatColor.RED + "YOU DIED!", "", 0, 20 * 5, 0);
        game.setSpectator(player);
        game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " died!");
        // TODO fancy death messages
    }

    @EventHandler (ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true)
    public void onTimeChange(TimeSkipEvent event) {
        event.setCancelled(true);
    }

    @EventHandler (ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isBlockBreak()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isBlockPlace()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isHunger()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isDropItem()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isPickupItem()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (!game.getSettings().isInteract()) {
            event.setCancelled(true);
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        event.setFormat(ChatColor.GOLD + "[PLAYER] " + "%s" + ChatColor.RESET + ": %s");
        event.getRecipients().removeIf(recipient -> !recipient.getWorld().equals(player.getWorld()));

        if (game == null) {
            return;
        }

        if (!game.getSettings().isChat()) {
            event.setCancelled(true);
        }
    }
}
