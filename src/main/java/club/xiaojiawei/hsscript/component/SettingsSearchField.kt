package club.xiaojiawei.hsscript.component

import club.xiaojiawei.controls.FilterField
import club.xiaojiawei.hsscript.bean.SettingItem
import club.xiaojiawei.hsscript.component.svg.HistoryIco
import club.xiaojiawei.hsscript.service.SettingsSearchService
import club.xiaojiawei.kt.dsl.config
import javafx.application.Platform
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.Cursor
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Popup
import java.util.*
import java.util.prefs.Preferences

/**
 * 设置搜索组件，带下拉结果列表和历史记录
 * @author 肖嘉威
 * @date 2026/1/28
 */
class SettingsSearchField : HBox() {

    private lateinit var searchField: FilterField

    private val popup = Popup()
    private val resultListView = ListView<Any>()  // 可以是 SettingItem 或 String（历史记录）
    private val searchResults = FXCollections.observableArrayList<SettingItem>()
    private val historyResults = FXCollections.observableArrayList<String>()

    /** 搜索结果选中回调 */
    val onItemSelectedProperty = SimpleObjectProperty<((SettingItem) -> Unit)?>()
    var onItemSelected: ((SettingItem) -> Unit)?
        get() = onItemSelectedProperty.get()
        set(value) = onItemSelectedProperty.set(value)

    private var debounceTimer: Timer? = null

    /** 历史记录存储 */
    private val prefs = Preferences.userNodeForPackage(this::class.java)
    private val historyKey = "search_history"
    private val maxHistorySize = 10
    private val searchHistory = mutableListOf<String>()

    /** 当前是否显示历史记录 */
    private var showingHistory = false

    /** 是否临时禁止历史弹出（选中后清空时使用） */
    private var suppressHistoryPopup = false

    init {
        config {
            alignLeft()
            spacing(10.0)
            padding(5.0, 20.0, 5.0, 20.0)
            add {
                FilterField().apply {
                    searchField = this
                    promptText = "搜索设置项..."
                    setHgrow(this, Priority.ALWAYS)
                    styleClass.addAll("text-field-ui", "text-field-ui-small")
                }
            }
            add {
                HistoryIco().apply {
                    cursor = Cursor.HAND
                    setOnMouseClicked { onHistoryBtnClicked() }
                    setOnMouseEntered { color = "main-color" }
                    setOnMouseExited { color = "" }
                }
            }
        }
        afterLoaded()
    }

    private fun afterLoaded() {
        config {
            alignCenter()
        }
        loadHistory()
        initPopup()
        addListeners()
    }

    /**
     * 加载历史记录
     */
    private fun loadHistory() {
        val historyStr = prefs.get(historyKey, "")
        if (historyStr.isNotBlank()) {
            searchHistory.clear()
            searchHistory.addAll(historyStr.split("\n").filter { it.isNotBlank() }.take(maxHistorySize))
        }
    }

    /**
     * 保存历史记录
     */
    private fun saveHistory() {
        prefs.put(historyKey, searchHistory.joinToString("\n"))
    }

    /**
     * 添加到历史记录
     */
    private fun addToHistory(keyword: String) {
        if (keyword.isBlank()) return
        searchHistory.remove(keyword)  // 移除重复项
        searchHistory.add(0, keyword)  // 添加到开头
        if (searchHistory.size > maxHistorySize) {
            searchHistory.removeAt(searchHistory.lastIndex)
        }
        saveHistory()
    }

    private fun initPopup() {
        resultListView.styleClass.addAll("list-view-ui")
        resultListView.maxHeight = 300.0
        resultListView.prefWidthProperty().unbind()
        resultListView.prefWidthProperty().bind(searchField.widthProperty())
        resultListView.setCellFactory {
            object : ListCell<Any>() {
                private val nameLabel = Label().apply { styleClass.add("setting-name") }
                private val descLabel = Label().apply { styleClass.add("setting-desc") }
                private val settingContainer = VBox(2.0, nameLabel, descLabel).apply {
                    styleClass.add("setting-cell")
                }

                // 历史记录使用单独的 Label
                private val historyNameLabel = Label().apply { styleClass.add("setting-name") }
                private val historyIcon = HistoryIco()
                private val deleteBtn = Label("×").apply {
                    styleClass.add("history-delete")
                    setOnMouseClicked { event ->
                        event.consume()
                        val listCell = this@apply.parent?.parent as? ListCell<*>
                        val cellItem = listCell?.item
                        if (cellItem is String) {
                            removeFromHistory(cellItem)
                        }
                    }
                }
                private val historyContainer = HBox(8.0, historyIcon, historyNameLabel, deleteBtn).apply {
                    styleClass.add("history-cell")
                }

                override fun updateItem(item: Any?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (empty || item == null) {
                        graphic = null
                    } else when (item) {
                        is SettingItem -> {
                            nameLabel.text = item.name
                            descLabel.text = if (item.groupName.isNotEmpty()) {
                                "${item.groupName} · ${item.description}"
                            } else {
                                item.description
                            }
                            descLabel.isVisible = descLabel.text.isNotEmpty()
                            descLabel.isManaged = descLabel.text.isNotEmpty()
                            graphic = settingContainer
                        }

                        is String -> {
                            historyNameLabel.text = item
                            graphic = historyContainer
                        }
                    }
                }
            }
        }

        // 点击结果项
        resultListView.setOnMouseClicked { event ->
            if (event.clickCount == 1) {
                selectCurrentItem()
            }
        }
        popup.content.add(resultListView)
        popup.isAutoHide = true
//        resultListView.stylesheets.add(javaClass.getResource("/fxml/css/settings/settings.css")?.toExternalForm() ?: "")
    }

