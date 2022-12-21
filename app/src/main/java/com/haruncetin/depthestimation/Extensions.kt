package com.haruncetin.depthestimation

import android.graphics.*
import android.media.Image
import java.io.ByteArrayOutputStream

fun FloatArray.toHeatMapBitmap(imageDim : Int): Bitmap {
    val bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.RGB_565)
    for (i in 0 until imageDim) {
        for (j in 0 until imageDim) {
            val c = this[ i * imageDim + j ].toInt()
            bitmap.setPixel(j, i, Color.rgb(r(c), g(c), b(c)))
        }
    }
    return bitmap
}

fun FloatArray.toGrayscaleBitmap(imageDim : Int): Bitmap {
    val bitmap = Bitmap.createBitmap(imageDim, imageDim, Bitmap.Config.RGB_565)
    for (i in 0 until imageDim) {
        for (j in 0 until imageDim) {
            val c = this[ i * imageDim + j ].toInt()
            bitmap.setPixel(j, i, Color.rgb(c, c, c))
        }
    }
    return bitmap
}

fun Image.toBitmap(rotationDegrees: Int = 0): Bitmap {
    val yBuffer = this.planes[0].buffer
    val uBuffer = this.planes[1].buffer
    val vBuffer = this.planes[2].buffer
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
    val yuv = out.toByteArray()

    return BitmapFactory
        .decodeByteArray(yuv, 0, yuv.size)
        .rotateBitmap(rotationDegrees.toFloat())
}

fun Bitmap.rotateBitmap(rotationDegrees: Float = 0.0f): Bitmap {
    val matrix = Matrix()
    matrix.postRotate(rotationDegrees)
    return Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, false )
}

private fun r(value: Int): Int {
    if (value > 127) return if (value < 192) (value - 127) * 4 else 255
    return 0
}

private fun g(value: Int): Int {
    if (value < 64) return value * 4 else if (value > 192) return 1 - (value - 192) * 4
    return 255
}

private fun b(value: Int): Int {
    if (value < 64) return 255 else if (value < 127) return 1 - (value - 64) * 4
    return 0
}
