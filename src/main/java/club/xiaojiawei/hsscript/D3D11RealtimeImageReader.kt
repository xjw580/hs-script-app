package club.xiaojiawei.hsscript

/**
 * @author 肖嘉威
 * @date 2025/8/20 9:45
 */
import club.xiaojiawei.hsscript.dll.KernelExDll
import club.xiaojiawei.hsscript.utils.goByLock
import club.xiaojiawei.hsscript.utils.runUI
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT.HANDLE
import javafx.scene.Scene
import javafx.scene.image.ImageView
import javafx.scene.image.PixelFormat
import javafx.scene.image.WritableImage
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread


// 共享内存头部结构对应
data class SharedImageHeader(
    var currentBufferIndex: Int = 0,    // 当前可读缓冲区 (0 or 1)
    var frameCounter: Int = 0,          // 帧计数器
    var width: Int = 0,                 // 图像宽度
    var height: Int = 0,                // 图像高度
    var format: Int = 0,                // 像素格式 (0=ARGB, 1=ABGR)
    var bufferSize: Int = 0,            // 每个缓冲区大小
    var timestamp: Long = 0L            // 时间戳
)

class D3D11RealtimeImageReader {
    companion object {
        private const val HEADER_SIZE = 64
        private const val CURRENT_BUFFER_OFFSET = 0
        private const val FRAME_COUNTER_OFFSET = 4
        private const val WIDTH_OFFSET = 8
        private const val HEIGHT_OFFSET = 12
        private const val FORMAT_OFFSET = 16
        private const val BUFFER_SIZE_OFFSET = 20
        private const val TIMESTAMP_OFFSET = 24

        private const val SHARED_MEMORY_NAME = "hs-script-file-map"
        private const val FRAME_EVENT_NAME = "hs-script-frame-event"
    }

    private var hMapFile: HANDLE? = null
    private var pBuf: Pointer? = null
    private var hFrameEvent: HANDLE? = null

    private val isRunning = AtomicBoolean(false)
    private val lastFrameCounter = AtomicInteger(0)
    private val frameCount = AtomicLong(0)
    private val lastFpsTime = AtomicLong(System.currentTimeMillis())

    // 回调接口
    interface FrameCallback {
        fun onNewFrame(frameData: FrameData, fps: Double)
        fun onError(error: String)
    }

    fun initialize(): Boolean {
        try {
            // 打开共享内存
            val sharedName = WString(SHARED_MEMORY_NAME)
            hMapFile = KernelExDll.INSTANCE.OpenFileMappingW(
                KernelExDll.FILE_MAP_READ,
                false,
                sharedName
            )

            if (hMapFile == null) {
                val error = Kernel32.INSTANCE.GetLastError()
                println("无法打开共享内存，错误码: $error")
                return false
            }

            // 映射共享内存
            pBuf = Kernel32.INSTANCE.MapViewOfFile(
                hMapFile,
                KernelExDll.FILE_MAP_READ,
                0, 0, 0
            )

            if (pBuf == null) {
                val error = Kernel32.INSTANCE.GetLastError()
                println("MapViewOfFile失败，错误码: $error")
                cleanup()
                return false
            }

            // 尝试打开帧事件（可选）
            try {
                val eventName = WString(FRAME_EVENT_NAME)
                hFrameEvent = KernelExDll.INSTANCE.OpenEventW(
                    Kernel32.EVENT_ALL_ACCESS,
                    false,
                    eventName
                )
                if (hFrameEvent != null) {
                    println("成功连接到帧事件，将使用事件通知模式")
                } else {
                    println("未找到帧事件，将使用轮询模式")
                }
            } catch (e: Exception) {
                println("无法打开帧事件: ${e.message}")
            }

            println("D3D11图像读取器初始化成功")
            return true

        } catch (e: Exception) {
            println("初始化失败: ${e.message}")
            cleanup()
            return false
        }
    }

