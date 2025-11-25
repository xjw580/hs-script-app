package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscript.MainApplication
import club.xiaojiawei.hsscript.consts.AOT_BATCH_NAME
import club.xiaojiawei.hsscript.consts.AOT_DIR
import club.xiaojiawei.hsscript.consts.AOT_PATH
import club.xiaojiawei.hsscript.consts.PROGRAM_NAME
import club.xiaojiawei.hsscript.enums.WindowEnum
import club.xiaojiawei.hsscriptbase.const.BuildInfo
import javafx.stage.Stage
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * @author 肖嘉威
 * @date 2025/11/25 9:29
 */
object AOTUtil {

    fun buildAOTAlert(successCallback: () -> Unit = {}): Result<Stage> {
        val aotBatch = Path(AOT_BATCH_NAME)
        return if (aotBatch.exists()) {
            Result.success(
                WindowUtil.createAlert(
                    "AOT缓存检测",
                    "是否创建AOT缓存，用于提高软件启动速度",
                    {
                        File(AOT_PATH).run {
                            FileUtil.deleteFile(this)
                            mkdirs()
                        }
                        val startCMD =
                            "$aotBatch \"${SystemUtil.getCurrentJarFile().name}\" \"${AOT_DIR}\\${PROGRAM_NAME}_${BuildInfo.VERSION}\" \"${MainApplication::class.java.packageName}.MainKt\""
                        CMDUtil.directExec(
                            arrayOf(
                                "cmd", "/c", "start", "\"AOTWindow\"", "cmd.exe", "/k", startCMD
                            )
                        ).waitFor()
                        successCallback()
                    },
                    {},
                    WindowEnum.MAIN,
                    "是",
                    "否"
                )
            )
        } else {
            Result.failure(Exception("$aotBatch 不存在"))
        }
    }
}