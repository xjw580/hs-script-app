package club.xiaojiawei.hsscript.listener

import club.xiaojiawei.hsscript.bean.WorkTimeRule
import club.xiaojiawei.hsscript.bean.single.WarEx
import club.xiaojiawei.hsscript.enums.WindowEnum
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.status.WorkTimeStatus
import club.xiaojiawei.hsscript.utils.SystemUtil
import club.xiaojiawei.hsscript.utils.WindowUtil
import club.xiaojiawei.hsscript.utils.go
import club.xiaojiawei.hsscript.utils.runUI
import club.xiaojiawei.hsscriptbase.bean.LRunnable
import club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import club.xiaojiawei.hsscriptbase.util.isFalse
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ChangeListener
import javafx.stage.Stage
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * 工作状态
 *
 * @author 肖嘉威
 * @date 2023/9/10 22:04
 */
object WorkTimeListener {
    private var checkWorkTask: ScheduledFuture<*>? = null

    val launch: Unit by lazy {
        checkWorkTask =
            EXTRA_THREAD_POOL.scheduleWithFixedDelay(
                LRunnable {
                    checkWork()
                    tryWork()
                },
                0,
                30,
                TimeUnit.SECONDS,
            )
        WarEx.inWarProperty.addListener { _, _, newValue ->
            if (!newValue && PauseStatus.isStart) {
                checkWork()
                if (cannotWork()) {
                    cannotWorkLog()
                    workingProperty.set(false)
                    execOperate(prevClosestWorkTimeRule)
                }
            }
        }
        log.info { "工作时段监听已启动" }
    }

    /**
     * 执行工作时间段结束后的操作
     */
    private fun execOperate(workTimeRule: WorkTimeRule?) {
        val operates = workTimeRule?.operates ?: return

        val alert: AtomicReference<Stage?> = AtomicReference<Stage?>()
        val countdownTime = 10
        val future =
            go {
                for (i in 0 until countdownTime) {
                    if (PauseStatus.isStart) {
                        Thread.sleep(1000)
                    } else {
                        break
                    }
                }
                runUI {
                    alert.get()?.hide()
                }
                for (operate in operates) {
                    if (PauseStatus.isStart) {
                        operate.exec().isFalse {
                            log.error {
                                operate.value + "执行失败"
                            }
                        }
                    } else {
                        return@go
                    }
                }
            }
        val operationName = operates.map { it.value }
        val text = "${countdownTime}秒后执行：$operationName"
        log.info { "工作时间段结束，$text" }
        runUI {
            alert.set(
                WindowUtil
                    .createAlert(
                        text,
                        null,
                        {
                            future.cancel(true)
                            runUI {
                                alert.get()?.hide()
                            }
                        },
                        null,
                        WindowUtil.getStage(WindowEnum.MAIN),
                        "阻止",
                    ).apply {
                        show()
                    },
            )
        }
    }

    var isDuringWorkDate = false

    /**
     * 是否处于工作中
     */
    private val workingProperty = SimpleBooleanProperty(false)

    /**
     * 当前工作时间规则
     */
    private var currentWorkTimeRule: WorkTimeRule? = null

    var closestWorkTimeRule: WorkTimeRule? = null
        private set

    var working: Boolean
        get() {
            return workingProperty.get()
        }
        set(value) {
            workingProperty.set(value)
        }

    fun addChangeListener(listener: ChangeListener<Boolean>) {
        workingProperty.addListener(listener)
    }

    fun removeChangeListener(listener: ChangeListener<Boolean>) {
        workingProperty.removeListener(listener)
    }

    fun canWork(): Boolean = isDuringWorkDate

    fun cannotWork(): Boolean = !isDuringWorkDate

    fun tryWork() {
        if (canWork() && PauseStatus.isStart) {
            workingProperty.set(true)
        }
    }

    /**
     * 获取当前的工作时间规则
     * @return 如果当前处于工作时间内，返回对应的WorkTimeRule；否则返回null
     */
    fun getCurrentWorkTimeRule(): WorkTimeRule? {
        return if (isDuringWorkDate) currentWorkTimeRule else null
    }

    /**
     * 获取当前生效的工作时间规则（不管是否处于工作时间内）
     * @return 返回当前时间段对应的WorkTimeRule，如果没有找到则返回null
     */
    fun getActiveWorkTimeRule(): WorkTimeRule? {
        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        val dayIndex = LocalDate.now().dayOfWeek.value - 1
        if (dayIndex >= readOnlyWorkTimeSetting.size) return null

        val id = readOnlyWorkTimeSetting[dayIndex]
        return WorkTimeStatus.readOnlyWorkTimeRuleSet().toList().find { it.id == id }?.let { ruleSet ->
            val timeRules = ruleSet.getTimeRules().toList()
            val nowTime = LocalTime.now()

            // 寻找当前时间所在的工作时间段
            timeRules.find { rule ->
                if (!rule.enable) return@find false
                val workTime = rule.workTime
                val startTime = workTime.parseStartTime()?.withSecond(0) ?: return@find false
                val endTime = workTime.parseEndTime()?.withSecond(59) ?: return@find false
                nowTime in startTime..endTime
            }
        }
    }

