package club.xiaojiawei.hsscript.component

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.controls.TableFilterManagerGroup
import club.xiaojiawei.hsscript.bean.SearchCardTableColumnConfig
import club.xiaojiawei.hsscript.bean.tableview.NoEditTextFieldTableCell
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscriptcardsdk.bean.DBCard
import club.xiaojiawei.hsscriptcardsdk.util.CardDBUtil
import club.xiaojiawei.kt.dsl.contextMenu
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.geometry.Side
import javafx.scene.control.Button
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ContextMenu
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.util.StringConverter
import java.util.stream.IntStream


/**
 * @author 肖嘉威
 * @date 2025/2/27 9:00
 */
class CardTableView : TableView<DBCard>() {

    @FXML
    protected var colMinWeight: Double = 0.0

    @FXML
    protected lateinit var noCol: TableColumn<DBCard, Number?>

    @FXML
    protected lateinit var cardIdCol: TableColumn<DBCard, String>

    @FXML
    protected lateinit var nameCol: TableColumn<DBCard, String>

    @FXML
    protected lateinit var attackCol: TableColumn<DBCard, Number>

    @FXML
    protected lateinit var healthCol: TableColumn<DBCard, Number>

    @FXML
    protected lateinit var costCol: TableColumn<DBCard, Number>

    @FXML
    protected lateinit var textCol: TableColumn<DBCard, String>

    @FXML
    protected lateinit var typeCol: TableColumn<DBCard, String>

    @FXML
    protected lateinit var cardSetCol: TableColumn<DBCard, String>

    private val cardTableProxy: TableFilterManagerGroup<DBCard, DBCard> = TableFilterManagerGroup()

    private val columnVisibilityConfig =
        ConfigUtil.getObject(ConfigEnum.CARD_SEARCH_TABLE_COLUMNS, SearchCardTableColumnConfig::class.java)
            ?: SearchCardTableColumnConfig()

    private val noColVisible = SimpleBooleanProperty(columnVisibilityConfig.no)
    private val cardIdColVisible = SimpleBooleanProperty(columnVisibilityConfig.cardId)
    private val nameColVisible = SimpleBooleanProperty(columnVisibilityConfig.name)
    private val attackColVisible = SimpleBooleanProperty(columnVisibilityConfig.attack)
    private val healthColVisible = SimpleBooleanProperty(columnVisibilityConfig.health)
    private val costColVisible = SimpleBooleanProperty(columnVisibilityConfig.cost)
    private val textColVisible = SimpleBooleanProperty(columnVisibilityConfig.text)
    private val typeColVisible = SimpleBooleanProperty(columnVisibilityConfig.type)
    private val cardSetColVisible = SimpleBooleanProperty(columnVisibilityConfig.cardSet)
    private val columnSettingMenu = contextMenu {
        style()
    }

    init {
        val fxmlLoader = FXMLLoader(javaClass.getResource("/fxml/component/CardTableView.fxml"))
        fxmlLoader.setRoot(this)
        fxmlLoader.setController(this)
        fxmlLoader.load<Any>()
        afterLoaded()
    }

    var notificationManager: NotificationManager<String>? = null

    var currentOffset = 0

    private fun afterLoaded() {
        initTable()
        initColumnSettingMenu()
        updateColumnVisibility()
    }

    private fun initColumnSettingMenu() {
        addColumnToggleItem("序号", noColVisible)
        addColumnToggleItem("ID", cardIdColVisible)
        addColumnToggleItem("名字", nameColVisible)
        addColumnToggleItem("攻击力", attackColVisible)
        addColumnToggleItem("血量", healthColVisible)
        addColumnToggleItem("费用", costColVisible)
        addColumnToggleItem("描述", textColVisible)
        addColumnToggleItem("类型", typeColVisible)
        addColumnToggleItem("扩展包", cardSetColVisible)
    }

    private fun addColumnToggleItem(text: String, property: BooleanProperty) {
        val item = CheckMenuItem(text)
        item.selectedProperty().bindBidirectional(property)
        property.addListener { _, _, _ ->
            saveColumnVisibilityConfig()
            updateColumnVisibility()
        }
        columnSettingMenu.items.add(item)
    }

