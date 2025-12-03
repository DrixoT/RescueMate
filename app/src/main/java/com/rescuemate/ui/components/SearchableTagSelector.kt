package com.rescuemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.rescuemate.ui.theme.CosmicBorder
import com.rescuemate.ui.theme.CosmicCard
import com.rescuemate.ui.theme.CosmicPrimary
import com.rescuemate.ui.theme.CosmicTextPrimary
import com.rescuemate.ui.theme.CosmicTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableTagSelector(
    label: String,
    allOptions: List<String>,
    selectedOptions: List<String>,
    onOptionSelected: (String) -> Unit,
    onOptionRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val filteredOptions = remember(query, allOptions, selectedOptions) {
        if (query.isBlank()) {
            allOptions.filterNot { it in selectedOptions }.take(5)
        } else {
            allOptions.filter { 
                it.contains(query, ignoreCase = true) && it !in selectedOptions 
            }
        }
    }

    Column(modifier = modifier) {
        // Search Input with ExposedDropdownMenuBox
        ExposedDropdownMenuBox(
            expanded = expanded && (filteredOptions.isNotEmpty() || query.isNotEmpty()),
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { 
                    query = it
                    expanded = true
                },
                label = { Text(label) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { 
                            // Add custom tag if not empty and not in list
                            if (query.isNotBlank() && query !in selectedOptions) {
                                onOptionSelected(query.trim())
                                query = ""
                                expanded = false
                            }
                        }) {
                            Icon(Icons.Default.Add, "Add Custom")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CosmicPrimary,
                    unfocusedBorderColor = CosmicBorder,
                    focusedTextColor = CosmicTextPrimary,
                    unfocusedTextColor = CosmicTextPrimary,
                    focusedContainerColor = CosmicCard,
                    unfocusedContainerColor = CosmicCard
                ),
                singleLine = true
            )

            if (expanded) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(CosmicCard).heightIn(max = 200.dp)
                ) {
                    filteredOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = CosmicTextPrimary) },
                            onClick = {
                                onOptionSelected(option)
                                query = ""
                                expanded = false
                                // Keeping focus to allow typing next tag easily or clearing it
                                // focusManager.clearFocus() // Optional: keep focus for rapid entry
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (query.isNotEmpty() && filteredOptions.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Add \"$query\"", color = CosmicPrimary) },
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = CosmicPrimary) },
                            onClick = {
                                onOptionSelected(query.trim())
                                query = ""
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // Selected Chips
        if (selectedOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SimpleFlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalGap = 8.dp,
                verticalGap = 8.dp
            ) {
                selectedOptions.forEach { option ->
                    AssistChip(
                        onClick = {},
                        label = { Text(option) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onOptionRemoved(option) }
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = CosmicPrimary.copy(alpha = 0.15f),
                            labelColor = CosmicTextPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    horizontalGap: androidx.compose.ui.unit.Dp = 0.dp,
    verticalGap: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalGapPx = horizontalGap.roundToPx()
        val verticalGapPx = verticalGap.roundToPx()

        val rows = mutableListOf<List<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentWidth = 0

        val placeables = measurables.map { it.measure(constraints) }

        placeables.forEach { placeable ->
            if (currentWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentWidth = 0
            }
            currentRow.add(placeable)
            currentWidth += placeable.width + horizontalGapPx
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 } + (rows.size - 1).coerceAtLeast(0) * verticalGapPx

        layout(constraints.maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                row.forEach { placeable ->
                    placeable.place(x, y)
                    x += placeable.width + horizontalGapPx
                }
                y += rowHeight + verticalGapPx
            }
        }
    }
}
