package lt.vitalijus.mymviandroid.core.analytics

import android.util.Log

class LogcatAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String) {
        Log.d("Analytics", event)
    }
}