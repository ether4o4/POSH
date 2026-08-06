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

// POSH uses one strict red, white, and black palette on every platform.
val brandBackground = Color.Black

// Both theme modes resolve to the same branded scheme. Near-black and dark-red
// containers add hierarchy without introducing off-palette hues.
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
    surface = brandBackground,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color(0xFFD0D0D0),
    background = brandBackground,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFCF2E2E),
    outlineVariant = Color(0xFF7A1C1C),
)

fun ColorScheme.withBlackBackground(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
)

// Cards stay transparent with red outlines against the black page.
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

// "Light" mode resolves to the same black/red/white palette — POSH is black-screen
// everywhere by design.
val LightColorScheme = DarkColorScheme

// POSH text boxes: black container, white text, and red interaction accents.
val textBoxBackground = Color.Black
val textBoxForeground = Color.White
val textBoxAccent = Color(0xFFFF3B30)

@Composable
fun outlineTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textBoxForeground,
    unfocusedTextColor = textBoxForeground,
    disabledTextColor = textBoxForeground.copy(alpha = 0.5f),
    cursorColor = textBoxAccent,
    focusedContainerColor = textBoxBackground,
    unfocusedContainerColor = textBoxBackground,
    disabledContainerColor = textBoxBackground,
    focusedBorderColor = textBoxAccent,
    unfocusedBorderColor = Color(0xFF444444),
    focusedLabelColor = textBoxAccent,
    unfocusedLabelColor = textBoxAccent.copy(alpha = 0.7f),
    disabledLabelColor = textBoxForeground.copy(alpha = 0.4f),
    focusedPlaceholderColor = textBoxForeground.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = textBoxForeground.copy(alpha = 0.45f),
    selectionColors = TextSelectionColors(
        handleColor = textBoxAccent,
        backgroundColor = textBoxAccent.copy(alpha = 0.35f),
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
