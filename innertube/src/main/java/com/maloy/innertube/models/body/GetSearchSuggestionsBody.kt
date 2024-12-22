package com.maloy.innertube.models.body

import com.maloy.innertube.models.Context
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class GetSearchSuggestionsBody(
    val context: Context<@Contextual Any?>,
    val input: String,
)
