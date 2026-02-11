package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

sealed interface StockAction {
    data object ScreenEntered : StockAction
    data object PulledToRefresh : StockAction
    data class FavoriteClicked(val id: String) : StockAction
    data object RetryClicked : StockAction
}
