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

        // Init API client
        val apiUrl = config.getString("api.url") ?: "http://localhost:3001"
        val apiKey = config.getString("api.apiKey") ?: ""
        apiClient = ApiClient(apiUrl, apiKey, logger)

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

        if (apiKey.isEmpty()) {
            logger.warning("API key is not configured! Set api.apiKey in config.yml")
            logger.warning("Use /fd test to verify connection after configuring")
        } else {
            logger.info("FreshDonate plugin enabled. Polling $apiUrl every ${config.getLong("api.pollInterval", 10)}s")
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
        val apiKey = config.getString("api.apiKey") ?: ""
        val pollInterval = config.getLong("api.pollInterval", 10)

        apiClient.updateConfig(apiUrl, apiKey)
        deliveryManager.start(pollInterval)
    }
}
