package com.auracode.itickets.gui;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.model.Ticket;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ProfileMenu {
    private final GuiService gui;

    ProfileMenu(GuiService gui) {
        this.gui = gui;
    }

    void open(Player player, int page) {
        if (!player.hasPermission("itickets.staff") && !player.hasPermission("itickets.staff.gui")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        Map<String, String> stats = gui.ticketService.adminStats(player.getName());
        Map<String, String> personal = gui.ticketService.personalStats(player.getUniqueId());
        HashMap<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("assigned", stats.getOrDefault("assigned", "0"));
        placeholders.put("handled_closed", stats.getOrDefault("closed", "0"));
        placeholders.put("handled_rated", stats.getOrDefault("rated", "0"));
        placeholders.put("avg", stats.getOrDefault("avg", "—"));
        placeholders.put("own_open", personal.getOrDefault("open", "0"));
        placeholders.put("own_closed", personal.getOrDefault("closed", "0"));
        placeholders.put("own_deleted", personal.getOrDefault("deleted", "0"));
        placeholders.put("page", String.valueOf(page + 1));
        placeholders.put("pages", "1");

        List<Ticket> handled = gui.storage.staffTickets(null, null, player.getUniqueId());
        int perPage = Math.min(gui.settings.ticketsPerPage(), gui.guiConfig.ticketSlots().size());
        int maxPage = Math.max(0, (handled.size() - 1) / perPage);
        int safePage = Math.max(0, Math.min(page, maxPage));
        placeholders.put("page", String.valueOf(safePage + 1));
        placeholders.put("pages", String.valueOf(maxPage + 1));

        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "profile", placeholders);
        gui.fillBackground(inventory);
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("profile.head", 4, "PLAYER_HEAD",
                        "<white>%player%</white> <dark_gray>|</dark_gray> <aqua>профиль</aqua>",
                        List.of("<gray>В работе: <white>%assigned%</white></gray>",
                                "<gray>Закрыто: <white>%handled_closed%</white> <dark_gray>|</dark_gray> <gray>оценено: <white>%handled_rated%</white></gray>",
                                "<gray>Средний балл: <yellow>%avg%</yellow></gray>",
                                " ",
                                "<gray>Твои тикеты: <green>%own_open%</green> открыто, <yellow>%own_closed%</yellow> закрыто, <red>%own_deleted%</red> удалено</gray>",
                                "<dark_gray>Ниже — тикеты, где ты ответственный.</dark_gray>")),
                placeholders, event -> {
                });

        List<Integer> slots = gui.guiConfig.ticketSlots();
        int start = safePage * perPage;
        int end = Math.min(handled.size(), start + perPage);
        for (int index = start; index < end; index++) {
            Ticket ticket = handled.get(index);
            int slot = slots.get(index - start);
            HashMap<String, String> itemPlaceholders = new HashMap<>(gui.ticketService.placeholders(ticket));
            itemPlaceholders.put("first_message",
                    gui.text.wrapPlaceholder(ticket.firstMessage() == null ? "" : ticket.firstMessage().text(),
                            "<dark_gray>", 38, gui.messages));
            GuiItemConfig template = gui.guiConfig.item("ticket.item", slot, "PAPER", "<white>Тикет #%id%</white>",
                    List.of("<gray>Игрок: <white>%player%</white></gray>",
                            "<gray>Категория: <white>%category%</white></gray>",
                            "<gray>Важность: %priority_colored%</gray>",
                            "<gray>Статус: %status_colored%</gray>",
                            "<dark_gray>%first_message%</dark_gray>"));
            gui.placeClickableItem(inventory, holder, gui.itemAt(template, slot), itemPlaceholders,
                    event -> gui.showTicketDetails(player, ticket));
        }
        if (safePage > 0) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("navigation.previous", 45, "ARROW", "<white>Страница назад</white>",
                            List.of()),
                    Map.of(), event -> open(player, safePage - 1));
        }
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("navigation.back", 49, "ARROW", "<white>Вернуться</white>", List.of()),
                Map.of(), event -> gui.showStaffDashboard(player));
        if (safePage < maxPage) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("navigation.next", 53, "ARROW", "<white>Страница вперед</white>", List.of()),
                    Map.of(), event -> open(player, safePage + 1));
        }
        player.openInventory(inventory);
    }
}
