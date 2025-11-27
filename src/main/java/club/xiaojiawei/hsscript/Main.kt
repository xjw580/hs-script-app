package club.xiaojiawei.hsscript

import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.spi.FilterReply
import club.xiaojiawei.hsscript.consts.ARG_AOT
import club.xiaojiawei.hsscript.consts.PROGRAM_NAME
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.WindowUtil
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinError
import javafx.application.Application
import org.slf4j.LoggerFactory
import java.io.File


/**
 * @author 肖嘉威
 * @date 2024/10/14 17:42
 */
private fun setLogPath() {
    try {
        val context = LoggerFactory.getILoggerFactory()
        if (context is LoggerContext) {
            val logbackConfigFile = File("logback.xml")
            if (logbackConfigFile.exists()) {
                val configurator = JoranConfigurator()
                configurator.context = context
                context.reset()
                configurator.doConfigure(logbackConfigFile)
            }

            val appender = context.getLogger("ROOT").getAppender("file_async")
            if (appender is AsyncAppender) {
                for (iteratorForAppender in appender.iteratorForAppenders()) {
                    if (iteratorForAppender.name == "file") {
                        iteratorForAppender.addFilter(object : ThresholdFilter() {
                            override fun decide(iLoggingEvent: ILoggingEvent): FilterReply {
                                return if (iLoggingEvent.level.toInt() >= ScriptStatus.fileLogLevel) FilterReply.ACCEPT else FilterReply.DENY
                            }
                        })
                        break
                    }
                }
            }
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun createProgramLock(): Boolean {
    val name = "${PROGRAM_NAME}.lock"

    val h = Kernel32.INSTANCE.CreateMutex(null, true, name)

    return when (Kernel32.INSTANCE.GetLastError()) {
        WinError.ERROR_ALREADY_EXISTS -> false
        else -> true
    }
}

fun main(args: Array<String>) {
    System.setProperty("jna.library.path", "lib")
    ScriptStatus.aotMode = args.any { it.startsWith(ARG_AOT) }

    if (!createProgramLock() && !ScriptStatus.aotMode){
        WindowUtil.hideLaunchPage()
        return
    }

    setLogPath()

    ScriptStatus.programArgs = args.toList()

    Application.launch(MainApplication::class.java, *args)
}
