package com.ai.aidicted.ui.favorites

import android.content.Intent
import android.net.Uri
import com.ai.aidicted.deeplink.DeepLinkHelper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ai.aidicted.data.model.NewsArticle
import com.ai.aidicted.ui.theme.*
import com.ai.aidicted.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedArticle by remember { mutableStateOf<NewsArticle?>(null) }

    // Full-screen article detail dialog
    selectedArticle?.let { article ->
        ArticleDetailDialog(
            article = article,
            onDismiss = { selectedArticle = null },
            onRemoveFavorite = {
                viewModel.toggleFavorite(article.id)
                selectedArticle = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )
            }

            // Thin gradient divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                NeonCyan.copy(alpha = 0.3f),
                                NeonPurple.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (state.favorites.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "♡",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 48.sp
                        ),
                        color = NeonPurple.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "No saved articles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the heart on any article to save it here",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 16.dp,
                        bottom = 120.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.favorites, key = { it.id }) { article ->
                        FavoriteCard(
                            article = article,
                            onClick = { selectedArticle = article },
                            onRemove = { viewModel.toggleFavorite(article.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleDetailDialog(
    article: NewsArticle,
    onDismiss: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-bleed image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(article.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = article.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Multi-layer gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.3f to Color.Black.copy(alpha = 0.15f),
                                0.55f to Color.Black.copy(alpha = 0.4f),
                                0.75f to Color.Black.copy(alpha = 0.75f),
                                1.0f to Color.Black.copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Close button — top right
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(10.dp)
                )
            }

            // Source badge
            if (article.source != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        text = article.source.uppercase(),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Content at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Text(
                    text = formatRelativeTime(article.publishedAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = NeonCyan.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = article.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 34.sp
                    ),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 26.sp,
                        fontSize = 15.sp
                    ),
                    color = Color.White.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Gradient divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    NeonCyan.copy(alpha = 0.4f),
                                    NeonPurple.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Remove favorite
                    Surface(
                        onClick = onRemoveFavorite,
                        shape = CircleShape,
                        color = NeonPink.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = "Remove",
                            tint = NeonPink,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(22.dp)
                        )
                    }

                    // Share
                    Surface(
                        onClick = {
                            val shareText = DeepLinkHelper.buildShareText(
                                title = article.title,
                                summary = article.summary,
                                articleId = article.id
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .padding(12.dp)
                                .size(22.dp)
                        )
                    }

                    // Read More
                    if (article.sourceUrl != null) {
                        Surface(
                            onClick = {
                                val intent =
                                    Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = NeonCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Read Full",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteCard(
    article: NewsArticle,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = DarkCard.copy(alpha = 0.9f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Thumbnail with rounded corners and subtle glow
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                // Subtle gradient overlay on thumbnail
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.95f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                            text = "·",
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
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Remove favorite
                    Surface(
                        onClick = onRemove,
                        shape = CircleShape,
                        color = NeonPink.copy(alpha = 0.1f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = "Remove",
                                tint = NeonPink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    // Share
                    Surface(
                        onClick = {
                            val shareText = DeepLinkHelper.buildShareText(
                                title = article.title,
                                summary = article.summary,
                                articleId = article.id
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via"))
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.06f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
