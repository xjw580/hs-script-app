package club.xiaojiawei.hsscript.dll

import club.xiaojiawei.hsscript.consts.LIB_CAPTURE_READER_FILE
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.config.log
import java.nio.ByteBuffer

/**
 * @author 肖嘉威
 * @date 2025/8/21 8:39
 */
object CaptureReader {

    init {
        SystemUtil.getDllFilePath(LIB_CAPTURE_READER_FILE)?.let {
            if (it.exists()) {
                System.load(it.absolutePath)
            } else {
                log.error { "找不到$it" }
            }
        } ?: let {
            log.error { "找不到${LIB_CAPTURE_READER_FILE}" }
        }
    }

    external fun nativeInit(): Long
    external fun nativeTryRead(handle: Long, buffer: ByteBuffer): Boolean
    external fun nativeWaitAndRead(handle: Long, buffer: ByteBuffer, timeoutMs: Int): Boolean
    external fun nativeWaitForNewFrame(handle: Long, buffer: ByteBuffer, timeoutMs: Int): Boolean
    external fun nativeGetFrameInfo(handle: Long): IntArray?
    external fun nativeGetStats(handle: Long): IntArray?
    external fun nativeDestroy(handle: Long)

}