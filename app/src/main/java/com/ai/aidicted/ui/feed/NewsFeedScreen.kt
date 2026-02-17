package com.ai.aidicted.ui.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ai.aidicted.ui.navigation.LocalBottomNavHeight
import com.ai.aidicted.ui.theme.*

@Composable
fun NewsFeedScreen(
    viewModel: NewsFeedViewModel = hiltViewModel(),
    deepLinkArticleId: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val bottomNavHeight = LocalBottomNavHeight.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when {
            state.isLoading && state.articles.isEmpty() -> {
                // Premium loading animation
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Animated pulsing glow indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "loading")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        NeonCyan.copy(alpha = pulseAlpha * 0.6f),
                                        NeonPurple.copy(alpha = pulseAlpha * 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan.copy(alpha = pulseAlpha),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "LOADING AI NEWS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 3.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextSecondaryDark.copy(alpha = pulseAlpha)
                    )
                }
            }

            state.error != null && state.articles.isEmpty() -> {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Something went wrong",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        onClick = { viewModel.loadArticles() },
                        shape = RoundedCornerShape(24.dp),
                        color = NeonCyan.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "RETRY",
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = NeonCyan
                        )
                    }
                }
            }

            state.articles.isNotEmpty() -> {
                val pagerState = rememberPagerState(pageCount = { state.articles.size })

                // Handle deep link — scroll to the target article
                LaunchedEffect(deepLinkArticleId, state.articles) {
                    if (deepLinkArticleId != null && state.articles.isNotEmpty()) {
                        val targetIndex = state.articles.indexOfFirst { it.id == deepLinkArticleId }
                        if (targetIndex >= 0) {
                            pagerState.animateScrollToPage(targetIndex)
                            // Only consume the deep link if we successfully handled it
                            onDeepLinkConsumed()
                        }
                    }
                }

                // Horizontal pager — differentiated from InShorts' vertical swipe
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                    pageSpacing = 0.dp
                ) { page ->
                    val article = state.articles[page]
                    NewsCard(
                        article = article,
                        onFavoriteClick = { viewModel.toggleFavorite(article.id) }
                    )
                }

                // Floating glassmorphic header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // App name with gradient
                        Text(
                            text = "AIDicted",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                letterSpacing = (-1).sp
                            ),
                            color = Color.White
                        )

                        // Article counter pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${state.articles.size}",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                ),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Gradient progress bar at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                ) {
                    // Track
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.06f))
                    )
                    // Progress fill with gradient
                    val progress = if (state.articles.size > 1) {
                        (pagerState.currentPage + 1).toFloat() / state.articles.size.toFloat()
                    } else 1f

                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(NeonCyan, NeonPurple, NeonPink)
                                )
                            )
                    )
                }

                // Bottom page dots — minimal, horizontal
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = bottomNavHeight + 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalDots = minOf(state.articles.size, 8)
                    val currentWindow = (pagerState.currentPage / totalDots) * totalDots

                    repeat(totalDots) { index ->
                        val dotIndex = currentWindow + index
                        if (dotIndex < state.articles.size) {
                            val isActive = dotIndex == pagerState.currentPage
                            val dotWidth by animateDpAsState(
                                targetValue = if (isActive) 24.dp else 6.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "dot_width"
                            )

                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) {
                                            Brush.horizontalGradient(
                                                colors = listOf(NeonCyan, NeonPurple)
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.White.copy(alpha = 0.2f),
                                                    Color.White.copy(alpha = 0.2f)
                                                )
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }

                // Swipe hint on first page
                AnimatedVisibility(
                    visible = pagerState.currentPage == 0 && state.articles.size > 1,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    enter = fadeIn(tween(1000)),
                    exit = fadeOut(tween(500))
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "hint")
                    val offsetX by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = EaseInOutCubic),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "hint_x"
                    )

                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Thin
                        ),
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.offset(x = offsetX.dp)
                    )
                }
            }
        }
    }
}
