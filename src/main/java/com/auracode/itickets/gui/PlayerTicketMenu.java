package com.auracode.itickets.gui;

import com.auracode.itickets.config.GuiItemConfig;
import com.auracode.itickets.model.TicketCategory;
import com.auracode.itickets.model.TicketStatus;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class PlayerTicketMenu {
    private final GuiService gui;

    PlayerTicketMenu(GuiService gui) {
        this.gui = gui;
    }

    void openHome(Player player) {
        if (!player.hasPermission("itickets.use")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "main", Map.of());
        gui.fillBackground(inventory);
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("main.create", 11, "WRITABLE_BOOK", "<white>Создать тикет</white>",
                        List.of("<gray>Новое обращение в поддержку.</gray>")),
                Map.of(), event -> gui.showTicketCreation(player, null, 3));
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("main.open", 13, "BOOK", "<white>Мои тикеты</white>",
                        List.of("<gray>Открытые обращения.</gray>")),
                Map.of(), event -> gui.showPlayerTickets(player, TicketStatus.OPEN, 0));
        gui.placeClickableItem(inventory, holder,
                gui.guiConfig.item("main.closed", 15, "CHEST", "<gray>Закрытые</gray>",
                        List.of("<dark_gray>История решенных обращений.</dark_gray>")),
                Map.of(), event -> gui.showPlayerTickets(player, TicketStatus.CLOSED, 0));
        if (player.hasPermission("itickets.staff") || player.hasPermission("itickets.staff.gui")) {
            gui.placeClickableItem(inventory, holder,
                    gui.guiConfig.item("main.staff", 31, "AMETHYST_SHARD", "<yellow>Персонал</yellow>",
                            List.of("<gray>Меню обработки тикетов.</gray>")),
                    Map.of(), event -> gui.showStaffDashboard(player));
        }
        player.openInventory(inventory);
    }

    void openCreateTicket(Player player, String selectedCategoryId, int selectedPriority) {
        if (!player.hasPermission("itickets.create")) {
            gui.messages.send((CommandSender) player, "errors.no-permission");
            return;
        }
        int safePriority = Math.max(1, Math.min(5, selectedPriority));
        TicketCategory selected = selectedCategoryId == null ? gui.categories.first()
                : gui.categories.byId(selectedCategoryId);
        MenuHolder holder = new MenuHolder();
        Inventory inventory = gui.createInventory(holder, "create",
                Map.of("category", selected == null ? "Не выбрана" : selected.displayName(),
                        "priority", String.valueOf(safePriority),
                        "priority_name", gui.text.priorityName(safePriority)));
        gui.fillBackground(inventory);
        gui.placeClickableItem(inventory, holder,
                new GuiItemConfig(4, "NETHER_STAR", "<white>Новое обращение</white>",
                        List.of("<dark_gray>Категория:</dark_gray> "
                                        + (selected == null ? "<red>не выбрана</red>"
                                                : selected.color() + selected.displayName()),
                                "<dark_gray>Важность:</dark_gray> "
                                        + gui.text.priorityColor(safePriority) + gui.text.priorityName(safePriority)
                                        + " <dark_gray>(" + safePriority + ")</dark_gray>",
                                " ",
                                "<gray>Выбери категорию и важность ниже,</gray>",
                                "<gray>затем нажми «Написать обращение».</gray>")),
                Map.of(), event -> {
                });
        for (TicketCategory category : gui.categories.all()) {
            boolean active = selected != null && selected.id().equalsIgnoreCase(category.id());
            ArrayList<String> lore = new ArrayList<>(category.description());
            lore.add(active ? "<green>Выбрано</green>" : "<gray>Нажми, чтобы выбрать.</gray>");
            GuiItemConfig item = new GuiItemConfig(category.slot(), category.material(),
                    category.color() + category.displayName(), lore, category.amount(), category.glow(),
                    category.customModelData());
            gui.placeClickableItem(inventory, holder, item, Map.of(),
                    event -> gui.showTicketCreation(player, category.id(), safePriority));
        }
        int[] prioritySlots = new int[] { 29, 30, 31, 32, 33 };
        int priority = 1;
        while (priority <= 5) {
            boolean active = priority == safePriority;
            GuiItemConfig item = new GuiItemConfig(prioritySlots[priority - 1], gui.text.priorityMaterial(priority),
                    gui.text.priorityColor(priority) + "Важность " + priority + " <dark_gray>·</dark_gray> "
                            + gui.text.priorityColor(priority) + gui.text.priorityName(priority),
                    List.of(active ? "<green>▸ Выбрано</green>" : "<gray>▸ Нажми, чтобы выбрать</gray>",
                            "<dark_gray>Чем выше важность, тем выше приоритет обработки.</dark_gray>"));
            int selectedValue = priority++;
            gui.placeClickableItem(inventory, holder, item, Map.of(),
                    event -> gui.showTicketCreation(player, selected == null ? null : selected.id(), selectedValue));
        }
        boolean ready = selected != null && gui.categories.canCreate(player, selected);
        GuiItemConfig finalizeItem = ready
                ? gui.guiConfig.item("create.finalize", 49, "LIME_CONCRETE", "<green>Написать обращение</green>",
                        List.of("<gray>Нажми и напиши текст в чат.</gray>",
                                "<dark_gray>Для отмены напиши: отмена.</dark_gray>"))
                : new GuiItemConfig(49, "RED_CONCRETE", "<red>Недоступно</red>",
                        List.of("<gray>Нет прав на выбранную категорию.</gray>"));
        gui.placeClickableItem(inventory, holder, finalizeItem, Map.of(), event -> {
            if (!ready) {
                gui.messages.send((CommandSender) player, "errors.category-no-permission");
                return;
            }
            gui.chatInputs.awaitCreate(player, selected.id(), safePriority);
            player.closeInventory();
            gui.messages.send((CommandSender) player, "input.create");
        });
        gui.placeClickableItem(inventory, holder,
                gui.itemAt(gui.guiConfig.item("navigation.back", 45, "ARROW", "<white>Вернуться</white>", List.of()),
                        45),
                Map.of(), event -> gui.showPlayerHome(player));
        player.openInventory(inventory);
    }
}
