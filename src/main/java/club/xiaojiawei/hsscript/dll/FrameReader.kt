package club.xiaojiawei.hsscript.dll

/**
 * @author 肖嘉威
 * @date 2025/8/20 11:54
 */

import java.nio.ByteBuffer
import java.nio.ByteOrder

class FrameReader {
    private var nativeHandle: Long = 0
    private var frameBuffer: ByteBuffer? = null
    private var isInitialized = false

    companion object {
        init {
            System.loadLibrary("reader") // 加载JNI库
        }
    }

    // 初始化读取器
    fun initialize(): Boolean {
        nativeHandle = nativeInit()
        if (nativeHandle != 0L) {
            isInitialized = true
            return true
        }
        return false
    }

    // 高性能轮询读取（推荐）
    fun tryReadFrame(): FrameData? {
        if (!isInitialized) return null

        val frameInfo = nativeGetFrameInfo(nativeHandle) ?: return null
        val width = frameInfo[0]
        val height = frameInfo[1]
        val frameCounter = frameInfo[2]

        if (width <= 0 || height <= 0) return null

        val bufferSize = width * height * 4

        // 复用或创建缓冲区
        if (frameBuffer == null || frameBuffer!!.capacity() < bufferSize) {
            frameBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
                order(ByteOrder.nativeOrder())
            }
        }

        frameBuffer?.clear()

        if (nativeTryRead(nativeHandle, frameBuffer!!)) {
            return FrameData(
                buffer = frameBuffer!!,
                width = width,
                height = height,
                frameCounter = frameCounter,
                format = frameInfo[3] // ABGR格式
            )
        }

        return null
    }

    // 阻塞式等待新帧
    fun waitForFrame(timeoutMs: Int = 1000): FrameData? {
        if (!isInitialized) return null

        val frameInfo = nativeGetFrameInfo(nativeHandle) ?: return null
        val width = frameInfo[0]
        val height = frameInfo[1]

        if (width <= 0 || height <= 0) return null

        val bufferSize = width * height * 4

        if (frameBuffer == null || frameBuffer!!.capacity() < bufferSize) {
            frameBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
                order(ByteOrder.nativeOrder())
            }
        }

        frameBuffer?.clear()

        if (nativeWaitAndRead(nativeHandle, frameBuffer!!, timeoutMs)) {
            val updatedInfo = nativeGetFrameInfo(nativeHandle) ?: return null
            return FrameData(
                buffer = frameBuffer!!,
                width = updatedInfo[0],
                height = updatedInfo[1],
                frameCounter = updatedInfo[2],
                format = updatedInfo[3]
            )
        }

        return null
    }

    fun waitForNewFrame(timeoutMs: Int = 1000): FrameData? {
        if (!isInitialized) return null

        try {
            val frameInfo = nativeGetFrameInfo(nativeHandle) ?: return null
            val width = frameInfo[0]
            val height = frameInfo[1]

            if (width <= 0 || height <= 0) return null

            val bufferSize = width * height * 4

            if (frameBuffer == null || frameBuffer!!.capacity() < bufferSize) {
                frameBuffer = ByteBuffer.allocateDirect(bufferSize).apply {
                    order(ByteOrder.nativeOrder())
                }
            }

            frameBuffer?.clear()

            if (nativeWaitForNewFrame(nativeHandle, frameBuffer!!, timeoutMs)) {
                val updatedInfo = nativeGetFrameInfo(nativeHandle) ?: return null
                return FrameData(
                    buffer = frameBuffer!!,
                    width = updatedInfo[0],
                    height = updatedInfo[1],
                    frameCounter = updatedInfo[2],
                    format = updatedInfo[3]
                )
            }

            return null

        } catch (e: Exception) {
            println("Exception in waitForNewFrame: ${e.message}")
            return null
        }
    }

    // 获取读取器统计信息
    fun getStats(): ReaderStats? {
        if (!isInitialized) return null

        val stats = nativeGetStats(nativeHandle) ?: return null
        return ReaderStats(
            framesWritten = stats[0],
            framesDropped = stats[1],
            readerCount = stats[2],
            version = stats[3]
        )
    }

    // 检查写入端是否活跃
    fun isWriterActive(): Boolean {
        if (!isInitialized) return false
        val frameInfo = nativeGetFrameInfo(nativeHandle) ?: return false
        return frameInfo[5] > 0 // dataReady flag
    }

    fun destroy() {
        if (isInitialized) {
            nativeDestroy(nativeHandle)
            isInitialized = false
            nativeHandle = 0
            frameBuffer = null
        }
    }

    // JNI函数声明
    private external fun nativeInit(): Long
    private external fun nativeTryRead(handle: Long, buffer: ByteBuffer): Boolean
    private external fun nativeWaitAndRead(handle: Long, buffer: ByteBuffer, timeoutMs: Int): Boolean
    private external fun nativeWaitForNewFrame(handle: Long, buffer: ByteBuffer, timeoutMs: Int): Boolean
    private external fun nativeGetFrameInfo(handle: Long): IntArray?
    private external fun nativeGetStats(handle: Long): IntArray?
    private external fun nativeDestroy(handle: Long)
}

