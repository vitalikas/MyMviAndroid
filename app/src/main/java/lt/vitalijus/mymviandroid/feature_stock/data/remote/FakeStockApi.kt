package lt.vitalijus.mymviandroid.feature_stock.data.remote

import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeStockApi : StockApi {

    override suspend fun fetchStocks(): List<StockDto> {
        // Simulate network delay (1-2 seconds)
        delay(1500L)
        
        // Return stocks with random prices to see refresh working
        return listOf(
            StockDto(id = "AAPL", name = "Apple Inc.", price = randomPrice(150.0, 200.0)),
            StockDto(id = "GOOGL", name = "Alphabet Inc.", price = randomPrice(120.0, 160.0)),
            StockDto(id = "MSFT", name = "Microsoft Corp.", price = randomPrice(380.0, 450.0)),
            StockDto(id = "AMZN", name = "Amazon.com Inc.", price = randomPrice(150.0, 200.0)),
            StockDto(id = "TSLA", name = "Tesla Inc.", price = randomPrice(200.0, 280.0)),
            StockDto(id = "META", name = "Meta Platforms", price = randomPrice(480.0, 560.0)),
            StockDto(id = "NVDA", name = "NVIDIA Corp.", price = randomPrice(800.0, 950.0)),
            StockDto(id = "NFLX", name = "Netflix Inc.", price = randomPrice(650.0, 750.0))
        )
    }

    private fun randomPrice(min: Double, max: Double): Double {
        val price = min + Random.nextDouble() * (max - min)
        return (price * 100).toInt() / 100.0 // Round to 2 decimal places
    }
}
