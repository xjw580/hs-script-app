package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.enums.TagEnum
import club.xiaojiawei.hsscriptcardsdk.bean.DBCard
import club.xiaojiawei.hsscriptcardsdk.enums.CardActionEnum
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface Token

@Serializable
internal sealed interface Quantity : Token

@Serializable
internal sealed interface Camp : Token

@Serializable
internal sealed interface Target : Token

@Serializable
internal sealed interface Segment : Token

@Serializable
internal data class Value(var target: TagEnum, var number: Int = 1) : Token

@Serializable
internal data object One : Quantity

@Serializable
internal data object All : Quantity

@Serializable
internal data object Enemy : Camp

@Serializable
internal data object Friendly : Camp

@Serializable
internal data object Minion : Target

@Serializable
internal data object Hero : Target

@Serializable
internal data object Role : Target

@Serializable
internal data object And : Segment

@Serializable
internal data object Comma : Segment

internal val dictionary = mapOf(
    "一个" to { One },
    "一只" to { One },
    "所有" to { All },
    "敌方" to { Enemy },
    "友方" to { Friendly },
    "随从" to { Minion },
    "角色" to { Role },
    "英雄" to { Hero },
    "并" to { And },
    "，" to { Comma },
    "冻结" to { Value(TagEnum.FROZEN) },
    "突袭" to { Value(TagEnum.RUSH) },
    "$" to { Value(TagEnum.HEALTH, -1) },
    "#" to { Value(TagEnum.HEALTH) },
    "+" to { Value(TagEnum.ATK) },
    "/+" to { Value(TagEnum.HEALTH) },
)

/**
 * @author 肖嘉威
 * @date 2026/2/4 15:50
 */
class CardDescriptionParser {

    companion object {
        private val htmlRegex = Regex("<.*?>")
        private val numberRegex = Regex("""(\d+)""")
    }

    private fun preprocess(text: String): String {
        return text
            .replace(htmlRegex, "")
            .replace("。", "")
            .trim()
    }

    private fun tokenize(text: String): MutableList<Token> {
        val clean = preprocess(text)
        val tokens = mutableListOf<Token>()

        var i = 0
        while (i < clean.length) {
            dictionary.entries
                .firstOrNull { clean.startsWith(it.key, i) }
                ?.let { matched ->
                    val token = matched.value()
                    tokens += token
                    i += matched.key.length
                    if (token is Value) {
                        val num = numberRegex.find(clean, i)
                        if (num != null && num.range.first == i) {
                            token.number = (num.groupValues[1].toIntOrNull() ?: 0) * token.number
                            i += num.value.length
                        }
                    }
                    continue
                }
            i++
        }
        if (tokens.firstOrNull() is Value) {
            tokens.addAll(0, listOf(One, Role))
        }
        return tokens
    }

    fun printInfo(text: String) {
        val tokens = tokenize(text)
        print("$text : ")
        println(tokens)
        //        val json = Json {
////            classDiscriminator = "type" // 可选，默认是 "type"
////            prettyPrint = true
//        }
//        val text = json.encodeToString<List<Token>>(tokenize)
//        val tokens: List<Token> =
//            json.decodeFromString<List<Token>>(text)
//        for (token in tokens) {
//            println(token)
//        }
//        println()
    }

    fun parse(dbCard: DBCard): CardGroupCard {
        val tokens = tokenize(dbCard.text)
        val cardGroupCard = CardGroupCard(dbCard.cardId, dbCard.dbfId, dbCard.name)
        if (tokens.isNotEmpty()) {
            val powerActions = mutableListOf<CardActionEnum>()
            val cardActionEnums = CardActionEnum.entries.toMutableList()
            for ((index, token) in tokens.withIndex()) {
                if (token is One){

                } else if (tokens is Segment){
                    break
                }
            }
            cardGroupCard.powerActions = powerActions
        }
        return cardGroupCard
    }

}

fun main() {
    val parser = CardDescriptionParser()
    parser.printInfo("使一只野兽获得+3/+4")
    parser.printInfo($$"造成$6点伤害")
    parser.printInfo($$"对一个随从造成$25点伤害")
    parser.printInfo($$"对一个角色造成$3点伤害，并使其<b>冻结</b>")
    parser.printInfo($$"造成$4点伤害，随机弃一张牌")
    parser.printInfo($$"对所有敌方随从造成$2点伤害，为所有友方角色恢复#2点生命值")
    parser.printInfo($$"对所有敌人造成$2点伤害")
    parser.printInfo($$"对所有敌方随从造成$2点伤害，并使其<b>冻结</b>")
    parser.printInfo($$"对所有随从造成$2点伤害>")
//    使一只野兽获得+3/+3。
//    造成$6点伤害。
//对一个随从造成$25点伤害。
//对一个角色造成$3点伤害，并使其<b>冻结</b>。
//对一个随从造成$4点伤害。
//造成$4点伤害，随机弃一张牌。
//对所有敌方随从造成$2点伤害，为所有友方角色恢复#2点生命值。
//对所有敌人造成$2点伤害。
//对所有敌方随从造成$2点伤害，并使其<b>冻结</b>。
//对所有非龙随从造成$5点伤害。
//对所有随从造成$2点伤害。
//使你手牌，牌库和战场上的所有野兽获得+2/+2。
//    无法攻击。使友方随从获得+1/+1和<b>突袭</b>。
}