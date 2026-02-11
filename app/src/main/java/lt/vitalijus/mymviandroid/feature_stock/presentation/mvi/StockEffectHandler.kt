package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import lt.vitalijus.mymviandroid.core.analytics.AnalyticsTracker
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.usecase.ObserveTradableStocksUseCase
import lt.vitalijus.mymviandroid.feature_stock.presentation.model.StockUi
import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockPartialState
import kotlin.random.Random

class StockEffectHandler(
    private val observeUseCase: ObserveTradableStocksUseCase,
    private val stockRepository: StockRepository,
    private val favoritesRepository: FavoritesRepository,
    private val marketRepository: MarketRepository,
    private val analytics: AnalyticsTracker,
    private val priceChangeEventBus: PriceChangeEventBus,
    private val logger: Logger
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun handle(effect: StockEffect): Flow<StockPartialState> =
        when (effect) {
            StockEffect.ObserveStocks -> {
                val stocksFlow = observeUseCase()
                    .map { tradableList ->
                        tradableList.map { tradable ->
                            StockUi(
                                id = tradable.stock.id,
                                name = tradable.stock.name,
                                price = tradable.stock.price,
                                isFavorite = tradable.isFavorite,
                                isPriceUp = null // No animation by default
                            )
                        }
                    }
                    .map<List<StockUi>, StockPartialState> { stocks ->
                        StockPartialState.DataLoaded(stocks = stocks)
                    }

                val marketFlow = marketRepository.observeMarketState()
                    .map { state ->
                        StockPartialState.MarketStateChanged(isOpen = state == MarketState.OPEN)
                    }

                /**
                 * Price change event flow from SharedFlow event bus.
                 * Collects events from Worker and converts to PartialState.
                 * Many-to-many: any producer can emit, multiple consumers can collect.
                 */
                val priceChangeFlow = priceChangeEventBus.events
                    .flatMapMerge(concurrency = 16) { event ->
                        logger.d(
                            LogCategory.PARTIAL_STATE,
                            StockEffectHandler::class,
                            "🔥 Price change event: ${event.stockId} ${
                                if (event.isPriceUp) "📈 UP" else "📉 DOWN"
                            }"
                        )

                        flow {
                            // Random stagger delay (0-1500ms) - each stock blinks at different time
                            val staggerDelay = Random.nextLong(0, 1500)
                            delay(staggerDelay)

                            // Emit price change to trigger animation
                            emit(StockPartialState.PriceChanged(event = event))

                            // After animation duration (3000ms = 3s), clear the blink state
                            delay(3000)
                            emit(StockPartialState.ClearPriceBlink(event.stockId))
                        }
                    }

                merge(stocksFlow, marketFlow, priceChangeFlow)
                    .onStart { emit(StockPartialState.Loading) }
                    .catch {
                        emit(
                            StockPartialState.Error(
                                message = it.message ?: "Unknown error"
                            )
                        )
                    }
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
