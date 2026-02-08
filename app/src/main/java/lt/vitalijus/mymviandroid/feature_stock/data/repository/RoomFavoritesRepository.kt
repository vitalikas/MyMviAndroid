package lt.vitalijus.mymviandroid.feature_stock.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import lt.vitalijus.mymviandroid.feature_stock.data.local.dao.FavoritesDao
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.FavoritesRepository

class RoomFavoritesRepository(
    private val dao: FavoritesDao
) : FavoritesRepository {

    // Cache layer: Hot Flow shared by all collectors
    private val favoritesCache = dao.observeFavorites()
        .map { it.toSet() }
        .shareIn(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            started = SharingStarted.Lazily,
            replay = 1
        )

    override fun observeFavorites(): Flow<Set<String>> = favoritesCache

    override suspend fun toggleFavorite(id: String) {
        // Atomic transaction - no need to read current state
        dao.toggle(id)
    }
}
