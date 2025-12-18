package site.dmbi.analytics

import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONArray
import site.dmbi.analytics.models.AnalyticsEvent
import site.dmbi.analytics.util.SignatureHelper
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Handles batching and sending events to the server
 */
internal class NetworkQueue(
    private val endpoint: String,
    private val batchSize: Int,
    private val flushInterval: Long,
    private val maxRetryCount: Int,
    private val debugLogging: Boolean
) {
    private val eventQueue = CopyOnWriteArrayList<AnalyticsEvent>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var flushJob: Job? = null
    private var offlineStore: OfflineStore? = null

    init {
        startFlushTimer()
    }

    fun setOfflineStore(store: OfflineStore) {
        offlineStore = store
    }

    /** Add event to queue */
    fun enqueue(event: AnalyticsEvent) {
        eventQueue.add(event)

        if (eventQueue.size >= batchSize) {
            flush()
        }
    }

    /** Flush all queued events */
    fun flush() {
        scope.launch {
            val eventsToSend = eventQueue.toList()
            eventQueue.clear()

            if (eventsToSend.isNotEmpty()) {
                sendEvents(eventsToSend)
            }
        }
    }

    /** Retry sending offline events */
    fun retryOfflineEvents() {
        scope.launch {
            val storedEvents = offlineStore?.fetchPendingEvents() ?: return@launch
            if (storedEvents.isEmpty()) return@launch

            if (debugLogging) {
                Log.d(TAG, "Retrying ${storedEvents.size} offline events")
            }

            val events = storedEvents.map { it.event }
            val ids = storedEvents.map { it.id }

            val success = sendEventsSync(events)

            if (success) {
                offlineStore?.delete(ids)
            } else {
                ids.forEach { id ->
                    offlineStore?.incrementRetry(id, maxRetryCount)
                }
            }
        }
    }

    private suspend fun sendEvents(events: List<AnalyticsEvent>) {
        if (events.isEmpty()) return

        val success = sendEventsSync(events)

        if (!success) {
            // Store events offline for retry
            events.forEach { event ->
                offlineStore?.store(event)
            }
        }
    }

    private fun sendEventsSync(events: List<AnalyticsEvent>): Boolean {
        return try {
            val jsonArray = JSONArray()
            events.forEach { jsonArray.put(it.toJson()) }
            val payload = jsonArray.toString()

            // Generate signature for request authentication
            val timestamp = System.currentTimeMillis()
            val signature = SignatureHelper.sign(timestamp, payload)

            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                // Add security headers
                if (signature.isNotEmpty()) {
                    setRequestProperty("X-Timestamp", timestamp.toString())
                    setRequestProperty("X-Signature", signature)
                }
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 30_000
            }

            if (debugLogging) {
                Log.d(TAG, "Sending ${events.size} events to $endpoint (signed: ${signature.isNotEmpty()})")
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload)
                writer.flush()
            }

            val responseCode = connection.responseCode
            connection.disconnect()

            if (responseCode == 202) {
                if (debugLogging) {
                    Log.d(TAG, "Successfully sent ${events.size} events")
                }
                true
            } else {
                if (debugLogging) {
                    Log.w(TAG, "Server returned status $responseCode")
                }
                responseCode >= 500 // Only retry on server errors
            }

        } catch (e: Exception) {
            if (debugLogging) {
                Log.e(TAG, "Network error: ${e.message}")
            }
            false
        }
    }

    private fun startFlushTimer() {
        flushJob = scope.launch {
            while (isActive) {
                delay(flushInterval)
                flush()
            }
        }
    }

    fun stop() {
        flushJob?.cancel()
        scope.cancel()
    }

    companion object {
        private const val TAG = "DMBIAnalytics"
    }
}
