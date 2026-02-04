package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscriptcardsdk.CardAction
import club.xiaojiawei.hsscriptcardsdk.bean.PlayAction
import club.xiaojiawei.hsscriptcardsdk.bean.Player
import club.xiaojiawei.hsscriptcardsdk.bean.War
import club.xiaojiawei.hsscriptcardsdk.util.CardUtil
import net.bytebuddy.implementation.bind.annotation.Argument
import net.bytebuddy.implementation.bind.annotation.This

/**
 * @author 肖嘉威
 * @date 2026/2/5 14:28
 */
interface PlayActionsInterceptor {
    fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction>
}

/*法术单体敌方*/

class SpellPointRivalMinionPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        val result = mutableListOf<PlayAction>()
        val rivalPlayCards = war.rival.playArea.cards
        cardAction.apply {
            for (rivalCard in rivalPlayCards) {
                if (rivalCard.canHurt() && rivalCard.canBeTargetedByRivalSpells()) {
                    result.add(
                        PlayAction({ newWar ->
                            findSelf(newWar)?.action?.power(rivalCard.action.findSelf(newWar))
                        }, { newWar ->
                            spendSelfCost(newWar)
                            removeSelf(newWar)?.let {
                                rivalCard.action.findSelf(newWar)?.injured(damage + newWar.me.getSpellPower())
                            }
                        }, belongCard)
                    )
                }
            }
        }
        return result
    }
}

class SpellPointRivalPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        val result = mutableListOf<PlayAction>()
        val rivalPlayCards = war.rival.playArea.hero?.let {
            buildList {
                addAll(war.rival.playArea.cards)
                add(it)
            }
        } ?: war.rival.playArea.cards

        cardAction.apply {
            for (rivalCard in rivalPlayCards) {
                if (rivalCard.canHurt() && rivalCard.canBeTargetedByRivalSpells()) {
                    result.add(
                        PlayAction({ newWar ->
                            findSelf(newWar)?.action?.power(rivalCard.action.findSelf(newWar))
                        }, { newWar ->
                            spendSelfCost(newWar)
                            removeSelf(newWar)?.let {
                                rivalCard.action.findSelf(newWar)?.injured(damage + newWar.me.getSpellPower())
                            }
                        }, belongCard)
                    )
                }
            }
        }
        return result
    }
}

class SpellPointRivalHeroPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        cardAction.apply {
            war.rival.playArea.hero?.let { rivalCard ->
                if (rivalCard.canHurt() && rivalCard.canBeTargetedByRivalSpells()) {
                    return listOf(PlayAction({ newWar ->
                        findSelf(newWar)?.action?.power(rivalCard.action.findSelf(newWar))
                    }, { newWar ->
                        spendSelfCost(newWar)
                        removeSelf(newWar)?.let {
                            rivalCard.action.findSelf(newWar)?.injured(damage + newWar.me.getSpellPower())
                        }
                    }, belongCard))
                }
            }
        }
        return emptyList()
    }
}

/*法术群体敌方*/

class SpellAllRivalMinionPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        val result = mutableListOf<PlayAction>()
        cardAction.apply {
            PlayAction({ newWar ->
                findSelf(newWar)?.action?.power()
            }, { newWar ->
                spendSelfCost(newWar)
                removeSelf(newWar)?.let {
                    val cards = newWar.rival.playArea.cards.toList()
                    for (card in cards) {
                        if (card.canHurt()) {
                            card.injured(damage + newWar.me.getSpellPower())
                        }
                    }
                }
            }, belongCard)
        }
        return result
    }
}

class SpellAllRivalPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        val result = mutableListOf<PlayAction>()
        cardAction.apply {
            PlayAction({ newWar ->
                findSelf(newWar)?.action?.power()
            }, { newWar ->
                spendSelfCost(newWar)
                removeSelf(newWar)?.let {
                    val cards = buildList {
                        addAll(newWar.rival.playArea.cards)
                        newWar.rival.playArea.hero?.let {
                            add(it)
                        }
                    }
                    for (card in cards) {
                        if (card.canHurt()) {
                            card.injured(damage + newWar.me.getSpellPower())
                        }
                    }
                }
            }, belongCard)
        }
        return result
    }
}

/*随从单体敌方*/

class MinionPointRivalMinionPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        val result = mutableListOf<PlayAction>()
        val rivalPlayCards = war.rival.playArea.cards
        cardAction.apply {
            for (rivalCard in rivalPlayCards) {
                if (rivalCard.canHurt() && rivalCard.canBeTargetedByRivalSpells()) {
                    result.add(
                        PlayAction({ newWar ->
                            findSelf(newWar)?.action?.power(false)?.pointTo(rivalCard.action.findSelf(newWar))
                        }, { newWar ->
                            spendSelfCost(newWar)
                            val me = newWar.me
                            removeSelf(newWar)?.let { card ->
                                if (me.playArea.safeAdd(card)) {
                                    CardUtil.handleCardExhaustedWhenIntoPlayArea(card)
                                    rivalCard.action.findSelf(newWar)?.injured(damage)
                                }
                            }
                        }, belongCard)
                    )
                }
            }
        }
        return result
    }
}

class MinionPointRivalPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        val result = mutableListOf<PlayAction>()
        val rivalPlayCards = war.rival.playArea.hero?.let {
            buildList {
                addAll(war.rival.playArea.cards)
                add(it)
            }
        } ?: war.rival.playArea.cards
        cardAction.apply {
            for (rivalCard in rivalPlayCards) {
                if (rivalCard.canHurt() && rivalCard.canBeTargetedByRivalSpells()) {
                    result.add(
                        PlayAction({ newWar ->
                            findSelf(newWar)?.action?.power(false)?.pointTo(rivalCard.action.findSelf(newWar))
                        }, { newWar ->
                            spendSelfCost(newWar)
                            val me = newWar.me
                            removeSelf(newWar)?.let { card ->
                                if (me.playArea.safeAdd(card)) {
                                    CardUtil.handleCardExhaustedWhenIntoPlayArea(card)
                                    rivalCard.action.findSelf(newWar)?.injured(damage)
                                }
                            }
                        }, belongCard)
                    )
                }
            }
        }
        return result
    }
}

class MinionPointRivalHeroPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        cardAction.apply {
            war.rival.playArea.hero?.let { rivalCard ->
                if (rivalCard.canHurt() && rivalCard.canBeTargetedByRivalSpells()) {
                    listOf(PlayAction({ newWar ->
                        findSelf(newWar)?.action?.power(false)?.pointTo(rivalCard.action.findSelf(newWar))
                    }, { newWar ->
                        spendSelfCost(newWar)
                        val me = newWar.me
                        removeSelf(newWar)?.let { card ->
                            if (me.playArea.safeAdd(card)) {
                                CardUtil.handleCardExhaustedWhenIntoPlayArea(card)
                                rivalCard.action.findSelf(newWar)?.injured(damage)
                            }
                        }
                    }, belongCard))
                }
            }
        }
        return emptyList()
    }
}

/*随从群体敌方*/

class MinionAllRivalMinionPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        cardAction.apply {
            return listOf(PlayAction({ newWar ->
                findSelf(newWar)?.action?.power()
            }, { newWar ->
                spendSelfCost(newWar)
                removeSelf(newWar)?.let { card ->
                    CardUtil.handleCardExhaustedWhenIntoPlayArea(card)
                    val cards = newWar.rival.playArea.cards.toList()
                    for (card in cards) {
                        if (card.canHurt()) {
                            card.injured(damage)
                        }
                    }
                }
            }, belongCard))

        }
    }
}

class MinionAllRivalPlayActionsInterceptor(private val damage: Int) : PlayActionsInterceptor {
    override fun generatePlayActions(
        @Argument(0) war: War,
        @Argument(1) player: Player,
        @This cardAction: CardAction
    ): List<PlayAction> {
        cardAction.apply {
            return listOf(PlayAction({ newWar ->
                findSelf(newWar)?.action?.power()
            }, { newWar ->
                spendSelfCost(newWar)
                removeSelf(newWar)?.let { card ->
                    CardUtil.handleCardExhaustedWhenIntoPlayArea(card)
                    val cards = newWar.rival.playArea.hero?.let {
                        buildList {
                            add(it)
                            addAll(newWar.rival.playArea.cards)
                        }
                    } ?: newWar.rival.playArea.cards.toList()
                    for (card in cards) {
                        if (card.canHurt()) {
                            card.injured(damage)
                        }
                    }
                }
            }, belongCard))

        }
    }
}