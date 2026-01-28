package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.NetUtil
import club.xiaojiawei.hsscriptbase.config.log

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:20
 */
object UseProxyService : Service<Boolean>() {

    override fun execStart(): Boolean {
        return true
    }

    override fun execStop(): Boolean {
        return true
    }

    override fun getStatus(value: Boolean?): Boolean {
        val status = value ?: ConfigUtil.getBoolean(ConfigEnum.USE_PROXY)
        if (status) {
            log.info { "代理地址:${NetUtil.getSystemProxy()}" }
        } else {
            log.info { "不使用代理" }
        }
        return status
    }
}
