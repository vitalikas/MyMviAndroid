package lt.vitalijus.mymviandroid.core.analytics

import android.util.Log
import kotlin.reflect.KClass

class LogcatLogger : Logger {
    // Auto TAG from class
    override fun d(clazz: KClass<*>, message: String) {
        Log.d(clazz.simpleName, message)
    }

    override fun e(clazz: KClass<*>, message: String) {
        Log.e(clazz.simpleName, message)
    }

    override fun v(clazz: KClass<*>, message: String) {
        Log.v(clazz.simpleName, message)
    }

    // Custom TAG
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

    // Categorized TAG
    override fun d(category: LogCategory, clazz: KClass<*>, message: String) {
        Log.d("${category.prefix}-${clazz.simpleName}", message)
    }

    override fun e(category: LogCategory, clazz: KClass<*>, message: String) {
        Log.e("${category.prefix}-${clazz.simpleName}", message)
    }

    override fun v(category: LogCategory, clazz: KClass<*>, message: String) {
        Log.v("${category.prefix}-${clazz.simpleName}", message)
    }
}