package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.enums.WindowEnum
import javafx.scene.Node

/**
 * 设置项信息，用于搜索功能
 * @author 肖嘉威
 * @date 2026/1/28
 */
data class SettingItem(
    /** 设置项名称，如"开机自启" */
    val name: String,
    /** 描述/提示文本 */
    val description: String = "",
    /** 所属选项卡 */
    val tabWindow: WindowEnum,
    /** 所属分组名称，如"系统"、"鼠标" */
    val groupName: String = "",
    /** 关联的 UI 节点，用于定位 */
    val node: Node? = null,
    /** 用于在高级设置中滚动定位的节点ID（兼容旧模式） */
    val scrollTargetId: String? = null
) {
    /**
     * 检查设置项是否匹配搜索关键词
     */
    fun matches(keyword: String): Boolean {
        if (keyword.isBlank()) return false
        val lowerKeyword = keyword.lowercase()
        return name.lowercase().contains(lowerKeyword) ||
                description.lowercase().contains(lowerKeyword) ||
                groupName.lowercase().contains(lowerKeyword)
    }
}
