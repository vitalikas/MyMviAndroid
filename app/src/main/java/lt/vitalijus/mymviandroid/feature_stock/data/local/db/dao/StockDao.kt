package lt.vitalijus.mymviandroid.feature_stock.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.model.StockEntity

@Dao
interface StockDao {
    @Query("SELECT * FROM StockEntity")
    fun observeStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAllStocks(stocks: List<StockEntity>)

    @Query("UPDATE StockEntity SET isDelisted = :isDelisted WHERE id = :stockId")
    suspend fun updateStockDelistStatus(stockId: String, isDelisted: Boolean)

    @Query("SELECT * FROM StockEntity WHERE isDelisted = 0 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomActiveStock(): StockEntity?

    @Query("UPDATE StockEntity SET price = :price, updatedAt = :timestamp WHERE id = :stockId")
    suspend fun updateStockPrice(
        stockId: String,
        price: Double,
        timestamp: Long = System.currentTimeMillis()
    )
}