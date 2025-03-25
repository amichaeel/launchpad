package org.tonydev.launchpad;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import org.tonydev.launchpad.commands.LaunchPadCommand;
import org.tonydev.launchpad.data.LaunchpadData;
import org.tonydev.launchpad.data.LaunchpadTemplate;
import org.tonydev.launchpad.events.DamageListener;
import org.tonydev.launchpad.events.InteractListener;
import org.tonydev.launchpad.util.LocationUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LaunchPad extends JavaPlugin implements Listener {

    private Material launchpadMaterial;
    private boolean usePermission;
    private String permissionNode;
    private boolean playSounds;
    private boolean showParticles;
    private int defaultFallImmunity;

    // Map to store the last time a message was sent to a player
    private Map<UUID, Long> lastImmunityMessageTime;

    // Message cooldown in milliseconds (e.g., 2 seconds)
    private static final long MESSAGE_COOLDOWN = 2000L;

    // Map to store launchpad data - key is location string, value is LaunchpadData
    private Map<String, LaunchpadData> launchpads;

    // Map to store launchpad name to location for quick lookup
    private Map<String, String> launchpadNameToLocation;

    // Map to store launchpad templates - key is template name, value is template data
    private Map<String, LaunchpadTemplate> templates;

    // For the "set" feature - track which player is in set mode and what template they're using
    private Map<String, String> playerSetMode;

    // Map to track player immunity from fall damage - key is player UUID, value is time when immunity expires
    private Map<UUID, Long> fallDamageImmunity;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize data structures
        launchpads = new HashMap<>();
        launchpadNameToLocation = new HashMap<>();
        templates = new HashMap<>();
        playerSetMode = new HashMap<>();
        fallDamageImmunity = new HashMap<>();
        lastImmunityMessageTime = new HashMap<>();

        // Load configuration
        loadConfig();

        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);

        // Register commands with tab completion
        LaunchPadCommand commandExecutor = new LaunchPadCommand(this);
        getCommand("launchpad").setExecutor(commandExecutor);
        getCommand("launchpad").setTabCompleter(commandExecutor);

        getLogger().info("LaunchPad plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        saveConfig();
        lastImmunityMessageTime.clear();
        getLogger().info("LaunchPad plugin has been disabled!");
    }

    // Getters
    public Material getLaunchpadMaterial() {
        return launchpadMaterial;
    }

    public boolean isUsePermission() {
        return usePermission;
    }

    public String getPermissionNode() {
        return permissionNode;
    }

    public boolean isPlaySounds() {
        return playSounds;
    }

    public boolean isShowParticles() {
        return showParticles;
    }

    public int getDefaultFallImmunity() {
        return defaultFallImmunity;
    }

    public Map<String, LaunchpadData> getLaunchpads() {
        return launchpads;
    }

    public Map<String, String> getLaunchpadNameToLocation() {
        return launchpadNameToLocation;
    }

    public Map<String, LaunchpadTemplate> getTemplates() {
        return templates;
    }

    public Map<String, String> getPlayerSetMode() {
        return playerSetMode;
    }

    public Map<UUID, Long> getFallDamageImmunity() {
        return fallDamageImmunity;
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();

        // Set defaults for global settings if config doesn't exist
        config.addDefault("default-settings.launchpad-material", "SLIME_BLOCK");
        config.addDefault("default-settings.use-permission", true);
        config.addDefault("default-settings.permission-node", "launchpad.use");
        config.addDefault("default-settings.play-sounds", true);
        config.addDefault("default-settings.show-particles", true);
        config.addDefault("default-settings.default-fall-damage-immunity", 3); // Default 3 seconds
        config.options().copyDefaults(true);

        // Load global settings
        try {
            launchpadMaterial = Material.valueOf(config.getString("default-settings.launchpad-material"));
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid material in config! Defaulting to SLIME_BLOCK");
            launchpadMaterial = Material.SLIME_BLOCK;
        }
        usePermission = config.getBoolean("default-settings.use-permission");
        permissionNode = config.getString("default-settings.permission-node");
        playSounds = config.getBoolean("default-settings.play-sounds");
        showParticles = config.getBoolean("default-settings.show-particles");
        defaultFallImmunity = config.getInt("default-settings.default-fall-damage-immunity");

        // Load saved launchpads
        if (config.contains("launchpads")) {
            ConfigurationSection launchpadSection = config.getConfigurationSection("launchpads");

            for (String locationStr : launchpadSection.getKeys(false)) {
                ConfigurationSection padData = launchpadSection.getConfigurationSection(locationStr);

                String name = padData.getString("name");
                double horizontal = padData.getDouble("horizontal-strength");
                double vertical = padData.getDouble("vertical-strength");

                // Load block type, default to global launchpad material if not specified
                Material blockType;
                if (padData.contains("block-type")) {
                    try {
                        blockType = Material.valueOf(padData.getString("block-type"));
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid block type for launchpad '" + name + "'. Using default.");
                        blockType = launchpadMaterial;
                    }
                } else {
                    blockType = launchpadMaterial;
                }

                // Load fall damage immunity, default to global setting if not specified
                int immunity = padData.contains("fall-damage-immunity") ?
                        padData.getInt("fall-damage-immunity") : defaultFallImmunity;

                LaunchpadData data = new LaunchpadData(name, horizontal, vertical, blockType, immunity);
                launchpads.put(locationStr, data);
                launchpadNameToLocation.put(name.toLowerCase(), locationStr);
            }

            getLogger().info("Loaded " + launchpads.size() + " launchpads.");
        }

        // Load saved templates
        if (config.contains("templates")) {
            ConfigurationSection templateSection = config.getConfigurationSection("templates");

            for (String templateName : templateSection.getKeys(false)) {
                ConfigurationSection templateData = templateSection.getConfigurationSection(templateName);

                double horizontal = templateData.getDouble("horizontal-strength");
                double vertical = templateData.getDouble("vertical-strength");

                // Load block type, default to global launchpad material if not specified
                Material blockType;
                if (templateData.contains("block-type")) {
                    try {
                        blockType = Material.valueOf(templateData.getString("block-type"));
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid block type for template '" + templateName + "'. Using default.");
                        blockType = launchpadMaterial;
                    }
                } else {
                    blockType = launchpadMaterial;
                }

                // Load fall damage immunity, default to global setting if not specified
                int immunity = templateData.contains("fall-damage-immunity") ?
                        templateData.getInt("fall-damage-immunity") : defaultFallImmunity;

                LaunchpadTemplate template = new LaunchpadTemplate(templateName, horizontal, vertical, blockType, immunity);
                templates.put(templateName.toLowerCase(), template);
            }

            getLogger().info("Loaded " + templates.size() + " templates.");
        }

        saveConfig();
    }

    public void saveLaunchpads() {
        FileConfiguration config = getConfig();

        // Clear existing launchpads section
        config.set("launchpads", null);

        // Save each launchpad
        for (Map.Entry<String, LaunchpadData> entry : launchpads.entrySet()) {
            String locationStr = entry.getKey();
            LaunchpadData data = entry.getValue();

            config.set("launchpads." + locationStr + ".name", data.getName());
            config.set("launchpads." + locationStr + ".horizontal-strength", data.getHorizontalStrength());
            config.set("launchpads." + locationStr + ".vertical-strength", data.getVerticalStrength());
            config.set("launchpads." + locationStr + ".block-type", data.getBlockType().toString());
            config.set("launchpads." + locationStr + ".fall-damage-immunity", data.getFallDamageImmunitySeconds());
        }

        saveConfig();
    }

    public void saveTemplates() {
        FileConfiguration config = getConfig();

        // Clear existing templates section
        config.set("templates", null);

        // Save each template
        for (Map.Entry<String, LaunchpadTemplate> entry : templates.entrySet()) {
            String templateName = entry.getKey();
            LaunchpadTemplate template = entry.getValue();

            config.set("templates." + templateName + ".horizontal-strength", template.getHorizontalStrength());
            config.set("templates." + templateName + ".vertical-strength", template.getVerticalStrength());
            config.set("templates." + templateName + ".block-type", template.getBlockType().toString());
            config.set("templates." + templateName + ".fall-damage-immunity", template.getFallDamageImmunitySeconds());
        }

        saveConfig();
    }

    public void reloadPlugin() {
        // Reload config file from disk
        reloadConfig();

        // Clear current data
        launchpads.clear();
        launchpadNameToLocation.clear();
        templates.clear();
        playerSetMode.clear();
        fallDamageImmunity.clear();
        lastImmunityMessageTime.clear();

        // Re-parse config values
        loadConfig();

        // Log reload
        getLogger().info("LaunchPad configuration has been reloaded!");
    }

    // Set fall damage immunity for a player
    public void setFallDamageImmunity(Player player, int seconds) {
        UUID playerId = player.getUniqueId();

        // Calculate the time when immunity expires (current time + immunity duration in milliseconds)
        long currentTime = System.currentTimeMillis();
        long immunityExpires = currentTime + (seconds * 1000L);

        // Set the immunity
        fallDamageImmunity.put(playerId, immunityExpires);

        if (seconds > 0) {
            // Check if we should send a message (if enough time has passed since the last one)
            boolean shouldSendMessage = true;
            if (lastImmunityMessageTime.containsKey(playerId)) {
                long lastMessageTime = lastImmunityMessageTime.get(playerId);
                if (currentTime - lastMessageTime < MESSAGE_COOLDOWN) {
                    shouldSendMessage = false;
                }
            }

            // Inform player about immunity (if cooldown has passed)
            if (shouldSendMessage) {
                player.sendMessage(org.bukkit.ChatColor.GREEN + "Fall damage immunity active for " + seconds + " seconds.");
                lastImmunityMessageTime.put(playerId, currentTime);
            }
        }
    }

    // Check if a player has fall damage immunity
    public boolean hasFallDamageImmunity(Player player) {
        clearExpiredImmunities();

        UUID playerId = player.getUniqueId();
        if (fallDamageImmunity.containsKey(playerId)) {
            long immunityExpires = fallDamageImmunity.get(playerId);
            return System.currentTimeMillis() < immunityExpires;
        }

        return false;
    }

    // Clean up expired immunities
    public void clearExpiredImmunities() {
        long currentTime = System.currentTimeMillis();
        fallDamageImmunity.entrySet().removeIf(entry -> entry.getValue() < currentTime);
    }

    // Remove fall damage immunity for a player
    public void removeFallDamageImmunity(Player player) {
        fallDamageImmunity.remove(player.getUniqueId());
    }
}