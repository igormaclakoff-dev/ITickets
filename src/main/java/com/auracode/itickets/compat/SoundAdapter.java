package com.auracode.itickets.compat;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundAdapter {
    public static void playClick(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.2f);
    }

    public static void playSuccess(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.0f);
    }

    public static void playError(Player player) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
    }
}
