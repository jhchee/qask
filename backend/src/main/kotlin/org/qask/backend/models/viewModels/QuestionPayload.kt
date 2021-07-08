package org.qask.backend.models.viewModels

import org.qask.backend.models.enums.Status
import java.time.Instant

// used in streaming
data class QuestionPayload(
    val content: String? = "",
    val questionerName: String? = null,
    val likeCount: Int = 0,
    val sent: Instant,
    val isDeleted: Boolean = false,
    val isHidden: Boolean = false,
    val id: String? = null,
    val status: String = Status.DEFAULT.name
)