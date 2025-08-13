package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.ConfigUtil

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:20
 */
object PowerBootService : Service<Boolean>() {

    override fun execStart(): Boolean {
        return CSystemDll.SystemPart.enablePowerBoot(true)
    }

    override fun execStop(): Boolean {
        return CSystemDll.SystemPart.enablePowerBoot(false)
    }

    override fun getStatus(value: Boolean?): Boolean {
        return value ?: let {
            val res = CSystemDll.SystemPart.isTaskExists()
            ConfigUtil.putBoolean(ConfigEnum.POWER_BOOT, res)
            res
        }
    }
}
