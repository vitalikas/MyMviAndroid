package lt.vitalijus.mymviandroid.feature_stock.di

import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.StockDatabase
import lt.vitalijus.mymviandroid.feature_stock.data.remote.FakeStockApi
import lt.vitalijus.mymviandroid.feature_stock.data.remote.StockApi
import lt.vitalijus.mymviandroid.feature_stock.data.repository.OfflineFirstStockRepository
import lt.vitalijus.mymviandroid.feature_stock.data.repository.RoomFavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.usecase.ObserveStocksWithFavoritesUseCase
import lt.vitalijus.mymviandroid.feature_stock.presentation.mvi.StockEffectHandler
import lt.vitalijus.mymviandroid.feature_stock.presentation.mvi.StockStore
import lt.vitalijus.mymviandroid.feature_stock.presentation.StockViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val stockModule = module {

    // Database
    single {
        val seedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        Room.databaseBuilder(get(), StockDatabase::class.java, "stocks.db")
            .addCallback(StockDatabase.SeedCallback(seedScope))
            .build()
    }

    // DAOs
    single { get<StockDatabase>().stockDao() }
    single { get<StockDatabase>().favoritesDao() }

    // API
    single<StockApi> { FakeStockApi() }

    // Repositories
    single<StockRepository> { OfflineFirstStockRepository(get(), get()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get()) }

    // Use Cases
    factory { ObserveStocksWithFavoritesUseCase(get(), get()) }

    // Presentation
    factory { StockEffectHandler(get(), get(), get(), get()) }
    factory { StockStore.Factory(get(), get()) }
    viewModel { StockViewModel(get()) }
}
