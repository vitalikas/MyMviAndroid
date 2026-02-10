package lt.vitalijus.mymviandroid.core.log

import kotlin.reflect.KClass

interface Logger {
    // Auto TAG from class
    fun d(clazz: KClass<*>, message: String)
    fun e(clazz: KClass<*>, message: String)
    fun v(clazz: KClass<*>, message: String)

    // Custom TAG
    fun d(tag: String, message: String)
    fun e(tag: String, message: String)
    fun v(tag: String, message: String)

    // Categorized TAG (e.g., "Analytics-Worker", "Analytics-PartialState")
    fun d(category: LogCategory, clazz: KClass<*>, message: String)
    fun e(category: LogCategory, clazz: KClass<*>, message: String)
    fun v(category: LogCategory, clazz: KClass<*>, message: String)
}

enum class LogCategory(val prefix: String) {
    WORKER("Analytics-Worker"),
    PARTIAL_STATE("Analytics-PartialState"),
    EFFECT("Analytics-Effect"),
    ACTION("Analytics-Action"),
    ANALYTICS("Analytics")
}
