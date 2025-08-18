package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.ConfigUtil

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:20
 */
object AutoStartService : Service<Boolean>() {

    override fun execStart(): Boolean {
        if (ConfigUtil.getBoolean(ConfigEnum.POWER_BOOT)) {
            return CSystemDll.SystemPart.enablePowerBoot(true, true)
        }
        return false
    }

    override fun execStop(): Boolean {
        if (ConfigUtil.getBoolean(ConfigEnum.POWER_BOOT)) {
            return CSystemDll.SystemPart.enablePowerBoot(true, false)
        }
        return false
    }

    override fun getStatus(value: Boolean?): Boolean {
        return value ?: ConfigUtil.getBoolean(ConfigEnum.POWER_BOOT) && ConfigUtil.getBoolean(ConfigEnum.AUTO_START)
    }

}
