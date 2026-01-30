package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.Modal
import club.xiaojiawei.controls.ico.EditIco
import club.xiaojiawei.controls.ico.FileIco
import club.xiaojiawei.hsscript.bean.*
import club.xiaojiawei.hsscript.bean.tableview.ComboBoxTableCell
import club.xiaojiawei.hsscript.bean.tableview.NoEditTextFieldTableCell
import club.xiaojiawei.hsscript.component.EditActionPane
import club.xiaojiawei.hsscript.component.svg.CodeIco
import club.xiaojiawei.hsscript.consts.CARD_GROUP_FILE_EXT
import club.xiaojiawei.hsscript.enums.CardInfoActionTypeEnum
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.interfaces.KeyHook
import club.xiaojiawei.hsscript.interfaces.StageHook
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.util.setAll
import club.xiaojiawei.hsscriptcardsdk.enums.CardActionEnum
import club.xiaojiawei.hsscriptcardsdk.enums.CardEffectTypeEnum
import club.xiaojiawei.hsscriptcardsdk.enums.CardTypeEnum
import club.xiaojiawei.hsscriptcardsdk.util.CardDBUtil
import club.xiaojiawei.kt.dsl.*
import club.xiaojiawei.kt.ext.runUILater
import club.xiaojiawei.tablecell.NumberFieldTableCellUI
import club.xiaojiawei.tablecell.TextFieldTableCellUI
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import javafx.beans.property.DoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.value.ChangeListener
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.CheckBox
import javafx.scene.control.ListCell
import javafx.scene.control.SelectionMode
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.FileChooser
import javafx.stage.Popup
import javafx.util.Callback
import javafx.util.StringConverter
import java.io.File
import java.net.URL
import java.util.*
import java.util.stream.IntStream
import kotlin.io.path.Path

/**
 * 卡牌组设置控制器
 * @author 肖嘉威
 * @date 2026/1/30
 */
class CardGroupSettingsController : CardGroupSettingsView(), Initializable, StageHook, KeyHook {

    private fun currentCardGroup(): CardGroupInfo? = cardGroupListView.selectionModel.selectedItem
    private fun currentCardGroups(): List<CardGroupInfo> = cardGroupListView.selectionModel.selectedItems
    private fun currentCard(): CardGroupCard? = cardGroupCardTable.selectionModel.selectedItem
    private fun currentCards(): List<CardGroupCard> = cardGroupCardTable.selectionModel.selectedItems

    // 存储复制的卡牌
    private val copiedCards = mutableListOf<CardGroupCard>()

    // 存储剪切的卡牌
    private var cutSourceCards: List<CardGroupCard>? = null
    private var cutSourceCardGroup: CardGroupInfo? = null

    // 存储修改过的卡牌组
    private val modifiedCardGroups = mutableSetOf<CardGroupInfo>()

    // 存储待删除的卡牌组名称
    private val pendingDeleteCardGroups = mutableSetOf<String>()

    private var hasNotifiedUser = false

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        initCardGroupListView()
        initCardTable()
        initCardGroupCardTable()

