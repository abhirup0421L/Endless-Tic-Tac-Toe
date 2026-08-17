package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BoardDark
import com.example.ui.theme.GameYellowBackground
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.TextDark

@Composable
fun SplashScreen(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GameYellowBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Icon Card
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(BoardDark)
                    .border(width = 3.dp, color = Color.Black, shape = RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "X",
                        color = PlayerXBlue,
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "O",
                        color = PlayerORed,
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Name
            Text(
                text = "ENDLESS TIC TAC TOE",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = TextDark,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "VANISHING 3-PIECE STRATEGY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF713F12),
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Simple loading bar with loading animation
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEAB308))
                    .testTag("splash_loading_bar")
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(50)),
                    color = BoardDark,
                    trackColor = Color(0xFFFEF08A),
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "INITIALIZING ARENA...",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF854D0E),
                letterSpacing = 1.sp
            )
        }
    }
}
