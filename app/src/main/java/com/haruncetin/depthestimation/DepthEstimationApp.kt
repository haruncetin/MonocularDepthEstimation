package com.haruncetin.depthestimation

import android.app.Application
import android.content.Context

/*
* This class is only created to access globally to the application context
* */
class DepthEstimationApp : Application() {

    init {
        instance = this
    }

    companion object {
        private lateinit var instance: DepthEstimationApp

        fun applicationContext() : Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}