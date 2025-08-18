package club.xiaojiawei.hsscript.initializer

import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.goByLock
import club.xiaojiawei.hsscriptbase.config.log

/**
 * @author 肖嘉威
 * @date 2025/4/1 17:20
 */
class ServiceInitializer : AbstractInitializer() {
    override fun exec() {
        goByLock(ServiceInitializer::class.java) {
            val services = ConfigEnum.entries
                .mapNotNull {
                    if (it.isEnable) it.service else null
                }
                .sortedByDescending { it.priority() }
            for (service in services) {
                service.intelligentStartStop()
            }
            log.info { "正在运行的服务:${services.filter { it.isRunning }}" }
        }
    }

    fun stop() {
        goByLock(ServiceInitializer::class.java) {
            val entries = ConfigEnum.entries
            for (configEnum in entries) {
                if (configEnum.isEnable) {
                    configEnum.service?.stop()
                }
            }
        }
    }

}

