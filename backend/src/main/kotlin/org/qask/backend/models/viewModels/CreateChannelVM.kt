package org.qask.backend.models.viewModels

// used by host
data class CreateChannelVM(
    val name: String,
    val durationInMinute: Int,
    val description: String,
)