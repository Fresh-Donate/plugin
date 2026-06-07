package ru.zaralx.freshDonatePlugin.command

import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import ru.zaralx.freshDonatePlugin.FreshDonatePlugin

class MainCommand(
    private val plugin: FreshDonatePlugin
) : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("freshdonate.admin")) {
            sender.sendMessage("${ChatColor.RED}Недостаточно прав.")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "status" -> cmdStatus(sender)
            "reload" -> cmdReload(sender)
            "poll" -> cmdPoll(sender)
            "test" -> cmdTest(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        if (!sender.hasPermission("freshdonate.admin")) return emptyList()
        if (args.size == 1) {
            return listOf("status", "reload", "poll", "test")
                .filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("${ChatColor.GOLD}=== FreshDonate Plugin ===")
        sender.sendMessage("${ChatColor.YELLOW}/fd status ${ChatColor.GRAY}- Статус плагина")
        sender.sendMessage("${ChatColor.YELLOW}/fd reload ${ChatColor.GRAY}- Перезагрузить конфиг")
        sender.sendMessage("${ChatColor.YELLOW}/fd poll ${ChatColor.GRAY}- Опросить API прямо сейчас")
        sender.sendMessage("${ChatColor.YELLOW}/fd test ${ChatColor.GRAY}- Проверить подключение к API")
    }

    private fun cmdStatus(sender: CommandSender) {
        val dm = plugin.deliveryManager
        val apiKey = (plugin.config.getString("api.apiKey") ?: "").trim()
        val serverId = (plugin.config.getString("api.serverId") ?: "").trim()
        sender.sendMessage("${ChatColor.GOLD}=== FreshDonate Status ===")
        sender.sendMessage("${ChatColor.GRAY}API: ${ChatColor.WHITE}${plugin.config.getString("api.url", "не настроен")}")
        if (apiKey.isEmpty()) {
            sender.sendMessage("${ChatColor.GRAY}API key: ${ChatColor.RED}не задан")
        } else {
            sender.sendMessage("${ChatColor.GRAY}API key: ${ChatColor.WHITE}настроен")
        }
        if (serverId.isNotEmpty()) {
            sender.sendMessage("${ChatColor.GRAY}Режим: ${ChatColor.WHITE}мультисервера")
            sender.sendMessage("${ChatColor.GRAY}Server ID: ${ChatColor.WHITE}$serverId")
        } else {
            sender.sendMessage("${ChatColor.GRAY}Режим: ${ChatColor.WHITE}один сервер")
        }
        sender.sendMessage("${ChatColor.GRAY}Интервал опроса: ${ChatColor.WHITE}${plugin.config.getLong("api.pollInterval", 10)}с")
        sender.sendMessage("${ChatColor.GRAY}В очереди (оффлайн): ${ChatColor.WHITE}${dm.pendingOfflineCount}")
        val players = dm.queuedPlayers
        if (players.isNotEmpty()) {
            sender.sendMessage("${ChatColor.GRAY}Ожидают игроков: ${ChatColor.WHITE}${players.joinToString(", ")}")
        }
    }

    private fun cmdReload(sender: CommandSender) {
        plugin.reloadConfig()
        plugin.applyConfig()
        sender.sendMessage("${ChatColor.GREEN}Конфиг перезагружен.")
    }

    private fun cmdPoll(sender: CommandSender) {
        sender.sendMessage("${ChatColor.YELLOW}Опрашиваю API...")
        plugin.deliveryManager.pollNow()
        sender.sendMessage("${ChatColor.GREEN}Запрос отправлен.")
    }

    private fun cmdTest(sender: CommandSender) {
        sender.sendMessage("${ChatColor.YELLOW}Проверяю подключение...")
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val result = plugin.apiClient.pingDetailed()
            plugin.server.scheduler.runTask(plugin, Runnable {
                if (result.ok) {
                    sender.sendMessage("${ChatColor.GREEN}Подключение к API успешно!")
                    if (result.serverId != null) {
                        sender.sendMessage("${ChatColor.GRAY}Авторизованы как сервер: ${ChatColor.WHITE}${result.serverId}")
                    } else {
                        sender.sendMessage("${ChatColor.GRAY}Режим: ${ChatColor.WHITE}один сервер (мультисервера выключены в панели).")
                    }
                } else {
                    sender.sendMessage("${ChatColor.RED}Не удалось подключиться. Проверьте api.url, api.apiKey и при мультисервере api.serverId.")
                    if (result.error != null) {
                        sender.sendMessage("${ChatColor.GRAY}Ошибка: ${ChatColor.WHITE}${result.error}")
                    }
                }
            })
        })
    }
}
