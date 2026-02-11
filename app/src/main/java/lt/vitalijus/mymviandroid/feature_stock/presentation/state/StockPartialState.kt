package lt.vitalijus.mymviandroid.feature_stock.presentation.state

import lt.vitalijus.mymviandroid.feature_stock.domain.event.StockPriceChangeEvent
import lt.vitalijus.mymviandroid.feature_stock.presentation.model.StockUi

sealed interface StockPartialState {
    data object Loading : StockPartialState
    data class DataLoaded(val stocks: List<StockUi>) : StockPartialState
    data class Error(val message: String) : StockPartialState
    data object RefreshStarted : StockPartialState
    data object RefreshCompleted : StockPartialState
    data class MarketStateChanged(val isOpen: Boolean) : StockPartialState
    data class PriceChanged(val event: StockPriceChangeEvent) : StockPartialState
    data class ClearPriceBlink(val stockId: String) : StockPartialState
}
