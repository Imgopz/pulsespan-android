package com.pulsespan.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulsespan.app.AppContainer
import com.pulsespan.app.data.ai.CoachRepository
import com.pulsespan.app.data.demo.DemoFitbitDataProvider
import com.pulsespan.app.data.healthconnect.HealthConnectManager
import com.pulsespan.app.data.local.UserPreferences
import com.pulsespan.app.data.local.UserPreferencesRepository
import com.pulsespan.app.domain.model.CoachMessage
import com.pulsespan.app.domain.model.DailySnapshot
import com.pulsespan.app.domain.model.HealthConnectAvailability
import com.pulsespan.app.domain.model.PersonalBaseline
import com.pulsespan.app.domain.scoring.HealthScoreEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PulseSpanUiState(
    val isLoading: Boolean = true,
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val hasHealthPermissions: Boolean = false,
    val isDemoMode: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val snapshots: List<DailySnapshot> = emptyList(),
    val baseline: PersonalBaseline = PersonalBaseline(60.0, 58.0, 480),
    val dailyBriefing: String = "",
    val isBriefingLoading: Boolean = false,
    val coachMessages: List<CoachMessage> = emptyList(),
    val isCoachReplying: Boolean = false,
    val statusMessage: String? = null
) {
    val today: DailySnapshot? get() = snapshots.lastOrNull()
    val last7Days: List<DailySnapshot> get() = snapshots.takeLast(7)
}

class PulseSpanViewModel(
    private val healthConnectManager: HealthConnectManager,
    private val preferencesRepository: UserPreferencesRepository,
    private val coachRepository: CoachRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PulseSpanUiState())
    val uiState: StateFlow<PulseSpanUiState> = _uiState.asStateFlow()

    val requiredPermissions: Set<String> get() = healthConnectManager.requiredPermissions

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, statusMessage = null) }

            val prefs = preferencesRepository.preferencesFlow.first()
            val availability = healthConnectManager.getAvailability()
            val hasPerms = if (availability == HealthConnectAvailability.AVAILABLE) {
                runCatching { healthConnectManager.hasAllPermissions() }.getOrDefault(false)
            } else {
                false
            }

            val useDemo = prefs.demoModeEnabled || !hasPerms
            val biometricsList = if (useDemo) {
                DemoFitbitDataProvider.generateLast14Days()
            } else {
                val realData = runCatching { healthConnectManager.readRecentBiometrics(14) }
                    .getOrDefault(emptyList())
                if (realData.isEmpty() || realData.all { it.sleep.totalMinutes == 0 && it.steps == 0 }) {
                    _uiState.update {
                        it.copy(statusMessage = "No Fitbit records found in Health Connect yet — showing sample data.")
                    }
                    DemoFitbitDataProvider.generateLast14Days()
                } else {
                    realData
                }
            }

            val baseline = HealthScoreEngine.computeBaseline(biometricsList, prefs.targetSleepMinutes)
            val snapshots = biometricsList.map { day ->
                DailySnapshot(day, HealthScoreEngine.scoreDay(day, baseline))
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    healthConnectAvailability = availability,
                    hasHealthPermissions = hasPerms,
                    isDemoMode = useDemo,
                    preferences = prefs,
                    snapshots = snapshots,
                    baseline = baseline
                )
            }

            val todaySnapshot = snapshots.lastOrNull()
            if (todaySnapshot != null && prefs.aiCoachingEnabled) {
                loadDailyBriefing(todaySnapshot, baseline)
            }
        }
    }

    private fun loadDailyBriefing(today: DailySnapshot, baseline: PersonalBaseline) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBriefingLoading = true) }
            val briefing = coachRepository.generateDailyBriefing(today, baseline)
            _uiState.update { state ->
                val initialMessages = if (state.coachMessages.isEmpty()) {
                    listOf(
                        CoachMessage(
                            id = UUID.randomUUID().toString(),
                            isFromUser = false,
                            text = briefing
                        )
                    )
                } else {
                    state.coachMessages
                }
                state.copy(
                    dailyBriefing = briefing,
                    isBriefingLoading = false,
                    coachMessages = initialMessages
                )
            }
        }
    }

    fun sendCoachQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank()) return
        val today = _uiState.value.today ?: return
        val baseline = _uiState.value.baseline

        val userMsg = CoachMessage(
            id = UUID.randomUUID().toString(),
            isFromUser = true,
            text = trimmed
        )

        _uiState.update {
            it.copy(
                coachMessages = it.coachMessages + userMsg,
                isCoachReplying = true
            )
        }

        viewModelScope.launch {
            val reply = coachRepository.askCoach(trimmed, today, baseline)
            val coachMsg = CoachMessage(
                id = UUID.randomUUID().toString(),
                isFromUser = false,
                text = reply
            )
            _uiState.update {
                it.copy(
                    coachMessages = it.coachMessages + coachMsg,
                    isCoachReplying = false
                )
            }
        }
    }

    fun toggleDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDemoMode(enabled)
            refreshData()
        }
    }

    fun onPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            if (granted.containsAll(requiredPermissions)) {
                preferencesRepository.setDemoMode(false)
            }
            refreshData()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PulseSpanViewModel(
                        healthConnectManager = container.healthConnectManager,
                        preferencesRepository = container.preferencesRepository,
                        coachRepository = container.coachRepository
                    ) as T
                }
            }
    }
}
