package ru.zaralx.freshDonatePlugin.api

data class PendingDelivery(
    val paymentId: String,
    val playerNickname: String,
    val productName: String,
    val commands: List<String>,
    val requireOnline: Boolean
)

data class DeliveryResult(
    val success: Boolean,
    val logs: List<CommandLog>
)

data class CommandLog(
    val command: String,
    val success: Boolean,
    val response: String
)
