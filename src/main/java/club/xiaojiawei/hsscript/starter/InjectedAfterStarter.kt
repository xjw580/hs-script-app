package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.getBoolean
import club.xiaojiawei.hsscript.utils.getInt


/**
 * 启动游戏
 * @author 肖嘉威
 * @date 2023/7/5 14:38
 */
class InjectedAfterStarter : AbstractStarter() {

    override fun execStart() {
        if (ConfigEnum.GAME_LOG_LIMIT.getInt() == -1) {
            CSystemDll.INSTANCE.logHook(true)
        }
        if (ConfigExUtil.getMouseControlMode() === MouseControlModeEnum.MESSAGE) {
            CSystemDll.INSTANCE.mouseHook(true)
        }
        if (ConfigEnum.AUTO_REFRESH_GAME_TASK.getBoolean()) {
            CSystemDll.INSTANCE.capture(true)
        }
        if (ConfigEnum.LIMIT_MOUSE_RANGE.getBoolean()) {
            CSystemDll.INSTANCE.limitMouseRange(true)
        }
        val displayMouseTrack = ConfigEnum.DISPLAY_MOUSE_TRACK.getBoolean()
        if (displayMouseTrack || ConfigEnum.DISPLAY_GAME_RECT_POS.getBoolean()) {
            CSystemDll.INSTANCE.presentDraw(true)
            if (displayMouseTrack){
                CSystemDll.INSTANCE.showMouseTrack(true)
            }
        }
//        if (ConfigEnum.GAME_WINDOW_REDUCTION_FACTOR.service?.getStatus(null) == true) {
//            CSystemDll.INSTANCE.resizeGameWindow(true)
//        }
        startNextStarter()
    }

}
