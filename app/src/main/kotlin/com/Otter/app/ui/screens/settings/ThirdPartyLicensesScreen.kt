package com.Otter.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThirdPartyLicensesScreen(
    navController: NavController,
    onBack: (() -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
            Text(
                text = "Third-Party Licenses",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // yt-dlp
        LicenseCard(
            name = "yt-dlp",
            description = "Video extraction and metadata fetching",
            license = "Unlicense",
            licenseText = """This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or distribute this software, either in source code form or as a compiled binary, for any purpose, commercial or non-commercial, and by any means.""",
            url = "https://github.com/yt-dlp/yt-dlp"
        )

        // youtubedl-android (junkfood02)
        LicenseCard(
            name = "youtubedl-android",
            description = "Android wrapper for yt-dlp/youtube-dl and FFmpeg binaries",
            license = "MIT",
            licenseText = """Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.""",
            url = "https://github.com/junkfood02/Seal"
        )

        // FFmpeg
        LicenseCard(
            name = "FFmpeg",
            description = "Multimedia framework used for media processing (packaged via youtubedl-android)",
            license = "LGPL/GPL",
            licenseText = """FFmpeg is licensed under the LGPL version 2.1 or later.
Some FFmpeg builds may include GPL components depending on configuration.
See the FFmpeg project for details.""",
            url = "https://ffmpeg.org/"
        )

        // NewPipeExtractor
        LicenseCard(
            name = "NewPipeExtractor",
            description = "Extracts metadata from supported streaming services (used for fast parsing)",
            license = "GPL-3.0",
            licenseText = """This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.""",
            url = "https://github.com/TeamNewPipe/NewPipeExtractor"
        )

        // Media3/ExoPlayer
        LicenseCard(
            name = "Media3 / ExoPlayer",
            description = "Media playback framework",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.""",
            url = "https://github.com/androidx/media"
        )

        // Coil3
        LicenseCard(
            name = "Coil3",
            description = "Image loading library",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.""",
            url = "https://github.com/coil-kt/coil"
        )

        // OkHttp
        LicenseCard(
            name = "OkHttp",
            description = "HTTP client",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://github.com/square/okhttp"
        )

        // Retrofit
        LicenseCard(
            name = "Retrofit",
            description = "Type-safe HTTP client",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://github.com/square/retrofit"
        )

        // Gson
        LicenseCard(
            name = "Gson",
            description = "JSON serialization/deserialization",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://github.com/google/gson"
        )

        // Ktor
        LicenseCard(
            name = "Ktor",
            description = "Networking client used by Coil network module",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://github.com/ktorio/ktor"
        )

        // Kotlin
        LicenseCard(
            name = "Kotlin",
            description = "Programming language",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://kotlinlang.org/"
        )

        // Room
        LicenseCard(
            name = "Room",
            description = "Database library",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://developer.android.com/training/data-storage/room"
        )

        // WorkManager
        LicenseCard(
            name = "WorkManager",
            description = "Background task scheduling",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://developer.android.com/topic/libraries/architecture/workmanager"
        )

        // DataStore
        LicenseCard(
            name = "DataStore",
            description = "Preferences and data storage",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://developer.android.com/topic/libraries/architecture/datastore"
        )

        // Hilt
        LicenseCard(
            name = "Hilt",
            description = "Dependency injection",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://dagger.dev/hilt/"
        )

        // Material Components
        LicenseCard(
            name = "Material Components for Android",
            description = "Material UI components",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://github.com/material-components/material-components-android"
        )

        // desugar_jdk_libs
        LicenseCard(
            name = "desugar_jdk_libs",
            description = "Java language library desugaring for older Android versions",
            license = "Apache 2.0",
            licenseText = """Licensed under the Apache License, Version 2.0.""",
            url = "https://github.com/google/desugar_jdk_libs"
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun LicenseCard(
    name: String,
    description: String,
    license: String,
    licenseText: String,
    url: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = license,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(4.dp)
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = licenseText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 5,
            )
        }
    }
}
