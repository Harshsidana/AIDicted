package com.ai.aidicted.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.aidicted.R
import com.ai.aidicted.ui.theme.DarkBackground
import com.ai.aidicted.ui.theme.GradientEnd
import com.ai.aidicted.ui.theme.GradientMid
import com.ai.aidicted.ui.theme.GradientStart
import com.ai.aidicted.ui.theme.NeonCyan
import com.ai.aidicted.ui.theme.NeonPurple
import com.ai.aidicted.ui.theme.TextSecondaryDark
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var showLogo by remember { mutableStateOf(false) }
    var showTagline by remember { mutableStateOf(false) }
    var showSubtext by remember { mutableStateOf(false) }

    val scale = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        showLogo = true
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )

        delay(300)
        showTagline = true

        delay(400)
        showSubtext = true

        delay(1200)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        Color(0xFF0A0E1A),
                        Color(0xFF0D1025),
                        DarkBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle radial glow behind the logo
        Box(
            modifier = Modifier
                .size(350.dp)
                .alpha(0.15f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.5f),
                            NeonPurple.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            // Animated Logo Image
            AnimatedVisibility(
                visible = showLogo,
                enter = fadeIn(animationSpec = tween(800)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(800, easing = FastOutSlowInEasing)
                        )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.splash_logo),
                    contentDescription = "AIDicted Logo",
                    modifier = Modifier
                        .size(260.dp)
                        .scale(scale.value)
                        .clip(RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Tagline
            AnimatedVisibility(
                visible = showTagline,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = tween(600, easing = FastOutSlowInEasing)
                        )
            ) {
                Text(
                    text = "AIDicted",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientMid, GradientEnd)
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtext
            AnimatedVisibility(
                visible = showSubtext,
                enter = fadeIn(animationSpec = tween(500))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AI News Hub",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondaryDark,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Animated loading bar
                    val loadingProgress by animateFloatAsState(
                        targetValue = if (showSubtext) 1f else 0f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing),
                        label = "loading"
                    )
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF1A1F30))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .width(120.dp * loadingProgress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(NeonCyan, NeonPurple)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
