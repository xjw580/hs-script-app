package club.xiaojiawei.hsscript.component

import club.xiaojiawei.hsscript.utils.SystemUtil
import javafx.geometry.Bounds
import javafx.scene.Node
import javafx.scene.control.Hyperlink
import javafx.scene.layout.StackPane
import javafx.stage.Popup

/**
 * @author 肖嘉威
 * @date 2025/2/10 20:40
 */
class UrlLabel() : Hyperlink() {

    var url: String? = null

    var file: String? = null

    var content: Node? = null

    private var contentPopup: Popup? = null

    init {
        setOnAction {
            invoke()
        }
    }

    fun invoke() {
        url?.let {
            SystemUtil.openURL(it)
        }
        file?.let {
            SystemUtil.openFile(it)
        }
        content?.let { node ->
            showContentPopup(node)
        }
    }

    private fun showContentPopup(node: Node) {
        val shadowRadius = 10.0
        val popup = contentPopup ?: Popup().apply {
            isAutoHide = true
            val container = StackPane(node).apply {
                style = "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.3), ${shadowRadius.toInt()}, 0, 0, 2);"
            }
            content.add(container)
            contentPopup = this
        }

        if (popup.isShowing) {
            popup.hide()
        } else {
            val bounds: Bounds = localToScreen(boundsInLocal)
            popup.show(scene.window, bounds.minX - shadowRadius, bounds.maxY)
        }
    }

}