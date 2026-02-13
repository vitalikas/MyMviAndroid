package lt.vitalijus.mymviandroid.feature_stock.domain.repository

/**
 * Repository for real-time price updates via WebSocket.
 *
 * Bridges WebSocket price stream to our event bus and database.
 * Batches updates to reduce DB writes and UI refreshes.
 */
interface PriceRepository {
    /**
     * Starts price streaming when market is open.
     * Automatically manages WebSocket lifecycle based on market state.
     */
    fun start()

    /**
     * Stops all price streaming and cleans up resources.
     */
    fun stop()
}
