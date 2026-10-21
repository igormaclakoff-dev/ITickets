package com.auracode.itickets.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PluginSettings {
    private final JavaPlugin plugin;
    private int cooldownSeconds;
    private int maxOpenTickets;
    private int maxMessageLength;
    private int ticketsPerPage;
    private boolean guiClickSound;
    private String clickSound;
    private String dateFormat;
    private String timeZone;
    private boolean captureChatInputs;
    private boolean autoCloseEnabled;
    private int autoCloseDays;
    private String autoCloseReason;
    private boolean remindersEnabled;
    private int reminderIntervalMinutes;
    private int reminderAfterMinutes;

    public PluginSettings(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        cooldownSeconds = config.getInt("settings.cooldown-seconds", 60);
        maxOpenTickets = config.getInt("settings.max-open-tickets", 3);
        maxMessageLength = config.getInt("settings.max-message-length", 500);
        ticketsPerPage = Math.max(1, Math.min(45, config.getInt("settings.tickets-per-page", 45)));
        guiClickSound = config.getBoolean("settings.gui-click-sound.enabled", true);
        clickSound = config.getString("settings.gui-click-sound.sound", "UI_BUTTON_CLICK");
        dateFormat = config.getString("settings.date-format", "dd.MM.yyyy HH:mm");
        timeZone = config.getString("settings.time-zone", "Europe/Moscow");
        captureChatInputs = config.getBoolean("settings.capture-chat-inputs", true);
        autoCloseEnabled = config.getBoolean("settings.auto-close.enabled", false);
        autoCloseDays = Math.max(1, config.getInt("settings.auto-close.days", 7));
        autoCloseReason = config.getString("settings.auto-close.reason", "Автозакрытие: нет активности");
        remindersEnabled = config.getBoolean("settings.reminders.enabled", false);
        reminderIntervalMinutes = Math.max(1, config.getInt("settings.reminders.interval-minutes", 30));
        reminderAfterMinutes = Math.max(1, config.getInt("settings.reminders.after-minutes", 60));
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public int maxOpenTickets() {
        return maxOpenTickets;
    }

    public int maxMessageLength() {
        return maxMessageLength;
    }

    public int ticketsPerPage() {
        return ticketsPerPage;
    }

    public boolean guiClickSound() {
        return guiClickSound;
    }

    public String clickSound() {
        return clickSound;
    }

    public String dateFormat() {
        return dateFormat;
    }

    public String timeZone() {
        return timeZone;
    }

    public boolean captureChatInputs() {
        return captureChatInputs;
    }

    public boolean autoCloseEnabled() {
        return autoCloseEnabled;
    }

    public int autoCloseDays() {
        return autoCloseDays;
    }

    public String autoCloseReason() {
        return autoCloseReason;
    }

    public boolean remindersEnabled() {
        return remindersEnabled;
    }

    public int reminderIntervalMinutes() {
        return reminderIntervalMinutes;
    }

    public int reminderAfterMinutes() {
        return reminderAfterMinutes;
    }
}
