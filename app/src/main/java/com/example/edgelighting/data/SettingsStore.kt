package com.example.edgelighting.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edge_lighting_settings")

data class AppRuleModel(
    val packageName: String,
    val primaryColor: Int,
    val secondaryColor: Int,
    val style: String,
    val enabled: Boolean
)

class SettingsStore(private val context: Context) {

    private val gson = Gson()

    companion object {
        val KEY_IS_ENABLED = booleanPreferencesKey("is_edge_lighting_enabled")
        val KEY_DEFAULT_STYLE = stringPreferencesKey("default_anim_style")
        val KEY_DURATION_MS = longPreferencesKey("lighting_duration_ms")
        val KEY_THICKNESS = floatPreferencesKey("edge_thickness_px")
        val KEY_SPEED = floatPreferencesKey("anim_speed_multiplier")
        val KEY_APP_RULES_JSON = stringPreferencesKey("custom_app_rules_json")
    }

    val isMainEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_ENABLED] ?: true
    }

    val defaultStyleFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_STYLE] ?: "laser_comet"
    }

    val durationFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_DURATION_MS] ?: 3500L
    }

    val thicknessFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_THICKNESS] ?: 6f
    }

    val speedFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEED] ?: 1.2f
    }

    suspend fun setMainEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_IS_ENABLED] = enabled }
    }

    suspend fun setDefaultStyle(style: String) {
        context.dataStore.edit { it[KEY_DEFAULT_STYLE] = style }
    }

    suspend fun setDuration(ms: Long) {
        context.dataStore.edit { it[KEY_DURATION_MS] = ms }
    }

    suspend fun setThickness(px: Float) {
        context.dataStore.edit { it[KEY_THICKNESS] = px }
    }

    suspend fun setSpeed(speed: Float) {
        context.dataStore.edit { it[KEY_SPEED] = speed }
    }

    suspend fun saveAppRule(rule: AppRuleModel) {
        context.dataStore.edit { prefs ->
            val existingJson = prefs[KEY_APP_RULES_JSON] ?: "[]"
            val type = object : TypeToken<MutableList<AppRuleModel>>() {}.type
            val list: MutableList<AppRuleModel> = gson.fromJson(existingJson, type) ?: mutableListOf()
            list.removeAll { it.packageName == rule.packageName }
            list.add(rule)
            prefs[KEY_APP_RULES_JSON] = gson.toJson(list)
        }
    }

    suspend fun getAppRule(packageName: String): AppRuleModel? {
        var result: AppRuleModel? = null
        context.dataStore.data.map { prefs ->
            val json = prefs[KEY_APP_RULES_JSON] ?: "[]"
            val type = object : TypeToken<List<AppRuleModel>>() {}.type
            val list: List<AppRuleModel> = gson.fromJson(json, type) ?: emptyList()
            list.find { it.packageName == packageName }
        }.collect { rule ->
            result = rule
        }
        return result
    }
}
