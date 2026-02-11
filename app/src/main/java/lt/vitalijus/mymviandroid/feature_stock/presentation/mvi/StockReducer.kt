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

    is StockPartialState.PriceChanged -> {
        val updatedStocks = state.stocks.map { stock ->
            if (stock.id == partial.event.stockId) {
                stock.copy(
                    price = partial.event.newPrice,
                    isPriceUp = partial.event.isPriceUp
                )
            } else {
                stock
            }
        }
        state.copy(stocks = updatedStocks)
    }

    is StockPartialState.ClearPriceBlink -> {
        val updatedStocks = state.stocks.map { stock ->
            if (stock.id == partial.stockId) {
                stock.copy(isPriceUp = null)
            } else {
                stock
            }
        }
        state.copy(stocks = updatedStocks)
    }
}
