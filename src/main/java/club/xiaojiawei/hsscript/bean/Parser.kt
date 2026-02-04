package club.xiaojiawei.hsscript.bean

/**
 * @author 肖嘉威
 * @date 2026/2/4 8:41
 */
class DBCardParser {
//    fun parseAsGroupCard(dbCard: DBCard): CardGroupCard {
//        val cardDescription = dbCard.text
//        val chars = cardDescription.toCharArray()
//        for ((index, ch) in chars.withIndex()) {
//
//        }
//        val card = CardGroupCard()
//        val cardGroupCard = CardGroupCard(
//            cardId = dbCard.cardId,
//            dbfId = dbCard.dbfId,
//            name = dbCard.name,
//            playActions = listOf(CardActionEnum.NO_POINT),
//            powerActions = if (dbCard.type == CardTypeEnum.MINION.name ||
//                dbCard.type == CardTypeEnum.HERO.name ||
//                dbCard.type == CardTypeEnum.WEAPON.name
//            ) {
//                listOf(CardActionEnum.POINT_RIVAL)
//            } else if (dbCard.type == CardTypeEnum.SPELL.name) {
//                emptyList()
//            } else {
//                listOf(CardActionEnum.NO_POINT)
//            },
//            weight = 1.0,
//            powerWeight = 1.0,
//            changeWeight = if (dbCard.cost == null || dbCard.cost!! > 2) -1.0 else 0.0
//        )
//        return card
//    }


    /*========================================================================================================================*/

    sealed class Action {
        data class Damage(val amount: Int) : Action()
        data class Heal(val amount: Int) : Action()
        object Freeze : Action()
        data class Discard(val count: Int, val random: Boolean) : Action()
        data class Buff(val atk: Int, val hp: Int) : Action()
    }

    sealed class Target {
        object Self : Target()
        object Hero : Target()
        object EnemyHero : Target()

        data class Single(
            val type: UnitType? = null
        ) : Target()

        data class All(
            val side: Side? = null,
            val unitType: UnitType? = null,
            val exclude: UnitType? = null
        ) : Target()
    }

    enum class Side { FRIENDLY, ENEMY }
    enum class UnitType {
        MINION,
        CHARACTER,
        BEAST,
        DRAGON,
        HERO
    }


    sealed class Modifier {

        /** 随机 */
        object Random : Modifier()

        /** 仅受伤的 */
        object InjuredOnly : Modifier()

        /** 排除某种族 / 类型（非龙、非野兽） */
        data class Exclude(val unitType: UnitType) : Modifier()

        /** 重复执行 N 次 */
        data class Repeat(val times: Int) : Modifier()
    }


    data class EffectNode(
        val action: Action,
        val target: Target,
        val modifiers: List<Modifier> = emptyList()
    )

    enum class SegmentTrigger {
        AND, COMMA, FOR
    }

    enum class TokenType {
        NUMBER,

        DAMAGE, HEAL, FREEZE, DISCARD, BUFF,

        TARGET_SINGLE,
        TARGET_ALL,

        FRIENDLY, ENEMY,
        MINION, HERO, CHARACTER,
        BEAST, DRAGON,

        RANDOM,
        AND,
        COMMA,
        FOR,
        UNKNOWN,

        INJURED,

        NON,
    }

    data class Token(
        val type: TokenType,
        val text: String
    )

    val dictionary = mapOf(
        "造成" to TokenType.DAMAGE,
        "恢复" to TokenType.HEAL,
        "冻结" to TokenType.FREEZE,
        "弃" to TokenType.DISCARD,
        "获得" to TokenType.BUFF,

        "一个" to TokenType.TARGET_SINGLE,
        "所有" to TokenType.TARGET_ALL,

        "敌方" to TokenType.ENEMY,
        "友方" to TokenType.FRIENDLY,

        "随从" to TokenType.MINION,
        "角色" to TokenType.CHARACTER,
        "野兽" to TokenType.BEAST,
        "龙" to TokenType.DRAGON,

        "随机" to TokenType.RANDOM,
        "并" to TokenType.AND,
        "并" to TokenType.AND,
        "，" to TokenType.COMMA,
        "为" to TokenType.FOR,
    )

    val ACTION_TOKENS = setOf(
        TokenType.DAMAGE,
        TokenType.HEAL,
        TokenType.FREEZE,
        TokenType.DISCARD,
        TokenType.BUFF
    )


    fun splitByAction(tokens: List<Token>): List<List<Token>> {
        val actionIndices = tokens
            .mapIndexedNotNull { i, t -> if (t.type in ACTION_TOKENS) i else null }

        if (actionIndices.isEmpty()) return emptyList()

        val segments = mutableListOf<List<Token>>()

        for (i in actionIndices.indices) {
            val start = if (i == 0) 0 else actionIndices[i - 1]
            val end = if (i == actionIndices.lastIndex)
                tokens.size
            else
                actionIndices[i + 1]

            // segment = 从上一个 action 到下一个 action
            segments += tokens.subList(start, end)
        }

        return segments
    }


    private val htmlRegex = Regex("<.*?>")
    fun preprocess(text: String): String {
        return text
            .replace(htmlRegex, "")
            .replace("。", "")
            .trim()
    }

    private val numberRegex = Regex("""[$#](\d+)""")

    fun tokenize(text: String): List<Token> {
        val clean = preprocess(text)
        val tokens = mutableListOf<Token>()

        var i = 0
        while (i < clean.length) {
            val matched = dictionary.entries
                .firstOrNull { clean.startsWith(it.key, i) }

            if (matched != null) {
                tokens += Token(matched.value, matched.key)
                i += matched.key.length
                continue
            }

            val num = numberRegex.find(clean, i)
            if (num != null && num.range.first == i) {
                tokens += Token(TokenType.NUMBER, num.groupValues[1])
                i += num.value.length
                continue
            }

            i++
        }
        return tokens
    }

