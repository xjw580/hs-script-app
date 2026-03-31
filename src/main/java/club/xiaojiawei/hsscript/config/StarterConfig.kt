package club.xiaojiawei.hsscript.config

import club.xiaojiawei.builder.buildStarter
import club.xiaojiawei.hsscript.starter.AbstractStarter

/**
 * Starter的责任链配置
 * @author 肖嘉威
 * @date 2023/7/5 14:48
 */
object StarterConfig {

    val starter: AbstractStarter = buildStarter {
        registerTask()
        clear()
        prepare()
        platform()
        loginPlatform()
        game()
        injectGame()
        injectedAfter()
        logListener()
        exceptionListen()
    }

}
