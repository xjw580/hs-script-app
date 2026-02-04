package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.ProgressModal
import club.xiaojiawei.hsscript.bean.SettingItem
import club.xiaojiawei.hsscript.component.SettingHBox
import club.xiaojiawei.hsscript.component.SettingsSearchField
import club.xiaojiawei.hsscript.controller.javafx.settings.view.SettingsView
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.WindowEnum
import club.xiaojiawei.hsscript.interfaces.StageHook
import club.xiaojiawei.hsscript.utils.WindowUtil
import club.xiaojiawei.hsscript.utils.getBoolean
import club.xiaojiawei.kt.ext.runUILater
import javafx.application.Platform
import javafx.beans.property.DoubleProperty
import javafx.beans.value.ObservableValue
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import java.net.URL
import java.util.*

/**
 * @author 肖嘉威
 * @date 2023/10/14 12:43
 */
class SettingsController : SettingsView(), Initializable, StageHook {

    private var progress: DoubleProperty? = null

    /*确保在initialize中调用*/
    private val windowMap by lazy {
        mapOf(
            buildWindowPair(WindowEnum.INIT_SETTINGS, initTab),
            buildWindowPair(WindowEnum.ADVANCED_SETTINGS, advancedTab),
            buildWindowPair(WindowEnum.PLUGIN_SETTINGS, pluginTab),
            buildWindowPair(WindowEnum.STRATEGY_SETTINGS, strategyTab),
            buildWindowPair(WindowEnum.CARD_GROUP_SETTINGS, cardGroupTab),
            buildWindowPair(WindowEnum.DEVELOPER_SETTINGS, developerTab),
            buildWindowPair(WindowEnum.ABOUT, aboutTab),
        )
    }

    private fun buildWindowPair(windowEnum: WindowEnum, tab: Tab): Pair<WindowEnum, Tab> {
        return windowEnum to tab.apply { properties[windowKey] = windowEnum }
    }

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        windowMap
        val selectedItem = tabPane.selectionModel.selectedItem
        if (selectedItem != null) {
            loadTab(selectedItem)
        }
        tabPane.selectionModel.selectedItemProperty()
            .addListener { observable: ObservableValue<out Tab>?, oldValue: Tab?, newValue: Tab ->
                loadTab(newValue)
            }

