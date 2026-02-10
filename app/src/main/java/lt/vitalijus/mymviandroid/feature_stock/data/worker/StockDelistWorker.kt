package lt.vitalijus.mymviandroid.feature_stock.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import lt.vitalijus.mymviandroid.core.analytics.LogCategory
import lt.vitalijus.mymviandroid.core.analytics.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class StockDelistWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: StockRepository,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val delistedStock = repository.delistRandomStock()

        logger.d(
            LogCategory.WORKER,
            StockDelistWorker::class,
            "🚫 $delistedStock delisted (removed from trading)"
        )

        return Result.success()
    }
}
