package club.xiaojiawei.hsscript.utils

import java.io.File
import java.security.MessageDigest

/**
 * @author 肖嘉威
 * @date 2026/1/29 15:18
 */
object MessageDigestUtil {

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return bytesToHex(digest.digest())
    }

    /**
     * 字节数组转十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    fun File.calcSHA256(): String = sha256(this)
}