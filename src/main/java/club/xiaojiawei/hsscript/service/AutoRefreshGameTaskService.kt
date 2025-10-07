package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.bean.DownloaderParam
import club.xiaojiawei.hsscript.bean.ResumeDownloader
import club.xiaojiawei.hsscript.consts.TESS_DATA_PATH
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.config.log
import java.io.File
import kotlin.io.path.Path

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:08
 */
object AutoRefreshGameTaskService : Service<Boolean>() {

    override fun execStart(): Boolean {
        val tessDir = File(TESS_DATA_PATH)
        if (!tessDir.exists() || tessDir.listFiles().isNullOrEmpty()) {
            runUI {
                WindowUtil.createAlert("自动刷新游戏任务需要下载tess数据集", "是否下载", {
                    File(TESS_DATA_PATH).mkdirs()
                    go {
                        runCatching {
                            val dataFileNames = listOf("chi_sim.traineddata", "chi_sim_vert.traineddata")
                            for (dataFileName in dataFileNames) {
                                SystemUtil.notice("开始下载tess数据集[${dataFileName}]")
                                log.info { "开始下载tess数据集[${dataFileName}]" }
                                val downloader = ResumeDownloader(
                                    "https://raw.githubusercontent.com/xjw580/hs-script-app/master/src/main/resources/resources/tessdata/${dataFileName}",
                                    Path(TESS_DATA_PATH, dataFileName).toString()
                                )
                                downloader.download(DownloaderParam())
                                log.info { "tess数据集[${dataFileName}]下载完成" }
                            }
                        }.onFailure {
                            log.error(it) { "" }
                            SystemUtil.notice(it.message ?: "", "tess数据集下载异常")
                        }.onSuccess {
                            SystemUtil.notice("", "tess数据集下载完成")
                        }
                    }
                }, {}, null, "是", "否").run {
                    isAlwaysOnTop = true
                    show()
                }
            }
        }
        return true
    }

    override fun execStop(): Boolean {
        return true
    }

    override fun getStatus(value: Boolean?): Boolean {
        return value ?: ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)
    }
}
