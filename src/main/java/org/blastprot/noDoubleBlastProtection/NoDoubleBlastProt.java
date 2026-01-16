package org.blastprot.noDoubleBlastProtection;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.block.BlockExplodeEvent;
import java.util.*;

public class NoDoubleBlastProt extends JavaPlugin implements Listener {

    private final Map<UUID, Long> knockbackCooldown = new HashMap<>();
    private static final long COOLDOWN_MS = 50;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("NoDoubleBlastProt enabled - Catches ALL explosions");
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.getLocation(), event.getEntity().getNearbyEntities(8, 8, 8));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        handleExplosion(loc, event.getBlock().getWorld().getNearbyEntities(loc, 8, 8, 8));
    }

    private void handleExplosion(Location explosionLoc, Iterable<Entity> entities) {
        for (Entity entity : entities) {
            if (!(entity instanceof Player player)) continue;
            if (!hasEnoughBlastProtection(player)) continue;

            applyKnockback(player, explosionLoc);
        }
    }

    private void applyKnockback(Player player, Location explosionLoc) {
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();

        Long last = knockbackCooldown.get(id);
        if (last != null && now - last < COOLDOWN_MS) return;

        knockbackCooldown.put(id, now);

        Vector velocity = calculateKnockback(player.getLocation(), explosionLoc);
        player.setVelocity(velocity);

        Bukkit.getScheduler().runTaskLater(this, () -> knockbackCooldown.remove(id), 4L);
    }

    private Vector calculateKnockback(Location playerLoc, Location explosionLoc) {
        Vector dir = playerLoc.toVector().subtract(explosionLoc.toVector());

        if (dir.lengthSquared() < 0.01) {
            dir.setY(1);
        }

        dir.normalize();
        dir.multiply(0.45);
        dir.setY(dir.getY() + 0.35);

        return dir;
    }

    private boolean hasEnoughBlastProtection(Player player) {
        int count = 0;

        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || piece.getType() == Material.AIR) continue;
            if (!piece.hasItemMeta()) continue;

            if (piece.getItemMeta().hasEnchant(Enchantment.BLAST_PROTECTION)) {
                if (++count >= 2) return true;
            }
        }
        return false;
    }
}