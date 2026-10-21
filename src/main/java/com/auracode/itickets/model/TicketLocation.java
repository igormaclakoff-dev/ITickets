package com.auracode.itickets.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public record TicketLocation(String world, double x, double y, double z, float yaw, float pitch) {
    public static TicketLocation from(Location location) {
        String worldName = location.getWorld() == null ? "" : location.getWorld().getName();
        return new TicketLocation(worldName, location.getX(), location.getY(), location.getZ(), location.getYaw(),
                location.getPitch());
    }

    public Location toBukkitLocation() {
        World bukkitWorld = Bukkit.getWorld(world);
        if (bukkitWorld == null) {
            return null;
        }
        return new Location(bukkitWorld, x, y, z, yaw, pitch);
    }

    public String compact() {
        return world + " " + Math.round(x) + " " + Math.round(y) + " " + Math.round(z);
    }
}
