package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.hsscript.consts.CHANNEL_IMG_NAME
import club.xiaojiawei.hsscript.consts.DONATE_IMG_NAME
import club.xiaojiawei.hsscript.utils.MessageDigestUtil.calcSHA256
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
    protected lateinit var joinChannelImageView: ImageView

    @FXML
    protected lateinit var donateImageView: ImageView

    @FXML
    protected lateinit var notificationManager: NotificationManager<Any>

    @FXML
    protected lateinit var projectIco: ImageView

    @FXML
    protected lateinit var rootPane: Pane

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        projectIco.image = Image("file:" + SystemUtil.getProgramIconFile().absolutePath)
        val donateFile = SystemUtil.getResourcesImgFile(DONATE_IMG_NAME)
        if (donateFile.calcSHA256() == "8b462ad8977e234375905c22deed31b46bcb52d14f57fbf1d5bddf9a9f8bb2f9") {
            donateImageView.image = Image("file:" + donateFile.absolutePath)
        } else {
            SystemUtil.messageError("当前软件已被非法修改")
            return
        }
        val channelFile = SystemUtil.getResourcesImgFile(CHANNEL_IMG_NAME)
        if (channelFile.calcSHA256() == "5271850da5372b2fb77017e1202f721c9dbcaadcd3e3fd3b8205a6ab02e0fe15") {
            joinChannelImageView.image = Image("file:" + channelFile.absolutePath)
        } else {
            SystemUtil.messageError("当前软件已被非法修改")
            return
        }
    }

    @FXML
    protected fun showExtraInfo(event: MouseEvent) {
        if (event.clickCount == 3) {
            notificationManager.showInfo("运行模式: ${BuildInfo.SOFT_RUN_MODE.name}", 5)
        }
    }

}