package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscriptcardsdk.bean.DBCard
import club.xiaojiawei.hsscriptcardsdk.enums.CardActionEnum
import club.xiaojiawei.hsscriptcardsdk.enums.CardEffectTypeEnum
import club.xiaojiawei.hsscriptcardsdk.enums.CardTypeEnum

/**
 * 卡牌描述分析结果
 */
data class CardTextAnalysisResult(
    /** 效果类型 */
    val effectType: CardEffectTypeEnum = CardEffectTypeEnum.UNKNOWN,
    /** 推荐的打出行为 */
    val playActions: List<CardActionEnum> = listOf(CardActionEnum.NO_POINT),
    /** 伤害值（如果是伤害类效果） */
    val damageValue: Int? = null,
    /** 攻击力增益值（如果是增益类效果） */
    val attackBuff: Int? = null,
    /** 生命值增益值（如果是增益类效果） */
    val healthBuff: Int? = null
)

/**
 * 卡牌描述分析器
 * @author 肖嘉威
 * @date 2026/2/4 8:41
 */
class DBCardParser {

    companion object {
        // ==================== 伤害类正则表达式 ====================
        
        /** 对敌方随从造成x点伤害 */
        private val DAMAGE_ENEMY_MINION_REGEX = Regex("对敌方随从造成(\\d+)点伤害")
        
        /** 对敌方角色造成x点伤害 */
        private val DAMAGE_ENEMY_CHARACTER_REGEX = Regex("对敌方角色造成(\\d+)点伤害")
        
        /** 造成x点伤害（通用，需要指向） */
        private val DAMAGE_GENERAL_REGEX = Regex("(?<!对[所所有敌友方]*[角色随从]*|对所有[敌友方]*[角色随从]*)造成(\\d+)点伤害")
        
        /** 对随从造成x点伤害 */
        private val DAMAGE_MINION_REGEX = Regex("对随从造成(\\d+)点伤害")
        
        /** 对所有角色造成x点伤害 */
        private val DAMAGE_ALL_CHARACTERS_REGEX = Regex("对所有角色造成(\\d+)点伤害")
        
        /** 对所有敌方角色造成x点伤害 */
        private val DAMAGE_ALL_ENEMY_CHARACTERS_REGEX = Regex("对所有敌方角色造成(\\d+)点伤害")
        
        /** 对所有敌方随从造成x点伤害 */
        private val DAMAGE_ALL_ENEMY_MINIONS_REGEX = Regex("对所有敌方随从造成(\\d+)点伤害")
        
        // ==================== 增益类正则表达式 ====================
        
        /** 使一个随从获得+x生命值 */
        private val BUFF_HEALTH_REGEX = Regex("使一个[友方敌方]*随从获得\\+(\\d+)生命值")
        
        /** 使一个随从获得+x攻击力 */
        private val BUFF_ATTACK_REGEX = Regex("使一个[友方敌方]*随从获得\\+(\\d+)攻击力")
        
        /** 使一个随从获得+x/+x */
        private val BUFF_STATS_REGEX = Regex("使一个([友方敌方]*)随从获得\\+(\\d+)/\\+(\\d+)")
        
        /** 使一个友方随从获得+x/+x */
        private val BUFF_FRIENDLY_STATS_REGEX = Regex("使一个友方随从获得\\+(\\d+)/\\+(\\d+)")
        
        /** 使一个敌方随从获得+x/+x */
        private val BUFF_ENEMY_STATS_REGEX = Regex("使一个敌方随从获得\\+(\\d+)/\\+(\\d+)")
    }

