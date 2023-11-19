package net.aerh.skywars.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.GameState;
import net.aerh.skywars.player.SkyWarsPlayer;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
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
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getState() == GameState.PRE_GAME || skyWarsGame.getState() == GameState.STARTING) {
                return;
            }

            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (player.getHealth() - event.getFinalDamage() > 0) {
                return;
            }

            event.setCancelled(true);
            player.sendTitle(ChatColor.RED + ChatColor.BOLD.toString() + "YOU DIED!", ChatColor.GRAY + "Better luck next time!", 0, 20 * 5, 20);

            SkyWarsPlayer skyWarsPlayer = skyWarsGame.getPlayer(player);
            skyWarsGame.setSpectator(skyWarsPlayer);

            UUID uuid = lastDamager.getIfPresent(player.getUniqueId());
            SkyWarsPlayer killer = skyWarsGame.getPlayer(uuid);

            if (killer != null) {
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " was killed by " + ChatColor.GOLD + killer.getDisplayName());
                killer.addKill();
            } else {
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " died!");
            }
        });
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player damager;

        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            damager = player;
        } else if (event.getDamager() instanceof Player player) {
            damager = player;
        } else {
            return;
        }

        Player player = (Player) event.getEntity();

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(damager)) {
                event.setCancelled(true);
                return;
            }

            lastDamager.put(player.getUniqueId(), damager.getUniqueId());
        });
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

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().canBreakBlocks()) {
                event.setCancelled(true);
            }

            skyWarsGame.removeRefillableChest(event.getBlock().getLocation());
        });
    }

    @EventHandler
    public void onBlockExplode(EntityExplodeEvent event) {

        plugin.getGameManager().findGame(event.getEntity().getWorld()).ifPresent(skyWarsGame -> {
            event.blockList().stream()
                .map(Block::getLocation)
                .filter(location -> skyWarsGame.getRefillableChests().stream().anyMatch(refillableChest -> refillableChest.getLocation().equals(location)))
                .forEach(skyWarsGame::removeRefillableChest);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().canPlaceBlocks()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof EnderDragon enderDragon && event.getTarget() instanceof Player target) {
            plugin.getGameManager().findGame(target).ifPresent(skyWarsGame -> {
                if (skyWarsGame.getBukkitSpectators().contains(target)) {
                    event.setCancelled(true);
                    enderDragon.setTarget(null);
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().isDamageEnabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().isHungerEnabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().canDropItems()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().canPickupItems()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().isInteractingEnabled()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onVehicleExit(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) {
            return;
        }

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getBukkitSpectators().contains(player)) {
                event.setCancelled(true);
            }
        });

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        plugin.getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            String format = event.getFormat();
            Predicate<? super Player> filter = null;

            if (skyWarsGame.getPlayer(player) != null) {
                format = ChatColor.GOLD + "[PLAYER] " + "%s" + ChatColor.RESET + ": %s";
                filter = recipient -> !recipient.getWorld().equals(player.getWorld());
            } else if (skyWarsGame.getBukkitSpectators().contains(player)) {
                format = ChatColor.GRAY + "[SPECTATOR] " + "%s" + ChatColor.RESET + ": %s";
                filter = recipient -> !skyWarsGame.getBukkitSpectators().contains(recipient);
            }

            if (!skyWarsGame.getSettings().isChatEnabled()) {
                event.setCancelled(true);
            }

            event.setFormat(format);

            if (filter != null) {
                event.getRecipients().removeIf(filter);
            }
        });
    }
}
