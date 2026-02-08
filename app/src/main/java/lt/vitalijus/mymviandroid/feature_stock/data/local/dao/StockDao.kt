package lt.vitalijus.mymviandroid.feature_stock.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.StockEntity

@Dao
interface StockDao {
    @Query("SELECT * FROM StockEntity")
    fun observeStocks(): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stocks: List<StockEntity>)
}
