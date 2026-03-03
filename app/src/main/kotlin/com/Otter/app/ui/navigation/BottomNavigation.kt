package com.Otter.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    object Playlists : BottomNavItem(
        "playlists",
        "Playlists",
        Icons.Filled.VideoLibrary,
        Icons.Outlined.VideoLibrary
    )

    object Downloads : BottomNavItem(
        "downloads",
        "Downloads",
        Icons.Filled.Download,
        Icons.Outlined.Download
    )

    object Settings : BottomNavItem(
        "settings",
        "Settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OtterBottomNavigation(
    selectedRoute: String,
    onItemSelected: (String) -> Unit,
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val items =
        listOf(
            BottomNavItem.Downloads,
            BottomNavItem.Playlists,
            BottomNavItem.Settings,
        )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .padding(horizontal = 0.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = selectedRoute == item.route

                    val scale by animateFloatAsState(
                        targetValue = 1.0f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow,
                            ),
                        label = "scale",
                    )

                    val bgColor by animateColorAsState(
                        targetValue = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            Color.Transparent,
                        animationSpec =
                            tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing,
                            ),
                        label = "bgColor",
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec =
                            tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing,
                            ),
                        label = "iconColor",
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec =
                            tween(
                                durationMillis = 400,
                                easing = FastOutSlowInEasing,
                            ),
                        label = "textColor",
                    )

                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onItemSelected(item.route) }
                                ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(50))
                                .background(bgColor)
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(350)) + 
                                        scaleIn(
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessLow
                                            ),
                                            initialScale = 0.7f
                                        )) togetherWith
                                    (fadeOut(animationSpec = tween(150)) + 
                                        scaleOut(
                                            animationSpec = tween(150),
                                            targetScale = 0.7f
                                        ))
                                },
                                label = "iconAnimation"
                            ) { selected ->
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(20.dp),
                                    tint = iconColor,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.title,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                ),
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}
