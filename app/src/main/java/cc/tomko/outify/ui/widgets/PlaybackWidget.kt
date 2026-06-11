package cc.tomko.outify.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.playback.PlaybackStateHolder
import cc.tomko.outify.ui.components.GlanceSmartImage
import cc.tomko.outify.ui.components.loadBitmap
import javax.inject.Inject

class PlaybackWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    @Inject
    lateinit var playbackStateHolder: PlaybackStateHolder

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val url = "https://i.scdn.co/image/ab67616d00001e026ed9aef791159496b286179f"
        val bitmap = loadBitmap(context, url)

        provideContent {
            val state by playbackStateHolder.state.collectAsState()
            val track = state.currentTrack
            track?.album?.getCover(CoverSize.SMALL)?.uri

            GlanceTheme {
                Scaffold(
                    modifier = GlanceModifier
                        .appWidgetBackground()
                        .background(GlanceTheme.colors.background)
                        .cornerRadius(android.R.dimen.system_app_widget_background_radius)
                ) {
                    Content(
                        itemsList = listOf(
                            MediaItem("1", bitmap),
                            MediaItem("2", bitmap),
                            MediaItem("3", bitmap),
                            MediaItem("4", bitmap),
                            MediaItem("5", bitmap),
                        ),
                        currentTrack = track,
                        isPlaying = state.isPlaying
                    )
                }
            }
        }
    }

    data class MediaItem(val uri: String, val bitmap: Bitmap?)

    @Composable
    private fun Content(itemsList: List<MediaItem>, currentTrack: Track?, isPlaying: Boolean) {
        val size = LocalSize.current
        if (itemsList.isEmpty()) {
            Text("No items to display", modifier = GlanceModifier.fillMaxSize())
            return
        }

        val gap = 8.dp
        val outerPadding = 8.dp
        val maxItemsPerRow = 5
        val scaffoldHorizontalPadding = 32.dp
        val availableWidth = size.width - scaffoldHorizontalPadding - outerPadding * 2

        val possibleItemsPerRow = (availableWidth / 80.dp).toInt()
        val itemsPerRow = possibleItemsPerRow
            .coerceIn(1, maxItemsPerRow)
            .coerceAtMost(itemsList.size)

        val itemSize = (availableWidth - gap * (itemsPerRow - 1)) / itemsPerRow
        val gridRows = itemsList.chunked(itemsPerRow)

        Column(modifier = GlanceModifier.padding(vertical = outerPadding)) {
            LazyColumn(
                modifier = GlanceModifier
                    .padding(outerPadding)
                    .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
                    .background(GlanceTheme.colors.secondaryContainer),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                gridRows.forEachIndexed { rowIndex, rowItems ->
                    item {
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .let { if (rowIndex > 0) it.padding(top = gap) else it }
                        ) {
                            rowItems.forEachIndexed { itemIndex, mediaItem ->
                                if (itemIndex > 0) Spacer(GlanceModifier.width(gap))
                                MediaEntry(item = mediaItem, size = itemSize)
                            }
                        }
                    }
                }
            }

            Spacer(GlanceModifier.height(outerPadding))


            if (currentTrack != null) {
                Row(
                    modifier = GlanceModifier
                        .background(GlanceTheme.colors.tertiaryContainer)
                        .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    // Track thumbnail
                    GlanceSmartImage(
                        bitmap = null,
                        modifier = GlanceModifier
                            .cornerRadius(8.dp),
                        size = 40.dp
                    )

                    Spacer(GlanceModifier.width(8.dp))

                    // Track name + artist — fills remaining horizontal space
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = currentTrack.name,
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onTertiaryContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = currentTrack.artists.joinToString { it.name },
                            maxLines = 1,
                            style = TextStyle(
                                color = GlanceTheme.colors.onTertiaryContainer,
                                fontSize = 11.sp,
                            )
                        )
                    }

                    // Playback controls
                    Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                        Image(
                            provider = ImageProvider(androidx.media3.session.R.drawable.media3_icon_previous),
                            contentDescription = "Previous",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(28.dp).clickable { /* previous */ }
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        val playPauseIcon =
                            if (isPlaying) androidx.media3.session.R.drawable.media3_icon_pause else androidx.media3.session.R.drawable.media3_icon_play

                        Image(
                            provider = ImageProvider(playPauseIcon),
                            contentDescription = "Play / Pause",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(32.dp).clickable { /* play/pause */ }
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        Image(
                            provider = ImageProvider(androidx.media3.session.R.drawable.media3_icon_next),
                            contentDescription = "Next",
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.onTertiaryContainer),
                            modifier = GlanceModifier.size(28.dp).clickable { /* next */ }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MediaEntry(item: MediaItem, size: Dp) {
        GlanceSmartImage(
            item.bitmap,
            modifier = GlanceModifier
                .cornerRadius(android.R.dimen.system_app_widget_inner_radius)
                .clickable { println("Clicked on ${item.uri}") },
            size = size
        )
    }
}