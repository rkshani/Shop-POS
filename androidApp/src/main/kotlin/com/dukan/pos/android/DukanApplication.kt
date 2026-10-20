package com.dukan.pos.android

import android.app.Application
import com.dukan.pos.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class DukanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@DukanApplication)
        }
    }
}
