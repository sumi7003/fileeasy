package com.xplay.player.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Hashtable

object QRCodeUtil {

    /**
     * 创建二维码位图
     */
    fun createQRCodeBitmap(
        content: String,
        width: Int,
        height: Int,
        characterSet: String = "UTF-8",
        errorCorrection: String = "H",
        margin: String = "2",
        colorBlack: Int = Color.BLACK,
        colorWhite: Int = Color.WHITE
    ): Bitmap? {
        if (content.isEmpty() || width < 0 || height < 0) {
            return null
        }

        return try {
            val hints = Hashtable<EncodeHintType, String>()
            if (characterSet.isNotEmpty()) {
                hints[EncodeHintType.CHARACTER_SET] = characterSet
            }
            if (errorCorrection.isNotEmpty()) {
                hints[EncodeHintType.ERROR_CORRECTION] = errorCorrection
            }
            if (margin.isNotEmpty()) {
                hints[EncodeHintType.MARGIN] = margin
            }

            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints)
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (bitMatrix[x, y]) {
                        pixels[y * width + x] = colorBlack
                    } else {
                        pixels[y * width + x] = colorWhite
                    }
                }
            }

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
