package club.xiaojiawei.hsscript.controller.javafx

import club.xiaojiawei.hsscriptbase.const.BuildInfo.VERSION
import club.xiaojiawei.md.MarkdownView
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.Pane
import java.net.URL
import java.util.*


/**
 * @author 肖嘉威
 * @date 2023/10/14 12:43
 */
class VersionMsgController : Initializable {
    @FXML
    protected lateinit var versionDescription: MarkdownView

    @FXML
    protected lateinit var rootPane: Pane

    @FXML
    protected lateinit var version: Label

    override fun initialize(
        url: URL?,
        resourceBundle: ResourceBundle?,
    ) {

        version.text = VERSION
        //        TODO 版本更新时修改！！！
        versionDescription.setMarkdown(
            """
            🐛 Bug修复
            1. 修复native版无法编辑行为的问题
            
            🔧 重构与优化
            1. 更新卡牌数据库
            2. 移除幻变模式的位置
            """.trimIndent()
        )
    }

    @FXML
    protected fun closeWindow(actionEvent: ActionEvent) {
        rootPane.scene.window.hide()
    }
}
