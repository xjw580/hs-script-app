package club.xiaojiawei.hsscript.consts

import club.xiaojiawei.hsscript.listener.VersionListener
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.const.BuildInfo
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.math.log

/**
 * @author 肖嘉威
 * @date 2024/10/13 16:45
 */

val ROOT_PATH by lazy { System.getProperty("user.dir") }

val TEMP_VERSION_PATH: String by lazy { Path.of(ROOT_PATH, "new_version_temp").toString() }

val LOG_PATH: String by lazy { Path.of(ROOT_PATH, "log").toString() }

const val AOT_DIR = "aot"

const val AOT_BATCH_NAME = "create-aot.bat"

val AOT_PATH: String by lazy { Path.of(ROOT_PATH, AOT_DIR).toString() }

val AOT_FILE_PATH: String by lazy { Path.of(AOT_PATH, "${PROGRAM_NAME}_${BuildInfo.VERSION}.aot").toString() }

val LIBRARY_PATH: String by lazy { Path.of(ROOT_PATH, "lib").toString() }
val DLL_PATH: String by lazy { Path.of(LIBRARY_PATH, "dll").toString() }

val CONFIG_PATH: String by lazy { Path.of(ROOT_PATH, "config").toString() }

val PLUGIN_PATH: String by lazy { Path.of(ROOT_PATH, "plugin").toString() }

val CARD_WEIGHT_CONFIG_PATH: Path by lazy { Path.of(CONFIG_PATH, "card.weight") }

val CARD_INFO_CONFIG_PATH: Path by lazy { Path.of(CONFIG_PATH, "card.info") }

const val FXML_DIR: String = "/fxml/"

const val GAME_LOG_DIR: String = "Logs"

const val DRIVE_PATH = "C:\\Windows\\System32\\drivers"

val MOUSE_DRIVE_PATH: String by lazy { Path.of(DRIVE_PATH, "mouse.sys").toString() }

val KEYBOARD_DRIVE_PATH: String by lazy { Path.of(DRIVE_PATH, "keyboard.sys").toString() }

val DATA_DIR: Path by lazy { Path.of(ROOT_PATH, "data") }

const val STATISTICS_DB_NAME: String = "statistics.db"

@JvmInline
value class ResourceFile(val name: String)

val INJECT_UTIL_FILE = ResourceFile("inject-util.exe")

val INSTALL_DRIVE_FILE = ResourceFile("install-drive.exe")

val HS_CARD_UTIL_FILE = ResourceFile("card-update-util.exe")

val UPDATE_FILE = ResourceFile("update.exe")

val LIB_HS_FILE = ResourceFile("hs.dll")

val LIB_BN_FILE = ResourceFile("bn.dll")

val LIB_CAPTURE_READER_FILE = ResourceFile("capture_reader.dll")

val LIB_LOG_READER_FILE = ResourceFile("log_reader.dll")

const val GAME_WAR_LOG_NAME = "Power.log"

const val GAME_MODE_LOG_NAME = "LoadingScreen.log"

const val GAME_DECKS_LOG_NAME = "Decks.log"

const val COMMON_CSS_PATH = "/fxml/css/common.css"

private fun getPath(relativePath: String): String {
    val jarDir = SystemUtil.getCurrentJarFile()

    var path = Path.of(
        jarDir.path, relativePath
    )
    if (!path.exists()) {
        path = Path.of(jarDir.parentFile.path, relativePath)
    }
    return path.toString()
}

/**
 * 图片路径
 */
val IMG_PATH by lazy {
    getPath("resources/img")
}

/**
 * tess数据集路径
 */
val TESS_DATA_PATH by lazy {
    getPath("resources/tessdata")
}

/**
 * 脚本程序图标名字
 */
const val MAIN_IMG_NAME: String = "favicon.png"

const val TRAY_IMG_NAME: String = "favicon.ico"

const val TRAY_SETTINGS_IMG_NAME: String = "settings.ico"

const val TRAY_STATISTICS_IMG_NAME: String = "statistics.ico"
const val TRAY_START_IMG_NAME: String = "start.ico"
const val TRAY_PAUSE_IMG_NAME: String = "pause.ico"
const val TRAY_EXIT_IMG_NAME: String = "exit.ico"