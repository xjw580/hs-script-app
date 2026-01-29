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
        return digest.digest().toHexString()
    }

    fun File.calcSHA256(): String = sha256(this)
}