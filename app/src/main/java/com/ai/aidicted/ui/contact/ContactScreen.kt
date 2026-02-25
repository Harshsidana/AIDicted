package com.ai.aidicted.ui.contact

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai.aidicted.ui.navigation.LocalBottomNavHeight
import com.ai.aidicted.ui.theme.*

@Composable
fun ContactScreen() {
    val context = LocalContext.current
    val bottomNavHeight = LocalBottomNavHeight.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())
            Spacer(modifier = Modifier.height(32.dp))

            // Header
            Text(
                text = "About & Contact",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AIDicted — AI News in Seconds",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = NeonCyan,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Curated AI news from the world's best tech sources.\nNo ads. No fluff. Just the future.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondaryDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Section: Contact Information ─────────────────────────
            SectionHeader(title = "Contact Information")

            Spacer(modifier = Modifier.height(12.dp))

            ContactCard(
                icon = Icons.Default.Email,
                title = "Email Support",
                subtitle = "info@aidicted.in",
                description = "General enquiries & feedback",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:info@aidicted.in")
                        putExtra(Intent.EXTRA_SUBJECT, "AIDicted App Feedback")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ContactCard(
                icon = Icons.Default.Phone,
                title = "Phone Support",
                subtitle = "+91 79824 27827",
                description = "Available Mon–Fri, 10 AM – 6 PM IST",
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:+917982427827")
                    }
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ContactCard(
                icon = Icons.Default.Language,
                title = "Website",
                subtitle = "https://aidicted.in",
                description = "Visit our website for more info",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aidicted.in"))
                    context.startActivity(intent)
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Section: News Sources ─────────────────────────────────
            SectionHeader(title = "News Sources")

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkCard.copy(alpha = 0.6f),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    Text(
                        text = "All articles are sourced from and attributed to the original publishers:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val sources = listOf(
                        "TechCrunch" to "techcrunch.com",
                        "The Verge" to "theverge.com",
                        "Wired" to "wired.com",
                        "MIT Technology Review" to "technologyreview.com",
                        "Ars Technica" to "arstechnica.com",
                        "VentureBeat" to "venturebeat.com"
                    )
                    sources.forEach { (name, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondaryDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Section: About ────────────────────────────────────────
            SectionHeader(title = "About AIDicted")

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkCard.copy(alpha = 0.6f),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "AIDicted is a free, ad-free AI news aggregator that curates the latest " +
                            "articles from the world's top technology publications. Every article links " +
                            "directly to its original publisher so you can read the full story.",
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AboutStat(value = "6", label = "Sources")
                        AboutStat(value = "60+", label = "Articles")
                        AboutStat(value = "0", label = "Ads")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version + developer info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "AIDicted v1.1.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "Developer: Harsh Sidana",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "© 2026 AIDicted. All rights reserved.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(bottomNavHeight + 16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(NeonCyan, NeonPurple))
                )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = Color.White
        )
    }
}

@Composable
private fun AboutStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 22.sp
            ),
            color = NeonCyan
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark
        )
    }
}

@Composable
fun ContactCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String = "",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = DarkCard.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent))
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = NeonCyan.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = NeonCyan
                )
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
