package org.qask.backend.models.domainModels

import org.qask.backend.models.enums.Status
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document
data class Question(
    val content: String,
    val questionerName: String,
    val sent: Instant,
    var likeCount: Int = 0,
    val channelId: String, // reference to channel
    var isDeleted: Boolean = false,
    var isHidden: Boolean = false,
    var status: String = Status.DEFAULT.name,
    @Id val id: String? = null,
)