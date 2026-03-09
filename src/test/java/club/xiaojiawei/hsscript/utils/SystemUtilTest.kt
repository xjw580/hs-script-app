package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscriptbase.config.log
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * @author 肖嘉威
 * @date 2026/3/9 9:03
 */
class SystemUtilTest {

    @Test
    @Ignore
    fun testChangeOpacityForGame() {
        GameUtil.findGameHWND()?.let {
            SystemUtil.changeWindowOpacity(it, 50)
        }?:let {
            log.warn { "未找到游戏窗口" }
        }
    }

    @Test
    @Ignore
    fun testChangeOpacityForPlatform() {
        GameUtil.findPlatformHWND()?.let {
            SystemUtil.changeWindowOpacity(it, 50)
        }?:let {
            log.warn { "未找到平台窗口" }
        }
    }

}