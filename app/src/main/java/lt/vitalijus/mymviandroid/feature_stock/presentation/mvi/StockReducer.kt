package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockPartialState
import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockState

fun reduceStockState(
    state: StockState,
    partial: StockPartialState
): StockState = when (partial) {
    StockPartialState.Loading ->
        state.copy(
            isLoading = true,
            error = null
        )

    is StockPartialState.DataLoaded ->
        state.copy(
            isLoading = false,
            isRefreshing = false,
            stocks = partial.stocks,
            error = null
        )

    is StockPartialState.Error ->
        state.copy(
            isLoading = false,
            isRefreshing = false,
            error = partial.message
        )

    StockPartialState.RefreshStarted -> state.copy(isRefreshing = true)

    StockPartialState.RefreshCompleted -> state.copy(isRefreshing = false)

    is StockPartialState.MarketStateChanged -> state.copy(isMarketOpen = partial.isOpen)
}
