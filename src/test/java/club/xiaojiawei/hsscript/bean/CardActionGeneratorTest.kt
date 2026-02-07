package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscriptcardsdk.cardparser.CardActionGenerator
import club.xiaojiawei.hsscriptcardsdk.cardparser.MinionPointRivalPlayActionInterceptor
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CardActionGeneratorTest {

    @Test
    fun testGenerateCardActionWithAdvice() {
        val className = "club.xiaojiawei.hsscript.bean.GeneratedAdviceTestCard"
        val cardIds = arrayOf("TEST_ADV_001", "TEST_ADV_002")

        val generatedClass = CardActionGenerator.generateCardActionClass(
            className, cardIds,
            MinionPointRivalPlayActionInterceptor(1)
        )
        assertEquals(className, generatedClass.name)

        val instance = generatedClass.getDeclaredConstructor().newInstance()
        assertArrayEquals(cardIds, instance.getCardId())

        val newInstance = instance.createNewInstance()
        assertEquals(className, newInstance.javaClass.name)
    }
}
