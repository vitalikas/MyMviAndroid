package lt.vitalijus.mymviandroid.feature_stock.data.local.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class StockEntity(
    @PrimaryKey val id: String,
    val name: String,
    val price: Double,
    val updatedAt: Long,
    val isDelisted: Boolean = false,
    val dailyChangePercent: Double = 0.0
)
