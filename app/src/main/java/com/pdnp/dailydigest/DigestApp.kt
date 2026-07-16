package com.pdnp.dailydigest

import android.app.Application
import com.pdnp.dailydigest.work.DailySyncWorker

class DigestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DailySyncWorker.schedule(this)
    }
}
