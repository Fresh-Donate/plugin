package ru.zaralx.freshDonatePlugin.delivery

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import ru.zaralx.freshDonatePlugin.api.ApiClient
import ru.zaralx.freshDonatePlugin.api.CommandLog
import ru.zaralx.freshDonatePlugin.api.DeliveryResult
import ru.zaralx.freshDonatePlugin.api.PendingDelivery
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class DeliveryManager(
    private val plugin: JavaPlugin,
    private val apiClient: ApiClient
) {
    // Deliveries waiting for player to come online
    private val offlineQueue = ConcurrentHashMap<String, ConcurrentLinkedQueue<PendingDelivery>>()

    // Track payments we've already seen to avoid double-processing
    private val processedIds = ConcurrentHashMap.newKeySet<String>()

    private var pollTaskId = -1

    val pendingOfflineCount: Int
        get() = offlineQueue.values.sumOf { it.size }

    val queuedPlayers: Set<String>
        get() = offlineQueue.keys.toSet()

    fun start(pollIntervalSeconds: Long) {
        stop()
        val intervalTicks = pollIntervalSeconds * 20L

        pollTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            pollAndDeliver()
        }, 20L, intervalTicks).taskId

        plugin.logger.info("Delivery manager started (poll every ${pollIntervalSeconds}s)")
    }

    fun stop() {
        if (pollTaskId != -1) {
            Bukkit.getScheduler().cancelTask(pollTaskId)
            pollTaskId = -1
        }
    }

    /**
     * Called when a player joins — process their queued deliveries
     */
    fun onPlayerJoin(playerName: String) {
        val queue = offlineQueue.remove(playerName.lowercase()) ?: return
        if (queue.isEmpty()) return

        plugin.logger.info("Player $playerName joined, delivering ${queue.size} queued item(s)")

        // Small delay to let the player fully load in
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            while (queue.isNotEmpty()) {
                val delivery = queue.poll() ?: break
                executeDelivery(delivery)
            }
        }, 40L) // 2 second delay
    }

    /**
     * Force re-poll now (triggered by admin command)
     */
    fun pollNow() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            pollAndDeliver()
        })
    }

    private fun pollAndDeliver() {
        val deliveries = apiClient.fetchPending()
        if (deliveries.isEmpty()) return

        for (delivery in deliveries) {
            // Skip already processed
            if (!processedIds.add(delivery.paymentId)) continue

            val player = Bukkit.getPlayerExact(delivery.playerNickname)

            if (player != null && player.isOnline) {
                // Player is online — deliver on main thread
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    executeDelivery(delivery)
                })
            } else if (delivery.requireOnline) {
                // Player offline, requires online — queue for later
                val queue = offlineQueue.computeIfAbsent(delivery.playerNickname.lowercase()) {
                    ConcurrentLinkedQueue()
                }
                queue.add(delivery)
                plugin.logger.info("Queued delivery for offline player ${delivery.playerNickname}: ${delivery.productName}")
            } else {
                // Player offline but doesn't require online — execute anyway
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    executeDelivery(delivery)
                })
            }
        }
    }

    private fun executeDelivery(delivery: PendingDelivery) {
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

        val result = DeliveryResult(success = allSuccess, logs = logs)

        if (allSuccess) {
            plugin.logger.info("Delivered ${delivery.productName} to ${delivery.playerNickname} (${delivery.commands.size} commands)")
        } else {
            plugin.logger.warning("Delivery failed for ${delivery.playerNickname}: ${delivery.productName}")
        }

        // Report result async
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val reported = apiClient.reportResult(delivery.paymentId, result)
            if (!reported) {
                plugin.logger.warning("Failed to report delivery result for payment ${delivery.paymentId}")
            }
            // If report failed, allow re-processing on next poll
            if (!reported && !allSuccess) {
                processedIds.remove(delivery.paymentId)
            }
        })
    }

    private fun resolveVariables(command: String, playerName: String): String {
        return command
            .replace("{player}", playerName)
            .replace("{PLAYER}", playerName)
            .replace("{nickname}", playerName)
    }
}
