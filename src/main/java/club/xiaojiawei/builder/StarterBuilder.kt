package club.xiaojiawei.builder

import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.starter.*
import club.xiaojiawei.hsscript.status.TaskManager
import club.xiaojiawei.hsscript.utils.ConfigUtil

/**
 * @author 肖嘉威
 * @date 2026/3/26 14:27
 */
class StarterBuilder {

    companion object {
        private val emptyStarter: AbstractStarter = object : AbstractStarter() {
            override fun execStart() {

            }
        }

        fun isEmpty(starter: AbstractStarter): Boolean {
            return starter === emptyStarter
        }
    }

    private var tailStarter: AbstractStarter? = null
    private var headStarter: AbstractStarter = emptyStarter

    private fun addStarter(starter: AbstractStarter) {
        if (headStarter == emptyStarter) {
            headStarter = starter
            tailStarter = starter
        } else {
            tailStarter?.setNextStarter(starter)
            tailStarter = starter
        }
        if (registerTask) {
            TaskManager.addTask(starter)
        }
    }

    private var registerTask = false

    fun registerTask() {
        registerTask = true
    }

    fun unregisterTask() {
        registerTask = false
    }

    fun clear() = addStarter(ClearStarter())

    fun exceptionListen() = addStarter(ExceptionListenStarter())

    fun game() = addStarter(GameStarter())

    fun injectedAfter() = addStarter(InjectedAfterStarter())

    fun injectGame(enableDebug: () -> Boolean = { ConfigUtil.getBoolean(ConfigEnum.ENABLE_CONSOLE_HOTKEY) }) =
        addStarter(InjectGameStarter(enableDebug))

    fun injectPlatform() = addStarter(InjectPlatformStarter())

    fun loginPlatform() = addStarter(LoginPlatformStarter())

    fun logListener() = addStarter(LogListenStarter())

    fun platform() = addStarter(PlatformStarter())

    fun prepare() = addStarter(PrepareStarter())

    fun custom(starter: AbstractStarter?) {
        starter?.let {
            addStarter(it)
        }
    }

    fun clearBuilder() {
        headStarter = emptyStarter
        tailStarter = null
    }

    fun build(): AbstractStarter {
        return headStarter
    }

}

fun buildStarter(config: StarterBuilder.() -> Unit): AbstractStarter = StarterBuilder().apply(config).build()

fun buildInjectStarter(
    lastStarter: AbstractStarter? = null,
    enableDebug: () -> Boolean = { ConfigUtil.getBoolean(ConfigEnum.ENABLE_CONSOLE_HOTKEY) },
): AbstractStarter = buildStarter {
    platform()
    game()
    injectGame(enableDebug)
    custom(lastStarter)
}