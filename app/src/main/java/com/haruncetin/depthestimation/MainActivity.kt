package com.haruncetin.depthestimation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.system.exitProcess


@ExperimentalGetImage
class MainActivity : AppCompatActivity() {
    var haruncetin: TextView? = null
    companion object {
        const val APP_LOG_TAG = "MiDaS Depth Estimation"
        private var instance: MainActivity? = null

//        fun applicationContext() : Context {
//            return instance!!.applicationContext
//        }
    }

    init {
        instance = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        applicationContext.openFileInput("/vendor/etc/public.libraries.txt").use {
//            val br = it.bufferedReader()
//            var line : String? = null
//            do {
//                line = br.readLine()
//                Log.i(MainActivity.APP_LOG_TAG, line)
//            } while (line != null)
//        }
        haruncetin = findViewById(R.id.haruncetin)
        haruncetin!!.paintFlags = haruncetin!!.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        haruncetin!!.setOnClickListener {
            ContextCompat.startActivity(
//                DepthEstimationApp.applicationContext(),
                DepthEstimationApp().applicationContext,
                Intent(Intent.ACTION_VIEW,Uri.parse("https://www.haruncetin.com.tr")),
                null
            )
            exitProcess(0)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val i = Intent(this@MainActivity, DepthEstimationActivity::class.java)
            startActivity(i)
            finish()
        }, 3000)
    }
}
