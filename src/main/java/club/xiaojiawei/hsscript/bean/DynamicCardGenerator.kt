package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscriptcardsdk.CardAction
import club.xiaojiawei.hsscriptcardsdk.bean.Card
import club.xiaojiawei.hsscriptcardsdk.bean.PlayAction
import club.xiaojiawei.hsscriptcardsdk.bean.Player
import club.xiaojiawei.hsscriptcardsdk.bean.War
import net.bytebuddy.ByteBuddy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.This
import net.bytebuddy.matcher.ElementMatchers

object DynamicCardGenerator {

    fun generateCardActionClass(
        className: String,
        cardIds: Array<String>,
        actionApplier: (War, Player, Card) -> List<PlayAction>
    ): Class<out CardAction> {
        lateinit var generatedClass: Class<out CardAction>

        val unloaded = ByteBuddy()
            .subclass(CardAction.DefaultCardAction::class.java)
            .name(className)
            .method(ElementMatchers.named("getCardId"))
            .intercept(MethodDelegation.to(GetCardIdInterceptor(cardIds)))
            .method(ElementMatchers.named("generatePlayActions"))
            .intercept(MethodDelegation.to(GeneratePlayActionsInterceptor(actionApplier)))
            .method(ElementMatchers.named("createNewInstance"))
            .intercept(
                MethodDelegation.to(
                    CreateNewInstanceInterceptor {
                        generatedClass.getDeclaredConstructor().newInstance()
                    }
                )
            )
            .make()

        generatedClass = unloaded
            .load(DynamicCardGenerator::class.java.classLoader)
            .loaded as Class<out CardAction>

        return generatedClass
    }

    fun generateCardActionClass(
        className: String,
        cardIds: Array<String>,
        delegate: Any
    ): Class<out CardAction> {

        lateinit var generatedClass: Class<out CardAction>

        val unloaded = ByteBuddy()
            .subclass(CardAction.DefaultCardAction::class.java)
            .name(className)
            .method(ElementMatchers.named("getCardId"))
            .intercept(MethodDelegation.to(GetCardIdInterceptor(cardIds)))
            .method(ElementMatchers.named("generatePlayActions"))
            .intercept(MethodDelegation.to(delegate))
            .method(ElementMatchers.named("createNewInstance"))
            .intercept(
                MethodDelegation.to(
                    CreateNewInstanceInterceptor {
                        generatedClass.getDeclaredConstructor().newInstance()
                    }
                )
            )
            .make()

        generatedClass = unloaded
            .load(DynamicCardGenerator::class.java.classLoader)
            .loaded as Class<out CardAction>

        return generatedClass
    }

    class GetCardIdInterceptor(private val cardIds: Array<String>) {
        fun getCardId(): Array<String> = cardIds
    }

    class GeneratePlayActionsInterceptor(private val actionApplier: (War, Player, Card) -> List<PlayAction>) {
        fun generatePlayActions(
            @Argument(0) war: War,
            @Argument(1) player: Player,
            @This cardAction: CardAction
        ): List<PlayAction> {
            val belongCard = cardAction.belongCard ?: return emptyList()
            return actionApplier(war, player, belongCard)
        }
    }

    class CreateNewInstanceInterceptor(
        private val supplier: () -> CardAction
    ) {
        fun createNewInstance(): CardAction = supplier()
    }


}
