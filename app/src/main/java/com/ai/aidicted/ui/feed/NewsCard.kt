package com.ai.aidicted.ui.feed

import android.content.Intent
import android.net.Uri
import com.ai.aidicted.deeplink.DeepLinkHelper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ai.aidicted.data.model.NewsArticle
import com.ai.aidicted.ui.navigation.LocalBottomNavHeight
import com.ai.aidicted.ui.theme.*
import com.ai.aidicted.util.formatRelativeTime

@Composable
fun NewsCard(
    article: NewsArticle,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val bottomNavHeight = LocalBottomNavHeight.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                isExpanded = !isExpanded
            }
    ) {
        // Full-bleed hero image as background
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(article.imageUrl)
                .crossfade(600)
                .build(),
            contentDescription = article.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Multi-layer gradient overlay for depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.25f to Color.Black.copy(alpha = 0.05f),
                            0.5f to Color.Black.copy(alpha = 0.2f),
                            0.7f to Color.Black.copy(alpha = 0.55f),
                            0.85f to Color.Black.copy(alpha = 0.82f),
                            1.0f to Color.Black.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Subtle neon accent glow line at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            NeonCyan.copy(alpha = 0.6f),
                            NeonPurple.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Source badge – frosted glass style, top-left
        if (article.source != null) {
            Box(
                modifier = Modifier
                    .padding(start = 20.dp, top = 56.dp)
                    .align(Alignment.TopStart)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = null,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = article.source.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }
            }
        }

        // Content area at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp)
                .padding(bottom = bottomNavHeight + 48.dp)
        ) {
            // Timestamp – small accent coloured
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

            // Title — large, bold, dramatic
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Summary (expandable) — elegant reveal with animation
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(400)),
                exit = shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeOut(animationSpec = tween(200))
            ) {
                Column {
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp,
                            fontSize = 15.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // "Tap to read" hint when collapsed
            AnimatedVisibility(
                visible = !isExpanded,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = article.summary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp
                        ),
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Divider — thin gradient line
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

            Spacer(modifier = Modifier.height(14.dp))

            // Action bar — glassmorphic floating row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite button with bouncy animation
                val favScale by animateFloatAsState(
                    targetValue = if (article.isFavorite) 1.15f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "fav_scale"
                )
                val heartColor by animateColorAsState(
                    targetValue = if (article.isFavorite) NeonPink else Color.White.copy(alpha = 0.7f),
                    label = "heart_color"
                )

                // Glass-style favorite button
                Surface(
                    onClick = onFavoriteClick,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = if (article.isFavorite) 0.12f else 0.08f),
                    modifier = Modifier.scale(favScale)
                ) {
                    Icon(
                        imageVector = if (article.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = heartColor,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(22.dp)
                    )
                }

                // Share button
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

                // Read More — pill-shaped glass button
                if (article.sourceUrl != null) {
                    Surface(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl))
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
