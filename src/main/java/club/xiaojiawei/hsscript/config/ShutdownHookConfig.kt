package club.xiaojiawei.hsscript.config

import club.xiaojiawei.hsscript.consts.PROTECT_PATH
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.WindowUtil
import club.xiaojiawei.hsscript.utils.getBoolean
import club.xiaojiawei.hsscriptbase.bean.LThread
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.util.isTrue

/**
 * @author 肖嘉威
 * @date 2026/2/7 12:02
 */
object ShutdownHookConfig {
    val init by lazy {
        Runtime
            .getRuntime()
            .addShutdownHook(
                LThread(
                    {
                        ConfigEnum.ONLY_RUNTIME_PROTECT.getBoolean().isTrue {
                            CSystemDll.INSTANCE.unprotectDirectory(PROTECT_PATH)
                        }
                        CSystemDll.INSTANCE.removeSystemTray()
                        WindowUtil.saveConfig()
//                        CSystemDll.INSTANCE.uninstall()
                        CSystemDll.INSTANCE.capture(false)
                        CSystemDll.INSTANCE.limitMouseRange(false)
                        CSystemDll.INSTANCE.showMouseTrack(false)
                        CSystemDll.INSTANCE.mouseHook(false)
                        CSystemDll.INSTANCE.acHook(false)
                        log.info { "软件已关闭" }
                    },
                    "ShutdownHook Thread",
                ),
            )
    }
}