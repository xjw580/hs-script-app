package club.xiaojiawei.hsscript.dll

import club.xiaojiawei.hsscript.consts.LIB_LOG_READER_FILE
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.config.log


/**
 * @author 肖嘉威
 * @date 2025/8/21 8:39
 */
object LogReader {

    init {
        SystemUtil.getDllFilePath(LIB_LOG_READER_FILE)?.let {
            if (it.exists()) {
                System.load(it.absolutePath)
            } else {
                log.error { "找不到$it" }
            }
        } ?: let {
            log.error { "找不到${LIB_LOG_READER_FILE}" }
        }
    }

    external fun init(): Boolean
    external fun readLines(maxLines: Int): Array<String?>?
    external fun readLineBlocking(timeoutMs: Long): String?
    external fun getStatus(): LogReaderStatus?
    external fun getReadPosition(): Long
    external fun setReadPosition(pos: Long)
    external fun reset()
    external fun close()

    class LogReaderStatus
        (val dataSize: Long, val bufferSize: Long, val usage: Float)

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val reader = LogReader
        if (!reader.init()) {
            System.err.println("Failed to initialize")
            return
        }


        // 批量读取
        val lines = reader.readLines(100)
        if (lines != null) {
            for (line in lines) {
                println(line)
            }
        }


        // 检查状态
        val status: LogReaderStatus = getStatus()!!
        println("Buffer usage: " + status.usage + "%")
    }
}