    /**
     * 获取今天所有启用的工作时间规则
     * @return 返回今天所有启用的WorkTimeRule列表
     */
    fun getTodayWorkTimeRules(): List<WorkTimeRule> {
        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        val dayIndex = LocalDate.now().dayOfWeek.value - 1
        if (dayIndex >= readOnlyWorkTimeSetting.size) return emptyList()

        val id = readOnlyWorkTimeSetting[dayIndex]
        return WorkTimeStatus.readOnlyWorkTimeRuleSet().toList().find { it.id == id }?.let { ruleSet ->
            ruleSet.getTimeRules().filter { it.enable }
        } ?: emptyList()
    }

    /**
     * 检查是否有紧接着的下一个工作时间段
     * @param currentEndTime 当前工作时间段的结束时间
     * @return true如果有紧接着的工作时间段，false如果没有
     */
    private fun hasImmediateNextWorkPeriod(currentEndTime: LocalTime): Boolean {
        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        val dayIndex = LocalDate.now().dayOfWeek.value - 1
        if (dayIndex >= readOnlyWorkTimeSetting.size) return false

        val id = readOnlyWorkTimeSetting[dayIndex]
        return WorkTimeStatus.readOnlyWorkTimeRuleSet().toList().find { it.id == id }?.let { ruleSet ->
            val timeRules = ruleSet.getTimeRules().filter { it.enable }

            // 检查是否有在当前结束时间后立即开始的工作时间段
            timeRules.any { rule ->
                val workTime = rule.workTime
                val startTime = workTime.parseStartTime()?.withSecond(0) ?: return@any false
                // 允许少量时间间隔（比如1分钟内）认为是连续的
                val timeDiff = startTime.toSecondOfDay() - currentEndTime.toSecondOfDay()
                timeDiff in 0..60 // 60秒内的间隔认为是连续的
            }
        } ?: false
    }

    @Synchronized
    fun checkWork() {
        var canWork = false
        var closestWorkTimeRule: WorkTimeRule? = null

        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        val dayIndex = LocalDate.now().dayOfWeek.value - 1
        if (dayIndex >= readOnlyWorkTimeSetting.size) {
            isDuringWorkDate = false
            currentWorkTimeRule = null
            prevClosestWorkTimeRule = null
            return
        }

        val id = readOnlyWorkTimeSetting[dayIndex]
        WorkTimeStatus.readOnlyWorkTimeRuleSet().toList().find { it.id == id }?.let { ruleSet ->
            val timeRules = ruleSet.getTimeRules().filter { it.enable } // 只处理启用的规则
            val nowTime = LocalTime.now()
            val nowSecondOfDay = nowTime.toSecondOfDay()

            var minDiffSec: Int = Int.MAX_VALUE

            // 重置当前工作时间规则
            currentWorkTimeRule = null

            for (rule in timeRules) {
                val workTime = rule.workTime
                val startTime = workTime.parseStartTime()?.withSecond(0) ?: continue
                val endTime = workTime.parseEndTime()?.withSecond(59) ?: continue

                // 检查时间有效性
                if (startTime > endTime) {
                    log.warn { "工作时间规则无效：开始时间 $startTime 晚于结束时间 $endTime" }
                    continue
                }

                if (nowTime in startTime..endTime) {
                    canWork = true
                    closestWorkTimeRule = rule
                    currentWorkTimeRule = rule // 设置当前工作时间规则
                    this.closestWorkTimeRule = rule
                    break
                } else {
                    // 找出最近刚结束的工作时间段（用于执行收尾操作）
                    val diffSec = nowSecondOfDay - endTime.toSecondOfDay()
                    if (diffSec in 1 until minDiffSec) {
                        minDiffSec = diffSec
                        closestWorkTimeRule = rule
                    }
                }
            }
        }

        isDuringWorkDate = canWork
        prevClosestWorkTimeRule = closestWorkTimeRule

        // 调试日志
        if (!canWork && prevClosestWorkTimeRule != null) {
            log.debug { "当前不在工作时间，最近结束的工作时间段：${prevClosestWorkTimeRule?.workTime}" }
        }
    }

    var prevClosestWorkTimeRule: WorkTimeRule? = null
        private set

    fun cannotWorkLog() {
        val context = "现在是下班时间 🌜"
        SystemUtil.notice(context)
        log.info { context }
    }

