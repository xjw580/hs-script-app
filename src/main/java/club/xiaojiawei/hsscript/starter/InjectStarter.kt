package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.consts.GAME_PROGRAM_NAME
import club.xiaojiawei.hsscript.consts.GAME_US_NAME
import club.xiaojiawei.hsscript.consts.INJECT_UTIL_FILE
import club.xiaojiawei.hsscript.consts.LIB_HS_FILE
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
        val limitMouseRange = ConfigUtil.getBoolean(ConfigEnum.LIMIT_MOUSE_RANGE)
        val autoRefreshGameTask = ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)

        log.info { "鼠标控制模式：${mouseControlMode.name}" }
        log.info { "阻止游戏反作弊：$acHook" }
        if (mouseHook ||
            acHook ||
            limitMouseRange ||
            autoRefreshGameTask
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
        val pid = CSystemDll.INSTANCE.findProcessId(GAME_PROGRAM_NAME, true)
        if (pid > 0) {
            if (CSystemDll.INSTANCE.isDllLoadedInProcess(pid, LIB_HS_FILE.name)) return true
        }else {
            return false
        }
        val injectFile = SystemUtil.getExeFilePath(INJECT_UTIL_FILE) ?: return false
        val dllFile = SystemUtil.getDllFilePath(LIB_HS_FILE) ?: return false
        return InjectUtil.execInject(injectFile.absolutePath, dllFile.absolutePath, "$GAME_US_NAME.exe")
    }
}
