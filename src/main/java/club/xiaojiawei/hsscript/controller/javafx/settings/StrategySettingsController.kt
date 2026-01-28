package club.xiaojiawei.hsscript.controller.javafx.settings

import club.xiaojiawei.controls.NotificationManager
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.ScrollPane
import javafx.scene.control.TitledPane
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Duration
import java.net.URL
import java.util.*

/**
 *
 * @author 肖嘉威
 * @date 2023/9/10 15:07
 */
class StrategySettingsController : Initializable {
    @FXML
    protected lateinit var rootPane: StackPane

    @FXML
    protected lateinit var notificationManager: NotificationManager<Any>

    @FXML
    protected lateinit var scrollPane: ScrollPane

    @FXML
    protected lateinit var titledRootPane: VBox

    @FXML
    protected lateinit var strategyPane: Group

    @FXML
    protected lateinit var surrenderPane: Group

    @FXML
    protected lateinit var strategyNavigation: ToggleButton

    @FXML
    protected lateinit var surrenderNavigation: ToggleButton

    @FXML
    protected lateinit var navigationBarToggle: ToggleGroup

    private var forbidSetToggle = false
    private var strategyMaxY = 0.0
    private var strategyMinY = 0.0
    private var surrenderMaxY = 0.0
    private var surrenderMinY = 0.0

    override fun initialize(url: URL?, resourceBundle: ResourceBundle?) {
        listen()
    }

    private fun listen() {
        navigationBarToggle.selectedToggleProperty().addListener { _, oldToggle, newToggle ->
            newToggle ?: let {
                navigationBarToggle.selectToggle(oldToggle)
            }
        }
        scrollPane.vvalueProperty().addListener { _, oldValue, newValue ->
            if (forbidSetToggle) return@addListener
            updateY()
            val newV = newValue.toDouble()
            val oldV = oldValue.toDouble()
            if (newV - oldV > 0) {
                if (newV > strategyMaxY) {
                    navigationBarToggle.selectToggle(surrenderNavigation)
                }
            } else {
                if (newV <= strategyMinY) {
                    navigationBarToggle.selectToggle(strategyNavigation)
                } else if (newV <= surrenderMinY) {
                    navigationBarToggle.selectToggle(surrenderNavigation)
                }
            }
        }
    }

    private fun updateY() {
        val diffH = titledRootPane.height - scrollPane.viewportBounds.height
        strategyMaxY = strategyPane.boundsInParent.maxY / diffH
        strategyMinY = strategyPane.boundsInParent.minY / diffH
        surrenderMaxY = surrenderPane.boundsInParent.maxY / diffH
        surrenderMinY = surrenderPane.boundsInParent.minY / diffH
    }

    private fun scrollTo(pane: Node) {
        pane.boundsInParent.let {
            val targetV = it.minY / (titledRootPane.height - scrollPane.viewportBounds.height)
            val sourceV = scrollPane.vvalue
            Timeline(
                KeyFrame(
                    Duration.millis(0.0), KeyValue(scrollPane.vvalueProperty(), sourceV)
                ), KeyFrame(
                    Duration.millis(200.0), KeyValue(
                        scrollPane.vvalueProperty(), targetV
                    )
                )
            ).run {
                forbidSetToggle = true
                onFinished = EventHandler {
                    forbidSetToggle = false
                }
                play()
            }
        }
    }

    @FXML
    protected fun scrollStrategy(actionEvent: ActionEvent) {
        scrollTo(strategyPane)
    }

    @FXML
    protected fun scrollSurrender(actionEvent: ActionEvent) {
        scrollTo(surrenderPane)
    }
}