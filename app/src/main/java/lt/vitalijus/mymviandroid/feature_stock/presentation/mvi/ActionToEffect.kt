package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

fun StockAction.toEffects(): List<StockEffect> = when (this) {

    StockAction.ScreenEntered -> listOf(
        StockEffect.ObserveStocks,
        StockEffect.TrackAnalytics(event = "screen_opened")
    )

    StockAction.PulledToRefresh -> listOf(
        StockEffect.RefreshStocks,
        StockEffect.TrackAnalytics(event = "pulled_to_refresh")
    )

    is StockAction.FavoriteClicked -> listOf(
        StockEffect.ToggleFavorite(id = id),
        StockEffect.TrackAnalytics(event = "favorite_clicked")
    )

    StockAction.RetryClicked -> listOf(
        StockEffect.RefreshStocks,
        StockEffect.TrackAnalytics(event = "retry_clicked")
    )
}
