package club.xiaojiawei.hsscript.listener

import club.xiaojiawei.hsscript.PROGRAM_ARGS
import club.xiaojiawei.hsscript.bean.DownloaderParam
import club.xiaojiawei.hsscript.bean.Release
import club.xiaojiawei.hsscript.bean.ResumeDownloader
import club.xiaojiawei.hsscript.bean.single.repository.GiteeRepository
import club.xiaojiawei.hsscript.consts.*
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.VersionTypeEnum
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.utils.ConfigExUtil
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.bean.LRunnable
import club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.const.BuildInfo
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.ReadOnlyBooleanWrapper
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit


/**
 * 脚本版本监听器，定时查看是否需要更新
 * @author 肖嘉威
 * @date 2023/9/17 21:49
 */
object VersionListener {

    const val VERSION_FILE_FLAG_NAME = "downloaded.flag"

    private var checkVersionTask: ScheduledFuture<*>? = null

    val currentRelease: Release = Release()

    var latestRelease: Release? = null

    /**
     * 能否升级
     */
    private val canUpdateProperty: ReadOnlyBooleanWrapper = ReadOnlyBooleanWrapper(false)

    val canUpdate: Boolean
        get() = canUpdateProperty.get()

    fun canUpdateReadOnlyProperty(): ReadOnlyBooleanProperty = canUpdateProperty.readOnlyProperty

    /**
     * 正在升级中
     */
    private val updatingProperty: ReadOnlyBooleanWrapper = ReadOnlyBooleanWrapper(false)

    val updating: Boolean
        get() = updatingProperty.get()

    fun updatingReadOnlyProperty(): ReadOnlyBooleanProperty = updatingProperty.readOnlyProperty

    /**
     * 正在下载中
     */
    private val downloadingProperty: ReadOnlyBooleanWrapper = ReadOnlyBooleanWrapper(false)

    val downloading: Boolean
        get() = downloadingProperty.get()

    fun downloadingReadOnlyProperty(): ReadOnlyBooleanProperty = downloadingProperty.readOnlyProperty

    private var updated = false

    init {
        currentRelease.tagName = BuildInfo.VERSION
        currentRelease.isPreRelease = VersionTypeEnum.getEnum(currentRelease).isPreview
        WorkTimeListener.addChangeListener { _, _, newValue ->
            if (!newValue) {
                if (!updated && ConfigUtil.getBoolean(ConfigEnum.AUTO_UPDATE) && VersionListener.canUpdate) {
                    updated = true
                    asyncDownloadLatestRelease(false, DownloaderParam()) { path ->
                        path?.let {
                            execUpdate(path)
                        } ?: let {
                            updated = false
                        }
                    }
                }
            }
        }
    }

    val launch: Unit by lazy {
        if (checkVersionTask != null) return@lazy

        checkVersionTask = EXTRA_THREAD_POOL.scheduleWithFixedDelay(LRunnable {
            checkVersion()
        }, 500, 1000 * 60 * 60 * 2, TimeUnit.MILLISECONDS)
        log.info { "版本更新检测已启动" }
    }

    /**
     * 下载最新版本
     */
    fun asyncDownloadLatestRelease(
        force: Boolean,
        downloaderParam: DownloaderParam,
        callback: ((filePath: String?) -> Unit)? = null
    ) {
        latestRelease?.let {
            asyncDownloadRelease(it, force, downloaderParam, callback)
        } ?: let {
            callback?.invoke(null)
        }
    }

