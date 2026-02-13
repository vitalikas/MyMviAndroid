package lt.vitalijus.mymviandroid.feature_stock.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lt.vitalijus.mymviandroid.core.log.LogCategory
import lt.vitalijus.mymviandroid.core.log.Logger
import lt.vitalijus.mymviandroid.feature_stock.data.remote.StockDto
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException

/**
 * Binance REST API implementation fetching top 10 cryptos by trading volume.
 *
 * Uses free public endpoint:
 * - GET /api/v3/ticker/24hr - Get 24hr ticker data with volume for all symbols
 *
 * No hardcoded lists - dynamically fetches top 10 by quoteVolume from Binance.
 */
class BinanceRestApi(
    private val client: OkHttpClient,
    private val logger: Logger
) : StockApi {

    companion object {
        const val BASE_URL = "https://api.binance.com"
        const val TICKER_24HR_ENDPOINT = "/api/v3/ticker/24hr"
        const val TOP_COUNT = 10
    }

    override suspend fun fetchStocks(): List<StockDto> = withContext(Dispatchers.IO) {
        try {
            // Fetch 24hr ticker data for all symbols
            val tickerArray = fetchTicker24hr()

            // Parse all USDT pairs and collect with volume for sorting
            val cryptoList = mutableListOf<CryptoData>()

            for (i in 0 until tickerArray.length()) {
                val ticker = tickerArray.getJSONObject(i)
                val symbol = ticker.getString("symbol")

                // Include only USDT spot trading pairs
                if (symbol.endsWith("USDT")) {
                    val baseAsset = symbol.removeSuffix("USDT")
                    val price = ticker.getString("lastPrice").toDoubleOrNull() ?: continue
                    val changePercent =
                        ticker.getString("priceChangePercent").toDoubleOrNull() ?: 0.0
                    val quoteVolume = ticker.getString("quoteVolume").toDoubleOrNull() ?: 0.0

                    // Skip zero-price entries (delisted/inactive)
                    if (price <= 0) continue

                    cryptoList.add(
                        CryptoData(
                            id = baseAsset,
                            name = baseAsset,
                            price = price,
                            dailyChangePercent = changePercent,
                            quoteVolume = quoteVolume
                        )
                    )
                }
            }

            // Sort by trading volume (quoteVolume) descending and take top 10
            val top10 = cryptoList
                .sortedByDescending { it.quoteVolume }
                .take(TOP_COUNT)
                .map { it.toStockDto() }

            logger.d(
                LogCategory.WORKER,
                BinanceRestApi::class,
                "📊 Fetched top ${top10.size} cryptos by volume from Binance API"
            )

            top10
        } catch (e: Exception) {
            logger.e(
                LogCategory.WORKER,
                BinanceRestApi::class,
                "❌ Failed to fetch from Binance: ${e.message}"
            )
            // Return empty list on error - no fallback hardcoded data
            emptyList()
        }
    }

    private fun fetchTicker24hr(): JSONArray {
        val url = "$BASE_URL$TICKER_24HR_ENDPOINT".toHttpUrl()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP ${response.code}")
        }

        val body = response.body?.string()
            ?: throw IOException("Empty response body")

        return JSONArray(body)
    }

    /**
     * Internal data class for sorting by volume.
     */
    private data class CryptoData(
        val id: String,
        val name: String,
        val price: Double,
        val dailyChangePercent: Double,
        val quoteVolume: Double
    ) {
        fun toStockDto() = StockDto(
            id = id,
            name = name,
            price = price,
            dailyChangePercent = dailyChangePercent
        )
    }
}