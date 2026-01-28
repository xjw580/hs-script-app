package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.bean.SettingItem

/**
 * 设置搜索服务，各 Controller 通过 register 方法注册设置项
 * @author 肖嘉威
 * @date 2026/1/28
 */
object SettingsSearchService {

    private val allSettings = mutableListOf<SettingItem>()

    /**
     * 注册设置项（由各 Controller 调用）
     */
    fun register(vararg items: SettingItem) {
        allSettings.addAll(items)
    }

    /**
     * 注册设置项列表
     */
    fun register(items: List<SettingItem>) {
        allSettings.addAll(items)
    }

    /**
     * 搜索设置项
     * @param keyword 搜索关键词
     * @return 匹配的设置项列表
     */
    fun search(keyword: String): List<SettingItem> {
        if (keyword.isBlank()) return emptyList()
        return allSettings.filter { it.matches(keyword) }
    }

    /**
     * 清除所有注册的设置项（用于重新加载）
     */
    fun clear() {
        allSettings.clear()
    }
}
