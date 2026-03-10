package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscript.enums.BlockTypeEnum
import club.xiaojiawei.hsscript.enums.TagEnum
import club.xiaojiawei.hsscriptcardsdk.bean.isValid
import club.xiaojiawei.hsscriptcardsdk.enums.ZoneEnum
import java.io.File
import java.io.RandomAccessFile
import java.util.ArrayDeque

// -------------------- ENTITY INFO --------------------

data class EntityInfo(
    val entityName: String? = null,
    val id: Int? = null,
    val zone: ZoneEnum? = null,
    val zonePos: Int? = null,
    val cardId: String? = null,
    val player: Int? = null
)

// -------------------- POWER NODE --------------------

sealed interface PowerNode

data class BlockNode(
    val type: BlockTypeEnum,
    val entityStr: String,
    val children: MutableList<PowerNode> = mutableListOf()
) : PowerNode

data class TagChangeNode(
    val entityStr: String,
    val entityId: Int?,
    val tag: TagEnum,
    val value: String
) : PowerNode

data class EntityNode(
    val type: String, // SHOW_ENTITY, FULL_ENTITY, CHANGE_ENTITY
    val entityId: Int,
    val cardId: String?,
    val info: EntityInfo? = null,
    val tags: MutableList<TagNode> = mutableListOf()
) : PowerNode

data class TagNode(
    val tag: TagEnum,
    val value: String
) : PowerNode

data class SystemNode(
    val type: String,
    val line: String
) : PowerNode

// -------------------- REGEX --------------------

object RegexRepo {

    // BLOCK_START BlockType=PLAY Entity=[entityName=吉安娜的礼物 id=13 zone=HAND zonePos=4 cardId=GIFT_02 player=1] EffectCardId= EffectIndex=-1 Target=0 SubOption=-1 TriggerKeyword=0
    val blockStart =
        Regex("""BLOCK_START BlockType=([A-Z0-9_]+) Entity=(.+?) (?:EffectCardId|EffectIndex|Target|SubOption|TriggerKeyword)""")

    val blockEnd =
        Regex("""BLOCK_END""")

    // TAG_CHANGE Entity=[entityName=UNKNOWN ENTITY [cardType=INVALID] id=89 zone=HAND zonePos=4 cardId= player=2] tag=ZONE_POSITION value=0
    // TAG_CHANGE Entity=128 tag=DISPLAYED_CREATOR value=46
    val tagChange =
        Regex("""TAG_CHANGE Entity=(.+?) tag=([A-Z0-9_]+) value=([^\s]+)""")

    // SHOW_ENTITY - Entity=[entityName=吉安娜的礼物 id=13 zone=HAND zonePos=4 cardId=GIFT_02 player=1] CardID=CS2_024
    val entityLine =
        Regex("""(SHOW_ENTITY|FULL_ENTITY|CHANGE_ENTITY) - Entity=(.+?)(?: CardID=([A-Z0-9_]+))?$""")

    val tagLine =
        Regex("""\s+tag=([A-Z0-9_]+) value=([^\s]+)""")

    val createGame = Regex("""CREATE_GAME""")

    fun parseEntityStr(entityStr: String): EntityInfo? {
        if (!entityStr.startsWith("[") || !entityStr.endsWith("]")) return null
        val inner = entityStr.substring(1, entityStr.length - 1)
        
        // Manual parsing because of potential nested brackets in entityName
        val idIdx = inner.lastIndexOf(" id=")
        if (idIdx == -1) return null
        val name = inner.substring(0, idIdx).removePrefix("entityName=").trim()
        
        val remaining = inner.substring(idIdx + 1)
        val parts = remaining.split(" ")
        val map = parts.associate { 
            val pair = it.split("=")
            if (pair.size == 2) pair[0] to pair[1] else "" to ""
        }
        
        return EntityInfo(
            entityName = name,
            id = map["id"]?.toIntOrNull(),
            zone = map["zone"]?.let { try { ZoneEnum.valueOf(it) } catch(e: Exception) { null } },
            zonePos = map["zonePos"]?.toIntOrNull(),
            cardId = map["cardId"],
            player = map["player"]?.toIntOrNull()
        )
    }

    fun parseEntityId(entityStr: String): Int? {
        return if (entityStr.startsWith("[")) {
            parseEntityStr(entityStr)?.id
        } else {
            entityStr.toIntOrNull()
        }
    }
}

// -------------------- PARSER --------------------

class PowerLogParser {

    private val stack = ArrayDeque<BlockNode>()
    val roots = mutableListOf<PowerNode>()

