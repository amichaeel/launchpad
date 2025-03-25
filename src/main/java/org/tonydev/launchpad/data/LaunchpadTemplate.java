package org.tonydev.launchpad.data;

import org.bukkit.Material;

public class LaunchpadTemplate {
    private String name;
    private double horizontalStrength;
    private double verticalStrength;
    private Material blockType;
    private int fallDamageImmunitySeconds;

    public LaunchpadTemplate(String name, double horizontalStrength, double verticalStrength, Material blockType, int fallDamageImmunitySeconds) {
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