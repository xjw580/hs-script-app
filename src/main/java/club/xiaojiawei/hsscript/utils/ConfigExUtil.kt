package club.xiaojiawei.hsscript.utils

import ch.qos.logback.classic.Level
import club.xiaojiawei.hsscript.bean.GameTask
import club.xiaojiawei.hsscript.bean.HotKey
import club.xiaojiawei.hsscript.bean.WindowConfig
import club.xiaojiawei.hsscript.bean.WorkTimeRuleSet
import club.xiaojiawei.hsscript.bean.single.repository.AbstractRepository
import club.xiaojiawei.hsscript.bean.single.repository.CustomRepository
import club.xiaojiawei.hsscript.bean.single.repository.GiteeRepository
import club.xiaojiawei.hsscript.bean.single.repository.GithubRepository
import club.xiaojiawei.hsscript.consts.GAME_PROGRAM_NAME
import club.xiaojiawei.hsscript.consts.PLATFORM_PROGRAM_NAME
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.GameStartupModeEnum
import club.xiaojiawei.hsscript.enums.MouseControlModeEnum
import club.xiaojiawei.hsscript.enums.SoftProtectedModeEnum
import club.xiaojiawei.hsscript.initializer.DriverInitializer
import club.xiaojiawei.hsscript.starter.InjectStarter
import club.xiaojiawei.hsscript.starter.InjectedAfterStarter
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.status.ScriptStatus
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * @author 肖嘉威 xjw580@qq.com
 * @date 2024/10/2 13:19
 */
object ConfigExUtil {

