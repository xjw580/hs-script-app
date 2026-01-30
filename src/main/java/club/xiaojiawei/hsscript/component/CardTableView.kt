package club.xiaojiawei.hsscript.component

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.controls.TableFilterManagerGroup
import club.xiaojiawei.hsscript.bean.tableview.NoEditTextFieldTableCell
import club.xiaojiawei.hsscriptcardsdk.bean.DBCard
import club.xiaojiawei.hsscriptcardsdk.util.CardDBUtil
import javafx.beans.property.SimpleIntegerProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
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