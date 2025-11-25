package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.SCREEN_HEIGHT
import club.xiaojiawei.hsscript.listener.WorkTimeListener
import club.xiaojiawei.hsscript.starter.InjectStarter
import club.xiaojiawei.hsscript.starter.InjectedAfterStarter
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.GameUtil
import com.sun.jna.platform.win32.WinDef.HWND
import javafx.beans.value.ChangeListener

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:20
 */
object GameWindowReductionFactorService : Service<Float>() {
    private val windowChangeListener: ChangeListener<HWND?> by lazy {
        ChangeListener<HWND?> { _, _, newValue ->
            if (WorkTimeListener.working) {
                changeWindowSize(newValue)
            }
        }
    }

    private val workingChangeListener: ChangeListener<Boolean> by lazy {
        ChangeListener { _, _, newValue ->
            if (newValue) {
                changeWindowSize(ScriptStatus.gameHWND)
            }
        }
    }

    override fun execStart(): Boolean {
//        InjectStarter().start()
//        CSystemDll.INSTANCE.resizeGameWindow(true)
        if (WorkTimeListener.working) {
            changeWindowSize(ScriptStatus.gameHWND)
        }
        ScriptStatus.gameHWNDProperty().addListener(windowChangeListener)
        WorkTimeListener.addChangeListener(workingChangeListener)
        return true
    }

    override fun execStop(): Boolean {
//        CSystemDll.INSTANCE.resizeGameWindow(false)
        ScriptStatus.gameHWNDProperty().removeListener(windowChangeListener)
        WorkTimeListener.removeChangeListener(workingChangeListener)
        return true
    }

    override fun getStatus(value: Float?): Boolean =
        (value ?: ConfigUtil.getFloat(ConfigEnum.GAME_WINDOW_REDUCTION_FACTOR)) > 0

    private fun changeWindowSize(
        hwnd: HWND?,
        scale: Float = ConfigUtil.getFloat(ConfigEnum.GAME_WINDOW_REDUCTION_FACTOR),
    ) {
        hwnd ?: return
        if (scale < 1) return
        val height = SCREEN_HEIGHT / scale
        GameUtil.legalizationGameWindowSize(height.toInt())
    }

    override fun execValueChanged(
        oldValue: Float,
        newValue: Float,
    ) {
        changeWindowSize(ScriptStatus.gameHWND, newValue)
    }
}
