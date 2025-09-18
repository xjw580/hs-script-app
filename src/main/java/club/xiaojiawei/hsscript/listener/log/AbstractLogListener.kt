package club.xiaojiawei.hsscript.listener.log

import club.xiaojiawei.hsscript.bean.DiskLogFile
import club.xiaojiawei.hsscript.bean.MemoryLogFile
import club.xiaojiawei.hsscript.dll.LogReader
import club.xiaojiawei.hsscript.enums.GameLogModeEnum
import club.xiaojiawei.hsscript.interfaces.LogFile
import club.xiaojiawei.hsscript.interfaces.closer.ScheduledCloser
import club.xiaojiawei.hsscript.listener.WorkTimeListener
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.status.TaskManager
import club.xiaojiawei.hsscript.utils.FileUtil
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscriptbase.config.LISTEN_LOG_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.util.isFalse
import java.io.File
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 在 [LogListenerConfig.logListener] 添加新的LogListener
 * @author 肖嘉威
 * @date 2023/9/20 16:54
 */
abstract class AbstractLogListener(
    protected var logFileName: String,
    protected var listenInitialDelay: Long,
    protected var listenPeriod: Long,
    protected var listenTimeUnit: TimeUnit
) : ScheduledCloser {

    init {
        TaskManager.addTask(this)
    }

    var logFile: LogFile? = null
        private set

    private var logScheduledFuture: ScheduledFuture<*>? = null

    var nextLogListener: AbstractLogListener? = null

    fun setNextLogListener(nextLogListener: AbstractLogListener): AbstractLogListener {
        return nextLogListener.also { this.nextLogListener = it }
    }

    protected abstract fun dealOldLog()

    protected abstract fun dealNewLog()

    private fun listenNextListener() {
        nextLogListener?.listen()
    }

    private fun waitLogCreated(maxWaitMillisTime: Long = 15_000): LogFile? {
        doWhileBlock@ do {
            val start = System.currentTimeMillis()
            if (ScriptStatus.gameLogMode === GameLogModeEnum.DISK) {
                log.info { "等待创建【${logFileName}】日志" }
                var latestLogDir = GameUtil.getLatestLogDir()

                while (true) {
                    latestLogDir?.listFiles()?.let {
                        for (file in it) {
                            if (FileUtil.isFileLocked(file.absolutePath)) {
                                val createLogFile = createLogFile(latestLogDir)
                                log.info { "已创建游戏【${logFileName}】日志, $createLogFile" }
                                return DiskLogFile(createLogFile.absolutePath)
                            }
                        }
                    }
                    if (PauseStatus.isPause) {
                        break@doWhileBlock
                    }
                    if (System.currentTimeMillis() - start > maxWaitMillisTime) {
                        break@doWhileBlock
                    }
                    Thread.sleep(50)
                    latestLogDir = GameUtil.getLatestLogDir()
                }
            } else if (ScriptStatus.gameLogMode === GameLogModeEnum.MEMORY) {
                log.info { "等待创建游戏【${logFileName}】日志缓冲区" }
                if (!LogReader.nativeInit()){
                    log.error { "日志读取器初始化失败" }
                    break@doWhileBlock
                }
                while (!LogReader.existChannel(logFileName)) {
                    if (PauseStatus.isPause) {
                        break@doWhileBlock
                    }
                    if (System.currentTimeMillis() - start > maxWaitMillisTime) {
                        break@doWhileBlock
                    }
                    Thread.sleep(50)
                }
                log.info { "已创建游戏【${logFileName}】日志缓冲区" }
                return MemoryLogFile(logFileName)
            } else {
                log.error { "不支持的日志模式: ${ScriptStatus.gameLogMode}" }
            }
        } while (false)
        return null
    }

    fun listen() {
        synchronized(this) {
            logScheduledFuture?.let {
                if (!it.isDone) {
                    log.warn { logFileName + "正在被监听，无法再次被监听" }
                    listenNextListener()
                    return
                }
            }
            closeLogFile()
            val waitLogFile = waitLogCreated()
            if (waitLogFile == null){
                log.error { "$logFileName 日志创建失败" }
                PauseStatus.isPause = true
                return
            }else{
                logFile = waitLogFile
            }
            log.info { "开始监听日志: $logFileName" }
            try {
                dealOldLog()
            } catch (e: Exception) {
                log.error(e) {}
                return
            }
            logScheduledFuture = LISTEN_LOG_THREAD_POOL.scheduleWithFixedDelay({
                if (PauseStatus.isPause || !WorkTimeListener.working) {
                    stopAll()
                } else {
                    try {
                        dealNewLog()
                    } catch (e: InterruptedException) {
                        log.warn(e) { logFileName + "监听中断" }
                    } catch (e: Exception) {
                        log.error(e) { logFileName + "监听发生错误" }
                    }
                }
            }, listenInitialDelay, listenPeriod, listenTimeUnit)
            listenNextListener()
        }
    }

    private fun createLogFile(logPath: File): File {
        val logFile = logPath.resolve(logFileName)
        logFile.createNewFile()
        return logFile
    }

    private fun closeLogFile() {
        synchronized(this) {
            logFile?.let {
                it.close()
                logFile = null
            }
        }
    }

    private fun closeLogListener() {
        synchronized(this) {
            logScheduledFuture?.let {
                it.isDone.isFalse {
                    it.cancel(true)
                }
            }
        }
    }

    override fun stopAll() {
        closeLogListener()
    }

}
