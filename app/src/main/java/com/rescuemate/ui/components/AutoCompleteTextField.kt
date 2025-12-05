  package com.rescuemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.rescuemate.ui.theme.CosmicBorder
import com.rescuemate.ui.theme.CosmicCard
import com.rescuemate.ui.theme.CosmicPrimary
import com.rescuemate.ui.theme.CosmicTextPrimary
import com.rescuemate.ui.theme.CosmicTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AutoCompleteTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<T>,
    onSuggestionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    itemContent: @Composable (T) -> Unit = { item -> 
        Text(
            text = item.toString(),
            color = CosmicTextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Filter suggestions based on input query
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) {
            emptyList()
        } else {
            suggestions.filter { 
                it.toString().contains(value, ignoreCase = true) 
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { 
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { 
                        onValueChange("")
                        expanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = CosmicTextSecondary
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { focusState ->
                    // Don't close immediately on focus loss as clicking the menu might cause temporary focus loss
                    if (!focusState.isFocused) {
                        // We rely on onExpandedChange from ExposedDropdownMenuBox to handle closing
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicPrimary,
                unfocusedBorderColor = CosmicBorder,
                focusedLabelColor = CosmicPrimary,
                unfocusedLabelColor = CosmicTextSecondary,
                cursorColor = CosmicPrimary
            ),
            singleLine = true
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(CosmicCard)
                    .heightIn(max = 200.dp)
            ) {
                filteredSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { itemContent(suggestion) },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            expanded = false
                            // We typically want to keep focus or clear it depending on UX. 
                            // Standard autocomplete behavior often keeps focus or moves to next field.
                            // Clearing query is handled by caller via onSuggestionSelected if needed (or passed here).
                        },
                        contentPadding = PaddingValues(0.dp)
                    )
                }
            }
        }
    }
}