    private var lastEntityNode: EntityNode? = null

    fun parse(line: String) {
        if (parseBlockStart(line)) return
        if (parseBlockEnd(line)) return
        if (parseEntityLine(line)) return
        if (parseTagChange(line)) return
        if (parseTagLine(line)) return
        if (parseSystemLine(line)) return
    }

    private fun currentBlock(): BlockNode? = stack.peekLast()

    private fun parseBlockStart(line: String): Boolean {
        return RegexRepo.blockStart.find(line)?.let {
            val typeStr = it.groupValues[1]
            val entityStr = it.groupValues[2]
            
            val node = BlockNode(
                BlockTypeEnum.fromString(typeStr),
                entityStr
            )

            val parent = currentBlock()
            if (parent != null) {
                parent.children.add(node)
            } else {
                roots.add(node)
            }

            stack.addLast(node)
            lastEntityNode = null
            true
        } ?: false
    }

    private fun parseBlockEnd(line: String): Boolean {
        if (RegexRepo.blockEnd.containsMatchIn(line)) {
            if (stack.isNotEmpty()) {
                stack.removeLast()
            }
            lastEntityNode = null
            return true
        }
        return false
    }

    private fun parseEntityLine(line: String): Boolean {
        return RegexRepo.entityLine.find(line)?.let {
            val type = it.groupValues[1]
            val entityStr = it.groupValues[2]
            val cardId = it.groupValues[3].takeIf { id -> id.isNotEmpty() }
            
            val info = RegexRepo.parseEntityStr(entityStr)
            val id = info?.id ?: 0
            
            val node = EntityNode(type, id, cardId, info)
            val current = currentBlock()
            if (current != null) {
                current.children.add(node)
            } else {
                roots.add(node)
            }
            lastEntityNode = node
            true
        } ?: false
    }

    private fun parseTagLine(line: String): Boolean {
        return RegexRepo.tagLine.find(line)?.let {
            val tagName = it.groupValues[1]
            val value = it.groupValues[2]

            val tag = TagEnum.fromString(tagName)
            if (tag != TagEnum.UNKNOWN) {
                val node = TagNode(tag, value)
                lastEntityNode?.tags?.add(node)
            }
            true
        } ?: false
    }

    private fun parseTagChange(line: String): Boolean {
        return RegexRepo.tagChange.find(line)?.let {
            val entityStr = it.groupValues[1]
            val tagName = it.groupValues[2]
            val value = it.groupValues[3]

            val tag = TagEnum.fromString(tagName)
            if (tag != TagEnum.UNKNOWN) {
                val entityId = RegexRepo.parseEntityId(entityStr)
                val node = TagChangeNode(entityStr, entityId, tag, value)
                val current = currentBlock()
                if (current != null) {
                    current.children.add(node)
                } else {
                    roots.add(node)
                }
            }
            lastEntityNode = null
            true
        } ?: false
    }

    private fun parseSystemLine(line: String): Boolean {
        if (RegexRepo.createGame.containsMatchIn(line)) {
            val node = SystemNode("CREATE_GAME", line)
            val current = currentBlock()
            if (current != null) {
                current.children.add(node)
            } else {
                roots.add(node)
            }
            return true
        }
        return false
    }
}

// -------------------- BLOCK EXECUTOR --------------------

class BlockExecutor {

    private val war = club.xiaojiawei.hsscriptcardsdk.status.WAR
    private val fullCardStack = club.xiaojiawei.hsscript.bean.FixedSizeStack<club.xiaojiawei.hsscriptcardsdk.bean.Card>(10)
    private var chooseFuture: java.util.concurrent.Future<*>? = null

    fun execute(node: PowerNode) {
        when (node) {
            is BlockNode -> {
                node.children.forEach { execute(it, node) }
            }
            is EntityNode -> handleEntityNode(node)
            is TagChangeNode -> handleTagChangeNode(node)
            is SystemNode -> {}
            is TagNode -> {}
        }
    }

    fun execute(node: PowerNode, parentBlock: BlockNode?) {
        when (node) {
            is BlockNode -> {
                node.children.forEach { execute(it, node) }
            }
            is EntityNode -> handleEntityNode(node, parentBlock)
            is TagChangeNode -> handleTagChangeNode(node)
            is SystemNode -> {}
            is TagNode -> {}
        }
    }

    private fun handleEntityNode(node: EntityNode, parentBlock: BlockNode? = null) {
        when (node.type) {
            "SHOW_ENTITY" -> handleShowEntity(node)
            "FULL_ENTITY" -> handleFullEntity(node, parentBlock)
            "CHANGE_ENTITY" -> handleChangeEntity(node)
        }
    }

