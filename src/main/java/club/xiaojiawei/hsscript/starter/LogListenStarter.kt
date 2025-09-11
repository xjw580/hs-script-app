package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.config.LogListenerConfig

/**
 * 初始化和启动日志监听器
 * @author 肖嘉威
 * @date 2023/9/20 17:22
 */
class LogListenStarter : AbstractStarter() {
    override fun execStart() {
        Thread.ofVirtual().name(Thread.currentThread().name.replace("Thread", "VThread")).start {
            LogListenerConfig.logListener.listen()
        }
        startNextStarter()
    }
}
