package lt.vitalijus.mymviandroid.feature_stock.data.remote.ws

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.websocket.PriceUpdate
import lt.vitalijus.mymviandroid.feature_stock.domain.websocket.WebSocketClient
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray

/**
 * Binance free WebSocket API client implementation.
 *
 * WebSocket endpoint: wss://stream.binance.com:9443/ws/!ticker@arr
 * - Streams ALL symbols 24hr ticker data
 * - Free, no API key required
 * - Updates every ~1 second
 *
 * Uses Flow-based API - emits price updates via [priceUpdates] Flow.
 */
class BinanceWebSocketClient(
    private val client: OkHttpClient,
    private val logger: Logger
) : WebSocketClient {

    companion object {
        const val BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/!ticker@arr"
        const val RECONNECT_DELAY_MS = 5000L
        const val NORMAL_CLOSURE_STATUS = 1000
    }

    // Hot flow for price updates - collectors get real-time data
    private val _priceUpdates = MutableSharedFlow<PriceUpdate>(
        extraBufferCapacity = 100, // Buffer for bursts
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val priceUpdates: Flow<PriceUpdate> = _priceUpdates.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectJob: Job? = null

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Connects to Binance WebSocket stream.
     */
    override fun connect() {
        if (isConnected) {
            logger.d(LogCategory.WORKER, BinanceWebSocketClient::class, "Already connected")
            return
        }

        val request = Request.Builder()
            .url(BINANCE_WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                logger.d(
                    LogCategory.WORKER,
                    BinanceWebSocketClient::class,
                    "🔗 Connected to Binance WebSocket"
                )
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseTickerMessage(jsonString = text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                logger.d(
                    LogCategory.WORKER,
                    BinanceWebSocketClient::class,
                    "⚠️ WebSocket closing: $code - $reason"
                )
                webSocket.close(NORMAL_CLOSURE_STATUS, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                logger.d(
                    LogCategory.WORKER,
                    BinanceWebSocketClient::class,
                    "🔌 WebSocket closed: $code - $reason"
                )
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                logger.e(
                    LogCategory.WORKER,
                    BinanceWebSocketClient::class,
                    "❌ WebSocket error: ${t.message}"
                )
                scheduleReconnect()
            }
        })
    }

    /**
     * Disconnects from WebSocket.
     */
    override fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(NORMAL_CLOSURE_STATUS, "User disconnected")
        webSocket = null
        isConnected = false
        logger.d(LogCategory.WORKER, BinanceWebSocketClient::class, "👋 Disconnected")
    }

    /**
     * Checks if currently connected.
     */
    override fun isConnected(): Boolean = isConnected

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (isActive) {
                logger.d(
                    LogCategory.WORKER,
                    BinanceWebSocketClient::class,
                    "🔄 Attempting to reconnect..."
                )
                connect()
            }
        }
    }

    private fun parseTickerMessage(jsonString: String) {
        try {
            val tickerArray = JSONArray(jsonString)

            for (i in 0 until tickerArray.length()) {
                val ticker = tickerArray.getJSONObject(i)

                val symbol = ticker.getString("s")
                val lastPrice = ticker.getString("c").toDoubleOrNull() ?: continue
                val priceChangePercent = ticker.getString("p").toDoubleOrNull() ?: 0.0

                val stockSymbol = mapBinanceSymbol(symbol) ?: continue

                // Emit to Flow instead of calling listener
                _priceUpdates.tryEmit(
                    PriceUpdate(
                        symbol = stockSymbol,
                        price = lastPrice,
                        percentChange = priceChangePercent
                    )
                )
            }
        } catch (e: Exception) {
            logger.e(
                LogCategory.WORKER,
                BinanceWebSocketClient::class,
                "Failed to parse message: ${e.message}"
            )
        }
    }

    /**
     * Maps Binance symbol to our stock symbol format.
     */
    private fun mapBinanceSymbol(binSymbol: String): String? {
        val baseAsset = when {
            binSymbol.endsWith("USDT") -> binSymbol.removeSuffix("USDT")
            binSymbol.endsWith("BUSD") -> binSymbol.removeSuffix("BUSD")
            binSymbol.endsWith("USDC") -> binSymbol.removeSuffix("USDC")
            else -> binSymbol
        }

        return when (baseAsset) {
            "BTC" -> "BTC"
            "ETH" -> "ETH"
            "BNB" -> "BNB"
            "SOL" -> "SOL"
            "ADA" -> "ADA"
            "DOT" -> "DOT"
            "MATIC" -> "MATIC"
            "AVAX" -> "AVAX"
            "LINK" -> "LINK"
            "UNI" -> "UNI"
            "ATOM" -> "ATOM"
            "ETC" -> "ETC"
            "XLM" -> "XLM"
            "ALGO" -> "ALGO"
            "NEAR" -> "NEAR"
            "FIL" -> "FIL"
            "APE" -> "APE"
            "SAND" -> "SAND"
            "MANA" -> "MANA"
            "AXS" -> "AXS"
            else -> null
        }
    }
}
