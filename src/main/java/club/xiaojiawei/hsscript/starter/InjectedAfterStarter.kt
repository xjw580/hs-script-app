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
class InjectedAfterStarter : AbstractStarter() {

    override fun execStart() {
        if (ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)) {
            CSystemDll.INSTANCE.capture(true)
        }
        if (ConfigExUtil.getMouseControlMode() === MouseControlModeEnum.MESSAGE) {
            CSystemDll.INSTANCE.mouseHook(true)
        }
        if (ConfigUtil.getBoolean(ConfigEnum.LIMIT_MOUSE_RANGE)) {
            CSystemDll.INSTANCE.limitMouseRange(true)
        }
        startNextStarter()
    }

}
