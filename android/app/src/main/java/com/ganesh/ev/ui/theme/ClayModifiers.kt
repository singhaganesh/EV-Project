package com.ganesh.ev.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Draws an outer soft shadow behind the composable — the main depth effect. */
fun Modifier.clayOuterShadow(
        shadowColor: Color = ClayShadowDark,
        cornerRadius: Dp = 24.dp,
        offsetX: Dp = 4.dp,
        offsetY: Dp = 6.dp,
        blurRadius: Dp = 12.dp
): Modifier =
        this.drawBehind {
            val paint =
                    Paint().also { p ->
                        p.asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = shadowColor.toArgb()
                            setShadowLayer(
                                    blurRadius.toPx(),
                                    offsetX.toPx(),
                                    offsetY.toPx(),
                                    shadowColor.toArgb()
                            )
                        }
                    }
            drawIntoCanvas { canvas ->
                canvas.drawRoundRect(
                        left = 0f,
                        top = 0f,
                        right = size.width,
                        bottom = size.height,
                        radiusX = cornerRadius.toPx(),
                        radiusY = cornerRadius.toPx(),
                        paint = paint
                )
            }
        }

/** Draws an inner shadow inside the composable — creates the clay depth illusion. */
fun Modifier.clayInnerShadow(
        shadowColor: Color = ClayShadowDark,
        cornerRadius: Dp = 24.dp,
        offsetX: Dp = 3.dp,
        offsetY: Dp = 3.dp,
        blurRadius: Dp = 6.dp
): Modifier =
        this.drawWithContent {
            drawContent()

            val paint =
                    Paint().also { p ->
                        p.asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = shadowColor.toArgb()
                            setShadowLayer(
                                    blurRadius.toPx(),
                                    offsetX.toPx(),
                                    offsetY.toPx(),
                                    shadowColor.toArgb()
                            )
                        }
                    }

            val clipPath =
                    Path().apply {
                        addRoundRect(
                                RoundRect(
                                        rect = Rect(Offset.Zero, size),
                                        cornerRadius = CornerRadius(cornerRadius.toPx())
                                )
                        )
                    }

            clipPath(clipPath) {
                // Draw a large rect outside the bounds, so only its shadow lands inside
                drawIntoCanvas { canvas ->
                    canvas.drawRoundRect(
                            left = -size.width,
                            top = -size.height,
                            right = 0f,
                            bottom = 0f,
                            radiusX = cornerRadius.toPx(),
                            radiusY = cornerRadius.toPx(),
                            paint = paint
                    )
                }
            }
        }

/** Inner highlight on the top-left — simulates light hitting the clay surface. */
fun Modifier.clayHighlight(
        highlightColor: Color = ClayShadowLight,
        cornerRadius: Dp = 24.dp,
        offsetX: Dp = (-3).dp,
        offsetY: Dp = (-3).dp,
        blurRadius: Dp = 6.dp
): Modifier =
        this.drawWithContent {
            drawContent()

            val paint =
                    Paint().also { p ->
                        p.asFrameworkPaint().apply {
                            isAntiAlias = true
                            color = highlightColor.toArgb()
                            setShadowLayer(
                                    blurRadius.toPx(),
                                    offsetX.toPx(),
                                    offsetY.toPx(),
                                    highlightColor.toArgb()
                            )
                        }
                    }

            val clipPath =
                    Path().apply {
                        addRoundRect(
                                RoundRect(
                                        rect = Rect(Offset.Zero, size),
                                        cornerRadius = CornerRadius(cornerRadius.toPx())
                                )
                        )
                    }

            clipPath(clipPath) {
                drawIntoCanvas { canvas ->
                    canvas.drawRoundRect(
                            left = size.width,
                            top = size.height,
                            right = size.width * 2,
                            bottom = size.height * 2,
                            radiusX = cornerRadius.toPx(),
                            radiusY = cornerRadius.toPx(),
                            paint = paint
                    )
                }
            }
        }

/** Full claymorphism surface effect — combines outer shadow, inner shadow, and highlight. */
fun Modifier.claySurface(
        cornerRadius: Dp = 24.dp,
        shadowElevation: Dp = 8.dp,
        outerShadowColor: Color = ClayShadowDark,
        innerShadowColor: Color = ClayShadowDark,
        highlightColor: Color = ClayShadowLight
): Modifier =
        this.clayOuterShadow(
                        shadowColor = outerShadowColor,
                        cornerRadius = cornerRadius,
                        offsetX = 4.dp,
                        offsetY = shadowElevation,
                        blurRadius = shadowElevation * 1.5f
                )
                .clayInnerShadow(
                        shadowColor = innerShadowColor,
                        cornerRadius = cornerRadius,
                        offsetX = 2.dp,
                        offsetY = 2.dp,
                        blurRadius = 4.dp
                )
                .clayHighlight(
                        highlightColor = highlightColor,
                        cornerRadius = cornerRadius,
                        offsetX = (-2).dp,
                        offsetY = (-2).dp,
                        blurRadius = 4.dp
                )

/** Inset clay effect — for text fields and inputs that look pressed in. */
fun Modifier.clayInset(
        cornerRadius: Dp = 24.dp,
        shadowColor: Color = ClayShadowDarkStrong
): Modifier =
        this.clayInnerShadow(
                        shadowColor = shadowColor,
                        cornerRadius = cornerRadius,
                        offsetX = 2.dp,
                        offsetY = 2.dp,
                        blurRadius = 4.dp
                )
                .clayHighlight(
                        highlightColor = ClayShadowLight,
                        cornerRadius = cornerRadius,
                        offsetX = (-2).dp,
                        offsetY = (-2).dp,
                        blurRadius = 3.dp
                )
