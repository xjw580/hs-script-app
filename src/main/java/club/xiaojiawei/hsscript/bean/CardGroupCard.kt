package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscriptcardsdk.enums.CardActionEnum
import club.xiaojiawei.hsscriptcardsdk.enums.CardEffectTypeEnum
import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty

/**
 * 卡牌组中的卡牌数据类，合并了 InfoCard 和 WeightCard 的功能
 * @author 肖嘉威
 * @date 2026/1/30
 */
class CardGroupCard : Cloneable {

    @JsonIgnore
    val cardIdProperty = SimpleStringProperty("")

    @JsonIgnore
    val dbfIdProperty = SimpleIntegerProperty(0)

    @JsonIgnore
    val nameProperty = SimpleStringProperty("")

    @JsonIgnore
    val effectTypeProperty = SimpleObjectProperty(CardEffectTypeEnum.UNKNOWN)

    @JsonIgnore
    val playActionsProperty = SimpleObjectProperty(emptyList<CardActionEnum>())

    @JsonIgnore
    val powerActionsProperty = SimpleObjectProperty(emptyList<CardActionEnum>())

    @JsonIgnore
    val weightProperty = SimpleDoubleProperty(1.0)

    @JsonIgnore
    val powerWeightProperty = SimpleDoubleProperty(1.0)

    @JsonIgnore
    val changeWeightProperty = SimpleDoubleProperty(0.0)

    /**
     * 卡牌ID
     */
    var cardId: String
        get() = cardIdProperty.value
        set(value) {
            cardIdProperty.set(value)
        }

    /**
     * 卡牌数据库ID
     */
    var dbfId: Int
        get() = dbfIdProperty.get()
        set(value) {
            dbfIdProperty.set(value)
        }

    /**
     * 名称
     */
    var name: String
        get() = nameProperty.value
        set(value) {
            nameProperty.set(value)
        }

    /**
     * 效果类型
     */
    var effectType: CardEffectTypeEnum
        get() = effectTypeProperty.value
        set(value) {
            effectTypeProperty.set(value)
        }

    /**
     * 打出行为
     */
    var playActions: List<CardActionEnum>
        get() = playActionsProperty.value
        set(value) {
            playActionsProperty.set(value)
        }

    /**
     * 使用行为
     */
    var powerActions: List<CardActionEnum>
        get() = powerActionsProperty.value
        set(value) {
            powerActionsProperty.set(value)
        }

    /**
     * 权重
     */
    var weight: Double
        get() = weightProperty.get()
        set(value) {
            weightProperty.set(value)
        }

    /**
     * 使用权重
     */
    var powerWeight: Double
        get() = powerWeightProperty.get()
        set(value) {
            powerWeightProperty.set(value)
        }

    /**
     * 换牌权重
     */
    var changeWeight: Double
        get() = changeWeightProperty.get()
        set(value) {
            changeWeightProperty.set(value)
        }

    constructor()

    constructor(
        cardId: String,
        dbfId: Int = 0,
        name: String = "",
        effectType: CardEffectTypeEnum = CardEffectTypeEnum.UNKNOWN,
        playActions: List<CardActionEnum> = listOf(CardActionEnum.NO_POINT),
        powerActions: List<CardActionEnum> = listOf(CardActionEnum.NO_POINT),
        weight: Double = 1.0,
        powerWeight: Double = 1.0,
        changeWeight: Double = 0.0
    ) {
        this.cardId = cardId
        this.dbfId = dbfId
        this.name = name
        this.effectType = effectType
        this.playActions = playActions
        this.powerActions = powerActions
        this.weight = weight
        this.powerWeight = powerWeight
        this.changeWeight = changeWeight
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardGroupCard

        return cardId == other.cardId
    }

    override fun hashCode(): Int {
        return cardId.hashCode()
    }

    public override fun clone(): CardGroupCard {
        return CardGroupCard(
            cardId, dbfId, name, effectType, playActions, powerActions,
            weight, powerWeight, changeWeight
        )
    }
}
