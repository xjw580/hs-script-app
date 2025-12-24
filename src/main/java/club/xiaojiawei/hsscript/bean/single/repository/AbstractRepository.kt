package club.xiaojiawei.hsscript.bean.single.repository

import club.xiaojiawei.hsscript.bean.Release
import club.xiaojiawei.hsscript.consts.PROGRAM_NAME
import club.xiaojiawei.hsscript.consts.PROJECT_NAME
import club.xiaojiawei.hsscriptbase.const.BuildInfo
import club.xiaojiawei.hsscriptbase.const.SoftRunMode

/**
 * @author 肖嘉威
 * @date 2024/5/23 19:18
 */
abstract class AbstractRepository {

    fun getReleaseDownloadURL(isPreview: Boolean = false): String? {
        val latestRelease = getLatestRelease(isPreview)
        return latestRelease?.let {
            getReleaseDownloadURL(it)
        }
    }

    open fun getReleaseDownloadURL(release: Release): String {
        return String.format(
            "https://%s/%s/%s/releases/download/%s/%s",
            getDomain(),
            getUserName(),
            PROJECT_NAME,
            release.tagName,
            getFileName(release)
        )
    }

    fun getFileName(release: Release): String {
        return "%s%s_%s.zip".format(
            PROGRAM_NAME,
            if (BuildInfo.SOFT_RUN_MODE === SoftRunMode.NATIVE) "-native" else "",
            release.tagName
        )
    }

    open fun getReleasePageURL(release: Release): String {
        return String.format(
            "https://%s/%s/%s/releases/tag/%s",
            GiteeRepository.getDomain(),
            GiteeRepository.getUserName(),
            PROJECT_NAME,
            release.tagName
        )
    }

    abstract fun getLatestRelease(isPreview: Boolean = false): Release?

    abstract fun getLatestReleaseURL(isPreview: Boolean = false): String

    abstract fun getDomain(): String?

    abstract fun getUserName(): String?
}
