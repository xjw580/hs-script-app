package club.xiaojiawei.hsscript.enums

import club.xiaojiawei.hsscript.consts.GAME_CN_NAME
import club.xiaojiawei.hsscript.consts.GAME_PROGRAM_NAME
import club.xiaojiawei.hsscript.consts.PLATFORM_CN_NAME
import club.xiaojiawei.hsscript.utils.CMDUtil
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.MouseUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscript.utils.getString
import com.sun.jna.platform.win32.WinDef
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import java.awt.Point
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * @author 肖嘉威
 * @date 2025/10/7 15:48
 */
enum class GameStartupModeEnum(val comment: String, val introduction: String, val exec: () -> Unit) {

//    GAME_ARG(
//        GAME_CN_NAME,
//        "通过向${GAME_CN_NAME}传递参数的方式启动",
//        {
//            Path(ConfigEnum.GAME_PATH.getString(), GAME_PROGRAM_NAME).let {
//                it.exists().ifTrue {
//                    CMDUtil.directExec(it.toString(), "-launch", "-uid", "hs_beta")
//                }
//            }
//        }),

    PLATFORM_ARG(
        "${PLATFORM_CN_NAME}参数",
        "通过向${PLATFORM_CN_NAME}传递参数的方式启动",
        {
            GameUtil.launchPlatformAndGame()
        }),

    PLATFORM_MESSAGE(
        "${PLATFORM_CN_NAME}消息",
        "通过模拟鼠标消息点击${PLATFORM_CN_NAME}窗口里的进入游戏按钮的方式启动",
        {
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