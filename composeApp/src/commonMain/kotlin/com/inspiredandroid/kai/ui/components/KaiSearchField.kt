package com.inspiredandroid.kai.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.inspiredandroid.kai.ui.handCursor
import com.inspiredandroid.kai.ui.textBoxBackground
import com.inspiredandroid.kai.ui.textBoxAccent
import com.inspiredandroid.kai.ui.textBoxForeground

@Composable
fun KaiSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    clearContentDescription: String? = null,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.handCursor(),
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = clearContentDescription)
                }
            }
        } else {
            null
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = textBoxAccent,
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            focusedContainerColor = textBoxBackground,
            unfocusedContainerColor = textBoxBackground,
            focusedTextColor = textBoxForeground,
            unfocusedTextColor = textBoxForeground,
            cursorColor = textBoxAccent,
            focusedPlaceholderColor = textBoxForeground.copy(alpha = 0.45f),
            unfocusedPlaceholderColor = textBoxForeground.copy(alpha = 0.45f),
            focusedLeadingIconColor = textBoxAccent,
            unfocusedLeadingIconColor = textBoxAccent.copy(alpha = 0.7f),
            focusedTrailingIconColor = textBoxAccent,
            unfocusedTrailingIconColor = textBoxAccent.copy(alpha = 0.7f),
        ),
    )
}
