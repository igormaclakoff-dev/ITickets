package com.auracode.itickets.gui;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.model.TicketStatus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

final class TicketListMenu {
    private final GuiService gui;

    TicketListMenu(GuiService gui) {
        this.gui = gui;
    }

    void openOwnTickets(Player player, TicketStatus status, int page) {
        List<Ticket> tickets = gui.storage.ownTickets(player.getUniqueId(), true, false).stream()
                .filter(ticket -> ticket.status() == status).toList();
        openTickets(player, tickets, "list", page, event -> gui.showPlayerHome(player));
    }

    void openStaffTickets(Player player, TicketStatus status, String categoryId, boolean assignedOnly, int page) {
        if (!player.hasPermission("itickets.view.all") && !player.hasPermission("itickets.staff")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        if (status == TicketStatus.DELETED && !player.hasPermission("itickets.view.deleted")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        if (status == TicketStatus.CLOSED && !player.hasPermission("itickets.view.closed")
                && !player.hasPermission("itickets.staff")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        List<Ticket> tickets = gui.storage.staffTickets(status, categoryId, assignedOnly ? player.getUniqueId() : null);
        openTickets(player, tickets, "list", page, event -> gui.showStaffDashboard(player));
    }

    private void openTickets(Player player, List<Ticket> tickets, String menuName, int page,
            Consumer<InventoryClickEvent> backAction) {
        int perPage = Math.min(gui.settings.ticketsPerPage(), gui.guiConfig.ticketSlots().size());
        int maxPage = Math.max(0, (tickets.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(page, maxPage));
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, menuName,
                Map.of("page", String.valueOf(safePage + 1), "pages", String.valueOf(maxPage + 1)));
        gui.fillBackground(inventory);
        List<Integer> slots = gui.guiConfig.ticketSlots();
        int start = safePage * perPage;
        int end = Math.min(tickets.size(), start + perPage);
        for (int index = start; index < end; index++) {
            Ticket ticket = tickets.get(index);
            int slot = slots.get(index - start);
            HashMap<String, String> placeholders = new HashMap<>(gui.ticketService.placeholders(ticket));
            placeholders.put("first_message",
                    gui.text.wrapPlaceholder(ticket.firstMessage() == null ? "" : ticket.firstMessage().text(),
                            "<dark_gray>", 38, gui.messages));
            GuiItemConfig template = gui.guiConfig.item("ticket.item", slot, "PAPER",
                    "<white>Тикет #%id%</white> <dark_gray>·</dark_gray> %status_colored%",
                    List.of("<dark_gray>Автор</dark_gray> <white>%player%</white>",
                            "%category% <dark_gray>·</dark_gray> <gray>важность</gray> %priority_colored%",
                            "<gray>Ответственный:</gray> <white>%assigned%</white>",
                            "<gray>Обновлен:</gray> <white>%updated%</white>",
                            " ",
                            "<dark_gray>%first_message%</dark_gray>",
                            " ",
                            "<green>▸ Открыть тикет</green>"));
            boolean glow = ticket.priority() >= 4;
            GuiItemConfig styled = new GuiItemConfig(template.slot(), template.material(), template.name(),
                    template.lore(), template.amount(), glow, template.customModelData());
            gui.placeClickableItem(inventory, holder, gui.itemAt(styled, slot), placeholders,
                    event -> gui.showTicketDetails(player, ticket));
        }
        if (safePage > 0) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("navigation.previous", 45, "ARROW", "<white>Страница назад</white>",
                            List.of()),
                    Map.of(), event -> openTickets(player, tickets, menuName, safePage - 1, backAction));
        }
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("navigation.back", 49, "ARROW", "<white>Вернуться</white>", List.of()),
                Map.of(), backAction);
        if (safePage < maxPage) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("navigation.next", 53, "ARROW", "<white>Страница вперед</white>", List.of()),
                    Map.of(), event -> openTickets(player, tickets, menuName, safePage + 1, backAction));
        }
        player.openInventory(inventory);
    }
}
