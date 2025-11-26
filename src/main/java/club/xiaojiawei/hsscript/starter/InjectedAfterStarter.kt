package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import club.xiaojiawei.hsscript.utils.ConfigUtil


/**
 * 启动游戏
 * @author 肖嘉威
 * @date 2023/7/5 14:38
 */
class InjectedAfterStarter : AbstractStarter() {

    override fun execStart() {
        if (ConfigUtil.getInt(ConfigEnum.GAME_LOG_LIMIT) == -1) {
            CSystemDll.INSTANCE.logHook(true)
        }
        if (ConfigExUtil.getMouseControlMode() === MouseControlModeEnum.MESSAGE) {
            CSystemDll.INSTANCE.mouseHook(true)
        }
        if (ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)) {
            CSystemDll.INSTANCE.capture(true)
        }
        if (ConfigUtil.getBoolean(ConfigEnum.LIMIT_MOUSE_RANGE)) {
            CSystemDll.INSTANCE.limitMouseRange(true)
        }
//        if (ConfigEnum.GAME_WINDOW_REDUCTION_FACTOR.service?.getStatus(null) == true) {
//            CSystemDll.INSTANCE.resizeGameWindow(true)
//        }
        startNextStarter()
    }

}
