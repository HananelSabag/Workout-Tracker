package com.hananel.workouttracker2.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid-glass design system.
 *
 * Every panel in the app is one of three tiers:
 *  - [GlassPanel]   — the frosted plate: translucent fill, bright top-edge highlight, hairline border.
 *  - [GlassCard]    — same plate with a colored accent wash for stateful content (active / done).
 *  - [AppBackground] — the gradient + light-blob backdrop the glass refracts. Without it the glass
 *    has nothing to sit on and just looks like a flat grey box, which is what the old UI did.
 *
 * The tint values differ between light and dark because glass over a bright surface needs *more*
 * white and a darker border, while glass over a dark surface needs a faint white veil.
 */

// ─── Brand palette ───────────────────────────────────────────────────────────

val Electric = Color(0xFF3B82F6)
val ElectricDeep = Color(0xFF1D4ED8)
val Violet = Color(0xFF8B5CF6)
val VioletDeep = Color(0xFF6D28D9)
val Mint = Color(0xFF10B981)
val MintDeep = Color(0xFF047857)
val Amber = Color(0xFFF59E0B)
val AmberDeep = Color(0xFFB45309)
val Rose = Color(0xFFF43F5E)
val Cyan = Color(0xFF06B6D4)

/** Primary CTA gradient — used on every "do the thing" button so actions read as one family. */
val AccentGradient = listOf(Electric, Violet)
val SuccessGradient = listOf(Mint, Color(0xFF059669))
val RestGradient = listOf(Amber, Color(0xFFEA580C))

@Immutable
data class GlassColors(
    val fill: Brush,
    val border: Brush,
    val hairline: Color,
    val elevatedFill: Brush,
    val onGlass: Color,
    val onGlassMuted: Color,
    val scrim: Color,
    val isDark: Boolean
)

@Composable
fun rememberGlassColors(): GlassColors {
    val dark = MaterialTheme.colorScheme.background.luminanceIsDark()
    return remember(dark) {
        if (dark) {
            GlassColors(
                fill = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.05f))
                ),
                border = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f))
                ),
                hairline = Color.White.copy(alpha = 0.10f),
                elevatedFill = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.08f))
                ),
                onGlass = Color(0xFFF8FAFC),
                onGlassMuted = Color(0xFF94A3B8),
                scrim = Color.Black.copy(alpha = 0.45f),
                isDark = true
            )
        } else {
            GlassColors(
                fill = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.88f), Color.White.copy(alpha = 0.62f))
                ),
                border = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.95f), Color(0xFF94A3B8).copy(alpha = 0.28f))
                ),
                hairline = Color(0xFF0F172A).copy(alpha = 0.07f),
                elevatedFill = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.97f), Color.White.copy(alpha = 0.80f))
                ),
                onGlass = Color(0xFF0F172A),
                onGlassMuted = Color(0xFF64748B),
                scrim = Color(0xFF0F172A).copy(alpha = 0.25f),
                isDark = false
            )
        }
    }
}

private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

// ─── Backdrop ────────────────────────────────────────────────────────────────

/**
 * Full-screen gradient with two soft light blobs. The blobs are what make the glass panels read as
 * translucent — they bleed color through the semi-transparent fills as the user scrolls past.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = rememberGlassColors()
    val base = if (glass.isDark) {
        listOf(Color(0xFF0B1120), Color(0xFF111C34), Color(0xFF0B1120))
    } else {
        listOf(Color(0xFFF1F5FF), Color(0xFFEEF2FF), Color(0xFFF8FAFC))
    }
    val blobA = if (glass.isDark) Electric.copy(alpha = 0.28f) else Electric.copy(alpha = 0.20f)
    val blobB = if (glass.isDark) Violet.copy(alpha = 0.24f) else Violet.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(base))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobA, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.06f),
                        radius = size.width * 0.85f
                    ),
                    radius = size.width * 0.85f,
                    center = Offset(size.width * 0.12f, size.height * 0.06f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blobB, Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.42f),
                        radius = size.width * 0.75f
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.95f, size.height * 0.42f)
                )
            },
        content = content
    )
}

// ─── Glass modifiers & containers ────────────────────────────────────────────

/**
 * Paints a frosted plate: translucent fill + hairline gradient border.
 * [tint] optionally washes the plate with a state color (blue = active, green = done…).
 */
fun Modifier.liquidGlass(
    glass: GlassColors,
    shape: Shape,
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.16f,
    elevated: Boolean = false,
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(if (elevated) glass.elevatedFill else glass.fill, shape)
    .then(
        if (tint.isSpecified()) Modifier.background(
            Brush.verticalGradient(
                listOf(tint.copy(alpha = tintAlpha), tint.copy(alpha = tintAlpha * 0.35f))
            ),
            shape
        ) else Modifier
    )
    .border(
        width = borderWidth,
        brush = if (tint.isSpecified()) Brush.verticalGradient(
            listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.12f))
        ) else glass.border,
        shape = shape
    )

private fun Color.isSpecified(): Boolean = this != Color.Unspecified

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.16f,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = rememberGlassColors()
    Box(
        modifier = modifier.liquidGlass(glass, shape, tint, tintAlpha, elevated),
        content = content
    )
}

/** Clickable variant — keeps the ripple inside the rounded shape. */
@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = Color.Unspecified,
    tintAlpha: Float = 0.16f,
    elevated: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = rememberGlassColors()
    val base = Modifier.liquidGlass(glass, shape, tint, tintAlpha, elevated)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = Color.Transparent
        ) {
            Box(modifier = base, content = content)
        }
    } else {
        Box(modifier = modifier.then(base), content = content)
    }
}

/** Solid-ish sheet background for bottom sheets, which sit above the blurred backdrop. */
@Composable
fun glassSheetColor(): Color {
    val glass = rememberGlassColors()
    return if (glass.isDark) Color(0xFF141E33) else Color(0xFFF8FAFF)
}

/** Softly tinted container used for stat chips and inline badges. */
@Composable
fun chipTint(color: Color): Color {
    val glass = rememberGlassColors()
    return if (glass.isDark) color.copy(alpha = 0.22f)
    else color.copy(alpha = 0.14f).compositeOver(Color.White)
}
