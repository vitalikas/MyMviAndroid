package lt.vitalijus.mymviandroid.feature_stock.data.local.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FavoriteEntity(
    @PrimaryKey val stockId: String
)
