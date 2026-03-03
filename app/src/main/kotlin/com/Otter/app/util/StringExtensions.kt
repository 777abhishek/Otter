package com.Otter.app.util

// Extension function to convert URL to HTTPS
fun String?.toHttpsUrl(): String {
    return this?.replace("http://", "https://") ?: ""
}

// Filter set with regex extension
fun Set<String>.filterWithRegex(regex: String): Set<String> {
    if (regex.isBlank()) return this
    return filter { it.contains(regex.toRegex()) }.toSet()
}
