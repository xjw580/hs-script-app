package club.xiaojiawei.hsscript.controller.javafx

import club.xiaojiawei.hsscriptbase.const.BuildInfo.VERSION
import club.xiaojiawei.md.MarkdownView
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
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
    protected lateinit var rootPane: AnchorPane

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
            1. 修复捕获画面刷新线程问题
            
            🔧 重构与优化
            1. 我方英雄鼠标控制区域改成上半边
            2. 打开游戏后战网窗口将置底
            3. 更新卡牌数据库
            
            🚀 新功能
            1. 开发者选项
               - 增加显示鼠标轨迹功能
               - 改用注入绘制显示游戏控件位置
               - 增加游戏注入和卸载功能
            2. 卡牌组设置页现在会保存分隔栏位置
            3. 策略设置页增加最大连胜功能
            """.trimIndent()
        )
    }

    @FXML
    protected fun closeWindow(actionEvent: ActionEvent) {
        rootPane.scene.window.hide()
    }
}
