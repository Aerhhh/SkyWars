package net.aerh.skywars.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class GameListener implements Listener {

    private final Cache<UUID, UUID> lastDamager = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build();


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

        if (game == null || game.getState() == GameState.PRE_GAME || game.getState() == GameState.STARTING) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (player.getHealth() - event.getFinalDamage() > 0) {
            return;
        }

        event.setCancelled(true);
        player.sendTitle(ChatColor.RED + ChatColor.BOLD.toString() + "YOU DIED!", ChatColor.GRAY + "Better luck next time!", 0, 20 * 5, 20);
        game.setSpectator(player);

        // TODO fancy death messages
        UUID uuid = lastDamager.getIfPresent(player.getUniqueId());
        SkyWarsPlayer killer = game.getPlayer(uuid);

        if (killer != null && killer.getBukkitPlayer() != null) {
            game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " was killed by " + ChatColor.GOLD + killer.getBukkitPlayer().getName());
            killer.addKill();
        } else {
            game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " died!");
        }
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        lastDamager.put(player.getUniqueId(), damager.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTimeChange(TimeSkipEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isBlockBreak()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isBlockPlace()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isDamage()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
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

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isDropItem()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isPickupItem()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.findGame(player);

        if (game == null) {
            return;
        }

        String format = event.getFormat();
        Predicate<? super Player> filter = null;

        if (game.getPlayer(player) != null) {
            format = ChatColor.GOLD + "[PLAYER] " + "%s" + ChatColor.RESET + ": %s";
            filter = recipient -> !recipient.getWorld().equals(player.getWorld());
        } else if (game.getBukkitSpectators().contains(player)) {
            format = ChatColor.GRAY + "[SPECTATOR] " + "%s" + ChatColor.RESET + ": %s";
            filter = recipient -> !game.getBukkitSpectators().contains(recipient);
        }

        if (!game.getSettings().isChat()) {
            event.setCancelled(true);
        }

        event.setFormat(format);

        if (filter != null) {
            event.getRecipients().removeIf(filter);
        }
    }
}
