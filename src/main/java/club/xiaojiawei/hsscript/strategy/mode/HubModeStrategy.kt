package club.xiaojiawei.hsscript.strategy.mode

import club.xiaojiawei.hsscript.bean.*
import club.xiaojiawei.hsscript.consts.CHI_SIM_DATA
import club.xiaojiawei.hsscript.consts.GAME_CN_NAME
import club.xiaojiawei.hsscript.consts.GameRationConst
import club.xiaojiawei.hsscript.consts.SCREEN_SCALE
import club.xiaojiawei.hsscript.consts.TESS_DATA_PATH
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.enums.SCREEN_WIDTH
import club.xiaojiawei.hsscript.starter.InjectStarter
import club.xiaojiawei.hsscript.statistics.RecordDaoEx
import club.xiaojiawei.hsscript.status.DeckStrategyManager
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.status.ScriptStatus
import club.xiaojiawei.hsscript.strategy.AbstractModeStrategy
import club.xiaojiawei.hsscript.utils.*
import club.xiaojiawei.hsscriptbase.config.EXTRA_THREAD_POOL
import club.xiaojiawei.hsscriptbase.config.log
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser.SWP_NOZORDER
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 主界面
 * @author 肖嘉威
 * @date 2022/11/25 12:36
 */
object HubModeStrategy : AbstractModeStrategy<Any?>() {

    /**
     * 广告弹窗关闭按钮
     */
    val CLOSE_AD1_RECT = GameRect(-0.0364, 0.0493, 0.2764, 0.3255)

    /**
     * 月初结算弹窗关闭按钮
     */
    val CLOSE_SETTLEMENT_RECT by lazy {
        GameRect(-0.0498, 0.0534, 0.2944, 0.3424);
    }

    /**
     * 月初结算弹窗宝箱
     */
    val CHEST_RECT by lazy {
        GameRect(-0.0771, 0.0858, -0.0368, 0.1576);
    }

    /**
     * 结算完成按钮
     */
    val CONFIRM_SETTLEMENT_RECT by lazy {
        GameRect(-0.0668, 0.0709, -0.0007, 0.0611);
    }

    /**
     * 未领取的奖励弹窗关闭按钮
     */
    val UNCLAIMED_REWARDS_RECT = GameRect(-0.0449, 0.0406, 0.1834, 0.2342)

    /**
     * 任务按钮
     */
    val TASK_RECT by lazy {
        GameRect(-0.3379, -0.2762, 0.3500, 0.4190)
    }

    /**
     * 任务标题栏
     */
    val TASK_TITLE_RECT by lazy {
        GameRect(-0.0500, 0.1563, -0.3637, -0.3348)
    }

    val DAILY_TASK_REFRESH_RECTS by lazy {
        arrayOf(
            GameRect(-0.1785, -0.1541, -0.2516, -0.2206),
            GameRect(-0.0003, 0.0247, -0.2516, -0.2225),
            GameRect(0.1729, 0.1959, -0.2479, -0.2168),
        )
    }

    val WEEKLY_TASK_REFRESH_RECTS by lazy {
        arrayOf(
            GameRect(-0.1778, -0.1534, 0.0522, 0.0833),
            GameRect(0.0003, 0.0233, 0.0513, 0.0795),
            GameRect(0.1799, 0.1987, 0.0522, 0.0804),
        )
    }

    val HIDE_TASK_RECT = GameRect(-0.0235, 0.1480, -0.4515, -0.4076)

    override fun wantEnter() {
    }

