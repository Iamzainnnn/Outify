package cc.tomko.outify.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ITEM_WIDTH = 56.dp
private val ITEM_HEIGHT = 44.dp
private val ROW_PADDING_H = 6.dp
private val ROW_PADDING_V = 4.dp

@Composable
fun FloatingOutifyBottomNav(
    items: List<NavDestination>,
    selectedId: String?,
    onItemSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
    tonalElevation: Dp = 0.dp,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showLabels: Boolean = false,
) {
    val selectedIndex = items.indexOfFirst { it.id == selectedId }

    Surface(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 20.dp),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box {
            if (selectedIndex >= 0) {
                val animatedOffsetX by animateDpAsState(
                    targetValue = ROW_PADDING_H + ITEM_WIDTH * selectedIndex.toFloat(),
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "indicator",
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX, y = ROW_PADDING_V)
                        .size(ITEM_WIDTH, ITEM_HEIGHT)
                        .clip(RoundedCornerShape(20.dp))
                        .background(selectedColor.copy(alpha = 0.15f)),
                )
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = ROW_PADDING_H, vertical = ROW_PADDING_V),
                horizontalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = item.id == selectedId
                    key(item.id) {
                        FloatingNavItem(
                            destination = item,
                            selected = isSelected,
                            onClick = { onItemSelected(item) },
                            selectedColor = selectedColor,
                            unselectedColor = unselectedColor,
                            showLabel = showLabels,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    destination: NavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color,
    unselectedColor: Color,
    showLabel: Boolean,
) {
    val iconTint by animateColorAsState(
        if (selected) selectedColor else unselectedColor,
        label = "navIcon",
    )

    Box(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics { contentDescription = destination.label },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides iconTint) {
            if (showLabel) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(24.dp)) {
                        destination.icon()
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = destination.label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = iconTint,
                    )
                }
            } else {
                Box(modifier = Modifier.size(24.dp)) {
                    destination.icon()
                }
            }
        }
    }
}
