package org.tonydev.launchpad.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.tonydev.launchpad.LaunchPad;

public class DamageListener implements Listener {

    private final LaunchPad plugin;

    public DamageListener(LaunchPad plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Check if we need to cancel fall damage
        if (event.getEntity() instanceof Player &&
                event.getCause() == EntityDamageEvent.DamageCause.FALL) {

            Player player = (Player) event.getEntity();

            // Check if the player has fall damage immunity
            if (plugin.hasFallDamageImmunity(player)) {
                // Cancel the damage
                event.setCancelled(true);
            }
        }
    }
}