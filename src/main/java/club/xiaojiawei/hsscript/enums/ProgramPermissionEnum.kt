package club.xiaojiawei.hsscript.enums

/**
 * @author 肖嘉威
 * @date 2026/1/27 14:37
 */
enum class ProgramPermissionEnum(val comment: String) {
    NORMAL("普通权限"),
    ADMINISTRATION("管理员权限"),
    NOT_RUNNING("未运行"),
    ;

    fun isAdministration(): Boolean {
        return this === ADMINISTRATION
    }

}