// 帧数据类
data class FrameData(
    val buffer: ByteBuffer,
    val width: Int,
    val height: Int,
    val frameCounter: Int,
    val format: Int // 1 = ABGR
) {
    // 转换为ARGB IntArray (如果需要)
    fun toArgbArray(): IntArray {
        val pixels = IntArray(width * height)
        buffer.rewind()
        buffer.asIntBuffer().get(pixels)

        // ABGR -> ARGB 转换
        for (i in pixels.indices) {
            val abgr = pixels[i]
            pixels[i] = (abgr and 0xFF00FF00.toInt()) or           // 保持G和A
                       ((abgr and 0x00FF0000) ushr 16) or          // R->B
                       ((abgr and 0x000000FF) shl 16)              // B->R
        }

        return pixels
    }

    // 获取原始ABGR数据
    fun getAbgrArray(): IntArray {
        val pixels = IntArray(width * height)
        buffer.rewind()
        buffer.asIntBuffer().get(pixels)
        return pixels
    }
}

// 统计信息类
data class ReaderStats(
    val framesWritten: Int,
    val framesDropped: Int,
    val readerCount: Int,
    val version: Int
)

// 使用示例
class GameCaptureExample {
    private val frameReader = FrameReader()
    private var isRunning = false

    fun startCapture() {
        if (!frameReader.initialize()) {
            println("Failed to initialize frame reader")
            return
        }

        isRunning = true

        // 方案1: 高性能轮询模式
        Thread {
            var lastFrameCounter = 0
            while (isRunning) {
                val frameData = frameReader.tryReadFrame()
                if (frameData != null && frameData.frameCounter != lastFrameCounter) {
                    // 处理新帧
                    processFrame(frameData)
                    lastFrameCounter = frameData.frameCounter
                }

                // 短暂休眠避免100% CPU占用
                Thread.sleep(1) // 1ms = 1000 FPS max
            }
        }.start()

        // 方案2: 事件驱动模式（更节能）
        Thread {
            while (isRunning) {
                val frameData = frameReader.waitForFrame(100) // 100ms timeout
                if (frameData != null) {
                    processFrame(frameData)
                }
            }
        }.start()
    }

    private fun processFrame(frameData: FrameData) {
        println("Got frame: ${frameData.width}x${frameData.height}, counter: ${frameData.frameCounter}")

        // 选择处理方式:
        // 1. 直接使用ByteBuffer (最高性能)
        processRawBuffer(frameData.buffer, frameData.width, frameData.height)

        // 2. 转换为ARGB IntArray
        val argbPixels = frameData.toArgbArray()

        // 3. 使用ABGR原始数据
        val abgrPixels = frameData.getAbgrArray()
    }

    private fun processRawBuffer(buffer: ByteBuffer, width: Int, height: Int) {
        // 直接处理ByteBuffer，避免数组拷贝开销
        buffer.rewind()
        // ... 你的处理逻辑
    }

    fun stopCapture() {
        isRunning = false
        frameReader.destroy()
    }

    fun printStats() {
        val stats = frameReader.getStats()
        if (stats != null) {
            println("Writer Stats - Written: ${stats.framesWritten}, " +
                   "Dropped: ${stats.framesDropped}, " +
                   "Readers: ${stats.readerCount}")
        }
    }
}

// ===============================
// 构建和集成说明
// ===============================

/*
1. C++ 写入端编译:
   - 编译为DLL注入到游戏进程
   - 需要链接: d3d11.lib, dxgi.lib, ole32.lib, windowscodecs.lib

2. JNI 读取端编译:
   - 编译为动态库 (Windows: GameCaptureReader.dll)
   - 包含JNI桥接代码和读取端逻辑

3. Kotlin集成:
   - 将JNI库放在 java.library.path 中
   - 或使用 System.loadLibrary() 加载

4. 性能优化建议:
   - 读取端使用轮询模式获得最低延迟
   - 写入端线程设置高优先级
   - 使用DirectByteBuffer避免JNI拷贝开销
   - 考虑内存池复用ByteBuffer

5. 错误处理:
   - 检查共享内存魔数验证有效性
   - 监控写入端心跳时间戳
   - 处理游戏进程退出的情况

6. 多读取端支持:
   - 支持多个Kotlin进程同时读取
   - readerCount字段跟踪活跃读取者
   - 三缓冲区确保读取不冲突
*/

fun processRawFrame(buffer:ByteBuffer, width: Int, height: Int) {
    println("new frame")
}

fun main() {
    val frameReader = FrameReader()
    frameReader.initialize()

// 高性能轮询
    while (true) {
        val frame = frameReader.tryReadFrame()
        if (frame != null) {
            // 处理新帧 - 直接使用ByteBuffer最高性能
            processRawFrame(frame.buffer, frame.width, frame.height)
        }
        Thread.sleep(1) // 1ms = 最高1000 FPS
    }
}