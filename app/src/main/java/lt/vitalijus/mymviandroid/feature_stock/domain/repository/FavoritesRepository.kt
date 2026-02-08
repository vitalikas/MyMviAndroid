package lt.vitalijus.mymviandroid.feature_stock.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(): Flow<Set<String>>
    suspend fun toggleFavorite(id: String)
}
