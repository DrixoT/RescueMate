package com.rescuemate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.rescuemate.ui.theme.AsciiLarge
import com.rescuemate.ui.theme.AsciiMedium
import com.rescuemate.ui.theme.AsciiSmall
import com.rescuemate.ui.theme.CosmicBackground
import com.rescuemate.ui.theme.CosmicBorder
import com.rescuemate.ui.theme.CosmicCard
import com.rescuemate.ui.theme.CosmicPrimary
import com.rescuemate.ui.theme.CosmicTextPrimary
import com.rescuemate.ui.theme.CosmicTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random

object AsciiArt {
    const val STAR = "*"
    const val PLANET = "O"
    const val GALAXY_DIVIDER = "*  .  +  .  *  .  +  .  *"
    const val ORBIT_RING = ".:*~*:._.:*~*:."
    const val ARROW_RIGHT = "-->"
    const val ARROW_LEFT = "<--"
    const val WARNING = "[!]"
}

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val stars = remember { mutableStateListOf<Star>() }
    var trigger by remember { mutableStateOf(0L) }

    LaunchedEffect(size) {
        if (size.width > 0 && size.height > 0) {
            stars.clear()
            repeat(100) {
                stars.add(
                    Star(
                        x = Random.nextFloat() * size.width,
                        y = Random.nextFloat() * size.height,
                        size = Random.nextFloat() * 3f + 1f,
                        speedX = (Random.nextFloat() - 0.5f) * 0.2f, // Reduced speed
                        speedY = (Random.nextFloat() - 0.5f) * 0.2f, // Reduced speed
                        alpha = Random.nextFloat() * 0.5f + 0.3f
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { time ->
                trigger = time
                stars.forEach { star ->
                    star.x += star.speedX
                    star.y += star.speedY

                    // Wrap around screen edges
                    if (star.x < 0) star.x = size.width.toFloat()
                    if (star.x > size.width) star.x = 0f
                    if (star.y < 0) star.y = size.height.toFloat()
                    if (star.y > size.height) star.y = 0f
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmicBackground)
            .onSizeChanged { size = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Use trigger to force redraw
            @Suppress("UNUSED_VARIABLE")
            val t = trigger
            
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = star.size / 2,
                    center = Offset(star.x, star.y)
                )
            }
        }
        content()
    }
}

private data class Star(
    var x: Float,
    var y: Float,
    val size: Float,
    val speedX: Float,
    val speedY: Float,
    val alpha: Float
)

@Composable
fun CosmicScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    GalaxyBackground(modifier = modifier) {
        Scaffold(
            topBar = topBar,
            bottomBar = bottomBar,
            containerColor = Color.Transparent, // Transparent to show GalaxyBackground
            contentColor = CosmicTextPrimary,
            content = content
        )
    }
}

@Composable
fun CosmicOverlay(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null
) {
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
                .background(Color.Black.copy(alpha = 0.8f))
                .clickable(enabled = false) {}, // Block interaction
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(CosmicCard, shape = RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, CosmicBorder), shape = RoundedCornerShape(16.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        icon()
                    }
                }
                
                Text(
                    text = title,
                    style = AsciiLarge,
                    color = CosmicTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = AsciiArt.GALAXY_DIVIDER,
                    style = AsciiSmall,
                    color = CosmicTextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (onDismiss != null && dismissText != null) {
                        CosmicButton(
                            text = dismissText,
                            onClick = onDismiss,
                            isPrimary = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (onConfirm != null && confirmText != null) {
                        CosmicButton(
                            text = confirmText,
                            onClick = onConfirm,
                            isPrimary = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CosmicToast(
    message: String?,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.zIndex(200f)
    ) {
        if (message != null) {
            LaunchedEffect(message) {
                delay(3000)
                onDismiss()
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(top = 40.dp) // Avoid status bar
            ) {
                Surface(
                    color = CosmicCard,
                    border = BorderStroke(1.dp, CosmicBorder),
                    shape = RoundedCornerShape(8.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AsciiArt.PLANET,
                            style = AsciiMedium,
                            color = CosmicPrimary
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmicTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CosmicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) CosmicPrimary.copy(alpha = 0.15f) else Color.Transparent,
            contentColor = if (isPrimary) CosmicPrimary else CosmicTextSecondary
        ),
        border = BorderStroke(
            1.dp, 
            if (isPrimary) CosmicPrimary else CosmicTextSecondary.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(50), // Pill shape
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = AsciiSmall.copy(fontWeight = FontWeight.Bold),
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun CosmicHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = AsciiLarge,
        color = CosmicTextPrimary,
        modifier = modifier
    )
}

@Composable
fun CosmicSubHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = AsciiMedium,
        color = CosmicTextSecondary,
        modifier = modifier
    )
}

@Composable
fun CosmicInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = AsciiSmall,
            color = CosmicTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = CosmicTextPrimary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            cursorBrush = SolidColor(CosmicPrimary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.dp, CosmicTextSecondary.copy(alpha = 0.3f)),
                            RoundedCornerShape(8.dp)
                        )
                        .background(CosmicCard.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CosmicTextSecondary.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun RotatingStar(
    modifier: Modifier = Modifier,
    color: Color = CosmicPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Text(
        text = "*",
        style = AsciiLarge.copy(fontSize = 48.sp),
        color = color,
        modifier = modifier
            .graphicsLayer { rotationZ = angle }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    maxSuggestions: Int = 5
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = it.isNotEmpty() && suggestions.any { suggestion ->
                    suggestion.contains(it, ignoreCase = true)
                }
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CosmicPrimary,
                unfocusedBorderColor = CosmicBorder,
                focusedLabelColor = CosmicPrimary,
                unfocusedLabelColor = CosmicTextSecondary,
                cursorColor = CosmicPrimary
            )
        )

        val filteredSuggestions = suggestions
            .filter { suggestion ->
                suggestion.contains(value, ignoreCase = true)
            }
            .take(maxSuggestions)

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = {
                            onSuggestionSelected(suggestion)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
    }
}
