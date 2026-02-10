package lt.vitalijus.mymviandroid.feature_stock.data.mapper

import lt.vitalijus.mymviandroid.feature_stock.data.local.model.StockEntity
import lt.vitalijus.mymviandroid.feature_stock.data.remote.StockDto
import lt.vitalijus.mymviandroid.feature_stock.domain.model.Stock

fun StockDto.toEntity(): StockEntity = StockEntity(
    id = id,
    name = name,
    price = price,
    updatedAt = System.currentTimeMillis(),
    isDelisted = isDelisted,
    dailyChangePercent = dailyChangePercent
)

fun StockEntity.toDomain(): Stock = Stock(
    id = id,
    name = name,
    price = price,
    updatedAt = updatedAt,
    isDelisted = isDelisted,
    dailyChangePercent = dailyChangePercent
)
