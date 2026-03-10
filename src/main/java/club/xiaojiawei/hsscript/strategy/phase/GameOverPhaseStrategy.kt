package club.xiaojiawei.hsscript.strategy.phase

import club.xiaojiawei.hsscript.bean.log.Block
import club.xiaojiawei.hsscript.bean.log.ExtraEntity
import club.xiaojiawei.hsscript.bean.log.TagChangeEntity
import club.xiaojiawei.hsscript.bean.single.WarEx
import club.xiaojiawei.hsscript.listener.log.PowerLogListener
import club.xiaojiawei.hsscript.strategy.AbstractPhaseStrategy
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscript.utils.GameUtil.addGameEndTask
import java.io.IOException

/**
 * 游戏结束阶段
 * @author 肖嘉威
 * @date 2022/11/27 13:44
 */
object GameOverPhaseStrategy : AbstractPhaseStrategy() {
    override fun dealTagChangeThenIsOver(node: TagChangeNode): Boolean {
        over()
        return true
    }

    override fun dealShowEntityThenIsOver(node: EntityNode): Boolean {
        over()
        return true
    }

    override fun dealFullEntityThenIsOver(node: EntityNode): Boolean {
        over()
        return true
    }

    override fun dealChangeEntityThenIsOver(node: EntityNode): Boolean {
        over()
        return true
    }

    override fun dealBlockIsOver(node: BlockNode): Boolean {
        over()
        return true
    }

    override fun dealBlockEndIsOver(node: BlockNode): Boolean {
        over()
        return true
    }

    override fun dealOtherThenIsOver(line: String): Boolean {
        over()
        return true
    }

    private fun over() {
        war.isMyTurn = false
        cancelAllTask()
        WarEx.endWar()
        try {
            SystemUtil.delay(1000)
            val accessFile = PowerLogListener.logFile
            accessFile?.seek(accessFile.length())
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        addGameEndTask()
        WarEx.reset()
    }
}
