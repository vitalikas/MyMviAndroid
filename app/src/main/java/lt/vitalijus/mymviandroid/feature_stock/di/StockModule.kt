package lt.vitalijus.mymviandroid.feature_stock.di

import androidx.room.Room
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.StockDatabase
import lt.vitalijus.mymviandroid.feature_stock.data.remote.api.BinanceRestApi
import lt.vitalijus.mymviandroid.feature_stock.data.remote.api.StockApi
import lt.vitalijus.mymviandroid.feature_stock.data.repository.BinancePriceRepository
import lt.vitalijus.mymviandroid.feature_stock.data.repository.MarketStateRepository
import lt.vitalijus.mymviandroid.feature_stock.data.repository.OfflineFirstStockRepository
import lt.vitalijus.mymviandroid.feature_stock.data.repository.RoomFavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.data.remote.ws.BinanceWebSocketClient
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository
import lt.vitalijus.mymviandroid.feature_stock.domain.usecase.ObserveTradableStocksUseCase
import lt.vitalijus.mymviandroid.feature_stock.presentation.StockViewModel
import lt.vitalijus.mymviandroid.feature_stock.presentation.mvi.StockEffectHandler
import lt.vitalijus.mymviandroid.feature_stock.presentation.mvi.StockStore
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val stockModule = module {

    single {
        Room.databaseBuilder(get(), StockDatabase::class.java, "stocks.db")
            .fallbackToDestructiveMigration(true)  // Clear data on schema change (dev only)
            .build()
    }

    // DAOs
    single { get<StockDatabase>().stockDao() }
    single { get<StockDatabase>().favoritesDao() }

    // API - Binance REST API for initial stock list
    single<StockApi> { BinanceRestApi(client = get(), logger = get()) }

    // Repositories
    single<StockRepository> { OfflineFirstStockRepository(get(), get()) }
    single<FavoritesRepository> { RoomFavoritesRepository(get()) }
    single { MarketStateRepository() }
    single<MarketRepository> { get<MarketStateRepository>() }

    // Binance WebSocket price streaming with lazy WebSocket initialization
    single {
        BinancePriceRepository(
            webSocketClientFactory = { listener ->
                BinanceWebSocketClient(
                    client = get(),
                    logger = get(),
                    listener = listener
                )
            },
            stockDao = get(),
            priceChangeEventBus = get(),
            marketRepository = get(),
            logger = get()
        )
    }

    // Use Cases
    factory { ObserveTradableStocksUseCase(get(), get(), get()) }

    // Presentation
    factory {
        StockEffectHandler(
            observeUseCase = get(),
            stockRepository = get(),
            favoritesRepository = get(),
            marketRepository = get(),
            binancePriceRepository = get(),
            analytics = get(),
            priceChangeEventBus = get(),
            logger = get()
        )
    }
    factory { StockStore.Factory(get(), get()) }
    viewModel { StockViewModel(get()) }
}
