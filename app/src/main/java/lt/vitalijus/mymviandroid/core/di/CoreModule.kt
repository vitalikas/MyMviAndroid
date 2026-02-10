package lt.vitalijus.mymviandroid.core.di

import lt.vitalijus.mymviandroid.core.analytics.AnalyticsTracker
import lt.vitalijus.mymviandroid.core.analytics.LogcatAnalyticsTracker
import lt.vitalijus.mymviandroid.core.analytics.LogcatLogger
import lt.vitalijus.mymviandroid.core.analytics.Logger
import org.koin.dsl.module

val coreModule = module {
    single<AnalyticsTracker> { LogcatAnalyticsTracker() }
    single<Logger> { LogcatLogger() }
    single { KoinWorkerFactory(stockRepository = get(), marketRepository = get(), logger = get()) }
}
