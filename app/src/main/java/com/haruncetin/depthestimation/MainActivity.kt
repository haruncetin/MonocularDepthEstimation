package com.haruncetin.depthestimation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage

@ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    companion object {
        val APP_LOG_TAG = "MiDaS Depth Estimation"
        private var instance: MainActivity? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }

    init {
        instance = this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Handler(Looper.getMainLooper()).postDelayed({
            val i = Intent(this@MainActivity, DepthEstimationActivity::class.java)
            startActivity(i)
            finish()
        }, 3000)
    }
}
