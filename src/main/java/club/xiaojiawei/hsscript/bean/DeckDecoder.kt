package club.xiaojiawei.hsscript.bean

import java.util.Base64

/**
 * 卡组格式枚举
 */
enum class DeckFormat(val value: Int) {
    UNKNOWN(0),
    WILD(1),
    STANDARD(2),
    CLASSIC(3),
    TWIST(4);

    companion object {
        fun fromValue(value: Int): DeckFormat = entries.find { it.value == value } ?: UNKNOWN
    }
}

/**
 * 卡牌信息
 * @param dbfId 卡牌的数据库ID
 * @param count 卡牌数量
 */
data class DeckCardInfo(
    val dbfId: Int,
    val count: Int
)

/**
 * 解析后的卡组信息
 * @param format 卡组格式（标准/狂野等）
 * @param heroes 英雄的dbfId列表
 * @param cards 卡牌列表
 */
data class DeckInfo(
    val format: DeckFormat,
    val heroes: List<Int>,
    val cards: List<DeckCardInfo>
)

/**
 * 卡组代码解析异常
 */
class DeckParseException(message: String) : Exception(message)

/**
 * 炉石传说卡组代码解析器
 */
class DeckDecoder {

    /**
     * 解析卡组代码字符串
     * @param deckString Base64编码的卡组代码
     * @return 解析后的卡组信息
     * @throws DeckParseException 如果解析失败
     */
    fun decode(deckString: String): DeckInfo {
        val data = parseDeckString(deckString)
        return parseDeck(data)
    }

    /**
     * 将Base64编码的卡组代码解码为字节数组
     */
    private fun parseDeckString(deckString: String): ByteArrayReader {
        val binary = try {
            Base64.getDecoder().decode(deckString)
        } catch (e: IllegalArgumentException) {
            throw DeckParseException("Invalid Base64 encoding: ${e.message}")
        }
        return ByteArrayReader(binary)
    }

    /**
     * 解析卡组数据
     */
    private fun parseDeck(reader: ByteArrayReader): DeckInfo {
        // 读取保留字节，必须为0
        val reserve = reader.readVarint()
        if (reserve != 0) {
            throw DeckParseException("Invalid deckstring: reserve byte is $reserve, expected 0")
        }

        // 读取版本号，目前只支持版本1
        val version = reader.readVarint()
        if (version != 1) {
            throw DeckParseException("Unsupported deckstring version: $version")
        }

        // 读取卡组格式
        val formatValue = reader.readVarint()
        val format = DeckFormat.fromValue(formatValue)

        // 读取英雄
        val heroes = mutableListOf<Int>()
        val numHeroes = reader.readVarint()
        repeat(numHeroes) {
            heroes.add(reader.readVarint())
        }

        // 读取卡牌
        val cards = mutableListOf<DeckCardInfo>()

        // 读取单张卡牌（count = 1）
        val numCardsX1 = reader.readVarint()
        repeat(numCardsX1) {
            val cardId = reader.readVarint()
            cards.add(DeckCardInfo(dbfId = cardId, count = 1))
        }

        // 读取双张卡牌（count = 2）
        val numCardsX2 = reader.readVarint()
        repeat(numCardsX2) {
            val cardId = reader.readVarint()
            cards.add(DeckCardInfo(dbfId = cardId, count = 2))
        }

        // 读取多张卡牌（count = n）
        val numCardsXN = reader.readVarint()
        repeat(numCardsXN) {
            val cardId = reader.readVarint()
            val count = reader.readVarint()
            cards.add(DeckCardInfo(dbfId = cardId, count = count))
        }

        return DeckInfo(
            format = format,
            heroes = heroes,
            cards = cards
        )
    }

