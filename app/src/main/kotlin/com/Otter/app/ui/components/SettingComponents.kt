package com.Otter.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.Otter.app.data.models.IconShape

/**
 * ModernInfoItem using Material 3 Expressive Ghostish shape for the icon container.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModernInfoItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    showArrow: Boolean = false,
    iconBackgroundColor: Color? = null,
    iconContentColor: Color? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    iconShape: IconShape = IconShape.GHOSTISH,
    iconSize: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val actualShape =
        remember(iconShape) {
            when (iconShape) {
                IconShape.CIRCLE -> CircleShape
                IconShape.ROUNDED_RECTANGLE -> RoundedCornerShape(8.dp)
                IconShape.GHOSTISH -> RoundedPolygonShape(MaterialShapes.Ghostish)
                IconShape.SQUARE -> RoundedCornerShape(0.dp)
            }
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon Container
        Box(
            modifier =
                Modifier
                    .size(iconSize)
                    .clip(actualShape)
                    .background(
                        color = iconBackgroundColor ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = actualShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides (iconContentColor ?: MaterialTheme.colorScheme.primary)) {
                icon()
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Trailing Content
        if (trailingContent != null) {
            trailingContent()
        } else {
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

internal class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private val matrix: Matrix = Matrix(),
) : Shape {
    private val path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        path.reset()
        path.addPath(polygon.toPath().asComposePath())

        val bounds = polygon.calculateBounds()
        val rect = Rect(bounds[0], bounds[1], bounds[2], bounds[3])
        val maxDimension = maxOf(rect.width, rect.height)

        matrix.reset()
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-rect.left, -rect.top)
        path.transform(matrix)

        return Outline.Generic(path)
    }
}

/**
 * Groups items together with adaptive corner shapes for top, middle, and bottom items.
 */
@Composable
fun Material3ExpressiveSettingsGroup(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    items: List<@Composable () -> Unit>,
) {
    val cornerRadius = 24.dp
    val connectionRadius = 4.dp

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEachIndexed { index, item ->
            val shape =
                when {
                    items.size == 1 -> RoundedCornerShape(cornerRadius)
                    index == 0 ->
                        RoundedCornerShape(
                            topStart = cornerRadius,
                            topEnd = cornerRadius,
                            bottomStart = connectionRadius,
                            bottomEnd = connectionRadius,
                        )
                    index == items.size - 1 ->
                        RoundedCornerShape(
                            topStart = connectionRadius,
                            topEnd = connectionRadius,
                            bottomStart = cornerRadius,
                            bottomEnd = cornerRadius,
                        )
                    else -> RoundedCornerShape(connectionRadius)
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(containerColor, shape)
                        .clip(shape),
            ) {
                item()
            }
        }
    }
}

/**
 * Data class for a button option in ConnectedButtonGroup.
 */
data class ButtonOption<T>(
    val value: T,
    val label: String,
)

/**
 * A Material 3 style ButtonGroup using ToggleButton for multi-option selections.
 * Uses official ButtonGroupDefaults for proper connected button shapes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> ConnectedButtonGroup(
    options: List<ButtonOption<T>>,
    selectedValue: T,
    onSelectionChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 40.dp,
    maxWidth: Dp = 280.dp,
) {
    val selectedIndex = options.indexOfFirst { it.value == selectedValue }.coerceAtLeast(0)

    Row(
        modifier =
            modifier
                .widthIn(max = maxWidth)
                .height(buttonHeight),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = index == selectedIndex

            ToggleButton(
                checked = isSelected,
                onCheckedChange = { onSelectionChange(option.value) },
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                shapes =
                    when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                colors =
                    ToggleButtonDefaults.toggleButtonColors(
                        checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
            ) {
                Text(
                    text = option.label,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/**
 * Permission item component showing permission status with check/circle icon.
 * Includes Allow button when permission is not granted.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onAllowClick: (() -> Unit)? = null,
    iconBackgroundColor: Color? = null,
    iconContentColor: Color? = null,
    iconShape: IconShape = IconShape.GHOSTISH,
    modifier: Modifier = Modifier,
) {
    val actualShape =
        remember(iconShape) {
            when (iconShape) {
                IconShape.CIRCLE -> CircleShape
                IconShape.ROUNDED_RECTANGLE -> RoundedCornerShape(8.dp)
                IconShape.GHOSTISH -> RoundedPolygonShape(MaterialShapes.Ghostish)
                IconShape.SQUARE -> RoundedCornerShape(0.dp)
            }
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(actualShape)
                    .background(
                        color = iconBackgroundColor ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = actualShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides (iconContentColor ?: MaterialTheme.colorScheme.primary)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Granted",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        } else if (onAllowClick != null) {
            Button(
                onClick = onAllowClick,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Text("Allow", style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Icon(
                imageVector = Icons.Outlined.Circle,
                contentDescription = "Pending",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * Modifier that adds a pulse/scale animation when the element is pressed.
 */
@Composable
fun Modifier.pulseClick(
    minScale: Float = 0.95f,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) minScale else 1f,
        animationSpec = tween(100),
        label = "pulse",
    )
    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick,
        )
}

/**
 * Modern clickable modifier with pulse effect for list items.
 */
@Composable
fun Modifier.pulseItemClick(onClick: () -> Unit): Modifier =
    this.pulseClick(
        minScale = 0.98f,
        onClick = onClick,
    )
