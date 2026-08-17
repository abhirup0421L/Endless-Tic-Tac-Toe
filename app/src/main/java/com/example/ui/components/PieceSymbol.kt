package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PlayerSymbol
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerORedLight
import com.example.ui.theme.PlayerXBlue
import com.example.ui.theme.PlayerXBlueLight

@Composable
fun PieceSymbol(
    player: PlayerSymbol,
    isOldest: Boolean,
    order: Int,
    isWinningPiece: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scaleAnim = remember { Animatable(0f) }

    LaunchedEffect(player) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    val currentAlpha = if (isOldest) alphaPulse else 1f

    val strokeWidthDp = if (isWinningPiece) 11.dp else 9.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            val scale = scaleAnim.value
            val strokePx = strokeWidthDp.toPx()
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            if (player == PlayerSymbol.X) {
                val startColor = if (isWinningPiece) Color(0xFF60A5FA) else PlayerXBlueLight
                val endColor = if (isWinningPiece) Color(0xFF93C5FD) else PlayerXBlue
                val brush = Brush.linearGradient(
                    colors = listOf(startColor.copy(alpha = currentAlpha), endColor.copy(alpha = currentAlpha)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                val offset = (w * 0.36f) * scale
                // Top-left to bottom-right
                drawLine(
                    brush = brush,
                    start = Offset(cx - offset, cy - offset),
                    end = Offset(cx + offset, cy + offset),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
                // Top-right to bottom-left
                drawLine(
                    brush = brush,
                    start = Offset(cx + offset, cy - offset),
                    end = Offset(cx - offset, cy + offset),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            } else {
                val startColor = if (isWinningPiece) Color(0xFFF87171) else PlayerORedLight
                val endColor = if (isWinningPiece) Color(0xFFFCA5A5) else PlayerORed
                val brush = Brush.linearGradient(
                    colors = listOf(startColor.copy(alpha = currentAlpha), endColor.copy(alpha = currentAlpha)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )

                val radius = (w * 0.36f) * scale
                drawCircle(
                    brush = brush,
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        // Order badge (1 = oldest, 2, 3 = newest)
        if (isOldest) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NEXT OUT",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = if (player == PlayerSymbol.X) PlayerXBlueLight else PlayerORedLight,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
