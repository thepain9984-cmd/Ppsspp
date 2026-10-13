package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ButtonMapping
import com.example.model.GamepadProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class GamepadRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("custom_controls_prefs", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow<List<GamepadProfile>>(emptyList())
    val profiles: StateFlow<List<GamepadProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<GamepadProfile>(GamepadProfile.createDefaultProfile())
    val activeProfile: StateFlow<GamepadProfile> = _activeProfile.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isEditModeActive = MutableStateFlow(false)
    val isEditModeActive: StateFlow<Boolean> = _isEditModeActive.asStateFlow()

    init {
        loadProfiles()
    }

    fun setOverlayActive(active: Boolean) {
        _isOverlayActive.value = active
    }

    fun setEditModeActive(editMode: Boolean) {
        _isEditModeActive.value = editMode
    }

    private fun loadProfiles() {
        val savedJson = prefs.getString(KEY_PROFILES, null)
        val loadedList = mutableListOf<GamepadProfile>()

        if (!savedJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i)
                    if (obj != null) {
                        loadedList.add(GamepadProfile.fromJsonObject(obj))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (loadedList.isEmpty()) {
            loadedList.add(GamepadProfile.createDefaultProfile())
            loadedList.add(GamepadProfile.createRpgProfile())
            loadedList.add(GamepadProfile.createFpsProfile())
            saveProfilesInternal(loadedList)
        }

        _profiles.value = loadedList

        val activeId = prefs.getString(KEY_ACTIVE_PROFILE_ID, loadedList.first().id)
        val selected = loadedList.find { it.id == activeId } ?: loadedList.first()
        _activeProfile.value = selected
    }

    fun selectProfile(id: String) {
        val found = _profiles.value.find { it.id == id } ?: return
        _activeProfile.value = found
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
    }

    fun updateProfile(updated: GamepadProfile) {
        val currentList = _profiles.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            currentList[index] = updated
        } else {
            currentList.add(updated)
        }
        _profiles.value = currentList
        if (_activeProfile.value.id == updated.id) {
            _activeProfile.value = updated
        }
        saveProfilesInternal(currentList)
    }

    fun updateMappingInActiveProfile(updatedMapping: ButtonMapping) {
        val current = _activeProfile.value
        val updatedMappings = current.mappings.map {
            if (it.key == updatedMapping.key) updatedMapping else it
        }
        val updatedProfile = current.copy(mappings = updatedMappings)
        updateProfile(updatedProfile)
    }

    fun addProfile(name: String, description: String = ""): GamepadProfile {
        val newProfile = GamepadProfile(
            name = name,
            description = description,
            mappings = GamepadProfile.createDefaultMappings()
        )
        val currentList = _profiles.value.toMutableList()
        currentList.add(newProfile)
        _profiles.value = currentList
        saveProfilesInternal(currentList)
        selectProfile(newProfile.id)
        return newProfile
    }

    fun deleteProfile(id: String) {
        if (_profiles.value.size <= 1) return // Keep at least one profile
        val currentList = _profiles.value.filter { it.id != id }
        _profiles.value = currentList
        saveProfilesInternal(currentList)
        if (_activeProfile.value.id == id) {
            selectProfile(currentList.first().id)
        }
    }

    private fun saveProfilesInternal(list: List<GamepadProfile>) {
        val array = JSONArray()
        list.forEach { array.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    companion object {
        private const val KEY_PROFILES = "saved_gamepad_profiles_v1"
        private const val KEY_ACTIVE_PROFILE_ID = "active_gamepad_profile_id"

        @Volatile
        private var instance: GamepadRepository? = null

        fun getInstance(context: Context): GamepadRepository {
            return instance ?: synchronized(this) {
                instance ?: GamepadRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
