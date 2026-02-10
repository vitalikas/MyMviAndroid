package lt.vitalijus.mymviandroid.feature_stock.presentation.util

import java.text.NumberFormat
import java.util.Locale

fun formatPrice(price: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(price)
}
