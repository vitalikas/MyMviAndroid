package lt.vitalijus.mymviandroid.feature_stock.data.remote

data class StockDto(
    val id: String,
    val name: String,
    val price: Double,
    val isDelisted: Boolean = false,
    val dailyChangePercent: Double = 0.0
)
