package club.xiaojiawei.hsscript.bean

/**
 * @author 肖嘉威
 * @date 2026/2/4 8:41
 */
import java.util.regex.Pattern

/**
 * @author 肖嘉威
 * @date 2026/2/4 8:41
 */
class CardParser {
    
    fun parse(description: String): List<Effect> {
        val tokens = tokenize(description)
        return parseTokens(tokens)
    }

    private fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        // Remove HTML tags for simplicity for now, or handle them as separators
        val cleanText = text.replace(Regex("<[^>]*>"), "")
        
        while (i < cleanText.length) {
            val char = cleanText[i]
            if (char.isWhitespace()) {
                i++
                continue
            }
            
            // 1. Try to match static tokens (numbers, punctuation, markers)
            if (char.isDigit()) {
                var j = i
                while (j < cleanText.length && cleanText[j].isDigit()) j++
                tokens.add(Token(TokenType.NUMBER, cleanText.substring(i, j)))
                i = j
                continue
            }
            if (char == '$' || char == '#') {
                tokens.add(Token(TokenType.VALUE_MARKER, char.toString()))
                i++
                continue
            }
            if (char == '+') {
                tokens.add(Token(TokenType.PLUS, "+"))
                i++
                continue
            }
            if (char == '/') {
                tokens.add(Token(TokenType.SLASH, "/"))
                i++
                continue
            }
            if (char == '，' || char == ',' || char == '。') {
                tokens.add(Token(TokenType.PUNCTUATION, char.toString()))
                i++
                continue
            }

            // 2. Try to match Keywords (Longest match first)
            var bestMatch: String? = null
            var bestTokenType: TokenType? = null

            // Check all maps
            fun checkMap(map: Map<String, Any>, type: TokenType) {
                for (key in map.keys) {
                    if (cleanText.startsWith(key, i)) {
                        if (bestMatch == null || key.length > bestMatch!!.length) {
                            bestMatch = key
                            bestTokenType = type
                        }
                    }
                }
            }

            checkMap(targetKeywords, TokenType.TARGET)
            checkMap(actionKeywords, TokenType.ACTION)
            checkMap(effectKeywords, TokenType.EFFECT_TYPE)
            checkMap(miscKeywords, TokenType.KEYWORD) // "点伤害" etc.
            checkMap(connectors, TokenType.CONNECTOR)
            
            if (bestMatch != null) {
                tokens.add(Token(bestTokenType!!, bestMatch!!))
                i += bestMatch!!.length
                continue
            }

            // 3. Handle prefixes/single chars that are safe to skip or known
            if (ignoredChars.contains(char)) {
                i++
                continue
            }

            // 4. Fallback: Unknown character
            // Skip unknown characters
            i++ 
        }
        return tokens
    }

    private val ignoredChars = setOf('对', '为', '使', '个', '只', '一') // "个", "只" might be part of "一个" but if "一个" is not in map, skip. 
    // Added '一' to support "一只野兽" if "一只" is not fully matched or split. "一只野兽" is in `targetKeywords`.

    private val miscKeywords = mapOf(
        "点伤害" to TokenType.UNIT,
        "点生命值" to TokenType.UNIT,
        "张牌" to TokenType.QUANTIFIER
    )
    
    private val connectors = mapOf(
        "使其" to TokenType.CONNECTOR,
        "并" to TokenType.CONNECTOR
    )

    private val targetKeywords = mapOf(
        "所有敌方随从" to TargetType.ALL_ENEMY_MINIONS,
        "所有友方角色" to TargetType.ALL_FRIENDLY_CHARACTERS,
        "一个随从" to TargetType.ONE_MINION,
        "一个角色" to TargetType.ONE_CHARACTER,
        "一只野兽" to TargetType.ONE_BEAST
    )

    private val actionKeywords = mapOf(
        "造成" to ActionType.DEAL_DAMAGE,
        "恢复" to ActionType.RESTORE_HEALTH,
        "获得" to ActionType.BUFF,
        "弃" to ActionType.DISCARD
    )
    
     private val effectKeywords = mapOf(
        "冻结" to EffectType.FREEZE
    )


    private fun parseTokens(tokens: List<Token>): List<Effect> {
        val effects = mutableListOf<Effect>()
        var currentTarget: TargetType? = null
        var i = 0
        
        while (i < tokens.size) {
            val token = tokens[i]
            
            if (token.type == TokenType.TARGET) {
                 currentTarget = targetKeywords[token.value]
                 i++
            } else if (token.type == TokenType.ACTION) {
                val actionType = actionKeywords[token.value]
                if (actionType == ActionType.DEAL_DAMAGE || actionType == ActionType.RESTORE_HEALTH) {
                    // Expect value next
                    // Logic to find value: look ahead for NUMBER or VALUE_MARKER + NUMBER
                    var value = 0
                    var j = i + 1
                    while (j < tokens.size) {
                        val t = tokens[j]
                        if (t.type == TokenType.NUMBER) {
                            value = t.value.toInt()
                            j++ // consume number
                            break
                        } else if (t.type == TokenType.VALUE_MARKER && j + 1 < tokens.size && tokens[j+1].type == TokenType.NUMBER) {
                            value = tokens[j+1].value.toInt()
                            j += 2
                            break
                        }
                        if (t.type == TokenType.ACTION || t.type == TokenType.TARGET || t.type == TokenType.EFFECT_TYPE) {
                            break // Safety break
                        }
                        j++
                    }
                    
                    val effectType = if (actionType == ActionType.DEAL_DAMAGE) EffectType.DAMAGE else EffectType.HEAL
                    // Default to ANY if no target specified (e.g. "Deal $6 damage" implies any target)
                    effects.add(Effect(effectType, currentTarget ?: TargetType.ANY, value))
                    i = j
                } else if (actionType == ActionType.BUFF) {
                     // Look for +X/+Y
                    var val1 = 0
                    var val2 = 0
                    var j = i + 1
                     while (j < tokens.size) {
                         if (tokens[j].type == TokenType.PLUS && j+1 < tokens.size && tokens[j+1].type == TokenType.NUMBER) {
                             val1 = tokens[j+1].value.toInt()
                             j += 2
                             // Check for slash and second value
                             if (j < tokens.size && tokens[j].type == TokenType.SLASH) {
                                  j++
                                  if (j < tokens.size && tokens[j].type == TokenType.PLUS) j++ // Optional +
                                  if (j < tokens.size && tokens[j].type == TokenType.NUMBER) {
                                      val2 = tokens[j].value.toInt()
                                      j++
                                  }
                             }
                             break
                         }
                         if (tokens[j].type == TokenType.ACTION || tokens[j].type == TokenType.TARGET || tokens[j].type == TokenType.EFFECT_TYPE) break
                         j++
                     }
                     effects.add(Effect(EffectType.BUFF, currentTarget ?: TargetType.UNKNOWN, val1, val2))
                     i = j
                } else if (actionType == ActionType.DISCARD) {
                     // Simplistic discard logic: assumes 1 card for now
                     effects.add(Effect(EffectType.DISCARD, TargetType.SELF, 1)) 
                     i++
                } else {
                    i++
                }
            } else if (token.type == TokenType.EFFECT_TYPE) {
                // e.g. "Freeze"
                 val effectType = effectKeywords[token.value]
                 if (effectType != null) {
                     effects.add(Effect(effectType, currentTarget ?: TargetType.UNKNOWN))
                 }
                 i++
            } else if (token.type == TokenType.CONNECTOR) {
                // "and make it..." -> keeps currentTarget
                i++
            } else {
                i++
            }
        }
        
        return effects
    }
}

