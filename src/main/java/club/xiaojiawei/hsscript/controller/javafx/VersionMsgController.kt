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
            1. 禁用游戏日志大小限制，开启方式：游戏日志大小限制改成-1
            2. 实现抉择功能
            3. 工作时间增加模式，策略，卡组位的设置
            4. 增加自动刷新游戏任务功能
            5. 增加实时读取游戏画面的功能
            6. 增加开机自启后自动运行功能
            
            🔧 重构与优化
            1. 修改鼠标控制模式MESSAGE的底层逻辑，将同步改成异步
            2. 避免以管理员权限启动游戏
            
            🐛 Bug修复
            1. 修复软件无法自动关机的问题
            """.trimIndent()
    }

    @FXML
    protected fun closeWindow(actionEvent: ActionEvent) {
        rootPane.scene.window.hide()
    }
}
