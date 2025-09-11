package club.xiaojiawei.hsscript.interfaces

import java.io.Closeable

/**
 * @author 肖嘉威
 * @date 2025/9/11 16:36
 */
interface LogFile : Closeable {

    fun readLine(): String?

    fun readLines(maxLines: Int = 100): List<String>

    fun readAll(): List<String>

    fun getPosition(): Long

    fun seek(position: Long)

    fun reset()

    fun length(): Long

    fun path(): String

    override fun close()

}