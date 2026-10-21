package com.auracode.itickets.gui;

import com.auracode.itickets.config.CloseReasonConfig;
import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.model.Ticket;
import com.auracode.itickets.model.TicketStatus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

final class CloseReasonMenu {
    private final GuiService gui;

    CloseReasonMenu(GuiService gui) {
        this.gui = gui;
    }

    void openReasonPicker(Player player, Ticket ticket) {
        if (!gui.ticketService.canClose((CommandSender) player, ticket)) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        if (ticket.status() == TicketStatus.DELETED) {
            gui.messages.send((CommandSender) player, "errors.ticket-deleted");
            return;
        }
        if (!ticket.status().canBeClosed()) {
            gui.messages.send((CommandSender) player, "errors.ticket-not-open");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Map<String, String> placeholders = gui.ticketService.placeholders(ticket);
        Inventory inventory = gui.createInventory(holder, "close_reasons", placeholders);
        gui.fillBackground(inventory);
        for (CloseReasonConfig option : gui.guiConfig.closeReasons(defaultReasons())) {
            gui.placeClickableItem(inventory, holder, option.item(), placeholders, event -> {
                if (option.custom()) {
                    gui.chatInputs.awaitCloseReason(player, ticket.id());
                    player.closeInventory();
                    gui.messages.send((CommandSender) player, "input.close", placeholders);
                    return;
                }
                gui.ticketService.close((CommandSender) player, ticket, option.reason());
                gui.showTicketDetails(player, ticket);
            });
        }
        gui.placeClickableItem(inventory, holder,
                gui.itemAt(
                        gui.guiConfig.item("navigation.back", 26, "ARROW", "<white>Вернуться</white>", List.of()),
                        26),
                Map.of(), event -> gui.showTicketDetails(player, ticket));
        player.openInventory(inventory);
    }

    private List<CloseReasonConfig> defaultReasons() {
        return List.of(
                reason("resolved", 10, "LIME_WOOL", "<green>Проблема решена</green>", "Проблема решена",
                        List.of("<gray>Вопрос закрыт, решение найдено.</gray>"), false),
                reason("no_info", 11, "YELLOW_WOOL", "<yellow>Недостаточно информации</yellow>",
                        "Недостаточно информации",
                        List.of("<gray>Не хватает деталей для проверки.</gray>"), false),
                reason("duplicate", 12, "PAPER", "<gray>Дубликат обращения</gray>", "Дубликат обращения",
                        List.of("<dark_gray>Такая заявка уже есть.</dark_gray>"), false),
                reason("not_confirmed", 13, "LIGHT_GRAY_WOOL", "<gray>Нарушение не подтверждено</gray>",
                        "Нарушение не подтверждено",
                        List.of("<dark_gray>Доказательств недостаточно.</dark_gray>"), false),
                reason("not_server", 14, "COMPASS", "<yellow>Не относится к серверу</yellow>",
                        "Не относится к серверу",
                        List.of("<dark_gray>Проблема вне зоны поддержки.</dark_gray>"), false),
                reason("cancelled", 15, "GRAY_DYE", "<gray>Отменено игроком</gray>", "Отменено игроком",
                        List.of("<dark_gray>Автор отказался от обращения.</dark_gray>"), false),
                reason("custom", 16, "FEATHER", "<white>Своя причина</white>", "",
                        List.of("<gray>Написать причину вручную в чат.</gray>"), true));
    }

    private CloseReasonConfig reason(String key, int slot, String material, String name, String reason,
            List<String> lore, boolean custom) {
        return new CloseReasonConfig(key, new GuiItemConfig(slot, material, name, lore), reason, custom);
    }
}
