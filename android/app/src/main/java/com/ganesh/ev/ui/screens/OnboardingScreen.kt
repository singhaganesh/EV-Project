package com.ganesh.ev.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

data class OnboardingPage(
        val title: String,
        val description: String,
        val icon: String,
        val color: Color
)

val onboardingPages =
        listOf(
                OnboardingPage(
                        title = "Smart Finding",
                        description =
                                "Discover the nearest charging stations with real-time location tracking.",
                        icon = "🔍",
                        color = Color(0xFF00E5FF)
                ),
                OnboardingPage(
                        title = "Instant Booking",
                        description = "Reserve your charging slot in advance and skip the wait.",
                        icon = "📅",
                        color = Color(0xFF6200EE)
                ),
                OnboardingPage(
                        title = "Real-time Status",
                        description =
                                "Monitor your charging progress and battery health from anywhere.",
                        icon = "⚡",
                        color = Color(0xFFFFC107)
                ),
                OnboardingPage(
                        title = "Secure Payments",
                        description =
                                "Experience secure, hassle-free transactions at your fingertips.",
                        icon = "💳",
                        color = Color(0xFF4CAF50)
                )
        )

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Horizontal Pager for slides
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                    index ->
                OnboardingSlide(page = onboardingPages[index])
            }

            // Bottom Navigation Area
            Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                        modifier = Modifier.padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.Center
                ) {
                    repeat(onboardingPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                                modifier =
                                        Modifier.padding(4.dp)
                                                .size(if (isSelected) 10.dp else 8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                        if (isSelected)
                                                                MaterialTheme.colorScheme.primary
                                                        else
                                                                MaterialTheme.colorScheme.primary
                                                                        .copy(alpha = 0.2f)
                                                )
                        )
                    }
                }

                // Action Buttons
                val isLastPage = pagerState.currentPage == onboardingPages.size - 1

                Button(
                        onClick = {
                            if (isLastPage) {
                                onFinished()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.large,
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                )
                ) {
                    Text(
                            text = if (isLastPage) "Get Started" else "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                    )
                    if (!isLastPage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!isLastPage) {
                    TextButton(onClick = onFinished, modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                                text = "Skip",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingSlide(page: OnboardingPage) {
    Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
    ) {
        // Icon/Illustration Container
        Box(
                modifier =
                        Modifier.size(200.dp)
                                .clip(CircleShape)
                                .background(page.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
        ) { Text(text = page.icon, fontSize = 80.sp) }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
                text = page.title,
                style =
                        MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                        ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
        )
    }
}
