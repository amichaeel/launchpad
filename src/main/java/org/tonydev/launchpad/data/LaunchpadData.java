package org.tonydev.launchpad.data;

import org.bukkit.Material;

public class LaunchpadData {
    private String name;
    private double horizontalStrength;
    private double verticalStrength;
    private Material blockType;
    private int fallDamageImmunitySeconds;

    public LaunchpadData(String name, double horizontalStrength, double verticalStrength, Material blockType, int fallDamageImmunitySeconds) {
        this.name = name;
        this.horizontalStrength = horizontalStrength;
        this.verticalStrength = verticalStrength;
        this.blockType = blockType;
        this.fallDamageImmunitySeconds = fallDamageImmunitySeconds;
    }

    public String getName() {
        return name;
    }

    public double getHorizontalStrength() {
        return horizontalStrength;
    }

    public double getVerticalStrength() {
        return verticalStrength;
    }

    public Material getBlockType() {
        return blockType;
    }

    public int getFallDamageImmunitySeconds() {
        return fallDamageImmunitySeconds;
    }
}