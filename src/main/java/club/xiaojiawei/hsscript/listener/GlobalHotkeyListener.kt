package club.xiaojiawei.hsscript.listener

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import com.melloware.jintellitype.HotkeyListener
import com.melloware.jintellitype.JIntellitype
import com.melloware.jintellitype.JIntellitypeConstants

/**
 * 热键监听器
 * @author 肖嘉威
 * @date 2022/12/11 11:23
 */
object GlobalHotkeyListener : HotkeyListener {

    private const val HOT_KEY_EXIT = 111

    private const val HOT_KEY_PAUSE = 222

    private const val HOT_KEY_CONSOLE = 333

    init {
        JIntellitype.getInstance().addHotKeyListener(this)
    }

    fun reload() {
        unregister()
        register()
    }

    private fun register() {
        if (JIntellitype.isJIntellitypeSupported()) {
            ConfigExUtil.getExitHotKey()?.let {
                if (it.keyCode != 0) {
                    JIntellitype.getInstance()
                        .registerHotKey(HOT_KEY_EXIT, it.modifier, it.keyCode)
                    log.info { "退出热键：$it" }
                }
            }
            ConfigExUtil.getPauseHotKey()?.let {
                if (it.keyCode != 0) {
                    JIntellitype.getInstance()
                        .registerHotKey(HOT_KEY_PAUSE, it.modifier, it.keyCode)
                    log.info { "开始/暂停热键：$it" }
                }
            }
            JIntellitype.getInstance()
                .registerHotKey(HOT_KEY_CONSOLE, JIntellitypeConstants.MOD_ALT, 'A'.code)
        } else {
            log.warn { "当前系统不支持设置热键" }
        }
    }

    private fun unregister() {
        if (JIntellitype.isJIntellitypeSupported()) {
            JIntellitype.getInstance().unregisterHotKey(HOT_KEY_PAUSE)
            JIntellitype.getInstance().unregisterHotKey(HOT_KEY_EXIT)
        }
    }

    /**
     * 快捷键组合键按键事件
     * @param i
     */
    override fun onHotKey(i: Int) {
        when (i) {
            HOT_KEY_EXIT -> {
                SystemUtil.notice("捕捉到热键，关闭程序")
                log.info { "捕捉到热键，关闭程序" }
                unregister()
                SystemUtil.shutdownSoft()
            }

            HOT_KEY_PAUSE -> {
                if (!PauseStatus.isPause) {
                    log.info { "捕捉到热键,停止脚本" }
                    Thread.ofVirtual().name("Pause VThread").start {
                        PauseStatus.isPause = true
                    }
                } else {
                    log.info { "捕捉到热键,开始脚本" }
                    Thread.ofVirtual().name("Start VThread").start {
                        PauseStatus.isPause = false
                    }
                }
            }

            HOT_KEY_CONSOLE -> {
                CSystemDll.INSTANCE.developer(true)
            }
        }
    }

    val launch: Unit by lazy {
        register()
    }

}