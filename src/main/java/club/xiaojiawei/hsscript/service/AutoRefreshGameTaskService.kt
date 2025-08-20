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
object AutoRefreshGameTaskService : Service<Boolean>() {
    private val changeListener: ChangeListener<Boolean> by lazy {
        ChangeListener { _, _, inWar ->

        }
    }

    override fun execStart(): Boolean {
        WarEx.inWarProperty.addListener(changeListener)
        return true
    }

    override fun execStop(): Boolean {
        WarEx.inWarProperty.removeListener(changeListener)
        return true
    }

    override fun getStatus(value: Boolean?): Boolean {
        return value ?: ConfigUtil.getBoolean(ConfigEnum.ONLY_ROBOT)
    }
}
