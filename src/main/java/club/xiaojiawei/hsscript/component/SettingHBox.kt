package club.xiaojiawei.hsscript.component

import club.xiaojiawei.hsscript.bean.SettingItem
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.WindowEnum
import club.xiaojiawei.hsscript.service.SettingsSearchService
import club.xiaojiawei.hsscript.utils.getBoolean
import club.xiaojiawei.kt.dsl.config
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.DefaultProperty
import javafx.beans.value.ChangeListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.util.Duration

/**
 * 设置项组件
 * @author 肖嘉威
 * @date 2026/1/28
 */
@DefaultProperty("content")
class SettingHBox : HBox() {

    private var sceneListener: ChangeListener<Scene>? = null

    init {
        config {
            styleClass("h-box")
            alignCenter()

            addText {
                nameText = instance()
            }
            addHSpacer()

            addHBox {
                contentBox = instance()
                alignRight()
                spacing(10.0)
            }
        }
        if (ConfigEnum.ENABLE_SETTINGS_SEARCH_SERVICE.getBoolean()){
            sceneListener = ChangeListener<Scene> { _, _, newScene ->
                if (newScene != null) {
                    registerSettingItem()
                    sceneProperty().removeListener(sceneListener)
                    sceneListener = null
                }
            }
            sceneProperty().addListener(sceneListener)
        }
    }

    var name: String
        get() = nameText.text
        set(value) {
            nameText.text = value
        }

    var tip: String
        get() = ensureTipNode().text
        set(value) {
            ensureTipNode().apply {
                text = value
                if (value.isNotBlank()) {
                    isVisible = true
                    isManaged = true
                }
            }
        }

    var beta: Boolean
        get() = ensureBetaTag().isVisible
        set(value) {
            ensureBetaTag().apply {
                isVisible = value
                isManaged = value
            }
        }

    /*注册到搜索服务*/
    var registerSearchService: Boolean = true

    /** 所属选项卡 */
    var tabWindow: WindowEnum? = null

    /** 所属分组 */
    var group: String = ""

    /** 关联的设置项对象 */
    var settingItem: SettingItem? = null

    private lateinit var nameText: Text
    private var tipNode: TipNode? = null
    private var betaTag: BetaTag? = null
    private lateinit var contentBox: HBox

    /** 高亮动画 */
    private var highlightTimeline: Timeline? = null

    /** 高亮颜色 */
    private val highlightColor = Color.rgb(248, 218, 154, 1.0)

    val content: ObservableList<Node> = contentBox.children

    private fun ensureTipNode(): TipNode {
        return tipNode ?: let {
            TipNode().apply {
                tipNode = this
                isVisible = false
                isManaged = false
                this@SettingHBox.children.add(1, this)
            }
        }
    }

    private fun ensureBetaTag(): BetaTag {
        return betaTag ?: let {
            BetaTag().apply {
                betaTag = this
                isVisible = false
                isManaged = false
                val insertAfter = tipNode ?: nameText
                val index = this@SettingHBox.children.indexOf(insertAfter) + 1
                this@SettingHBox.children.add(index, this)
            }
        }
    }

    /**
     * 高亮动画效果
     */
    fun highlight() {
        highlightTimeline?.stop()
        val originalBackground = background
        highlightTimeline = Timeline(
            KeyFrame(
                Duration.ZERO,
                KeyValue(backgroundProperty(), Background(BackgroundFill(highlightColor, CornerRadii(4.0), null)))
            ),
            KeyFrame(
                Duration.millis(200.0),
                KeyValue(backgroundProperty(), Background(BackgroundFill(highlightColor, CornerRadii(4.0), null)))
            ),
            KeyFrame(
                Duration.millis(400.0),
                KeyValue(backgroundProperty(), originalBackground)
            ),
            KeyFrame(
                Duration.millis(600.0),
                KeyValue(backgroundProperty(), Background(BackgroundFill(highlightColor, CornerRadii(4.0), null)))
            ),
            KeyFrame(
                Duration.millis(800.0),
                KeyValue(backgroundProperty(), originalBackground)
            ),
            KeyFrame(
                Duration.millis(1000.0),
                KeyValue(backgroundProperty(), Background(BackgroundFill(highlightColor, CornerRadii(4.0), null)))
            ),
            KeyFrame(
                Duration.millis(1200.0),
                KeyValue(backgroundProperty(), originalBackground)
            )
        ).apply {
            play()
        }
    }

    /**
     * 注册设置项到搜索服务
     */
    private fun registerSettingItem() {
        if (registerSearchService) {
            tabWindow?.let {
                val item = SettingItem(
                    name = name,
                    description = tip,
                    tabWindow = it,
                    groupName = group,
                    node = this
                )
                settingItem = item
                SettingsSearchService.register(item)
            }
        }
    }
}
