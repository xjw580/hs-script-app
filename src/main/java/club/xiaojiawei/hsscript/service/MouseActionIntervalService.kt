package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscriptcardsdk.CardAction

/**
 * @author 肖嘉威
 * @date 2025/4/1 15:20
 */
object MouseActionIntervalService : Service<Int>() {

    override fun execStart(): Boolean {
        return true
    }

    override fun execStop(): Boolean {
        return true
    }

    override fun execIntelligentStartStop(value: Int?): Boolean {
        value?.let {
            CardAction.mouseActionInterval = it
        }
        return true
    }

    override fun execValueChanged(
        oldValue: Int,
        newValue: Int,
    ) {
        CardAction.mouseActionInterval = newValue
    }
}
