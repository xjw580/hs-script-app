package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.interfaces.LogFile
import java.io.ByteArrayOutputStream
import java.io.RandomAccessFile

/**
 * @author 肖嘉威
 * @date 2025/9/11 16:43
 */
class DiskLogFile(val filepath: String, val mode: String = "r") : LogFile {

    private val innerFile: RandomAccessFile = RandomAccessFile(filepath, mode)

    private val bufSize = 8192
    private val buffer = ByteArray(bufSize)
    private var bufferPos = 0
    private var bufferLimit = 0
    private var needSeek = false  // 标记是否需要 seek
    private var nextFilePos = 0L   // 下次读取的文件位置

    init {
        nextFilePos = innerFile.filePointer
    }

    private fun fillBuffer(): Int {
        // 只在必要时才 seek
        if (needSeek) {
            innerFile.seek(nextFilePos)
            needSeek = false
        }

        bufferPos = 0
        bufferLimit = innerFile.read(buffer)
        if (bufferLimit > 0) {
            nextFilePos = innerFile.filePointer  // 更新下次读取位置
        }
        return bufferLimit
    }

    override fun readLine(): String? {
        val line = ByteArrayOutputStream()

        while (true) {
            // 缓冲区为空时才读取
            if (bufferPos >= bufferLimit) {
                if (fillBuffer() == -1) {
                    return if (line.size() > 0) line.toString(Charsets.UTF_8) else null
                }
            }

            // 在缓冲区中查找换行符
            val startPos = bufferPos
            while (bufferPos < bufferLimit) {
                val b = buffer[bufferPos]
                if (b == '\n'.code.toByte() || b == '\r'.code.toByte()) {
                    // 写入换行符之前的内容
                    if (bufferPos > startPos) {
                        line.write(buffer, startPos, bufferPos - startPos)
                    }

                    // 处理换行符
                    bufferPos++
                    if (b == '\r'.code.toByte() && bufferPos < bufferLimit &&
                        buffer[bufferPos] == '\n'.code.toByte()) {
                        bufferPos++
                    }

                    // 不需要 seek！下次 fillBuffer 会从正确位置继续读
                    return line.toString(Charsets.UTF_8)
                }
                bufferPos++
            }

            // 将当前缓冲区剩余内容写入
            line.write(buffer, startPos, bufferPos - startPos)
        }
    }

    override fun seek(position: Long) {
        val newPos = position.coerceIn(0, innerFile.length())
        if (newPos != nextFilePos - (bufferLimit - bufferPos)) {
            // 只有真正需要跳转时才标记
            nextFilePos = newPos
            needSeek = true
            bufferPos = 0
            bufferLimit = 0
        }
    }

    override fun readLines(maxLines: Int): List<String> {
        val lines = mutableListOf<String>()
        var line = readLine()
        while (line != null && lines.size < maxLines) {
            lines.add(line)
            line = readLine()
        }
        return lines
    }


    override fun readAll(): List<String> = readLines(Int.MAX_VALUE)

    override fun getPosition(): Long {
        // 返回逻辑位置（考虑缓冲区）
        return nextFilePos - (bufferLimit - bufferPos)
    }

    override fun reset() = innerFile.seek(0)

    override fun length(): Long = innerFile.length()

    override fun path(): String = filepath

    override fun close() = innerFile.close()

}