    /**
     * 分析卡牌描述文本，提取效果信息
     * @param text 卡牌描述文本
     * @return 分析结果
     */
    fun analyzeCardText(text: String): CardTextAnalysisResult {
        if (text.isBlank()) {
            return CardTextAnalysisResult()
        }
        
        // 移除HTML标签
        val cleanText = text.replace(Regex("<[^>]+>"), "")
        
        // 优先匹配更具体的模式
        
        // 1. 对所有敌方随从造成x点伤害（AOE，无指向）
        DAMAGE_ALL_ENEMY_MINIONS_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.DAMAGE,
                playActions = listOf(CardActionEnum.NO_POINT),
                damageValue = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 2. 对所有敌方角色造成x点伤害（AOE，无指向）
        DAMAGE_ALL_ENEMY_CHARACTERS_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.DAMAGE,
                playActions = listOf(CardActionEnum.NO_POINT),
                damageValue = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 3. 对所有角色造成x点伤害（AOE，无指向）
        DAMAGE_ALL_CHARACTERS_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.DAMAGE,
                playActions = listOf(CardActionEnum.NO_POINT),
                damageValue = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 4. 对敌方随从造成x点伤害（指向敌方随从）
        DAMAGE_ENEMY_MINION_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.DAMAGE,
                playActions = listOf(CardActionEnum.POINT_RIVAL_MINION),
                damageValue = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 5. 对敌方角色造成x点伤害（指向敌方角色）
        DAMAGE_ENEMY_CHARACTER_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.DAMAGE,
                playActions = listOf(CardActionEnum.POINT_RIVAL),
                damageValue = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 6. 对随从造成x点伤害（指向任意随从）
        DAMAGE_MINION_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.DAMAGE,
                playActions = listOf(CardActionEnum.POINT_MINION),
                damageValue = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 7. 使一个友方随从获得+x/+x
        BUFF_FRIENDLY_STATS_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.BUFF,
                playActions = listOf(CardActionEnum.POINT_MY_MINION),
                attackBuff = match.groupValues[1].toIntOrNull(),
                healthBuff = match.groupValues[2].toIntOrNull()
            )
        }
        
        // 8. 使一个敌方随从获得+x/+x
        BUFF_ENEMY_STATS_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.BUFF,
                playActions = listOf(CardActionEnum.POINT_RIVAL_MINION),
                attackBuff = match.groupValues[1].toIntOrNull(),
                healthBuff = match.groupValues[2].toIntOrNull()
            )
        }
        
        // 9. 使一个随从获得+x/+x（通用，指向任意随从，通常是友方）
        BUFF_STATS_REGEX.find(cleanText)?.let { match ->
            val target = match.groupValues[1]
            val action = when {
                target.contains("友方") -> CardActionEnum.POINT_MY_MINION
                target.contains("敌方") -> CardActionEnum.POINT_RIVAL_MINION
                else -> CardActionEnum.POINT_MY_MINION // 默认为友方
            }
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.BUFF,
                playActions = listOf(action),
                attackBuff = match.groupValues[2].toIntOrNull(),
                healthBuff = match.groupValues[3].toIntOrNull()
            )
        }
        
        // 10. 使一个随从获得+x攻击力
        BUFF_ATTACK_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.BUFF,
                playActions = listOf(CardActionEnum.POINT_MY_MINION),
                attackBuff = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 11. 使一个随从获得+x生命值
        BUFF_HEALTH_REGEX.find(cleanText)?.let { match ->
            return CardTextAnalysisResult(
                effectType = CardEffectTypeEnum.BUFF,
                playActions = listOf(CardActionEnum.POINT_MY_MINION),
                healthBuff = match.groupValues[1].toIntOrNull()
            )
        }
        
        // 12. 造成x点伤害（通用伤害，需要指向）
        // 需要排除已经匹配的 AOE 模式
        if (!cleanText.contains("对所有") && !cleanText.contains("对敌方") && !cleanText.contains("对随从")) {
            Regex("造成(\\d+)点伤害").find(cleanText)?.let { match ->
                return CardTextAnalysisResult(
                    effectType = CardEffectTypeEnum.DAMAGE,
                    playActions = listOf(CardActionEnum.POINT_WHATEVER),
                    damageValue = match.groupValues[1].toIntOrNull()
                )
            }
        }
        
        return CardTextAnalysisResult()
    }

    /**
     * 解析 DBCard 为 CardGroupCard
     * @param dbCard 数据库卡牌
     * @return 卡组卡牌
     */
    fun parseAsGroupCard(dbCard: DBCard): CardGroupCard {
        // 分析卡牌描述
        val analysisResult = analyzeCardText(dbCard.text)
        
        // 确定打出行为
        val playActions = if (analysisResult.playActions.isNotEmpty() && 
            analysisResult.playActions != listOf(CardActionEnum.NO_POINT)) {
            analysisResult.playActions
        } else {
            listOf(CardActionEnum.NO_POINT)
        }
        
        // 确定使用行为（随从、英雄、武器可以攻击敌方）
        val powerActions = if (dbCard.type == CardTypeEnum.MINION.name ||
            dbCard.type == CardTypeEnum.HERO.name ||
            dbCard.type == CardTypeEnum.WEAPON.name
        ) {
            listOf(CardActionEnum.POINT_RIVAL)
        } else if (dbCard.type == CardTypeEnum.SPELL.name) {
            emptyList()
        } else {
            listOf(CardActionEnum.NO_POINT)
        }
        
        // 确定效果类型
        val effectType = if (analysisResult.effectType != CardEffectTypeEnum.UNKNOWN) {
            analysisResult.effectType
        } else {
            CardEffectTypeEnum.UNKNOWN
        }
        
        return CardGroupCard(
            cardId = dbCard.cardId,
            dbfId = dbCard.dbfId,
            name = dbCard.name,
            effectType = effectType,
            playActions = playActions,
            powerActions = powerActions,
            weight = 1.0,
            powerWeight = 1.0,
            changeWeight = if (dbCard.cost == null || dbCard.cost!! > 2) -1.0 else 0.0
        )
    }

}