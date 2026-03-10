package club.xiaojiawei.hsscript.strategy

import club.xiaojiawei.hsscript.consts.*
import club.xiaojiawei.hsscript.enums.BlockTypeEnum
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.interfaces.closer.ThreadCloser
import club.xiaojiawei.hsscript.status.TaskManager
import club.xiaojiawei.hsscript.utils.BlockNode
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.EntityNode
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.PowerLogParser.*
import club.xiaojiawei.hsscript.utils.PowerNode
import club.xiaojiawei.hsscript.utils.SystemNode
import club.xiaojiawei.hsscript.utils.TagChangeNode
import club.xiaojiawei.hsscript.utils.TagNode
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.enums.WarPhaseEnum
import club.xiaojiawei.hsscriptbase.interfaces.PhaseStrategy
import club.xiaojiawei.hsscriptbase.util.isTrue
import club.xiaojiawei.hsscriptcardsdk.status.WAR

/**
 * 游戏阶段抽象类
 * @author 肖嘉威
 * @date 2022/11/26 17:59
 */
abstract class AbstractPhaseStrategy : PhaseStrategy {

    protected val war = WAR

    override fun deal(line: String) {
        // This method is kept for compatibility with PhaseStrategy interface if needed
    }

    open fun deal(node: PowerNode) {
        dealing = true
        try {
            beforeDeal()
            when (node) {
                is BlockNode -> {
                    if (dealBlockIsOver(node)) return
                    node.children.forEach { deal(it) }
                    dealBlockEndIsOver(node)
                }
                is EntityNode -> {
                    when (node.type) {
                        "SHOW_ENTITY" -> dealShowEntityThenIsOver(node)
                        "FULL_ENTITY" -> dealFullEntityThenIsOver(node)
                        "CHANGE_ENTITY" -> dealChangeEntityThenIsOver(node)
                    }
                }
                is TagChangeNode -> {
                    dealTagChangeThenIsOver(node)
                }
                is SystemNode -> {
                    dealSystemNode(node)
                }
                is TagNode -> {}
            }
            afterDeal()
        } finally {
            dealing = false
        }
    }

    protected open fun dealSystemNode(node: SystemNode): Boolean {
        return false
    }

    protected fun beforeDeal() {
        WarPhaseEnum.find(this)?.let {
            log.info { "当前处于：" + it.comment }
        }
    }

    protected fun afterDeal() {
        WarPhaseEnum.find(this)?.let {
            log.info { it.comment + " -> 结束" }
        }
    }

    protected open fun dealTagChangeThenIsOver(node: TagChangeNode): Boolean {
        return false
    }

    protected open fun dealShowEntityThenIsOver(node: EntityNode): Boolean {
        return false
    }

    protected open fun dealFullEntityThenIsOver(node: EntityNode): Boolean {
        return false
    }

    protected open fun dealChangeEntityThenIsOver(node: EntityNode): Boolean {
        return false
    }

    protected open fun dealBlockIsOver(node: BlockNode): Boolean {
        return false
    }

    protected open fun dealBlockEndIsOver(node: BlockNode): Boolean {
        if (ConfigUtil.getBoolean(ConfigEnum.KILLED_SURRENDER) && !WAR.isMyTurn) {
            if (node.type === BlockTypeEnum.ATTACK || node.type === BlockTypeEnum.POWER) {
                GameUtil.triggerCalcMyDeadLine()
            }
        }
        return false
    }

    companion object : ThreadCloser {

        init {
            TaskManager.addTask(this)
        }

        var dealing = false
        private val tasks: MutableList<Thread> = mutableListOf()

        fun addTask(task: Thread) {
            tasks.add(task)
        }

        fun cancelAllTask() {
            val toList = tasks.toList()
            tasks.clear()
            toList.forEach {
                it.isAlive.isTrue {
                    it.interrupt()
                }
            }
        }

        override fun stopAll() {
            cancelAllTask()
        }
    }

}
