package com.ai.aidicted.ui.search

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ai.aidicted.data.model.NewsArticle
import com.ai.aidicted.ui.theme.*
import com.ai.aidicted.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Text(
                text = "Search",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Premium search bar with glow border
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkCard,
                tonalElevation = 0.dp
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Search AI news...",
                            color = TextSecondaryDark.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = NeonCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan.copy(alpha = 0.4f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.06f),
                        cursorColor = NeonCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            when {
                state.isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "search_loading")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "search_pulse"
                        )
                        CircularProgressIndicator(
                            color = NeonCyan.copy(alpha = pulseAlpha),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                state.hasSearched && state.results.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "∅",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Thin
                            ),
                            color = TextSecondaryDark.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try a different search term",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                !state.hasSearched && state.query.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated glowing search icon
                        val infiniteTransition = rememberInfiniteTransition(label = "idle_anim")
                        val glowAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = 0.4f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = EaseInOutCubic),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "glow"
                        )

                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            NeonCyan.copy(alpha = glowAlpha),
                                            NeonPurple.copy(alpha = glowAlpha * 0.5f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Discover AI News",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Search by keyword or topic",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryDark
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        items(state.results, key = { it.id }) { article ->
                            SearchResultCard(article = article)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultCard(article: NewsArticle) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                article.sourceUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            },
        shape = RoundedCornerShape(18.dp),
        color = DarkCard.copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Thumbnail
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.25f)
                                )
                            )
                        )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TextSecondaryDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (article.source != null) {
                        Text(
                            text = article.source,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = NeonCyan.copy(alpha = 0.8f)
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondaryDark
                        )
                    }
                    Text(
                        text = formatRelativeTime(article.publishedAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondaryDark
                    )
                }
            }
        }
    }
}
