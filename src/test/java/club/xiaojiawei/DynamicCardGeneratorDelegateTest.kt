package club.xiaojiawei

import club.xiaojiawei.hsscript.bean.DynamicCardGenerator
import club.xiaojiawei.hsscriptcardsdk.bean.PlayAction
import club.xiaojiawei.hsscriptcardsdk.bean.Player
import club.xiaojiawei.hsscriptcardsdk.bean.War
import net.bytebuddy.implementation.bind.annotation.Argument
import org.junit.jupiter.api.Assertions.assertArrayEquals
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicCardGeneratorDelegateTest {

    class MyDelegate {
        fun generatePlayActions(@Argument(0) war: War, @Argument(1) player: Player): List<PlayAction> {
            return emptyList()
        }
    }

    @Test
    fun testGenerateCardActionWithDelegate() {
        val className = "club.xiaojiawei.hsscript.bean.GeneratedDelegateTestCard"
        val cardIds = arrayOf("TEST_DEL_001", "TEST_DEL_002")
        val delegate = MyDelegate()

        val generatedClass = DynamicCardGenerator.generateCardActionClass(className, cardIds, delegate)
        assertEquals(className, generatedClass.name)

        val instance = generatedClass.getDeclaredConstructor().newInstance()
        assertArrayEquals(cardIds, instance.getCardId())

        val newInstance = instance.createNewInstance()
        assertEquals(className, newInstance.javaClass.name)
    }
}