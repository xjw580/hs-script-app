package club.xiaojiawei.hsscript.listener.log

import club.xiaojiawei.hsscript.bean.Deck
import club.xiaojiawei.hsscript.consts.GAME_DECKS_LOG_NAME
import club.xiaojiawei.hsscript.listener.WorkTimeListener
import club.xiaojiawei.hsscript.status.PauseStatus
import club.xiaojiawei.hsscript.utils.PowerLogUtil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 套牌日志监听器
 * @author 肖嘉威
 * @date 2023/9/20 16:43
 */

object DeckLogListener : AbstractLogListener(GAME_DECKS_LOG_NAME, 0, 1500L, TimeUnit.MILLISECONDS) {

    val DECKS = LinkedList<Deck>()

    var dealing = false

    override fun dealOldLog() {
        if (dealing) return
        dealing = true
        logFile?.let { file ->
            var line: String?
            while (!PauseStatus.isPause && WorkTimeListener.working) {
                line = file.readLine()
                if (line == null || line.isEmpty()) break
                if (line.contains("Deck Contents Received")) {
                    dealReceived()
                } else if (line.contains("Finished Editing Deck")) {
                    dealEditing()
                }
            }
        }
        dealing = false
    }

    private fun dealReceived() {
        DECKS.clear()
        var line: String?
        var filePointer = logFile!!.getPosition()
        while (true) {
            line = logFile!!.readLine()
            if (line == null ) break
            if (!line.contains("#")) {
                logFile!!.seek(filePointer)
                break
            }
            DECKS.addFirst(createDeck(line))
            filePointer = logFile!!.getPosition()
        }
    }

    private fun dealEditing() {
        val line = logFile?.readLine() ?: return
        val deck = createDeck(line)
        var exist = false
        for (d in DECKS) {
            if (d.id == deck.id) {
                d.apply {
                    name = deck.name
                    code = deck.code
                }
                exist = true
                break
            }
        }
        if (!exist) {
            DECKS.addFirst(deck)
        }
    }

    private fun createDeck(line: String): Deck {
        var l = line
        return Deck(
            l.substring(l.indexOf("#") + 4),
            (logFile!!.readLine()!!.also { l = it }).substring(l.indexOf("#") + 11),
            (logFile!!.readLine()!!.also { l = it }).substring(l.lastIndexOf(" ") + 1)
        )
    }

    override fun dealNewLog() {
        dealOldLog()
    }

}
