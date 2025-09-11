package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.dll.LogReader
import club.xiaojiawei.hsscript.interfaces.LogFile
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class MemoryLogFile(val filename: String, private val recoverReadPos: Boolean = false) : LogFile {
    /**
     * 日志通道id
     */
    private var channelId: Int = -1

    /**
     * 日志读取位置
     */
    private val position = AtomicLong(0)
    private val lock = ReentrantReadWriteLock()
    private var closed = false

    init {
        require(LogReader.nativeInit()) { "初始化日志读取器系统失败" }
        open()
    }

    /**
     * 打开日志文件通道
     */
    private fun open() {
        lock.write {
            check(!closed) { "【${filename}】已经被关闭" }
            channelId = LogReader.nativeOpenChannel(filename)
            if (channelId < 0) {
                throw IllegalStateException("无法打开日志通道: 【${filename}】")
            }
            if (recoverReadPos) {
                position.set(LogReader.nativeGetPosition(channelId))
            }
        }
    }

    /**
     * 读取一行
     */
    override fun readLine(): String? {
        return lock.read {
            checkOpen()
            LogReader.nativeReadLine(channelId)?.also {
                position.set(LogReader.nativeGetPosition(channelId))
            }
        }
    }

    /**
     * 读取多行
     */
    override fun readLines(maxLines: Int): List<String> {
        return lock.read {
            checkOpen()
            LogReader.nativeReadLines(channelId, maxLines)?.toList()?.also {
                position.set(LogReader.nativeGetPosition(channelId))
            } ?: emptyList()
        }
    }

    /**
     * 读取所有可用行
     */
    override fun readAll(): List<String> {
        return lock.read {
            checkOpen()
            val lines = mutableListOf<String>()
            var line = LogReader.nativeReadLine(channelId)
            while (line != null) {
                lines.add(line)
                line = LogReader.nativeReadLine(channelId)
            }
            position.set(LogReader.nativeGetPosition(channelId))
            lines
        }
    }

    /**
     * 获取当前读取位置
     */
    override fun getPosition(): Long = position.get()

    /**
     * 设置读取位置
     */
    override fun seek(position: Long) {
        lock.write {
            checkOpen()
            LogReader.nativeSetPosition(channelId, position)
            this.position.set(position)
        }
    }

    /**
     * 获取可读数据大小
     */
    fun available(): Long {
        return lock.read {
            checkOpen()
            LogReader.nativeGetAvailable(channelId)
        }
    }

    /**
     * 重置读取位置到开始
     */
    override fun reset() {
        lock.write {
            checkOpen()
            LogReader.nativeReset(channelId)
            position.set(0)
        }
    }

    override fun length(): Long {
        return lock.write {
            checkOpen()
            LogReader.nativeGetWritePos(channelId)
        }
    }

    override fun path(): String = filename

    /**
     * 跳过指定行数
     */
    fun skip(lines: Int): Int {
        return lock.write {
            checkOpen()
            var skipped = 0
            repeat(lines) {
                if (LogReader.nativeReadLine(channelId) != null) {
                    skipped++
                } else {
                    return@write skipped
                }
            }
            position.set(LogReader.nativeGetPosition(channelId))
            skipped
        }
    }

    /**
     * 检查是否有新数据
     */
    fun hasNewData(): Boolean = available() > 0

    /**
     * 关闭文件
     */
    override fun close() {
        lock.write {
            if (!closed && channelId >= 0) {
                channelId = -1
                closed = true
            }
        }
    }

    private fun reallyClose() {
        lock.write {
            if (!closed && channelId >= 0) {
                LogReader.nativeCloseChannel(channelId)
                channelId = -1
                closed = true
            }
        }
    }

    private fun checkOpen() {
        check(!closed && channelId >= 0) { "【${filename}】被关闭或没有初始化" }
    }

}