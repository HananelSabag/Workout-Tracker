package com.hananel.workouttracker2.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hananel.workouttracker2.ui.theme.Electric
import com.hananel.workouttracker2.ui.theme.Mint
import com.hananel.workouttracker2.ui.theme.rememberGlassColors

@Composable
fun SetProgressIndicator(
    completedSets: Int,
    currentSet: Int,
    totalSets: Int,
    modifier: Modifier = Modifier
) {
    val glass = rememberGlassColors()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSets) { index ->
            val setNumber = index + 1
            val isCurrent = setNumber == currentSet && setNumber > completedSets
            val isDone    = setNumber <= completedSets

            val pillColor by animateColorAsState(
                targetValue = when {
                    isDone    -> Mint
                    isCurrent -> Electric
                    else      -> glass.onGlassMuted.copy(alpha = 0.35f)
                },
                animationSpec = tween(400),
                label = "pillColor_$index"
            )

            val pillScale by animateFloatAsState(
                targetValue = if (isCurrent) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
                label = "pillScale_$index"
            )

            val pillHeight by animateDpAsState(
                targetValue = if (isCurrent) 26.dp else 22.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
                label = "pillH_$index"
            )

            Box(
                modifier = Modifier
                    .scale(pillScale)
                    .height(pillHeight)
                    .widthIn(min = pillHeight)
                    .clip(RoundedCornerShape(50))
                    .background(pillColor)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                } else {
                    Text(
                        text = setNumber.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}
