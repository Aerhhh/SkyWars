package net.aerh.skywars.listener;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import net.aerh.skywars.SkyWarsPlugin;
import net.aerh.skywars.game.state.GameState;
import net.aerh.skywars.menu.PlayerTrackerMenu;
import net.aerh.skywars.menu.SpectatorSettingsMenu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class GameListener implements Listener {

    public static final String FALL_DAMAGE_IMMUNITY_METADATA_KEY = "has_taken_fall_damage";

    private final Cache<String, String> lastDamager = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .scheduler(Scheduler.systemScheduler())
        .build();

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getState() == GameState.PRE_GAME || skyWarsGame.getState() == GameState.STARTING) {
                return;
            }

            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
                event.setCancelled(true);
                return;
            }

            if (!player.hasMetadata(FALL_DAMAGE_IMMUNITY_METADATA_KEY) && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                player.setMetadata(FALL_DAMAGE_IMMUNITY_METADATA_KEY, new FixedMetadataValue(SkyWarsPlugin.getInstance(), true));
                event.setCancelled(true);
                return;
            }

            if (player.getHealth() - event.getFinalDamage() > 0) {
                return;
            }

            String damagerName = lastDamager.getIfPresent(player.getName());

            event.setCancelled(true);
            player.sendTitle(ChatColor.RED + ChatColor.BOLD.toString() + "YOU DIED!", ChatColor.GRAY + "Better luck next time!", 0, 20 * 5, 20);
            skyWarsGame.getPlayerManager().getPlayer(player.getUniqueId()).ifPresent(skyWarsGame::setSpectator);

            if (damagerName != null) {
                skyWarsGame.getKills().put(damagerName, skyWarsGame.getKills().get(damagerName) + 1);
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " was killed by " + ChatColor.GOLD + damagerName);
            } else {
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " died!");
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuitWhileDamaged(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String damagerName = lastDamager.getIfPresent(player.getName());

        if (damagerName != null) {
            SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
                skyWarsGame.getPlayerManager().getPlayer(player.getUniqueId()).ifPresent(skyWarsGame::addKill);
                skyWarsGame.broadcast(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " was killed by " + ChatColor.GOLD + damagerName);
                lastDamager.invalidate(player.getName());
            });
        }
    }

    @EventHandler
    public void onPlayerDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Player damager;

        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damager = shooter;
        } else if (event.getDamager() instanceof Player damagerPlayer) {
            damager = damagerPlayer;
        } else {
            return;
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(damager)) {
                event.setCancelled(true);
                return;
            }

            lastDamager.put(player.getName(), damager.getName());
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTimeChange(TimeSkipEvent event) {
        if (event.getSkipReason() == TimeSkipEvent.SkipReason.CUSTOM) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
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
        SkyWarsPlugin.getInstance().getGameManager().findGame(event.getEntity().getWorld()).ifPresent(skyWarsGame -> {
            event.blockList().stream()
                .map(Block::getLocation)
                .filter(location -> skyWarsGame.getRefillableChests().stream().anyMatch(refillableChest -> refillableChest.getLocation().equals(location)))
                .forEach(skyWarsGame::removeRefillableChest);
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
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
            SkyWarsPlugin.getInstance().getGameManager().findGame(target).ifPresent(skyWarsGame -> {
                if (skyWarsGame.getPlayerManager().isSpectator(target)) {
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

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
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

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
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

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
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

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
                event.setCancelled(true);
                return;
            }

            if (!skyWarsGame.getSettings().canPickupItems()) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
                event.setCancelled(true);

                if (event.getItem() != null) {
                    if (event.getItem().getType() == Material.COMPASS) {
                        PlayerTrackerMenu playerTrackerMenu = new PlayerTrackerMenu();
                        playerTrackerMenu.displayTo(player);
                    } else if (event.getItem().getType() == Material.COMPARATOR) {
                        SpectatorSettingsMenu spectatorSettingsMenu = new SpectatorSettingsMenu();
                        spectatorSettingsMenu.displayTo(player);
                    }
                }

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

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
                event.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onVehicleExit(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) {
            return;
        }

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            if (skyWarsGame.getPlayerManager().isSpectator(player)) {
                event.setCancelled(true);
            }
        });

    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        SkyWarsPlugin.getInstance().getGameManager().findGame(player).ifPresent(skyWarsGame -> {
            String format = event.getFormat();
            Predicate<? super Player> filter = null;

            if (skyWarsGame.getPlayerManager().getPlayer(player.getUniqueId()).isPresent()) {
                format = ChatColor.GOLD + "[PLAYER] " + "%s" + ChatColor.RESET + ": %s";
                filter = recipient -> !recipient.getWorld().equals(player.getWorld());
            } else if (skyWarsGame.getPlayerManager().getSpectator(player.getUniqueId()).isPresent()) {
                format = ChatColor.GRAY + "[SPECTATOR] " + "%s" + ChatColor.RESET + ": %s";
                filter = recipient -> !skyWarsGame.getPlayerManager().isSpectator(recipient);
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
