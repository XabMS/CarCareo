package com.xabier.carcareo

import android.app.Application
import com.xabier.carcareo.di.AppContainer
import com.xabier.carcareo.notifications.Notifications
import com.xabier.carcareo.notifications.WorkScheduler

/**
 * Manual dependency injection root. The spec's architecture is
 * Compose -> ViewModel -> Repository -> DAO with no DI framework, so the
 * Application object owns the singletons via [AppContainer] and ViewModels
 * reach them through it.
 */
class CarCareoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Optional reminders (F6). Channels are cheap to (re)create; the workers
        // are (re)scheduled to match whatever the user last chose in Settings.
        Notifications.ensureChannels(this)
        WorkScheduler.apply(this, container.appPreferences.snapshot())
    }
}
