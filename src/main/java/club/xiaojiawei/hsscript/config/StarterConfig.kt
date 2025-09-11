package club.xiaojiawei.hsscript.config

import club.xiaojiawei.hsscript.starter.*
import club.xiaojiawei.hsscript.status.TaskManager

/**
 * Starter的责任链配置
 * @author 肖嘉威
 * @date 2023/7/5 14:48
 */
object StarterConfig {

    val starter: AbstractStarter = ClearStarter().also {
        TaskManager.addTask(it)
        it.setNextStarter(PrepareStarter())
            .setNextStarter(PlatformStarter().apply { TaskManager.addTask(this) })
            .setNextStarter(LoginPlatformStarter().apply { TaskManager.addTask(this) })
            .setNextStarter(GameStarter().apply { TaskManager.addTask(this) })
            .setNextStarter(InjectStarter())
            .setNextStarter(InjectedAfterStarter())
            .setNextStarter(LogListenStarter())
            .setNextStarter(ExceptionListenStarter().apply { TaskManager.addTask(this) })
    }

}
