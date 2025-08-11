package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.bean.single.WarEx
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.status.PlayerBehaviorStatus
import club.xiaojiawei.hsscript.utils.ConfigUtil
import javafx.beans.value.ChangeListener

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:08
 */
object OnlyRobotService : Service<Boolean>() {
    private val changeListener: ChangeListener<Boolean> by lazy {
        ChangeListener { _, _, inWar ->
            PlayerBehaviorStatus.resetBehaviour()
        }
    }

    override fun execStart(): Boolean {
        PlayerBehaviorStatus.resetBehaviour()
        WarEx.inWarProperty.addListener(changeListener)
        return true
    }

    override fun execStop(): Boolean {
        PlayerBehaviorStatus.resetBehaviour()
        WarEx.inWarProperty.removeListener(changeListener)
        return true
    }

    override fun execIntelligentStartStop(value: Boolean?): Boolean {
        return if (ConfigUtil.getBoolean(ConfigEnum.ONLY_ROBOT)) {
            execStart()
        } else {
            execStop()
        }
    }
}
