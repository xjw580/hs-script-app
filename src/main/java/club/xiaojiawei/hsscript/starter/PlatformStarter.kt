package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.consts.PLATFORM_CN_NAME
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.getBoolean
import club.xiaojiawei.hsscriptbase.config.LAUNCH_PROGRAM_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import java.util.concurrent.TimeUnit

/**
 * 启动战网
 * @author 肖嘉威
 * @date 2023/7/5 14:39
 */
class PlatformStarter : AbstractStarter() {

    public override fun execStart() {
        if (GameUtil.isAliveOfGame()) {
            next()
            return
        }
        if (GameUtil.isAliveOfPlatform()) {
            log.info { PLATFORM_CN_NAME + "正在运行" }
//            GameUtil.hidePlatformWindow()
        } else {
            log.info { "启动$PLATFORM_CN_NAME" }
            GameUtil.launchPlatformAndGame()
        }

        var startTime = System.currentTimeMillis()
        addTask(
            LAUNCH_PROGRAM_THREAD_POOL.scheduleWithFixedDelay({
                if (System.currentTimeMillis() - startTime >= 10 * 1000 && !GameUtil.isAliveOfPlatform()) {
                    startTime = System.currentTimeMillis()
                    GameUtil.killPlatform()
                    GameUtil.killLoginPlatform()
                    log.info { "${PLATFORM_CN_NAME}可能被关闭，启动中" }
                    GameUtil.launchPlatformAndGame()
                }
                next()
            }, 1, 50, TimeUnit.MILLISECONDS)
        )
    }

    private fun next() {
        if (GameUtil.findPlatformHWND() != null || GameUtil.findLoginPlatformHWND() != null) {
            if (ConfigEnum.PREVENT_ADMIN_LAUNCH_GAME.getBoolean() && GameUtil.getPlatformProgramPermission()
                    .isAdministration()
            ) {
                log.warn { "${PLATFORM_CN_NAME}正在以管理员权限运行" }
            }
        }
        startNextStarter()
    }
}
