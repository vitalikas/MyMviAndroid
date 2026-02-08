package lt.vitalijus.mymviandroid.feature_stock.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import lt.vitalijus.mymviandroid.feature_stock.domain.model.Stock
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class ObserveStocksWithFavoritesUseCase(
    private val stockRepo: StockRepository,
    private val favoritesRepo: FavoritesRepository
) {
    operator fun invoke(): Flow<Pair<List<Stock>, Set<String>>> =
        combine(
            stockRepo.observeStocks(),
            favoritesRepo.observeFavorites()
        ) { stocks, favorites ->
            stocks to favorites
        }
}
