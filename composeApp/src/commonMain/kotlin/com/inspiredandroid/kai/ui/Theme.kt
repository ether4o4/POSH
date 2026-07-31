@file:Suppress("DEPRECATION")

package com.inspiredandroid.kai.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

// POSH brand accent is red (the theme is red / white / black). These names are kept
// for source compatibility with the rest of the UI; only their values changed.
val darkPurple = Color(0xFFD32F2F)
val lightPurple = Color(0xFFFF5252)
val gradientBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(darkPurple, lightPurple))

// Animated border gradient colors — red tones.
val gradientPurple = Color(0xFFE53935)
val gradientViolet = Color(0xFFFF1744)
val gradientMagenta = Color(0xFFFF5252)

fun Modifier.handCursor() = pointerHoverIcon(PointerIcon.Hand, overrideDescendants = true)

// POSH dark theme: red accent on a near-black surface with white content. Background is
// 0xFF0A0A0A (not pure black) so it stays distinct from the OledBlack mode, which forces
// pure black via withBlackBackground() and drives the isOledFlavor card styling.
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF3B30),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7F0000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFFF6E6E),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF3A0A0A),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF000000),
    surface = Color(0xFF161616),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFCCCCCC),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    outline = Color(0xFF555555),
    outlineVariant = Color(0xFF333333),
)

fun ColorScheme.withBlackBackground(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
)

val ColorScheme.isOledFlavor: Boolean get() = background == Color.Black

@Composable
fun kaiAdaptiveCardColors(): CardColors = CardDefaults.cardColors(
    containerColor = if (MaterialTheme.colorScheme.isOledFlavor) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    },
)

@Composable
fun kaiAdaptiveCardBorder(): BorderStroke? = if (MaterialTheme.colorScheme.isOledFlavor) {
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
} else {
    null
}

@Composable
fun Modifier.kaiAdaptiveCardSurface(shape: Shape = CardDefaults.shape): Modifier = this
    .clip(shape)
    .background(
        if (MaterialTheme.colorScheme.isOledFlavor) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
    )
    .then(
        if (MaterialTheme.colorScheme.isOledFlavor) {
            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
        } else {
            Modifier
        },
    )

// POSH light theme: red accent on white with black content.
val LightColorScheme = lightColorScheme(
    primary = darkPurple,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFFB71C1C),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF000000),
    onTertiary = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFFE7E7E7),
    onSurfaceVariant = Color(0xFF444444),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    outline = Color(0xFFBBBBBB),
)

// POSH text boxes: black container with cyan text, in both light and dark themes.
val textBoxBackground = Color(0xFF000000)
val textBoxCyan = Color(0xFF00E5FF)

@Composable
fun outlineTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textBoxCyan,
    unfocusedTextColor = textBoxCyan,
    disabledTextColor = textBoxCyan.copy(alpha = 0.5f),
    cursorColor = textBoxCyan,
    focusedContainerColor = textBoxBackground,
    unfocusedContainerColor = textBoxBackground,
    disabledContainerColor = textBoxBackground,
    focusedBorderColor = textBoxCyan,
    unfocusedBorderColor = Color(0xFF444444),
    focusedLabelColor = textBoxCyan,
    unfocusedLabelColor = textBoxCyan.copy(alpha = 0.7f),
    disabledLabelColor = textBoxCyan.copy(alpha = 0.4f),
    focusedPlaceholderColor = textBoxCyan.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = textBoxCyan.copy(alpha = 0.45f),
    selectionColors = TextSelectionColors(
        handleColor = textBoxCyan,
        backgroundColor = textBoxCyan.copy(alpha = 0.35f),
    ),
)

@Composable
fun KaiOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = outlineTextFieldColors(),
    )
}

@Composable
fun KaiClearableTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    KaiOutlinedTextField(
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        trailingIcon = {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.handCursor()
                    .alpha(if (focused && value.isNotEmpty()) 1f else 0f),
                enabled = value.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
@Preview
fun Theme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
    ) {
        content()
    }
}
