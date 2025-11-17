package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.enums.WindowEnum

/**
 * @author 肖嘉威
 * @date 2025/11/10 18:19
 */
class WindowConfig(
    var x: Int, var y: Int,
    var width: Int, var height: Int,
    val windowEnum: WindowEnum
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WindowConfig

        return windowEnum == other.windowEnum
    }

    override fun hashCode(): Int {
        return windowEnum.hashCode()
    }
}