package club.xiaojiawei.hsscript.starter

import ch.qos.logback.classic.Level
import club.xiaojiawei.hsscript.consts.GAME_MODE_LOG_NAME
import club.xiaojiawei.hsscript.controller.javafx.MainController
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.GameLogModeEnum
import club.xiaojiawei.hsscript.enums.WindowEnum
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.util.isFalse

/**
 * 发出警告
 * @author 肖嘉威
 * @date 2023/7/6 10:46
 */

class PrepareStarter : AbstractStarter() {

    private fun checkConfig() {
        val notificationManager =
            WindowUtil.getController(WindowEnum.MAIN)?.let {
                it as MainController
                it.getNotificationManagerInstance()
            }
        val closeTime = 10L
        runUI {
            var text = ""
            ConfigUtil.getBoolean(ConfigEnum.ENABLE_MOUSE).isFalse {
                text = "启用鼠标处于关闭状态！！！"
                log.warn { text }
                SystemUtil.notice(text, forceNotify = true)
            }
            ConfigUtil.getBoolean(ConfigEnum.STRATEGY).isFalse {
                text = "执行策略处于关闭状态！！！"
                log.warn { text }
                SystemUtil.notice(text, forceNotify = true)
            }
            if (ConfigExUtil.getFileLogLevel() === Level.OFF) {
                text = "日志处于关闭状态！！！"
                log.warn { text }
                notificationManager?.showWarn(text, "", closeTime)
            }
        }
    }

    private fun checkGameLogMode() {
        ScriptStatus.gameLogMode = if (ConfigUtil.getInt(ConfigEnum.GAME_LOG_LIMIT) < 0) {
            GameUtil.getLatestLogDir()?.let {
                val modeLog = it.resolve(GAME_MODE_LOG_NAME)
                if (modeLog.exists() && FileUtil.isFileLocked(modeLog.absolutePath) && modeLog.length() < 200) {
                    GameLogModeEnum.DISK
                } else GameLogModeEnum.MEMORY
            } ?: GameLogModeEnum.MEMORY
        } else GameLogModeEnum.DISK
        log.info { "游戏日志读取模式: ${ScriptStatus.gameLogMode}" }
    }

    override fun execStart() {
        checkGameLogMode()
        checkConfig()
        startNextStarter()
    }
}
