package lt.vitalijus.mymviandroid.feature_stock.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.model.TradableStock
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class ObserveTradableStocksUseCase(
    private val stockRepo: StockRepository,
    private val favoritesRepo: FavoritesRepository,
    private val marketRepository: MarketRepository
) {

    operator fun invoke(): Flow<List<TradableStock>> =
        combine(
            stockRepo.observeStocks(),
            favoritesRepo.observeFavorites(),
            marketRepository.observeMarketState()
        ) { stocks, favorites, marketState ->
            stocks
                .filter { !it.isDelisted }
                .map { stock ->
                    TradableStock(
                        stock = stock,
                        isFavorite = stock.id in favorites,
                        isHot = stock.dailyChangePercent <= -5.0
                    )
                }.let { list ->
                    when (marketState) {
                        MarketState.OPEN -> list.sortedByDescending { it.isFavorite }
                        MarketState.CLOSED -> list.filter { it.isFavorite }
                    }
                }
        }
}
