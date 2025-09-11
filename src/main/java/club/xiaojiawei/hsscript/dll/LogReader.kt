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

    external fun nativeReadLine(channelId: Int): String?

    external fun nativeReadLines(channelId: Int, maxLines: Int): Array<String>?

    external fun nativeGetPosition(channelId: Int): Long

    external fun nativeGetWritePos(channelId: Int): Long

    external fun nativeSetPosition(channelId: Int, position: Long)

    external fun nativeGetAvailable(channelId: Int): Long

    external fun nativeReset(channelId: Int)

    external fun nativeCloseChannel(channelId: Int)

    external fun nativeCleanup()

    fun existChannel(filename: String): Boolean {
        return nativeOpenChannel(filename) >= 0
    }

}