@file:OptIn(ExperimentalTextApi::class)

package com.Otter.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

// Professional Typography - Refined for readability and hierarchy
val OtterTypography =
    Typography().run {
        copy(
            // Display - Large headlines, hero text
            displayLarge = displayLarge.copy(
                fontSize = 57.sp,
                fontWeight = FontWeight.W400,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            displayMedium = displayMedium.copy(
                fontSize = 45.sp,
                fontWeight = FontWeight.W400,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            displaySmall = displaySmall.copy(
                fontSize = 36.sp,
                fontWeight = FontWeight.W400,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            // Headline - Section headers
            headlineLarge = headlineLarge.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            headlineMedium = headlineMedium.copy(
                fontSize = 28.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            headlineSmall = headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            // Title - Card titles, app bar titles
            titleLarge = titleLarge.copy(
                fontSize = 22.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            titleMedium = titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            titleSmall = titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
                lineBreak = LineBreak.Heading,
                textDirection = TextDirection.Content
            ),
            // Body - Main content text
            bodyLarge = bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.W400,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
                lineBreak = LineBreak.Paragraph,
                textDirection = TextDirection.Content
            ),
            bodyMedium = bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.W400,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
                lineBreak = LineBreak.Paragraph,
                textDirection = TextDirection.Content
            ),
            bodySmall = bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.W400,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
                lineBreak = LineBreak.Paragraph,
                textDirection = TextDirection.Content
            ),
            // Label - Buttons, tabs, captions
            labelLarge = labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
                textDirection = TextDirection.Content
            ),
            labelMedium = labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                textDirection = TextDirection.Content
            ),
            labelSmall = labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
                textDirection = TextDirection.Content
            ),
        )
    }
