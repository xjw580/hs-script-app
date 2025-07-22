package club.xiaojiawei.hsscript.bean

/**
 * @author 肖嘉威
 * @date 2025/7/22 10:22
 */
data class DownloaderParam(
    val callbackProgressStep: Double = 0.1,
    /**
     * progress: [0, 100]
     */
    val downloadingCallback: ((progress: Double, totalSize: Long, downloadedBytes: Long) -> Unit)? = null
)