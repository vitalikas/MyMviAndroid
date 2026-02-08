package lt.vitalijus.mymviandroid.feature_stock.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.FavoritesDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.FavoriteEntity
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.StockEntity

@Database(
    entities = [StockEntity::class, FavoriteEntity::class],
    version = 1
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun favoritesDao(): FavoritesDao

    class SeedCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            scope.launch(Dispatchers.IO) {
                // Seed initial data
                db.execSQL("""
                    INSERT INTO StockEntity (id, name, price, updatedAt) VALUES
                    ('AAPL', 'Apple Inc.', 178.50, ${System.currentTimeMillis()}),
                    ('GOOGL', 'Alphabet Inc.', 141.80, ${System.currentTimeMillis()}),
                    ('MSFT', 'Microsoft Corp.', 420.55, ${System.currentTimeMillis()}),
                    ('AMZN', 'Amazon.com Inc.', 178.25, ${System.currentTimeMillis()}),
                    ('TSLA', 'Tesla Inc.', 242.84, ${System.currentTimeMillis()}),
                    ('META', 'Meta Platforms', 522.33, ${System.currentTimeMillis()}),
                    ('NVDA', 'NVIDIA Corp.', 875.28, ${System.currentTimeMillis()}),
                    ('NFLX', 'Netflix Inc.', 701.35, ${System.currentTimeMillis()})
                """.trimIndent())
            }
        }
    }
}
