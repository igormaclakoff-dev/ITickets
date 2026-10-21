package com.auracode.itickets.gui;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.model.Ticket;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

final class TicketViewMenu {
    private final GuiService gui;

    TicketViewMenu(GuiService gui) {
        this.gui = gui;
    }

    void openTicketDetails(Player player, Ticket ticket) {
        if (!gui.ticketService.canView((CommandSender) player, ticket)) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Map<String, String> placeholders = gui.ticketService.placeholders(ticket);
        Inventory inventory = gui.createInventory(holder, "view", placeholders);
        gui.fillBackground(inventory);
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("view.info", 11, "BOOK", "<white>Информация</white>",
                        List.of("<gray>Тикет: <white>#%id%</white></gray>",
                                "<gray>Игрок: <white>%player%</white></gray>",
                                "<gray>Категория: <white>%category%</white></gray>",
                                "<gray>Важность: <yellow>%priority%</yellow></gray>",
                                "<gray>Создан: <white>%created%</white></gray>",
                                "<gray>Место: <white>%location%</white></gray>",
                                "<gray>Ответственный: <white>%assigned%</white></gray>",
                                "<gray>Оценка: %rating%</gray>")),
                placeholders, event -> {
                });
        gui.placeClickableItem(inventory, holder,
                new GuiItemConfig(13, "WRITTEN_BOOK", "<white>Сообщения</white>",
                        gui.text.messageLore(ticket, gui.messages)),
                Map.of(), event -> {
                });
        if (gui.ticketService.isStaff((CommandSender) player, ticket)) {
            gui.placeClickableItem(inventory, holder,
                    new GuiItemConfig(14, "PAPER", "<aqua>Заметки персонала</aqua>",
                            gui.text.noteLore(ticket, gui.messages)),
                    Map.of(), event -> {
                    });
        }
        if (player.hasPermission("itickets.assign") && ticket.status().canBeAssigned()) {
            boolean mine = player.getUniqueId().equals(ticket.assignedToUuid());
            if (mine) {
                gui.placeClickableItem(inventory, holder,
                        gui.guiConfig.item("view.taken", 15, "PLAYER_HEAD", "<green>В работе у тебя</green>",
                                List.of("<dark_gray>Ты ответственный за этот тикет.</dark_gray>")),
                        placeholders, event -> {
                        });
            } else {
                gui.placeClickableItem(inventory, holder,
                        gui.guiConfig.item("view.take", 15, "PLAYER_HEAD", "<yellow>Взять в работу</yellow>",
                                List.of("<dark_gray>Назначить тикет на себя.</dark_gray>",
                                        "<gray>Сейчас: <white>%assigned%</white></gray>")),
                        placeholders, event -> {
                            gui.ticketService.takeTicket((CommandSender) player, ticket);
                            gui.showTicketDetails(player, ticket);
                        });
            }
        }
        if (gui.ticketService.canComment((CommandSender) player, ticket)) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.comment", 28, "FEATHER", "<green>Ответить</green>",
                            List.of("<gray>Добавить сообщение в тикет.</gray>")),
                    placeholders, event -> {
                        gui.chatInputs.awaitComment(player, ticket.id());
                        player.closeInventory();
                        gui.messages.send((CommandSender) player, "input.comment", placeholders);
                    });
        }
        if (gui.ticketService.isStaff((CommandSender) player, ticket) && !gui.templates.all().isEmpty()
                && ticket.status().canReceiveComments()) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.templates", 29, "KNOWLEDGE_BOOK", "<yellow>Шаблоны</yellow>",
                            List.of("<gray>Быстрые заготовки ответов.</gray>")),
                    placeholders, event -> gui.showTemplates(player, ticket));
        }
        if (gui.ticketService.isStaff((CommandSender) player, ticket)) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.note", 33, "PAPER", "<aqua>Добавить заметку</aqua>",
                            List.of("<dark_gray>Скрытая от игрока запись.</dark_gray>")),
                    placeholders, event -> {
                        gui.chatInputs.awaitNote(player, ticket.id());
                        player.closeInventory();
                        gui.messages.send((CommandSender) player, "input.note", placeholders);
                    });
        }
        if (player.hasPermission("itickets.manage") && ticket.status().isActive()) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.priority", 20, "COMPARATOR", "<yellow>Важность: %priority_colored%",
                            List.of("<gray>Нажми, чтобы изменить важность 1-5.</gray>")),
                    placeholders, event -> {
                        int next = ticket.priority() >= 5 ? 1 : ticket.priority() + 1;
                        gui.ticketService.updatePriority((CommandSender) player, ticket, next);
                        gui.showTicketDetails(player, ticket);
                    });
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.category", 22, "HOPPER", "<yellow>Категория: %category%",
                            List.of("<gray>Нажми, чтобы переключить категорию.</gray>")),
                    placeholders, event -> {
                        var all = gui.categories.all();
                        int current = 0;
                        for (int index = 0; index < all.size(); index++) {
                            if (all.get(index).id().equalsIgnoreCase(ticket.categoryId())) {
                                current = index;
                                break;
                            }
                        }
                        var nextCategory = all.get((current + 1) % all.size());
                        gui.ticketService.updateCategory((CommandSender) player, ticket, nextCategory.id());
                        gui.showTicketDetails(player, ticket);
                    });
        }
        if (ticket.status().canBeClosed() && gui.ticketService.canClose((CommandSender) player, ticket)) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.close", 30, "REDSTONE", "<red>Закрыть</red>",
                            List.of("<gray>Выбрать причину закрытия.</gray>")),
                    placeholders, event -> gui.showCloseReasonPicker(player, ticket));
        }
        if (ticket.status().canBeReopened() && player.hasPermission("itickets.reopen")) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.reopen", 30, "LIME_DYE", "<green>Переоткрыть</green>",
                            List.of("<gray>Вернуть тикет в работу.</gray>")),
                    placeholders, event -> {
                        gui.ticketService.reopen((CommandSender) player, ticket);
                        gui.showTicketDetails(player, ticket);
                    });
        }
        if (player.hasPermission("itickets.teleport") && ticket.location() != null) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.teleport", 32, "ENDER_PEARL", "<yellow>К месту</yellow>",
                            List.of("<white>%location%</white>")),
                    placeholders, event -> {
                        Location location = ticket.location().toBukkitLocation();
                        if (location == null) {
                            gui.messages.send((CommandSender) player, "errors.world-not-found");
                            return;
                        }
                        player.teleport(location);
                        gui.messages.send((CommandSender) player, "ticket.teleported", placeholders);
                    });
        }
        if (player.hasPermission("itickets.delete") && ticket.status().canBeSoftDeleted()) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.delete", 34, "BARRIER", "<red>Удалить</red>",
                            List.of("<dark_gray>Скрыть тикет из обычных списков.</dark_gray>")),
                    placeholders, event -> {
                        gui.ticketService.delete((CommandSender) player, ticket);
                        openReturnMenu(player);
                    });
        }
        if (player.hasPermission("itickets.purge") && ticket.status().canBePurged()) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("view.purge", 34, "LAVA_BUCKET", "<red>Удалить навсегда</red>",
                            List.of("<dark_gray>Окончательно стереть запись.</dark_gray>")),
                    placeholders, event -> {
                        gui.ticketService.purge((CommandSender) player, ticket);
                        openReturnMenu(player);
                    });
        }
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("navigation.back", 49, "ARROW", "<white>Вернуться</white>", List.of()),
                Map.of(), event -> openReturnMenu(player));
        player.openInventory(inventory);
    }

    private void openReturnMenu(Player player) {
        if (player.hasPermission("itickets.staff") || player.hasPermission("itickets.staff.gui")) {
            gui.showStaffDashboard(player);
        } else {
            gui.showPlayerHome(player);
        }
    }
}
