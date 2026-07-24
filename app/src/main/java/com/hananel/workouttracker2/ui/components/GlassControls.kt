package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.ui.theme.AccentGradient
import com.hananel.workouttracker2.ui.theme.chipTint
import com.hananel.workouttracker2.ui.theme.glassSheetColor
import com.hananel.workouttracker2.ui.theme.rememberGlassColors

/**
 * Shared controls so every sheet, form and button in the app looks like it came from the same
 * place. Before this, each screen hand-rolled its own OutlinedTextField + Button pairs and they
 * all drifted apart.
 */

// ─── Buttons ─────────────────────────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    gradient: List<Color> = AccentGradient,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    fontSize: Int = 15
) {
    val shape = RoundedCornerShape(16.dp)
    val pressScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.99f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "btnScale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height).scale(pressScale),
        shape = shape,
        color = Color.Transparent,
        shadowElevation = if (enabled) 6.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    if (enabled) Brush.horizontalGradient(gradient)
                    else Brush.horizontalGradient(gradient.map { it.copy(alpha = 0.35f) })
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                }
                Text(
                    text = text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.sp
                )
            }
        }
    }
}

/** Low-emphasis action that still sits on glass — used for Cancel and secondary paths. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = Color.Unspecified,
    height: Dp = 52.dp
) {
    val glass = rememberGlassColors()
    val shape = RoundedCornerShape(16.dp)
    val tint = if (contentColor == Color.Unspecified) glass.onGlass else contentColor

    Surface(
        onClick = onClick,
        modifier = modifier.height(height),
        shape = shape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(glass.fill, shape)
                .border(1.dp, glass.border, shape),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Text(text = text, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

// ─── Chips & badges ──────────────────────────────────────────────────────────

@Composable
fun StatChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    /** Forces LTR on the label. "5 × 10-12" is a formula, not prose — bidi reorders it to
     *  "10-12 × 5" in Hebrew, which reads as the wrong sets/reps pair. */
    numeric: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val container = chipTint(tint)
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(container)
                .border(1.dp, tint.copy(alpha = 0.25f), shape)
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                color = tint,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                style = if (numeric) LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                else LocalTextStyle.current
            )
        }
    }
    if (onClick != null) {
        Surface(onClick = onClick, color = Color.Transparent, shape = shape, modifier = modifier) { body() }
    } else {
        Box(modifier) { body() }
    }
}

// ─── Sheet shell ─────────────────────────────────────────────────────────────

/**
 * Standard bottom sheet: rounded glass container, icon + title header, and whatever content the
 * caller needs. Every dialog in the app goes through this so they share padding and rhythm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accent: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    val glass = rememberGlassColors()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accentColor = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassSheetColor(),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 6.dp)
                    .size(width = 44.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(glass.onGlassMuted.copy(alpha = 0.35f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(chipTint(accentColor))
                            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = glass.onGlass
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.onGlassMuted
                        )
                    }
                }
            }
            content()
        }
    }
}

// ─── Inputs ──────────────────────────────────────────────────────────────────

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    val glass = rememberGlassColors()
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = glass.onGlassMuted) } },
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        },
        suffix = suffix?.let { { Text(it, color = glass.onGlassMuted) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = glass.onGlassMuted.copy(alpha = 0.35f),
            focusedContainerColor = glass.onGlass.copy(alpha = 0.04f),
            unfocusedContainerColor = glass.onGlass.copy(alpha = 0.03f),
            focusedTextColor = glass.onGlass,
            unfocusedTextColor = glass.onGlass,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = glass.onGlassMuted
        )
    )
}

/**
 * Number entry as −/value/+ instead of a keyboard field. Sets, reps and rest seconds are all small
 * integers the user nudges; making them typed fields was the single biggest source of the "forms
 * feel awful" complaint.
 */
@Composable
fun NumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    range: IntRange = 1..99,
    suffix: String = "",
    icon: ImageVector? = null,
    accent: Color = Color.Unspecified
) {
    val glass = rememberGlassColors()
    val accentColor = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(glass.fill, shape)
            .border(1.dp, glass.border, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = glass.onGlassMuted
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                enabled = value - step >= range.first,
                accent = accentColor,
                onClick = { onValueChange((value - step).coerceIn(range)) }
            )
            Text(
                text = if (suffix.isBlank()) "$value" else "$value$suffix",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                color = glass.onGlass,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            StepperButton(
                icon = Icons.Default.Add,
                enabled = value + step <= range.last,
                accent = accentColor,
                onClick = { onValueChange((value + step).coerceIn(range)) }
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (enabled) chipTint(accent) else chipTint(accent).copy(alpha = 0.25f),
        animationSpec = tween(200),
        label = "stepBg"
    )
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = bg
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .size(19.dp)
                    .alpha(if (enabled) 1f else 0.4f)
            )
        }
    }
}

/** Decimal stepper for weights: ±2.5 kg taps plus a free-typed field for anything unusual. */
@Composable
fun WeightStepper(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    unit: String,
    modifier: Modifier = Modifier,
    step: Float = 2.5f,
    accent: Color = Color.Unspecified
) {
    val glass = rememberGlassColors()
    val accentColor = if (accent == Color.Unspecified) MaterialTheme.colorScheme.secondary else accent
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(glass.fill, shape)
            .border(1.dp, glass.border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = glass.onGlassMuted
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                enabled = value - step >= 0f,
                accent = accentColor,
                onClick = { onValueChange((value - step).coerceAtLeast(0f)) }
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatWeightValue(value),
                    style = MaterialTheme.typography.displaySmall,
                    color = glass.onGlass
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.titleSmall,
                    color = glass.onGlassMuted,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
            StepperButton(
                icon = Icons.Default.Add,
                enabled = value + step <= 999f,
                accent = accentColor,
                onClick = { onValueChange(value + step) }
            )
        }
    }
}

fun formatWeightValue(weight: Float): String =
    if (weight % 1f == 0f) weight.toInt().toString() else "%.1f".format(weight)
