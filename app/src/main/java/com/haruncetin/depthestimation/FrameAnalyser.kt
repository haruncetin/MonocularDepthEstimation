package com.haruncetin.depthestimation

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceView
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Image Analyser for performing depth estimation on the selected camera frames.
@ExperimentalGetImage
class FrameAnalyser(
    private var depthModel : MidasNetSmall,
    private var depthView: SurfaceView
) : ImageAnalysis.Analyzer {

    private var metrics: DisplayMetrics? = null

    private var inferenceTime: Long = 0
    private var mLastTime: Long = 0
    private var fps = 0
    private var ifps:Int = 0

    init{
        metrics =  DepthEstimationApp.applicationContext().resources.displayMetrics
    }

    private var readyToProcess = true

    override fun analyze(image: ImageProxy) {

        // If the analyser is not ready to process the current frame, skip it.
        if ( !readyToProcess ) {
            image.close()
            return
        }

        readyToProcess = false

        if (image.image != null) {
//            Log.i(MainActivity.APP_LOG_TAG, "Image.format: %d, Image.width: %d, Image.height: %d".format(image.image!!.format, image.image!!.width, image.image!!.height))
            val bitmap = image.image!!.toBitmap(image.imageInfo.rotationDegrees)
            image.close()
            CoroutineScope( Dispatchers.Main ).launch {
                run(bitmap)
            }
        }
    }

    private fun draw(image: Bitmap) {
        val canvas: Canvas = depthView.holder.lockCanvas() ?: return
        val now: Long = System.currentTimeMillis()
        synchronized(depthView.holder) {
            val scaled: Bitmap = Bitmap.createScaledBitmap(
                image,
                depthView.width,
                depthView.height,
                true
            )
            val paint = Paint()
            canvas.drawBitmap(scaled, 0f, 0f, null)
            paint.color = Color.RED
            paint.isAntiAlias = true
            paint.textSize = 14f * (metrics!!.densityDpi/160f) // 14dp
            canvas.drawText("Inference Time : $inferenceTime ms", 50f, 80f, paint)
            canvas.drawText("FPS : $fps", 50f, 150f, paint)
            depthView.holder.unlockCanvasAndPost(canvas)
        }
        ifps++
        if(now > (mLastTime + 1000)) {
            mLastTime = now
            fps = ifps
            ifps = 0
        }
    }

    private suspend fun run(inputImage : Bitmap) = withContext(Dispatchers.Default) {

        // Compute the depth for the given frame Bitmap.
        val output = depthModel.getDepthMap(inputImage)
        inferenceTime = depthModel.getInferenceTime()

        withContext( Dispatchers.Main ) {

            // Draw the depth Bitmap to the SurfaceView.
            // Please refer to the draw function for details.
            draw(output)

            // Notify that the current frame is processed and
            // the pipeline is ready for the next frame.
            readyToProcess = true

        }
    }
}