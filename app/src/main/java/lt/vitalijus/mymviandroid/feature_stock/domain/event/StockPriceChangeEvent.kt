package lt.vitalijus.mymviandroid.feature_stock.domain.event

/**
 * Event representing a stock price change.
 * Used with SharedFlow as an event bus for many-to-many communication.
 */
data class StockPriceChangeEvent(
    val stockId: String,
    val oldPrice: Double,
    val newPrice: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the percentage change between old and new price.
     */
    val percentChange: Double =
        if (oldPrice != 0.0) ((newPrice - oldPrice) / oldPrice) * 100 else 0.0

    /**
     * True if price increased, false if decreased or stayed the same.
     */
    val isPriceUp: Boolean = newPrice > oldPrice

    /**
     * True if price decreased.
     */
    val isPriceDown: Boolean = newPrice < oldPrice
}
