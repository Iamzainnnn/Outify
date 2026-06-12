package cc.tomko.outify

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.media3.common.util.UnstableApi
import cc.tomko.outify.core.Spirc.SpircWrapper
import cc.tomko.outify.core.spirc.SpircController
import cc.tomko.outify.data.database.AppDatabase
import cc.tomko.outify.services.PlaybackService
import cc.tomko.outify.ui.viewmodel.detail.DetailViewModelStore
import cc.tomko.outify.ui.viewmodel.detail.setDetailViewModelStore
import cc.tomko.outify.utils.ExceptionCollector
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

const val ALBUM_COVER_URL: String = "https://i.scdn.co/image/"
fun widgetMediaPreference(id: GlanceId) =
    stringPreferencesKey("widget_media_$id")

@HiltAndroidApp
class OutifyApplication : Application() {
    lateinit var database: AppDatabase
        private set

    @Inject
    lateinit var spircController: SpircController

    @Inject
    lateinit var spircWrapper: SpircWrapper

    @Inject
    lateinit var detailViewModelStore: DetailViewModelStore

    @Inject
    lateinit var exceptionCollector: ExceptionCollector

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        exceptionCollector.install()
        database = AppDatabase.getInstance(this)

        setDetailViewModelStore(detailViewModelStore)

        System.loadLibrary("librespot_ffi")
        LibrespotFfi.libInit(applicationContext)

        // Starting playback service
        val intent = Intent(this, PlaybackService::class.java)
        ContextCompat.startForegroundService(this, intent)

        spircController.start()
        spircWrapper.setRestartCallback { spircController.restart() }
    }
}