package org.qask.backend.models.domainModels

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Channel(
    val name: String,
    val description: String = "",
    val presenterToken: String,
    val audienceToken: String,
    val endTime: Instant, // FIXME: Temporary 30 minutes end time
    @Id val id: String? = null,
)