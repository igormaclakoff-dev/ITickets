<div align="center">

# 🎫 ITickets

**Система тикетов от студии AuraCode** — открытый плагин для Paper 1.21.x.

GUI-меню для игроков и персонала, шаблоны ответов, внутренние заметки, оценка работы и экспорт базы.

![Версия](https://img.shields.io/badge/version-1.0.0-blue)
![Paper](https://img.shields.io/badge/Paper-1.21.x-white)
![Лицензия](https://img.shields.io/badge/license-MIT-green)

</div>

---

## ✨ Возможности

- **Система тикетов** — создание, ответ, закрытие, переоткрытие, мягкое удаление и оценка работы (1–5).
- **Статусы** — `OPEN`, `IN_PROGRESS`, `CLOSED`, `DELETED`: статус «В работе» ставится автоматически при взятии тикета в работу.
- **7 категорий** — баг, жалоба на игрока, нарушение правил, потеря вещей, донат/покупка, вопрос, другое (`categories.yml`).
- **GUI-меню** — меню игрока, меню персонала с вкладками «Открытые», «В работе», «Назначены мне», профиль со статистикой.
- **Шаблоны ответов** — 29 готовых шаблонов с группировкой, пагинацией и плейсхолдерами (`templates.yml`).
- **Внутренние заметки** — скрытые комментарии персонала, видны только сотрудникам и не отправляются игроку.
- **Управление тикетом** — смена важности и категории прямо из меню просмотра.
- **Автозакрытие и напоминания** — неактивные тикеты закрываются автоматически, персонал получает напоминания об unanswered-тикетах.
- **Ограничения** — задержка между тикетами, лимит открытых тикетов, запрет создания тикетов игроку, обходы через права.
- **Экспорт** — `/iticket export [csv|txt]` в `plugins/ITickets/exports/`.
- **Единый стиль чата** — компактные уведомления с префиксом `[IT]` и поддержкой MiniMessage.
- **Полная настройка** — конфиги, тексты, GUI, категории и шаблоны вынесены в YAML без правки кода.

---

## 🎮 Команды

### Игрок

| Команда | Описание | Право |
|---|---|---|
| `/iticket` | Открыть меню тикетов | `itickets.use` |
| `/iticket create` | Создать новое обращение | `itickets.create` |
| `/iticket list [open\|closed]` | Список тикетов | `itickets.view.own` |
| `/iticket view <id>` | Открыть тикет | `itickets.view.own` |
| `/iticket comment <id> <текст>` | Ответить в тикете | `itickets.comment.own` |
| `/iticket close <id> [причина]` | Закрыть свой тикет | `itickets.close.own` |
| `/iticket rate <id> <1-5>` | Оценить работу администратора | `itickets.view.own` |
| `/iticket cancel` | Отменить ввод текста в чат | `itickets.use` |

**Алиасы:** `/tickets`, `/report`, `/reports`, `/support`, `/it` → `/iticket`; `new` → `create`; `open` → `view`; `reply` → `comment`.

### Персонал

| Команда | Описание | Право |
|---|---|---|
| `/iticket staff` | Меню персонала | `itickets.staff.gui` |
| `/iticket assign <id> <ник>` | Назначить ответственного | `itickets.assign` |
| `/iticket reopen <id>` | Переоткрыть закрытый тикет | `itickets.reopen` |
| `/iticket tp <id>` | Телепорт к месту создания тикета | `itickets.teleport` |
| `/iticket history <ник>` | История тикетов администратора | `itickets.history` |
| `/iticket stats [ник]` | Статистика тикетов | `itickets.stats` |

**Алиасы:** `admin` → `staff`; `teleport` → `tp`.

### Администрация

| Команда | Описание | Право |
|---|---|---|
| `/iticket delete <id>` | Мягко удалить тикет | `itickets.delete` |
| `/iticket purge <id>` | Удалить тикет навсегда | `itickets.purge` |
| `/iticket ban <ник>` | Запретить игроку создавать тикеты | `itickets.ban` |
| `/iticket unban <ник>` | Снять запрет создания тикетов | `itickets.unban` |
| `/iticket reload` | Перезагрузить конфиги | `itickets.reload` |
| `/iticket reset confirm` | Полностью очистить базу тикетов | `itickets.reset` |
| `/iticket export [csv\|txt]` | Выгрузить базу тикетов в файл | `itickets.export` |
| `/iticket help` | Показать список команд | `itickets.use` |

**Алиасы:** `wipe` → `reset`; `?` → `help`.

> Все команды поддерживают **Tab-дополнение** (подкоманды, номера тикетов, ники игроков).

---

## 🔐 Права

| Право | Описание | По умолчанию |
|---|---|---|
| `itickets.use` | Доступ к меню тикетов | все (`true`) |
| `itickets.create` | Создание тикетов | все (`true`) |
| `itickets.view.own` | Просмотр своих тикетов | все (`true`) |
| `itickets.comment.own` | Ответ в своих тикетах | все (`true`) |
| `itickets.close.own` | Закрытие своих тикетов | все (`true`) |
| `itickets.category.<id>.create` | Создание тикета в конкретной категории | все (`true`) |
| `itickets.staff` | Базовые права персонала | `op` |
| `itickets.staff.gui` | Меню персонала | `op` |
| `itickets.view.all` | Просмотр всех открытых тикетов | `op` |
| `itickets.view.closed` | Просмотр закрытых тикетов | `op` |
| `itickets.view.deleted` | Просмотр мягко удалённых тикетов | `op` |
| `itickets.comment.staff` | Ответ в тикетах как персонал | `op` |
| `itickets.assign` | Назначение ответственного | `op` |
| `itickets.manage` | Смена важности и категории | `op` |
| `itickets.close` | Закрытие чужих тикетов | `op` |
| `itickets.reopen` | Переоткрытие тикетов | `op` |
| `itickets.delete` | Мягкое удаление тикетов | `op` |
| `itickets.purge` | Окончательное удаление тикетов | `op` |
| `itickets.teleport` | Телепорт к месту тикета | `op` |
| `itickets.stats` | Глобальная статистика тикетов | `op` |
| `itickets.history` | История тикетов администраторов | `op` |
| `itickets.reload` | Перезагрузка конфигов | `op` |
| `itickets.reset` | Полная очистка базы тикетов | `op` |
| `itickets.export` | Экспорт базы тикетов | `op` |
| `itickets.ban` | Запрет создания тикетов игроку | `op` |
| `itickets.unban` | Снятие запрета создания тикетов | `op` |
| `itickets.bypass.cooldown` | Обход задержки создания тикетов | `op` |
| `itickets.bypass.limit` | Обход лимита открытых тикетов | `op` |
| `itickets.notify.create` | Уведомления о новых тикетах | `op` |
| `itickets.notify.comment` | Уведомления о новых комментариях | `op` |
| `itickets.notify.close` | Уведомления о закрытии тикетов | `op` |
| `itickets.notify.join` | Сводка тикетов при входе на сервер | `op` |
| `itickets.notify.rating` | Уведомления об оценке тикетов | `op` |
| `itickets.admin` | Полный доступ к ITickets | `op` |

`itickets.staff` и `itickets.admin` — групповые права: они раздают дочерние узлы (см. `permissions.yml` в папке плагина).

---

## 📦 Установка

1. Скачайте `ITickets-1.0.0.jar` (см. раздел «Сборка» или Releases).
2. Положите jar в папку `plugins/` вашего Paper-сервера.
3. Перезапустите сервер — плагин создаст `plugins/ITickets/` со всеми конфигами.
4. Выдайте персоналу права `itickets.staff` (или `itickets.admin`).

**Требования:** Paper/Spigot/Purpur 1.21.x, Java 21+.

---

## 🔨 Сборка из исходников

```bash
mvn clean package
```

Готовый jar появится в `target/ITickets-1.0.0.jar`.

---

## ⚙️ Конфигурация

Все файлы создаются в `plugins/ITickets/`:

| Файл | Назначение |
|---|---|
| `config.yml` | Задержки, лимиты, автозакрытие, напоминания, формат дат |
| `gui.yml` | Оформление всех меню (заголовки, слоты, иконки, размеры) |
| `messages.yml` | Все тексты сообщений (MiniMessage) |
| `templates.yml` | Шаблоны быстрых ответов персонала |
| `categories.yml` | Категории тикетов, иконки и права на создание |
| `permissions.yml` | Памятка по правам (player / staff / admin) |

Ключевые настройки `config.yml`:

```yaml
settings:
  cooldown-seconds: 60        # задержка между тикетами от одного игрока
  max-open-tickets: 3         # лимит открытых тикетов
  max-message-length: 500     # максимальная длина текста тикета и комментария
  tickets-per-page: 45        # количество тикетов на странице GUI
  capture-chat-inputs: true   # перехватывать следующее сообщение игрока после клика в GUI
  auto-close:
    enabled: false            # автозакрытие неактивных тикетов
    days: 7
    reason: "Автозакрытие: нет активности"
  reminders:
    enabled: false            # напоминания персоналу о тикетах без ответа
    interval-minutes: 30
    after-minutes: 60
  date-format: "dd.MM.yyyy HH:mm"
  time-zone: "Europe/Moscow"
```

---

## 🗂 Хранение данных

| Путь | Содержимое |
|---|---|
| `plugins/ITickets/tickets/<ник>.yml` | Тикеты игроков |
| `plugins/ITickets/tickets/_meta.yml` | Счётчик идентификаторов тикетов |
| `plugins/ITickets/tickets/backups/` | Резервные копии при перезаписи |
| `plugins/ITickets/exports/` | Выгрузки базы (`csv`, `txt`) |

---

## 🧭 Как это работает

1. Игрок открывает `/iticket` → «Создать тикет» → выбирает категорию → пишет текст в чат (ввод перехватывается плагином).
2. Тикет получает номер и статус `OPEN`, персонал получает уведомление (по праву `itickets.notify.create`).
3. Персонал берёт тикет в работу — статус меняется на `IN_PROGRESS`, ответы пишутся через GUI или шаблоны.
4. Внутренние заметки видны только персоналу и не отправляются игроку.
5. Игрок может закрыть тикет сам или поставить оценку 1–5; персонал закрывает с указанием причины из `gui.yml`.
6. Закрытые тикеты доступны через `/iticket list closed` и в профиле персонала.

---

## 📄 Лицензия

Проект распространяется под лицензией [MIT](LICENSE).

Сделано студией **AuraCode** (IgMacla) с ❤️.
