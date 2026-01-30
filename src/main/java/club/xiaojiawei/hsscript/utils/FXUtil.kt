package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.controls.ico.CopyIco
import club.xiaojiawei.controls.ico.OKIco
import club.xiaojiawei.hsscriptbase.bean.LRunnable
import club.xiaojiawei.hsscriptbase.config.VIRTUAL_THREAD_POOL
import club.xiaojiawei.hsscriptbase.util.isFalse
import club.xiaojiawei.hsscriptbase.util.isTrue
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.beans.value.WritableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseEvent
import javafx.stage.Window
import javafx.util.Duration
import java.util.concurrent.Future
import kotlin.reflect.KProperty

/**
 * @author 肖嘉威
 * @date 2024/9/28 14:22
 */

/**
 * 虚拟线程中执行
 */
inline fun go(crossinline block: () -> Unit): Future<*> {
    return VIRTUAL_THREAD_POOL.submit(LRunnable {
        block()
    })
}

inline fun goByLock(lock: Any, crossinline block: () -> Unit): Future<*> {
    return VIRTUAL_THREAD_POOL.submit(LRunnable {
        synchronized(lock) {
            block()
        }
    })
}

/**
 * 虚拟线程中执行
 */
fun Runnable.go(): Future<*> {
    return VIRTUAL_THREAD_POOL.submit(LRunnable { this.run() })
}

/**
 * 虚拟线程中执行
 */
fun (() -> Any).goWithResult(): Future<*> {
    return VIRTUAL_THREAD_POOL.submit(LRunnable { this() })
}

/**
 * 确保在ui线程中执行
 */
inline fun runUI(crossinline block: () -> Unit) {
    Platform.isFxApplicationThread().isTrue {
        block()
    }.isFalse {
        Platform.runLater(LRunnable { block() })
    }
}

fun Node.ownWindow(): Window? = this.scene?.window

fun TextField.addKeyFilter(
    eventType: EventType<KeyEvent> = KeyEvent.KEY_RELEASED,
    keyCode: KeyCode,
    callback: (event: KeyEvent) -> Unit
) {
    val handler = EventHandler<KeyEvent> {
        if (it.code === keyCode) {
            callback(it)
        }
    }
    this.addEventFilter(eventType, handler)
}

fun TextField.addEnterKeyFilter(
    eventType: EventType<KeyEvent> = KeyEvent.KEY_RELEASED,
    callback: (text: String, event: KeyEvent) -> Unit
) {
    val handler = EventHandler<KeyEvent> {
        if (it.code === KeyCode.ENTER) {
            callback(this.text, it)
        }
    }
    this.addEventFilter(eventType, handler)
}

fun <T> ListView<T>.addDeleteKeyFilter(
    eventType: EventType<KeyEvent> = KeyEvent.KEY_RELEASED,
    callback: (event: KeyEvent) -> Unit
) {
    val handler = EventHandler<KeyEvent> {
        if (it.code === KeyCode.DELETE) {
            callback(it)
        }
    }
    this.addEventFilter(eventType, handler)
}

fun <T> TableView<T>.addDeleteKeyFilter(
    eventType: EventType<KeyEvent> = KeyEvent.KEY_RELEASED,
    callback: (event: KeyEvent) -> Unit
) {
    val handler = EventHandler<KeyEvent> {
        if (it.code === KeyCode.DELETE) {
            callback(it)
        }
    }
    this.addEventFilter(eventType, handler)
}

/**
 * 为 Property<T> 创建委托，直接操作其值
 */
operator fun <T> WritableValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return this.value
}

operator fun <T> WritableValue<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
}

fun <T> ListView<T>.scrollToLast() {
    if (this.items.isNotEmpty()) {
        this.scrollTo(this.items.size - 1)
    }
}

fun <T> ListView<T>.scrollToFirst() {
    if (this.items.isNotEmpty()) {
        this.scrollTo(0)
    }
}

fun <T> ListView<T>.addAndScroll(item: T, index: Int = -1) {
    if (index < 0) {
        items.add(item)
        scrollToLast()
    } else {
        items.add(index, item)
        scrollTo(index)
    }
}


object FXUtil {

    /**
     * 构建复制节点
     */
    fun buildCopyNode(clickHandler: Runnable?, tooltip: String? = null, opacity: Double = 0.9): Node {
        val graphicLabel = Label()
        val copyIco = CopyIco()
        val icoColor = "#e4e4e4"
        copyIco.color = icoColor
        graphicLabel.graphic = copyIco
        tooltip?.let {
            graphicLabel.tooltip = Tooltip(it)
        }
        graphicLabel.style = """
                    -fx-cursor: hand;
                    -fx-alignment: CENTER;
                    -fx-pref-width: 22;
                    -fx-pref-height: 22;
                    -fx-background-radius: 3;
                    -fx-background-color: rgba(128,128,128,${opacity});
                    -fx-font-size: 10;
                    """.trimIndent()
        graphicLabel.onMouseClicked = EventHandler { actionEvent: MouseEvent? ->
            clickHandler?.run()
            val okIco = OKIco()
            okIco.color = icoColor
            graphicLabel.graphic = okIco
            val pauseTransition = PauseTransition(Duration.millis(1000.0))
            pauseTransition.onFinished = EventHandler { actionEvent1: ActionEvent? ->
                graphicLabel.graphic = copyIco
            }
            pauseTransition.play()
        }
        return graphicLabel
    }

}

