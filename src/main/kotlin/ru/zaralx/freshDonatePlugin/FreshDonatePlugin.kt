package ru.zaralx.freshDonatePlugin

import org.bukkit.plugin.java.JavaPlugin
import ru.zaralx.freshDonatePlugin.api.ApiClient
import ru.zaralx.freshDonatePlugin.command.MainCommand
import ru.zaralx.freshDonatePlugin.delivery.DeliveryManager
import ru.zaralx.freshDonatePlugin.listener.PlayerJoinListener

class FreshDonatePlugin : JavaPlugin() {

    lateinit var apiClient: ApiClient
        private set

    lateinit var deliveryManager: DeliveryManager
        private set

    override fun onEnable() {
        // Save default config if not exists
        saveDefaultConfig()

        val apiUrl = config.getString("api.url") ?: "http://localhost:3001"
        val apiKey = (config.getString("api.apiKey") ?: "").trim()
        val serverId = (config.getString("api.serverId") ?: "").trim()

        apiClient = ApiClient(apiUrl, apiKey, serverId, description.version, logger)

        // Init delivery manager
        deliveryManager = DeliveryManager(this, apiClient)

        // Register listener
        server.pluginManager.registerEvents(PlayerJoinListener(deliveryManager), this)

        // Register command
        val cmd = MainCommand(this)
        getCommand("freshdonate")?.apply {
            setExecutor(cmd)
            setTabCompleter(cmd)
        }

        // Start polling
        applyConfig()

        when {
            apiKey.isEmpty() -> {
                logger.warning("API key is not configured! Set api.apiKey in config.yml (берётся из «Настройки → Общее → Выдача → Токен плагина»).")
                logger.warning("Use /fd test to verify connection after configuring.")
            }
            serverId.isNotEmpty() -> {
                logger.info("FreshDonate enabled (serverId=$serverId). Polling $apiUrl every ${config.getLong("api.pollInterval", 10)}s.")
            }
            else -> {
                logger.info("FreshDonate enabled in single-server mode. Polling $apiUrl every ${config.getLong("api.pollInterval", 10)}s.")
                logger.info("Если в панели включены «Мультисервера», заполните api.serverId в config.yml.")
            }
        }
    }

    override fun onDisable() {
        deliveryManager.stop()
        logger.info("FreshDonate plugin disabled.")
    }

    /**
     * Apply config values (called on enable and reload)
     */
    fun applyConfig() {
        val apiUrl = config.getString("api.url") ?: "http://localhost:3001"
        val apiKey = (config.getString("api.apiKey") ?: "").trim()
        val serverId = (config.getString("api.serverId") ?: "").trim()
        val pollInterval = config.getLong("api.pollInterval", 10)

        apiClient.updateConfig(apiUrl, apiKey, serverId)
        deliveryManager.start(pollInterval)
    }
}
