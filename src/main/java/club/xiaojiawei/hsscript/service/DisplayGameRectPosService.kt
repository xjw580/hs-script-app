package club.xiaojiawei.hsscript.service

import club.xiaojiawei.hsscript.bean.GameRect
import club.xiaojiawei.hsscript.dll.CSystemDll
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.ConfigUtil

/**
 * @author 肖嘉威
 * @date 2025/3/24 17:21
 */
object DisplayGameRectPosService : Service<Boolean>() {
    private const val MAX_RECT_COUNT = 3
    private const val RECT_THICKNESS = 2f

    private val rectQueue = ArrayDeque<GameRect>()

    @Synchronized
    fun show(gameRect: GameRect) {
        if (!isRunning || !CSystemDll.INSTANCE.isConnected()) {
            return
        }
        if (rectQueue.size >= MAX_RECT_COUNT) {
            rectQueue.removeFirst()
        }
        rectQueue.addLast(gameRect)
        redraw()
    }

    @Synchronized
    private fun redraw() {
        if (!CSystemDll.INSTANCE.isConnected()) {
            return
        }
//        CSystemDll.INSTANCE.presentDraw(true)
        CSystemDll.INSTANCE.clearPresentDraw()
        rectQueue.forEach { gameRect ->
            val rect = gameRect.getRelativeRect()
            CSystemDll.INSTANCE.drawPresentRect(
                rect.x.toFloat(),
                rect.y.toFloat(),
                rect.width.toFloat(),
                rect.height.toFloat(),
                RECT_THICKNESS,
                false,
                1f,
                0f,
                0f,
                1f,
            )
        }
    }

    @Synchronized
    private fun clear() {
        rectQueue.clear()
        CSystemDll.INSTANCE.clearPresentDraw()
    }

    override fun execStart(): Boolean {
        clear()
        CSystemDll.INSTANCE.presentDraw(true)
        return true
    }

    override fun execStop(): Boolean {
        clear()
        return true
    }

    override fun getStatus(value: Boolean?): Boolean = value ?: ConfigUtil.getBoolean(ConfigEnum.DISPLAY_GAME_RECT_POS)
}
