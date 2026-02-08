package lt.vitalijus.mymviandroid.feature_stock.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import lt.vitalijus.mymviandroid.core.analytics.LogCategory
import lt.vitalijus.mymviandroid.core.analytics.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class StockSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: StockRepository,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        logger.d(LogCategory.WORKER, StockSyncWorker::class, "🔄 Background refresh started...")
        
        return runCatching {
            repository.refresh()
        }.fold(
            onSuccess = {
                logger.d(LogCategory.WORKER, StockSyncWorker::class, "✅ Background refresh completed")
                Result.success()
            },
            onFailure = { error ->
                logger.e(LogCategory.WORKER, StockSyncWorker::class, "❌ Background refresh failed: ${error.message}")
                Result.retry()
            }
        )
    }
}
