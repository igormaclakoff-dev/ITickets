package com.auracode.itickets;

import com.auracode.itickets.command.TicketCommand;
import com.auracode.itickets.config.CategoryRegistry;
import com.auracode.itickets.config.GuiConfig;
import com.auracode.itickets.config.MessageService;
import com.auracode.itickets.config.PluginSettings;
import com.auracode.itickets.config.TemplateRegistry;
import com.auracode.itickets.gui.GuiService;
import com.auracode.itickets.listener.ChatInputListener;
import com.auracode.itickets.listener.GuiListener;
import com.auracode.itickets.listener.JoinListener;
import com.auracode.itickets.session.ChatInputService;
import com.auracode.itickets.storage.TicketStorage;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ITicketsPlugin extends JavaPlugin {
    private PluginSettings settings;
    private MessageService messages;
    private CategoryRegistry categories;
    private GuiConfig guiConfig;
    private TemplateRegistry templates;
    private TicketStorage storage;
    private TicketService ticketService;
    private ChatInputService chatInputs;
    private GuiService guiService;
    private org.bukkit.scheduler.BukkitTask autoCloseTask;
    private org.bukkit.scheduler.BukkitTask reminderTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("permissions.yml");
        settings = new PluginSettings(this);
        messages = new MessageService(this);
        categories = new CategoryRegistry(this);
        guiConfig = new GuiConfig(this);
        templates = new TemplateRegistry(this);
        storage = new TicketStorage(this);
        chatInputs = new ChatInputService();
        reloadEverything();
        ticketService = new TicketService(storage, settings, messages, categories);
        guiService = new GuiService(storage, ticketService, settings, messages, guiConfig, categories, chatInputs,
                templates);
        getServer().getPluginManager().registerEvents((Listener) new GuiListener(guiService), this);
        getServer().getPluginManager().registerEvents(
                (Listener) new ChatInputListener(this, settings, messages, chatInputs, storage, ticketService), this);
        getServer().getPluginManager().registerEvents((Listener) new JoinListener(storage, messages), this);
        TicketCommand command = new TicketCommand(storage, ticketService, guiService, messages, chatInputs,
                this::reloadEverything);
        PluginCommand pluginCommand = getCommand("iticket");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }
        startAutomation();
        getLogger().info("ITickets включён. Хранилище: plugins/ITickets/tickets/<ник>.yml");
    }

    private void startAutomation() {
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        if (reminderTask != null) {
            reminderTask.cancel();
        }
        if (settings.autoCloseEnabled()) {
            autoCloseTask = getServer().getScheduler().runTaskTimer(this, this::runAutoClose, 20L * 60L,
                    20L * 60L * 60L);
        }
        if (settings.remindersEnabled()) {
            long interval = 20L * 60L * settings.reminderIntervalMinutes();
            reminderTask = getServer().getScheduler().runTaskTimer(this, this::runReminders, interval, interval);
        }
    }

    private void runAutoClose() {
        if (ticketService == null || !settings.autoCloseEnabled()) {
            return;
        }
        long threshold = System.currentTimeMillis() - settings.autoCloseDays() * 86_400_000L;
        for (var ticket : storage.allTickets()) {
            if (ticket.status().isActive() && ticket.updatedAt() < threshold) {
                ticketService.autoClose(ticket);
            }
        }
    }

    private void runReminders() {
        if (ticketService == null || !settings.remindersEnabled()) {
            return;
        }
        ticketService.remindStaff();
    }

    @Override
    public void onDisable() {
        if (autoCloseTask != null) {
            autoCloseTask.cancel();
        }
        if (reminderTask != null) {
            reminderTask.cancel();
        }
        if (messages != null) {
            messages.close();
        }
    }

    private void saveResourceIfMissing(String resourceName) {
        if (!new File(getDataFolder(), resourceName).exists()) {
            saveResource(resourceName, false);
        }
    }

    public void reloadEverything() {
        settings.reload();
        messages.reload();
        categories.reload();
        guiConfig.reload();
        templates.reload();
        storage.loadAll();
        if (ticketService != null) {
            ticketService.reloadFormatter();
        }
        startAutomation();
    }
}
