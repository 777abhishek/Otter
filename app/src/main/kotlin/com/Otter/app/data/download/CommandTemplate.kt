package com.Otter.app.data.download

import kotlinx.serialization.Serializable

@Serializable
data class CommandTemplate(
    val id: Int,
    val name: String,
    val template: String = "",
)
