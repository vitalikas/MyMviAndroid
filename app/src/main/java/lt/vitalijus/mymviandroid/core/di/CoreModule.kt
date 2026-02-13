package lt.vitalijus.mymviandroid.core.di

import lt.vitalijus.mymviandroid.core.analytics.AnalyticsTracker
import lt.vitalijus.mymviandroid.core.analytics.LogcatAnalyticsTracker
import lt.vitalijus.mymviandroid.core.log.LogcatLogger
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import okhttp3.OkHttpClient
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

val coreModule = module {
    single<AnalyticsTracker> { LogcatAnalyticsTracker() }
    single<Logger> { LogcatLogger() }

    // SharedFlow event bus for price changes (many-to-many pattern)
    single { PriceChangeEventBus() }

    // Shared OkHttpClient for REST API and WebSocket
    single {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS) // For WebSocket
            .build()
    }

    single {
        KoinWorkerFactory(
            stockRepository = get(),
            marketRepository = get(),
            stockDao = get(),
            priceChangeEventBus = get(),
            logger = get()
        )
    }
}
