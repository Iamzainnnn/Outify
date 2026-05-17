package cc.tomko.outify.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CapturedException(
    val message: String,
    val threadName: String,
    val stackTrace: String,
    val timestamp: String,
)

@Singleton
class ExceptionCollector @Inject constructor() {
    private val _exceptions = mutableListOf<CapturedException>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    val exceptions: List<CapturedException>
        get() = _exceptions.toList()

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            capture(thread.name, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun capture(threadName: String, throwable: Throwable) {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()

        _exceptions.add(
            CapturedException(
                message = throwable.message ?: throwable.javaClass.simpleName,
                threadName = threadName,
                stackTrace = sw.toString(),
                timestamp = dateFormat.format(Date()),
            )
        )
    }
}
