package com.example.bananasball.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpinningBaseballCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val centerOffset = this.center

        // Yellow baseball base
        drawCircle(
            color = Color(0xFFFFE000),
            radius = radius
        )
        // Seam border
        drawCircle(
            color = Color(0xFFE5C700),
            radius = radius,
            style = Stroke(width = size.minDimension * 0.05f)
        )

        // Red Seams
        val seamColor = Color(0xFFD32F2F)
        val strokeWidth = (size.minDimension * 0.05f).coerceAtLeast(1.5f)

        // Left seam arc
        drawArc(
            color = seamColor,
            startAngle = 120f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(centerOffset.x - radius * 0.8f, centerOffset.y - radius * 0.7f),
            size = Size(radius * 0.8f, radius * 1.4f),
            style = Stroke(width = strokeWidth)
        )
        // Right seam arc
        drawArc(
            color = seamColor,
            startAngle = -60f,
            sweepAngle = 120f,
            useCenter = false,
            topLeft = Offset(centerOffset.x, centerOffset.y - radius * 0.7f),
            size = Size(radius * 0.8f, radius * 1.4f),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun SpinningBaseballLoader(
    modifier: Modifier = Modifier,
    text: String? = "Loading...",
    ballSize: Dp = 56.dp
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SpinningBaseballCanvas(
            modifier = Modifier
                .size(ballSize)
                .rotate(rotation)
        )
        if (!text.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoxScope.BananaPullToRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val distance = state.distanceFraction
    if (isRefreshing || distance > 0f) {
        val rotation = if (isRefreshing) {
            val infiniteTransition = rememberInfiniteTransition()
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            angle
        } else {
            distance * 360f
        }

        val topOffset = ((distance.coerceIn(0f, 1.2f) * 52f).dp).coerceAtLeast(8.dp)

        Box(
            modifier = modifier
                .align(Alignment.TopCenter)
                .offset(y = topOffset)
                .size(42.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(Color(0xFF1E293B), CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            SpinningBaseballCanvas(
                modifier = Modifier
                    .size(30.dp)
                    .rotate(rotation)
            )
        }
    }
}
