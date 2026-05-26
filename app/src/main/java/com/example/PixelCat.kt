package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrightYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RetroPink

/**
 * Custom modern canvas pixel rendering for the retro dancing cat.
 * Defines multiple hand-crafted pixel frames, giving an absolutely charming retro cat driving style!
 */
@Composable
fun PixelCat(
    isPlaying: Boolean,
    beatStep: Int,
    modifier: Modifier = Modifier
) {
    // Frames are 16x16 pixel sprites represented as character arrays
    // Character legend:
    // '.' = Transparent / Space
    // 'C' = Cat Body (Neon Pink / purple)
    // 'W' = Cat Accent (White whiskers / belly / ears)
    // 'S' = Sunglasses / Cool Cyber Shades (Cyan)
    // 'R' = Sunglasses Reflection (Red / Magenta)
    // 'Y' = Accessories (Golden belt / collar)
    
    val frame1 = arrayOf(
        "................",
        "...C........C...",
        "...CC......CC...",
        "...CWC....CWC...",
        "...CCCCCCCCCC...",
        "...CSSSSSSSSC...",
        "...CSSSSSSSSC...",
        "...CCCCWWCCCC...",
        "....CCCWWCCC....",
        "....CCCWWCCC....",
        ".....CCCCCC.C...",
        ".....C.CC.C.CC..",
        ".....CCCCFF..C..",
        ".....C.CC.C.....",
        "....CC....CC....",
        "................"
    )

    val frame2 = arrayOf(
        "................",
        "...C........C...",
        "...CC......CC...",
        "...CWC....CWC...",
        "...CCCCCCCCCC...",
        "...CSSSSSSSSC...",
        "...CRSSSSRSSC...",
        "...CCCCWWCCCC...",
        "....C.CWW.C.C...",
        ".....C.WW.C.....",
        ".....CCCCCC.....",
        ".....C.C..C..C..",
        "....CC.CCCC.CC..",
        ".....C.C..C.....",
        "....CC....CC....",
        "................"
    )

    val frame3 = arrayOf(
        "................",
        "...C........C...",
        "...CC......CC...",
        "...CWC....CWC...",
        "...CCCCCCCCCC...",
        "...CSSSSSSSSC...",
        "...CSSSSSRSSC...",
        "...CCCCWWCCCC...",
        "....CCCWWCCC.C..",
        "....CCCWWCCC.CC.",
        ".....CCCCCC..C..",
        ".....C.CC.C.C...",
        "....CC.CC.CC....",
        ".....C.CC.C.....",
        "....CC....CC....",
        "................"
    )

    val frame4 = arrayOf(
        "................",
        "...C........C...",
        "...CC......CC...",
        "...CWC....CWC...",
        "...CCCCCCCCCC...",
        "...CSSSSSSSSC...",
        "...CRSSSSRSSC...",
        "...CCCCWWCCCC...",
        "..C.C.CWW.C.C...",
        "..C..C.WW.C.....",
        ".....CCCCCC.....",
        ".....C.C..C.....",
        "....CC.C..C.....",
        ".....C.C..C.....",
        "....CC....CC....",
        "................"
    )

    // Handle frame updates. If paused, lock to idle frame 1, otherwise cycle frames 1-4 synced to the beatStep!
    val currentFrame = if (isPlaying) {
        val index = beatStep % 4
        when (index) {
            0 -> frame1
            1 -> frame2
            2 -> frame3
            else -> frame4
        }
    } else {
        frame1
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val gridWidth = 16
            val gridHeight = 16
            val pixelW = size.width / gridWidth
            val pixelH = size.height / gridHeight

            // Draw shadow for floating feel
            drawOval(
                color = Color(0x33000000),
                topLeft = Offset(pixelW * 2f, pixelH * 14f),
                size = Size(pixelW * 12f, pixelH * 2f)
            )

            for (row in 0 until gridHeight) {
                val rowStr = currentFrame[row]
                for (col in 0 until gridWidth) {
                    val char = rowStr[col]
                    if (char != '.') {
                        val color = when (char) {
                            'C' -> RetroPink
                            'W' -> Color.White
                            'S' -> NeonCyan
                            'R' -> BrightYellow
                            'F' -> NetPink // tail flip
                            else -> Color.White
                        }

                        // Draw retro chunky square
                        drawRect(
                            color = color,
                            topLeft = Offset(col * pixelW, row * pixelH),
                            size = Size(pixelW - 0.5f, pixelH - 0.5f) // Subtle gap for mesh grid vibe
                        )
                    }
                }
            }
        }
        
        // Dynamic "Now Playing Vibes" music visual text
        if (isPlaying) {
            Text(
                text = when (beatStep % 4) {
                    0 -> "🎵 BOUNCE 🎵"
                    1 -> "✨ GROOVE ✨"
                    2 -> "⚡ RETRO ⚡"
                    else -> "🐱 VIBINGS 🐱"
                },
                color = BrightYellow,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
            )
        }
    }
}

private val NetPink = Color(0xFFFF529C)
