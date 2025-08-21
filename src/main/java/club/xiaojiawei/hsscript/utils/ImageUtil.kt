package club.xiaojiawei.hsscript.utils

import club.xiaojiawei.hsscript.utils.ImageUtil.byteBufferToBufferedImage
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.ByteBuffer
import javax.imageio.ImageIO

/**
 * @author 肖嘉威
 * @date 2025/8/21 14:28
 */
object ImageUtil {

    /**
     * 从 ByteBuffer (BGRA 格式) 转换为 BufferedImage
     * @param buffer BGRA 格式的字节数据 (每像素4字节)
     * @param width 图片宽度
     * @param height 图片高度
     */
    fun byteBufferToBufferedImage(buffer: ByteBuffer, width: Int, height: Int): BufferedImage {
        buffer.rewind()
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val dataBuffer = (image.raster.dataBuffer as DataBufferInt).data

        // ByteBuffer 是顺序存储的 ARGB，每像素4字节
        var i = 0
        while (buffer.hasRemaining() && i < dataBuffer.size) {
            val b = buffer.get().toInt() and 0xFF
            val g = buffer.get().toInt() and 0xFF
            val r = buffer.get().toInt() and 0xFF
            val a = buffer.get().toInt() and 0xFF

            // ARGB int: 0xAARRGGBB
            dataBuffer[i++] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        buffer.rewind()
        return image
    }

    /**
     * 从图片文件中读取并裁剪指定区域，返回 BufferedImage
     *
     * @param imagePath 图片路径
     * @param x         裁剪起点 X 坐标
     * @param y         裁剪起点 Y 坐标
     * @param width     裁剪宽度
     * @param height    裁剪高度
     * @return 裁剪后的 BufferedImage
     */
    @Throws(Exception::class)
    fun cropImage(
        imagePath: String,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): BufferedImage {
        val originalImage = ImageIO.read(File(imagePath))
            ?: throw IllegalArgumentException("无法读取图片: $imagePath")
        return cropImage(originalImage, x, y, width, height)
    }

    /**
     * 裁剪图片指定区域，返回 BufferedImage
     *
     * @param image 图片
     * @param x         裁剪起点 X 坐标
     * @param y         裁剪起点 Y 坐标
     * @param width     裁剪宽度
     * @param height    裁剪高度
     * @return 裁剪后的 BufferedImage
     */
    @Throws(Exception::class)
    fun cropImage(
        image: BufferedImage,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): BufferedImage {
        val originalImage = image

        val cropRect = Rectangle(
            x.coerceAtLeast(0),
            y.coerceAtLeast(0),
            width.coerceAtMost(originalImage.width - x),
            height.coerceAtMost(originalImage.height - y)
        )

        val croppedImage = originalImage.getSubimage(
            cropRect.x,
            cropRect.y,
            cropRect.width,
            cropRect.height
        )

        val outputImage = BufferedImage(cropRect.width, cropRect.height, BufferedImage.TYPE_INT_ARGB)
        val g: Graphics2D = outputImage.createGraphics()
        g.drawImage(croppedImage, 0, 0, null)
        g.dispose()

        return outputImage
    }

}

fun BufferedImage.cropImage(
    x: Int,
    y: Int,
    width: Int,
    height: Int
): BufferedImage {
    return ImageUtil.cropImage(this, x, y, width, height)
}

fun ByteBuffer.toBufferedImage(width: Int, height: Int): BufferedImage {
    return byteBufferToBufferedImage(this, width, height)
}