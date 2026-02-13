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
import lt.vitalijus.mymviandroid.feature_stock.di.stockModule
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.PriceRepository
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import java.util.concurrent.TimeUnit

class MyApplication : Application(), Configuration.Provider {

    private val workerFactory: KoinWorkerFactory by inject()
    private val priceRepository: PriceRepository by inject()

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

        startWebSocket()

        runWorks()
    }

    private fun startWebSocket() {
        priceRepository.start()
    }

    private fun runWorks() {
        WorkManager.getInstance(this).apply {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Cancel any existing workers first
            cancelAllWork()

            // Market toggle worker - toggles market state every 15 min (OPEN <-> CLOSED)
            // Long delay (10 min) to let user test the app
            val marketToggleRequest =
                PeriodicWorkRequestBuilder<MarketToggleWorker>(15, TimeUnit.MINUTES)
                    .setInitialDelay(10, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
            enqueueUniquePeriodicWork(
                "market_toggle",
                ExistingPeriodicWorkPolicy.REPLACE,
                marketToggleRequest
            )

            // Open market after 10 seconds for testing
            Log.d("MyApplication", "📅 Scheduling market open in 10 seconds...")
            val openMarketRequest = OneTimeWorkRequestBuilder<MarketToggleWorker>()
                .setInitialDelay(10, TimeUnit.SECONDS)
                .setConstraints(constraints)
                .build()
            enqueueUniqueWork(
                "market_open",
                ExistingWorkPolicy.REPLACE,
                openMarketRequest
            )
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}
