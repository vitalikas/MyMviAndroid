package lt.vitalijus.mymviandroid.feature_stock.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class StockSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val stockRepository: StockRepository,
    private val marketRepository: MarketRepository,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Check if market is open - only sync when market is open
        val marketState = marketRepository.observeMarketState().first()

        if (marketState == MarketState.CLOSED) {
            logger.d(
                LogCategory.WORKER,
                StockSyncWorker::class,
                "🔒 Market is CLOSED - skipping background refresh"
            )
            return Result.success() // Not a failure, just skipped
        }

        logger.d(
            LogCategory.WORKER,
            StockSyncWorker::class,
            "🔄 Market is OPEN - background refresh starting..."
        )

        return runCatching {
            stockRepository.refresh()
        }.fold(
            onSuccess = {
                logger.d(
                    LogCategory.WORKER,
                    StockSyncWorker::class,
                    "✅ Background refresh completed"
                )
                Result.success()
            },
            onFailure = { error ->
                logger.e(
                    LogCategory.WORKER,
                    StockSyncWorker::class,
                    "❌ Background refresh failed: ${error.message}"
                )
                Result.retry()
            }
        )
    }
}
