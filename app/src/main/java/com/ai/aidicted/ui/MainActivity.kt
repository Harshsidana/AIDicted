package com.ai.aidicted.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ai.aidicted.deeplink.DeepLinkHelper
import com.ai.aidicted.ui.navigation.MainNavigation
import com.ai.aidicted.ui.splash.SplashScreen
import com.ai.aidicted.ui.theme.AIDictedTheme
import com.ai.aidicted.ui.theme.DarkBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled automatically */ }

    // Holds the deep link article ID extracted from the intent
    private val deepLinkArticleId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install and immediately dismiss the system splash screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()

        // Extract deep link from the launching intent
        handleDeepLink(intent)

        setContent {
            AIDictedTheme {
                var showSplash by remember { mutableStateOf(true) }
                val targetArticleId by remember { deepLinkArticleId }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) togetherWith
                                    fadeOut(animationSpec = tween(500))
                        },
                        label = "splash_transition"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(
                                onSplashComplete = { showSplash = false }
                            )
                        } else {
                            MainNavigation(
                                deepLinkArticleId = targetArticleId,
                                onDeepLinkConsumed = { deepLinkArticleId.value = null }
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Called when the activity receives a new intent while already running
     * (because launchMode="singleTask" in manifest).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val articleId = DeepLinkHelper.extractArticleId(intent)
        android.util.Log.d("DeepLink", "Handling intent: ${intent?.data}, extractedId: $articleId")
        if (articleId != null) {
            deepLinkArticleId.value = articleId
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
