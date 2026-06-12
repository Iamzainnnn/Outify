package cc.tomko.outify.ui.components.bottomsheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cc.tomko.outify.ALBUM_COVER_URL
import cc.tomko.outify.core.model.CoverSize
import cc.tomko.outify.core.model.Track
import cc.tomko.outify.core.model.getCover
import cc.tomko.outify.ui.viewmodel.bottomsheet.AddToWidgetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToWidgetBottomSheet(
    track: Track,
    viewModel: AddToWidgetViewModel,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadWidgets()
    }
    val glanceIds by viewModel.glanceIds.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
                onDismiss?.invoke()
            }
        },
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "Add to widget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TrackInfoHeader(tracks = listOf(track))
            }

            items(glanceIds) { glanceId ->
                Text(
                    text = glanceId.toString(),
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                viewModel.saveBitmap(track.album?.getCover(CoverSize.MEDIUM)?.uri?.let { ALBUM_COVER_URL + it }, track.uri)
                                viewModel.addToWidget(track.uri, glanceId)
                            }
                        }
                )
            }
        }
    }
}