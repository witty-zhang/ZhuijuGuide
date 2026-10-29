package com.lzzhang.zhuijuguide

import android.app.Application
import com.lzzhang.zhuijuguide.data.ResourceRefreshWorker

class ZhuijuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ResourceRefreshWorker.schedule(this)
    }
}
