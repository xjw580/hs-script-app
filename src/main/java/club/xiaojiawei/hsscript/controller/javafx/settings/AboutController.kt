package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.const.BuildInfo
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import java.net.URL
import java.util.*

/**
 * @author 肖嘉威
 * @date 2023/10/14 12:43
 */
class AboutController : Initializable {

    @FXML
    protected lateinit var notificationManager: NotificationManager<Any>

    @FXML
    protected lateinit var projectIco: ImageView

    @FXML
    protected lateinit var rootPane: Pane

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        projectIco.image = Image("file:" + SystemUtil.getProgramIconFile().absolutePath)
    }

    @FXML
    protected fun showExtraInfo(event: MouseEvent) {
        if (event.clickCount == 3) {
            notificationManager.showInfo("运行模式: ${BuildInfo.SOFT_RUN_MODE.name}", 5)
        }
    }

}