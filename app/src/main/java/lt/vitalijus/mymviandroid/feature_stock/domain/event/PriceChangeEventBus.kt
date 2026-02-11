package lt.vitalijus.mymviandroid.feature_stock.domain.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event bus for stock price changes using MutableSharedFlow.
 *
 * Many-to-many relationship pattern:
 * - Multiple producers (workers, repositories) can emit events
 * - Multiple consumers (ViewModels, UI components) can collect events
 *
 * Features:
 * - replay = 0: new collectors don't receive old events (signal-like behavior)
 * - extraBufferCapacity = 64: buffers events when collectors are slow
 * *
 * This design follows reactive programming best practices where:
 * - MVI State remains stable (regular Flow for UI state)
 * - Signals are optional (SharedFlow for transient events like animations)
 */
class PriceChangeEventBus {

    /**
     * SharedFlow for price change events.
     * No replay - only active collectors receive events.
     * Buffer allows events to be queued if collector is busy.
     */
    private val _events = MutableSharedFlow<StockPriceChangeEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )

    /**
     * Public read-only SharedFlow for consumers.
     */
    val events: SharedFlow<StockPriceChangeEvent> = _events.asSharedFlow()

    /**
     * Emit a price change event to all active collectors.
     */
    suspend fun emit(event: StockPriceChangeEvent) {
        _events.emit(event)
    }
}
