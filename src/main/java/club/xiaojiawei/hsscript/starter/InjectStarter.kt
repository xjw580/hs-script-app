package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.consts.GAME_US_NAME
import club.xiaojiawei.hsscript.consts.INJECT_UTIL_FILE
import club.xiaojiawei.hsscript.consts.LIB_HS_FILE
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.dll.FrameData
import club.xiaojiawei.hsscript.dll.FrameReader
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.config.log
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.stage.Stage


/**
 * 启动游戏
 * @author 肖嘉威
 * @date 2023/7/5 14:38
 */
class InjectStarter : AbstractStarter() {

    private var imageView: ImageView? = null

    fun update(frameData: FrameData) {
        go {
            imageView?.let { imageView ->
                val width = frameData.width
                val height = frameData.height
                val image = let {
                    var img = imageView.image
                    if (img != null && (img.width.toInt() != width || img.height.toInt() != height)) {
                        img = null
                    }
                    img
                } ?: let {
                    val img = WritableImage(width, height)
                    imageView.image = img
                    img
                }
                if (image is WritableImage) {
                    val pixelWriter = image.pixelWriter
                    // 刷新图像到 WritableImage
                    pixelWriter.setPixels(
                        0,                 // x 起点
                        0,                 // y 起点
                        width,             // 宽度
                        height,            // 高度
                        PixelFormat.getByteBgraInstance(), // ARGB 格式
                        frameData.buffer,         // 像素数据
                        width * 4          // 每行的字节数
                    )
                }
            }
        }

    }

    override fun execStart() {
        val mouseControlMode = ConfigExUtil.getMouseControlMode()
        val acHook = ConfigUtil.getBoolean(ConfigEnum.PREVENT_AC)
        val mouseHook = mouseControlMode === MouseControlModeEnum.MESSAGE
        val limitMouseRange = ConfigUtil.getBoolean(ConfigEnum.LIMIT_MOUSE_RANGE)
        val autoRefreshGameTask = ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)

        log.info { "鼠标控制模式：${mouseControlMode.name}" }
        log.info { "阻止游戏反作弊：$acHook" }
        if (mouseHook ||
            acHook ||
            limitMouseRange ||
            autoRefreshGameTask
        ) {
            if (ScriptStatus.gameHWND == null || !injectCheck()) {
                pause()
                return
            }
            val delay = 200L
            val maxRetry = 10_000 / delay
            var retryI = 0
            go {
                while (!CSystemDll.INSTANCE.isConnected() && retryI++ < maxRetry) {
                    Thread.sleep(delay)
                }
                if (autoRefreshGameTask) {
                    log.info { "capturing" }
                    CSystemDll.INSTANCE.capture(true)
                    log.info { "captured" }
                    runUI {
                        Stage().apply {
                            width = 384.0 * 2
                            height = 216.0 * 2
                            imageView = ImageView().apply {
                                fitWidthProperty().bind(widthProperty())
                                fitHeightProperty().bind(heightProperty())
                            }
                            scene = Scene(StackPane(imageView).apply {
                                style = "-fx-background-color: pink;"
                            })
                            isAlwaysOnTop = true
                            show()
                        }
                    }
                    go {
                        Thread.sleep(10_000)
                        log.info { "frameReader" }
                        val frameReader = FrameReader()
                        log.info { "initialize" }
                        frameReader.initialize()
                        log.info { "initialized" }
                        while (true) {
                            // 方式1: 纯轮询（最可靠）
                            val frame = frameReader.tryReadFrame()
                            if (frame != null) {
                                // 处理新帧 - 直接使用ByteBuffer最高性能
                                update(frame)
                            }
                            Thread.sleep(1) // 1ms = 最高1000 FPS

                            // 方式2: 基于帧计数器的等待（推荐）
//                            val frame2 = frameReader.waitForNewFrame(10) // 10ms超时
//                            if (frame2 != null) {
//                                update(frame2)
//                            }
                        }
                    }
                }
                if (mouseHook) {
                    CSystemDll.INSTANCE.mouseHook(true)
                }
                if (limitMouseRange) {
                    CSystemDll.INSTANCE.limitMouseRange(true)
                }
            }
        } else {
            log.info { "无需注入" }
        }
        startNextStarter()
    }

    private fun injectCheck(): Boolean {
        val injectFile = SystemUtil.getExeFilePath(INJECT_UTIL_FILE) ?: return false
        val dllFile = SystemUtil.getDllFilePath(LIB_HS_FILE) ?: return false
        return InjectUtil.execInject(injectFile.absolutePath, dllFile.absolutePath, "$GAME_US_NAME.exe")
    }
}
