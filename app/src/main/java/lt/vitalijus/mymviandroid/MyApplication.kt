package lt.vitalijus.mymviandroid

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import lt.vitalijus.mymviandroid.core.di.coreModule
import lt.vitalijus.mymviandroid.core.di.KoinWorkerFactory
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
        val periodicRequest = PeriodicWorkRequestBuilder<StockSyncWorker>(15, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(this).apply {
            enqueueUniquePeriodicWork(
                "stock_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            
            // For testing: schedule one-time work after 30 seconds
            val testRequest = OneTimeWorkRequestBuilder<StockSyncWorker>()
                .setInitialDelay(30, TimeUnit.SECONDS)
                .build()
            enqueue(testRequest)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()
}
