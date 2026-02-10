package lt.vitalijus.mymviandroid.feature_stock.domain.model

data class TradableStock(
    val stock: Stock,
    val isFavorite: Boolean,
    val isHot: Boolean
)
