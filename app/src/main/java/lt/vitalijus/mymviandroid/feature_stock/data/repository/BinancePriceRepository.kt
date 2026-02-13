package lt.vitalijus.mymviandroid.feature_stock.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import lt.vitalijus.mymviandroid.feature_stock.domain.event.StockPriceChangeEvent
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.websocket.PriceUpdateListener
import lt.vitalijus.mymviandroid.feature_stock.domain.websocket.WebSocketClient
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Repository that connects WebSocket price stream to our event bus AND updates DB.
 *
 * 🔄 Live price streaming with batching to reduce DB writes and UI updates
 * 📡 Emits price changes every 500ms (not per-message) to prevent overwhelming the UI
 * 💾 Updates DB with batched prices
 * 🎯 Only active when market is OPEN
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BinancePriceRepository(
    private val webSocketClientFactory: (PriceUpdateListener) -> WebSocketClient,
    private val stockDao: StockDao,
    private val priceChangeEventBus: PriceChangeEventBus,
    private val marketRepository: MarketRepository,
    private val logger: Logger
) : PriceUpdateListener {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Lazy WebSocket initialization - created when first needed
    private val webSocketClient: WebSocketClient by lazy { webSocketClientFactory(this) }

    // Track last known prices for change detection
    private val lastPrices = ConcurrentHashMap<String, Double>()

    // Track current market state
    private val isMarketOpen = AtomicBoolean(false)

    // Buffer for pending price updates (before batching)
    private val pendingUpdates = ConcurrentHashMap<String, Double>()

    // Flow to trigger batch processing (conflated - drops signals if collector is slow)
    private val _updateSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val updateSignal = _updateSignal.asSharedFlow()

    private var isStarted = false

    /**
     * Starts the WebSocket connection and batch processing.
     */
    fun start() {
        if (isStarted) return
        isStarted = true

        // Start batch processing coroutine
        scope.launch {
            processBatchedUpdates()
        }

        scope.launch {
            marketRepository.observeMarketState().collect { state ->
                when (state) {
                    MarketState.OPEN -> {
                        logger.d(
                            LogCategory.WORKER,
                            BinancePriceRepository::class,
                            "🔓 Market OPEN - loading prices from DB, connecting WebSocket..."
                        )
                        isMarketOpen.set(true)
                        // Load current prices from DB for change detection baseline
                        scope.launch {
                            stockDao.observeStocks().first().forEach { stock ->
                                lastPrices[stock.id] = stock.price
                            }
                            logger.d(
                                LogCategory.WORKER,
                                BinancePriceRepository::class,
                                "📊 Loaded ${lastPrices.size} prices from DB"
                            )
                        }
                        webSocketClient.connect()
                    }

                    MarketState.CLOSED -> {
                        logger.d(
                            LogCategory.WORKER,
                            BinancePriceRepository::class,
                            "🔒 Market CLOSED - disconnecting WebSocket..."
                        )
                        isMarketOpen.set(false)
                        webSocketClient.disconnect()
                        lastPrices.clear()
                        pendingUpdates.clear()
                    }
                }
            }
        }
    }

    /**
     * Stops the WebSocket connection.
     */
    fun stop() {
        isStarted = false
        isMarketOpen.set(false)
        webSocketClient.disconnect()
        lastPrices.clear()
        pendingUpdates.clear()
    }

    /**
     * Processes batched updates every 500ms.
     * This prevents overwhelming the DB and UI with too many individual updates.
     */
    private suspend fun processBatchedUpdates() {
        updateSignal
            .sample(2000.milliseconds) // Process at most once every 2 seconds
            .collect {
                if (!isMarketOpen.get()) return@collect

                // Get all pending updates and clear buffer
                val updates = pendingUpdates.toMap()
                pendingUpdates.clear()

                if (updates.isEmpty()) return@collect

                // Process batch
                val events = mutableListOf<StockPriceChangeEvent>()

                updates.forEach { (symbol, newPrice) ->
                    val oldPrice = lastPrices[symbol]

                    if (oldPrice != null && oldPrice != newPrice) {
                        // Update DB
                        stockDao.updateStockPrice(symbol, newPrice)

                        // Create event
                        val event = StockPriceChangeEvent(
                            stockId = symbol,
                            oldPrice = oldPrice,
                            newPrice = newPrice
                        )
                        events.add(event)

                        // Update cache
                        lastPrices[symbol] = newPrice

                        // Log significant changes only
                        val changePercent = ((newPrice - oldPrice) / oldPrice) * 100
                        if (abs(changePercent) > 0.5) {
                            val direction = when {
                                event.isPriceUp -> "📈"
                                event.isPriceDown -> "📉"
                                else -> "➡️"
                            }
                            logger.d(
                                LogCategory.WORKER,
                                BinancePriceRepository::class,
                                "$direction $symbol: $$oldPrice → $$newPrice (${changePercent.toInt()}%)"
                            )
                        }
                    }
                }

                // Emit all events as a batch (UI can handle multiple at once)
                events.forEach { event ->
                    priceChangeEventBus.emit(event)
                }

                if (events.isNotEmpty()) {
                    logger.d(
                        LogCategory.WORKER,
                        BinancePriceRepository::class,
                        "📦 Processed batch of ${events.size} price updates"
                    )
                }
            }
    }

    /**
     * Called by WebSocket client on price updates.
     * Just adds to pending buffer - actual processing happens every 500ms.
     */
    override fun onPriceUpdate(
        symbol: String,
        price: Double,
        @Suppress("UNUSED_PARAMETER") percentChange: Double
    ) {
        if (!isMarketOpen.get()) return

        // Skip if price hasn't changed from last known
        val lastPrice = lastPrices[symbol]
        if (lastPrice != null && lastPrice == price) return

        // Add to pending updates buffer
        pendingUpdates[symbol] = price

        // Signal that we have updates (conflated - multiple signals = one processing)
        _updateSignal.tryEmit(Unit)
    }
}