    /**
     * 获取下一次可工作的时间
     * @return 距离下一次工作时间的秒数，如果没有找到返回-1L，如果当前正在工作返回0L
     */
    fun getSecondsUntilNextWorkPeriod(): Long {
        if (working) return 0L

        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        val currentDayIndex = LocalDate.now().dayOfWeek.value - 1
        if (currentDayIndex >= readOnlyWorkTimeSetting.size) return -1L

        // 先检查今天剩余的工作时间
        val todaySeconds = getSecondsUntilNextWorkPeriodForDay(currentDayIndex, 0)
        if (todaySeconds > 0) return todaySeconds

        // 检查后续几天的工作时间
        val totalDays = readOnlyWorkTimeSetting.size
        for (dayOffset in 1 until totalDays) {
            val dayIndex = (currentDayIndex + dayOffset) % totalDays
            val seconds = getSecondsUntilNextWorkPeriodForDay(dayIndex, dayOffset)
            if (seconds > 0) return seconds
        }

        return -1L
    }

    /**
     * 获取指定天的下一个工作时间段开始的秒数
     * @param dayIndex 星期索引 (0-6，0为周一)
     * @param dayOffset 天数偏移量 (0为今天，1为明天，以此类推)
     * @return 距离该天最近工作时间开始的秒数，如果没有找到返回-1L
     */
    private fun getSecondsUntilNextWorkPeriodForDay(dayIndex: Int, dayOffset: Int): Long {
        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        if (dayIndex >= readOnlyWorkTimeSetting.size) return -1L

        val id = readOnlyWorkTimeSetting[dayIndex]
        return WorkTimeStatus.readOnlyWorkTimeRuleSet().toList().find { it.id == id }?.let { ruleSet ->
            val timeRules = ruleSet.getTimeRules().filter { it.enable }
            val nowTime = LocalTime.now()
            val nowSecondOfDay = nowTime.toSecondOfDay()

            var minDiffSec: Long = Long.MAX_VALUE

            for (rule in timeRules) {
                val workTime = rule.workTime
                val startTime = workTime.parseStartTime() ?: continue
                val startSecondOfDay = startTime.toSecondOfDay().toLong()

                val diffSec: Long = if (dayOffset == 0) {
                    // 今天：只考虑未来的时间
                    startSecondOfDay - nowSecondOfDay
                } else {
                    // 其他天：加上天数偏移的秒数
                    startSecondOfDay + dayOffset * 24 * 3600L - nowSecondOfDay
                }

                if (diffSec > 0 && diffSec < minDiffSec) {
                    minDiffSec = diffSec
                }
            }

            if (minDiffSec == Long.MAX_VALUE) -1L else minDiffSec
        } ?: -1L
    }

    /**
     * 获取下一个工作时间段的详细信息
     * @return Pair<WorkTimeRule?, Long> - 工作规则和距离开始的秒数
     */
    fun getNextWorkPeriodInfo(): Pair<WorkTimeRule?, Long> {
        if (working) return Pair(currentWorkTimeRule, 0L)

        val readOnlyWorkTimeSetting = WorkTimeStatus.readOnlyWorkTimeSetting()
        val currentDayIndex = LocalDate.now().dayOfWeek.value - 1
        if (currentDayIndex >= readOnlyWorkTimeSetting.size) return Pair(null, -1L)

        val nowTime = LocalTime.now()
        val nowSecondOfDay = nowTime.toSecondOfDay()

        var nearestRule: WorkTimeRule? = null
        var nearestSeconds: Long = Long.MAX_VALUE

        // 检查所有天的工作时间
        val totalDays = readOnlyWorkTimeSetting.size
        for (dayOffset in 0 until totalDays) {
            val dayIndex = (currentDayIndex + dayOffset) % totalDays
            val id = readOnlyWorkTimeSetting[dayIndex]

            WorkTimeStatus.readOnlyWorkTimeRuleSet().toList().find { it.id == id }?.let { ruleSet ->
                val timeRules = ruleSet.getTimeRules().filter { it.enable }

                for (rule in timeRules) {
                    val workTime = rule.workTime
                    val startTime = workTime.parseStartTime() ?: continue
                    val startSecondOfDay = startTime.toSecondOfDay().toLong()

                    val diffSec: Long = if (dayOffset == 0) {
                        startSecondOfDay - nowSecondOfDay
                    } else {
                        startSecondOfDay + dayOffset * 24 * 3600L - nowSecondOfDay
                    }

                    if (diffSec > 0 && diffSec < nearestSeconds) {
                        nearestSeconds = diffSec
                        nearestRule = rule
                    }
                }
            }
        }

        return if (nearestSeconds == Long.MAX_VALUE) {
            Pair(null, -1L)
        } else {
            Pair(nearestRule, nearestSeconds)
        }
    }
}