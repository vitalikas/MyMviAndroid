package lt.vitalijus.mymviandroid.feature_stock.data.remote

import kotlinx.coroutines.delay
import kotlin.random.Random

class FakeStockApi : StockApi {

    override suspend fun fetchStocks(): List<StockDto> {
        // Simulate network delay (1 seconds)
        delay(1000L)

        // Return stocks with random prices to see refresh working
        return listOf(
            StockDto(
                id = "AAPL",
                name = "Apple Inc.",
                price = randomPrice(150.0, 200.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "GOOGL",
                name = "Alphabet Inc.",
                price = randomPrice(120.0, 160.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "MSFT",
                name = "Microsoft Corp.",
                price = randomPrice(380.0, 450.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "AMZN",
                name = "Amazon.com Inc.",
                price = randomPrice(150.0, 200.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "TSLA",
                name = "Tesla Inc.",
                price = randomPrice(200.0, 280.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "META",
                name = "Meta Platforms",
                price = randomPrice(480.0, 560.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "NVDA",
                name = "NVIDIA Corp.",
                price = randomPrice(800.0, 950.0),
                dailyChangePercent = randomChangePercent()
            ),
            StockDto(
                id = "NFLX",
                name = "Netflix Inc.",
                price = randomPrice(650.0, 750.0),
                dailyChangePercent = randomChangePercent()
            )
        )
    }

    private fun randomPrice(min: Double, max: Double): Double {
        val price = min + Random.nextDouble() * (max - min)
        return (price * 100).toInt() / 100.0 // Round to 2 decimal places
    }

    private fun randomChangePercent(): Double {
        // Random change between -15% and +15%
        val change = -15.0 + Random.nextDouble() * 30.0
        return (change * 100).toInt() / 100.0 // Round to 2 decimal places
    }
}
