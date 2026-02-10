package lt.vitalijus.mymviandroid.feature_stock.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import lt.vitalijus.mymviandroid.core.analytics.LogCategory
import lt.vitalijus.mymviandroid.core.analytics.Logger
import lt.vitalijus.mymviandroid.feature_stock.domain.model.MarketState
import lt.vitalijus.mymviandroid.feature_stock.domain.repository.MarketRepository

class MarketToggleWorker(
    context: Context,
    params: WorkerParameters,
    private val repository: MarketRepository,
    private val logger: Logger
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Get current state before toggle
        val previousState = repository.observeMarketState().first()

        // Toggle to new state
        repository.toggleMarketState()

        val transitionText = when (previousState) {
            MarketState.OPEN -> "📉 OPEN → CLOSED 🚫 (Trading stopped)"
            MarketState.CLOSED -> "📈 CLOSED → OPEN ✅ (Trading resumed)"
        }

        logger.d(
            LogCategory.WORKER,
            MarketToggleWorker::class,
            "🔄 Market state toggled: $transitionText"
        )

        return Result.success()
    }
}
