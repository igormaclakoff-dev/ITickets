package com.auracode.itickets.gui;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.config.TemplateRegistry;
import com.auracode.itickets.model.Ticket;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class TemplatesMenu {
    private final GuiService gui;

    TemplatesMenu(GuiService gui) {
        this.gui = gui;
    }

    void open(Player player, Ticket ticket) {
        open(player, ticket, 0);
    }

    void open(Player player, Ticket ticket, int page) {
        if (!gui.ticketService.isStaff((CommandSender) player, ticket)) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        if (!ticket.status().canReceiveComments()) {
            gui.messages.send((CommandSender) player, "errors.ticket-not-open");
            return;
        }
        boolean admin = player.hasPermission("itickets.manage");
        List<TemplateRegistry.TemplateEntry> available = new ArrayList<>();
        for (TemplateRegistry.TemplateEntry entry : gui.templates.all()) {
            if (entry.adminOnly() && !admin) {
                continue;
            }
            available.add(entry);
        }
        if (available.isEmpty()) {
            gui.messages.send((CommandSender) player, "templates.empty");
            return;
        }
        List<Integer> slots = gui.guiConfig.ticketSlots();
        int perPage = Math.max(1, Math.min(slots.size(), gui.settings.ticketsPerPage()));
        int pages = Math.max(1, (available.size() + perPage - 1) / perPage);
        int clamped = Math.min(Math.max(0, page), pages - 1);

        MenuHolder holder = new MenuHolder();
        Map<String, String> placeholders = gui.ticketService.placeholders(ticket);
        placeholders.put("admin", player.getName());
        Inventory inventory = gui.createInventory(holder, "templates", placeholders);
        gui.fillBackground(inventory);

        int start = clamped * perPage;
        int end = Math.min(start + perPage, available.size());
        for (int index = start; index < end; index++) {
            TemplateRegistry.TemplateEntry template = available.get(index);
            int offset = index - start;
            int slot = offset < slots.size() ? slots.get(offset) : offset;
            String resolvedText = gui.ticketService.applyTemplate(ticket, player, template.text());
            List<String> lore = new ArrayList<>();
            lore.add("<dark_gray>Группа:</dark_gray> <gray>" + GuiTextFormatter.clean(template.group()) + "</gray>");
            if (template.adminOnly()) {
                lore.add("<red>Только для администрации</red>");
            }
            lore.add(" ");
            lore.addAll(gui.text.wrapLore(GuiTextFormatter.plainOf(resolvedText), "<gray>",
                    GuiTextFormatter.LORE_WRAP_WIDTH));
            lore.add(" ");
            lore.add("<green>Нажми, чтобы отправить</green>");
            GuiItemConfig item = new GuiItemConfig(slot, template.material(),
                    "<white>" + GuiTextFormatter.clean(
                            gui.ticketService.applyTemplate(ticket, player, template.name())) + "</white>",
                    lore);
            gui.placeClickableItem(inventory, holder, item, Map.of(), event -> {
                gui.ticketService.addComment((CommandSender) player, ticket, resolvedText);
                player.closeInventory();
            });
        }

        if (clamped > 0) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("navigation.previous", 45, "ARROW", "<white>Назад</white>", List.of()),
                    Map.of(), event -> open(player, ticket, clamped - 1));
        }
        if (clamped < pages - 1) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("navigation.next", 53, "ARROW", "<white>Вперед</white>", List.of()),
                    Map.of(), event -> open(player, ticket, clamped + 1));
        }
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("navigation.back", 49, "ARROW", "<white>Вернуться</white>", List.of()),
                Map.of(), event -> gui.showTicketDetails(player, ticket));
        player.openInventory(inventory);
    }
}
