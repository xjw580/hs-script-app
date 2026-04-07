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
                1. 修复开启保护模式后注入dll会失败的问题
                
                🔧 重构与优化
                1. 卡牌行为表格添加费用列
                2. 我方英雄鼠标控制区域改成上半边
                3. 打开游戏后战网窗口将置底
                4. 更新卡牌数据库
                5. 重复插件仅加载最高版本
                
                🚀 新功能
                1. 开发者选项
                 - 增加显示鼠标轨迹功能
                 - 改用注入绘制显示游戏控件位置
                 - 增加游戏注入和卸载功能
                 - 增加手动注入功能
                 - 增加手动启动游戏和战网功能
                2. 卡牌组设置页现在会保存分隔栏位置
                3. 策略设置页增加最大连胜功能
                4. 卡牌行为设置页增加隐藏列功能
                5. 增加软件退出后卸载注入功能
                6. 增加阻止反作弊状态检测功能
                7. 插件设置支持打开插件位置
                8. 解析卡牌描述功能现已经支持mcts策略
            """.trimIndent()
        )
    }

    @FXML
    protected fun closeWindow(actionEvent: ActionEvent) {
        rootPane.scene.window.hide()
    }
}
