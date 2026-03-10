package club.xiaojiawei.hsscript.strategy.phase

import club.xiaojiawei.hsscript.utils.BlockNode
import club.xiaojiawei.hsscript.utils.TagChangeNode
import club.xiaojiawei.hsscript.utils.toTagChangeEntity

/**
 * 游戏回合阶段
 *
 * @author 肖嘉威
 * @date 2022/11/26 17:24
 */
object GameTurnPhaseStrategy : AbstractPhaseStrategy() {

    override fun dealTagChangeThenIsOver(node: TagChangeNode): Boolean {
        val tagChangeEntity = node.toTagChangeEntity()
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


    override fun dealBlockIsOver(node: BlockNode): Boolean {
        if (ConfigUtil.getBoolean(ConfigEnum.ONLY_ROBOT)) {
            if (node.type === BlockTypeEnum.ATTACK || node.type === BlockTypeEnum.PLAY) {
                val behavior = Behavior(node.type)
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
