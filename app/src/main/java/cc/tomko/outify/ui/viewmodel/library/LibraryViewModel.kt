package cc.tomko.outify.ui.viewmodel.library

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.UserProfile
import cc.tomko.outify.core.model.Playlist
import cc.tomko.outify.core.model.PlaylistFolder
import cc.tomko.outify.core.model.Profile
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.data.metadata.Metadata
import cc.tomko.outify.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class LibraryState(
    val playlists: List<Playlist> = emptyList(),
    val folders: List<PlaylistFolder> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val metadata: Metadata,
    private val json: Json,
    private val userProfile: UserProfile,
    private val settingsRepository: SettingsRepository,
    private val spClient: SpClient,
    private val spirc: SpircWrapper,
) : ViewModel() {

    init {
        viewModelScope.launch {
            metadata.syncLikedPlaylists()
        }
    }
    private val _headerArtwork = mutableStateOf<String?>(null)
    val headerArtwork = _headerArtwork

    private val playlistUris = MutableStateFlow<List<String>>(emptyList())
    private var playlistsLoaded = false

    private val _authors = MutableStateFlow<Map<String, Profile>>(emptyMap())
    val authors: StateFlow<Map<String, Profile>> = _authors
    val isRefreshing = MutableStateFlow(false)

    private val artworkCache = mutableMapOf<String, String?>()
    private val authorsCache = mutableMapOf<String, List<Profile>>()

    private val foldersFlow = settingsRepository.folders

    private val _error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val playlists: StateFlow<List<Playlist>> =
        playlistUris
            .flatMapLatest { uris ->
                if (uris.isEmpty()) {
                    flow { emit(emptyList()) }
                } else {
                    metadata.observePlaylists(uris)
                }
            }
            .debounce(50)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val libraryState: StateFlow<LibraryState> = combine(
        playlists,
        foldersFlow,
        _error,
    ) { playlists, folders, error ->
        LibraryState(
            playlists = playlists,
            folders = folders,
            error = error,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        LibraryState()
    )

    suspend fun getArtworkUrl(playlist: Playlist): String? {
        artworkCache[playlist.uri]?.let { return it }
        val url = playlist.getCover(metadata)
        artworkCache[playlist.uri] = url
        return url
    }

    fun loadPlaylistUris(force: Boolean = false) {
        if (!force && playlistsLoaded) return
        viewModelScope.launch {
            isRefreshing.value = true
            _error.value = null

            val cached = settingsRepository.cachedUris.first()
            if (cached.isNotEmpty()) {
                playlistUris.value = cached
            }

            runCatching {
                metadata.getPlaylistUris()
            }.onSuccess { uris ->
                playlistUris.value = uris
                settingsRepository.saveCachedUris(uris)
                playlistsLoaded = true
            }.onFailure { e ->
                Log.w("LibraryViewModel", "Failed to fetch playlist URIs", e)
                _error.value = e.message ?: "Failed to load library"
            }

            isRefreshing.value = false
        }
    }

    fun loadHeaderArtwork(playlists: List<Playlist>) {
        if (_headerArtwork.value != null) return
        if (playlists.isNotEmpty()) {
            viewModelScope.launch {
                _headerArtwork.value =
                    getArtworkUrl(playlists.random())
            }
        }
    }

    suspend fun getAuthors(playlist: Playlist): List<Profile> = coroutineScope {
        authorsCache[playlist.uri]?.let { return@coroutineScope it }

        val ids = playlist.contents
            .map { it.attributes.addedBy }
            .distinct()

        val profiles = ids.map { id ->
            async(Dispatchers.IO) {
                _authors.value[id]?.let { return@async it }

                val jsonRaw = try {
                    userProfile.getUserProfile(id)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: return@async null

                val profile = try {
                    json.decodeFromString<Profile>(jsonRaw)
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@async null
                }

                _authors.update { current -> current + (id to profile) }
                profile
            }
        }.awaitAll()
            .filterNotNull()

        authorsCache[playlist.uri] = profiles
        profiles
    }

    fun retry() {
        viewModelScope.launch {
            spirc.restart()
            refresh()
        }
    }

    fun refresh(){
        playlistsLoaded = false
        artworkCache.clear()
        authorsCache.clear()
        loadPlaylistUris(force = true)
    }

    fun createFolder(folder: PlaylistFolder) {
        viewModelScope.launch {
            val current = settingsRepository.folders.first()
            settingsRepository.saveFolders(current + folder)
        }
    }

    fun updateFolder(folder: PlaylistFolder) {
        viewModelScope.launch {
            val current = settingsRepository.folders.first()
            settingsRepository.saveFolders(current.map { if (it.id == folder.id) folder else it })
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            val current = settingsRepository.folders.first()
            settingsRepository.saveFolders(current.filter { it.id != folderId })
        }
    }

    fun movePlaylistToFolder(playlistUri: String, folderId: String?) {
        viewModelScope.launch {
            val current = settingsRepository.folders.first()
            val updated = current.map { folder ->
                if (folder.id == folderId) {
                    if (playlistUri in folder.playlistIds) folder
                    else folder.copy(playlistIds = folder.playlistIds + playlistUri)
                } else {
                    folder.copy(playlistIds = folder.playlistIds - playlistUri)
                }
            }
            settingsRepository.saveFolders(updated)
        }
    }
}
