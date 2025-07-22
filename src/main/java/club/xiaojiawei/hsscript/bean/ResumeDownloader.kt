package club.xiaojiawei.hsscript.bean

/**
 * 支持断点续传的下载器
 * @author 肖嘉威
 * @date 2025/6/5 11:28
 */
import club.xiaojiawei.hsscript.interfaces.Downloader
import club.xiaojiawei.hsscript.utils.NetUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI

class ResumeDownloader(
    val urlStr: String,
    val outputPath: String,
) : Downloader {

    var progress: Double = 0.0

    var totalSize: Long = 0L

    var downloadedBytes: Long = 0L

    private var downloadThread: Thread? = null

    override fun download(
        downloaderParam: DownloaderParam,
    ) {
        val tempPath = "$outputPath.part"
        val outputFile = File(outputPath)
        val tempFile = File(tempPath)

        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        val conn = NetUtil.buildConnection(urlStr) as HttpURLConnection
        conn.requestMethod = "GET"

        // ✅ 设置请求头必须在 connect() 之前
        if (downloadedBytes > 0) {
            conn.setRequestProperty("Range", "bytes=$downloadedBytes-")
        }

        // ✅ 建立连接（此时才开始访问服务端响应）
        conn.connect()

        // ✅ 响应码检查
        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_PARTIAL && responseCode != HttpURLConnection.HTTP_OK) {
            error("服务器不支持断点续传或链接失效，返回码: $responseCode，url: $urlStr")
        }

        // ✅ 获取总大小（必须在 connect() 之后）
        this.totalSize = when {
            conn.getHeaderField("Content-Range") != null -> {
                val contentRange = conn.getHeaderField("Content-Range")
                contentRange.substringAfterLast("/").toLong()
            }

            conn.getHeaderField("Content-Length") != null -> {
                conn.getHeaderField("Content-Length").toLong() + downloadedBytes
            }

            else -> -1L
        }

        // ✅ 开始下载
        conn.inputStream.use { inputStream ->
            BufferedOutputStream(FileOutputStream(tempFile, true)).use { outputStream ->
                val buffer = ByteArray(8192)
                var total = downloadedBytes
                var bytesRead: Int
                downloadThread = Thread.currentThread()
                while (true) {
                    if (Thread.interrupted()) {
                        error("暂停下载[${urlStr}]")
                    }
                    if ((inputStream.read(buffer).also { bytesRead = it } == -1)) {
                        break
                    } else {
                        outputStream.write(buffer, 0, bytesRead)
                        total += bytesRead
                        this.downloadedBytes = total
                        val currentProgress = if (totalSize > 0) {
                            (total * 100.0) / totalSize
                        } else {
                            0.0
                        }
                        if (currentProgress >= 100.0 || currentProgress - this.progress > downloaderParam.callbackProgressStep) {
                            downloaderParam.downloadingCallback?.invoke(
                                currentProgress,
                                this.downloadedBytes,
                                this.totalSize
                            )
                            this.progress = currentProgress
                        }
                    }
                }
                downloadThread = null
            }
        }
        // ✅ 重命名文件
        if (!tempFile.renameTo(outputFile)) {
            error("下载完成但重命名[${outputPath}]失败")
        }
    }

    override fun pause() {
        downloadThread?.interrupt()
    }
}

//fun main() {
//    val url = "https://gitee.com/zergqueen/Hearthstone-Script/releases/download/v4.8.0-GA/hs-script_v4.8.0-GA.zip"
////    val url = "https://github.com/xjw580/Hearthstone-Script/releases/download/v4.8.0-GA/hs-script_v4.8.0-GA.zip"
//    val output = "hs-script_v4.8.0-GA.zip"
//    val resumeDownloader = ResumeDownloader(url, output)
//    val thread = Thread {
//        println("download")
//        resumeDownloader.download { progress: Double, totalSize: Long, downloadedBytes: Long ->
//            println("progress: $progress, totalSize: $totalSize, downloadedBytes: $downloadedBytes")
//        }
//    }
//    thread.start()
//    Thread.sleep(3000)
//    resumeDownloader.pause()
//    println("pause")
//}
