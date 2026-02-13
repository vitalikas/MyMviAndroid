package lt.vitalijus.mymviandroid.feature_stock.domain.websocket

/**
 * Abstraction for WebSocket price streaming clients.
 *
 * Enables:
 * - Multiple exchange implementations (Binance, Coinbase, Kraken)
 * - Easy testing with fake implementations
 * - Swap implementations without changing repository code
 */
interface WebSocketClient {

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

/**
 * Callback for price updates from WebSocket.
 */
fun interface PriceUpdateListener {
    fun onPriceUpdate(
        symbol: String,
        price: Double,
        percentChange: Double
    )
}