        initSearch()
    }

    private fun initSearch() {
        if (ConfigEnum.ENABLE_SETTINGS_SEARCH_SERVICE.getBoolean()) {
            progress = progressModal.show("搜索服务启用中...")
            searchField.onItemSelected = { item ->
                navigateToSetting(item)
            }

            // Ctrl+F 快捷键聚焦搜索框
            rootPane.sceneProperty().addListener { _, _, newScene ->
                newScene?.accelerators?.put(
                    KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN)
                ) {
                    searchField.requestSearchFocus()
                    searchField.showHistory()
                }
            }
        } else {
            searchField.isVisible = false
            searchField.isManaged = false
        }

    }

    override fun onShown() {
        super.onShown()
        if (ConfigEnum.ENABLE_SETTINGS_SEARCH_SERVICE.getBoolean()) {
            runUILater {
                initSearchService
                progressModal.hide(progress)
                progress = null
            }
        }
    }

    /**
     * 导航到指定的设置项
     */
    private fun navigateToSetting(item: SettingItem) {
        // 切换到对应的选项卡
        val tab = windowMap[item.tabWindow]
        if (tab != null) {
            tabPane.selectionModel.select(tab)

            // 延迟执行滚动，确保Tab内容已加载
            runUILater {
                when {
                    // 优先使用 node 直接定位
                    item.node != null -> {
                        scrollToNode(tab.content, item.node)
                    }
                    // 兼容旧模式：通过 ID 定位
                    item.scrollTargetId != null -> {
                        scrollToTarget(tab.content, item.scrollTargetId)
                    }
                }
            }
        }
    }

    /**
     * 滚动到指定节点
     */
    private fun scrollToNode(content: Node?, targetNode: Node) {
        if (content == null) return

        val scrollPane = findScrollPane(content) ?: return
        val contentNode = scrollPane.content ?: return

        runUILater {
            // 获取节点在 ScrollPane 内容中的位置
            val boundsInContent = targetNode.localToScene(targetNode.boundsInLocal)
            val contentBoundsInScene = contentNode.localToScene(contentNode.boundsInLocal)

            if (boundsInContent != null && contentBoundsInScene != null) {
                val relativeY = boundsInContent.minY - contentBoundsInScene.minY
                val viewportHeight = scrollPane.viewportBounds.height
                val contentHeight = (contentNode as? javafx.scene.layout.Region)?.height ?: return@runUILater

                if (contentHeight > viewportHeight) {
                    val targetY = (relativeY - viewportHeight / 3) / (contentHeight - viewportHeight)
                    scrollPane.vvalue = targetY.coerceIn(0.0, 1.0)
                }

                // 播放高亮动画
                if (targetNode is SettingHBox) {
                    targetNode.highlight()
                }
            }
        }
    }

    /**
     * 滚动到目标节点
     */
    private fun scrollToTarget(content: Node?, targetId: String) {
        if (content == null) return

        // 查找ScrollPane和目标节点
        val scrollPane = findScrollPane(content)
        val targetNode = content.lookup("#$targetId")

        if (scrollPane != null && targetNode != null) {
            val contentNode = scrollPane.content
            if (contentNode is VBox) {
                Platform.runLater {
                    val bounds = targetNode.boundsInParent
                    val viewportHeight = scrollPane.viewportBounds.height
                    val contentHeight = contentNode.height

                    if (contentHeight > viewportHeight) {
                        val targetY = bounds.minY / (contentHeight - viewportHeight)
                        scrollPane.vvalue = targetY.coerceIn(0.0, 1.0)
                    }
                }
            }
        }
    }

    /**
     * 递归查找ScrollPane
     */
    private fun findScrollPane(node: Node?): ScrollPane? {
        if (node == null) return null
        if (node is ScrollPane) return node

        if (node is javafx.scene.Parent) {
            for (child in node.childrenUnmodifiable) {
                val result = findScrollPane(child)
                if (result != null) return result
            }
        }
        return null
    }

    fun showTab(window: WindowEnum) {
        windowMap[window]?.let {
            tabPane.selectionModel.select(it)
        }
    }

    fun loadTab(windowEnum: WindowEnum) {
        windowMap[windowEnum]?.let {
            loadTab(it)
        }
    }

    private val initSearchService by lazy {
        loadTab(WindowEnum.INIT_SETTINGS)
        loadTab(WindowEnum.ADVANCED_SETTINGS)
        loadTab(WindowEnum.STRATEGY_SETTINGS)
        loadTab(WindowEnum.DEVELOPER_SETTINGS)
    }

    private var currentTab: Tab? = null

    private val windowKey = "window"
    private val controllerKey = "controller"

    override fun onHidden() {
        super.onHidden()
        (currentTab?.properties?.get(controllerKey) as? StageHook)?.onHidden()
    }

    private fun loadTab(tab: Tab) {
        (currentTab?.properties?.get(controllerKey) as? StageHook)?.onHidden()
        val windowEnum = tab.properties[windowKey]
        if (windowEnum is WindowEnum && tab.content == null) {
            val loader = WindowUtil.getLoader(windowEnum)
            val start = System.currentTimeMillis()
            tab.content = loader.load()
//            println("load ${windowEnum.name} time:${System.currentTimeMillis() - start}ms")
            val controller = loader.getController<Any>()
            tab.properties[controllerKey] = controller
            WindowUtil.addEventHook(tab.content, controller)
        }
        currentTab = tab
        (currentTab?.properties?.get(controllerKey) as? StageHook)?.onShown()
    }

}