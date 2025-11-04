package com.rescuemate.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rescuemate.R
import com.rescuemate.ui.theme.*

@Composable
fun OnboardingScreen(
    onStart: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CosmicBackground,
                        CosmicCard,
                        CosmicCardHover
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Logo Icon - Outlined Shield
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    val shieldPath = Path().apply {
                        moveTo(size.width * 0.5f, size.height * 0.05f)
                        lineTo(size.width * 0.85f, size.height * 0.2f)
                        lineTo(size.width * 0.85f, size.height * 0.55f)
                        cubicTo(
                            size.width * 0.85f, size.height * 0.75f,
                            size.width * 0.65f, size.height * 0.92f,
                            size.width * 0.5f, size.height * 0.95f
                        )
                        cubicTo(
                            size.width * 0.35f, size.height * 0.92f,
                            size.width * 0.15f, size.height * 0.75f,
                            size.width * 0.15f, size.height * 0.55f
                        )
                        lineTo(size.width * 0.15f, size.height * 0.2f)
                        close()
                    }

                    drawPath(
                        path = shieldPath,
                        color = Color(0xFFE91E63),
                        style = Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // App Name
            Text(
                text = stringResource(R.string.rescuemate),
                style = MaterialTheme.typography.displayMedium,
                color = CosmicTextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Label
            Text(
                text = stringResource(R.string.emergency_response_system),
                style = MaterialTheme.typography.labelSmall,
                color = CosmicTextSecondary,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Reassurance Message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.youre_never_alone),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CosmicTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.onboarding_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmicTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // CTA Button
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicPrimary
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Text(
                    text = stringResource(R.string.get_started),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