    private fun handleShowEntity(node: EntityNode) {
        val extraEntity = node.toExtraEntity()
        val card = war.cardMap[extraEntity.entityId]

        if (extraEntity.extraCard.zone === extraEntity.zone || extraEntity.extraCard.zone === null) {
            CardUtil.updateCardByExtraEntity(extraEntity, card)
        } else {
            CardUtil.updateCardByExtraEntity(extraEntity, card)
            CardUtil.exchangeAreaOfCard(extraEntity, war)
        }
    }

    private fun handleFullEntity(node: EntityNode, parentBlock: BlockNode?) {
        val extraEntity = node.toExtraEntity()
        if (war.cardMap[extraEntity.entityId] == null) {
            val card = club.xiaojiawei.hsscriptcardsdk.bean.Card(club.xiaojiawei.hsscript.bean.CommonCardAction.DEFAULT).apply {
                CardUtil.updateCardByExtraEntity(extraEntity, this)
                war.cardMap[extraEntity.entityId] = this
                CardUtil.setCardAction(this)
                cardIdChangeListener = java.util.function.BiConsumer { oldCardId, newCardId ->
                    CardUtil.setCardAction(this)
                }
                war.maxEntityId = entityId
            }

            fullCardStack.push(card)

            club.xiaojiawei.hsscript.bean.single.WarEx.getPlayer(extraEntity.playerId).getArea(extraEntity.extraCard.zone)
                ?.add(card, extraEntity.extraCard.zonePos)
                ?: club.xiaojiawei.hsscriptbase.config.log.debug { "生成的card【entityId:${card.entityId}】不应没有area" }
            
            val creator = card.creator
            if (creator.isNotEmpty()) {
                war.cardMap[creator]?.child?.add(card) ?: club.xiaojiawei.hsscriptbase.config.log.debug { "找不到creator:${card.creator}" }
            }

            if (parentBlock != null) {
                dealTriggerChoose(parentBlock)
            }
        }
    }

    private fun dealTriggerChoose(block: BlockNode) {
        if (fullCardStack.size() < 3 && war.isMyTurn) return
        if (block.type !== BlockTypeEnum.POWER && block.type !== BlockTypeEnum.UNKNOWN) return

        val testChooseCard: (club.xiaojiawei.hsscriptcardsdk.bean.Card) -> Boolean = { testCard ->
            val blockEntityId = RegexRepo.parseEntityId(block.entityStr)
            testCard.area::class.java === club.xiaojiawei.hsscriptcardsdk.bean.area.SetasideArea::class.java
                    && testCard.creator.isNotEmpty()
                    && (blockEntityId == null || testCard.creator == blockEntityId.toString())
        }

        var creator: String? = null
        val chooseCards = java.util.ArrayList<club.xiaojiawei.hsscriptcardsdk.bean.Card>()
        val cards = fullCardStack.toList()
        for (i in cards.indices.reversed()) {
            val chooseCard = cards[i]
            if (creator == null) {
                creator = chooseCard.creator
            } else if (chooseCard.creator != creator) {
                break
            }
            if (testChooseCard(chooseCard)) {
                chooseCards.add(0, chooseCard)
            } else {
                break
            }
        }
        chooseCards.removeIf { it.isNightmareBonus }
        if (chooseCards.isNotEmpty()) {
            chooseFuture?.cancel(true)
            chooseFuture = club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL.schedule({
                club.xiaojiawei.hsscriptcardsdk.status.WAR.isChooseCardTime = true
                club.xiaojiawei.hsscriptbase.config.log.info { "发现卡牌：${chooseCards}" }
                (club.xiaojiawei.hsscript.bean.DiscoverCardThread {
                    try {
                        if (chooseCards.size == 1) {
                            GameUtil.chooseDiscoverCard(1, 3)
                        } else {
                            club.xiaojiawei.hsscript.strategy.DeckStrategyActuator.discoverChooseCard(chooseCards)
                        }
                    } finally {
                        club.xiaojiawei.hsscriptcardsdk.status.WAR.isChooseCardTime = false
                        try {
                            club.xiaojiawei.hsscript.config.DRIVER_LOCK.lock()
                            SystemUtil.delay(2000)
                        } finally {
                            club.xiaojiawei.hsscript.config.DRIVER_LOCK.unlock()
                        }
                    }
                }.also { club.xiaojiawei.hsscript.strategy.AbstractPhaseStrategy.addTask(it) }).start()

            }, 1500, java.util.concurrent.TimeUnit.MILLISECONDS)
        }
    }

