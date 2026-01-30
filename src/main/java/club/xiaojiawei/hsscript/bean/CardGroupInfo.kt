package club.xiaojiawei.hsscript.bean

import com.fasterxml.jackson.annotation.JsonIgnore
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList

/**
 * 卡牌组信息数据类
 * @author 肖嘉威
 * @date 2026/1/30
 */
class CardGroupInfo : Cloneable {

    @JsonIgnore
    val nameProperty = SimpleStringProperty("")

    @JsonIgnore
    val enabledProperty = SimpleBooleanProperty(true)

    @JsonIgnore
    val cardsObservable: ObservableList<CardGroupCard> = FXCollections.observableArrayList()

    /**
     * 卡牌组名称
     */
    var name: String
        get() = nameProperty.value
        set(value) {
            nameProperty.set(value)
        }

    /**
     * 是否启用此卡牌组（是否应用到系统）
     */
    var enabled: Boolean
        get() = enabledProperty.get()
        set(value) {
            enabledProperty.set(value)
        }

    /**
     * 卡牌组中的卡牌列表（用于序列化）
     */
    var cards: List<CardGroupCard>
        get() = cardsObservable.toList()
        set(value) {
            cardsObservable.clear()
            cardsObservable.addAll(value)
        }

    constructor()

    constructor(name: String, enabled: Boolean = true, cards: List<CardGroupCard> = emptyList()) {
        this.name = name
        this.enabled = enabled
        this.cards = cards
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CardGroupInfo

        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    public override fun clone(): CardGroupInfo {
        return CardGroupInfo(name, enabled, cards.map { it.clone() })
    }

    override fun toString(): String {
        return name
    }
}
