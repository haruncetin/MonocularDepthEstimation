package com.haruncetin.depthestimation

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat


@ExperimentalGetImage
class MidasNetSmall(var mapType: MapType = MapType.DEPTHVIEW_GRAYSCALE) {
    companion object {
        private const val MODEL_NAME        = "lite-model_midas_v2_1_small_1_lite_1.tflite"
        private const val INPUT_IMAGE_DIM   = 256
        private const val NUM_THREADS       = 8
        private val NORM_MEAN               = floatArrayOf( 123.675f ,  116.28f ,  103.53f )
        private val NORM_STD                = floatArrayOf( 58.395f , 57.12f ,  57.375f )
    }

    private var inferenceTime: Long = 0

    private var interpreter : Interpreter

    // Prepare the image for the MiDAS model
    private val inputTensorProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, ResizeOp.ResizeMethod.BILINEAR))
        .add(NormalizeOp(NORM_MEAN, NORM_STD))
        .build()

    // Perform depth scaling for the output.
    // Please refer to the `DepthScalingOp` class for details.
    private val outputTensorProcessor = TensorProcessor.Builder()
        .add(DepthScalingOp())
        .build()

    init {
        // Initialize TFLite Interpreter
        val interpreterOptions = Interpreter.Options().apply {
            // Use GPU acceleration if available.
            // See -> https://www.tensorflow.org/lite/performance/gpu#android
            val compatibilityList = CompatibilityList()
            if (compatibilityList.isDelegateSupportedOnThisDevice) {
                this.addDelegate(GpuDelegate(compatibilityList.bestOptionsForThisDevice))
            }
            this.numThreads = NUM_THREADS
        }
        interpreter = Interpreter(FileUtil.loadMappedFile(MainActivity.applicationContext(), MODEL_NAME) , interpreterOptions)
    }

    fun getDepthMap( inputImage : Bitmap) : Bitmap {
        // The model takes in an RGB image of shape (256, 256, 3) and
        // outputs a depth map of shape (256, 256, 1)
        // Create a tensor of shape (1, INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, 3)
        // from the given Bitmap.
        // Then perform operations on the tensor as described by `inputTensorProcessor`.
        var inputTensor = TensorImage.fromBitmap( inputImage )

        val startTime = System.currentTimeMillis()

        inputTensor = inputTensorProcessor.process(inputTensor)

        // Output tensor of shape (256, 256, 1) and data type float32
        var outputTensor = TensorBufferFloat.createFixedSize(
            intArrayOf(INPUT_IMAGE_DIM, INPUT_IMAGE_DIM, 1),
            DataType.FLOAT32
        )

        // Perform inference computation on the MiDAS model
        interpreter.run( inputTensor.buffer, outputTensor.buffer )

        // Perform operations on the output tensor as described by `outputTensorProcessor`.
        outputTensor = outputTensorProcessor.process(outputTensor)

        inferenceTime = System.currentTimeMillis() - startTime

        Log.i(MainActivity.APP_LOG_TAG, "Inference time (in ms): $inferenceTime")

        // Create a Bitmap from the depth map
        return if (mapType == MapType.DEPTHVIEW_GRAYSCALE)
            outputTensor.floatArray.toGrayscaleBitmap(INPUT_IMAGE_DIM)
        else
            outputTensor.floatArray.toHeatMapBitmap(INPUT_IMAGE_DIM)
    }

    fun getInferenceTime(): Long {
        return inferenceTime
    }

    // Normalize the output values
    class DepthScalingOp : TensorOperator {
        override fun apply( input : TensorBuffer?): TensorBuffer {
            val values = input!!.floatArray
            // Compute min and max of the output
            val max = values.maxOrNull()!!
            val min = values.minOrNull()!!
            if(max - min > Float.MIN_VALUE) {
                for (i in values.indices) {
                    // Normalize the values and scale them by a factor of 255
                    var p: Int = ((( values[i] - min ) / ( max - min )) * 255).toInt()
                    if (p < 0) {
                        p += 255
                    }
                    values[i] = p.toFloat()
                }
            } else {
                for(i in values.indices) {
                    values[i] = 0.0f
                }
            }

            // Convert the normalized values to the TensorBuffer and return.
            val output = TensorBufferFloat.createFrom( input , DataType.FLOAT32 )
            output.loadArray(values)
            return output
        }

    }

}