        cardGroupNameField.addEnterKeyFilter { _, _ -> renameCardGroup() }
        changeWeightCheckBox.isSelected = ConfigUtil.getBoolean(ConfigEnum.ENABLE_CHANGE_WEIGHT)
        changeWeightCol.visibleProperty().bind(changeWeightCheckBox.selectedProperty())
        changeWeightCheckBox.selectedProperty().addListener { _, _, newValue ->
            ConfigUtil.putBoolean(ConfigEnum.ENABLE_CHANGE_WEIGHT, newValue)
        }
    }

    private var progress: DoubleProperty? = null

    override fun onShown() {
        super.onShown()
        progress = progressModal.show("加载卡牌组中")
        loadCardGroups()
    }

    /**
     * 初始化卡牌组列表视图
     */
    private fun initCardGroupListView() {
        cardGroupListView.selectionModel.selectionMode = SelectionMode.MULTIPLE

        cardGroupListView.cellFactory = Callback {
            object : ListCell<CardGroupInfo>() {
                private var checkBox: CheckBox = checkBox {
                    styleMain()
                }

                private fun onAction(item: CardGroupInfo) {
                    item.enabled = checkBox.isSelected
                    modifyCardGroup(item)
                    if (item === currentCardGroup()) {
                        enabledCheckBox.isSelected = checkBox.isSelected
                    }
                }

                override fun updateItem(item: CardGroupInfo?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        checkBox.onAction = null
                        text = null
                        graphic = null
                    } else {
                        checkBox.isSelected = item.enabled
                        checkBox.setOnAction {
                            onAction(item)
                        }
                        text = item.name
                        graphic = checkBox
                    }
                }
            }
        }

        cardGroupListView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            selectCardGroup(newValue)
        }

        cardGroupListView.addDeleteKeyFilter {
            deleteCardGroup()
            it.consume()
        }
    }

    private fun initCardTable() {
        cardTable.notificationManager = this.notificationManager
    }

    private fun initCardGroupCardTable() {
        val numberConverter: StringConverter<Number> = object : StringConverter<Number>() {
            override fun toString(number: Number): String {
                return number.toString()
            }

            override fun fromString(s: String?): Number {
                return if (s.isNullOrBlank()) 0.0 else s.toDouble()
            }
        }
        cardGroupCardTable.contextMenu = contextMenu {
            style()
            addMenuItem {
                +"编辑${CardInfoActionTypeEnum.PLAY.comment}"
                +EditIco()
                onAction {
                    val selectedItem = cardGroupCardTable.selectionModel.selectedItem
                    if (selectedItem == null) {
                        notificationManager.showInfo("请先选择要编辑的行", 1)
                        return@onAction
                    }
                    editAction(selectedItem, CardInfoActionTypeEnum.PLAY)
                }
            }
            addMenuItem {
                +"编辑${CardInfoActionTypeEnum.POWER.comment}"
                +EditIco()
                onAction {
                    val selectedItem = cardGroupCardTable.selectionModel.selectedItem
                    if (selectedItem == null) {
                        notificationManager.showInfo("请先选择要编辑的行", 1)
                        return@onAction
                    }
                    editAction(selectedItem, CardInfoActionTypeEnum.POWER)
                }
            }
        }

        cardGroupCardTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
        cardGroupCardTable.isEditable = true

        val stringConverter: StringConverter<String?> = object : StringConverter<String?>() {
            override fun toString(`object`: String?): String? {
                return `object`
            }

            override fun fromString(string: String?): String? {
                return string
            }
        }

        // 序号列
        noCol.setCellValueFactory { param ->
            val items = param.tableView.items
            val index = IntStream.range(0, items.size)
                .filter { i -> items[i] === param.value }
                .findFirst().orElse(-2)
            SimpleIntegerProperty(index + 1)
        }

        // cardID 列
        cardIdCol.setCellValueFactory { it.value.cardIdProperty }
        cardIdCol.setCellFactory {
            object : TextFieldTableCellUI<CardGroupCard?, String?>(stringConverter) {
                override fun commitEdit(s: String?) {
                    super.commitEdit(s)
                    modifyCurrentCardGroup()
                }
            }
        }

        // 名称列
        nameCol.setCellValueFactory { it.value.nameProperty }
        nameCol.setCellFactory {
            object : TextFieldTableCellUI<CardGroupCard?, String?>(stringConverter) {
                override fun commitEdit(s: String?) {
                    super.commitEdit(s)
                    modifyCurrentCardGroup()
                }
            }
        }

        // 效果类型列
        effectTypeCol.setCellValueFactory { it.value.effectTypeProperty }
        effectTypeCol.setCellFactory {
            object :
                ComboBoxTableCell<CardGroupCard?, CardEffectTypeEnum?>(*CardEffectTypeEnum.entries.toTypedArray()) {
                override fun comboBoxStyleClass(): MutableList<String?> {
                    val comboBoxStyleClass = super.comboBoxStyleClass()
                    comboBoxStyleClass.add("combo-box-ui-small")
                    return comboBoxStyleClass
                }

                override fun commitEdit(p0: CardEffectTypeEnum?) {
                    super.commitEdit(p0)
                    modifyCurrentCardGroup()
                }
            }
        }

        // 打出行为列
        val actionConverter = object : StringConverter<List<CardActionEnum>?>() {
            override fun toString(`object`: List<CardActionEnum>?): String? {
                return `object`?.joinToString(",") { it.comment }
            }

            override fun fromString(string: String?): List<CardActionEnum>? {
                return null
            }
        }
        playActionCol.setCellValueFactory { it.value.playActionsProperty }
        playActionCol.setCellFactory {
            object : NoEditTextFieldTableCell<CardGroupCard?, List<CardActionEnum>?>(actionConverter) {
                override fun startEdit() {
                    editAction(cardGroupCardTable.items[index], CardInfoActionTypeEnum.PLAY)
                }
            }
        }

        // 使用行为列
        powerActionCol.setCellValueFactory { it.value.powerActionsProperty }
        powerActionCol.setCellFactory {
            object : NoEditTextFieldTableCell<CardGroupCard?, List<CardActionEnum>?>(actionConverter) {
                override fun startEdit() {
                    editAction(cardGroupCardTable.items[index], CardInfoActionTypeEnum.POWER)
                }
            }
        }

        // 权重列
        weightCol.setCellValueFactory { it.value.weightProperty }
        weightCol.setCellFactory {
            object : NumberFieldTableCellUI<CardGroupCard?, Number>(numberConverter) {
                override fun commitEdit(number: Number) {
                    super.commitEdit(number)
                    modifyCurrentCardGroup()
                    notificationManager.showSuccess("修改权重成功", 1)
                }
            }
        }

        // 使用权重列
        powerWeightCol.setCellValueFactory { it.value.powerWeightProperty }
        powerWeightCol.setCellFactory {
            object : NumberFieldTableCellUI<CardGroupCard?, Number>(numberConverter) {
                override fun commitEdit(number: Number) {
                    super.commitEdit(number)
                    modifyCurrentCardGroup()
                    notificationManager.showSuccess("修改权重成功", 1)
                }
            }
        }

        // 换牌权重列
        changeWeightCol.setCellValueFactory { it.value.changeWeightProperty }
        changeWeightCol.setCellFactory {
            object : NumberFieldTableCellUI<CardGroupCard?, Number>(numberConverter) {
                override fun commitEdit(number: Number) {
                    super.commitEdit(number)
                    modifyCurrentCardGroup()
                    notificationManager.showSuccess("修改权重成功", 1)
                }
            }
        }

        cardGroupCardTable.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            newValue ?: return@addListener
            updatePopup(newValue, CardInfoActionTypeEnum.PLAY)
        }
    }

    private fun loadCardGroups() {
        go {
            val cardGroups = CardGroupUtil.loadAllCardGroups()
            runUILater {
                cardGroupListView.items.clear()
                cardGroupListView.items.addAll(cardGroups)
                if (cardGroups.isNotEmpty()) {
                    cardGroupListView.selectionModel.selectFirst()
                }
                progressModal.hide(progress)
                progress = null
            }
        }
    }

    private var enabledCheckBoxListener: ChangeListener<Boolean>? = null

    private fun selectCardGroup(cardGroup: CardGroupInfo?) {
        enabledCheckBoxListener?.let {
            enabledCheckBox.selectedProperty().removeListener(it)
        }

        if (cardGroup == null) {
            cardGroupNameField.text = ""
            enabledCheckBox.isSelected = false
            cardGroupCardTableProxy.clear()
            return
        }

        cardGroupNameField.text = cardGroup.name
        enabledCheckBox.isSelected = cardGroup.enabled
        cardGroupCardTableProxy.resetFilter()
        cardGroupCardTableProxy.setAll(cardGroup.cardsObservable)

        // 绑定启用状态
        enabledCheckBoxListener = ChangeListener { _, _, newValue ->
            cardGroup.enabled = newValue
            modifyCardGroup(cardGroup)
            cardGroupListView.refresh()
        }
        enabledCheckBox.selectedProperty().addListener(enabledCheckBoxListener)
    }

    private fun modifyCardGroup(cardGroup: CardGroupInfo) {
        modifiedCardGroups.add(cardGroup)
        markAsModified()
    }

    private fun modifyCurrentCardGroup() = currentCardGroup()?.let { modifyCardGroup(it) }

    /**
     * 标记有未保存的更改，首次修改时提示用户
     */
    private fun markAsModified() {
        if (hasNotifiedUser) return
        runUILater {
            if (!hasNotifiedUser) {
                hasNotifiedUser = true
                notificationManager.showInfo("离开此页面后自动保存", 3)
            }
        }
    }

    /**
     * 保存所有修改过的卡牌组并删除待删除的卡牌组
     */
    private fun saveAllModifiedCardGroups() {
        val hasChanges = modifiedCardGroups.isNotEmpty() || pendingDeleteCardGroups.isNotEmpty()
        if (!hasChanges) return

        // 保存修改的卡牌组
        for (cardGroup in modifiedCardGroups) {
            CardGroupUtil.saveCardGroup(cardGroup)
        }

        // 删除待删除的卡牌组
        for (cardGroupName in pendingDeleteCardGroups) {
            CardGroupUtil.deleteCardGroup(cardGroupName)
        }

        CardGroupUtil.applyEnabledCardGroups()
        modifiedCardGroups.clear()
        pendingDeleteCardGroups.clear()
    }

    // ============ 卡牌组操作 ============

    private fun waitCardGroupName(): String? {
        var textFiled: TextField? = null
        var name: String? = null
        var ok = false
        Modal(
            rootPane,
            "输入新建卡牌组名称",
            textField {
                textFiled = instance()
                styleMain(StyleSize.SMALL)
                settings {
                    addEnterKeyFilter { text, _ ->
                        if (text.isEmpty()) return@addEnterKeyFilter
                        name = text
                        ok = true
                        textFiled.ownWindow()?.hide()
                    }
                }
            },
            {
                name = textFiled?.text
            }, {}
        ).showAndWait()
        if (ok) {
            val groupName = name
            if (groupName.isNullOrBlank()) {
                notificationManager.showError("卡牌组名称不能为空", 2)
                return null
            }
            if (!CardGroupUtil.isValidCardGroupName(groupName)) {
                notificationManager.showError("卡牌组名称包含非法字符", 2)
                return null
            }
            if (cardGroupListView.items.any { it.name == groupName }) {
                notificationManager.showError("已存在同名卡牌组", 2)
                return null
            }
        }
        return name
    }

    override fun createCardGroup() {
        val cardGroupName = waitCardGroupName() ?: return
        val newCardGroup = CardGroupInfo(cardGroupName, true, emptyList())
        cardGroupListView.addAndScroll(newCardGroup)
        modifyCardGroup(newCardGroup)
    }

    override fun deleteCardGroup() {
        val selectedCardGroups = cardGroupListView.selectionModel.selectedItems.toList()
        if (selectedCardGroups.isEmpty()) {
            notificationManager.showInfo("请先选择要删除的卡牌组", 1)
            return
        }

        val cardGroupNames = selectedCardGroups.joinToString(",") { "「${it.name}」" }
        Modal(
            rootPane,
            null,
            "确定要删除卡牌组 $cardGroupNames 吗？",
            {
                for (cardGroup in selectedCardGroups) {
                    pendingDeleteCardGroups.add(cardGroup.name)
                    modifiedCardGroups.remove(cardGroup)
                    cardGroupListView.items.remove(cardGroup)
                }
                markAsModified()
            }, {}
        ).show()
    }

    override fun renameCardGroup() {
        val selectedCardGroup = currentCardGroup()
        if (selectedCardGroup == null) {
            notificationManager.showInfo("请先选择卡牌组", 1)
            return
        }

        val newName = cardGroupNameField.text.trim()
        if (newName.isBlank()) {
            notificationManager.showError("卡牌组名称不能为空", 1)
            return
        }
        if (!CardGroupUtil.isValidCardGroupName(newName)) {
            notificationManager.showError("卡牌组名称包含非法字符", 2)
            return
        }
        if (newName == selectedCardGroup.name) {
            return
        }
        if (cardGroupListView.items.any { it.name == newName }) {
            notificationManager.showError("已存在同名卡牌组", 2)
            return
        }

        val oldName = selectedCardGroup.name
        if (CardGroupUtil.renameCardGroup(oldName, newName)) {
            selectedCardGroup.name = newName
            cardGroupListView.refresh()
            notificationManager.showSuccess("重命名成功", 1)
        } else {
            notificationManager.showError("重命名失败", 1)
        }
    }

    override fun importCardGroup() {
        val btnWidth = 120.0
        Modal(
            rootPane,
            "导入卡牌组",
            null,
            button {
                val btn = instance()
                +"从文件"
                minWidth(btnWidth)
                style()
                graphic { FileIco() }
                onAction {
                    btn.scene?.window?.hide()
                    importCardGroupFromFile()
                }
            },
            button {
                val btn = instance()
                +"从卡组代码"
                minWidth(btnWidth)
                style()
                graphic { CodeIco() }
                onAction {
                    btn.scene?.window?.hide()
                    importCardGroupFromDeckCode()
                }
            },
            button {
                val btn = instance()
                +"从旧版文件"
                minWidth(btnWidth)
                styleWarn()
                graphic { FileIco() }
                onAction {
                    btn.scene?.window?.hide()
                    importCardGroupFromLegacyFiles()
                }
            }
        ).apply {
            isMaskClosable = true
            show()
        }
    }

    override fun exportCardGroup() {
        val btnWidth = 120.0
        Modal(
            rootPane,
            "导出卡牌组",
            null,
            button {
                val btn = instance()
                +"为文件"
                style()
                minWidth(btnWidth)
                graphic { FileIco() }
                onAction {
                    btn.scene?.window?.hide()
                    exportCardGroupAsFile()
                }
            },
            button {
                val btn = instance()
                +"为卡组代码"
                style()
                minWidth(btnWidth)
                graphic { CodeIco() }
                onAction {
                    btn.scene?.window?.hide()
                    exportCardGroupAsDeckCode()
                }
            }
        ).apply {
            isMaskClosable = true
            show()
        }
    }

    override fun copyCardGroup() {
        val selectedCardGroups = cardGroupListView.selectionModel.selectedItems.toList()
        if (selectedCardGroups.isEmpty()) {
            notificationManager.showInfo("请先选择要复制的卡牌组", 1)
            return
        }

        var copiedCount = 0
        for (cardGroup in selectedCardGroups) {
            // 生成新名称
            var newName = "${cardGroup.name}_副本"
            var counter = 1
            while (cardGroupListView.items.any { it.name == newName }) {
                newName = "${cardGroup.name}_副本$counter"
                counter++
            }

            val newCardGroup = CardGroupInfo(newName, cardGroup.enabled, cardGroup.cards.map { it.clone() })
            modifyCardGroup(newCardGroup)
            cardGroupListView.items.add(newCardGroup)
            copiedCount++
        }

        notificationManager.showSuccess("复制 $copiedCount 个卡牌组成功", 1)
    }

    @FXML
    protected fun exportCardGroupAsDeckCode() {
        val selectedCardGroup = currentCardGroup()
        if (selectedCardGroup == null) {
            notificationManager.showInfo("请先选择要导出的卡牌组", 1)
            return
        }

        // 检查是否有有效的 dbfId
        val cardsWithDbfId = selectedCardGroup.cards.filter { it.dbfId > 0 }
        if (cardsWithDbfId.isEmpty()) {
            notificationManager.showError("该卡牌组没有有效的卡牌（缺少dbfId），无法导出为卡组代码", 2)
            return
        }

        try {
            val decoder = DeckDecoder()
            val deckCode = decoder.encodeCardGroup(selectedCardGroup)

            // 复制到剪切板
            val clipboard = javafx.scene.input.Clipboard.getSystemClipboard()
            val content = javafx.scene.input.ClipboardContent()
            content.putString(deckCode)
            clipboard.setContent(content)

            notificationManager.showSuccess(
                "卡组代码已复制到剪切板（${cardsWithDbfId.size}/${selectedCardGroup.cards.size} 张卡牌）",
                2
            )
        } catch (e: Exception) {
            notificationManager.showError("导出失败: ${e.message}", 2)
        }
    }

    private fun exportCardGroupAsFile() {
        val selectedCardGroups = cardGroupListView.selectionModel.selectedItems.toList()
        if (selectedCardGroups.isEmpty()) {
            notificationManager.showInfo("请先选择要导出的卡牌组", 1)
            return
        }

        if (selectedCardGroups.size == 1) {
            val selectedCardGroup = selectedCardGroups[0]
            val chooser = FileChooser()
            chooser.title = "导出卡牌组至"
            chooser.initialFileName = "${selectedCardGroup.name}.${CARD_GROUP_FILE_EXT}"
            val extFilter =
                FileChooser.ExtensionFilter("卡牌组文件 (*.${CARD_GROUP_FILE_EXT})", "*.${CARD_GROUP_FILE_EXT}")
            chooser.extensionFilters.add(extFilter)

            val file = chooser.showSaveDialog(rootPane.scene.window)
            if (file == null) {
                notificationManager.showInfo("未选择导出路径，导出取消", 1)
                return
            }

            CardGroupUtil.exportCardGroup(selectedCardGroup, file.toPath())
            notificationManager.showSuccess("导出成功", 1)
        } else {
            val dirChooser = javafx.stage.DirectoryChooser()
            dirChooser.title = "选择导出目录"

            val dir = dirChooser.showDialog(rootPane.scene.window)
            if (dir == null) {
                notificationManager.showInfo("未选择导出目录，导出取消", 1)
                return
            }

            var exportedCount = 0
            for (cardGroup in selectedCardGroups) {
                val targetPath = Path(dir.absolutePath, "${cardGroup.name}.${CARD_GROUP_FILE_EXT}")
                CardGroupUtil.exportCardGroup(cardGroup, targetPath)
                exportedCount++
            }
            notificationManager.showSuccess("导出 $exportedCount 个卡牌组成功", 1)
        }
    }

    private fun importCardGroupFromFile() {
        val chooser = FileChooser()
        chooser.title = "选择要导入的卡牌组文件"
        val extFilter = FileChooser.ExtensionFilter("卡牌组文件 (*.${CARD_GROUP_FILE_EXT})", "*.${CARD_GROUP_FILE_EXT}")
        chooser.extensionFilters.add(extFilter)

        val files = chooser.showOpenMultipleDialog(rootPane.scene.window)
        if (files == null || files.isEmpty()) {
            notificationManager.showInfo("未选择导入路径，导入取消", 1)
            return
        }

        var importedCount = 0
        for (file in files) {
            val cardGroupInfo = CardGroupUtil.importCardGroup(file.toPath())
            if (cardGroupInfo != null) {
                // 检查是否存在同名卡牌组
                if (cardGroupListView.items.any { it.name == cardGroupInfo.name }) {
                    cardGroupInfo.name = "${cardGroupInfo.name}_${System.currentTimeMillis()}"
                }
                modifyCardGroup(cardGroupInfo)
                cardGroupListView.items.add(cardGroupInfo)
                importedCount++
            }
        }

        if (importedCount > 0) {
            notificationManager.showSuccess("导入 $importedCount 个卡牌组成功", 1)
        } else {
            notificationManager.showError("导入失败", 1)
        }
    }

    /**
     * 从旧版文件（*.info 和 *.weight）导入卡牌组
     */
    private fun importCardGroupFromLegacyFiles() {
        val fileChooser = FileChooser().apply {
            title = "选择旧版卡牌信息文件（*.info）"
            extensionFilters.add(FileChooser.ExtensionFilter("卡牌信息文件", "*.info"))
        }
        val infoFile: File? = fileChooser.showOpenDialog(rootPane.scene.window)

        fileChooser.title = "选择旧版卡牌权重文件（*.weight）"
        fileChooser.extensionFilters.setAll(FileChooser.ExtensionFilter("卡牌权重文件", "*.weight"))
        val weightFile: File? = fileChooser.showOpenDialog(rootPane.scene.window)

        if (infoFile == null && weightFile == null) {
            notificationManager.showInfo("未选择任何文件", 1)
            return
        }

        try {
            val objectMapper = ObjectMapper().apply {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }

            // 读取 info 文件
            val infoCards: List<LegacyInfoCard> = if (infoFile != null) {
                val infoListType =
                    objectMapper.typeFactory.constructCollectionType(List::class.java, LegacyInfoCard::class.java)
                objectMapper.readValue(infoFile, infoListType)
            } else {
                emptyList()
            }

            // 读取 weight 文件
            val weightCards: List<LegacyWeightCard> = if (weightFile != null) {
                val weightListType =
                    objectMapper.typeFactory.constructCollectionType(List::class.java, LegacyWeightCard::class.java)
                objectMapper.readValue(weightFile, weightListType)
            } else {
                emptyList()
            }

            // 以 cardId 为 key 建立映射
            val infoMap = infoCards.associateBy { it.cardId }
            val weightMap = weightCards.associateBy { it.cardId }

            // 合并所有 cardId
            val allCardIds = (infoCards.map { it.cardId } + weightCards.map { it.cardId }).toSet()

            // 合并数据创建 CardGroupCard 列表
            val cardGroupCards = allCardIds.map { cardId ->
                val info = infoMap[cardId]
                val weight = weightMap[cardId]
                CardGroupCard(
                    cardId = cardId,
                    name = info?.name ?: weight?.name ?: "",
                    effectType = info?.effectType ?: CardEffectTypeEnum.UNKNOWN,
                    playActions = info?.playActions ?: emptyList(),
                    powerActions = info?.powerActions ?: emptyList(),
                    weight = weight?.weight ?: 1.0,
                    powerWeight = weight?.powerWeight ?: 1.0,
                    changeWeight = weight?.changeWeight ?: 0.0
                )
            }

            if (cardGroupCards.isEmpty()) {
                notificationManager.showError("未能解析任何卡牌数据", 2)
                return
            }

            val name = waitCardGroupName() ?: return

            val cardGroupInfo = CardGroupInfo(name, true, cardGroupCards)
            cardGroupListView.items.add(cardGroupInfo)
            modifyCardGroup(cardGroupInfo)
            notificationManager.showSuccess("从旧版文件导入 ${cardGroupCards.size} 张卡牌成功", 2)

        } catch (e: Exception) {
            notificationManager.showError("导入失败：${e.message}", 2)
        }

    }

    /**
     * 旧版卡牌信息（*.info）数据结构
     */
    private data class LegacyInfoCard(
        val cardId: String = "",
        val name: String = "",
        val effectType: CardEffectTypeEnum? = null,
        val playActions: List<CardActionEnum>? = null,
        val powerActions: List<CardActionEnum>? = null
    )

    /**
     * 旧版卡牌权重（*.weight）数据结构
     */
    private data class LegacyWeightCard(
        val cardId: String = "",
        val name: String = "",
        val weight: Double = 1.0,
        val powerWeight: Double = 1.0,
        val changeWeight: Double = 0.0
    )

    private fun importCardGroupFromDeckCode(deckCode: String) {
        if (deckCode.isBlank()) {
            notificationManager.showError("卡组代码不能为空", 1)
            return
        }

        try {
            val decoder = DeckDecoder()
            val deckInfo = decoder.decode(deckCode.trim())

            // 获取所有卡牌的 dbfId
            val dbfIds = deckInfo.cards.map { it.dbfId }
            val cardsMap = CardDBUtil.queryCardsByDbfIds(dbfIds)

            if (cardsMap.isEmpty()) {
                notificationManager.showError("未能从数据库中找到任何卡牌信息", 2)
                return
            }

            // 构建卡牌列表
            val cards = mutableListOf<CardGroupCard>()
            var notFoundCount = 0
            for (cardInfo in deckInfo.cards) {
                val dbCard = cardsMap[cardInfo.dbfId]
                if (dbCard != null) {
                    val cardGroupCard = CardGroupCard(
                        cardId = dbCard.cardId,
                        dbfId = dbCard.dbfId,
                        name = dbCard.name,
                        playActions = listOf(CardActionEnum.NO_POINT),
                        powerActions = if (dbCard.type == CardTypeEnum.MINION.name ||
                            dbCard.type == CardTypeEnum.HERO.name ||
                            dbCard.type == CardTypeEnum.WEAPON.name
                        ) {
                            listOf(CardActionEnum.POINT_RIVAL)
                        } else if (dbCard.type == CardTypeEnum.SPELL.name) {
                            emptyList()
                        } else {
                            listOf(CardActionEnum.NO_POINT)
                        },
                        weight = 1.0,
                        powerWeight = 1.0,
                        changeWeight = if (dbCard.cost == null || dbCard.cost!! > 2) -1.0 else 0.0
                    )
                    cards.add(cardGroupCard)
                } else {
                    notFoundCount++
                }
            }

            // 检查是否有选中的卡牌组
            val selectedCardGroup = currentCardGroup()
            var importType = 0
            if (selectedCardGroup != null) {
                Modal(
                    rootPane,
                    "检测到已选中卡牌组",
                    "选择导入方式",
                    button {
                        val btn = instance()
                        +"创建新卡牌组"
                        style()
                        onAction {
                            btn.ownWindow()?.hide()
                            importType = 1
                        }
                    },
                    button {
                        val btn = instance()
                        +"添加到已选中的卡牌组"
                        style()
                        onAction {
                            btn.ownWindow()?.hide()
                            importType = 2
                        }
                    }
                ).showAndWait()
                when (importType) {
                    1 -> {

                    }

                    2 -> {
                        selectedCardGroup.cardsObservable.addAll(cards)
                        modifyCardGroup(selectedCardGroup)

                        if (notFoundCount > 0) {
                            notificationManager.showSuccess(
                                "已添加 ${cards.size} 张卡牌到「${selectedCardGroup.name}」（$notFoundCount 张未找到）",
                                2
                            )
                        } else {
                            notificationManager.showSuccess(
                                "已添加 ${cards.size} 张卡牌到「${selectedCardGroup.name}」",
                                1
                            )
                        }
                        return
                    }

                    else -> {
                        notificationManager.showInfo("取消导入", 1)
                        return
                    }
                }
            }

            val name = waitCardGroupName() ?: return
            // 创建并保存卡牌组
            val newCardGroup = CardGroupInfo(name, true, cards)
            cardGroupListView.items.add(newCardGroup)
            cardGroupListView.selectionModel.select(newCardGroup)
            modifyCardGroup(newCardGroup)

            if (notFoundCount > 0) {
                notificationManager.showSuccess("导入成功，共 ${cards.size} 张卡牌（$notFoundCount 张未找到）", 2)
            } else {
                notificationManager.showSuccess("导入成功，共 ${cards.size} 张卡牌", 1)
            }
        } catch (e: DeckParseException) {
            notificationManager.showError("卡组代码解析失败: ${e.message}", 2)
        } catch (e: Exception) {
            notificationManager.showError("导入失败: ${e.message}", 2)
        }
    }

    private fun waitDeckCode(): String? {
        var textFiled: TextField? = null
        var deckCode: String? = null
        Modal(
            rootPane,
            "请粘贴炉石传说卡组代码",
            textField {
                textFiled = instance()
                styleMain(StyleSize.SMALL)
                settings {
                    addEnterKeyFilter { text, _ ->
                        if (text.isEmpty()) return@addEnterKeyFilter
                        deckCode = text
                        textFiled.ownWindow()?.hide()
                    }
                }
            },
            {
                deckCode = textFiled?.text
            }, {}
        ).showAndWait()
        return deckCode
    }


    private fun importCardGroupFromDeckCode() {
        waitDeckCode()?.let {
            importCardGroupFromDeckCode(it)
        }
    }

    // ============ 卡牌操作 ============

    override fun addCard() {
        val cardGroup = currentCardGroup()
        if (cardGroup == null) {
            notificationManager.showInfo("请先选择或创建一个卡牌组", 1)
            return
        }

        val selectedItems = cardTable.selectionModel.selectedItems
        if (selectedItems.isEmpty()) {
            notificationManager.showInfo("左边数据表没有选中行", 1)
            return
        }

        val list = ArrayList(selectedItems)
        for (dbCard in list) {
            dbCard.run {
                val cost = cost
                val cardGroupCard = CardGroupCard(
                    cardId = cardId,
                    dbfId = dbfId,
                    name = name,
                    playActions = listOf(CardActionEnum.NO_POINT),
                    powerActions = listOf(CardActionEnum.NO_POINT),
                    weight = 1.0,
                    powerWeight = 1.0,
                    changeWeight = if (cost == null || cost > 2) -1.0 else 0.0
                )
                // 根据卡牌类型设置默认使用行为
                if (type == CardTypeEnum.MINION.name || type == CardTypeEnum.HERO.name || type == CardTypeEnum.WEAPON.name) {
                    cardGroupCard.powerActions = listOf(CardActionEnum.POINT_RIVAL)
                } else if (type == CardTypeEnum.SPELL.name) {
                    cardGroupCard.powerActions = emptyList()
                }
                cardGroup.cardsObservable.add(cardGroupCard)
                cardGroupCardTable.items.add(cardGroupCard)
            }
        }

        modifyCurrentCardGroup()
        cardGroupCardTable.scrollTo(cardGroupCardTable.items.size - 1)
        cardGroupCardTable.selectionModel.clearSelection()
        cardGroupCardTable.selectionModel.selectLast()
    }

    override fun removeCard() {
        val selectedItems = cardGroupCardTable.selectionModel.selectedItems
        if (selectedItems.isEmpty()) {
            notificationManager.showInfo("右边卡牌组表没有选中行", 1)
            return
        }
        val cards = ArrayList(selectedItems)
        currentCardGroup()?.cardsObservable?.removeAll(cards)
        cardGroupCardTable.items.removeAll(cards)
        modifyCurrentCardGroup()
    }

    override fun copyCard() {
        val selectedItems = cardGroupCardTable.selectionModel.selectedItems
        if (selectedItems.isEmpty()) {
            notificationManager.showInfo("请先选择要复制的行", 1)
            return
        }
        val selectedItemsCopy = selectedItems.toList()
        cardGroupCardTable.selectionModel.clearSelection()
        for (card in selectedItemsCopy) {
            val clonedCard = card.clone()
            currentCardGroup()?.cardsObservable?.add(clonedCard)
            cardGroupCardTable.items.add(clonedCard)
        }
        modifyCurrentCardGroup()
    }

    // ============ 行为编辑 ============

    private var editActionPopup: Popup? = null
    private var editActionPane: EditActionPane? = null

    private fun updatePopup(cardGroupCard: CardGroupCard, type: CardInfoActionTypeEnum) {
        editActionPane?.let { pane ->
            // 创建临时对象用于编辑
            val infoCard = InfoCard(
                cardGroupCard.cardId,
                cardGroupCard.name,
                cardGroupCard.effectType,
                cardGroupCard.playActions,
                cardGroupCard.powerActions
            )
            pane.setTitle("修改[${cardGroupCard.name}]的${type.comment}")
            pane.infoCard = infoCard
            pane.actionTypeEnum = type
            // 更新保存回调
            pane.onApply = {
                cardGroupCard.effectType = infoCard.effectType
                cardGroupCard.playActions = infoCard.playActions
                cardGroupCard.powerActions = infoCard.powerActions
                modifyCurrentCardGroup()
                val nextSelectedIndex = cardGroupCardTable.selectionModel.selectedIndex + 1
                if (nextSelectedIndex < cardGroupCardTable.items.size - 1) {
                    cardGroupCardTable.selectionModel.clearAndSelect(nextSelectedIndex)
                }
            }
            pane.update()
        }
    }

    private fun editAction(cardGroupCard: CardGroupCard, type: CardInfoActionTypeEnum) {
//        创建临时对象用于编辑
        val infoCard = InfoCard(
            cardGroupCard.cardId,
            cardGroupCard.name,
            cardGroupCard.effectType,
            cardGroupCard.playActions,
            cardGroupCard.powerActions
        )

        val popup = editActionPopup?.let {
            updatePopup(cardGroupCard, type)
            it
        } ?: let {
            val localToScreen = cardGroupCardTable.localToScreen(cardGroupCardTable.boundsInLocal)
            val popup = Popup().apply {
                x = localToScreen.maxX
                y = localToScreen.minY
            }

            val editActionPane = EditActionPane(infoCard, type) {
                // 同步值
                cardGroupCard.effectType = infoCard.effectType
                cardGroupCard.playActions = infoCard.playActions
                cardGroupCard.powerActions = infoCard.powerActions
                modifyCurrentCardGroup()
                val nextSelectedIndex = cardGroupCardTable.selectionModel.selectedIndex + 1
                if (nextSelectedIndex < cardGroupCardTable.items.size - 1) {
                    cardGroupCardTable.selectionModel.clearAndSelect(nextSelectedIndex)
                }
            }
            editActionPane.setTitle("修改[${cardGroupCard.name}]的${type.comment}")
            this.editActionPane = editActionPane
            popup.content.add(editActionPane)
            popup
        }
        editActionPopup = popup
        popup.show(rootPane.scene.window)
    }

    override fun onHidden() {
        editActionPopup?.hide()
        saveAllModifiedCardGroups()
    }

    fun clearSourceCard() {
        cutSourceCards = null
        cutSourceCardGroup = null
    }

    private fun copyCards(): Boolean {
        val selectedItems = currentCards()
        if (selectedItems.isNotEmpty()) {
            copiedCards.setAll(selectedItems.map { it.clone() })
            clearSourceCard()
            notificationManager.showSuccess("已复制 ${selectedItems.size} 张卡牌", 1)
            return true
        }
        return false
    }

    private fun cutCards(currentCardGroup: CardGroupInfo): Boolean {
        val selectedItems = cardGroupCardTable.selectionModel.selectedItems
        if (selectedItems.isNotEmpty()) {
            copiedCards.setAll(selectedItems.map { it.clone() })
            cutSourceCards = selectedItems.toList()
            cutSourceCardGroup = currentCardGroup
            notificationManager.showSuccess("已剪切 ${selectedItems.size} 张卡牌）", 1)
            return true
        }
        return false
    }

    private fun pasteCards(currentCardGroup: CardGroupInfo): Boolean {
        if (copiedCards.isNotEmpty()) {
            val pastedCards = copiedCards.map { it.clone() }

            val sourceCards = cutSourceCards
            val sourceGroup = cutSourceCardGroup
            val isCutOperation = sourceCards != null && sourceGroup != null
            if (isCutOperation) {
                sourceGroup.cardsObservable.removeAll(sourceCards)
                // 如果是当前选中的卡牌组，也需要从 TableView 中移除
                if (sourceGroup === currentCardGroup) {
                    cardGroupCardTable.items.removeAll(sourceCards)
                } else {
                    modifyCardGroup(sourceGroup)
                }
                copiedCards.clear()
            }
            clearSourceCard()

            currentCardGroup.cardsObservable.addAll(pastedCards)
            cardGroupCardTable.items.addAll(pastedCards)

            modifyCurrentCardGroup()

            if (isCutOperation) {
                notificationManager.showSuccess("已剪切粘贴 ${pastedCards.size} 张卡牌", 1)
            } else {
                notificationManager.showSuccess("已粘贴 ${pastedCards.size} 张卡牌", 1)
            }
            return true
        }
        return false
    }


    override fun handleKeyEvent(event: KeyEvent) {
        if (event.eventType === KeyEvent.KEY_PRESSED) {
            val currentCardGroup = currentCardGroup() ?: return
            if (event.isControlDown) {
                when (event.code) {
                    KeyCode.C -> {
                        if (copyCards()) event.consume()
                    }

                    KeyCode.X -> {
                        if (cutCards(currentCardGroup)) event.consume()
                    }

                    KeyCode.V -> {
                        if (pasteCards(currentCardGroup)) event.consume()
                    }

                    else -> {}
                }
            } else if (event.code === KeyCode.DELETE) {
                if (cardGroupCardTable.isFocused && cardGroupCardTable.selectionModel.selectedItems.isNotEmpty()) {
                    removeCard()
                    event.consume()
                }
            }
        }
    }


}
