package club.xiaojiawei.hsscript.bean

import club.xiaojiawei.hsscript.consts.ROOT_PATH
import club.xiaojiawei.hsscript.enums.ConfigEnum
import club.xiaojiawei.hsscript.utils.ConfigUtil
import club.xiaojiawei.hsscript.utils.FileUtil
import net.sourceforge.tess4j.Tesseract
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * @author 肖嘉威
 * @date 2025/8/25 10:57
 */
class TesseractEx : Tesseract() {

    private val ocrDir = "ocr_res"

    fun doOCR(p0: BufferedImage?, desc: String = ""): String {
        if (ConfigUtil.getBoolean(ConfigEnum.SAVE_OCR_IMG)) {
            val ocrPath = Path(ROOT_PATH, ocrDir)
            if (!ocrPath.exists()) {
                ocrPath.createDirectories()
            }
            ocrPath.toFile().listFiles()?.let {
                if (it.size > 20) {
                    FileUtil.clearDirectory(ocrPath.toFile())
                }
            }
            p0?.let {
                val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
                val ocrPath = ocrPath.resolve("${time}_${desc}_${UUID.randomUUID()}.png").toFile()
                ImageIO.write(it, "png", ocrPath)
            }
        }
        return super.doOCR(p0)
    }
}