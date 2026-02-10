package lt.vitalijus.mymviandroid

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import lt.vitalijus.mymviandroid.core.di.KoinWorkerFactory
import lt.vitalijus.mymviandroid.core.di.coreModule
import lt.vitalijus.mymviandroid.feature_stock.data.worker.MarketToggleWorker
import lt.vitalijus.mymviandroid.feature_stock.data.worker.StockDelistWorker
import lt.vitalijus.mymviandroid.feature_stock.data.worker.StockSyncWorker
import lt.vitalijus.mymviandroid.feature_stock.di.stockModule
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.concurrent.TimeUnit

class MyApplication : Application(), Configuration.Provider {

    private val workerFactory: KoinWorkerFactory by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@MyApplication)
            modules(
                coreModule,
                stockModule
            )
        }

        // Schedule periodic work (15 minutes - minimum for PeriodicWork)
        WorkManager.getInstance(this).apply {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Stock sync worker - refreshes stock data every 15 minutes
            // Add 5 minute initial delay so it doesn't run immediately with test workers
            val stockSyncRequest = PeriodicWorkRequestBuilder<StockSyncWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            enqueueUniquePeriodicWork(
                "stock_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                stockSyncRequest
            )

            // Market toggle worker - toggles market state every 15 min (OPEN <-> CLOSED)
            // Add 5 minute initial delay so it doesn't run immediately with test workers
            val marketToggleRequest =
                PeriodicWorkRequestBuilder<MarketToggleWorker>(15, TimeUnit.MINUTES)
                    .setInitialDelay(5, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            enqueueUniquePeriodicWork(
                "market_toggle",
                ExistingPeriodicWorkPolicy.KEEP,
                marketToggleRequest
            )

            // For testing: schedule one-time workers after 30 seconds (unique to prevent duplicates)
            // Use REPLACE to ensure only the new delayed work runs
            val testStockSync = OneTimeWorkRequestBuilder<StockSyncWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build()
            enqueueUniqueWork(
                "test_stock_sync",
                ExistingWorkPolicy.REPLACE,
                testStockSync
            )

            val testMarketToggle = OneTimeWorkRequestBuilder<MarketToggleWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build()
            enqueueUniqueWork(
                "test_market_toggle",
                ExistingWorkPolicy.REPLACE,
                testMarketToggle
            )

            val testStockDelist = OneTimeWorkRequestBuilder<StockDelistWorker>()
                .setInitialDelay(60, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build()
            enqueueUniqueWork(
                "test_stock_delist",
                ExistingWorkPolicy.REPLACE,
                testStockDelist
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}
