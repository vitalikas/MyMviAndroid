package lt.vitalijus.mymviandroid.feature_stock.presentation.state

import lt.vitalijus.mymviandroid.feature_stock.presentation.model.StockUi

data class StockState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val stocks: List<StockUi> = emptyList(),
    val error: String? = null,
    val isMarketOpen: Boolean = true
)
