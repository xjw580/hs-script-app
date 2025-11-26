package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.consts.*
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.config.log


/**
 * 启动游戏
 * @author 肖嘉威
 * @date 2023/7/5 14:38
 */
class InjectStarter : AbstractStarter() {

    override fun execStart() {
        val mouseControlMode = ConfigExUtil.getMouseControlMode()
        val acHook = ConfigUtil.getBoolean(ConfigEnum.PREVENT_AC)
        val mouseHook = mouseControlMode === MouseControlModeEnum.MESSAGE
        val logHook = ConfigUtil.getInt(ConfigEnum.GAME_LOG_LIMIT) == -1
        val limitMouseRange = ConfigUtil.getBoolean(ConfigEnum.LIMIT_MOUSE_RANGE)
        val autoRefreshGameTask = ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)
//        val reductionGameWindowSize = ConfigEnum.GAME_WINDOW_REDUCTION_FACTOR.service?.getStatus(null) == true

        log.info { "鼠标控制模式：${mouseControlMode.name}" }
        log.info { "阻止游戏反作弊：$acHook" }
        if (mouseHook ||
            acHook ||
            limitMouseRange ||
            autoRefreshGameTask ||
            logHook
//            reductionGameWindowSize
        ) {
            if (ScriptStatus.gameHWND == null || !injectCheck()) {
                pause()
                return
            }
            val delay = 200L
            val maxRetry = 10_000 / delay
            var retryI = 0
            while (!CSystemDll.INSTANCE.isConnected() && retryI++ < maxRetry) {
                Thread.sleep(delay)
            }
        } else {
            log.info { "无需注入" }
        }
        startNextStarter()
    }

    private fun injectCheck(): Boolean {
        if (ConfigEnum.ALLOW_GAME_INJECT.getBoolean()) {
            val pid = CSystemDll.INSTANCE.findProcessId(GAME_PROGRAM_NAME, true)
            if (pid > 0) {
                if (CSystemDll.INSTANCE.isDllLoadedInProcess(pid, LIB_HS_FILE.name)) return true
            } else {
                return false
            }
            val injectFile = SystemUtil.getExeFilePath(INJECT_UTIL_FILE) ?: return false
            val dllFile = if (ConfigEnum.ENABLE_CONSOLE_HOTKEY.getBoolean()) {
                SystemUtil.getDllFilePath(LIB_HS_FILE)
            } else SystemUtil.getDllFilePath(LIB_HS_BASE_FILE) ?: SystemUtil.getDllFilePath(LIB_HS_FILE)
            dllFile ?: return false
            return InjectUtil.execInject(injectFile.absolutePath, dllFile.absolutePath, "$GAME_US_NAME.exe")
        } else {
            val text = "已禁用${GAME_CN_NAME}注入，如有需要请到开发者选项中打开"
            SystemUtil.notice(text)
            log.warn { text }
            return true
        }
    }
}
