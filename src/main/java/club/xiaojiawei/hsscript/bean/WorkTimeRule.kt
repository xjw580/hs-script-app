package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.enums.OperateEnum
import club.xiaojiawei.hsscript.utils.getValue
import club.xiaojiawei.hsscript.utils.setValue
import club.xiaojiawei.hsscriptbase.enums.RunModeEnum
import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.*
import java.util.*

/**
 * @author 肖嘉威
 * @date 2025/4/8 15:17
 */
class WorkTimeRule : Cloneable {

    private val id = UUID.randomUUID().toString()

    @JsonIgnore
    val workTimeProperty: ObjectProperty<WorkTime> = SimpleObjectProperty(WorkTime())
    var workTime: WorkTime by workTimeProperty

    @JsonIgnore
    val operatesProperty: ObjectProperty<Set<OperateEnum>> = SimpleObjectProperty(emptySet())
    var operates: Set<OperateEnum> by operatesProperty

    @JsonIgnore
    val runModeProperty: ObjectProperty<RunModeEnum> = SimpleObjectProperty(RunModeEnum.STANDARD)
    var runMode: RunModeEnum by runModeProperty

    @JsonIgnore
    val strategyIdProperty: StringProperty = SimpleStringProperty("")
    var strategyId: String by strategyIdProperty

    @JsonIgnore
    val deckPosProperty: ObjectProperty<Set<Int>> = SimpleObjectProperty(emptySet())
    var deckPos: Set<Int> by deckPosProperty

    @JsonIgnore
    val enableProperty: BooleanProperty = SimpleBooleanProperty(false)
    var enable: Boolean by enableProperty

    constructor()

    constructor(
        workTime: WorkTime,
        operates: Set<OperateEnum>,
        runMode: RunModeEnum,
        strategyId: String,
        deckPos: Set<Int>,
        enable: Boolean
    ) {
        this.workTime = workTime
        this.operates = operates
        this.runMode = runMode
        this.strategyId = strategyId
        this.deckPos = deckPos
        this.enable = enable
    }

    public override fun clone(): WorkTimeRule {
        val clone = WorkTimeRule()
        clone.workTime = this.workTime.clone()
        clone.operates = this.operates.toSet()
        clone.runMode = this.runMode
        clone.strategyId = this.strategyId
        clone.deckPos = this.deckPos.toSet()
        clone.enable = this.enable
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
