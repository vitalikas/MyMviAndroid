package lt.vitalijus.mymviandroid.feature_stock.presentation.model

/**
 * UI model for stock display with price change animation support.
 */
data class StockUi(
    val id: String,
    val name: String,
    val price: Double,
    val isFavorite: Boolean,
    /**
     * Price change direction for blinking animation:
     * - null = no animation
     * - true = price went up (green blink)
     * - false = price went down (red blink)
     */
    val isPriceUp: Boolean? = null
)
