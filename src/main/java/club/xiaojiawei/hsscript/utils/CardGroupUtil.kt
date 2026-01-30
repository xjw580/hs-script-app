package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscript.bean.CardGroupInfo
import club.xiaojiawei.hsscript.consts.CARD_GROUP_DIR
import club.xiaojiawei.hsscript.consts.CARD_GROUP_FILE_EXT
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptcardsdk.bean.CardInfo
import club.xiaojiawei.hsscriptcardsdk.data.CARD_DATA_TRIE
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * 卡牌组管理工具类
 * @author 肖嘉威
 * @date 2026/1/30
 */
object CardGroupUtil {

    private val objectMapper = let {
        val it = ObjectMapper()
        it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//        it.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT)
        it
    }

    /**
     * 确保卡牌组目录存在
     */
    private fun ensureCardGroupDirExists() {
        val cardGroupDir = CARD_GROUP_DIR.toFile()
        if (!cardGroupDir.exists()) {
            cardGroupDir.mkdirs()
        }
    }

    /**
     * 从文件名获取卡牌组名称
     */
    private fun getCardGroupNameFromFile(file: java.io.File): String {
        return file.nameWithoutExtension
    }

    /**
     * 从卡牌组名称获取文件路径
     */
    private fun getCardGroupFilePath(cardGroupName: String): Path {
        return Path.of(CARD_GROUP_DIR.toString(), "${cardGroupName}.${CARD_GROUP_FILE_EXT}")
    }

    /**
     * 加载所有卡牌组
     */
    fun loadAllCardGroups(): List<CardGroupInfo> {
        ensureCardGroupDirExists()
        val cardGroupDir = CARD_GROUP_DIR.toFile()
        val cardGroupFiles = cardGroupDir.listFiles { file -> file.extension == CARD_GROUP_FILE_EXT } ?: return emptyList()

        return cardGroupFiles.mapNotNull { file ->
            try {
                val cardGroupInfo = objectMapper.readValue(file, CardGroupInfo::class.java)
                // 确保卡牌组名与文件名一致
                cardGroupInfo.name = getCardGroupNameFromFile(file)
                cardGroupInfo
            } catch (e: IOException) {
                log.error(e) { "读取卡牌组文件异常: ${file.name}" }
                null
            }
        }
    }

    /**
     * 保存卡牌组到文件
     */
    fun saveCardGroup(cardGroupInfo: CardGroupInfo) {
        ensureCardGroupDirExists()
        val cardGroupPath = getCardGroupFilePath(cardGroupInfo.name)
        val file = cardGroupPath.toFile()

        try {
            FileChannel.open(
                cardGroupPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { fileChannel ->
                val buffer = ByteBuffer.wrap(objectMapper.writeValueAsBytes(cardGroupInfo))
                fileChannel.write(buffer)
            }
        } catch (e: IOException) {
            log.error(e) { "保存卡牌组文件异常: ${file.name}" }
        }
    }

    /**
     * 删除卡牌组文件
     */
    fun deleteCardGroup(cardGroupName: String): Boolean {
        val cardGroupPath = getCardGroupFilePath(cardGroupName)
        return try {
            Files.deleteIfExists(cardGroupPath)
        } catch (e: IOException) {
            log.error(e) { "删除卡牌组文件异常: $cardGroupName" }
            false
        }
    }

    /**
     * 重命名卡牌组
     */
    fun renameCardGroup(oldName: String, newName: String): Boolean {
        val oldPath = getCardGroupFilePath(oldName)
        val newPath = getCardGroupFilePath(newName)

        if (!oldPath.toFile().exists()) {
            return false
        }
        if (newPath.toFile().exists()) {
            return false
        }

        return try {
            Files.move(oldPath, newPath)
            true
        } catch (e: IOException) {
            log.error(e) { "重命名卡牌组文件异常: $oldName -> $newName" }
            false
        }
    }

    /**
     * 导出卡牌组到指定路径
     */
    fun exportCardGroup(cardGroupInfo: CardGroupInfo, targetPath: Path) {
        try {
            FileChannel.open(
                targetPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { fileChannel ->
                val buffer = ByteBuffer.wrap(objectMapper.writeValueAsBytes(cardGroupInfo))
                fileChannel.write(buffer)
            }
        } catch (e: IOException) {
            log.error(e) { "导出卡牌组文件异常: ${targetPath}" }
        }
    }

    /**
     * 从指定路径导入卡牌组
     */
    fun importCardGroup(sourcePath: Path): CardGroupInfo? {
        val file = sourcePath.toFile()
        if (!file.exists()) return null

        return try {
            objectMapper.readValue(file, CardGroupInfo::class.java)
        } catch (e: IOException) {
            log.error(e) { "导入卡牌组文件异常: ${sourcePath}" }
            null
        }
    }

    /**
     * 应用启用的卡牌组到系统
     * 将所有启用的卡牌组中的卡牌信息和权重加载到系统中
     */
    fun applyEnabledCardGroups() {
        val allCardGroups = loadAllCardGroups()
        val enabledCardGroups = allCardGroups.filter { it.enabled }

        // 清空现有数据
        CARD_DATA_TRIE.clear()

        // 应用启用的卡牌组
        for (cardGroup in enabledCardGroups) {
            for (card in cardGroup.cards) {
                CARD_DATA_TRIE[card.cardId] = CardInfo(
                    card.effectType, card.playActions, card.powerActions,
                    card.weight, card.powerWeight, card.changeWeight
                )
            }
        }

        log.info { "已应用 ${enabledCardGroups.size} 个卡牌组，共 ${enabledCardGroups.sumOf { it.cards.size }} 张卡牌配置" }
    }

    /**
     * 检查卡牌组名称是否有效（不包含非法字符）
     */
    fun isValidCardGroupName(name: String): Boolean {
        if (name.isBlank()) return false
        val invalidChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return invalidChars.none { name.contains(it) }
    }
}