    private fun ocrTask(frameReader: FrameReader): GameTaskRes? {
        log.info { "开始识别${GAME_CN_NAME}任务" }
        val instance = TesseractEx().apply {
            setDatapath(TESS_DATA_PATH)
            setLanguage(CHI_SIM_DATA)
        }
        val dailyTask = mutableListOf<GameTaskBase>()
        val weeklyTask = mutableListOf<GameTaskBase>()

        var frame: FrameData? = null
        for (i in 0 until 10) {
            frame = frameReader.tryReadFrame()
            if (frame != null) break
            Thread.sleep(100)
        }

        frame ?: let {
            log.error { "游戏帧获取失败" }
            return null
        }

        val fullImage = frame.buffer.toBufferedImage(frame.width, frame.height)
        val taskTitleImg = fullImage.cropImage(TASK_TITLE_RECT)

        var result = ""
        for (i in 0 until 3) {
            result = instance.doOCR(taskTitleImg, "taskTitle").replace("\\s".toRegex(), "")
            if (result.isNotBlank()) break
        }
        if (result.isBlank()) {
            log.warn { "识别任务失败" }
            return null
        }

        if (result.isNotBlank()) {
            for (rect in GameUtil.DAILY_TASK_DESC_RECTS) {
                result = instance.doOCR(fullImage.cropImage(rect), "dailyDesc")
                    .replace("\\s".toRegex(), "")
                if (result.length > 5) {
                    dailyTask.add(GameTaskBase(result))
                }
            }
            log.info { "每日任务: \n" + dailyTask.joinToString("\n") { it.desc } }
            for (rect in GameUtil.WEEKLY_TASK_DESC_RECTS) {
                result = instance.doOCR(fullImage.cropImage(rect), "weeklyDesc")
                    .replace("\\s".toRegex(), "")
                if (result.length > 5) {
                    weeklyTask.add(GameTaskBase(result))
                }
            }
            log.info { "每周任务: \n" + weeklyTask.joinToString("\n") { it.desc } }
            log.info { "${GAME_CN_NAME}任务识别完毕" }
            return GameTaskRes(dailyTask, weeklyTask)
        }
        return null
    }

    private fun handleTask() {
        if (File(TESS_DATA_PATH).listFiles().isEmpty()) {
            log.warn { "tess数据集文件不存在，无法使用自动刷新任务功能" }
            return
        }
        val gameTask = ConfigExUtil.getGameTask()
        val canRefreshDailyTask = gameTask.refreshDailyTime.isBefore(LocalDate.now())
        val canRefreshWeeklyTask = gameTask.refreshWeeklyTime.isBefore(LocalDate.now())
        if (!canRefreshDailyTask && !canRefreshWeeklyTask) return

        val day = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0)
        val latestDay = if (day > gameTask.updateTime) {
            day
        } else gameTask.updateTime

        val records =
            RecordDaoEx.RECORD_DAO.queryByDateRange(latestDay, LocalDateTime.now().plusDays(1))
        val minWarCount = 5
        if (records.size < minWarCount) {
            log.info { "对局数小于${minWarCount}把，放弃识别任务" }
            return
        }

        log.info { "准备识别${GAME_CN_NAME}任务" }

        InjectStarter().start()

        val oldWidth = ScriptStatus.GAME_RECT.right - ScriptStatus.GAME_RECT.left
        val oldHeight = ScriptStatus.GAME_RECT.bottom - ScriptStatus.GAME_RECT.top
        val width = SCREEN_WIDTH.toInt() * SCREEN_SCALE
        val middleRation =
            (GameRationConst.GAME_WINDOW_MIN_WIDTH_HEIGHT_RATIO + GameRationConst.GAME_WINDOW_MAX_WIDTH_HEIGHT_RATIO) / 2
        val height = (width.toDouble() / middleRation).toInt()

        val gameWindowRECT = WinDef.RECT()
        User32.INSTANCE.GetWindowRect(ScriptStatus.gameHWND, gameWindowRECT)

        log.info { "调整${GAME_CN_NAME}窗口大小" }
        User32.INSTANCE.SetWindowPos(
            ScriptStatus.gameHWND,
            null,
            0,
            0,
            width.toInt(),
            height.toInt(),
            SWP_NOZORDER
        )
        Thread.sleep(2000L)
        GameUtil.updateGameRect()
        val frameReader = FrameReader().apply {
            initialize()
        }

