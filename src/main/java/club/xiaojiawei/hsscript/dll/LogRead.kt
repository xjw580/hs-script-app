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
    external fun readLineBlocking(): String?
    external fun readLineNonBlocking(): String?
    external fun getReadPosition(): Long
    external fun setReadPosition(newPos: Long): Long
    external fun close()

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val reader = LogReader
        if (!reader.init()) {
            println("初始化失败")
            return
        }
        var readPos: Long = 0
        while (true) {
            val line = reader.readLineBlocking() // 阻塞读取
            if (line != null) {
                println("line:${line}")
                readPos = reader.getReadPosition()
                println("pos:${readPos}")
            }
            Thread.sleep(100)
        }
    }
}