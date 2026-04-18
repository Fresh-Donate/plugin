# FreshDonate — Minecraft Plugin

> Paper/Spigot plugin that delivers purchases to players in‑game.

Part of the **FreshDonate** open‑source donation platform for Minecraft servers.
See also: [Backend](https://github.com/Fresh-Donate/backend) · [Shop](https://github.com/Fresh-Donate/shop) · [Admin Panel](https://github.com/Fresh-Donate/panel) · [Russian README](README.ru.md)

---

## About FreshDonate

FreshDonate is a self‑hosted donation system for Minecraft servers. It lets you sell ranks, items, currency and any other in‑game goods through your own storefront, accept payments via multiple providers, and deliver purchases to players automatically the next time they are online — without any third‑party commission or lock‑in.

The platform is split into four repositories:

| Repository | Role |
| --- | --- |
| [fresh-donate-backend](https://github.com/Fresh-Donate/backend) | Fastify API, payments, webhooks, delivery queue |
| [fresh-donate-shop](https://github.com/Fresh-Donate/shop) | Public storefront for players (Nuxt) |
| [fresh-donate-panel](https://github.com/Fresh-Donate/panel) | Admin panel for owners (Nuxt) |
| **fresh-donate-plugin** *(this repo)* | Minecraft plugin that delivers purchases in‑game |

## Role of this repository

The plugin is the in‑game side of FreshDonate. When a player joins the server, the plugin asks the backend whether there are pending deliveries for that nickname, and if so, runs the configured commands for each one (giving an item, promoting the player, running any console command). The backend then marks the purchase as delivered.

This lets players get their donations without restarts, without the owner running commands manually, and without the backend needing to hold an open connection to the server.

> If you don't want to install the plugin, the backend can fall back to **RCON** for delivery — see the backend README.

## Tech stack

- **Kotlin 2.0** (JVM target 8)
- **Paper API 1.16.5** at compile time, uses only Bukkit API so it runs on **Spigot/Paper 1.13.2+** (tested against 1.21)
- **Gson** for JSON
- **Gradle** with `shadow` plugin (fat JAR, shaded Kotlin stdlib and Gson)

## Requirements

- A Paper or Spigot server (1.13.2+)
- Java 8+ on the server
- A reachable [fresh-donate-backend](https://github.com/Fresh-Donate/backend) instance

## Build

```bash
./gradlew shadowJar
```

The fat JAR will appear in `build/libs/`. Drop it into your server's `plugins/` folder and restart the server.

## Run a dev server

The `run-paper` plugin is configured, so you can boot a local test server:

```bash
./gradlew runServer
```

## Configuration

After the first launch, a config file appears in `plugins/FreshDonatePlugin/`. You will need at least:

- the backend base URL;
- a shared secret / API key issued by the admin panel, so the plugin can authenticate to the backend.

See the generated config file in the plugin folder for the full list of options.

## Project structure

```
src/main/
  kotlin/         plugin source (listeners, HTTP client, delivery handler)
  resources/      plugin.yml, default config
build.gradle.kts  Gradle build (Kotlin DSL, shadow plugin)
```

## Related repositories

- [fresh-donate-backend](https://github.com/Fresh-Donate/backend) — REST API this plugin polls for deliveries
- [fresh-donate-shop](https://github.com/Fresh-Donate/shop) — public storefront
- [fresh-donate-panel](https://github.com/Fresh-Donate/panel) — admin panel

## License

See [LICENSE](LICENSE).
