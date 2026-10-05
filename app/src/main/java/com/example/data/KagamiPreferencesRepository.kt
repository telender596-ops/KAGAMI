package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kagami_preferences")

data class KagamiSettings(
    val pureOledTheme: Boolean = true,
    val showParticles: Boolean = true,
    val defaultBrushSize: Float = 8f,
    val brushSmoothing: Float = 0.65f,
    val showCanvasGrid: Boolean = true,
    val defaultPaperTone: String = "PAPER_WHITE", // PAPER_WHITE, DARK_SLATE, OLED_VOID
    val exportFormat: String = "PNG", // PNG or JPG
    val jpgQuality: Int = 95
)

class KagamiPreferencesRepository(private val context: Context) {

    private object Keys {
        val PURE_OLED_THEME = booleanPreferencesKey("pure_oled_theme")
        val SHOW_PARTICLES = booleanPreferencesKey("show_particles")
        val DEFAULT_BRUSH_SIZE = floatPreferencesKey("default_brush_size")
        val BRUSH_SMOOTHING = floatPreferencesKey("brush_smoothing")
        val SHOW_CANVAS_GRID = booleanPreferencesKey("show_canvas_grid")
        val DEFAULT_PAPER_TONE = stringPreferencesKey("default_paper_tone")
        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val JPG_QUALITY = intPreferencesKey("jpg_quality")
    }

    val settingsFlow: Flow<KagamiSettings> = context.dataStore.data.map { prefs ->
        KagamiSettings(
            pureOledTheme = prefs[Keys.PURE_OLED_THEME] ?: true,
            showParticles = prefs[Keys.SHOW_PARTICLES] ?: true,
            defaultBrushSize = prefs[Keys.DEFAULT_BRUSH_SIZE] ?: 8f,
            brushSmoothing = prefs[Keys.BRUSH_SMOOTHING] ?: 0.65f,
            showCanvasGrid = prefs[Keys.SHOW_CANVAS_GRID] ?: true,
            defaultPaperTone = prefs[Keys.DEFAULT_PAPER_TONE] ?: "PAPER_WHITE",
            exportFormat = prefs[Keys.EXPORT_FORMAT] ?: "PNG",
            jpgQuality = prefs[Keys.JPG_QUALITY] ?: 95
        )
    }

    suspend fun setPureOledTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PURE_OLED_THEME] = enabled }
    }

    suspend fun setShowParticles(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_PARTICLES] = enabled }
    }

    suspend fun setDefaultBrushSize(size: Float) {
        context.dataStore.edit { it[Keys.DEFAULT_BRUSH_SIZE] = size.coerceIn(2f, 48f) }
    }

    suspend fun setBrushSmoothing(smoothing: Float) {
        context.dataStore.edit { it[Keys.BRUSH_SMOOTHING] = smoothing.coerceIn(0f, 1f) }
    }

    suspend fun setShowCanvasGrid(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_CANVAS_GRID] = enabled }
    }

    suspend fun setDefaultPaperTone(tone: String) {
        context.dataStore.edit { it[Keys.DEFAULT_PAPER_TONE] = tone }
    }

    suspend fun setExportFormat(format: String) {
        context.dataStore.edit { it[Keys.EXPORT_FORMAT] = format }
    }

    suspend fun setJpgQuality(quality: Int) {
        context.dataStore.edit { it[Keys.JPG_QUALITY] = quality.coerceIn(60, 100) }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { it.clear() }
    }
}