enum class TokenType {
    TARGET, ACTION, NUMBER, VALUE_MARKER, PUNCTUATION, SLASH, PLUS, KEYWORD, CONNECTOR, EFFECT_TYPE, QUANTIFIER, UNIT
}

data class Token(val type: TokenType, val value: String)

enum class EffectType {
    DAMAGE, HEAL, BUFF, FREEZE, DISCARD, UNKNOWN
}

enum class ActionType {
    DEAL_DAMAGE, RESTORE_HEALTH, BUFF, DISCARD
}

enum class TargetType {
    ALL_ENEMY_MINIONS, ALL_FRIENDLY_CHARACTERS, ONE_MINION, ONE_CHARACTER, ONE_BEAST, SELF, UNKNOWN, HERO, ANY
}

data class Effect(val type: EffectType, val target: TargetType, val value1: Int = 0, val value2: Int = 0) {
    override fun toString(): String {
        return if (type == EffectType.BUFF) "Effect(type=$type, target=$target, +$value1/+$value2)"
        else "Effect(type=$type, target=$target, value=$value1)"
    }
}

fun main() {
    val parser = CardParser()
    fun test(text: String) {
        println("Parsing: $text")
        val effects = parser.parse(text)
        effects.forEach { println(it) }
        println("---")
    }
    test("对所有敌方随从造成\$2点伤害，为所有友方角色恢复#2点生命值。")
    test("造成\$6点伤害")
    test("对一个随从造成\$25点伤害。")
    test("对一个角色造成\$3点伤害，并使其<b>冻结</b>。")
    test("对一个随从造成\$4点伤害。")
    test("造成\$4点伤害，随机弃一张牌。")
    test("对所有敌方随从造成\$2点伤害，并使其<b>冻结</b>。")
    test("对所有敌方随从造成\$2点伤害，并使其冻结。")
    test("对所有随从造成\$2点伤害。")
    test("使一只野兽获得+3/+3。")
}
