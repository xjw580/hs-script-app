package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscriptbase.bean.LRunnable
import club.xiaojiawei.hsscriptbase.config.VIRTUAL_THREAD_POOL
import club.xiaojiawei.controls.ico.CopyIco
import club.xiaojiawei.controls.ico.OKIco
import club.xiaojiawei.hsscriptbase.util.isFalse
import club.xiaojiawei.hsscriptbase.util.isTrue
import javafx.animation.PauseTransition
import javafx.application.Platform
import javafx.beans.property.Property
import javafx.beans.value.WritableValue
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.util.Duration
import java.util.concurrent.Future
import kotlin.properties.ReadWriteProperty
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

/**
 * 为 Property<T> 创建委托，直接操作其值
 */
operator fun <T> WritableValue<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    return this.value
}

operator fun <T> WritableValue<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    this.value = value
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

