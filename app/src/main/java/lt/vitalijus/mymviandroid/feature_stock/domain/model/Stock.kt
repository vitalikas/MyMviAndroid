package lt.vitalijus.mymviandroid.feature_stock.domain.model

data class Stock(
    val id: String,
    val name: String,
    val price: Double,
    val updatedAt: Long,
    val isDelisted: Boolean = false,
    val dailyChangePercent: Double = 0.0
)
