package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

sealed interface StockEffect {
    data object ObserveStocks : StockEffect
    data object RefreshStocks : StockEffect
    data class ToggleFavorite(val id: String) : StockEffect
    data class TrackAnalytics(val event: String) : StockEffect
}