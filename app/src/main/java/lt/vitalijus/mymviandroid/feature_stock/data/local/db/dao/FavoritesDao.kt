package lt.vitalijus.mymviandroid.feature_stock.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.model.FavoriteEntity

@Dao
interface FavoritesDao {
    @Query("SELECT stockId FROM FavoriteEntity")
    fun observeFavorites(): Flow<List<String>>

    @Transaction
    suspend fun toggle(id: String) {
        findById(id = id)?.let {
            delete(id = id)
        } ?: insert(FavoriteEntity(stockId = id))
    }

    @Query("SELECT stockId FROM FavoriteEntity WHERE stockId = :id LIMIT 1")
    suspend fun findById(id: String): String?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM FavoriteEntity WHERE stockId = :id")
    suspend fun delete(id: String)
}