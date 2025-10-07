package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.ico.FailIco
import club.xiaojiawei.controls.ico.HelpIco
import club.xiaojiawei.controls.ico.OKIco
import club.xiaojiawei.hsscript.MainApplication
import club.xiaojiawei.hsscript.bean.HotKey
import club.xiaojiawei.hsscript.bean.single.repository.GiteeRepository
import club.xiaojiawei.hsscript.component.ConfigSwitch
import club.xiaojiawei.hsscript.consts.AOT_BATCH_NAME
import club.xiaojiawei.hsscript.consts.AOT_DIR
import club.xiaojiawei.hsscript.consts.AOT_FILE_PATH
import club.xiaojiawei.hsscript.consts.AOT_PATH
import club.xiaojiawei.hsscript.consts.PROGRAM_NAME
import club.xiaojiawei.hsscript.consts.ROOT_PATH
import club.xiaojiawei.hsscript.controller.javafx.settings.view.AdvancedSettingsView
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.GameStartupModeEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.interfaces.StageHook
import club.xiaojiawei.hsscript.listener.GlobalHotkeyListener
import club.xiaojiawei.hsscript.utils.CMDUtil
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import club.xiaojiawei.hsscript.utils.ConfigExUtil.getExitHotKey
import club.xiaojiawei.hsscript.utils.ConfigExUtil.getPauseHotKey
import club.xiaojiawei.hsscript.utils.ConfigExUtil.storeExitHotKey
import club.xiaojiawei.hsscript.utils.ConfigExUtil.storeMouseControlMode
import club.xiaojiawei.hsscript.utils.ConfigExUtil.storePauseHotKey
import club.xiaojiawei.hsscript.utils.ConfigUtil.putString
import club.xiaojiawei.hsscript.utils.FileUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.config.log
import com.melloware.jintellitype.JIntellitypeConstants
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.util.Duration
import java.io.File
import java.net.URL
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * @author 肖嘉威
 * @date 2023/9/10 15:07
 */
