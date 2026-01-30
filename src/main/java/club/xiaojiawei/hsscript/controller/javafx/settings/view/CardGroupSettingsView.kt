package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.controls.ProgressModal
import club.xiaojiawei.controls.TableFilterManagerGroup
import club.xiaojiawei.hsscript.bean.CardGroupCard
import club.xiaojiawei.hsscript.bean.CardGroupInfo
import club.xiaojiawei.hsscript.component.CardTableView
import club.xiaojiawei.hsscriptcardsdk.enums.CardActionEnum
import club.xiaojiawei.hsscriptcardsdk.enums.CardEffectTypeEnum
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.StackPane

/**
 * 卡牌组设置控制器
 * @author 肖嘉威
 * @date 2026/1/30
 */
abstract class CardGroupSettingsView {

    @FXML
    protected lateinit var notificationManager: NotificationManager<String>

    @FXML
    protected lateinit var progressModal: ProgressModal

    @FXML
    protected lateinit var cardGroupCardTableProxy: TableFilterManagerGroup<CardGroupCard, CardGroupCard>

    @FXML
    protected lateinit var rootPane: StackPane

    @FXML
    protected lateinit var cardTable: CardTableView

    // 卡牌组列表
    @FXML
    protected lateinit var cardGroupListView: ListView<CardGroupInfo>

    // 卡牌组信息
    @FXML
    protected lateinit var cardGroupNameField: TextField

    @FXML
    protected lateinit var enabledCheckBox: CheckBox

    // 卡牌表格
    @FXML
    protected lateinit var cardGroupCardTable: TableView<CardGroupCard>

    @FXML
    protected lateinit var noCol: TableColumn<CardGroupCard, Number?>

    @FXML
    protected lateinit var cardIdCol: TableColumn<CardGroupCard, String>

    @FXML
    protected lateinit var nameCol: TableColumn<CardGroupCard, String>

    @FXML
    protected lateinit var effectTypeCol: TableColumn<CardGroupCard, CardEffectTypeEnum>

    @FXML
    protected lateinit var playActionCol: TableColumn<CardGroupCard, List<CardActionEnum>>

    @FXML
    protected lateinit var powerActionCol: TableColumn<CardGroupCard, List<CardActionEnum>>

    @FXML
    protected lateinit var weightCol: TableColumn<CardGroupCard, Number?>

    @FXML
    protected lateinit var powerWeightCol: TableColumn<CardGroupCard, Number?>

    @FXML
    protected lateinit var changeWeightCol: TableColumn<CardGroupCard, Number?>

    @FXML
    protected lateinit var changeWeightCheckBox: CheckBox

    // ============ FXML 事件处理方法（由子类实现） ============

    /**
     * 创建卡牌组
     */
    @FXML
    protected abstract fun createCardGroup()

    /**
     * 删除卡牌组
     */
    @FXML
    protected abstract fun deleteCardGroup()

    /**
     * 复制卡牌组
     */
    @FXML
    protected abstract fun copyCardGroup()

    /**
     * 导入卡牌组
     */
    @FXML
    protected abstract fun importCardGroup()

    /**
     * 导出卡牌组
     */
    @FXML
    protected abstract fun exportCardGroup()

    /**
     * 添加卡牌到卡牌组
     */
    @FXML
    protected abstract fun addCard()

    /**
     * 从卡牌组移除卡牌
     */
    @FXML
    protected abstract fun removeCard()

    /**
     * 重命名卡牌组
     */
    @FXML
    protected abstract fun renameCardGroup()

    /**
     * 复制卡牌
     */
    @FXML
    protected abstract fun copyCard()

}
