package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.config.StarterConfig
import club.xiaojiawei.hsscript.consts.GAME_CN_NAME
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.LAUNCH_PROGRAM_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.util.isFalse
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import java.util.concurrent.TimeUnit

/**
 * 启动游戏
 * @author 肖嘉威
 * @date 2023/7/5 14:38
 */
class GameStarter : AbstractStarter() {

    public override fun execStart() {
        log.info { "开始检查$GAME_CN_NAME" }
        val gameHWND = ScriptStatus.gameHWND
        if (gameHWND != null && User32.INSTANCE.IsWindow(gameHWND)) {
            next(gameHWND)
            return
        }
        var startTime = System.currentTimeMillis()
        var firstLogLaunch = true
        var firstLogSecondaryLaunch = true
        addTask(
            LAUNCH_PROGRAM_THREAD_POOL.scheduleWithFixedDelay(
                {
                    do {
                        if (startTime == -1L) break
                        val diffTime = System.currentTimeMillis() - startTime
                        if (diffTime > 30_000) {
                            log.warn { "启动${GAME_CN_NAME}失败次数过多，重新执行启动器链" }
                            startTime = -1L
                            EXTRA_THREAD_POOL.schedule({
                                GameUtil.killGame(true)
                                GameUtil.killLoginPlatform()
                                GameUtil.killPlatform()
                                StarterConfig.starter.start()
                            }, 1, TimeUnit.SECONDS)
                            stopTask()
                            break
                        }
                        if (GameUtil.isAliveOfGame()) {
//                    游戏刚启动时可能找不到窗口句柄
                            GameUtil.findGameHWND()?.let {
                                next(it)
                            } ?: let {
                                if (diffTime > 10_000) {
                                    log.info { "${GAME_CN_NAME}已在运行，但未找到对应窗口句柄" }
                                }
                            }
                        } else {
                            if (diffTime > 10_000) {
                                val startupModeEnum = ConfigExUtil.getGameStartupMode().last()
                                if (firstLogSecondaryLaunch) {
                                    firstLogSecondaryLaunch = false
                                    log.info { "以${startupModeEnum.name}方式启动$GAME_CN_NAME" }
                                }
                                startupModeEnum.exec()
                            } else {
                                val startupModeEnum = ConfigExUtil.getGameStartupMode().first()
                                if (firstLogLaunch) {
                                    firstLogLaunch = false
                                    log.info { "以${startupModeEnum.name}方式启动$GAME_CN_NAME" }
                                }
                                startupModeEnum.exec()
                            }
                            Thread.sleep(500)
                        }
                    } while (false)
                },
                100,
                500,
                TimeUnit.MILLISECONDS,
            ),
        )
    }

    private fun next(gameHWND: HWND) {
        updateGameMsg(gameHWND)
        if (ConfigEnum.PREVENT_ADMIN_LAUNCH_GAME.getBoolean() && GameUtil.getGameProgramPermission()
                .isAdministration()
        ) {
            log.warn { "${GAME_CN_NAME}正在以管理员权限运行" }
        } else {
            log.info { GAME_CN_NAME + "正在运行" }
        }
        startNextStarter()
    }

    private fun updateGameMsg(gameHWND: HWND) {
        ScriptStatus.gameHWND = gameHWND
        ScriptStatus.platformHWND = GameUtil.findPlatformHWND()
        GameUtil.updateGameRect()
        go {
            Thread.sleep(3000)
            GameUtil.updateGameRect()
            ConfigUtil.getBoolean(ConfigEnum.UPDATE_GAME_WINDOW).isFalse {
                CSystemDll.INSTANCE.limitWindowResize(gameHWND, true)
            }
        }
    }
}