    private val objectMapper: ObjectMapper by lazy {
        jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
        }
    }

    fun storeGamePath(gameInstallPath: String?): Boolean {
        gameInstallPath ?: return false

        if (Path.of(gameInstallPath, GAME_PROGRAM_NAME).toFile().exists()) {
            ConfigUtil.putString(ConfigEnum.GAME_PATH, gameInstallPath)
            return true
        }
        return false
    }

    fun storePlatformPath(platformInstallPath: String?): Boolean {
        platformInstallPath ?: return false
        val programAbsolutePath =
            if (platformInstallPath.endsWith(".exe")) {
                platformInstallPath
            } else {
                platformInstallPath + File.separator + PLATFORM_PROGRAM_NAME
            }
        if (File(programAbsolutePath).exists()) {
            ConfigUtil.putString(ConfigEnum.PLATFORM_PATH, programAbsolutePath)
            return true
        }
        return false
    }

    fun getExitHotKey(): HotKey? = ConfigUtil.getObject(ConfigEnum.EXIT_HOT_KEY, HotKey::class.java)

    fun storeExitHotKey(hotKey: HotKey) {
        ConfigUtil.putObject(ConfigEnum.EXIT_HOT_KEY, hotKey)
    }

    fun getPauseHotKey(): HotKey? = ConfigUtil.getObject(ConfigEnum.PAUSE_HOT_KEY, HotKey::class.java)

    fun storePauseHotKey(hotKey: HotKey) {
        ConfigUtil.putObject(ConfigEnum.PAUSE_HOT_KEY, hotKey)
    }

    fun getDeckPluginDisabled(): MutableList<String> =
        ConfigUtil.getArray(ConfigEnum.DECK_PLUGIN_DISABLED, String::class.java) ?: mutableListOf()

    fun storeDeckPluginDisabled(disabledList: List<String>) {
        ConfigUtil.putArray(ConfigEnum.DECK_PLUGIN_DISABLED, disabledList)
    }

    fun getCardPluginDisabled(): MutableList<String> =
        ConfigUtil.getArray(ConfigEnum.CARD_PLUGIN_DISABLED, String::class.java) ?: mutableListOf()

    fun storeCardPluginDisabled(disabledList: List<String>) {
        ConfigUtil.putArray(ConfigEnum.CARD_PLUGIN_DISABLED, disabledList)
    }

    fun getFileLogLevel(): Level = Level.toLevel(ConfigUtil.getString(ConfigEnum.FILE_LOG_LEVEL))

    fun storeFileLogLevel(level: String) {
        ConfigUtil.putString(ConfigEnum.FILE_LOG_LEVEL, level)
        ScriptStatus.fileLogLevel = getFileLogLevel().toInt()
    }

    fun storeMouseControlMode(mouseControlModeEnum: MouseControlModeEnum): Boolean {
        val oldMouseControlMode = getMouseControlMode()
        ConfigUtil.putString(ConfigEnum.MOUSE_CONTROL_MODE, mouseControlModeEnum.name)
        when (mouseControlModeEnum) {
            MouseControlModeEnum.MESSAGE -> {
                if (oldMouseControlMode === MouseControlModeEnum.DRIVE) {
                    DriverInitializer().uninstall()
                }
                InjectStarter().start()
                InjectedAfterStarter().start()
            }

            MouseControlModeEnum.EVENT -> {
                if (oldMouseControlMode === MouseControlModeEnum.DRIVE) {
                    DriverInitializer().uninstall()
                }
                CSystemDll.INSTANCE.mouseHook(false)
            }

            MouseControlModeEnum.DRIVE -> {
                DriverInitializer().install()
                CSystemDll.INSTANCE.mouseHook(false)
            }
        }
        return true
    }

    fun getMouseControlMode(): MouseControlModeEnum =
        MouseControlModeEnum.fromString(ConfigUtil.getString(ConfigEnum.MOUSE_CONTROL_MODE))

    fun storeTopGameWindow(enabled: Boolean) {
        ConfigUtil.putBoolean(ConfigEnum.TOP_GAME_WINDOW, enabled)
        if (enabled) {
            if (!PauseStatus.isPause) {
                CSystemDll.INSTANCE.topWindow(ScriptStatus.gameHWND, true)
            }
        } else {
            CSystemDll.INSTANCE.topWindow(ScriptStatus.gameHWND, false)
        }
    }

    fun getUpdateSourceList(): List<AbstractRepository> {
        val updateSource = ConfigUtil.getString(ConfigEnum.UPDATE_SOURCE).lowercase()
        if (updateSource.isBlank()) {
            return listOf(GiteeRepository, GithubRepository)
        }

        // 检查是否配置了自定义服务器
        if (updateSource.startsWith("custom") || updateSource.contains("自定义")) {
            // 配置自定义服务器（可以从配置文件读取）
            val customDomain = ConfigUtil.getString(ConfigEnum.CUSTOM_UPDATE_SERVER_DOMAIN)
            if (customDomain.isNotBlank()) {
                CustomRepository.configure(
                    domain = customDomain,
                    userName = ConfigUtil.getString(ConfigEnum.CUSTOM_UPDATE_SERVER_USER).ifBlank { "xiaojiawei" }
                )
                return listOf(CustomRepository, GiteeRepository, GithubRepository)
            }
        }

        if (GiteeRepository::class.java.simpleName
                .lowercase()
                .startsWith(updateSource)
        ) {
            return listOf(GiteeRepository, GithubRepository)
        }
        return listOf(GithubRepository, GiteeRepository)
    }

    fun storePreventAntiCheat(status: Boolean) {
        ConfigUtil.putBoolean(ConfigEnum.PREVENT_AC, status, true)
        val gameDir = File(ConfigUtil.getString(ConfigEnum.GAME_PATH))
        if (gameDir.exists()) {
            val acFile = gameDir.resolve(".ac")
            if (acFile.exists()) {
                Files.setAttribute(acFile.toPath(), "dos:hidden", false)
            }
            FileOutputStream(acFile).use {
                it.write(status.toString().toByteArray())
            }
            Files.setAttribute(acFile.toPath(), "dos:hidden", true)
        }
    }

    fun getWorkTimeRuleSet(): MutableList<WorkTimeRuleSet> =
        ConfigUtil.getArray(
            ConfigEnum.WORK_TIME_RULE_SET,
            WorkTimeRuleSet::class.java,
        ) ?: mutableListOf()

    fun storeWorkTimeRuleSet(workTimeRuleSets: List<WorkTimeRuleSet>) {
        ConfigUtil.putString(ConfigEnum.WORK_TIME_RULE_SET, objectMapper.writeValueAsString(workTimeRuleSets))
    }

    /**
     * @return 长度为7的集合，依次记录周一到周日的[WorkTimeRuleSet.id]
     */
    fun getWorkTimeSetting(): MutableList<String> {
        return ConfigUtil.getArray(
            ConfigEnum.WORK_TIME_SETTING,
            String::class.java,
        ) ?: let {
            val res = mutableListOf<String>()
            for (i in 0 until 7) {
                res.add("")
            }
            return res
        }
    }

    /**
     * @param workTimeSetting 长度为7的集合，依次记录周一到周日的[WorkTimeRuleSet.id]
     */
    fun storeWorkTimeSetting(workTimeSetting: List<String>) {
        ConfigUtil.putString(ConfigEnum.WORK_TIME_SETTING, objectMapper.writeValueAsString(workTimeSetting))
    }

    fun getGameStartupMode(): MutableList<GameStartupModeEnum> {
        return ConfigUtil.getArray(
            ConfigEnum.GAME_STARTUP_MODE,
            GameStartupModeEnum::class.java,
        ) ?: mutableListOf(GameStartupModeEnum.MESSAGE, GameStartupModeEnum.CMD)
    }

    fun storeGameStartupMode(gameStartupModeEnums: List<GameStartupModeEnum>) {
        ConfigUtil.putString(ConfigEnum.GAME_STARTUP_MODE, objectMapper.writeValueAsString(gameStartupModeEnums))
    }

    fun getSoftProtectedMode(): SoftProtectedModeEnum {
        return ConfigUtil.getObject(
            ConfigEnum.SOFT_PROTECTED_MODE,
            SoftProtectedModeEnum::class.java,
        ) ?: SoftProtectedModeEnum.NONE
    }

    fun storeSoftProtectedMode(softProtectedModeEnum: SoftProtectedModeEnum) {
        ConfigUtil.putObject(ConfigEnum.SOFT_PROTECTED_MODE, softProtectedModeEnum)
    }

    fun getChooseDeckPos(): MutableList<Int> {
        return ConfigUtil.getArray(
            ConfigEnum.CHOOSE_DECK_POS,
            Int::class.java,
        ) ?: mutableListOf<Int>()
    }

    fun storeChooseDeckPos(chooseDeckPos: List<Int>) {
        ConfigUtil.putArray(ConfigEnum.CHOOSE_DECK_POS, chooseDeckPos)
    }

    fun getGameTask(): GameTask {
        return ConfigUtil.getObject(ConfigEnum.GAME_TASK_STATUS, GameTask::class.java)
            ?: try {
                objectMapper.readValue(ConfigEnum.GAME_TASK_STATUS.defaultValue, GameTask::class.java)
            } catch (e: Exception) {
                GameTask()
            }
    }

    fun storeGameTask(gameTaskStatus: GameTask) {
        ConfigUtil.putString(ConfigEnum.GAME_TASK_STATUS, objectMapper.writeValueAsString(gameTaskStatus))
    }

    fun getWindowConfig(): MutableList<WindowConfig> {
        return ConfigUtil.getArray(ConfigEnum.WINDOW_CONFIG, WindowConfig::class.java)
            ?: mutableListOf()
    }

    fun storeWindowConfig(windowConfigs: List<WindowConfig>) {
        ConfigUtil.putString(ConfigEnum.WINDOW_CONFIG, objectMapper.writeValueAsString(windowConfigs))
    }

}
