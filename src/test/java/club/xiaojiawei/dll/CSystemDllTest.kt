package club.xiaojiawei.dll

import club.xiaojiawei.hsscript.dll.CSystemDll
import com.sun.jna.WString
import org.junit.jupiter.api.Disabled
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @author 肖嘉威
 * @date 2025/4/20 16:11
 */
class CSystemDllTest {

    @Test
    fun testIsDebug() {
        assertFalse("dll不能以debug打包") { CSystemDll.INSTANCE.isDebug() }
    }

    @Test
    fun testUninstall() {
        CSystemDll.INSTANCE.uninstall()
    }

    private val protectionDir by lazy {
        val path = WString(Paths.get(System.getProperty("user.home"), "Downloads", "test_protect").toString())
        File(path.toString()).mkdirs()
        path
    }

    @Test
    @Disabled
    fun testStrongProtection() {
        assertTrue { CSystemDll.INSTANCE.protectDirectory(protectionDir, true) }
    }

    @Test
    @Disabled
    fun testNormalProtection() {
        assertTrue {
            CSystemDll.INSTANCE.protectDirectory(
                protectionDir,
                false
            )
        }
    }

    @Test
    @Disabled
    fun testUnprotect() {
        assertTrue {
            CSystemDll.INSTANCE.unprotectDirectory(protectionDir)
        }
    }

}