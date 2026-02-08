package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import lt.vitalijus.mymviandroid.core.analytics.AnalyticsTracker
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.usecase.ObserveStocksWithFavoritesUseCase
import lt.vitalijus.mymviandroid.feature_stock.presentation.model.StockUi
import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockPartialState

class StockEffectHandler(
    private val observeUseCase: ObserveStocksWithFavoritesUseCase,
    private val stockRepository: StockRepository,
    private val favoritesRepository: FavoritesRepository,
    private val analytics: AnalyticsTracker
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun handle(effect: StockEffect): Flow<StockPartialState> =
        when (effect) {
            StockEffect.ObserveStocks ->
                observeUseCase()
                    .map { (stocks, favorites) ->
                        stocks.map { stock ->
                            StockUi(
                                id = stock.id,
                                name = stock.name,
                                price = stock.price,
                                isFavorite = stock.id in favorites
                            )
                        }
                    }
                    .map<List<StockUi>, StockPartialState> { stocks ->
                        StockPartialState.DataLoaded(stocks = stocks)
                    }
                    .onStart { emit(StockPartialState.Loading) }
                    .catch {
                        emit(
                            StockPartialState.Error(
                                message = it.message ?: "Unknown error"
                            )
                        )
                    }

            StockEffect.RefreshStocks ->
                flow {
                    emit(StockPartialState.RefreshStarted)
                    stockRepository.refresh()
                    emit(StockPartialState.RefreshCompleted)
                }.catch {
                    emit(StockPartialState.Error(it.message ?: "Refresh failed"))
                }

            is StockEffect.ToggleFavorite ->
                flow<Unit> {
                    favoritesRepository.toggleFavorite(effect.id)
                }.flatMapLatest { emptyFlow<StockPartialState>() }

            is StockEffect.TrackAnalytics ->
                flow<Unit> {
                    analytics.track(effect.event)
                }.flatMapLatest { emptyFlow<StockPartialState>() }
        }
}