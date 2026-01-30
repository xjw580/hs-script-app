package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscriptcardsdk.CardAction
import club.xiaojiawei.hsscriptcardsdk.bean.Card
import club.xiaojiawei.hsscriptcardsdk.bean.War
import club.xiaojiawei.hsscriptcardsdk.data.COIN_CARD_ID
import club.xiaojiawei.hsscriptcardsdk.enums.ZoneEnum
import club.xiaojiawei.hsscript.bean.CommonCardAction
import club.xiaojiawei.hsscript.bean.CommonCardAction.Companion.DEFAULT
import club.xiaojiawei.hsscript.bean.log.ExtraEntity
import club.xiaojiawei.hsscript.bean.log.TagChangeEntity
import club.xiaojiawei.hsscript.bean.single.WarEx
import club.xiaojiawei.hsscript.status.CardActionManager.CARD_ACTION_MAP
import club.xiaojiawei.hsscript.status.DeckStrategyManager
import club.xiaojiawei.hsscriptcardsdk.mapper.BaseCardMapper
import club.xiaojiawei.hsscriptcardsdk.mapper.EntityMapper
import java.util.function.Supplier

/**
 * @author 肖嘉威
 * @date 2024/9/6 21:07
 */
object CardUtil {

    fun updateCardByExtraEntity(extraEntity: ExtraEntity, card: Card?) {
        card?.let {
            BaseCardMapper.INSTANCE.update(extraEntity.extraCard.card, card)
            EntityMapper.INSTANCE.update(extraEntity, card)
        }
    }

    fun exchangeAreaOfCard(extraEntity: ExtraEntity, war: War): Card? {
        val sourceArea = war.cardMap[extraEntity.entityId]?.area ?: return null
        val targetArea = WarEx.getPlayer(extraEntity.playerId).getArea(extraEntity.extraCard.zone) ?: return null

        val card = sourceArea.removeByEntityId(extraEntity.entityId) ?: return null
        targetArea.add(card, extraEntity.extraCard.zonePos)

        return card
    }

    fun exchangeAreaOfCard(tagChangeEntity: TagChangeEntity, war: War): Card? {
        val sourceCard = war.cardMap[tagChangeEntity.entityId] ?: return null
        val targetArea =
            WarEx.getPlayer(tagChangeEntity.playerId).getArea(ZoneEnum.valueOf(tagChangeEntity.value)) ?: return null

        sourceCard.area.removeByEntityId(tagChangeEntity.entityId) ?: return null
        targetArea.add(sourceCard, 0)

        return sourceCard
    }

    fun setCardAction(card: Card?) {
        card ?: return
        val deckStrategy = DeckStrategyManager.currentDeckStrategy
        deckStrategy ?: return

        if (card.isCoinCard) {
            card.cardId = COIN_CARD_ID
        }

        val supplier: Supplier<CardAction>? = (CARD_ACTION_MAP[deckStrategy.pluginId]?.get(card.cardId)) ?: let {
            CARD_ACTION_MAP[""]?.get(card.cardId)
        }

        val cardAction = supplier?.get() ?: if (card.action === DEFAULT) CommonCardAction() else card.action
        cardAction.belongCard = card
        card.action = cardAction
    }

}
