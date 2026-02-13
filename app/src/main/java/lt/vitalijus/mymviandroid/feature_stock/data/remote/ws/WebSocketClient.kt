package lt.vitalijus.mymviandroid.feature_stock.data.remote.ws

import kotlinx.coroutines.flow.Flow

/**
 * Price update from WebSocket stream.
 */
data class PriceUpdate(
    val symbol: String,
    val price: Double,
    val percentChange: Double
)

/**
 * Abstraction for WebSocket price streaming clients.
 *
 * Uses Flow-based API instead of callbacks - no circular dependencies,
 * easier to test, follows Kotlin coroutines best practices.
 */
interface WebSocketClient {

    /**
     * Hot Flow of price updates from the WebSocket stream.
     * Collectors receive real-time price updates.
     */
    val priceUpdates: Flow<PriceUpdate>

    /**
     * Connects to the WebSocket stream.
     */
    fun connect()

    /**
     * Disconnects from the WebSocket stream.
     */
    fun disconnect()

    /**
     * Checks if currently connected.
     */
    fun isConnected(): Boolean
}
