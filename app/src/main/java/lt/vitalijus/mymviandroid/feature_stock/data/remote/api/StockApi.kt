package lt.vitalijus.mymviandroid.feature_stock.data.remote.api

import lt.vitalijus.mymviandroid.feature_stock.data.remote.StockDto

interface StockApi {
    suspend fun fetchStocks(): List<StockDto>
}