    private fun saveColumnVisibilityConfig() {
        ConfigUtil.putObject(
            ConfigEnum.CARD_SEARCH_TABLE_COLUMNS,
            SearchCardTableColumnConfig(
                no = noColVisible.get(),
                cardId = cardIdColVisible.get(),
                name = nameColVisible.get(),
                attack = attackColVisible.get(),
                health = healthColVisible.get(),
                cost = costColVisible.get(),
                text = textColVisible.get(),
                type = typeColVisible.get(),
                cardSet = cardSetColVisible.get(),
            )
        )
    }

    private fun updateColumnVisibility() {
        noCol.isVisible = noColVisible.get()
        cardIdCol.isVisible = cardIdColVisible.get()
        nameCol.isVisible = nameColVisible.get()
        attackCol.isVisible = attackColVisible.get()
        healthCol.isVisible = healthColVisible.get()
        costCol.isVisible = costColVisible.get()
        textCol.isVisible = textColVisible.get()
        typeCol.isVisible = typeColVisible.get()
        cardSetCol.isVisible = cardSetColVisible.get()
    }

    fun bindColumnSettingButton(button: Button) {
        button.setOnAction {
            columnSettingMenu.show(button, Side.BOTTOM, 0.0, 0.0)
        }
    }

    private fun initTable() {
        this.selectionModel.selectionMode = SelectionMode.MULTIPLE
        this.isEditable = true
        cardTableProxy.tableView = this
        cardTableProxy.isAutoRegisterColFilter = true
        val stringConverter: StringConverter<String?> = object : StringConverter<String?>() {
            override fun toString(`object`: String?): String? {
                return `object`
            }

            override fun fromString(string: String?): String? {
                return string
            }
        }
        val cellBuilder = {
            object : NoEditTextFieldTableCell<DBCard?, String?>(stringConverter) {
                override fun commitEdit(s: String?) {
                    super.commitEdit(s)
                    notificationManager?.showInfo("不允许修改", 1)
                }
            }
        }

        noCol.setCellValueFactory { param: TableColumn.CellDataFeatures<DBCard, Number?> ->
            val items = param.tableView.items
            val index =
                IntStream.range(0, items.size).filter { i: Int -> items[i] === param.value }.findFirst().orElse(-2)
            SimpleIntegerProperty(index + 1 + currentOffset)
        }
        noCol.text = "#"
        noCol.maxWidth = 30.0

        cardIdCol.cellValueFactory = PropertyValueFactory("cardId")
        cardIdCol.setCellFactory { cellBuilder() }

        nameCol.cellValueFactory = PropertyValueFactory("name")
        nameCol.setCellFactory { cellBuilder() }

        attackCol.cellValueFactory = PropertyValueFactory("attack")
        healthCol.cellValueFactory = PropertyValueFactory("health")
        costCol.cellValueFactory = PropertyValueFactory("cost")

        textCol.cellValueFactory = PropertyValueFactory("text")
        textCol.setCellFactory { cellBuilder() }

        typeCol.cellValueFactory = PropertyValueFactory("type")
        typeCol.setCellFactory { cellBuilder() }

        cardSetCol.cellValueFactory = PropertyValueFactory("cardSet")
        cardSetCol.setCellFactory { cellBuilder() }
    }

    fun setCardByName(name: String, limit: Int, offset: Int) {
        this.currentOffset = offset
        cardTableProxy.resetFilter()
        cardTableProxy.setAll(CardDBUtil.queryCardByName(name, limit, offset, false))
    }

    fun setCardById(id: String, limit: Int, offset: Int) {
        this.currentOffset = offset
        cardTableProxy.resetFilter()
        cardTableProxy.setAll(CardDBUtil.queryCardById(id, limit, offset, false))
    }

    fun addCardByName(name: String, limit: Int, offset: Int) {
        cardTableProxy.resetFilter()
        this.items.addAll(CardDBUtil.queryCardByName(name, limit, offset, false))
    }

    fun addCardById(id: String, limit: Int, offset: Int) {
        cardTableProxy.resetFilter()
        this.items.addAll(CardDBUtil.queryCardById(id, limit, offset, false))
    }

}