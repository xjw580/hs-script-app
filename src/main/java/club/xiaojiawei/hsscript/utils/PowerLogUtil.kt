package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscript.consts.*
import club.xiaojiawei.hsscript.core.Core.restart
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscriptbase.config.log
import java.io.*

/**
 * 解析power.log日志的工具，非常非常非常重要
 * @author 肖嘉威
 * @date 2022/11/28 23:12
 */
object PowerLogUtil {

    fun isRelevance(l: String): Boolean {
        var flag = false
        if (l.contains("Truncating log")) {
            val text = "${GAME_WAR_LOG_NAME}达到" + (ScriptStatus.maxLogSizeKB) + "KB，游戏停止输出日志，准备重启游戏"
            log.info { text }
            SystemUtil.notice(text)
            restart()
        } else {
            flag = l.contains("PowerTaskList")
        }
        club.xiaojiawei.hsscript.core.Core.lastActiveTime = System.currentTimeMillis()
        return flag
    }

    fun formatLogFile(logDir: String, renew: Boolean): File? {
        val sourceFile = File("$logDir\\${GAME_WAR_LOG_NAME}")
        var res: File? = null
        if (sourceFile.exists()) {
            val newFile = File("$logDir\\renew_${GAME_WAR_LOG_NAME}")
            runCatching {
                BufferedReader(FileReader(sourceFile)).use { reader ->
                    BufferedWriter(FileWriter(newFile)).use { writer ->
                        reader.lineSequence()
                            .filter { it.contains("PowerTaskList") }
                            .map { it.replace("PowerTaskList.Debug", "") + "\n" }
                            .forEach { writer.write(it) }
                    }
                }
            }.onFailure {
                log.error { it }
            }.onSuccess {
                if (renew) {
                    res = newFile
                } else {
                    newFile.renameTo(sourceFile)
                    res = sourceFile
                }
            }
        } else {
            log.error { "${sourceFile}不存在" }
        }
        return res
    }
}
