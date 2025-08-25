package club.xiaojiawei.hsscript.statistics

import club.xiaojiawei.hsscript.consts.DATA_DIR
import club.xiaojiawei.hsscript.consts.STATISTICS_DB_NAME
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * @author 肖嘉威
 * @date 2025/3/14 0:40
 */
object RecordDaoEx {

    val RECORD_DAO: RecordDao by lazy {
        RecordDao(DATA_DIR.resolve(STATISTICS_DB_NAME).toString())
    }

    fun queryRecord(startDate: LocalDate, endDate: LocalDate): List<Record> {
        val recordDao = RECORD_DAO
        val minDateTime = LocalDateTime.of(startDate.year, startDate.monthValue, startDate.dayOfMonth, 0, 0)
        val maxDateTime = LocalDateTime.of(endDate.year, endDate.monthValue, endDate.dayOfMonth, 0, 0).plusDays(1)
        val records = recordDao.queryByDateRange(minDateTime, maxDateTime).filter {
            val endTime = it.endTime ?: return@filter false
            endTime.isAfter(minDateTime) && endTime.isBefore(maxDateTime)
        }
        return records
    }

}