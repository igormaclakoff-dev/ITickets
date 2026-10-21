package com.auracode.itickets.gui;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.model.TicketCategory;
import com.auracode.itickets.model.TicketStatus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Map;

final class StaffTicketMenu {
    private final GuiService gui;

    StaffTicketMenu(GuiService gui) {
        this.gui = gui;
    }

    void openDashboard(Player player) {
        if (!player.hasPermission("itickets.staff") && !player.hasPermission("itickets.staff.gui")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "staff", Map.of());
        gui.fillBackground(inventory);
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("staff.open", 10, "LIME_DYE", "<green>Открытые</green>",
                        List.of("<gray>Все тикеты, ожидающие ответа.</gray>")),
                Map.of(), event -> gui.showStaffTickets(player, TicketStatus.OPEN, null, false, 0));
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("staff.in_progress", 11, "CLOCK", "<aqua>В работе</aqua>",
                        List.of("<gray>Тикеты, взятые в работу.</gray>")),
                Map.of(), event -> gui.showStaffTickets(player, TicketStatus.IN_PROGRESS, null, false, 0));
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("staff.closed", 12, "GRAY_DYE", "<gray>Закрытые</gray>",
                        List.of("<dark_gray>Завершенные обращения.</dark_gray>")),
                Map.of(), event -> gui.showStaffTickets(player, TicketStatus.CLOSED, null, false, 0));
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("staff.mine", 13, "NAME_TAG", "<yellow>Назначены мне</yellow>",
                        List.of("<gray>Тикеты, где ты ответственный.</gray>")),
                Map.of(), event -> gui.showStaffTickets(player, null, null, true, 0));
        if (player.hasPermission("itickets.view.deleted")) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("staff.deleted", 14, "BARRIER", "<red>Удаленные</red>",
                            List.of("<dark_gray>Мягко удаленные тикеты.</dark_gray>")),
                    Map.of(), event -> gui.showStaffTickets(player, TicketStatus.DELETED, null, false, 0));
        }
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("staff.profile", 15, "PLAYER_HEAD", "<aqua>Мой профиль</aqua>",
                        List.of("<gray>Твоя статистика и обработанные тикеты.</gray>")),
                Map.of(), event -> gui.showStaffProfile(player, 0));
        int slot = 28;
        for (TicketCategory category : gui.categories.all()) {
            int targetSlot = category.slot() > 26 ? category.slot() : slot++;
            Map<String, String> placeholders = Map.of("category", category.displayName(),
                    "category_id", category.id());
            GuiItemConfig item = new GuiItemConfig(targetSlot, category.material(),
                    category.color() + category.displayName(),
                    List.of("<gray>Открытые тикеты этой категории."), category.amount(), category.glow(),
                    category.customModelData());
            gui.placeClickableItem(inventory, holder, item, placeholders,
                    event -> gui.showStaffTickets(player, TicketStatus.OPEN, category.id(), false, 0));
        }
        gui.placeClickableItem(inventory, holder,
                gui.itemAt(gui.guiConfig.item("navigation.back", 40, "ARROW", "<white>Вернуться</white>", List.of()),
                        40),
                Map.of(), event -> gui.showPlayerHome(player));
        player.openInventory(inventory);
    }
}
