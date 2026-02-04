package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscriptcardsdk.CardAction
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers

/**
 * [CardAction]类生成器
 * @author 肖嘉威
 * @date 2026/2/5
 */
object CardActionGenerator {

    val loadedClassMap = mutableMapOf<String, Class<out CardAction>>()

    fun generateCardActionClass(
        className: String,
        cardIds: Array<String>,
        playActionsInterceptor: PlayActionsInterceptor
    ): Class<out CardAction> {
        val loadedClass = loadedClassMap[className]
        if (loadedClass != null) return loadedClass
        lateinit var generatedClass: Class<out CardAction>
        val unloaded = ByteBuddy().subclass(CardAction.DefaultCardAction::class.java).name(className)
            .method(ElementMatchers.named("getCardId"))
            .intercept(FixedValue.value(cardIds))
            .method(ElementMatchers.named("generatePlayActions"))
            .intercept(MethodDelegation.to(playActionsInterceptor))
            .method(ElementMatchers.named("createNewInstance"))
            .intercept(MethodDelegation.to(CreateNewInstanceInterceptor {
                generatedClass.getDeclaredConstructor().newInstance()
            }))
            .make()
        generatedClass = unloaded.load(CardActionGenerator::class.java.classLoader).loaded as Class<out CardAction>
        loadedClassMap[className] = generatedClass
        return generatedClass
    }

    class CreateNewInstanceInterceptor(private val supplier: () -> CardAction) {
        fun createNewInstance(): CardAction = supplier()
    }
}