package com.example

import android.app.Application
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class ShieldApplication : Application() {
    lateinit var container: AppContainer
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
