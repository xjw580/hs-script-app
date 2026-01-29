package club.xiaojiawei.hsscript.controller.javafx.settings.view

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.hsscript.component.ConfigSwitch
import club.xiaojiawei.hsscript.enums.GameStartupModeEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.enums.SoftProtectedModeEnum
import javafx.fxml.FXML
import javafx.scene.Group
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

/**
 * @author 肖嘉威
 * @date 2025/3/7 16:11
 */
open class AdvancedSettingsView {

    @FXML
    protected lateinit var autoStartPane: HBox

    @FXML
    protected lateinit var aotPane: Pane

    @FXML
    protected lateinit var powerBootSwitch: ConfigSwitch

    @FXML
    protected lateinit var systemTitled: TitledPane

    @FXML
    protected lateinit var behaviorTitled: TitledPane

    @FXML
    protected lateinit var versionTitled: TitledPane

    @FXML
    protected lateinit var titledRootPane: VBox

    @FXML
    protected lateinit var scrollPane: ScrollPane

    @FXML
    protected lateinit var versionPane: Group

    @FXML
    protected lateinit var windowTitled: TitledPane

    @FXML
    protected lateinit var windowPane: Group

    @FXML
    protected lateinit var mouseTitled: TitledPane

    @FXML
    protected lateinit var mousePane: Group

    @FXML
    protected lateinit var behaviorPane: Group

    @FXML
    protected lateinit var systemPane: Group

    @FXML
    protected lateinit var systemNavigation: ToggleButton

    @FXML
    protected lateinit var windowNavigation: ToggleButton

    @FXML
    protected lateinit var mouseNavigation: ToggleButton

    @FXML
    protected lateinit var behaviorNavigation: ToggleButton

    @FXML
    protected lateinit var versionNavigation: ToggleButton

    @FXML
    protected lateinit var navigationBarToggle: ToggleGroup

    @FXML
    protected lateinit var mouseControlModeComboBox: ComboBox<MouseControlModeEnum>

    @FXML
    protected lateinit var githubUpdateSource: RadioButton

    @FXML
    protected lateinit var customUpdateSource: RadioButton

    @FXML
    protected lateinit var giteeUpdateSource: RadioButton

    @FXML
    protected lateinit var updateSourceToggle: ToggleGroup

    @FXML
    protected lateinit var pauseHotKey: TextField

    @FXML
    protected lateinit var exitHotKey: TextField

    @FXML
    protected lateinit var notificationManager: NotificationManager<Any>

    @FXML
    protected lateinit var topGameWindow: ConfigSwitch

    @FXML
    protected lateinit var rootPane: StackPane

    @FXML
    protected lateinit var refreshDriver: Button

    @FXML
    protected lateinit var gameStartupModeComboBox: ComboBox<GameStartupModeEnum>

    @FXML
    protected lateinit var softProtectedModeComboBox: ComboBox<SoftProtectedModeEnum>

    @FXML
    protected lateinit var aotFlushBtn: Button

}