package com.ganesh.ev.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════
//  ClayCard — replaces Material Card
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayCard(
        modifier: Modifier = Modifier,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        cornerRadius: Dp = 24.dp,
        content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(modifier = modifier.claySurface(cornerRadius = cornerRadius)) {
        Column(
                modifier =
                        Modifier.clip(shape)
                                .background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        containerColor,
                                                                        containerColor.copy(
                                                                                alpha = 0.92f
                                                                        )
                                                                )
                                                )
                                )
                                .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        shape = shape
                                )
                                .padding(16.dp),
                content = content
        )
    }
}

@Composable
fun ClayClickableCard(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        containerColor: Color = MaterialTheme.colorScheme.surface,
        cornerRadius: Dp = 24.dp,
        content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
            modifier =
                    modifier.claySurface(cornerRadius = cornerRadius)
                            .clip(shape)
                            .clickable(onClick = onClick)
    ) {
        Column(
                modifier =
                        Modifier.background(
                                        brush =
                                                Brush.verticalGradient(
                                                        colors =
                                                                listOf(
                                                                        containerColor,
                                                                        containerColor.copy(
                                                                                alpha = 0.92f
                                                                        )
                                                                )
                                                )
                                )
                                .border(
                                        width = 1.5.dp,
                                        color = Color.White.copy(alpha = 0.5f),
                                        shape = shape
                                )
                                .padding(16.dp),
                content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ClayButton — replaces Material Button
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        containerColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.onPrimary,
        cornerRadius: Dp = 20.dp,
        content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val elevation by
            animateDpAsState(
                    targetValue = if (isPressed) 2.dp else 8.dp,
                    animationSpec = spring(),
                    label = "clayButtonElevation"
            )
    val shape = RoundedCornerShape(cornerRadius)
    val actualColor = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)

    Box(
            modifier =
                    modifier.claySurface(cornerRadius = cornerRadius, shadowElevation = elevation)
                            .clip(shape)
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    actualColor.copy(alpha = 0.95f),
                                                                    actualColor
                                                            )
                                            )
                            )
                            .border(
                                    width = 1.5.dp,
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = shape
                            )
                            .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    enabled = enabled,
                                    onClick = onClick
                            )
                            .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge.copy(color = contentColor)) {
            Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
            )
        }
    }
}

@Composable
fun ClayOutlinedButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        borderColor: Color = MaterialTheme.colorScheme.primary,
        contentColor: Color = MaterialTheme.colorScheme.primary,
        cornerRadius: Dp = 20.dp,
        content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
            modifier =
                    modifier.claySurface(cornerRadius = cornerRadius, shadowElevation = 4.dp)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                    width = 2.dp,
                                    color =
                                            if (enabled) borderColor
                                            else borderColor.copy(alpha = 0.4f),
                                    shape = shape
                            )
                            .clickable(enabled = enabled, onClick = onClick)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
    ) {
        ProvideTextStyle(
                value =
                        MaterialTheme.typography.labelLarge.copy(
                                color =
                                        if (enabled) contentColor
                                        else contentColor.copy(alpha = 0.4f)
                        )
        ) {
            Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  ClayTextField — replaces OutlinedTextField
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayTextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier = Modifier,
        label: @Composable (() -> Unit)? = null,
        placeholder: @Composable (() -> Unit)? = null,
        enabled: Boolean = true,
        singleLine: Boolean = true,
        keyboardOptions: androidx.compose.foundation.text.KeyboardOptions =
                androidx.compose.foundation.text.KeyboardOptions.Default,
        cornerRadius: Dp = 20.dp
) {
    val shape = RoundedCornerShape(cornerRadius)
    OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.clayInset(cornerRadius = cornerRadius),
            label = label,
            placeholder = placeholder,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            shape = shape,
            colors =
                    OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor =
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            cursorColor = MaterialTheme.colorScheme.primary
                    )
    )
}

// ═══════════════════════════════════════════════════════════════
//  ClayTopBar — replaces TopAppBar
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClayTopBar(
        title: String,
        navigationIcon: @Composable (() -> Unit)? = null,
        actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .clayOuterShadow(
                                    cornerRadius = 0.dp,
                                    offsetY = 4.dp,
                                    blurRadius = 8.dp,
                                    shadowColor = ClayShadowDark.copy(alpha = 0.15f)
                            )
    ) {
        TopAppBar(
                title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { navigationIcon?.invoke() },
                actions = actions,
                colors =
                        TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.primary
                        )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ClayBottomBar — replaces NavigationBar
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayBottomBar(content: @Composable RowScope.() -> Unit) {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .clayOuterShadow(
                                    cornerRadius = 0.dp,
                                    offsetY = (-4).dp,
                                    blurRadius = 12.dp,
                                    shadowColor = ClayShadowDark.copy(alpha = 0.15f)
                            )
    ) {
        NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier =
                        Modifier.border(
                                width = 0.5.dp,
                                color = Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
                        ),
                content = content
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ClayChip — for status badges
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayChip(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
            modifier =
                    modifier.claySurface(cornerRadius = 12.dp, shadowElevation = 3.dp)
                            .clip(shape)
                            .background(color.copy(alpha = 0.15f))
                            .border(width = 1.dp, color = color.copy(alpha = 0.3f), shape = shape)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) { Text(text = text, color = color, style = MaterialTheme.typography.labelMedium) }
}

// ═══════════════════════════════════════════════════════════════
//  ClayIconButton — circular clay button
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayIconButton(
        onClick: () -> Unit,
        icon: ImageVector,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        tint: Color = MaterialTheme.colorScheme.primary
) {
    Box(
            modifier =
                    modifier.size(48.dp)
                            .claySurface(cornerRadius = 24.dp, shadowElevation = 4.dp)
                            .clip(CircleShape)
                            .background(
                                    brush =
                                            Brush.verticalGradient(
                                                    colors =
                                                            listOf(
                                                                    MaterialTheme.colorScheme
                                                                            .surface,
                                                                    MaterialTheme.colorScheme
                                                                            .surface.copy(
                                                                            alpha = 0.9f
                                                                    )
                                                            )
                                            )
                            )
                            .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                            )
                            .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
    ) {
        Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  ClayDivider — subtle clay inset line
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
            modifier = modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
    )
}

// ═══════════════════════════════════════════════════════════════
//  ClayProgressIndicator
// ═══════════════════════════════════════════════════════════════

@Composable
fun ClayProgressIndicator(
        modifier: Modifier = Modifier,
        color: Color = MaterialTheme.colorScheme.primary
) {
    CircularProgressIndicator(
            modifier = modifier.size(48.dp),
            color = color,
            strokeWidth = 4.dp
    )
}

