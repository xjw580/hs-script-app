package club.xiaojiawei.hsscript.bean.log

import club.xiaojiawei.hsscriptcardsdk.bean.BaseCard
import club.xiaojiawei.hsscriptcardsdk.enums.ZoneEnum

/**
 * @author 肖嘉威
 * @date 2022/11/30 12:33
 */
class ExtraCard {

    val card: BaseCard = BaseCard()

    var zone: ZoneEnum? = null

    var zonePos = 0

    var controllerPlayerId: String = ""

}