    /**
     * 从历史记录中删除
     */
    private fun removeFromHistory(keyword: String) {
        searchHistory.remove(keyword)
        saveHistory()
        historyResults.remove(keyword)
        if (historyResults.isEmpty()) {
            hidePopup()
        } else {
            resultListView.items = FXCollections.observableArrayList(historyResults as List<Any>)
        }
    }

    private fun addListeners() {
        searchField.textProperty().addListener { _, _, newValue ->
            debounceSearch(newValue)
        }

        searchField.setOnKeyPressed { event ->
            when (event.code) {
                KeyCode.DOWN -> {
                    if (popup.isShowing) {
                        resultListView.requestFocus()
                        if (resultListView.selectionModel.isEmpty) {
                            resultListView.selectionModel.selectFirst()
                        }
                    }
                }

                KeyCode.ENTER -> {
                    if (popup.isShowing) {
                        selectCurrentItem()
                    }
                }

                KeyCode.ESCAPE -> hidePopup()
                else -> {}
            }
        }

        resultListView.setOnKeyPressed { event ->
            when (event.code) {
                KeyCode.ENTER -> selectCurrentItem()
                KeyCode.ESCAPE -> {
                    hidePopup()
                    searchField.requestFocus()
                }

                else -> {}
            }
        }

        searchField.focusedProperty().addListener { _, _, focused ->
            if (!focused && !resultListView.isFocused) {
                hidePopup()
            }
        }
    }

    /**
     * 历史记录图标点击事件
     */
    private fun onHistoryBtnClicked() {
        if (popup.isShowing && showingHistory) {
            hidePopup()
        } else if (searchHistory.isNotEmpty()) {
            showHistory()
        }
    }

    /**
     * 显示历史记录
     */
    fun showHistory() {
        historyResults.setAll(searchHistory)
        resultListView.items = FXCollections.observableArrayList(historyResults as List<Any>)
        showingHistory = true
        showPopup()
    }

    private fun debounceSearch(text: String) {
        debounceTimer?.cancel()
        debounceTimer = Timer().apply {
            schedule(object : TimerTask() {
                override fun run() {
                    Platform.runLater { performSearch(text) }
                }
            }, 200)
        }
    }

    private fun performSearch(keyword: String) {
        if (keyword.isBlank()) {
            hidePopup()
            return
        }

        val results = SettingsSearchService.search(keyword)
        searchResults.setAll(results)
        showingHistory = false

        if (results.isEmpty()) {
            hidePopup()
        } else {
            resultListView.items = FXCollections.observableArrayList(searchResults as List<Any>)
            showPopup()
        }
    }

    private fun showPopup() {
        if (!popup.isShowing) {
            val bounds = searchField.localToScreen(searchField.boundsInLocal)
            if (bounds != null) {
                popup.show(searchField, bounds.minX, bounds.maxY)
            }
        }
    }

    private fun hidePopup() {
        popup.hide()
        showingHistory = false
    }

    private fun selectCurrentItem() {
        val selected = resultListView.selectionModel.selectedItem
        when {
            selected is SettingItem -> {
                addToHistory(selected.name)
                onItemSelected?.invoke(selected)
                suppressHistoryPopup = true
                searchField.clear()
                hidePopup()
            }

            selected is String -> {
                // 点击历史记录，填充到搜索框并搜索
                searchField.text = selected
                searchField.positionCaret(selected.length)
            }

            !showingHistory && searchResults.isNotEmpty() -> {
                // 如果没有选中但有结果，选择第一个
                val first = searchResults.first()
                addToHistory(first.name)
                onItemSelected?.invoke(first)
                searchField.clear()
                hidePopup()
            }
        }
    }

    fun clear() {
        searchField.clear()
        hidePopup()
    }

    /**
     * 请求搜索框获取焦点
     */
    fun requestSearchFocus() {
        searchField.requestFocus()
        searchField.selectAll()
    }
}
