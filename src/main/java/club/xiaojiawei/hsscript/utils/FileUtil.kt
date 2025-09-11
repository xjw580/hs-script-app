package club.xiaojiawei.hsscript.utils

import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.StandardOpenOption

/**
 * @author 肖嘉威
 * @date 2024/10/11 11:37
 */
object FileUtil {

    /**
     * 清空指定目录（不删除该目录）
     */
    fun clearDirectory(directory: File?) {
        if (directory == null || !directory.exists() || !directory.isDirectory) return
        directory.listFiles()?.let {
            for (file in it) {
                deleteFile(file)
            }
        }
    }

    /**
     * 删除文件或目录
     */
    fun deleteFile(file: File?) {
        file ?: return
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                deleteFile(it)
            }
        }
        Files.deleteIfExists(file.toPath())
    }

    fun createDirectory(directory: File?): Boolean {
        directory ?: return false
        if (directory.exists()) {
            if (directory.isFile) {
                deleteFile(directory)
                return directory.mkdirs()
            } else {
                clearDirectory(directory)
                return true
            }
        } else {
            return directory.mkdirs()
        }
    }

    /**
     * 检查文件是否可以写入（不被其他进程占用）
     * @param filePath 文件路径
     * @return true 如果文件可写且未被占用，false 否则
     */
    fun isFileWritable(filePath: String): Boolean {
        val file = File(filePath)

        // 1. 检查基本的文件系统权限
        if (!file.exists()) {
            return try {
                file.parentFile?.mkdirs()
                file.createNewFile()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (!file.canWrite()) {
            return false
        }

        // 2. 尝试获取独占锁来检测是否被占用
        return try {
            RandomAccessFile(file, "rw").use { raf ->
                raf.channel.use { channel ->
                    // 尝试获取独占锁
                    channel.tryLock()?.use { lock ->
                        true // 成功获取锁，文件未被占用
                    } ?: false // 无法获取锁，文件被占用
                }
            }
        } catch (e: OverlappingFileLockException) {
            false // 文件已被当前进程锁定
        } catch (e: Exception) {
            false // 其他异常，假设文件不可用
        }
    }

    /**
     * 尝试以写模式打开文件，检测是否被占用
     * 这是一个更直接的方法，但可能影响文件的最后修改时间
     */
    fun canOpenForWrite(filePath: String): Boolean {
        return try {
            RandomAccessFile(filePath, "rw").use {
                true
            }
        } catch (e: FileNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 使用 NIO 方式检测文件锁定状态
     * 这个方法对文件的影响最小
     */
    fun isFileLocked(filePath: String): Boolean {
        val path = File(filePath).toPath()

        if (!Files.exists(path)) {
            return false
        }

        return try {
            Files.newByteChannel(
                path,
                StandardOpenOption.WRITE,
                StandardOpenOption.READ
            ).use { channel ->
                if (channel is FileChannel) {
                    channel.tryLock()?.use {
                        false // 成功获取锁，文件未被锁定
                    } ?: true // 无法获取锁，文件被锁定
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            true // 异常通常表示文件被占用或无法访问
        }
    }

    fun checkFileStatus(filePath: String): FileStatus {
        val file = File(filePath)

        return when {
            !file.exists() -> FileStatus.NOT_EXISTS
            !file.canRead() -> FileStatus.NO_READ_PERMISSION
            !file.canWrite() -> FileStatus.NO_WRITE_PERMISSION
            isFileLocked(filePath) -> FileStatus.LOCKED_BY_OTHER_PROCESS
            else -> FileStatus.AVAILABLE
        }
    }

}

enum class FileStatus {
    AVAILABLE,                  // 文件可用
    NOT_EXISTS,                // 文件不存在
    NO_READ_PERMISSION,        // 没有读权限
    NO_WRITE_PERMISSION,       // 没有写权限
    LOCKED_BY_OTHER_PROCESS    // 被其他进程锁定
}