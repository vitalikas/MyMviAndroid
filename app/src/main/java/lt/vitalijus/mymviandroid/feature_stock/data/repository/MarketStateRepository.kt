package lt.vitalijus.mymviandroid.feature_stock.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository

class MarketStateRepository : MarketRepository {

    private val _marketState = MutableStateFlow(MarketState.CLOSED)
    val marketState: Flow<MarketState> = _marketState.asStateFlow()

    override fun observeMarketState(): Flow<MarketState> = marketState

    override suspend fun toggleMarketState() {
        _marketState.value = when (_marketState.value) {
            MarketState.OPEN -> MarketState.CLOSED
            MarketState.CLOSED -> MarketState.OPEN
        }
    }
}
