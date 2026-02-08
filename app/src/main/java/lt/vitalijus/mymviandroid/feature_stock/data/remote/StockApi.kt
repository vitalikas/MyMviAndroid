package lt.vitalijus.mymviandroid.feature_stock.data.remote

interface StockApi {
    suspend fun fetchStocks(): List<StockDto>
}
