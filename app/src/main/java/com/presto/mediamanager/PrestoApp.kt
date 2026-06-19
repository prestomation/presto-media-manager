package com.presto.mediamanager

import android.app.Application
import com.presto.mediamanager.work.BadgeManager
import com.presto.mediamanager.work.WorkScheduler

class PrestoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        BadgeManager.ensureChannel(this)
        WorkScheduler.schedulePeriodic(this)
    }
}
