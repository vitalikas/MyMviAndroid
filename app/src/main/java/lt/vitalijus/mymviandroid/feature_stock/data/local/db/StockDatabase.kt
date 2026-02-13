package lt.vitalijus.mymviandroid.feature_stock.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.dao.FavoritesDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.model.FavoriteEntity
import lt.vitalijus.mymviandroid.feature_stock.data.local.db.model.StockEntity

@Database(
    entities = [StockEntity::class, FavoriteEntity::class],
    version = 1
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun favoritesDao(): FavoritesDao
}
