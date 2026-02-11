package lt.vitalijus.mymviandroid.feature_stock.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.StockEntity
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import lt.vitalijus.mymviandroid.feature_stock.domain.event.StockPriceChangeEvent
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import kotlin.random.Random

/**
 * Worker that simulates stock price changes every 15 minutes.
 * Minimum WorkManager interval: 15 minutes (PeriodicWorkRequestBuilder constraint)
 * ⚠️ Only changes prices when market is OPEN
 */
class StockPriceChangeWorker(
    context: Context,
    params: WorkerParameters,
    private val stockDao: StockDao,
    private val priceChangeEventBus: PriceChangeEventBus,
    private val marketRepository: MarketRepository,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        logger.d(
            LogCategory.WORKER,
            StockPriceChangeWorker::class,
            "💰 Starting price change simulation..."
        )

        return runCatching {
            // Check if market is open
            val marketState = marketRepository.observeMarketState().first()

            if (marketState == MarketState.CLOSED) {
                logger.d(
                    LogCategory.WORKER,
                    StockPriceChangeWorker::class,
                    "🔒 Market is CLOSED - skipping price changes"
                )
                return Result.success()
            }

            // Get current stocks from database
            val currentStocks = stockDao.observeStocks().first()

            if (currentStocks.isEmpty()) {
                logger.d(
                    LogCategory.WORKER,
                    StockPriceChangeWorker::class,
                    "⚠️ No stocks found, skipping price changes"
                )
                return Result.success()
            }

            // Simulate price changes for each stock
            val updatedStocks = currentStocks.map { stock ->
                val newPrice = calculateNewPrice(currentPrice = stock.price)
                stock.copy(price = newPrice)
            }

            // Save updated prices to database
            stockDao.insertAll(stocks = updatedStocks)

            // Emit price change events to SharedFlow event bus
            emitPriceChangeEvents(
                oldStocks = currentStocks,
                newStocks = updatedStocks
            )

            logger.d(
                LogCategory.WORKER,
                StockPriceChangeWorker::class,
                "✅ Price changes applied for ${updatedStocks.size} stocks"
            )

            Result.success()
        }.getOrElse { error ->
            logger.e(
                LogCategory.WORKER,
                StockPriceChangeWorker::class,
                "❌ Price change simulation failed: ${error.message}"
            )
            Result.retry()
        }
    }

    /**
     * Calculates new price with random fluctuation between -10% and +10%.
     */
    private fun calculateNewPrice(currentPrice: Double): Double {
        // Random change between -10% and +10%
        val changePercent = -10.0 + Random.nextDouble() * 20.0
        val changeAmount = currentPrice * (changePercent / 100)
        val newPrice = (currentPrice + changeAmount).coerceAtLeast(0.01)
        return (newPrice * 100).toInt() / 100.0 // Round to 2 decimal places
    }

    /**
     * Emits price change events to the SharedFlow event bus.
     * Many-to-many: worker produces, multiple consumers can react.
     *
     * Elegant functional approach:
     * 1. associateBy → Map for O(1) lookup
     * 2. mapNotNull → transform + filter in one pass
     * 3. forEach → emit events
     */
    private suspend fun emitPriceChangeEvents(
        oldStocks: List<StockEntity>,
        newStocks: List<StockEntity>
    ) {
        val oldPriceMap = oldStocks.associateBy { it.id }

        newStocks
            .mapNotNull { newStock ->
                oldPriceMap[newStock.id]?.takeIf { it.price != newStock.price }?.let { oldStock ->
                    StockPriceChangeEvent(
                        stockId = newStock.id,
                        oldPrice = oldStock.price,
                        newPrice = newStock.price
                    )
                }
            }
            .forEach { event ->
                priceChangeEventBus.emit(event)
                logPriceChange(event)
            }
    }

    private fun logPriceChange(event: StockPriceChangeEvent) {
        val direction = when {
            event.isPriceUp -> "📈 UP"
            event.isPriceDown -> "📉 DOWN"
            else -> "➡️ FLAT"
        }
        logger.d(
            LogCategory.WORKER,
            StockPriceChangeWorker::class,
            "$direction ${event.stockId}: $${event.oldPrice} → $${event.newPrice} (${event.percentChange.toInt()}%)"
        )
    }
}
