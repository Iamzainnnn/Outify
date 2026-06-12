package cc.tomko.outify.ui.viewmodel.bottomsheet

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.dao.PlaylistDao
import cc.tomko.outify.data.database.playlist.PlaylistItemEntity
import cc.tomko.outify.data.database.playlist.canModify
import cc.tomko.outify.data.database.playlist.toDomain
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.ui.widgets.PlaybackWidget
import cc.tomko.outify.widgetMediaPreference
import coil3.imageLoader
import coil3.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class AddToWidgetViewModel @Inject constructor(
    private val metadata: Metadata,

    @ApplicationContext
    private val context: Context,
    private val json: Json,
) : ViewModel() {
    private val _glanceIds = MutableStateFlow<List<GlanceId>>(emptyList())
    val glanceIds: StateFlow<List<GlanceId>> = _glanceIds.asStateFlow()

    suspend fun loadWidgets() {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(PlaybackWidget::class.java)

        _glanceIds.value = ids
    }

    suspend fun addToWidget(uri: String, id: GlanceId) {
        updateAppWidgetState(context, id) {
            val currentJson = json.decodeFromString<List<String>>(it[widgetMediaPreference(id)] ?: "[]").toMutableList()
            if(currentJson.contains(uri)) return@updateAppWidgetState

            currentJson.add(uri)
            it[widgetMediaPreference(id)] = json.encodeToString(currentJson)
        }
        PlaybackWidget().update(context, id)
    }

    /**
     * Saves the bitmap locally.
     */
    suspend fun saveBitmap(imageUrl: String?, uri: String) {
        if(imageUrl == null) return
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCacheKey(uri)
            .build()

        context.imageLoader.execute(request)
    }
}