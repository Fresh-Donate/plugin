package ru.zaralx.freshDonatePlugin.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Logger

class ApiClient(
    private var baseUrl: String,
    private var apiKey: String,
    private var serverId: String,
    private val pluginVersion: String,
    private val logger: Logger
) {
    private val gson = Gson()
    private val clientHeader = "plugin/$pluginVersion"

    fun updateConfig(baseUrl: String, apiKey: String, serverId: String) {
        this.baseUrl = baseUrl.trimEnd('/')
        this.apiKey = apiKey
        this.serverId = serverId
    }

    /**
     * Fetch pending deliveries from the backend
     */
    fun fetchPending(): List<PendingDelivery> {
        return try {
            val response = get("/plugin/deliveries/pending")
            val type = object : TypeToken<List<PendingDelivery>>() {}.type
            gson.fromJson(response, type) ?: emptyList()
        } catch (e: Exception) {
            logger.warning("Failed to fetch pending deliveries: ${e.message}")
            emptyList()
        }
    }

    /**
     * Report delivery result back to the backend
     */
    fun reportResult(paymentId: String, result: DeliveryResult): Boolean {
        return try {
            val body = gson.toJson(result)
            post("/plugin/deliveries/$paymentId/result", body)
            true
        } catch (e: Exception) {
            logger.warning("Failed to report delivery result for $paymentId: ${e.message}")
            false
        }
    }

    /**
     * Ping the backend to verify connection and API key
     */
    fun ping(): Boolean {
        return try {
            val response = get("/plugin/ping")
            response.contains("ok")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ping the backend and parse the response. In multi-server mode the
     * backend echoes back the server id we authenticated as - useful for
     * /fd status to confirm which server this plugin is bound to.
     */
    fun pingDetailed(): PingResult {
        return try {
            val response = get("/plugin/ping")
            val parsed = gson.fromJson(response, PingResponse::class.java)
            PingResult(ok = parsed?.status == "ok", serverId = parsed?.serverId)
        } catch (e: Exception) {
            PingResult(ok = false, serverId = null, error = e.message)
        }
    }

    private fun get(path: String): String {
        val url = URL("$baseUrl$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("X-Api-Key", apiKey)
        if (serverId.isNotEmpty()) {
            conn.setRequestProperty("X-Server-Id", serverId)
        }
        conn.setRequestProperty("X-FD-Client", clientHeader)
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val code = conn.responseCode
        if (code != 200) {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            } catch (_: Exception) { "" }
            throw ApiException("GET $path returned $code: $errorBody")
        }

        return conn.inputStream.bufferedReader().readText()
    }

    private fun post(path: String, body: String): String {
        val url = URL("$baseUrl$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("X-Api-Key", apiKey)
        if (serverId.isNotEmpty()) {
            conn.setRequestProperty("X-Server-Id", serverId)
        }
        conn.setRequestProperty("X-FD-Client", clientHeader)
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.doOutput = true

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body) }

        val code = conn.responseCode
        if (code !in 200..299) {
            val errorBody = try {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            } catch (_: Exception) { "" }
            throw ApiException("POST $path returned $code: $errorBody")
        }

        return conn.inputStream.bufferedReader().readText()
    }
}

class ApiException(message: String) : Exception(message)