    /**
     * 将卡组信息编码为卡组代码字符串
     * @param deckInfo 卡组信息
     * @return Base64编码的卡组代码
     */
    fun encode(deckInfo: DeckInfo): String {
        val writer = ByteArrayWriter()

        // 写入保留字节（必须为0）
        writer.writeVarint(0)

        // 写入版本号（当前为1）
        writer.writeVarint(1)

        // 写入卡组格式
        writer.writeVarint(deckInfo.format.value)

        // 写入英雄数量和英雄ID
        writer.writeVarint(deckInfo.heroes.size)
        for (heroId in deckInfo.heroes) {
            writer.writeVarint(heroId)
        }

        // 按数量分组卡牌
        val cardsX1 = deckInfo.cards.filter { it.count == 1 }
        val cardsX2 = deckInfo.cards.filter { it.count == 2 }
        val cardsXN = deckInfo.cards.filter { it.count > 2 }

        // 写入单张卡牌
        writer.writeVarint(cardsX1.size)
        for (card in cardsX1) {
            writer.writeVarint(card.dbfId)
        }

        // 写入双张卡牌
        writer.writeVarint(cardsX2.size)
        for (card in cardsX2) {
            writer.writeVarint(card.dbfId)
        }

        // 写入多张卡牌
        writer.writeVarint(cardsXN.size)
        for (card in cardsXN) {
            writer.writeVarint(card.dbfId)
            writer.writeVarint(card.count)
        }

        return java.util.Base64.getEncoder().encodeToString(writer.toByteArray())
    }

    /**
     * 从 CardGroupInfo 编码为卡组代码
     * @param cardGroupInfo 卡牌组信息
     * @param heroDbfId 英雄的 dbfId（默认为0，会被省略）
     * @param format 卡组格式（默认为狂野）
     * @return Base64编码的卡组代码
     */
    fun encodeCardGroup(
        cardGroupInfo: CardGroupInfo,
        heroDbfId: Int = 0,
        format: DeckFormat = DeckFormat.WILD
    ): String {
        // 按 dbfId 分组并统计数量
        val cardCountMap = mutableMapOf<Int, Int>()
        for (card in cardGroupInfo.cards) {
            if (card.dbfId > 0) {
                cardCountMap[card.dbfId] = cardCountMap.getOrDefault(card.dbfId, 0) + 1
            }
        }

        val cards = cardCountMap.map { (dbfId, count) ->
            DeckCardInfo(dbfId, count)
        }

        val deckInfo = DeckInfo(
            format = format,
            heroes = if (heroDbfId > 0) listOf(heroDbfId) else emptyList(),
            cards = cards
        )

        return encode(deckInfo)
    }
}

/**
 * 字节数组读取器，支持读取Varint
 */
private class ByteArrayReader(private val data: ByteArray) {
    private var position = 0

    /**
     * 读取一个Varint（变长整数）
     * Varint是一种紧凑的整数编码方式，用于Protocol Buffers
     * 每个字节的最高位表示是否还有后续字节
     */
    fun readVarint(): Int {
        var shift = 0
        var result = 0
        var byte: Int

        do {
            if (position >= data.size) {
                throw DeckParseException("Unexpected end of data while reading varint")
            }
            byte = data[position++].toInt() and 0xFF
            result = result or ((byte and 0x7F) shl shift)
            shift += 7
        } while ((byte and 0x80) != 0)

        return result
    }
}

/**
 * 字节数组写入器，支持写入Varint
 */
private class ByteArrayWriter {
    private val bytes = mutableListOf<Byte>()

    /**
     * 写入一个Varint（变长整数）
     */
    fun writeVarint(value: Int) {
        var v = value
        while ((v and 0x7F.inv()) != 0) {
            bytes.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        bytes.add((v and 0x7F).toByte())
    }

    fun toByteArray(): ByteArray = bytes.toByteArray()
}

// ============== 使用示例 ==============
fun main() {
    val decoder = DeckDecoder()

    // 示例卡组代码（可替换为实际的卡组代码）
//    val deckCode = "AAECAf0GAA/yAa0CigfhB/sHxAjMCPYI+g6tEMQV2LsCl8EC68IC8tACAA=="
    val deckCode = "AAECAf0EDN74BdGeBrqnBsW6BrHOBuntBqn1BsODB9uXB4KYB+SyB8ekBgnx0wS0pwa2pwbCvgauwAbbwQaPzwaa9AazvQcAAQP3swbHpAb1swbHpAbr3gbHpAYAAA=="

    try {
        val deck = decoder.decode(deckCode)

        println("卡组格式: ${deck.format}")
        println("英雄 DbfId: ${deck.heroes}")
        println("卡牌列表:")
        deck.cards.forEach { card ->
            println("  - DbfId: ${card.dbfId}, 数量: ${card.count}")
        }
        println("总卡牌种类: ${deck.cards.size}")
        println("总卡牌数量: ${deck.cards.sumOf { it.count }}")
    } catch (e: DeckParseException) {
        println("解析失败: ${e.message}")
    }
}
