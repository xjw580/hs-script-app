package club.xiaojiawei.hsscript.controller.javafx

import club.xiaojiawei.hsscriptbase.const.BuildInfo.VERSION
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.AnchorPane
import java.io.File
import java.net.URI
import java.net.URL
import java.util.*


/**
 * @author 肖嘉威
 * @date 2023/10/14 12:43
 */
class VersionMsgController : Initializable {
    @FXML
    protected lateinit var versionDescription: TextArea

    @FXML
    protected lateinit var rootPane: AnchorPane

    @FXML
    protected lateinit var version: Label

    override fun initialize(
        url: URL?,
        resourceBundle: ResourceBundle?,
    ) {
        version.text = VERSION
        //        TODO 版本更新时修改！！！
        versionDescription.text =
            """
            🚀 新功能
            1. 设置页增加搜索设置项功能
            2. 高级设置页增加保护软件所在目录的功能
            
            🔧 重构与优化
            1. 增强`避免管理员权限启动游戏`功能的适用性生效
            """.trimIndent()
    }

    @FXML
    protected fun closeWindow(actionEvent: ActionEvent) {
        rootPane.scene.window.hide()
    }
}