    /**
     * 更新版本
     */
    @Suppress("DEPRECATION")
    fun execUpdate(versionFilePath: String) {
        if (updatingProperty.get()) return

        synchronized(updatingProperty) {
            log.info { "开始更新软件【${versionFilePath}】" }
            try {
                if (updatingProperty.get()) return
                updatingProperty.set(true)

                val updateProgramPath = SystemUtil.getExeFilePath(UPDATE_FILE)
                Runtime
                    .getRuntime()
                    .exec(
                        "$updateProgramPath ${ARG_TARGET}'${ROOT_PATH}' ${ARG_PAUSE}'${PauseStatus.isPause}' ${ARG_PID}'${
                            ProcessHandle.current().pid()
                        }' ${ARG_VERSION_FILE}'${versionFilePath}'"
                    )
            } catch (e: RuntimeException) {
                log.error(e) { "执行版本更新失败" }
            } finally {
                updatingProperty.set(false)
            }
        }
    }

    /**
     * 下载指定版本
     */
    fun asyncDownloadRelease(
        release: Release,
        force: Boolean,
        downloaderParam: DownloaderParam,
        callback: ((filePath: String?) -> Unit)? = null
    ) {
        if (downloadingProperty.get()) return

        synchronized(downloadingProperty) {
            if (downloadingProperty.get()) return
            downloadingProperty.set(true)

            EXTRA_THREAD_POOL.submit {
                var filePath: String? = null
                try {
                    val versionFile: File = Path.of(ROOT_PATH, release.fileName()).toFile()
                    if (!force && versionFile.exists()) {
                        filePath = versionFile.absolutePath
                    } else {
                        val repositoryList = ConfigExUtil.getUpdateSourceList()
                        for (repository in repositoryList) {
                            filePath =
                                downloadRelease(release, repository.getReleaseDownloadURL(release), downloaderParam)
                            if (filePath == null) {
                                log.info { "更换下载源重新下载" }
                            } else {
                                break
                            }
                        }
                    }
                } finally {
                    downloadingProperty.set(false)
                    callback?.invoke(filePath)
                }
            }
        }

    }

    /**
     * 检查最新版本
     */
    fun checkVersion() {
//        以IDEA启动不检查更新
        if (Objects.requireNonNull(javaClass.getResource(""))
                .protocol != "jar" && !PROGRAM_ARGS.contains("--update")
        ) {
            return
        }
        synchronized(canUpdateProperty) {
            val updateDev = ConfigUtil.getBoolean(ConfigEnum.UPDATE_DEV)
            val repositoryList = ConfigExUtil.getUpdateSourceList()
            val updateSource = (if (repositoryList.isEmpty()) GiteeRepository else repositoryList.first())
            val repositoryStr = updateSource::class.java.simpleName.replace(
                "Repository",
                ""
            )
            log.info {
                "开始检查更新，更新源：${repositoryStr}, 更新开发版：$updateDev"
            }
            for (repository in repositoryList) {
                try {
                    latestRelease = repository.getLatestRelease(updateDev)
                } catch (e: Exception) {
                    latestRelease = null
                    log.error(e) { "${repository.getDomain()}检查最新版异常" }
                    continue
                }
                break
            }
            latestRelease?.let {
                if (currentRelease < it && VersionTypeEnum.getEnum(it) !== VersionTypeEnum.TEST) {
                    canUpdateProperty.set(true)
                    log.info { "有更新可用😊，当前版本：【${currentRelease.tagName}】, 最新版本：【${it.tagName}】" }
                    SystemUtil.notice(
                        String.format("更新日志：\n%s", it.body),
                        String.format("发现新版本：%s", it.tagName),
                        "查看详情",
                        repositoryList.first().getReleasePageURL(it)
                    )
                } else {
                    log.info { "已是最新，当前版本：【${currentRelease.tagName}】, 最新版本：【${it.tagName}】" }
                    canUpdateProperty.set(false)
                }
            } ?: {
                log.warn { "没有任何最新版本" }
                canUpdateProperty.set(false)
            }
        }
    }

    private fun downloadRelease(release: Release, url: String, downloaderParam: DownloaderParam): String? {
        val fileName = release.fileName()
        val outPath = Path.of(ROOT_PATH, fileName)
        val downloader = ResumeDownloader(url, outPath.toString())
        try {
            val startContent = "开始下载<$fileName>"
            log.info { startContent }
            downloader.download(downloaderParam)
        } catch (e: Exception) {
            val errorContent = "<$fileName>下载失败"
            log.error(e) { "$errorContent,$url" }
            return null
        }
        val endContent = "<$fileName>下载完毕"
        log.info { endContent }
        return outPath.toString()
    }

}
