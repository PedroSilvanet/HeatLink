package pt.heatlink.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.heatlink.shared.HeatUpdate

object HeatStore {
    private const val PREFS = "heatlink"
    private const val LAST_UPDATE = "last_update"
    private val state = MutableStateFlow(HeatUpdate())
    val updates = state.asStateFlow()

    fun load(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(LAST_UPDATE, null)
            ?.let { runCatching { HeatUpdate.fromJson(it) }.getOrNull() }
            ?.let { state.value = it }
    }

    fun save(context: Context, update: HeatUpdate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(LAST_UPDATE, update.toJson()).apply()
        state.value = update
    }
}
