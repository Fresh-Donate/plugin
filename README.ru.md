# FreshDonate — Плагин для Minecraft

> Плагин для Paper/Spigot, который выдаёт покупки игрокам прямо в игре.

Часть проекта **FreshDonate** — open‑source платформы донатов для Minecraft‑серверов.
См. также: [Backend](https://github.com/Fresh-Donate/backend) · [Магазин](https://github.com/Fresh-Donate/shop) · [Админ‑панель](https://github.com/Fresh-Donate/panel) · [English README](README.md)

---

## О проекте FreshDonate

FreshDonate — самохостимая система приёма донатов для Minecraft‑серверов. Она позволяет продавать привилегии, предметы, валюту и любые другие внутриигровые товары через собственную витрину, принимать оплату через несколько платёжных систем и автоматически доставлять покупки игрокам при следующем заходе на сервер — без комиссий сторонних сервисов и без вендор‑лока.

Платформа разделена на четыре репозитория:

| Репозиторий | Роль |
| --- | --- |
| [fresh-donate-backend](https://github.com/Fresh-Donate/backend) | Fastify API, платежи, вебхуки, очередь доставки |
| [fresh-donate-shop](https://github.com/Fresh-Donate/shop) | Публичная витрина для игроков (Nuxt) |
| [fresh-donate-panel](https://github.com/Fresh-Donate/panel) | Админка для владельца (Nuxt) |
| **fresh-donate-plugin** *(этот)* | Плагин Minecraft, выдающий покупки в игре |

## Роль этого репозитория

Плагин — это внутриигровая часть FreshDonate. Когда игрок заходит на сервер, плагин спрашивает у бекенда, есть ли для этого ника неотправленные доставки, и если есть — выполняет настроенные команды (выдать предмет, повысить группу, любую консольную команду). После этого бекенд помечает покупку как доставленную.

Это позволяет игроку получать донат без рестарта сервера, без ручного ввода команд владельцем, и без того чтобы бекенду приходилось держать постоянное соединение с игровым сервером.

> Если ставить плагин не хочется — бекенд умеет доставлять покупки через **RCON**. Подробности в README бекенда.

## Стек

- **Kotlin 2.0** (JVM target 8)
- **Paper API 1.16.5** при компиляции, используется только Bukkit API — поэтому плагин работает на **Spigot/Paper 1.13.2+** (проверен на 1.21)
- **Gson** для JSON
- **Gradle** с плагином `shadow` (fat JAR, шейдинг Kotlin stdlib и Gson)

## Требования

- Сервер Paper или Spigot (1.13.2+)
- Java 8+ на сервере
- Доступный [fresh-donate-backend](https://github.com/Fresh-Donate/backend)

## Сборка

```bash
./gradlew shadowJar
```

Готовый JAR появится в `build/libs/`. Положи его в папку `plugins/` своего сервера и перезапусти сервер.

## Запуск dev‑сервера

В сборке подключён `run-paper`, так что локальный тестовый сервер поднимается одной командой:

```bash
./gradlew runServer
```

## Конфигурация

После первого запуска в `plugins/FreshDonatePlugin/` появится конфиг. В минимуме понадобится указать:

- базовый URL бекенда;
- общий секрет / API‑ключ, выданный в админ‑панели, чтобы плагин мог авторизоваться на бекенде.

Полный список настроек смотри в сгенерированном конфиге.

## Структура проекта

```
src/main/
  kotlin/         исходники плагина (listeners, HTTP‑клиент, обработчик доставки)
  resources/      plugin.yml, дефолтный конфиг
build.gradle.kts  сборка Gradle (Kotlin DSL, shadow plugin)
```

## Связанные репозитории

- [fresh-donate-backend](https://github.com/Fresh-Donate/backend) — REST API, за доставками к которому ходит плагин
- [fresh-donate-shop](https://github.com/Fresh-Donate/shop) — публичная витрина
- [fresh-donate-panel](https://github.com/Fresh-Donate/panel) — админ‑панель

## Лицензия

См. [LICENSE](LICENSE).
