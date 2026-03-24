package club.xiaojiawei.hsscript.bean

data class SearchCardTableColumnConfig(
    var no: Boolean = true,
    var cardId: Boolean = true,
    var name: Boolean = true,
    var attack: Boolean = true,
    var health: Boolean = true,
    var cost: Boolean = true,
    var text: Boolean = true,
    var type: Boolean = true,
    var cardSet: Boolean = true,
)

data class CardGroupTableColumnConfig(
    var no: Boolean = true,
    var cardId: Boolean = true,
    var name: Boolean = true,
    var effectType: Boolean = true,
    var cost: Boolean = true,
    var playAction: Boolean = true,
    var powerAction: Boolean = true,
    var weight: Boolean = true,
    var powerWeight: Boolean = true,
    var changeWeight: Boolean = true,
)
