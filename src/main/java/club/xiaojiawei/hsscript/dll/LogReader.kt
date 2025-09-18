package club.xiaojiawei.hsscript.dll

import club.xiaojiawei.hsscript.consts.LIB_LOG_READER_FILE
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.config.log

/**
 * @author 肖嘉威
 * @date 2025/9/10 22:12
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

    external fun nativeInit(): Boolean

    external fun nativeOpenChannel(filename: String): Int

    external fun nativeReadLine(readId: Int): String?

    external fun nativeReadLines(readId: Int, maxLines: Int): Array<String>?

    external fun nativeGetPosition(readId: Int): Long

    external fun nativeGetWritePos(readId: Int): Long

    external fun nativeSetPosition(readId: Int, position: Long)

    external fun nativeGetAvailable(readId: Int): Long

    external fun nativeReset(readId: Int)

    external fun nativeCloseChannel(readId: Int)

    external fun nativeGetChannelName(readId: Int): String?

    external fun nativeGetChannelId(filename: String): Int

    external fun nativeGetActiveChannels(): Array<String>?

    external fun nativeCleanup()

    fun existChannel(filename: String): Boolean {
        return nativeOpenChannel(filename) >= 0
    }

}