package com.auracode.itickets.gui;

import com.auracode.itickets.TicketService;
import com.auracode.itickets.config.CategoryRegistry;
import com.auracode.itickets.config.GuiConfig;
import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.config.MessageService;
import com.auracode.itickets.config.PluginSettings;
import com.auracode.itickets.config.TemplateRegistry;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.model.TicketStatus;
import com.auracode.itickets.session.ChatInputService;
import com.auracode.itickets.storage.TicketStorage;
import com.auracode.itickets.util.ItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class GuiService {
    final TicketStorage storage;
    final TicketService ticketService;
    final PluginSettings settings;
    final MessageService messages;
    final GuiConfig guiConfig;
    final CategoryRegistry categories;
    final ChatInputService chatInputs;
    final TemplateRegistry templates;
    final ItemFactory items;
    final GuiTextFormatter text;
    private final PlayerTicketMenu playerMenu;
    private final StaffTicketMenu staffMenu;
    private final TicketListMenu ticketListMenu;
    private final TicketViewMenu ticketViewMenu;
    private final CloseReasonMenu closeReasonMenu;
    private final ProfileMenu profileMenu;
    private final TemplatesMenu templatesMenu;

    public GuiService(TicketStorage storage, TicketService ticketService, PluginSettings settings,
            MessageService messages, GuiConfig guiConfig, CategoryRegistry categories,
            ChatInputService chatInputs, TemplateRegistry templates) {
        this.storage = storage;
        this.ticketService = ticketService;
        this.settings = settings;
        this.messages = messages;
        this.guiConfig = guiConfig;
        this.categories = categories;
        this.chatInputs = chatInputs;
        this.templates = templates;
        items = new ItemFactory(messages);
        text = new GuiTextFormatter(settings);
        playerMenu = new PlayerTicketMenu(this);
        staffMenu = new StaffTicketMenu(this);
        ticketListMenu = new TicketListMenu(this);
        ticketViewMenu = new TicketViewMenu(this);
        closeReasonMenu = new CloseReasonMenu(this);
        profileMenu = new ProfileMenu(this);
        templatesMenu = new TemplatesMenu(this);
    }

    public void showPlayerHome(Player player) {
        playerMenu.openHome(player);
    }

    public void showTicketCreation(Player player, String selectedCategoryId, int selectedPriority) {
        playerMenu.openCreateTicket(player, selectedCategoryId, selectedPriority);
    }

    public void showPlayerTickets(Player player, TicketStatus status, int page) {
        ticketListMenu.openOwnTickets(player, status, page);
    }

    public void showStaffDashboard(Player player) {
        staffMenu.openDashboard(player);
    }

    public void showStaffProfile(Player player, int page) {
        profileMenu.open(player, page);
    }

    public void showTemplates(Player player, Ticket ticket) {
        templatesMenu.open(player, ticket);
    }

    public void showTemplates(Player player, Ticket ticket, int page) {
        templatesMenu.open(player, ticket, page);
    }

    public void showStaffTickets(Player player, TicketStatus status, String categoryId, boolean assignedOnly,
            int page) {
        ticketListMenu.openStaffTickets(player, status, categoryId, assignedOnly, page);
    }

    public void showTicketDetails(Player player, Ticket ticket) {
        ticketViewMenu.openTicketDetails(player, ticket);
    }

    public void showCloseReasonPicker(Player player, Ticket ticket) {
        closeReasonMenu.openReasonPicker(player, ticket);
    }

    public void playClick(Player player) {
        if (settings.guiClickSound()) {
            try {
                Sound sound = Sound.valueOf(settings.clickSound().toUpperCase(Locale.ROOT));
                player.playSound(player.getLocation(), sound, 0.35f, 1.25f);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    Inventory createInventory(MenuHolder holder, String menuName, Map<String, String> placeholders) {
        int size = guiConfig.size(menuName, switch (menuName) {
            case "create", "list", "view", "profile" -> 54;
            case "staff" -> 54;
            case "templates" -> 54;
            case "main" -> 36;
            case "close_reasons" -> 27;
            default -> 27;
        });
        String titleRaw = guiConfig.title(menuName, "<white>ITickets</white>");
        Component titleComponent = renderTitle(applyPlaceholders(titleRaw, placeholders));
        Inventory inventory = Bukkit.createInventory((InventoryHolder) holder, size, titleComponent);
        holder.inventory(inventory);
        return inventory;
    }

    private Component renderTitle(String raw) {
        Component component = MiniMessage.miniMessage().deserialize(raw == null ? "" : raw);
        return disableItalicDeep(component);
    }

    private Component disableItalicDeep(Component component) {
        Component result = component.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false);
        java.util.List<Component> children = new java.util.ArrayList<>();
        for (Component child : result.children()) {
            children.add(disableItalicDeep(child));
        }
        return result.children(children);
    }

    void fillBackground(Inventory inventory) {
        GuiItemConfig filler = guiConfig.item("filler", -1, "GRAY_STAINED_GLASS_PANE", " ", List.of());
        ItemStack item = items.build(filler, Map.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    void placeClickableItem(Inventory inventory, MenuHolder holder, GuiItemConfig item,
            Map<String, String> placeholders, Consumer<InventoryClickEvent> action) {
        if (item.slot() < 0 || item.slot() >= inventory.getSize()) {
            return;
        }
        inventory.setItem(item.slot(), items.build(item, placeholders));
        holder.action(item.slot(), action);
    }

    GuiItemConfig itemAt(GuiItemConfig item, int slot) {
        return new GuiItemConfig(slot, item.material(), item.name(), item.lore(), item.amount(), item.glow(),
                item.customModelData());
    }

    private String applyPlaceholders(String raw, Map<String, String> placeholders) {
        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }
}
