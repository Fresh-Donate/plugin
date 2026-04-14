package ru.zaralx.freshDonatePlugin.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import ru.zaralx.freshDonatePlugin.delivery.DeliveryManager

class PlayerJoinListener(
    private val deliveryManager: DeliveryManager
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        deliveryManager.onPlayerJoin(event.player.name)
    }
}
