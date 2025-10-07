package club.xiaojiawei.hsscript.enums

import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.MouseUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import com.sun.jna.platform.win32.WinDef
import java.awt.Point

/**
 * @author 肖嘉威
 * @date 2025/10/7 15:48
 */
enum class GameStartupModeEnum(val exec: () -> Unit) {

    CMD({
        GameUtil.launchPlatformAndGame()
    }),
    MESSAGE({
        val platformHWND = GameUtil.findPlatformHWND()
        val rect = WinDef.RECT()
        SystemUtil.updateRECT(platformHWND, rect)
        MouseUtil.leftButtonClick(
            Point(145, rect.bottom - rect.top - 150),
            platformHWND,
            MouseControlModeEnum.MESSAGE.code,
        )
        SystemUtil.delayShort()
        MouseUtil.leftButtonClick(
            Point(145, rect.bottom - rect.top - 130),
            platformHWND,
            MouseControlModeEnum.MESSAGE.code,
        )
    }),

    ;


}