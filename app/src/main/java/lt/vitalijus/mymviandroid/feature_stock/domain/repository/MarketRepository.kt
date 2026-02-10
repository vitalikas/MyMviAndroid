package lt.vitalijus.mymviandroid.feature_stock.domain.repository

import kotlinx.coroutines.flow.Flow
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState

interface MarketRepository {
    fun observeMarketState(): Flow<MarketState>
    suspend fun toggleMarketState()
}
