package cc.tomko.outify.ui.viewmodel.bottomsheet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.data.dao.PlaylistDao
import cc.tomko.outify.data.database.PlaylistEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreatePlaylistViewModel @Inject constructor(
    private val spClient: SpClient,
    private val playlistDao: PlaylistDao,
) : ViewModel() {

    private val _result = MutableSharedFlow<Result<String>>(extraBufferCapacity = 1)
    val result: SharedFlow<Result<String>> = _result

    fun createPlaylist(name: String, description: String?, isPublic: Boolean, isCollaborative: Boolean) {
        viewModelScope.launch {
            try {
                // TODO: Show some error?
                val playlistId = withContext(Dispatchers.IO) {
                    spClient.createPlaylist(name, description, isPublic, isCollaborative)
                } ?: return@launch

                playlistDao.upsertPlaylist(
                    PlaylistEntity(
                        id = playlistId,
                        uri = "spotify:playlist:$playlistId",
                        ownerUsername = spClient.username() ?: "",
                        revision = "",
                        name = name,
                        description = description ?: "",
                        pictureId = "",
                        isCollaborative = isCollaborative,
                        isDeletedByOwner = false,
                        timestamp = System.currentTimeMillis(),
                    )
                )
                _result.tryEmit(Result.success(playlistId))
            } catch (e: Exception) {
                _result.tryEmit(Result.failure(e))
            }
        }
    }
}
