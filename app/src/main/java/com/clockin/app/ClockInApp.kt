package com.clockin.app

import android.app.Application
import com.clockin.app.data.local.AppDatabase
import com.clockin.app.data.local.DatabaseProvider
import com.clockin.app.data.local.PreferencesManager
import com.clockin.app.util.ScanClock

class ClockInApp : Application() {

    val database: AppDatabase by lazy {
        DatabaseProvider.getDatabase(this)
    }

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    val scanClock: ScanClock by lazy {
        ScanClock(this)
    }
}
