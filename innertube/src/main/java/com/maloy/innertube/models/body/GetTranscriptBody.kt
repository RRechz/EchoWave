package com.maloy.innertube.models.body

import com.maloy.innertube.models.Context
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class GetTranscriptBody(
    val context: Context<@Contextual Any?>,
    val params: String,
)
