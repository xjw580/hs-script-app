package club.xiaojiawei.hsscript.strategy.mode

import club.xiaojiawei.hsscript.bean.GameRect
import club.xiaojiawei.hsscript.strategy.AbstractModeStrategy
import club.xiaojiawei.hsscriptbase.bean.LRunnable
import club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL
import java.util.concurrent.TimeUnit

/**
 * 抽奖界面
 * @author 肖嘉威
 * @date 2022/11/25 12:27
 */
object LuckyDrawModeStrategy : AbstractModeStrategy<Any?>() {

    private val BACK_RECT = GameRect(-0.5000, -0.4313, 0.4113, 0.4694)


    override fun wantEnter() {
    }

    override fun afterEnter(t: Any?) {
        addEnteredTask(EXTRA_THREAD_POOL.scheduleWithFixedDelay(
            LRunnable { BACK_RECT.lClick() },
            DELAY_TIME,
            4000,
            TimeUnit.MILLISECONDS
        ))
    }

}
