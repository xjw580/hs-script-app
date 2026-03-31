package club.xiaojiawei.hsscript.starter

import club.xiaojiawei.hsscript.consts.INJECT_UTIL_FILE
import club.xiaojiawei.hsscript.consts.LIB_BN_FILE
import club.xiaojiawei.hsscript.consts.PLATFORM_CN_NAME
import club.xiaojiawei.hsscript.consts.PLATFORM_PROGRAM_NAME
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.utils.InjectUtil
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscript.utils.getBoolean
import club.xiaojiawei.hsscriptbase.config.log
import com.sun.jna.platform.win32.User32

/**
 * @author 肖嘉威
 * @date 2026/3/31 11:02
 */
class InjectPlatformStarter : AbstractStarter() {
    override fun execStart() {
        if (ConfigEnum.ALLOW_PLATFORM_INJECT.getBoolean()) {
            if (User32.INSTANCE.IsWindow(ScriptStatus.platformHWND)) {
                val injectFile = SystemUtil.getExeFilePath(INJECT_UTIL_FILE) ?: return
                val dllFile = SystemUtil.getDllFilePath(LIB_BN_FILE) ?: return
                InjectUtil.execInject(injectFile, dllFile, PLATFORM_PROGRAM_NAME)
            }
        } else {
            val text = "已禁用${PLATFORM_CN_NAME}注入，如有需要请到开发者选项中打开"
            SystemUtil.notice(text)
            log.warn { text }
        }
    }
}