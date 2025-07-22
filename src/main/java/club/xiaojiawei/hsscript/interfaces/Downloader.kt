package club.xiaojiawei.hsscript.interfaces

import club.xiaojiawei.hsscript.bean.DownloaderParam

/**
 * @author 肖嘉威
 * @date 2025/7/22 10:18
 */
interface Downloader {

    fun download(
        downloaderParam: DownloaderParam
    )

    fun pause()
}