package club.xiaojiawei.hsscript.bean

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.annotation.JSONField
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @author 肖嘉威
 * @date 2025/8/25 10:44
 */
data class GameTask(
    var gameTaskRes: GameTaskRes = GameTaskRes(),
    @field:JSONField(format = "yyyy-MM-dd")
    var refreshDailyTime: LocalDate = LocalDate.of(1990, 1, 1),
    @field:JSONField(format = "yyyy-MM-dd")
    var refreshWeeklyTime: LocalDate = LocalDate.of(1990, 1, 1),
    @field:JSONField(format = "yyyy-MM-dd HH:mm:ss")
    var updateTime: LocalDateTime = LocalDateTime.of(1990, 1, 1, 0, 0, 0),
)

data class GameTaskRes(
    var dailyTask: List<GameTaskBase> = emptyList(),// 每日任务
    var weeklyTask: List<GameTaskBase> = emptyList(),// 每周任务
)

data class GameTaskBase(
    var desc: String = "",// 描述
    var time: Long = System.currentTimeMillis(), // 时间戳（毫秒）
)