package com.maloy.innertube.models.body

import com.maloy.innertube.models.Context
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueBody(
    val context: Context<@Contextual Any?>,
    val videoIds: List<String>?,
    val playlistId: String?,
)
