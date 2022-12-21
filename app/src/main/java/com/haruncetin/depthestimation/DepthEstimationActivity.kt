package com.haruncetin.depthestimation

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.system.exitProcess


@ExperimentalGetImage
class DepthEstimationActivity : AppCompatActivity() {
    private var spinner: Spinner? = null
    private var viewOriginal: PreviewView? = null
    private var viewDepth: SurfaceView? = null
    private var btnFlipCamera: FloatingActionButton? = null
    private var originalView: Preview? = null
    private var depthAnalysisView: ImageAnalysis? = null

    private lateinit var cameraProviderListenableFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var frameAnalyser : FrameAnalyser

    private var useFrontCamera = false
    private val REQUEST_CODE_PERMISSIONS = 1001

    private val REQUIRED_PERMISSIONS = arrayOf(
        "android.permission.CAMERA"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_depth_estimation)

        var doubleBackToExit = false

        viewOriginal = findViewById(R.id.view_original)
        viewDepth = findViewById(R.id.view_depth)
        btnFlipCamera = findViewById(R.id.fab_flip_camera)
        spinner = findViewById(R.id.spinner)

        frameAnalyser = FrameAnalyser(MidasNetSmall(MapType.DEPTHVIEW_GRAYSCALE), viewDepth!!)

        initCamera(useFrontCamera)

        btnFlipCamera!!.setOnClickListener {
            useFrontCamera = !useFrontCamera
            initCamera(useFrontCamera)
        }

        spinner!!.onItemSelectedListener =  object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                frameAnalyser = if (position == 0)
                    FrameAnalyser(MidasNetSmall(MapType.DEPTHVIEW_GRAYSCALE), viewDepth!!)
                else
                    FrameAnalyser(MidasNetSmall(MapType.DEPTHVIEW_HEATMAP), viewDepth!!)

                initCamera(useFrontCamera)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {

            }
        }

        onBackPressedDispatcher.addCallback(this /* lifecycle owner */, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Back is pressed... Exiting the app
                if (doubleBackToExit) {
                    exitProcess(0)
                }
                doubleBackToExit = true
                Toast.makeText(MainActivity.applicationContext(), "Press again to exit", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExit = false }, 2000)
            }
        })
    }

    private fun initCamera(frontCamera: Boolean) {
        if (allPermissionsGranted()) {
            when(frontCamera) {
                false -> setupCameraProvider( CameraSelector.LENS_FACING_BACK )
                true -> setupCameraProvider( CameraSelector.LENS_FACING_FRONT )
            }
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initCamera(useFrontCamera)
            } else {
                Toast.makeText(
                    this,
                    "If the required permissions are not granted, " +
                            "the program may not work properly.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun setupCameraProvider(cameraFacing : Int) {
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderListenableFuture.addListener({
            try {
                val cameraProvider: ProcessCameraProvider = cameraProviderListenableFuture.get()
                bindPreview( cameraProvider , cameraFacing )
            }
            catch (e: ExecutionException) {
                Log.e(MainActivity.APP_LOG_TAG, e.message!!)
            }
            catch (e: InterruptedException) {
                Log.e(MainActivity.APP_LOG_TAG, e.message!!)
            }
        }, ContextCompat.getMainExecutor( this ))
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider , lensFacing : Int ) {
        if (originalView != null) {
            cameraProvider.unbind(originalView)
        }

        if (depthAnalysisView != null) {
            cameraProvider.unbind(depthAnalysisView)
        }

        val originalResolution = Size(viewOriginal!!.width, viewOriginal!!.height)
        originalView = Preview.Builder()
            .setTargetResolution(originalResolution)
            .build()
        originalView!!.setSurfaceProvider(viewOriginal!!.surfaceProvider)

        val cameraFacingSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        depthAnalysisView = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        depthAnalysisView!!.setAnalyzer(Executors.newCachedThreadPool() , frameAnalyser)

        cameraProvider.bindToLifecycle(
            (this as LifecycleOwner),
            cameraFacingSelector,
            depthAnalysisView,
            originalView
        )
    }
}
