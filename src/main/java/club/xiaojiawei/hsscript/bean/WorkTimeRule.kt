package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.enums.OperateEnum
import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.*
import java.util.*

/**
 * @author 肖嘉威
 * @date 2025/4/8 15:17
 */
class WorkTimeRule : Cloneable {

    @JsonIgnore
    private val id = UUID.randomUUID().toString()

    private val workTime: ObjectProperty<WorkTime> = SimpleObjectProperty<WorkTime>(WorkTime())

    private val operates: ObjectProperty<Set<OperateEnum>> =
        SimpleObjectProperty(emptySet())

    private val strategyId: StringProperty = SimpleStringProperty("e71234fa-1-radical-deck-97e9-1f4e126cd33b")

    private val enable: BooleanProperty = SimpleBooleanProperty(false)

    constructor()

    constructor(workTime: WorkTime?, operates: Set<OperateEnum>?, enable: Boolean) {
        workTime?.let {
            this.workTime.set(it)
        }
        operates?.let {
            this.operates.set(it)
        }
        this.enable.set(enable)
    }

    fun getWorkTime(): WorkTime {
        return workTime.get()
    }

    fun workTimeProperty(): ObjectProperty<WorkTime> {
        return workTime
    }

    fun setWorkTime(workTime: WorkTime) {
        this.workTime.set(workTime)
    }

    fun getOperate(): Set<OperateEnum> {
        return operates.get()
    }

    fun operateProperty(): ObjectProperty<Set<OperateEnum>> {
        return operates
    }

    fun setOperate(operates: Set<OperateEnum>) {
        this.operates.set(operates)
    }

    fun isEnable(): Boolean {
        return enable.get()
    }

    fun enableProperty(): BooleanProperty {
        return enable
    }

    fun setEnable(isEnable: Boolean) {
        this.enable.set(isEnable)
    }

    fun getStrategyId(): String? {
        return strategyId.get()
    }

    fun strategyIdProperty(): StringProperty {
        return strategyId
    }

    fun setStrategyId(strategyId: String?) {
        this.strategyId.set(strategyId)
    }

    public override fun clone(): WorkTimeRule {
        val clone = WorkTimeRule()
        clone.workTime.set(this.workTime.get().clone())
        clone.operates.set(this.operates.get().toSet())
        clone.enable.set(this.enable.get())
        return clone
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkTimeRule

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}
