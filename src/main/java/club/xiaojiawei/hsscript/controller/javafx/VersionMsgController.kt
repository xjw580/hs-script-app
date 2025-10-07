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
            1. 增加控制台工具(设置->开发者选项)
            2. 增加游戏启动方式偏好设置(设置->高级设置->行为)
            3. 使用aot技术大幅提高软件启动速度(设置->高级设置->系统)
            4. 适配抽奖界面
            
            🔧 重构与优化
            1. 不再自带tess数据集，使用时再下载
            2. 优化暂停再开始后有概率无法恢复的问题
            3. 避免以管理员权限启动游戏增加单独的开关，避免某些系统版本不支持导致无法启动游戏(设置->高级设置->行为)
            4. 优化软件更新检查，避免重复启动软件导致的重复检查
            """.trimIndent()
    }

    @FXML
    protected fun closeWindow(actionEvent: ActionEvent) {
        rootPane.scene.window.hide()
    }
}
