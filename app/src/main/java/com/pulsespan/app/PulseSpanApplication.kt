package com.pulsespan.app

import android.app.Application
import com.pulsespan.app.data.ai.CoachRepository
import com.pulsespan.app.data.ai.GeminiCoachRepository
import com.pulsespan.app.data.healthconnect.HealthConnectManager
import com.pulsespan.app.data.local.UserPreferencesRepository

class AppContainer(application: Application) {
    val healthConnectManager = HealthConnectManager(application)
    val preferencesRepository = UserPreferencesRepository(application)
    val coachRepository: CoachRepository = GeminiCoachRepository()
}

class PulseSpanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
