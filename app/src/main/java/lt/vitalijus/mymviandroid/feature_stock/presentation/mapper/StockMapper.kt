package lt.vitalijus.mymviandroid.feature_stock.presentation.mapper

import lt.vitalijus.mymviandroid.feature_stock.domain.model.Stock
import lt.vitalijus.mymviandroid.feature_stock.presentation.model.StockUi

fun Stock.toUi(favorites: Set<String>): StockUi = StockUi(
    id = id,
    name = name,
    price = price,
    isFavorite = id in favorites
)
