package club.xiaojiawei.hsscript.status

import club.xiaojiawei.hsscript.bean.MemoryLogFile
import java.util.concurrent.ConcurrentHashMap

/**
 * @author 肖嘉威
 * @date 2025/9/10 22:12
 */

/**
 * 日志文件管理器 - 管理多个LogFile实例
 */
object LogFileManager {

    private val openFiles = ConcurrentHashMap<String, MemoryLogFile>()

    /**
     * 打开或获取日志文件
     */
    fun open(filename: String): MemoryLogFile {
        return openFiles.computeIfAbsent(filename) { MemoryLogFile(it) }
    }

    /**
     * 关闭指定文件
     */
    fun close(filename: String) {
        openFiles.remove(filename)?.close()
    }

    /**
     * 关闭所有文件
     */
    fun closeAll() {
        openFiles.values.forEach { it.close() }
        openFiles.clear()
    }

    /**
     * 获取所有打开的文件
     */
    fun getOpenFiles(): List<String> = openFiles.keys().toList()

}