    private fun handleChangeEntity(node: EntityNode) {
        val extraEntity = node.toExtraEntity()
        val card = war.cardMap[extraEntity.entityId]
        club.xiaojiawei.hsscriptbase.config.log.info {
            String.format(
                "玩家%s【%s】 的 【entityId:%s】 由 【entityName:%s，cardId:%s】 变形成了 【entityName:，cardId:%s】",
                extraEntity.playerId,
                club.xiaojiawei.hsscript.bean.single.WarEx.getPlayer(extraEntity.playerId).gameId,
                extraEntity.entityId,
                card?.entityName,
                card?.cardId,
                extraEntity.cardId
            )
        }
        extraEntity.entityName = ""
        CardUtil.updateCardByExtraEntity(extraEntity, card)
    }

    private fun handleTagChangeNode(node: TagChangeNode) {
        val tagChangeEntity = node.toTagChangeEntity()
        if (tagChangeEntity.tag !== TagEnum.UNKNOWN) {
            if (tagChangeEntity.entity.isBlank()) {
                val card = war.cardMap[tagChangeEntity.entityId] ?: return

                tagChangeEntity.tag?.tagChangeHandler?.handle(card, tagChangeEntity, war, card.area.player, card.area)

                if (tagChangeEntity.entityName.isNotBlank() && club.xiaojiawei.hsscriptcardsdk.bean.Entity.isNotUnknownEntityName(tagChangeEntity.entityName)) {
                    card.entityName = tagChangeEntity.entityName
                }
            } else {
                val player = club.xiaojiawei.hsscript.bean.single.WarEx.getPlayerByGameId(tagChangeEntity.entity)
                if (player.isValid()) {
                    tagChangeEntity.tag?.tagChangeHandler?.handle(null, tagChangeEntity, war, player, null)
                } else {
                    tagChangeEntity.tag?.tagChangeHandler?.handle(tagChangeEntity)
                }
            }
        }
    }
}

fun EntityNode.toExtraEntity(): club.xiaojiawei.hsscript.bean.log.ExtraEntity {
    val extra = club.xiaojiawei.hsscript.bean.log.ExtraEntity()
    extra.entityId = entityId.toString()
    extra.cardId = cardId ?: ""
    info?.let {
        extra.entityName = it.entityName ?: ""
        extra.playerId = it.player?.toString() ?: ""
        extra.extraCard.zone = it.zone
        extra.extraCard.zonePos = it.zonePos ?: 0
        extra.extraCard.card.cardId = it.cardId ?: ""
    }
    tags.forEach { 
        it.tag.extraEntityHandler?.handle(extra, it.value)
    }
    return extra
}

fun TagChangeNode.toTagChangeEntity(): club.xiaojiawei.hsscript.bean.log.TagChangeEntity {
    val entity = club.xiaojiawei.hsscript.bean.log.TagChangeEntity()
    entity.tag = tag
    entity.value = value
    entity.entityId = entityId?.toString() ?: ""
    
    if (entityStr.startsWith("[")) {
        val info = RegexRepo.parseEntityStr(entityStr)
        info?.let {
            entity.entityName = it.entityName ?: ""
            entity.playerId = it.player?.toString() ?: ""
            entity.zone = it.zone
            entity.zonePos = it.zonePos ?: 0
            entity.cardId = it.cardId ?: ""
        }
    } else {
        entity.entity = entityStr
    }
    return entity
}

// -------------------- STREAMER --------------------

class PowerLogStreamer(
    private val file: File,
    private val parser: PowerLogParser
) {

    fun stream(onBlock: (BlockNode) -> Unit) {
        RandomAccessFile(file, "r").use { raf ->
            var pointer = 0L
            while (true) {
                val len = raf.length()
                if (len > pointer) {
                    raf.seek(pointer)
                    var line = raf.readLine()
                    while (line != null) {
                        parser.parse(String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8))
                        line = raf.readLine()
                    }
                    pointer = raf.filePointer
                    parser.roots.forEach { node ->
                        if (node is BlockNode) onBlock(node)
                    }
                    parser.roots.clear()
                }
                Thread.sleep(50)
            }
        }
    }
}

// -------------------- MAIN --------------------

fun main() {
    val parser = PowerLogParser()
    val executor = BlockExecutor()
    val file = File("S:\\IdeaProjects\\Hearthstone-Script\\power_logs\\renew_Power.log")
    val streamer = PowerLogStreamer(file, parser)

    streamer.stream {
        executor.execute(it)
    }
}
