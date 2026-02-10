package lt.vitalijus.mymviandroid.feature_stock.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.StockEntity
import lt.vitalijus.mymviandroid.feature_stock.data.mapper.toDomain
import lt.vitalijus.mymviandroid.feature_stock.data.mapper.toEntity
import lt.vitalijus.mymviandroid.feature_stock.data.remote.StockApi
import lt.vitalijus.mymviandroid.feature_stock.domain.model.Stock
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.StockRepository

class OfflineFirstStockRepository(
    private val api: StockApi,
    private val dao: StockDao
) : StockRepository {

    override fun observeStocks(): Flow<List<Stock>> =
        dao.observeStocks().map { it.map(StockEntity::toDomain) }

    override suspend fun refresh() {
        val remoteStocks = api.fetchStocks()
        dao.insertAll(stocks = remoteStocks.map { it.toEntity() })
    }

    override suspend fun delistRandomStock(): Stock? {
        val stock = dao.getRandomActiveStock()
        if (stock != null) {
            dao.updateDelistStatus(
                stockId = stock.id,
                isDelisted = true
            )
        }
        return stock?.toDomain()
    }
}
