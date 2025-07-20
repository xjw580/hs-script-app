package club.xiaojiawei.hsscript.component

import club.xiaojiawei.hsscriptcardsdk.CardPlugin
import club.xiaojiawei.hsscriptstrategysdk.StrategyPlugin
import club.xiaojiawei.hsscriptpluginsdk.bean.PluginWrapper
import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.controls.ico.FailIco
import club.xiaojiawei.controls.ico.OKIco
import club.xiaojiawei.hsscript.bean.Release
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.control.CheckBox
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.text.Text

/**
 * @author 肖嘉威
 * @date 2024/9/24 13:51
 */
class PluginItem(
    val pluginWrapper: PluginWrapper<*>,
    var notificationManager: NotificationManager<*>? = null,
) : HBox() {
    @FXML
    private lateinit var enable: CheckBox

    @FXML
    private lateinit var name: Text

    @FXML
    private lateinit var type: Label

    @FXML
    private lateinit var author: Text

    @FXML
    private lateinit var version: Text

    @FXML
    private lateinit var cardSDKVersion: Label

    @FXML
    private lateinit var strategySDKVersion: Label

    init {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/component/PluginItem.fxml"))
        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)
        fxmlLoader.load<Any>()
        afterLoaded()
    }

    private fun afterLoaded() {
        name.text = pluginWrapper.plugin.name()
        val minimumCompatibleVersion: String
        if (pluginWrapper.plugin is StrategyPlugin) {
            type.text = "策略"
            type.styleClass.add("label-ui-success")
            minimumCompatibleVersion = StrategyPlugin.MINIMUM_COMPATIBLE_VERSION.removeSuffix("v")
        } else {
            type.text = "卡牌"
            type.styleClass.add("label-ui-normal")
            minimumCompatibleVersion = CardPlugin.MINIMUM_COMPATIBLE_VERSION.removeSuffix("v")
        }

        author.text = pluginWrapper.plugin.author()
        version.text = pluginWrapper.plugin.version()

        val cardSDKVersionStr = pluginWrapper.plugin.cardSDKVersion()
        cardSDKVersion.contentDisplay = ContentDisplay.RIGHT
        cardSDKVersionStr?.let { sdk ->
            cardSDKVersion.isVisible = true
            cardSDKVersion.isManaged = true
            if (sdk.isBlank() ||
                Release.compareVersion(
                    minimumCompatibleVersion,
                    sdk.removeSuffix("v"),
                ) > 0
            ) {
                cardSDKVersion.styleClass.add("label-ui-error")
                cardSDKVersion.graphic =
                    FailIco().apply {
                        scaleX = 0.8
                        scaleY = 0.8
                    }
                cardSDKVersion.tooltip = Tooltip("卡牌SDK版本不兼容，最低为$minimumCompatibleVersion，可能无法正常使用")
            } else {
                cardSDKVersion.styleClass.add("label-ui-success")
                cardSDKVersion.graphic =
                    OKIco().apply {
                        scaleX = 0.8
                        scaleY = 0.8
                    }
                cardSDKVersion.tooltip = Tooltip("卡牌SDK版本兼容")
            }
            cardSDKVersion.text = sdk.ifBlank { "版本号错误" }
        } ?: let {
            cardSDKVersion.isVisible = false
            cardSDKVersion.isManaged = false
        }

        val strategySDKVersionStr = pluginWrapper.plugin.strategySDKVersion()
        strategySDKVersion.contentDisplay = ContentDisplay.RIGHT
        strategySDKVersionStr?.let { sdk ->
            strategySDKVersion.isVisible = true
            strategySDKVersion.isManaged = true
            if (sdk.isBlank() ||
                Release.compareVersion(
                    minimumCompatibleVersion,
                    sdk.removeSuffix("v"),
                ) > 0
            ) {
                strategySDKVersion.styleClass.add("label-ui-error")
                strategySDKVersion.graphic =
                    FailIco().apply {
                        scaleX = 0.8
                        scaleY = 0.8
                    }
                strategySDKVersion.tooltip = Tooltip("策略SDK版本不兼容，最低为$minimumCompatibleVersion，可能无法正常使用")
            } else {
                strategySDKVersion.styleClass.add("label-ui-success")
                strategySDKVersion.graphic =
                    OKIco().apply {
                        scaleX = 0.8
                        scaleY = 0.8
                    }
                strategySDKVersion.tooltip = Tooltip("策略SDK版本兼容")
            }
            strategySDKVersion.text = sdk.ifBlank { "版本号错误" }
        } ?: let {
            strategySDKVersion.isVisible = false
            strategySDKVersion.isManaged = false
        }


        enable.selectedProperty().bindBidirectional(pluginWrapper.enabledProperty())
        enable.selectedProperty().addListener { _, _, enable ->
            notificationManager?.showSuccess(
                "已${if (enable) "启用" else "禁用"}${name.text}",
                2,
            )

            val isDeck = pluginWrapper.plugin is StrategyPlugin

            val disableList =
                if (isDeck) {
                    ConfigExUtil.getDeckPluginDisabled()
                } else {
                    ConfigExUtil.getCardPluginDisabled()
                }

            if (enable) {
                disableList.remove(pluginWrapper.plugin.id())
            } else {
                disableList.add(pluginWrapper.plugin.id())
            }

            if (isDeck) {
                ConfigExUtil.storeDeckPluginDisabled(disableList)
            } else {
                ConfigExUtil.storeCardPluginDisabled(disableList)
            }
        }
    }
}
