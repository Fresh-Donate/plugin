package ru.zaralx.freshDonatePlugin.delivery

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.zaralx.freshDonatePlugin.api.ApiClient
import ru.zaralx.freshDonatePlugin.api.CommandLog
import ru.zaralx.freshDonatePlugin.api.DeliveryResult
import ru.zaralx.freshDonatePlugin.api.PendingDelivery
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class DeliveryManager(
    private val plugin: JavaPlugin,
    private val apiClient: ApiClient
) {
    private val inFlight = ConcurrentHashMap.newKeySet<String>()

    private val unreported = ConcurrentHashMap<String, DeliveryResult>()

    private val polling = AtomicBoolean(false)

    private var pollTaskId = -1

    val inFlightCount: Int
        get() = inFlight.size

    val unreportedCount: Int
        get() = unreported.size

    fun start(pollIntervalSeconds: Long) {
        stop()
        val intervalTicks = pollIntervalSeconds * 20L

        pollTaskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            pollNow()
        }, 20L, intervalTicks).taskId

        plugin.logger.info("Delivery manager started (poll every ${pollIntervalSeconds}s)")
    }

    fun stop() {
        if (pollTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pollTaskId)
            pollTaskId = -1
        }
    }

    fun onPlayerJoin() {
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            pollNow()
        }, 40L)
    }

    /**
     * Force re-poll now (triggered by admin command)
     */
    fun pollNow() {
        if (!polling.compareAndSet(false, true)) return
        val online = Bukkit.getOnlinePlayers().map { it.name }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                retryReports()
                schedule(apiClient.fetchPending(online))
            } finally {
                polling.set(false)
            }
        })
    }

    private fun schedule(deliveries: List<PendingDelivery>) {
        for (delivery in deliveries) {
            if (unreported.containsKey(delivery.paymentId)) continue
            if (!inFlight.add(delivery.paymentId)) continue

            Bukkit.getScheduler().runTask(plugin, Runnable {
                execute(delivery)
            })
        }
    }

    private fun execute(delivery: PendingDelivery) {
        if (delivery.requireOnline && Bukkit.getPlayerExact(delivery.playerNickname) == null) {
            inFlight.remove(delivery.paymentId)
            return
        }

        val logs = mutableListOf<CommandLog>()
        var allSuccess = true

        for (commandTemplate in delivery.commands) {
            val command = resolveVariables(commandTemplate, delivery.playerNickname)

            try {
                val success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                logs.add(CommandLog(command = command, success = success, response = if (success) "OK" else "Command returned false"))
                if (!success) allSuccess = false
            } catch (e: Exception) {
                logs.add(CommandLog(command = command, success = false, response = e.message ?: "Unknown error"))
                allSuccess = false
            }
        }

        if (allSuccess) {
            plugin.logger.info("Delivered ${delivery.productName} to ${delivery.playerNickname} (${delivery.commands.size} commands)")
        } else {
            plugin.logger.warning("Delivery failed for ${delivery.playerNickname}: ${delivery.productName}")
        }

        val result = DeliveryResult(success = allSuccess, logs = logs)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            report(delivery.paymentId, result)
        })
    }

    private fun report(paymentId: String, result: DeliveryResult) {
        if (apiClient.reportResult(paymentId, result)) {
            unreported.remove(paymentId)
        } else {
            unreported[paymentId] = result
            plugin.logger.warning("Failed to report delivery result for payment $paymentId, will retry")
        }
        inFlight.remove(paymentId)
    }

    private fun retryReports() {
        for ((paymentId, result) in unreported) {
            if (apiClient.reportResult(paymentId, result)) {
                unreported.remove(paymentId)
            }
        }
    }

    private fun resolveVariables(command: String, playerName: String): String {
        return command
            .replace("{player}", playerName)
            .replace("{PLAYER}", playerName)
            .replace("{nickname}", playerName)
    }
}
