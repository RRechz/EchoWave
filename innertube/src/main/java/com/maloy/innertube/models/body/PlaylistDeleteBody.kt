package com.maloy.innertube.models.body

import com.maloy.innertube.models.Context
import kotlinx.serialization.Serializable
@Serializable
data class PlaylistDeleteBody(
    val context: Context,
    val playlistId: String
)