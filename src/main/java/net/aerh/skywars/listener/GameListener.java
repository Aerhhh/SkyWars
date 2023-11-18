package net.aerh.skywars.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.game.SkyWarsGame;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class GameListener implements Listener {

    private final Cache<UUID, UUID> lastDamager = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .scheduler(Scheduler.systemScheduler())
        .build();

    private final SkyWarsPlugin plugin;

    public GameListener(SkyWarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.getGameManager().findGame(player);

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

        SkyWarsPlayer skyWarsPlayer = game.getPlayer(player);
        game.setSpectator(skyWarsPlayer);

        UUID uuid = lastDamager.getIfPresent(player.getUniqueId());
        SkyWarsPlayer killer = game.getPlayer(uuid);

        if (killer != null) {
            game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " was killed by " + ChatColor.GOLD + killer.getDisplayName());
            killer.addKill();
        } else {
            game.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " died!");
        }
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player damager;

        if (event.getDamager() instanceof Projectile projectile) {
            if (!(projectile.getShooter() instanceof Player)) {
                return;
            }

            damager = (Player) projectile.getShooter();
        } else if (event.getDamager() instanceof Player player) {
            damager = player;
        } else {
            return;
        }

        Player player = (Player) event.getEntity();
        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(damager)) {
            event.setCancelled(true);
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
        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().canBreakBlocks()) {
            event.setCancelled(true);
        }

        Location location = event.getBlock().getLocation();
        game.removeRefillableChest(location);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().canPlaceBlocks()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof EnderDragon enderDragon && event.getTarget() instanceof Player target) {
            SkyWarsGame game = plugin.getGameManager().findGame(target);

            if (game == null) {
                return;
            }

            if (game.getBukkitSpectators().contains(target)) {
                event.setCancelled(true);
                enderDragon.setTarget(null);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isDamageEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isHungerEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().canDropItems()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().canPickupItems()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
            return;
        }

        if (!game.getSettings().isInteractingEnabled()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }

        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) {
            return;
        }

        SkyWarsGame game = plugin.getGameManager().findGame(player);

        if (game == null) {
            return;
        }

        if (game.getBukkitSpectators().contains(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        SkyWarsGame game = plugin.getGameManager().findGame(player);

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

        if (!game.getSettings().isChatEnabled()) {
            event.setCancelled(true);
        }

        event.setFormat(format);

        if (filter != null) {
            event.getRecipients().removeIf(filter);
        }
    }
}
