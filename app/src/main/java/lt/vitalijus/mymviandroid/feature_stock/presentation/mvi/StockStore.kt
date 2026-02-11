package lt.vitalijus.mymviandroid.feature_stock.presentation.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockPartialState
import lt.vitalijus.mymviandroid.feature_stock.presentation.state.StockState

class StockStore(
    private val effectHandler: StockEffectHandler,
    private val logger: Logger,
    private val scope: CoroutineScope,
    initialState: StockState = StockState()
) {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<StockState> = _state

    // Track long-running effects to prevent duplicates
    private var observeStocksJob: Job? = null

    fun dispatch(action: StockAction) {
        action.toEffects().forEach { effect ->
            when (effect) {
                is StockEffect.ObserveStocks -> {
                    // Cancel previous collector to prevent duplicates
                    observeStocksJob?.let {
                        logger.d(
                            LogCategory.EFFECT,
                            StockStore::class,
                            "⚠️ Cancelling previous ObserveStocks"
                        )
                        it.cancel()
                    }
                    logger.d(LogCategory.EFFECT, StockStore::class, "▶️ Starting new ObserveStocks")
                    observeStocksJob = launchEffect(effect = effect)
                }

                else -> {
                    // One-shot effects (fire-and-forget)
                    launchEffect(effect)
                }
            }
        }
    }

    private fun launchEffect(effect: StockEffect): Job {
        return scope.launch {
            effectHandler.handle(effect = effect)
                .collect { partial ->
                    logPartialState(partial = partial)
                    _state.update {
                        reduceStockState(
                            state = it,
                            partial = partial
                        )
                    }
                }
        }
    }

    private fun logPartialState(partial: StockPartialState) {
        when (partial) {
            is StockPartialState.DataLoaded ->
                logger.d(
                    LogCategory.PARTIAL_STATE,
                    StockStore::class,
                    "📦 DataLoaded: ${partial.stocks.size} stocks"
                )

            is StockPartialState.Error ->
                logger.e(
                    LogCategory.PARTIAL_STATE,
                    StockStore::class,
                    "❌ Error: ${partial.message}"
                )

            StockPartialState.Loading ->
                logger.d(LogCategory.PARTIAL_STATE, StockStore::class, "⏳ Loading...")

            StockPartialState.RefreshStarted ->
                logger.d(LogCategory.PARTIAL_STATE, StockStore::class, "🔄 Refresh started")

            StockPartialState.RefreshCompleted ->
                logger.d(LogCategory.PARTIAL_STATE, StockStore::class, "✅ Refresh completed")

            is StockPartialState.MarketStateChanged ->
                logger.d(
                    LogCategory.PARTIAL_STATE,
                    StockStore::class,
                    "📊 Market state: ${if (partial.isOpen) "OPEN" else "CLOSED"}"
                )

            is StockPartialState.ClearPriceBlink ->
                logger.d(LogCategory.PARTIAL_STATE, StockStore::class, "🔴 Price blink cleared")

            is StockPartialState.PriceChanged ->
                logger.d(
                    LogCategory.PARTIAL_STATE,
                    StockStore::class,
                    "💹 Price changed: ${partial.event.stockId}"
                )
        }
    }

    class Factory(
        private val effectHandler: StockEffectHandler,
        private val logger: Logger
    ) {
        fun create(scope: CoroutineScope): StockStore {
            return StockStore(
                effectHandler = effectHandler,
                logger = logger,
                scope = scope
            )
        }
    }
}
