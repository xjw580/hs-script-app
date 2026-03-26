package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.listener.WorkTimeListener
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.ConfigUtil
import com.sun.jna.platform.win32.WinDef.HWND
import javafx.beans.value.ChangeListener

/**
 * @author 肖嘉威
 * @date 2026/3/25 17:30
 */
object DisplayMouseTrackService : Service<Boolean>() {
    private val hwndListener: ChangeListener<HWND?> by lazy {
        ChangeListener<HWND?> { _, _, hwnd ->
            syncMouseTrackStatus()
        }
    }
    private val workStatusListener: ChangeListener<Boolean> by lazy {
        ChangeListener<Boolean> { _, _, working ->
            syncMouseTrackStatus(working)
        }
    }

    private fun syncMouseTrackStatus(status: Boolean = ConfigUtil.getBoolean(ConfigEnum.DISPLAY_MOUSE_TRACK)) {
        CSystemDll.INSTANCE.showMouseTrack(status)
    }

    override fun execStart(): Boolean {
        ScriptStatus.gameHWNDProperty().addListener(hwndListener)
        WorkTimeListener.addWorkStatusListener(workStatusListener)
        syncMouseTrackStatus()
        return true
    }

    override fun execStop(): Boolean {
        ScriptStatus.gameHWNDProperty().removeListener(hwndListener)
        WorkTimeListener.removeWorkStatusListener(workStatusListener)
        CSystemDll.INSTANCE.showMouseTrack(false)
        return true
    }

    override fun getStatus(value: Boolean?): Boolean =
        value ?: ConfigUtil.getBoolean(ConfigEnum.DISPLAY_MOUSE_TRACK)

    override fun execValueChanged(oldValue: Boolean, newValue: Boolean) {
        CSystemDll.INSTANCE.showMouseTrack(newValue)
    }
}
