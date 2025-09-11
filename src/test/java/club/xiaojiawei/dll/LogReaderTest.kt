package club.xiaojiawei.dll

import club.xiaojiawei.hsscript.bean.MemoryLogFile
import club.xiaojiawei.hsscript.consts.GAME_MODE_LOG_NAME
import club.xiaojiawei.hsscript.consts.GAME_WAR_LOG_NAME
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.dll.LogReader
import club.xiaojiawei.hsscript.starter.AbstractStarter
import club.xiaojiawei.hsscript.starter.GameStarter
import club.xiaojiawei.hsscript.starter.InjectStarter
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.CountDownLatch
import kotlin.test.Test

/**
 * @author 肖嘉威
 * @date 2025/9/11 9:14
 */
class LogReaderTest {

    @BeforeEach
    fun setup() {
        val starter = GameStarter()
        val countDownLatch = CountDownLatch(1)
        starter.setNextStarter(InjectStarter().apply {
            setNextStarter(object : AbstractStarter() {
                override fun execStart() {
                    CSystemDll.INSTANCE.logHook(true)
                    Thread.sleep(1000)
                    assert(LogReader.nativeInit())
                    countDownLatch.countDown()
                }
            })
        })
        starter.start()
        countDownLatch.await()
    }

    private val waitTime = 20000L

    @Test
    fun testReadPowerLog() {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < waitTime) {
            if (LogReader.existChannel(GAME_WAR_LOG_NAME)){
                break
            }
            Thread.sleep(200)
        }
        println("wait end")
        val memoryLogFile = MemoryLogFile(GAME_WAR_LOG_NAME)
        memoryLogFile.readAll().forEach { println(it) }
        while (true) {
            memoryLogFile.readLine()?.let {
                println(it)
            } ?: let {
                Thread.sleep(100)
            }
        }
    }

    @Test
    fun testReadLoadingScreenLog() {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < waitTime) {
            if (LogReader.existChannel(GAME_MODE_LOG_NAME)){
                break
            }
            Thread.sleep(200)
        }
        println("wait end")
        val memoryLogFile = MemoryLogFile(GAME_MODE_LOG_NAME)
        memoryLogFile.readAll().forEach { println(it) }
        while (true) {
            memoryLogFile.readLine()?.let {
                println(it)
            } ?: let {
                Thread.sleep(100)
            }
        }
    }

}