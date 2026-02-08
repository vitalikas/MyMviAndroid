package lt.vitalijus.mymviandroid.feature_stock.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import lt.vitalijus.mymviandroid.feature_stock.presentation.mvi.StockAction
import lt.vitalijus.mymviandroid.feature_stock.presentation.mvi.StockStore

class StockViewModel(
    storeFactory: StockStore.Factory
) : ViewModel() {

    private val store = storeFactory.create(scope = viewModelScope)

    val state = store.state

    fun dispatch(action: StockAction) {
        store.dispatch(action = action)
    }
}
