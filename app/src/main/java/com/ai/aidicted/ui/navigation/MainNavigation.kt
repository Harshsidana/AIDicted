package com.ai.aidicted.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.aidicted.ui.favorites.FavoritesScreen
import com.ai.aidicted.ui.feed.NewsFeedScreen
import com.ai.aidicted.ui.search.SearchScreen
import com.ai.aidicted.ui.theme.*

// CompositionLocal to share the bottom nav bar height with child screens
val LocalBottomNavHeight = compositionLocalOf { 0.dp }

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Feed : BottomNavItem("feed", "Feed", Icons.Filled.Home, Icons.Outlined.Home)
    data object Favorites : BottomNavItem("favorites", "Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    data object Search : BottomNavItem("search", "Search", Icons.Filled.Search, Icons.Outlined.Search)
}

@Composable
fun MainNavigation(
    deepLinkArticleId: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val items = listOf(BottomNavItem.Feed, BottomNavItem.Favorites, BottomNavItem.Search)
    val density = LocalDensity.current
    var navBarHeightDp by remember { mutableStateOf(0.dp) }

    // If a deep link is present, ensure we're on the Feed tab
    LaunchedEffect(deepLinkArticleId) {
        if (deepLinkArticleId != null) {
            selectedTab = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Content — provide bottom nav height so children can add padding
        CompositionLocalProvider(LocalBottomNavHeight provides navBarHeightDp) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> NewsFeedScreen(
                        deepLinkArticleId = deepLinkArticleId,
                        onDeepLinkConsumed = onDeepLinkConsumed
                    )
                    1 -> FavoritesScreen()
                    2 -> SearchScreen()
                }
            }
        }

        // Floating glassmorphic bottom navigation
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 48.dp, vertical = 16.dp)
                .onGloballyPositioned { coordinates ->
                    with(density) {
                        navBarHeightDp = coordinates.size.height.toDp() + 16.dp // plus the vertical padding
                    }
                }
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DarkCard.copy(alpha = 0.9f),
                tonalElevation = 0.dp,
                shadowElevation = 16.dp
            ) {
                // Neon top border
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        NeonCyan.copy(alpha = 0.4f),
                                        NeonPurple.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.1f else 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "nav_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .scale(scale)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedTab = index }
                                    .then(
                                        if (isSelected) {
                                            Modifier.background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        NeonCyan.copy(alpha = 0.15f),
                                                        NeonPurple.copy(alpha = 0.1f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            )
                                        } else Modifier
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = item.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            letterSpacing = if (isSelected) 0.5.sp else 0.sp
                                        ),
                                        color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.35f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
