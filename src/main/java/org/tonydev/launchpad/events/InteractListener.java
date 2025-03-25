package org.tonydev.launchpad.events;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.tonydev.launchpad.LaunchPad;
import org.tonydev.launchpad.data.LaunchpadData;
import org.tonydev.launchpad.data.LaunchpadTemplate;
import org.tonydev.launchpad.util.LocationUtil;

public class InteractListener implements Listener {

    private final LaunchPad plugin;

    public InteractListener(LaunchPad plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Only check if player stepped on a block (from->to difference on same Y level)
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            Player player = event.getPlayer();
            Block block = event.getTo().getBlock().getRelative(0, -1, 0); // Block below player
            String blockLoc = LocationUtil.locationToString(block.getLocation());

            // Check if the location is a registered launchpad
            if (plugin.getLaunchpads().containsKey(blockLoc)) {
                LaunchpadData pad = plugin.getLaunchpads().get(blockLoc);

                // Check if the block type matches the launchpad's block type
                if (block.getType() == pad.getBlockType()) {
                    // Check permission if needed
                    if (plugin.isUsePermission() && !player.hasPermission(plugin.getPermissionNode())) {
                        return;
                    }

                    // Launch the player in the direction they're facing
                    Vector direction = player.getLocation().getDirection()
                            .multiply(pad.getHorizontalStrength())
                            .setY(pad.getVerticalStrength());
                    player.setVelocity(direction);

                    // Apply fall damage immunity
                    if (pad.getFallDamageImmunitySeconds() > 0) {
                        plugin.setFallDamageImmunity(player, pad.getFallDamageImmunitySeconds());
                    }

                    // Play sound effect
                    if (plugin.isPlaySounds()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.0f);
                    }

                    // Show particles
                    if (plugin.isShowParticles()) {
                        block.getWorld().spawnParticle(
                                Particle.CLOUD,
                                block.getLocation().add(0.5, 1.0, 0.5),
                                20, 0.3, 0.1, 0.3, 0.05
                        );
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();

        // Check if the player is in set mode
        if (plugin.getPlayerSetMode().containsKey(playerId)) {
            // Only handle right clicks on blocks
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasBlock()) {
                event.setCancelled(true); // Cancel the interaction

                String templateName = plugin.getPlayerSetMode().get(playerId);
                LaunchpadTemplate template = plugin.getTemplates().get(templateName);

                if (template == null) {
                    player.sendMessage(ChatColor.RED + "The template you were using no longer exists.");
                    plugin.getPlayerSetMode().remove(playerId);
                    return;
                }

                Block clickedBlock = event.getClickedBlock();
                String locStr = LocationUtil.locationToString(clickedBlock.getLocation());

                // Check if there's already a launchpad here
                if (plugin.getLaunchpads().containsKey(locStr)) {
                    player.sendMessage(ChatColor.RED + "There is already a launchpad at this location.");
                    return;
                }

                // Generate a unique name for this launchpad
                String baseName = template.getName() + "-pad";
                String uniqueName = baseName;
                int counter = 1;

                while (plugin.getLaunchpadNameToLocation().containsKey(uniqueName.toLowerCase())) {
                    uniqueName = baseName + counter;
                    counter++;
                }

                // Set the block to the specified block type
                clickedBlock.setType(template.getBlockType());

                // Create the launchpad data
                LaunchpadData data = new LaunchpadData(
                        uniqueName,
                        template.getHorizontalStrength(),
                        template.getVerticalStrength(),
                        template.getBlockType(),
                        template.getFallDamageImmunitySeconds()
                );

                // Save the launchpad
                plugin.getLaunchpads().put(locStr, data);
                plugin.getLaunchpadNameToLocation().put(uniqueName.toLowerCase(), locStr);
                plugin.saveLaunchpads();

                player.sendMessage(ChatColor.GREEN + "LaunchPad '" + uniqueName + "' created from template '" + template.getName() + "'");
            }
        }
    }
}