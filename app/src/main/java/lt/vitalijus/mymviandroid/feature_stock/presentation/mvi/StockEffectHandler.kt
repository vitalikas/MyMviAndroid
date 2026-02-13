package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.core.analytics.AnalyticsTracker
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.repository.BinancePriceRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.usecase.ObserveTradableStocksUseCase
import lt.vitalijus.mymviandroid.feature_stock.presentation.model.StockUi
import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockPartialState

class StockEffectHandler(
    private val observeUseCase: ObserveTradableStocksUseCase,
    private val stockRepository: StockRepository,
    private val favoritesRepository: FavoritesRepository,
    private val marketRepository: MarketRepository,
    private val binancePriceRepository: BinancePriceRepository,
    private val analytics: AnalyticsTracker,
    private val priceChangeEventBus: PriceChangeEventBus,
    private val logger: Logger
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun handle(effect: StockEffect): Flow<StockPartialState> =
        when (effect) {
            StockEffect.ObserveStocks -> observeStocksFlow()

            StockEffect.RefreshStocks -> refreshStocksFlow()

            StockEffect.ConnectWebSocket -> {
                binancePriceRepository.start()
                logger.d(LogCategory.WORKER, StockEffectHandler::class, "▶️ WebSocket connected")
                emptyFlow()
            }

            StockEffect.DisconnectWebSocket -> {
                binancePriceRepository.stop()
                logger.d(LogCategory.WORKER, StockEffectHandler::class, "⏹️ WebSocket disconnected")
                emptyFlow()
            }

            is StockEffect.ToggleFavorite ->
                flow<Unit> {
                    favoritesRepository.toggleFavorite(id = effect.id)
                }.flatMapLatest { emptyFlow() }

            is StockEffect.TrackAnalytics ->
                flow<Unit> {
                    analytics.track(event = effect.event)
                }.flatMapLatest { emptyFlow() }
        }

    /**
     * Main observation flow:
     * 1. Show loading
     * 2. Fetch from API and cache to DB (initial load)
     * 3. Observe DB changes + market state + animations
     */
    private fun observeStocksFlow(): Flow<StockPartialState> = flow {
        emit(StockPartialState.Loading)

        // Step 1: Fetch from API and cache to DB
        try {
            stockRepository.refresh()
            logger.d(
                LogCategory.PARTIAL_STATE,
                StockEffectHandler::class,
                "✅ Initial data loaded from API"
            )
        } catch (e: Exception) {
            logger.e(
                LogCategory.PARTIAL_STATE,
                StockEffectHandler::class,
                "❌ Initial load failed: ${e.message}"
            )
        }

        // Step 2: Track active animations (stockId -> isPriceUp)
        val activeAnimations = MutableStateFlow<Map<String, Boolean>>(emptyMap())

        coroutineScope {
            // Collect price changes for animations
            launch {
                priceChangeEventBus.events.collect { event ->
                    // Start animation
                    activeAnimations.value += (event.stockId to event.isPriceUp)
                    // Clear after 1 second
                    launch {
                        delay(1000)
                        activeAnimations.value -= event.stockId
                    }
                }
            }

            // Combine DB + animations + market state
            combine(
                observeUseCase(),
                activeAnimations,
                marketRepository.observeMarketState()
            ) { tradableList, animations, marketState ->
                val stocks = tradableList.map { tradable ->
                    StockUi(
                        id = tradable.stock.id,
                        name = tradable.stock.name,
                        price = tradable.stock.price,
                        isFavorite = tradable.isFavorite,
                        isPriceUp = animations[tradable.stock.id]
                    )
                }

                Pair(
                    StockPartialState.DataLoaded(stocks = stocks),
                    StockPartialState.MarketStateChanged(isOpen = marketState == MarketState.OPEN)
                )
            }.collect { (dataLoaded, marketState) ->
                emit(dataLoaded)
                emit(marketState)
            }
        }
    }.catch {
        emit(StockPartialState.Error(it.message ?: "Unknown error"))
    }

    private fun refreshStocksFlow(): Flow<StockPartialState> = flow {
        emit(StockPartialState.RefreshStarted)
        stockRepository.refresh()
        emit(StockPartialState.RefreshCompleted)
    }.catch {
        emit(StockPartialState.Error(it.message ?: "Refresh failed"))
    }
}
