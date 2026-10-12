package com.example.soukitv.features.home.presentation.components

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

class AutoCropTransparentTransformation : Transformation {
    override val cacheKey: String = "auto_crop_transparent"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = (pixels[y * width + x] ushr 24) and 0xff
                if (alpha > 15) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return input

        val cropWidth = (maxX - minX + 1).coerceAtLeast(1)
        val cropHeight = (maxY - minY + 1).coerceAtLeast(1)

        return Bitmap.createBitmap(input, minX, minY, cropWidth, cropHeight)
    }
}

