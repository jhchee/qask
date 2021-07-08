package org.qask.backend.models.viewModels

import java.time.Instant

data class ChannelDetailVM(
    val name: String,
    val description: String,
    val endTime: Instant
)

// return this when someone create a channel
data class ChannelDetailForHostVM(
    val name: String,
    val description: String,
    val endTime: Instant,
    val presenterToken: String,
    val audienceToken: String
)