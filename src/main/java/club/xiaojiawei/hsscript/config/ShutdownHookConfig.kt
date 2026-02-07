package club.xiaojiawei.hsscript.config

import club.xiaojiawei.hsscript.consts.PROTECT_PATH
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscriptbase.bean.LThread
import club.xiaojiawei.hsscriptbase.config.log

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
                        CSystemDll.INSTANCE.unprotectDirectory(PROTECT_PATH)
                        CSystemDll.INSTANCE.removeSystemTray()
//                        CSystemDll.INSTANCE.uninstall()
                        CSystemDll.INSTANCE.capture(false)
                        CSystemDll.INSTANCE.limitMouseRange(false)
                        CSystemDll.INSTANCE.mouseHook(false)
                        CSystemDll.INSTANCE.acHook(false)
                        log.info { "软件已关闭" }
                    },
                    "ShutdownHook Thread",
                ),
            )
    }
}