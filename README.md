# QuickTag 1.0.0

[![Java 25](https://img.shields.io/badge/Java-25-orange)](https://docs.papermc.io/paper/getting-started/)
[![Paper 26.2](https://img.shields.io/badge/Paper-26.2-blue)](https://papermc.io/)
[![Release](https://img.shields.io/github/v/release/stasspasjuk-rgb/QuickTag)](https://github.com/stasspasjuk-rgb/QuickTag/releases/latest)

Плагин для **Paper / Purpur 26.2, Java 25**. Дополнительные плагины не нужны.

**[Скачать готовый JAR](https://github.com/stasspasjuk-rgb/QuickTag/releases/latest/download/QuickTag-1.0.0.jar)** ·
[Все релизы](https://github.com/stasspasjuk-rgb/QuickTag/releases) ·
[Сообщить об ошибке](https://github.com/stasspasjuk-rgb/QuickTag/issues)

## Возможности

- Скрытие ников над головами всех игроков.
- Показ настоящего ника в actionbar по Shift + ПКМ по игроку — только нажавшему.
- Настраиваемый текст MiniMessage и перезагрузка через `/quicktag reload`.
- Работа без ProtocolLib и других дополнительных зависимостей.

## Установка

1. Остановите сервер.
2. Скопируйте `QuickTag-1.0.0.jar` в папку `plugins`.
3. Запустите сервер. Конфиг появится в `plugins/QuickTag/config.yml`.

Ники над головами скрыты у всех игроков. Зажмите Shift и нажмите ПКМ по игроку:
его настоящий ник появится в вашем actionbar над панелью быстрого доступа.
Сообщение исчезает со стандартным клиентским затуханием actionbar, примерно через 3 секунды.
TAB и чат плагин не изменяет. Для просмотра ника отдельное право не требуется.

## Команда

`/quicktag reload` — перечитать конфиг и повторно применить скрытие ников.
Право: `quicktag.reload` (по умолчанию у OP). Команда доступна и из консоли.

## Настройка

`actionbar` — текст в формате MiniMessage. `<nickname>` заменяется настоящим ником:

```yaml
actionbar: '<yellow>Ник: <white><nickname>'
messages:
  reloaded: '<green>QuickTag: конфигурация перезагружена.'
  no-permission: '<red>У вас нет права quicktag.reload.'
  usage: '<yellow>Использование: /quicktag reload'
```

После изменения выполните `/quicktag reload`.

## Совместимость

Скрытие использует scoreboard teams. Плагин сохраняет существующее членство в
командах и изменяет только NAME_TAG_VISIBILITY. Игроков без команды добавляет в
свою служебную команду. Проверяет главный scoreboard и scoreboard каждого
онлайн-игрока каждые 10 тиков; новые команды и смена scoreboard обрабатываются
с задержкой до 0,5 секунды при 20 TPS.

При штатном отключении плагин удаляет свои команды и восстанавливает исходную
видимость изменённых команд. После аварийного завершения сервера восстановление
не гарантируется. Плагины TAB/nametag, отправляющие собственные пакеты команд или
постоянно меняющие видимость, могут конфликтовать: отключите их управление nametag.
Другие плагины могут перезаписывать actionbar. Отменённое другим плагином
взаимодействие, скрытый через Bukkit vanish игрок и цель за стеной не раскрывают ник.
Folia, Spigot и модовые серверы не заявлены как поддерживаемые.

## Сборка и проверка

Установите JDK 25 и Maven 3.9+, выполните `mvn clean verify`.
Результат: `target/QuickTag-1.0.0.jar`.

Используется официальный Paper API `26.2.build.135-stable`, зависимости сервера
в JAR не включаются. Автотесты проверяют скрытие на главном и личном scoreboard,
сохранение существующих команд, повторное применение, смену scoreboard, восстановление
при отключении и конфликт имени служебной команды.

Для проверки на своём сервере зайдите двумя игроками: убедитесь, что ники скрыты,
Shift + ПКМ показывает ник только нажавшему, обычный ПКМ его не показывает,
а изменение `actionbar` применяется через `/quicktag reload`.
Проверка с двумя настоящими клиентами при подготовке сборки не выполнялась.

Справка: https://docs.papermc.io/paper/dev/project-setup/
