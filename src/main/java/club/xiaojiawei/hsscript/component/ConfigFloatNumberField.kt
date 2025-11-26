package club.xiaojiawei.hsscript.component

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.controls.NumberField
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.service.Service
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.putFloat
import club.xiaojiawei.hsscript.utils.runUI
import club.xiaojiawei.hsscriptbase.util.isFalse

/**
 * @author 肖嘉威
 * @date 2025/3/24 17:46
 */
open class ConfigFloatNumberField : NumberField() {
    var config: ConfigEnum? = null
        set(value) {
            isDisable = value?.isEnable != true
            isEditable = value?.isEnable == true
            value?.let {
                promptText = it.defaultValue
                text = ConfigUtil.getString(it)
            }
            field = value
        }

    var notificationManager: NotificationManager<Any>? = null

    private var interceptChange = false

    init {
        textProperty().addListener { _, oldValue, newValue ->
            config?.let {
                interceptChange.isFalse {
                    val newValueI = newValue.toFloatOrNull()
                    if (newValueI != null) {
                        val oldValueI = oldValue.toFloatOrNull() ?: newValueI
                        statusChangeCallback(oldValueI, newValueI)
                    }
                }
            }
        }
    }

    protected open fun statusChangeCallback(
        oldValue: Float,
        newValue: Float,
    ) {
        config?.putFloat(newValue)
        var res = true
        config?.service?.let { service ->
            res = (service as Service<Float>).intelligentStartStop(newValue)
            service.valueChanged(oldValue, newValue)
        }
        if (res) {
            notificationManager?.let { nm ->
                runUI {
                    nm.showSuccess("设置成功", 1)
                }
            }
        } else {
            config?.let {
                it.putFloat(oldValue)
                interceptChange = true
                text = oldValue.toString()
                interceptChange = false
            }
            notificationManager?.let { nm ->
                runUI {
                    nm.showError("设置失败", 3)
                }
            }
        }
    }
}