    fun startReading(callback: FrameCallback) {
        if (pBuf == null) {
            callback.onError("共享内存未初始化")
            return
        }

        isRunning.set(true)

        // 启动读取线程
        thread(name = "D3D11FrameReader", isDaemon = true) {
            var consecutiveErrors = 0
            val maxConsecutiveErrors = 100

            while (isRunning.get()) {
                try {
                    val frameData = readCurrentFrame()

                    if (frameData != null) {
                        val fps = calculateFPS()
                        callback.onNewFrame(frameData, fps)
//                        val image = createBufferedImage(frameData)
//                        if (image != null) {
//                            // 计算FPS
//                            val fps = calculateFPS()
//                            callback.onNewFrame(image, fps)
//                            consecutiveErrors = 0
//                        } else {
//                            consecutiveErrors++
//                        }
                    } else {
                        consecutiveErrors++
                    }

                    // 错误恢复机制
                    if (consecutiveErrors > maxConsecutiveErrors) {
                        callback.onError("连续读取失败过多，暂停读取")
                        Thread.sleep(1000)
                        consecutiveErrors = 0
                    }

                    // 等待策略
                    waitForNextFrame()

                } catch (e: Exception) {
                    callback.onError("读取帧时发生错误: ${e.message}")
                    consecutiveErrors++
                    Thread.sleep(50)
                }
            }
        }
    }

    private fun readCurrentFrame(): FrameData? {
        val buffer = pBuf ?: return null

        try {
            // 读取帧计数器
            val currentFrameCounter = buffer.getInt(FRAME_COUNTER_OFFSET.toLong())

            // 检查是否有新帧
            if (currentFrameCounter == lastFrameCounter.get()) {
                return null // 没有新帧
            }

            // 读取头部信息
            val currentBufferIndex = buffer.getInt(CURRENT_BUFFER_OFFSET.toLong())
            val width = buffer.getInt(WIDTH_OFFSET.toLong())
            val height = buffer.getInt(HEIGHT_OFFSET.toLong())
            val format = buffer.getInt(FORMAT_OFFSET.toLong())
            val bufferSize = buffer.getInt(BUFFER_SIZE_OFFSET.toLong())

            // 验证数据有效性
            if (width <= 0 || height <= 0 || currentBufferIndex < 0 || currentBufferIndex > 1) {
                return null
            }

            val expectedSize = width * height * 4
            if (bufferSize != expectedSize || bufferSize > 8192 * 8192 * 4) {
                return null
            }

            // 计算当前缓冲区的偏移
            val bufferOffset = HEADER_SIZE + (currentBufferIndex * bufferSize)

            // 读取图像数据
            val imageBuffer = buffer.getByteBuffer(bufferOffset.toLong(), bufferSize.toLong())
            val imageData = ByteArray(bufferSize)
            imageBuffer.get(imageData)

            // 再次检查帧计数器，确保读取期间没有更新
            val endFrameCounter = buffer.getInt(FRAME_COUNTER_OFFSET.toLong())
            if (currentFrameCounter != endFrameCounter) {
                // 读取期间有更新，重试
                return readCurrentFrame()
            }

            lastFrameCounter.set(currentFrameCounter)

            return FrameData(imageData, width, height, format)

        } catch (e: Exception) {
            println("读取帧数据失败: ${e.message}")
            return null
        }
    }

    private fun createBufferedImage(frameData: FrameData): BufferedImage? {
        try {
            val image = BufferedImage(frameData.width, frameData.height, BufferedImage.TYPE_INT_ARGB)

            // 将字节数据转换为像素数组
            val pixels = IntArray(frameData.width * frameData.height)
            val data = frameData.imageData

            for (i in pixels.indices) {
                val base = i * 4
                val b = data[base].toUByte().toInt()
                val g = data[base + 1].toUByte().toInt()
                val r = data[base + 2].toUByte().toInt()
                val a = data[base + 3].toUByte().toInt()

                when (frameData.format) {
                    0 -> { // ARGB格式
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }

                    1 -> { // ABGR格式 (需要转换为ARGB)
                        pixels[i] = (a shl 24) or (b shl 16) or (g shl 8) or r
                    }

                    else -> {
                        pixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
                    }
                }
            }

            image.setRGB(0, 0, frameData.width, frameData.height, pixels, 0, frameData.width)
            return image

        } catch (e: Exception) {
            println("创建BufferedImage失败: ${e.message}")
            return null
        }
    }

