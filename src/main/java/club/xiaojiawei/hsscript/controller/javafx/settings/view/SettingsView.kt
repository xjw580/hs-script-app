package club.xiaojiawei.hsscript.controller.javafx.settings.view

import club.xiaojiawei.controls.ProgressModal
import club.xiaojiawei.hsscript.component.SettingsSearchField
import javafx.fxml.FXML
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.Pane

/**
 * @author 肖嘉威
 * @date 2026/2/3 14:18
 */
abstract class SettingsView {

    @FXML
    protected lateinit var progressModal: ProgressModal

    @FXML
    protected lateinit var searchField: SettingsSearchField

    @FXML
    protected lateinit var initTab: Tab

    @FXML
    protected lateinit var advancedTab: Tab

    @FXML
    protected lateinit var pluginTab: Tab

    @FXML
    protected lateinit var strategyTab: Tab

    @FXML
    protected lateinit var cardGroupTab: Tab

    @FXML
    protected lateinit var developerTab: Tab

    @FXML
    protected lateinit var aboutTab: Tab

    @FXML
    protected lateinit var rootPane: Pane

    @FXML
    protected lateinit var tabPane: TabPane
}