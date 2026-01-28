package club.xiaojiawei.dll

import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.starter.AbstractStarter
import club.xiaojiawei.hsscript.starter.GameStarter
import club.xiaojiawei.hsscript.starter.InjectStarter
import club.xiaojiawei.hsscript.starter.PlatformStarter
import org.junit.jupiter.api.Disabled
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

/**
 * @author 肖嘉威
 * @date 2025/9/11 9:14
 */
@Disabled
class DeveloperTest {

    @Test
    fun testDeveloper() {
        val starter = PlatformStarter()
        val countDownLatch = CountDownLatch(1)
        starter.setNextStarter(GameStarter()).setNextStarter(InjectStarter().apply {
            setNextStarter(object : AbstractStarter() {
                override fun execStart() {
                    CSystemDll.INSTANCE.logHook(true)
                    CSystemDll.INSTANCE.developer(true)
                    Thread.sleep(1000)
                    countDownLatch.countDown()
                }
            })
        })
        starter.start()
        countDownLatch.await()
    }

}