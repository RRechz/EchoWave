package com.maloy.innertube.models.body

import com.maloy.innertube.models.Context
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBody(
    val context: Context<@Contextual Any?>,
    val videoId: String,
    val playlistId: String?,
)
