package club.xiaojiawei.hsscript.strategy.phase

import club.xiaojiawei.hsscript.bean.Behavior
import club.xiaojiawei.hsscript.bean.DeckStrategyThread
import club.xiaojiawei.hsscript.bean.OutCardThread
import club.xiaojiawei.hsscript.bean.log.Block
import club.xiaojiawei.hsscript.bean.log.TagChangeEntity
import club.xiaojiawei.hsscript.enums.BlockTypeEnum
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.TagEnum
import club.xiaojiawei.hsscript.status.PlayerBehaviorStatus
import club.xiaojiawei.hsscript.strategy.AbstractPhaseStrategy
import club.xiaojiawei.hsscript.strategy.DeckStrategyActuator
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.enums.StepEnum
import club.xiaojiawei.hsscriptbase.util.isTrue
import club.xiaojiawei.hsscriptcardsdk.bean.isValid

/**
 * 游戏回合阶段
 *
 * @author 肖嘉威
 * @date 2022/11/26 17:24
 */
object GameTurnPhaseStrategy : AbstractPhaseStrategy() {

    override fun dealTagChangeThenIsOver(line: String, tagChangeEntity: TagChangeEntity): Boolean {
        if (tagChangeEntity.tag == TagEnum.STEP) {
            if (tagChangeEntity.value == StepEnum.MAIN_ACTION.name) {
                if (war.me === war.currentPlayer && war.me.isValid()) {
                    log.info { "我方回合" }
                    cancelAllTask()
                    war.isMyTurn = true
                    if (ConfigUtil.getBoolean(ConfigEnum.ONLY_ROBOT)) {
                        PlayerBehaviorStatus.checkRivalRobot()
                    }
                    // 异步执行出牌策略，以便监听出牌后的卡牌变动
                    (OutCardThread {
                        (ConfigUtil.getBoolean(ConfigEnum.RANDOM_EMOTION) && war.me.turn == 0).isTrue {
                            GameUtil.sendGreetEmoji()
                            SystemUtil.delayShortMedium()
                        }
                        val start = System.currentTimeMillis()
                        DeckStrategyActuator.outCard()
                        if (ConfigUtil.getBoolean(ConfigEnum.KILLED_SURRENDER)) {
                            GameUtil.triggerCalcMyDeadLine()
                        }
                        if (ConfigUtil.getBoolean(ConfigEnum.RANDOM_EMOTION) && System.currentTimeMillis() - start > 60_000) {
                            GameUtil.sendErrorEmoji()
                        }
                    }.also { addTask(it) }).start()
                } else {
                    log.info { "对方回合" }
                    war.isMyTurn = false
                    cancelAllTask()
                    if (ConfigUtil.getBoolean(ConfigEnum.ONLY_ROBOT)) {
                        PlayerBehaviorStatus.checkMeRobot()
                    }
                    ConfigUtil.getBoolean(ConfigEnum.RANDOM_EMOTION).isTrue {
                        DeckStrategyActuator.randEmoji()
                    }
                    if (ConfigUtil.getBoolean(ConfigEnum.RANDOM_EVENT)) {
                        (DeckStrategyThread({
                            DeckStrategyActuator.randomDoSomething()
                        }, "Random Do Something Thread").also { addTask(it) }).start()
                    }
                }
            } else if (tagChangeEntity.value == StepEnum.MAIN_END.name) {
                war.isMyTurn = false
                cancelAllTask()
            }
        }
        return false
    }


    override fun dealBlockIsOver(line: String, block: Block): Boolean {
        if (ConfigUtil.getBoolean(ConfigEnum.ONLY_ROBOT)) {
            if (block.blockType === BlockTypeEnum.ATTACK || block.blockType === BlockTypeEnum.PLAY) {
                val behavior = Behavior(block.blockType)
                if (war.currentPlayer == war.me) {
                    PlayerBehaviorStatus.meBehavior.add(behavior)
                } else if (war.currentPlayer == war.rival) {
                    PlayerBehaviorStatus.rivalBehavior.add(behavior)
                }
            }
        }
        return false
    }
}
