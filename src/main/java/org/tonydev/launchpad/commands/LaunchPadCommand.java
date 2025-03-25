package org.tonydev.launchpad.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.tonydev.launchpad.LaunchPad;
import org.tonydev.launchpad.data.LaunchpadData;
import org.tonydev.launchpad.data.LaunchpadTemplate;
import org.tonydev.launchpad.util.LocationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LaunchPadCommand implements CommandExecutor, TabCompleter {
    private final LaunchPad plugin;

    public LaunchPadCommand(LaunchPad plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("launchpad.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        FileConfiguration config = plugin.getConfig();

        switch (args[0].toLowerCase()) {
            case "create":
                if (args.length < 4) {
                    player.sendMessage(ChatColor.RED + "Usage: /launchpad create <name> <horizontal> <vertical> [fall-immunity] [block-type]");
                    return true;
                }

                String name = args[1];
                double horizontal, vertical;
                int fallImmunity = plugin.getDefaultFallImmunity(); // Default from config

                try {
                    horizontal = Double.parseDouble(args[2]);
                    vertical = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Horizontal and vertical values must be numbers.");
                    return true;
                }

                if (horizontal <= 0 || vertical < 0) {
                    player.sendMessage(ChatColor.RED + "Horizontal must be positive, vertical must be non-negative.");
                    return true;
                }

                // Check for fall damage immunity parameter
                if (args.length >= 5) {
                    try {
                        fallImmunity = Integer.parseInt(args[4]);
                        if (fallImmunity < 0) {
                            player.sendMessage(ChatColor.RED + "Fall damage immunity must be a non-negative number.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        // If not a number, assume it's the block type instead
                        fallImmunity = plugin.getDefaultFallImmunity();
                    }
                }

                // Check for block type parameter (now possibly in position 5 or 6 depending on if immunity was specified)
                Material blockType = plugin.getLaunchpadMaterial(); // Default
                int blockTypeIndex = (args.length >= 5 && isInteger(args[4])) ? 5 : 4;

                if (args.length > blockTypeIndex) {
                    try {
                        blockType = Material.valueOf(args[blockTypeIndex].toUpperCase());
                        if (!blockType.isBlock()) {
                            player.sendMessage(ChatColor.RED + "The specified material is not a valid block.");
                            return true;
                        }
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(ChatColor.RED + "Invalid block type. Use a valid Minecraft block name.");
                        return true;
                    }
                }

                createLaunchPad(player, name, horizontal, vertical, fallImmunity, blockType);
                break;

            case "template":
                if (args.length < 2) {
                    listTemplates(player);
                    return true;
                }

                if (args[1].equalsIgnoreCase("create")) {
                    if (args.length < 5) {
                        player.sendMessage(ChatColor.RED + "Usage: /launchpad template create <n> <horizontal> <vertical> [fall-immunity] [block-type]");
                        return true;
                    }

                    String templateName = args[2];
                    double templateH, templateV;
                    int templateImmunity = plugin.getDefaultFallImmunity(); // Default from config

                    try {
                        templateH = Double.parseDouble(args[3]);
                        templateV = Double.parseDouble(args[4]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Horizontal and vertical values must be numbers.");
                        return true;
                    }

                    if (templateH <= 0 || templateV < 0) {
                        player.sendMessage(ChatColor.RED + "Horizontal must be positive, vertical must be non-negative.");
                        return true;
                    }

                    // Check for fall damage immunity parameter
                    if (args.length >= 6) {
                        try {
                            templateImmunity = Integer.parseInt(args[5]);
                            if (templateImmunity < 0) {
                                player.sendMessage(ChatColor.RED + "Fall damage immunity must be a non-negative number.");
                                return true;
                            }
                        } catch (NumberFormatException e) {
                            // If not a number, assume it's the block type instead
                            templateImmunity = plugin.getDefaultFallImmunity();
                        }
                    }

                    // Check for block type parameter
                    Material templateBlockType = plugin.getLaunchpadMaterial(); // Default
                    int templateBlockTypeIndex = (args.length >= 6 && isInteger(args[5])) ? 6 : 5;

                    if (args.length > templateBlockTypeIndex) {
                        try {
                            templateBlockType = Material.valueOf(args[templateBlockTypeIndex].toUpperCase());
                            if (!templateBlockType.isBlock()) {
                                player.sendMessage(ChatColor.RED + "The specified material is not a valid block.");
                                return true;
                            }
                        } catch (IllegalArgumentException e) {
                            player.sendMessage(ChatColor.RED + "Invalid block type. Use a valid Minecraft block name.");
                            return true;
                        }
                    }

                    createTemplate(player, templateName, templateH, templateV, templateImmunity, templateBlockType);
                } else if (args[1].equalsIgnoreCase("remove")) {
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /launchpad template remove <n>");
                        return true;
                    }

                    removeTemplate(player, args[2]);
                } else if (args[1].equalsIgnoreCase("info")) {
                    if (args.length < 3) {
                        player.sendMessage(ChatColor.RED + "Usage: /launchpad template info <n>");
                        return true;
                    }

                    showTemplateInfo(player, args[2]);
                } else {
                    player.sendMessage(ChatColor.RED + "Unknown template command. Use create, remove, or info.");
                }
                break;

            case "set":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /launchpad set <template-name>");
                    return true;
                }

                String templateName = args[1].toLowerCase();
                if (!plugin.getTemplates().containsKey(templateName)) {
                    player.sendMessage(ChatColor.RED + "No template found with the name '" + args[1] + "'.");
                    return true;
                }

                // Enable set mode
                plugin.getPlayerSetMode().put(player.getUniqueId().toString(), templateName);
                player.sendMessage(ChatColor.GREEN + "You are now in set mode with template '" + args[1] + "'.");
                player.sendMessage(ChatColor.GREEN + "Right-click on blocks to place launchpads. Type '/launchpad cancel' to exit set mode.");
                break;

            case "cancel":
                // Cancel set mode
                if (plugin.getPlayerSetMode().remove(player.getUniqueId().toString()) != null) {
                    player.sendMessage(ChatColor.GREEN + "Set mode cancelled.");
                } else {
                    player.sendMessage(ChatColor.YELLOW + "You were not in set mode.");
                }
                break;

            case "remove":
                if (args.length >= 2) {
                    // Remove by name
                    removeLaunchPadByName(player, args[1]);
                } else {
                    // Remove by looking at block
                    removeLaunchPad(player);
                }
                break;

            case "list":
                listLaunchPads(player);
                break;

            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /launchpad info <n>");
                    return true;
                }
                showLaunchPadInfo(player, args[1]);
                break;

            case "reload":
                player.sendMessage(ChatColor.YELLOW + "Reloading LaunchPad configuration...");
                plugin.reloadPlugin();
                player.sendMessage(ChatColor.GREEN + "LaunchPad configuration reloaded successfully!");
                player.sendMessage(ChatColor.GREEN + "Loaded " + plugin.getLaunchpads().size() + " launchpads and " +
                        plugin.getTemplates().size() + " templates.");
                break;

            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("create");
            completions.add("template");
            completions.add("set");
            completions.add("cancel");
            completions.add("remove");
            completions.add("list");
            completions.add("info");
            completions.add("reload");

            return filterCompletions(completions, args[0]);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("info")) {
                // Return all launchpad names
                return filterCompletions(new ArrayList<>(plugin.getLaunchpadNameToLocation().keySet()), args[1]);
            } else if (args[0].equalsIgnoreCase("set")) {
                // Return all template names
                return filterCompletions(new ArrayList<>(plugin.getTemplates().keySet()), args[1]);
            } else if (args[0].equalsIgnoreCase("template")) {
                completions.add("create");
                completions.add("remove");
                completions.add("info");
                completions.add("list");
                return filterCompletions(completions, args[1]);
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("template") &&
                    (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("info"))) {
                // Return all template names
                return filterCompletions(new ArrayList<>(plugin.getTemplates().keySet()), args[2]);
            }
        }

        return completions;
    }

    private List<String> filterCompletions(List<String> completions, String prefix) {
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GREEN + "===== LaunchPad Commands =====");
        player.sendMessage(ChatColor.YELLOW + "/launchpad create <n> <horizontal> <vertical> [fall-immunity] [block-type] " +
                ChatColor.WHITE + "- Create a launchpad at your target block");
        player.sendMessage(ChatColor.YELLOW + "/launchpad template create <n> <horizontal> <vertical> [fall-immunity] [block-type] " +
                ChatColor.WHITE + "- Create a reusable launchpad template");
        player.sendMessage(ChatColor.YELLOW + "/launchpad template remove <n> " +
                ChatColor.WHITE + "- Remove a template");
        player.sendMessage(ChatColor.YELLOW + "/launchpad template info <n> " +
                ChatColor.WHITE + "- Show template details");
        player.sendMessage(ChatColor.YELLOW + "/launchpad template " +
                ChatColor.WHITE + "- List all templates");
        player.sendMessage(ChatColor.YELLOW + "/launchpad set <template-name> " +
                ChatColor.WHITE + "- Enter set mode to place launchpads from template");
        player.sendMessage(ChatColor.YELLOW + "/launchpad cancel " +
                ChatColor.WHITE + "- Exit set mode");
        player.sendMessage(ChatColor.YELLOW + "/launchpad remove [name] " +
                ChatColor.WHITE + "- Remove a launchpad by name or by looking at it");
        player.sendMessage(ChatColor.YELLOW + "/launchpad list " +
                ChatColor.WHITE + "- List all launchpads");
        player.sendMessage(ChatColor.YELLOW + "/launchpad info <n> " +
                ChatColor.WHITE + "- Show details about a launchpad");
        player.sendMessage(ChatColor.YELLOW + "/launchpad reload " +
                ChatColor.WHITE + "- Reload the plugin configuration");
    }

    private void createLaunchPad(Player player, String name, double horizontal, double vertical, int fallImmunity, Material blockType) {
        // Check if name already exists
        if (plugin.getLaunchpadNameToLocation().containsKey(name.toLowerCase())) {
            player.sendMessage(ChatColor.RED + "A launchpad with that name already exists.");
            return;
        }

        Block targetBlock = player.getTargetBlock(null, 5);
        if (targetBlock.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be looking at a block.");
            return;
        }

        // Check if there's already a launchpad at this location
        String locStr = LocationUtil.locationToString(targetBlock.getLocation());
        if (plugin.getLaunchpads().containsKey(locStr)) {
            player.sendMessage(ChatColor.RED + "There is already a launchpad at this location.");
            return;
        }

        // Set the block to the specified block type
        targetBlock.setType(blockType);

        // Save the location and data
        LaunchpadData data = new LaunchpadData(name, horizontal, vertical, blockType, fallImmunity);

        plugin.getLaunchpads().put(locStr, data);
        plugin.getLaunchpadNameToLocation().put(name.toLowerCase(), locStr);
        plugin.saveLaunchpads();

        player.sendMessage(ChatColor.GREEN + "LaunchPad '" + name + "' created with:");
        player.sendMessage(ChatColor.GREEN + "- Horizontal strength: " + horizontal);
        player.sendMessage(ChatColor.GREEN + "- Vertical strength: " + vertical);
        player.sendMessage(ChatColor.GREEN + "- Fall damage immunity: " + fallImmunity + " seconds");
        player.sendMessage(ChatColor.GREEN + "- Block type: " + blockType.toString());
    }

    private void createTemplate(Player player, String name, double horizontal, double vertical, int fallImmunity, Material blockType) {
        String lowerName = name.toLowerCase();

        // Check if name already exists
        if (plugin.getTemplates().containsKey(lowerName)) {
            player.sendMessage(ChatColor.RED + "A template with that name already exists.");
            return;
        }

        // Create the template
        LaunchpadTemplate template = new LaunchpadTemplate(name, horizontal, vertical, blockType, fallImmunity);
        plugin.getTemplates().put(lowerName, template);
        plugin.saveTemplates();

        player.sendMessage(ChatColor.GREEN + "Template '" + name + "' created with:");
        player.sendMessage(ChatColor.GREEN + "- Horizontal strength: " + horizontal);
        player.sendMessage(ChatColor.GREEN + "- Vertical strength: " + vertical);
        player.sendMessage(ChatColor.GREEN + "- Fall damage immunity: " + fallImmunity + " seconds");
        player.sendMessage(ChatColor.GREEN + "- Block type: " + blockType.toString());
        player.sendMessage(ChatColor.GREEN + "Use '/launchpad set " + name + "' to place launchpads with this template.");
    }

    private void removeTemplate(Player player, String name) {
        String lowerName = name.toLowerCase();

        if (!plugin.getTemplates().containsKey(lowerName)) {
            player.sendMessage(ChatColor.RED + "No template found with the name '" + name + "'.");
            return;
        }

        plugin.getTemplates().remove(lowerName);
        plugin.saveTemplates();

        player.sendMessage(ChatColor.GREEN + "Template '" + name + "' removed!");
    }

    private void showTemplateInfo(Player player, String name) {
        String lowerName = name.toLowerCase();

        if (!plugin.getTemplates().containsKey(lowerName)) {
            player.sendMessage(ChatColor.RED + "No template found with the name '" + name + "'.");
            return;
        }

        LaunchpadTemplate template = plugin.getTemplates().get(lowerName);

        player.sendMessage(ChatColor.GREEN + "===== Template '" + template.getName() + "' =====");
        player.sendMessage(ChatColor.YELLOW + "Horizontal Strength: " + ChatColor.WHITE + template.getHorizontalStrength());
        player.sendMessage(ChatColor.YELLOW + "Vertical Strength: " + ChatColor.WHITE + template.getVerticalStrength());
        player.sendMessage(ChatColor.YELLOW + "Fall Damage Immunity: " + ChatColor.WHITE + template.getFallDamageImmunitySeconds() + " seconds");
        player.sendMessage(ChatColor.YELLOW + "Block Type: " + ChatColor.WHITE + template.getBlockType().toString());
    }

    private void listTemplates(Player player) {
        if (plugin.getTemplates().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no templates created yet.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "===== Templates (" + plugin.getTemplates().size() + ") =====");

        for (LaunchpadTemplate template : plugin.getTemplates().values()) {
            player.sendMessage(ChatColor.YELLOW + template.getName() +
                    ChatColor.WHITE + " - H:" + template.getHorizontalStrength() +
                    " V:" + template.getVerticalStrength() +
                    " Immunity:" + template.getFallDamageImmunitySeconds() + "s" +
                    " Block:" + template.getBlockType().toString().toLowerCase());
        }
    }

    private void removeLaunchPad(Player player) {
        Block targetBlock = player.getTargetBlock(null, 5);
        String locStr = LocationUtil.locationToString(targetBlock.getLocation());

        if (!plugin.getLaunchpads().containsKey(locStr)) {
            player.sendMessage(ChatColor.RED + "This is not a registered LaunchPad.");
            return;
        }

        // Get the name before removing
        String name = plugin.getLaunchpads().get(locStr).getName();
        Material blockType = plugin.getLaunchpads().get(locStr).getBlockType();

        // Check if the block type matches
        if (targetBlock.getType() != blockType) {
            player.sendMessage(ChatColor.YELLOW + "Warning: The block type has changed from the original launchpad.");
        }

        // Remove from saved locations
        plugin.getLaunchpads().remove(locStr);
        plugin.getLaunchpadNameToLocation().remove(name.toLowerCase());
        plugin.saveLaunchpads();

        player.sendMessage(ChatColor.GREEN + "LaunchPad '" + name + "' removed!");
    }

    private void removeLaunchPadByName(Player player, String name) {
        String locStr = plugin.getLaunchpadNameToLocation().get(name.toLowerCase());

        if (locStr == null) {
            player.sendMessage(ChatColor.RED + "No launchpad found with the name '" + name + "'.");
            return;
        }

        // Get the block type before removing
        Material blockType = plugin.getLaunchpads().get(locStr).getBlockType();

        // Remove from maps
        plugin.getLaunchpads().remove(locStr);
        plugin.getLaunchpadNameToLocation().remove(name.toLowerCase());
        plugin.saveLaunchpads();

        // Try to find and remove the actual block
        org.bukkit.Location loc = LocationUtil.stringToLocation(locStr);
        if (loc != null && loc.getBlock().getType() == blockType) {
            // Optionally reset block type if you want
            // loc.getBlock().setType(Material.AIR);
        }

        player.sendMessage(ChatColor.GREEN + "LaunchPad '" + name + "' removed!");
    }

    private void listLaunchPads(Player player) {
        if (plugin.getLaunchpads().isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "There are no launchpads created yet.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "===== LaunchPads (" + plugin.getLaunchpads().size() + ") =====");

        for (LaunchpadData pad : plugin.getLaunchpads().values()) {
            player.sendMessage(ChatColor.YELLOW + pad.getName() +
                    ChatColor.WHITE + " - H:" + pad.getHorizontalStrength() +
                    " V:" + pad.getVerticalStrength() +
                    " Immunity:" + pad.getFallDamageImmunitySeconds() + "s" +
                    " Block:" + pad.getBlockType().toString().toLowerCase());
        }
    }

    private void showLaunchPadInfo(Player player, String name) {
        String locStr = plugin.getLaunchpadNameToLocation().get(name.toLowerCase());

        if (locStr == null) {
            player.sendMessage(ChatColor.RED + "No launchpad found with the name '" + name + "'.");
            return;
        }

        LaunchpadData pad = plugin.getLaunchpads().get(locStr);
        org.bukkit.Location loc = LocationUtil.stringToLocation(locStr);

        player.sendMessage(ChatColor.GREEN + "===== LaunchPad '" + pad.getName() + "' =====");
        player.sendMessage(ChatColor.YELLOW + "Horizontal Strength: " + ChatColor.WHITE + pad.getHorizontalStrength());
        player.sendMessage(ChatColor.YELLOW + "Vertical Strength: " + ChatColor.WHITE + pad.getVerticalStrength());
        player.sendMessage(ChatColor.YELLOW + "Fall Damage Immunity: " + ChatColor.WHITE + pad.getFallDamageImmunitySeconds() + " seconds");
        player.sendMessage(ChatColor.YELLOW + "Block Type: " + ChatColor.WHITE + pad.getBlockType().toString());

        if (loc != null) {
            player.sendMessage(ChatColor.YELLOW + "Location: " + ChatColor.WHITE +
                    loc.getWorld().getName() + " at " +
                    loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
    }

    // Helper method to check if a string is an integer
    private boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}