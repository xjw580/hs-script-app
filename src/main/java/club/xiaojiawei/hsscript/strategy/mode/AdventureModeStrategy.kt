package club.xiaojiawei.hsscript.strategy.mode

import club.xiaojiawei.hsscript.bean.GameRect
import club.xiaojiawei.hsscript.listener.WorkTimeListener
import club.xiaojiawei.hsscript.listener.log.PowerLogListener
import club.xiaojiawei.hsscript.status.DeckStrategyManager
import club.xiaojiawei.hsscript.status.Mode
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.strategy.AbstractModeStrategy
import club.xiaojiawei.hsscript.strategy.mode.TournamentModeStrategy.BACK_RECT
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.bean.LRunnable
import club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.enums.ModeEnum
import club.xiaojiawei.hsscriptbase.enums.RunModeEnum
import java.util.concurrent.TimeUnit

/**
 * 冒险模式
 * @author 肖嘉威
 * @date 2022/11/25 12:41
 */
object AdventureModeStrategy : AbstractModeStrategy<Any?>() {
    val CHOOSE_RECT: GameRect by lazy { GameRect(0.2467, 0.3441, 0.2778, 0.3772) }
    val PRACTICE_RECT: GameRect by lazy { GameRect(0.1655, 0.4198, -0.4079, -0.3187) }
    val START_RECT: GameRect by lazy { GameRect(0.2564, 0.3452, 0.2690, 0.3728) }
    val FIRST_HERO_RECT by lazy { GameRect(0.1765, 0.4151, -0.4037, -0.3615) }
    val PREV_DECK_PAGE: GameRect by lazy { GameRect(-0.4743, -0.4498, -0.0414, 0.0033) }

    override fun wantEnter() {
        addWantEnterTask(
            EXTRA_THREAD_POOL.scheduleWithFixedDelay(
                {
                    if (PauseStatus.isPause) {
                        cancelAllWantEnterTasks()
                    } else if (Mode.currMode == ModeEnum.HUB) {
                        cancelAllWantEnterTasks()
                        ModeEnum.GAME_MODE.modeStrategy?.wantEnter()
                    } else if (Mode.currMode == ModeEnum.GAME_MODE) {
                        GameModeModeStrategy.enterAdventureMode()
                    } else {
                        cancelAllWantEnterTasks()
                    }
                },
                DELAY_TIME,
                INTERVAL_TIME,
                TimeUnit.MILLISECONDS,
            ),
        )
    }

    override fun afterEnter(t: Any?) {
        if (WorkTimeListener.canWork()) {
            val runMode = DeckStrategyManager.currentRunMode
            if (runMode === RunModeEnum.PRACTICE) {
                if (!runMode.isEnable){
                    log.warn { "${runMode.comment}未启用" }
                    PauseStatus.isPause = false
                    return
                }
                if (!PowerLogListener.checkPowerLogSize()) {
                    return
                }
                PRACTICE_RECT.lClick()
                SystemUtil.delayShort()
                CHOOSE_RECT.lClick()
                SystemUtil.delayMedium()
                PREV_DECK_PAGE.lClick()
                SystemUtil.delayShort()
                GameUtil.lClickDeckPos(2)
                SystemUtil.delayShort()
                START_RECT.lClick()
                SystemUtil.delayShort()
                FIRST_HERO_RECT.lClick()
                SystemUtil.delayShort()
                START_RECT.lClick()
            } else {
//            退出该界面
                addEnteredTask(
                    EXTRA_THREAD_POOL.scheduleWithFixedDelay(
                        LRunnable {
                            if (PauseStatus.isPause) {
                                cancelAllEnteredTasks()
                            } else if (Mode.currMode === ModeEnum.ADVENTURE) {
                                BACK_RECT.lClick()
                            } else {
                                cancelAllEnteredTasks()
                            }
                        },
                        0,
                        1000,
                        TimeUnit.MILLISECONDS,
                    ),
                )
            }
        }
    }
}
