package lt.vitalijus.mymviandroid.core.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.worker.MarketToggleWorker
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository

/**
 * Koin-powered WorkerFactory for creating WorkManager workers with dependency injection.
 */
class KoinWorkerFactory(
    private val marketRepository: MarketRepository,
    private val logger: Logger
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        logger.d(
            LogCategory.WORKER,
            KoinWorkerFactory::class,
            "🔧 Creating worker: $workerClassName"
        )
        return when (workerClassName) {
            MarketToggleWorker::class.java.name -> {
                MarketToggleWorker(
                    context = appContext,
                    params = workerParameters,
                    repository = marketRepository,
                    logger = logger
                )
            }

            else -> null
        }
    }
}
