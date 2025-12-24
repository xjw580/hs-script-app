package club.xiaojiawei.hsscript.bean.single.repository

import club.xiaojiawei.hsscript.bean.Release
import club.xiaojiawei.hsscript.consts.PROJECT_NAME
import club.xiaojiawei.hsscript.utils.NetUtil
import org.springframework.web.client.getForObject

/**
 * 自定义版本服务器
 * 提供与 GitHub 兼容的 API 接口
 *
 * @author 肖嘉威
 * @date 2025/12/23
 */
object CustomRepository : AbstractRepository() {

    // 自定义服务器配置
    private var customDomain: String = "localhost:8080"
    private var customUserName: String = "xiaojiawei"

    /**
     * 配置自定义服务器
     * @param domain 服务器域名，如 "version.example.com" 或 "192.168.1.100:8080"
     * @param userName 用户名，默认 "xiaojiawei"
     */
    fun configure(domain: String, userName: String = "xiaojiawei") {
        customDomain = domain
        customUserName = userName
    }

    override fun getLatestRelease(isPreview: Boolean): Release? {
        var latestRelease: Release? = null
        if (isPreview) {
            // 预发布版：从 /releases 列表获取
            val releases: Array<Release>? = NetUtil.buildRestTemplate().getForObject<Array<Release>>(
                getLatestReleaseURL(true)
            )
            if (!releases.isNullOrEmpty()) {
                latestRelease = releases[0]
            }
        } else {
            // 正式版：从 /releases/latest 获取
            latestRelease = NetUtil.buildRestTemplate().getForObject<Release>(
                getLatestReleaseURL(false)
            )
        }
        return latestRelease
    }

    override fun getLatestReleaseURL(isPreview: Boolean): String {
        return if (isPreview) {
            String.format(
                "http://%s/repos/%s/%s/releases",
                getDomain(),
                getUserName(),
                PROJECT_NAME
            )
        } else {
            String.format(
                "http://%s/repos/%s/%s/releases/latest",
                getDomain(),
                getUserName(),
                PROJECT_NAME
            )
        }
    }

    override fun getReleaseDownloadURL(release: Release): String {
        return String.format(
            "http://%s/%s/%s/releases/download/%s/%s",
            getDomain(),
            getUserName(),
            PROJECT_NAME,
            release.tagName,
            getFileName(release)
        )
    }

    override fun getReleasePageURL(release: Release): String {
        // 自定义服务器没有网页界面，返回下载链接
        return getReleaseDownloadURL(release)
    }

    override fun getDomain(): String {
        return customDomain
    }

    override fun getUserName(): String {
        return customUserName
    }
}
