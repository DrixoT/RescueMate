package com.rescuemate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
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
fun CosmicScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        containerColor = CosmicBackground,
        contentColor = CosmicTextPrimary,
        content = content
    )
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
    isPrimary: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) CosmicPrimary.copy(alpha = 0.1f) else Color.Transparent,
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
            style = AsciiSmall,
            letterSpacing = 2.sp
        )
    }
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

