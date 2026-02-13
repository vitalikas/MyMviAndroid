package lt.vitalijus.mymviandroid.feature_stock.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import lt.vitalijus.mymviandroid.feature_stock.domain.event.StockPriceChangeEvent
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.PriceRepository
import lt.vitalijus.mymviandroid.feature_stock.data.remote.ws.WebSocketClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

/**
 * Repository that connects WebSocket price stream to event bus and DB.
 *
 * 🔄 Batches price updates every 2 seconds to reduce DB writes
 * 📡 Emits events to [PriceChangeEventBus] for UI animations
 * 💾 Updates DB with batched prices
 * 🎯 Only active when market is OPEN
 */
class BinancePriceRepository(
    private val webSocketClient: WebSocketClient,
    private val stockDao: StockDao,
    private val priceChangeEventBus: PriceChangeEventBus,
    private val marketRepository: MarketRepository,
    private val logger: Logger
) : PriceRepository {
    private val scope = CoroutineScope(Dispatchers.IO)

    // Track last known prices for change detection
    private val lastPrices = ConcurrentHashMap<String, Double>()

    // Batched updates buffer: symbol -> newPrice
    private val pendingUpdates = ConcurrentHashMap<String, Double>()

    // Coroutine jobs for cleanup
    private var collectionJob: Job? = null
    private var batchingJob: Job? = null

    private var isStarted = false

    /**
     * Starts price streaming when market is open.
     * Monitors market state and manages WebSocket lifecycle.
     */
    override fun start() {
        if (isStarted) return
        isStarted = true

        collectionJob = scope.launch {
            marketRepository.observeMarketState().collect { state ->
                when (state) {
                    MarketState.OPEN -> openStream()
                    MarketState.CLOSED -> closeStream()
                }
            }
        }
    }

    /**
     * Stops all price streaming and cleans up resources.
     */
    override fun stop() {
        isStarted = false
        closeStream()
        collectionJob?.cancel()
        collectionJob = null
        scope.cancel()
    }

    private suspend fun openStream() {
        logger.d(
            LogCategory.WORKER,
            BinancePriceRepository::class,
            "🔓 Market OPEN - connecting WebSocket, starting batch processing"
        )

        // Load baseline prices from DB
        stockDao.observeStocks().first().forEach { stock ->
            lastPrices[stock.id] = stock.price
        }
        logger.d(
            LogCategory.WORKER,
            BinancePriceRepository::class,
            "📊 Loaded ${lastPrices.size} baseline prices from DB"
        )

        // Connect WebSocket
        webSocketClient.connect()

        // Start batching coroutine
        batchingJob?.cancel()
        batchingJob = scope.launch {
            runBatchingLoop()
        }

        // Collect price updates from WebSocket Flow
        scope.launch {
            webSocketClient.priceUpdates
                .filter { it.symbol in lastPrices.keys } // Only tracked symbols
                .collect { update ->
                    val lastPrice = lastPrices[update.symbol]
                    if (lastPrice != null && lastPrice != update.price) {
                        // Buffer the update
                        pendingUpdates[update.symbol] = update.price
                    }
                }
        }
    }

    private fun closeStream() {
        logger.d(
            LogCategory.WORKER,
            BinancePriceRepository::class,
            "🔒 Market CLOSED - disconnecting WebSocket"
        )

        webSocketClient.disconnect()
        batchingJob?.cancel()
        batchingJob = null
        lastPrices.clear()
        pendingUpdates.clear()
    }

    /**
     * Batching loop - processes updates every 2 seconds.
     */
    private suspend fun runBatchingLoop() {
        while (true) {
            delay(2000.milliseconds)

            if (pendingUpdates.isEmpty()) continue

            // Atomically swap buffers
            val batch = pendingUpdates.toMap()
            pendingUpdates.clear()

            // Process batch
            val events = mutableListOf<StockPriceChangeEvent>()

            batch.forEach { (symbol, newPrice) ->
                val oldPrice = lastPrices[symbol] ?: return@forEach
                if (oldPrice == newPrice) return@forEach

                // Update DB
                stockDao.updateStockPrice(
                    stockId = symbol,
                    price = newPrice
                )

                // Create event
                val event = StockPriceChangeEvent(
                    stockId = symbol,
                    oldPrice = oldPrice,
                    newPrice = newPrice
                )
                events.add(event)

                // Update cache
                lastPrices[symbol] = newPrice

                // Log significant changes
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

            // Emit events to UI
            events.forEach { priceChangeEventBus.emit(it) }

            if (events.isNotEmpty()) {
                logger.d(
                    LogCategory.WORKER,
                    BinancePriceRepository::class,
                    "📦 Processed batch of ${events.size} price updates"
                )
            }
        }
    }
}