class AdvancedSettingsController : AdvancedSettingsView() , StageHook, Initializable {

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        initValue()
        listen()
    }

    override fun onShown() {
        refreshAOTCache()
    }

    private fun initValue() {
        val repositoryList = ConfigExUtil.getUpdateSourceList()
        if (repositoryList.isEmpty() || repositoryList.first() == GiteeRepository) {
            giteeUpdateSource.isSelected = true
        } else {
            githubUpdateSource.isSelected = true
        }
        mouseControlModeComboBox.setCellFactory {
            object : ListCell<MouseControlModeEnum?>() {
                private val ico = HelpIco()
                override fun updateItem(s: MouseControlModeEnum?, b: Boolean) {
                    super.updateItem(s, b)
                    if (s == null || b) return
                    contentDisplay = ContentDisplay.RIGHT
                    text = s.name
                    ico.color =
                        if (mouseControlModeComboBox.selectionModel.selectedItem != null && mouseControlModeComboBox.selectionModel.selectedItem === item) "white" else ""
                    graphic = Label().apply {
                        graphic = ico
                        tooltip = Tooltip(s.comment)
                    }
                }
            }
        }
        mouseControlModeComboBox.items.addAll(MouseControlModeEnum.entries.toTypedArray())
        mouseControlModeComboBox.value = ConfigExUtil.getMouseControlMode()
        val isDrive = mouseControlModeComboBox.value === MouseControlModeEnum.DRIVE
        refreshDriver.isVisible = isDrive
        refreshDriver.isManaged = isDrive

        gameStartupModeComboBox.items.addAll(GameStartupModeEnum.entries.toTypedArray())
        gameStartupModeComboBox.value = ConfigExUtil.getGameStartupMode().first()

        val pauseKey = getPauseHotKey()
        if (pauseKey != null) {
            pauseHotKey.text = pauseKey.toString()
        }
        val exitKey = getExitHotKey()
        if (exitKey != null) {
            exitHotKey.text = exitKey.toString()
        }

        refreshAOTCache()
    }

    private fun scrollTo(pane: Node) {
        pane.boundsInParent.let {
            val targetV = it.minY / (titledRootPane.height - scrollPane.viewportBounds.height)
            val sourceV = scrollPane.vvalue
            Timeline(
                KeyFrame(
                    Duration.millis(0.0), KeyValue(scrollPane.vvalueProperty(), sourceV)
                ),
                KeyFrame(
                    Duration.millis(200.0),
                    KeyValue(
                        scrollPane.vvalueProperty(),
                        targetV
                    )
                )
            ).run {
                forbidSetToggle = true
                onFinished = EventHandler {
                    forbidSetToggle = false
                }
                play()
            }
        }
    }

    private var forbidSetToggle = false
    private var mouseMaxY = 0.0
    private var mouseMinY = 0.0
    private var windowMaxY = 0.0
    private var windowMinY = 0.0
    private var behaviorMaxY = 0.0
    private var behaviorMinY = 0.0
    private var systemMaxY = 0.0
    private var systemMinY = 0.0
    private var versionMaxY = 0.0
    private var versionMinY = 0.0


    private fun updateY() {
        val diffH = titledRootPane.height - scrollPane.viewportBounds.height
        mouseMaxY = mousePane.boundsInParent.maxY / diffH
        mouseMinY = mousePane.boundsInParent.minY / diffH
        windowMaxY = windowPane.boundsInParent.maxY / diffH
        windowMinY = windowPane.boundsInParent.minY / diffH
        behaviorMaxY = behaviorPane.boundsInParent.maxY / diffH
        behaviorMinY = behaviorPane.boundsInParent.minY / diffH
        systemMaxY = systemPane.boundsInParent.maxY / diffH
        systemMinY = systemPane.boundsInParent.minY / diffH
        versionMaxY = versionPane.boundsInParent.maxY / diffH
        versionMinY = versionPane.boundsInParent.minY / diffH
    }

    private fun listen() {
        autoStartPane.visibleProperty().bind(powerBootSwitch.statusProperty())
        autoStartPane.managedProperty().bind(powerBootSwitch.statusProperty())

        navigationBarToggle.selectedToggleProperty().addListener { _, oldToggle, newToggle ->
            newToggle ?: let {
                navigationBarToggle.selectToggle(oldToggle)
            }
        }
        scrollPane.vvalueProperty().addListener { _, oldValue, newValue ->
            if (forbidSetToggle) return@addListener
            updateY()
            val newV = newValue.toDouble()
            val oldV = oldValue.toDouble()
            if (newV - oldV > 0) {
                if (newV > systemMaxY) {
                    navigationBarToggle.selectToggle(versionNavigation)
                } else if (newV > behaviorMaxY) {
                    navigationBarToggle.selectToggle(systemNavigation)
                } else if (newV > windowMaxY) {
                    navigationBarToggle.selectToggle(behaviorNavigation)
                } else if (newV > mouseMaxY) {
                    navigationBarToggle.selectToggle(windowNavigation)
                }
            } else {
                if (newV <= mouseMinY) {
                    navigationBarToggle.selectToggle(mouseNavigation)
                } else if (newV <= windowMinY) {
                    navigationBarToggle.selectToggle(windowNavigation)
                } else if (newV <= behaviorMinY) {
                    navigationBarToggle.selectToggle(behaviorNavigation)
                } else if (newV <= systemMinY) {
                    navigationBarToggle.selectToggle(systemNavigation)
                }
            }
        }
//        监听更新源
        updateSourceToggle.selectedToggleProperty().addListener { _, _, newValue ->
            putString(ConfigEnum.UPDATE_SOURCE, (newValue as ToggleButton).text)
        }
//        监听鼠标模式开关
        mouseControlModeComboBox.valueProperty()
            .addListener { observable, oldValue, newValue ->
                storeMouseControlMode(newValue)
                val isDrive = ConfigExUtil.getMouseControlMode() === MouseControlModeEnum.DRIVE
                refreshDriver.isVisible = isDrive
                refreshDriver.isManaged = isDrive
                topGameWindow.status =
                    (newValue === MouseControlModeEnum.EVENT || newValue === MouseControlModeEnum.DRIVE)
            }
        gameStartupModeComboBox.valueProperty().addListener { _, _, newValue ->
            ConfigExUtil.storeGameStartupMode(GameStartupModeEnum.entries.sortedBy { if (it == newValue) 0 else 1 })
        }

        pauseHotKey.onKeyPressed =
            EventHandler { event: KeyEvent ->
                val hotKey = plusModifier(event)
                if (hotKey != null) {
                    if (hotKey.keyCode == 0) {
                        pauseHotKey.text = ""
                        storePauseHotKey(hotKey)
                        GlobalHotkeyListener.reload()
                        notificationManager.showSuccess("开始/暂停热键热键已删除", 2)
                    } else {
                        pauseHotKey.text = hotKey.toString()
                        storePauseHotKey(hotKey)
                        GlobalHotkeyListener.reload()
                        notificationManager.showSuccess("开始/暂停热键已修改", 2)
                    }
                }
            }
        pauseHotKey.onKeyReleased =
            EventHandler { event: KeyEvent ->
                this.reduceModifier(
                    event
                )
            }
        pauseHotKey.focusedProperty()
            .addListener { observable, oldValue, newValue ->
                if (!newValue) {
                    modifier = 0
                }
            }

        exitHotKey.onKeyPressed =
            EventHandler { event: KeyEvent ->
                val hotKey = plusModifier(event)
                if (hotKey != null) {
                    if (hotKey.keyCode == 0) {
                        exitHotKey.text = ""
                        storeExitHotKey(hotKey)
                        GlobalHotkeyListener.reload()
                        notificationManager.showSuccess("退出热键已删除", 2)
                    } else {
                        exitHotKey.text = hotKey.toString()
                        storeExitHotKey(hotKey)
                        GlobalHotkeyListener.reload()
                        notificationManager.showSuccess("退出热键已修改", 2)
                    }
                }
            }
        exitHotKey.onKeyReleased =
            EventHandler { event: KeyEvent ->
                this.reduceModifier(
                    event
                )
            }
        exitHotKey.focusedProperty()
            .addListener { observable, oldValue, newValue ->
                if (!newValue) {
                    modifier = 0
                }
            }
    }

    private var modifier = 0

    private fun plusModifier(event: KeyEvent): HotKey? {
        if (event.code == KeyCode.ALT) {
            modifier += JIntellitypeConstants.MOD_ALT
        } else if (event.code == KeyCode.CONTROL) {
            modifier += JIntellitypeConstants.MOD_CONTROL
        } else if (event.code == KeyCode.SHIFT) {
            modifier += JIntellitypeConstants.MOD_SHIFT
        } else if (event.code == KeyCode.WINDOWS) {
            modifier += JIntellitypeConstants.MOD_WIN
        } else if (event.code == KeyCode.BACK_SPACE) {
            return HotKey()
        } else {
            val code = event.code.code
            if (code >= KeyCode.A.code && code <= KeyCode.Z.code) {
                return HotKey(modifier, code)
            }
        }
        return null
    }

    private fun reduceModifier(event: KeyEvent) {
        if (event.code == KeyCode.ALT) {
            modifier -= JIntellitypeConstants.MOD_ALT
        } else if (event.code == KeyCode.CONTROL) {
            modifier -= JIntellitypeConstants.MOD_CONTROL
        } else if (event.code == KeyCode.SHIFT) {
            modifier -= JIntellitypeConstants.MOD_SHIFT
        } else if (event.code == KeyCode.WINDOWS) {
            modifier -= JIntellitypeConstants.MOD_WIN
        }
    }

    @FXML
    protected fun scrollMouse(actionEvent: ActionEvent) {
        scrollTo(mousePane)
    }

    @FXML
    protected fun scrollWindow(actionEvent: ActionEvent) {
        scrollTo(windowPane)
    }

    @FXML
    protected fun scrollBehavior(actionEvent: ActionEvent) {
        scrollTo(behaviorPane)
    }

    @FXML
    protected fun scrollSystem(actionEvent: ActionEvent) {
        scrollTo(systemPane)
    }

    @FXML
    protected fun scrollVersion(actionEvent: ActionEvent) {
        scrollTo(versionPane)
    }

    @FXML
    protected fun refreshDriver(actionEvent: ActionEvent) {
        val res = CSystemDll.safeRefreshDriver()
        if (res >= 0) {
            notificationManager.showSuccess("刷新驱动成功", 2)
        } else {
            notificationManager.showError("刷新驱动失败", 2)
        }
    }

    @FXML
    protected fun createAOTCache() {
        val aotBatch = Path(AOT_BATCH_NAME)
        if (aotBatch.exists()){
            File(AOT_PATH).mkdirs()
            val startCMD = "$aotBatch \"${SystemUtil.getCurrentJarFile().name}\" \"${AOT_DIR}\\${PROGRAM_NAME}\" \"${MainApplication::class.java.packageName}.MainKt\""
            log.info { "cmd: $startCMD" }
            CMDUtil.directExec(arrayOf(
                "cmd", "/c", "start", "\"AOTWindow\"", "cmd.exe", "/k", startCMD
            )).waitFor()
            refreshAOTCache()
        }else{
            notificationManager.showError("无法创建AOT缓存","$aotBatch 不存在", 3)
        }
    }

    @FXML
    protected fun refreshAOTCache(action: ActionEvent? = null) {
        aotCacheLabel.graphic = if (File(AOT_FILE_PATH).exists()) OKIco() else FailIco()
    }

    @FXML
    protected fun openAOTCacheDir(){
        SystemUtil.openFile(AOT_PATH)
    }

}