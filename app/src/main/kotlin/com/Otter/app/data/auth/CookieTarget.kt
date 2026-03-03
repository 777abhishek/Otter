package com.Otter.app.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class CookieTarget(
    val id: String,
    val title: String,
    val loginUrl: String,
    val domains: List<String>,
)
