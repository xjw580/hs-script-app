package club.xiaojiawei.hsscript.status

import club.xiaojiawei.hsscript.bean.PlayerBehavior
import club.xiaojiawei.hsscript.bean.single.WarEx
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptcardsdk.bean.Player

/**
 * @author 肖嘉威
 * @date 2025/8/11 17:07
 */
object PlayerBehaviorStatus {

    val rivalBehavior by lazy {
        PlayerBehavior(Player.UNKNOWN_PLAYER)
    }

    val meBehavior by lazy {
        PlayerBehavior(Player.UNKNOWN_PLAYER)
    }

    fun resetBehaviour() {
        meBehavior.reset(WarEx.war.me)
        rivalBehavior.reset(WarEx.war.rival)
    }

    fun checkRivalRobot() {
        log.info { "计算对面" }
        if (rivalBehavior.updateRobotProbability() < 0.1) {
            log.info { "发现对面这个b疑似真人，准备投降，gameId:${WarEx.war.rival.gameId}" }
            GameUtil.surrender()
        }
        log.info { "对面是脚本概率:" + rivalBehavior.robotProbability }
    }

    fun checkMeRobot() {
//        log.info { "计算我方" }
        if (meBehavior.updateRobotProbability() < 0.1) {
//            log.info { "发现自己这个b疑似真人，准备投降，gameId:${war.me.gameId}" }
//            GameUtil.surrender()
        }
//        log.info { "我是脚本概率:" + meBehavior.robotProbability }
    }

}