package lt.vitalijus.mymviandroid.core.di

import lt.vitalijus.mymviandroid.core.analytics.AnalyticsTracker
import lt.vitalijus.mymviandroid.core.analytics.LogcatAnalyticsTracker
import lt.vitalijus.mymviandroid.core.log.LogcatLogger
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.event.PriceChangeEventBus
import org.koin.dsl.module

val coreModule = module {
    single<AnalyticsTracker> { LogcatAnalyticsTracker() }
    single<Logger> { LogcatLogger() }

    // SharedFlow event bus for price changes (many-to-many pattern)
    single { PriceChangeEventBus() }

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
