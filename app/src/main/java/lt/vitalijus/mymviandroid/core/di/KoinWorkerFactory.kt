package lt.vitalijus.mymviandroid.core.di

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.worker.MarketToggleWorker
import lt.vitalijus.mymviandroid.feature_stock.data.worker.StockDelistWorker
import lt.vitalijus.mymviandroid.feature_stock.data.worker.StockSyncWorker
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class KoinWorkerFactory(
    private val stockRepository: StockRepository,
    private val marketRepository: MarketRepository,
    private val logger: Logger
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            StockSyncWorker::class.java.name -> {
                StockSyncWorker(
                    context = appContext,
                    params = workerParameters,
                    repository = stockRepository,
                    logger = logger
                )
            }

            MarketToggleWorker::class.java.name -> {
                MarketToggleWorker(
                    context = appContext,
                    params = workerParameters,
                    repository = marketRepository,
                    logger = logger
                )
            }

            StockDelistWorker::class.java.name -> {
                StockDelistWorker(
                    context = appContext,
                    params = workerParameters,
                    repository = stockRepository,
                    logger = logger
                )
            }

            else -> null
        }
    }
}
