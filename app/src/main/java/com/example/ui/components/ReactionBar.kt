package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ActiveReaction
import com.example.ui.theme.GameYellowVibrant
import com.example.ui.theme.PlayerORed
import com.example.ui.theme.PlayerXBlue
import kotlinx.coroutines.delay

val POPULAR_REACTION_EMOJIS = listOf(
    "🔥", "👏", "🎉", "😎", "😂", 
    "😭", "😡", "🤯", "💀", "🫡", 
    "⚡", "🏆", "💔", "🤔", "👀"
)

/**
 * Reaction bubble displayed strictly under player's name in the scoreboard
 * Has a 5-second animated presentation and auto-disappears without covering grid cells.
 */
@Composable
fun PlayerReactionBadge(
    reaction: ActiveReaction?,
    isPlayerX: Boolean,
    modifier: Modifier = Modifier
) {
    if (reaction == null) {
        Spacer(modifier = Modifier.height(28.dp))
        return
    }

    val scale = remember(reaction.id) { Animatable(0.4f) }
    val alpha = remember(reaction.id) { Animatable(1f) }
    val borderColor = if (isPlayerX) PlayerXBlue else PlayerORed

    LaunchedEffect(reaction.id) {
        // Pop-in bounce
        scale.animateTo(1.2f, animationSpec = tween(220, easing = FastOutSlowInEasing))
        scale.animateTo(1.0f, animationSpec = tween(150))
        // Stay active for 4.2 seconds, then fade out during the 5th second
        delay(4200)
        alpha.animateTo(0f, animationSpec = tween(600))
    }

    Box(
        modifier = modifier
            .height(28.dp)
            .scale(scale.value)
            .alpha(alpha.value)
            .shadow(4.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF141418))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = reaction.emoji,
                fontSize = 15.sp
            )
            Text(
                text = reaction.emoji,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.0f) // spacer layout anchor
            )
        }
    }
}

/**
 * Reaction control bar with built-in 3-second anti-spam cooldown timer
 */
@Composable
fun ReactionBar(
    onSendReaction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var cooldownRemaining by remember { mutableStateOf(0) }

    LaunchedEffect(cooldownRemaining) {
        if (cooldownRemaining > 0) {
            delay(1000)
            cooldownRemaining -= 1
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(10.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF141416),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF2E2E32))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SEND REACTION",
                                color = GameYellowVibrant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            if (cooldownRemaining > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${cooldownRemaining}s cooldown)",
                                    color = PlayerORed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        POPULAR_REACTION_EMOJIS.forEach { emoji ->
                            val isCooldownActive = cooldownRemaining > 0
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCooldownActive) Color(0xFF1A1A1E) else Color(0xFF222226))
                                    .clickable(enabled = !isCooldownActive) {
                                        onSendReaction(emoji)
                                        cooldownRemaining = 3 // 3-second anti-spam cooldown
                                        isExpanded = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    fontSize = 22.sp,
                                    modifier = Modifier.alpha(if (isCooldownActive) 0.35f else 1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (!isExpanded) {
            val isCooldownActive = cooldownRemaining > 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1C1C1E))
                        .border(
                            1.dp,
                            if (isCooldownActive) Color.White.copy(alpha = 0.2f) else GameYellowVibrant.copy(alpha = 0.5f),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { isExpanded = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isCooldownActive) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = PlayerORed,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "COOLDOWN ${cooldownRemaining}s",
                                color = PlayerORed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        } else {
                            Text(text = "🔥", fontSize = 14.sp)
                            Text(text = "👏", fontSize = 14.sp)
                            Text(text = "😭", fontSize = 14.sp)
                            Text(text = "😎", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "REACT",
                                color = GameYellowVibrant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
