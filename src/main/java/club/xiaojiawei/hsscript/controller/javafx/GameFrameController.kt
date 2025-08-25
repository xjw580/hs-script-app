package club.xiaojiawei.hsscript.controller.javafx

import club.xiaojiawei.controls.NotificationManager
import club.xiaojiawei.hsscript.bean.FrameData
import club.xiaojiawei.hsscript.bean.FrameReader
import club.xiaojiawei.hsscript.consts.GAME_CN_NAME
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.interfaces.StageHook
import club.xiaojiawei.hsscript.starter.InjectStarter
import club.xiaojiawei.hsscript.utils.GameUtil
import club.xiaojiawei.hsscript.utils.go
import club.xiaojiawei.hsscriptbase.config.log
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import java.net.URL
import java.util.*

/**
 * @author 肖嘉威
 * @date 2025/8/21 10:18
 */

class GameFrameController : Initializable, StageHook {

    @FXML
    protected lateinit var notificationManager: NotificationManager<String>

    @FXML
    protected lateinit var gameFrameView: ImageView

    @FXML
    protected lateinit var rootPane: StackPane

    @FXML
    protected fun closeWindow() {
        rootPane.scene?.window?.hide()
    }

    fun handleFrame(frameData: FrameData) {
        go {
            val width = frameData.width
            val height = frameData.height
            val image = let {
                var img = gameFrameView.image
                if (img != null && (img.width.toInt() != width || img.height.toInt() != height)) {
                    img = null
                }
                img
            } ?: let {
                val img = WritableImage(width, height)
                gameFrameView.image = img
                img
            }
            if (image is WritableImage) {
                val pixelWriter = image.pixelWriter
                pixelWriter.setPixels(
                    0,
                    0,
                    width,
                    height,
                    PixelFormat.getByteBgraInstance(),
                    frameData.buffer,
                    width * 4
                )
            }
        }
    }

    @Volatile
    private var running = false

    @FXML
    protected fun startCapture() {
        if (running) return
        if (!GameUtil.isAliveOfGame()) {
            notificationManager.showInfo("请先打开${GAME_CN_NAME}", 2)
        }
        InjectStarter().start()
        CSystemDll.INSTANCE.capture(true)
        val frameReader = FrameReader().apply {
            initialize()
        }
        running = true
        log.info { "开始捕获" }
        go {
            while (running) {
                val frame = frameReader.tryReadFrame()
                if (frame != null) {
                    handleFrame(frame)
                }
                Thread.sleep(1)
            }
            frameReader.close()
            CSystemDll.INSTANCE.capture(false)
        }
    }

    @FXML
    protected fun stopCapture() {
        if (!running) return
        running = false
        log.info { "停止捕获" }
    }

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
    }

    override fun onHiding() {
        stopCapture()
    }

    override fun onShown() {
        startCapture()
    }
}
//
//fun main() {
//    val instance: ITesseract = Tesseract()
//    instance.setDatapath(Path.of(TESS_DATA_PATH).toString())
//    instance.setLanguage("chi_sim")
//    instance.setVariable("tessedit_char_whitelist", "0123456789/");
//    val result = instance.doOCR(File("C:\\Users\\28671\\Downloads\\1.png")).replace("\\s".toRegex(), "")
//    println(result)
//}