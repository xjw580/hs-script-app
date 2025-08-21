package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.dll.CaptureReader
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @author 肖嘉威
 * @date 2025/8/20 11:54
 */
class FrameReader : Closeable {
    private var nativeHandle: Long = 0
    private var frameBuffer: ByteBuffer? = null
    private var isInitialized = false

    // 初始化读取器
    fun initialize(): Boolean {
        nativeHandle = CaptureReader.nativeInit()
        if (nativeHandle != 0L) {
            isInitialized = true
            return true
        }
        return false
    }

    // 高性能轮询读取（推荐）
    fun tryReadFrame(): FrameData? {
        if (!isInitialized) return null

        val frameInfo = CaptureReader.nativeGetFrameInfo(nativeHandle) ?: return null
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

        if (CaptureReader.nativeTryRead(nativeHandle, frameBuffer!!)) {
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

        val frameInfo = CaptureReader.nativeGetFrameInfo(nativeHandle) ?: return null
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

        if (CaptureReader.nativeWaitAndRead(nativeHandle, frameBuffer!!, timeoutMs)) {
            val updatedInfo = CaptureReader.nativeGetFrameInfo(nativeHandle) ?: return null
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
            val frameInfo = CaptureReader.nativeGetFrameInfo(nativeHandle) ?: return null
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

            if (CaptureReader.nativeWaitForNewFrame(nativeHandle, frameBuffer!!, timeoutMs)) {
                val updatedInfo = CaptureReader.nativeGetFrameInfo(nativeHandle) ?: return null
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

        val stats = CaptureReader.nativeGetStats(nativeHandle) ?: return null
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
        val frameInfo = CaptureReader.nativeGetFrameInfo(nativeHandle) ?: return false
        return frameInfo[5] > 0 // dataReady flag
    }

    override fun close() {
        destroy()
    }

    fun destroy() {
        if (isInitialized) {
            CaptureReader.nativeDestroy(nativeHandle)
            isInitialized = false
            nativeHandle = 0
            frameBuffer = null
        }
    }
}

// 帧数据类
data class FrameData(
    val buffer: ByteBuffer,
    val width: Int,
    val height: Int,
    val frameCounter: Int, // 帧计数器
    val format: Int // 像素格式(0=ARGB, 1=ABGR)
) {
    fun toArgbArray(): IntArray? {
        if (format == 0) {
            val pixels = IntArray(width * height)
            buffer.rewind()
            buffer.asIntBuffer().get(pixels)
            return pixels
        } else if (format == 1) {
            val pixels = IntArray(width * height)
            buffer.rewind()
            buffer.asIntBuffer().get(pixels)

            // ABGR -> ARGB 转换
            for (i in pixels.indices) {
                val abgr = pixels[i]
                pixels[i] = (abgr and 0xFF00FF00.toInt()) or
                        ((abgr and 0x00FF0000) ushr 16) or
                        ((abgr and 0x000000FF) shl 16)
            }

            return pixels
        }
        return null
    }

    fun getAbgrArray(): IntArray? {
        if (format == 1) {
            val pixels = IntArray(width * height)
            buffer.rewind()
            buffer.asIntBuffer().get(pixels)
            return pixels
        } else if (format == 0) {
            val pixels = IntArray(width * height)
            buffer.rewind()
            buffer.asIntBuffer().get(pixels)

            // ABGR -> ARGB 转换
            for (i in pixels.indices) {
                val abgr = pixels[i]
                pixels[i] = (abgr and 0xFF00FF00.toInt()) or
                        ((abgr and 0x00FF0000) ushr 16) or
                        ((abgr and 0x000000FF) shl 16)
            }

            return pixels
        }
        return null
    }
}

// 统计信息类
data class ReaderStats(
    val framesWritten: Int, // 已写入帧数
    val framesDropped: Int, // 丢弃帧数
    val readerCount: Int, // 当前读取进程数量
    val version: Int, // 版本号
)