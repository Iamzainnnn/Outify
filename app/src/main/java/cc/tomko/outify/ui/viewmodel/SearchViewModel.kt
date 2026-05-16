package cc.tomko.outify.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.R
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.model.Album
import cc.tomko.outify.core.model.Artist
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.repository.SearchRepository
import cc.tomko.outify.data.repository.SettingsRepository
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.model.search.SearchHistoryItem
import cc.tomko.outify.ui.model.search.SearchResultType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    val metadata: Metadata,
    val spirc: SpircWrapper,
    val spClient: SpClient,
    private val repository: SearchRepository,
    private val playbackStateHolder: PlaybackStateHolder,
    private val settingsRepository: SettingsRepository,
): ViewModel() {
    private val queryFlow = MutableStateFlow("")

    private val _results = MutableStateFlow<List<SearchUiModel>>(emptyList())
    val results: StateFlow<List<SearchUiModel>> = _results

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    val currentTrack: StateFlow<Track?> = playbackStateHolder.state
        .map { it.currentTrack }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val isPlaying: StateFlow<Boolean> = playbackStateHolder.state
        .map { it.isPlaying }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val searchHistory: StateFlow<List<SearchHistoryItem>> = settingsRepository.searchHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _historyResults = MutableStateFlow<List<SearchUiModel>>(emptyList())
    val historyResults: StateFlow<List<SearchUiModel>> = _historyResults

    init {
        _isLoggedIn.value = spClient.isOAuthAuthenticated()

        viewModelScope.launch {
            queryFlow
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { query ->

                    if (query.isBlank()) {
                        _results.value = emptyList()
                        return@collectLatest
                    }

                    _isLoading.value = true

                    try {
                        _results.value = emptyList()

                        coroutineScope {
                            launch {
                                try {
                                    val trackResults = repository.searchByType(query, "track")
                                    if (trackResults.isNotEmpty()) {
                                        val trackUris = trackResults.map { it.uri }
                                        val trackMap = withContext(Dispatchers.IO) {
                                            metadata.getTrackMetadata(trackUris).associateBy { t -> t.uri }
                                        }
                                        val items = trackResults.mapNotNull { result ->
                                            trackMap[result.uri]?.let { track ->
                                                SearchUiModel.TrackItem(result.uri, track)
                                            }
                                        }
                                        if (items.isNotEmpty()) {
                                            _results.update { current ->
                                                current + SearchUiModel.SectionHeader(R.string.search_section_tracks) + items
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("SearchViewModel", "Track search failed", e)
                                }
                            }
                            launch {
                                try {
                                    val artistResults = repository.searchByType(query, "artist")
                                    if (artistResults.isNotEmpty()) {
                                        val items = withContext(Dispatchers.IO) {
                                            artistResults.mapNotNull { result ->
                                                runCatching {
                                                    metadata.getArtistMetadata(result.uri)
                                                }.getOrNull()?.let { artist ->
                                                    SearchUiModel.ArtistItem(result.uri, artist)
                                                }
                                            }
                                        }
                                        if (items.isNotEmpty()) {
                                            _results.update { current ->
                                                current + SearchUiModel.SectionHeader(R.string.search_section_artists) + items
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("SearchViewModel", "Artist search failed", e)
                                }
                            }
                            launch {
                                try {
                                    val albumResults = repository.searchByType(query, "album")
                                    if (albumResults.isNotEmpty()) {
                                        val items = withContext(Dispatchers.IO) {
                                            albumResults.mapNotNull { result ->
                                                runCatching {
                                                    metadata.getAlbumMetadata(result.uri)
                                                }.getOrNull()?.let { album ->
                                                    SearchUiModel.AlbumItem(result.uri, album)
                                                }
                                            }
                                        }
                                        if (items.isNotEmpty()) {
                                            _results.update { current ->
                                                current + SearchUiModel.SectionHeader(R.string.search_section_albums) + items
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("SearchViewModel", "Album search failed", e)
                                }
                            }
                            launch {
                                try {
                                    val playlistResults = repository.searchByType(query, "playlist")
                                    if (playlistResults.isNotEmpty()) {
                                        val items = withContext(Dispatchers.IO) {
                                            playlistResults.mapNotNull { result ->
                                                runCatching {
                                                    metadata.getPlaylistMetadata(result.uri, true)
                                                }.getOrNull()?.let { playlist ->
                                                    SearchUiModel.PlaylistItem(result.uri, playlist)
                                                }
                                            }
                                        }
                                        if (items.isNotEmpty()) {
                                            _results.update { current ->
                                                current + SearchUiModel.SectionHeader(R.string.search_section_playlists) + items
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.w("SearchViewModel", "Playlist search failed", e)
                                }
                            }
                        }

                    } catch (e: Exception) {
                        _results.value = emptyList()
                    } finally {
                        _isLoading.value = false
                    }
                }
        }

        viewModelScope.launch {
            settingsRepository.searchHistory.collect { items ->
                if (items.isEmpty()) {
                    _historyResults.value = emptyList()
                    return@collect
                }
                val results = withContext(Dispatchers.IO) {
                    items.mapNotNull { item ->
                        try {
                            when (item.type) {
                                SearchResultType.TRACK -> {
                                    val tracks = metadata.getTrackMetadata(listOf(item.uri))
                                    tracks.firstOrNull()?.let { track ->
                                        SearchUiModel.TrackItem(item.uri, track)
                                    }
                                }
                                SearchResultType.ARTIST -> {
                                    val artist = metadata.getArtistMetadata(item.uri)
                                    artist?.let { SearchUiModel.ArtistItem(item.uri, it) }
                                }
                                SearchResultType.ALBUM -> {
                                    val album = metadata.getAlbumMetadata(item.uri)
                                    album?.let { SearchUiModel.AlbumItem(item.uri, it) }
                                }
                                SearchResultType.PLAYLIST -> {
                                    val playlist = metadata.getPlaylistMetadata(item.uri, true)
                                    playlist?.let { SearchUiModel.PlaylistItem(item.uri, it) }
                                }
                                else -> null
                            }
                        } catch (e: Exception) {
                            Log.w("SearchViewModel", "Failed to load history metadata for ${item.uri}", e)
                            null
                        }
                    }
                }
                _historyResults.value = results
            }
        }
    }

    fun onQueryChange(query: String){
        queryFlow.value = query
    }

    suspend fun getArtworkUrl(playlist: Playlist): String? {
        return playlist.getCover(metadata)
    }

    fun saveItem(uri: String) {
        viewModelScope.launch {
            if(!spClient.saveItems(arrayOf(uri))){
                Log.w("SearchViewModel", "saveItem failed", )
            }
        }
    }
    fun setTrack(track: Track) {
        playbackStateHolder.setTrack(track)
    }

    fun addToHistory(item: SearchUiModel) {
        val historyItem = when (item) {
            is SearchUiModel.TrackItem -> SearchHistoryItem(item.uri, SearchResultType.TRACK)
            is SearchUiModel.ArtistItem -> SearchHistoryItem(item.uri, SearchResultType.ARTIST)
            is SearchUiModel.AlbumItem -> SearchHistoryItem(item.uri, SearchResultType.ALBUM)
            is SearchUiModel.PlaylistItem -> SearchHistoryItem(item.uri, SearchResultType.PLAYLIST)
            else -> return
        }
        viewModelScope.launch {
            settingsRepository.addSearchHistoryItems(listOf(historyItem))
        }
    }

    fun removeFromHistory(uri: String) {
        viewModelScope.launch {
            settingsRepository.removeSearchHistoryItem(uri)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            settingsRepository.clearSearchHistory()
        }
    }
}

sealed class SearchUiModel {
    abstract val uri: String

    data class SectionHeader(
        @StringRes val titleRes: Int
    ) : SearchUiModel() {
        override val uri: String = "header_$titleRes"
    }

    data class TrackItem(
        override val uri: String,
        val track: Track
    ) : SearchUiModel()

    data class ArtistItem(
        override val uri: String,
        val artist: Artist
    ) : SearchUiModel()

    data class AlbumItem(
        override val uri: String,
        val album: Album
    ) : SearchUiModel()

    data class PlaylistItem(
        override val uri: String,
        val playlist: Playlist
    ) : SearchUiModel()

//    data class ShowItem(
//        override val uri: String,
//        val show: Show
//    ) : SearchUiModel()
//
//    data class EpisodeItem(
//        override val uri: String,
//        val episode: Episode
//    ) : SearchUiModel()
}