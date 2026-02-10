package lt.vitalijus.mymviandroid.feature_stock.domain.repository

import kotlinx.coroutines.flow.Flow
import lt.vitalijus.mymviandroid.feature_stock.domain.model.Stock

interface StockRepository {
    fun observeStocks(): Flow<List<Stock>>
    suspend fun refresh()
    suspend fun delistRandomStock(): Stock?
}