    private fun waitForNextFrame() {
        if (hFrameEvent != null) {
            // 事件模式：等待新帧事件，超时100ms
            Kernel32.INSTANCE.WaitForSingleObject(hFrameEvent, 100)
        } else {
            // 轮询模式：根据目标帧率等待
            Thread.sleep(16) // 约60FPS
        }
    }

    private fun calculateFPS(): Double {
        val currentCount = frameCount.incrementAndGet()
        val currentTime = System.currentTimeMillis()
        val lastTime = lastFpsTime.get()

        if (currentTime - lastTime >= 1000) { // 每秒更新一次FPS
            val fps = currentCount * 1000.0 / (currentTime - lastTime)
            lastFpsTime.set(currentTime)
            frameCount.set(0)
            return fps
        }

        return 0.0
    }

    fun stop() {
        isRunning.set(false)
    }

    fun cleanup() {
        stop()

        pBuf?.let {
            Kernel32.INSTANCE.UnmapViewOfFile(it)
            pBuf = null
        }

        hMapFile?.let {
            Kernel32.INSTANCE.CloseHandle(it)
            hMapFile = null
        }

        hFrameEvent?.let {
            Kernel32.INSTANCE.CloseHandle(it)
            hFrameEvent = null
        }
    }

    data class FrameData(
        val imageData: ByteArray,
        val width: Int,
        val height: Int,
        val format: Int
    )
}

// 使用示例
class RealtimeImageDisplay {
    private val reader = D3D11RealtimeImageReader()

    private var imageView: ImageView? = null

    fun start() {
        if (!reader.initialize()) {
            println("初始化图像读取器失败")
            return
        }

        runUI {
            Stage().apply {
                width = 384.0
                height = 216.0
                imageView = ImageView().apply {
                    fitWidthProperty().bind(widthProperty())
                    fitHeightProperty().bind(heightProperty())
                }
                scene = Scene(StackPane(imageView).apply {
                    style = "-fx-background-color: pink;"
                })
                show()
            }
            reader.startReading(object : D3D11RealtimeImageReader.FrameCallback {
                override fun onNewFrame(frameData: D3D11RealtimeImageReader.FrameData, fps: Double) {
                    // 处理新帧
                    handleNewFrame(frameData, fps)
                }

                override fun onError(error: String) {
                    println("错误: $error")
                }
            })

            println("开始实时读取D3D11图像...")
        }


    }

    private fun handleNewFrame(frameData: D3D11RealtimeImageReader.FrameData, fps: Double) {
        // 在这里处理接收到的图像
        // 可以显示到Swing/JavaFX窗口，或者进行其他处理

        if (fps > 0) {
            goByLock(this) {
                val data = frameData.imageData
                if (data[0].toInt() and 0x11 == 0) {
                    println("无效帧")
                    return@goByLock
                }
                val width = frameData.width
                val height = frameData.height
                println("收到新帧: ${width}x$height, FPS: ${"%.1f".format(fps)}")
                imageView?.let { imageView ->
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
                            data,         // 像素数据
                            0,                 // 偏移量
                            width * 4          // 每行的字节数
                        )
                    }
                }
            }
        }
    }

    fun stop() {
        reader.cleanup()
    }
}

// 主函数示例
fun main() {
    val display = RealtimeImageDisplay()

    // 启动读取
    display.start()

    // 运行一段时间后停止
    Thread.sleep(10000) // 运行10秒
    display.stop()

    println("图像读取已停止")
}