        try {
            HIDE_TASK_RECT.lClick(false)
            Thread.sleep(2000L)
            log.info { "进入任务界面" }
            TASK_RECT.lClick(false)
            Thread.sleep(4000L)

            var currGameTaskRes = ocrTask(frameReader) ?: return

            val newDailyTask = mutableListOf<GameTaskBase>()
            for (taskBase in currGameTaskRes.dailyTask) {
                gameTask.gameTaskRes.dailyTask.find { it.desc == taskBase.desc }?.let {
                    newDailyTask.add(it)
                } ?: let {
                    newDailyTask.add(taskBase)
                }
            }
            val newWeeklyTask = mutableListOf<GameTaskBase>()
            for (taskBase in currGameTaskRes.weeklyTask) {
                gameTask.gameTaskRes.weeklyTask.find { it.desc == taskBase.desc }?.let {
                    newWeeklyTask.add(it)
                } ?: let {
                    newWeeklyTask.add(taskBase)
                }
            }
            var dailyIndex = -1
            var weeklyIndex = -1
            if (canRefreshDailyTask) {
                var minTime = Long.MAX_VALUE

                for ((index, taskBase) in newDailyTask.withIndex()) {
                    if (taskBase.time < minTime) {
                        minTime = taskBase.time
                        dailyIndex = index
                    }
                }
                if (dailyIndex != -1) {
                    DAILY_TASK_REFRESH_RECTS[dailyIndex].lClick(false)
                    val desc = newDailyTask[dailyIndex].desc
                    log.info { "刷新每日任务[${dailyIndex + 1}]: $desc" }
                    gameTask.refreshDailyTime = LocalDate.now()
                }
            }
            gameTask.updateTime = LocalDateTime.now()

            if (canRefreshWeeklyTask) {
                var minTime = Long.MAX_VALUE
                for ((index, taskBase) in newWeeklyTask.withIndex()) {
                    if (taskBase.time < minTime) {
                        minTime = taskBase.time
                        weeklyIndex = index
                    }
                }
                if (weeklyIndex != -1) {
                    WEEKLY_TASK_REFRESH_RECTS[weeklyIndex].lClick(false)
                    val desc = newWeeklyTask[weeklyIndex].desc
                    log.info { "刷新每周任务[${weeklyIndex + 1}]: $desc" }
                    gameTask.refreshWeeklyTime = LocalDate.now()
                }
            }

            Thread.sleep(1000L)
            currGameTaskRes = ocrTask(frameReader) ?: return
            currGameTaskRes.dailyTask.getOrNull(dailyIndex)?.let {
                newDailyTask.getOrNull(dailyIndex)?.desc = it.desc
                newDailyTask.getOrNull(dailyIndex)?.time = System.currentTimeMillis()
            }
            currGameTaskRes.weeklyTask.getOrNull(weeklyIndex)?.let {
                newWeeklyTask.getOrNull(weeklyIndex)?.desc = it.desc
                newWeeklyTask.getOrNull(weeklyIndex)?.time = System.currentTimeMillis()
            }

            gameTask.gameTaskRes.dailyTask = newDailyTask
            gameTask.gameTaskRes.weeklyTask = newWeeklyTask
            gameTask.updateTime = LocalDateTime.now()
        } catch (e: Exception) {
            log.error(e) {}
        } finally {
            frameReader.close()

            User32.INSTANCE.SetWindowPos(
                ScriptStatus.gameHWND,
                null,
                gameWindowRECT.left,
                gameWindowRECT.top,
                oldWidth,
                oldHeight,
                SWP_NOZORDER
            )

            for (i in 0 until 2) {
                Thread.sleep(2000L)
                GameUtil.updateGameRect()
                HIDE_TASK_RECT.lClick(false)
            }
            ConfigExUtil.storeGameTask(gameTask)
        }
    }

    override fun afterEnter(t: Any?) {
        if (ConfigUtil.getBoolean(ConfigEnum.AUTO_REFRESH_GAME_TASK)) {
            handleTask()
        }

        addEnteredTask(EXTRA_THREAD_POOL.scheduleWithFixedDelay({
            if (PauseStatus.isPause) return@scheduleWithFixedDelay
            log.info { "点击广告弹窗等" }
            CLOSE_AD1_RECT.lClick()
            SystemUtil.delayShortMedium()
            CLOSE_SETTLEMENT_RECT.lClick()
            SystemUtil.delayShortMedium()
            UNCLAIMED_REWARDS_RECT.lClick()
            SystemUtil.delayShortMedium()
        }, 5, 2, TimeUnit.SECONDS))

        val runMode = DeckStrategyManager.currentRunMode
        if (runMode != null) {
            if (!runMode.isEnable) {
                log.warn { "${runMode.comment}未启用" }
                PauseStatus.isPause = false
                return
            }
            log.info { "准备进入指定模式" }
            runMode.modeEnum.modeStrategy?.wantEnter()
        }
    }

}