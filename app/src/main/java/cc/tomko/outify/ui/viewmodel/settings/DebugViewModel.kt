package cc.tomko.outify.ui.viewmodel.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.tomko.outify.core.AuthManager
import cc.tomko.outify.core.SpClient
import cc.tomko.outify.core.model.CurrentUserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DebugViewModel @Inject constructor(
    @ApplicationContext val context:  Context,
    val spClient: SpClient,
    val authManager: AuthManager,
    val json: Json,
) : ViewModel() {
    //region Accounts
    private val _isPlaybackLoggedIn = MutableStateFlow(false)
    val isPlaybackLoggedIn: StateFlow<Boolean> = _isPlaybackLoggedIn.asStateFlow()

    private val _isAccountLoggedIn = MutableStateFlow(false)
    val isAccountLoggedIn: StateFlow<Boolean> = _isAccountLoggedIn.asStateFlow()

    private val _hasAccountsFile = MutableStateFlow(false)
    val hasAccountsFile: StateFlow<Boolean> = _hasAccountsFile.asStateFlow()

    private val _hasPlaybackFile = MutableStateFlow(false)
    val hasPlaybackFile: StateFlow<Boolean> = _hasPlaybackFile.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username.asStateFlow()

    private val _isPremium = MutableStateFlow(true)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    //endregion

    fun loadData() {
        // Accounts
        _isAccountLoggedIn.value = spClient.isOAuthAuthenticated()
        _isPlaybackLoggedIn.value = authManager.hasCachedCredentials()
        _hasAccountsFile.value = File(context.filesDir, "account.json").exists()
        _hasPlaybackFile.value = File(context.filesDir, "credentials.json").exists()

        if(_isAccountLoggedIn.value) {
            fetchProfile()
        }
    }


    private fun fetchProfile() {
        viewModelScope.launch {
            try {
                val profile = spClient.getCurrentUserProfile()
                if (profile == null) {
                    return@launch
                }
                val jsonObject = json.decodeFromString<CurrentUserProfile>(profile)

                val id = jsonObject.id
                val username = jsonObject.displayName
                val imageUrl = jsonObject.images.first().url

                _isPremium.value = jsonObject.product == "premium"

                _userId.value = id
                _username.value = username
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}