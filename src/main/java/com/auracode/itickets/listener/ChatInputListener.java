package com.auracode.itickets.listener;

import com.auracode.itickets.TicketService;
import com.auracode.itickets.config.MessageService;
import com.auracode.itickets.config.PluginSettings;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.session.ChatInput;
import com.auracode.itickets.session.ChatInputService;
import com.auracode.itickets.storage.TicketStorage;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputListener implements Listener {
    private final JavaPlugin plugin;
    private final PluginSettings settings;
    private final MessageService messages;
    private final ChatInputService chatInputs;
    private final TicketStorage storage;
    private final TicketService ticketService;
    private final Map<UUID, Long> suppressedChat = new ConcurrentHashMap<>();

    public ChatInputListener(JavaPlugin plugin, PluginSettings settings, MessageService messages,
            ChatInputService chatInputs, TicketStorage storage, TicketService ticketService) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.chatInputs = chatInputs;
        this.storage = storage;
        this.ticketService = ticketService;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!shouldCapture(player)) {
            suppressIfNeeded(event);
            return;
        }
        hide(event);
        ChatInput input = chatInputs.consume(player).orElse(null);
        if (input == null) {
            return;
        }
        markSuppressed(player);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> finishChatInput(player, input, text));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void keepChatHidden(AsyncChatEvent event) {
        suppressIfNeeded(event);
    }

    private void finishChatInput(Player player, ChatInput input, String text) {
        if (text.equalsIgnoreCase("отмена") || text.equalsIgnoreCase("cancel")) {
            messages.send((CommandSender) player, "input.cancelled");
            return;
        }
        switch (input.type()) {
            case CREATE -> ticketService.createFromInput(player, input.categoryId(), input.priority(), text);
            case COMMENT -> {
                Ticket ticket = storage.byId(input.ticketId());
                ticketService.addComment((CommandSender) player, ticket, text);
            }
            case CLOSE_REASON -> {
                Ticket ticket = storage.byId(input.ticketId());
                ticketService.close((CommandSender) player, ticket, text);
            }
            case NOTE -> {
                Ticket ticket = storage.byId(input.ticketId());
                ticketService.addNote((CommandSender) player, ticket, text);
            }
        }
    }

    private boolean shouldCapture(Player player) {
        return settings.captureChatInputs() && chatInputs.hasInput(player);
    }

    private void hide(AsyncChatEvent event) {
        event.setCancelled(true);
        event.viewers().clear();
    }

    private void suppressIfNeeded(AsyncChatEvent event) {
        if (isSuppressed(event.getPlayer())) {
            hide(event);
        }
    }

    private void markSuppressed(Player player) {
        suppressedChat.put(player.getUniqueId(), System.currentTimeMillis() + 1500L);
    }

    private boolean isSuppressed(Player player) {
        Long until = suppressedChat.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (until < System.currentTimeMillis()) {
            suppressedChat.remove(player.getUniqueId(), until);
            return false;
        }
        return true;
    }
}
