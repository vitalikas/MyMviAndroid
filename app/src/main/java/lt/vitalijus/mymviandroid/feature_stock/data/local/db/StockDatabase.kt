package lt.vitalijus.mymviandroid.feature_stock.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.FavoritesDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.StockDao
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.FavoriteEntity
import lt.vitalijus.mymviandroid.feature_stock.data.local.model.StockEntity

@Database(
    entities = [StockEntity::class, FavoriteEntity::class],
    version = 2
)
abstract class StockDatabase : RoomDatabase() {
    abstract fun stockDao(): StockDao
    abstract fun favoritesDao(): FavoritesDao

    class SeedCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed initial data
            scope.launch {
                val currentTime = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT INTO StockEntity (id, name, price, updatedAt, isDelisted, dailyChangePercent) VALUES
                    ('AAPL', 'Apple Inc.', 178.50, $currentTime, 0, 2.5),
                    ('GOOGL', 'Alphabet Inc.', 141.80, $currentTime, 0, -1.2),
                    ('MSFT', 'Microsoft Corp.', 420.55, $currentTime, 0, 0.8),
                    ('AMZN', 'Amazon.com Inc.', 178.25, $currentTime, 0, -6.5),
                    ('TSLA', 'Tesla Inc.', 242.84, $currentTime, 0, 5.2),
                    ('META', 'Meta Platforms', 522.33, $currentTime, 0, -8.1),
                    ('NVDA', 'NVIDIA Corp.', 875.28, $currentTime, 0, 12.3),
                    ('NFLX', 'Netflix Inc.', 701.35, $currentTime, 0, -3.4)
                """.trimIndent()
                )
            }
        }
    }
}