    fun splitByAnd(tokens: List<Token>): List<List<Token>> {
        val result = mutableListOf<MutableList<Token>>()
        var current = mutableListOf<Token>()

        for (t in tokens) {
            if (t.type == TokenType.AND) {
                result += current
                current = mutableListOf()
            } else {
                current += t
            }
        }
        result += current
        return result
    }

    fun parseTarget(tokens: List<Token>, modifiers: List<Modifier>): Target {
        val isAll = tokens.any { it.type == TokenType.TARGET_ALL }

        val side = when {
            tokens.any { it.type == TokenType.ENEMY } -> Side.ENEMY
            tokens.any { it.type == TokenType.FRIENDLY } -> Side.FRIENDLY
            else -> null
        }

        val unit = when {
            tokens.any { it.type == TokenType.BEAST } -> UnitType.BEAST
            tokens.any { it.type == TokenType.DRAGON } -> UnitType.DRAGON
            tokens.any { it.type == TokenType.MINION } -> UnitType.MINION
            tokens.any { it.type == TokenType.CHARACTER } -> UnitType.CHARACTER
            else -> null
        }

        val exclude = modifiers
            .filterIsInstance<Modifier.Exclude>()
            .firstOrNull()
            ?.unitType

        return if (isAll) {
            Target.All(side, unit, exclude)
        } else {
            Target.Single(unit)
        }
    }


    fun parseAction(tokens: List<Token>): Action {
        val num = tokens.firstOrNull { it.type == TokenType.NUMBER }?.text?.toInt()

        return when {
            tokens.any { it.type == TokenType.DAMAGE } ->
                Action.Damage(num!!)

            tokens.any { it.type == TokenType.HEAL } ->
                Action.Heal(num!!)

            tokens.any { it.type == TokenType.FREEZE } ->
                Action.Freeze

            tokens.any { it.type == TokenType.DISCARD } ->
                Action.Discard(1, tokens.any { it.type == TokenType.RANDOM })

            tokens.any { it.type == TokenType.BUFF } -> {
                val (atk, hp) = Regex("""\+(\d+)/\+(\d+)""")
                    .find(tokens.joinToString("") { it.text })!!
                    .destructured
                Action.Buff(atk.toInt(), hp.toInt())
            }

            else -> error("Unknown action")
        }
    }

    fun parseModifiers(tokens: List<Token>): List<Modifier> {
        val modifiers = mutableListOf<Modifier>()

        // 随机
        if (tokens.any { it.type == TokenType.RANDOM }) {
            modifiers += Modifier.Random
        }

        // 受伤的
        if (tokens.any { it.type == TokenType.INJURED }) {
            modifiers += Modifier.InjuredOnly
        }

        // 非龙 / 非野兽
        val nonIndex = tokens.indexOfFirst { it.type == TokenType.NON }
        if (nonIndex != -1 && nonIndex + 1 < tokens.size) {
            val unitType = when (tokens[nonIndex + 1].type) {
                TokenType.DRAGON -> UnitType.DRAGON
                TokenType.BEAST -> UnitType.BEAST
                else -> null
            }
            if (unitType != null) {
                modifiers += Modifier.Exclude(unitType)
            }
        }

        // 重复 N 次（例如：重复3次）
        val repeatIndex = tokens.indexOfFirst { it.text == "重复" }
        if (repeatIndex != -1) {
            val num = tokens
                .firstOrNull { it.type == TokenType.NUMBER }
                ?.text
                ?.toInt()

            if (num != null) {
                modifiers += Modifier.Repeat(num)
            }
        }

        return modifiers
    }


    fun cleanSegment(tokens: List<Token>): List<Token> =
        tokens.filterNot { it.type == TokenType.FOR }

    fun parse(text: String): List<EffectNode> {
        val tokens = tokenize(text)
        val segments = splitByAction(tokens)

        return segments.map { segment ->
            val clean = cleanSegment(segment)
            val modifiers = parseModifiers(clean)

            EffectNode(
                action = parseAction(clean),
                target = parseTarget(clean, modifiers),
                modifiers = modifiers
            )
        }
    }


}

/*
造成$6点伤害。
对一个随从造成$25点伤害。
对一个角色造成$3点伤害，并使其<b>冻结</b>。
对一个随从造成$4点伤害。
造成$4点伤害，随机弃一张牌。
对所有敌方随从造成$2点伤害，为所有友方角色恢复#2点生命值。
对所有敌人造成$2点伤害。
对所有敌方随从造成$2点伤害，并使其<b>冻结</b>。
对所有非龙随从造成$5点伤害。
对所有随从造成$2点伤害。
使你手牌，牌库和战场上的所有野兽获得+2/+2。
* */


fun main() {
    fun test(text: String) {
        println("text:$text")
        val parser = DBCardParser()
        val res = parser.parse(text)
        for (node in res) {
            println(node)
        }
        println()
    }
    test($$"对所有敌方随从造成$2点伤害，为所有友方角色恢复#2点生命值。")
    test($$"造成$6点伤害")
    test($$"对一个随从造成$25点伤害。")
    test($$"对一个角色造成$3点伤害，并使其<b>冻结</b>。")
    test($$"对一个随从造成$4点伤害。")
    test($$"造成$4点伤害，随机弃一张牌。")
    test($$"对所有敌方随从造成$2点伤害，并使其<b>冻结</b>。")
    test($$"对所有随从造成$2点伤害。")
    test($$"对所有敌方随从造成$2点伤害，并使其冻结。")
}