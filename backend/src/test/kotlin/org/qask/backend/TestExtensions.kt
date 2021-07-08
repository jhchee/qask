package org.qask.backend

import app.cash.turbine.FlowTurbine
import org.qask.backend.models.domainModels.Question
import org.qask.backend.models.enums.Action
import org.qask.backend.models.viewModels.EditQuestionVM
import org.qask.backend.models.viewModels.QuestionPayload
import java.time.Instant
import kotlin.math.abs

fun Question.compare(other: Question): Boolean {
    return content == other.content
            && questionerName == other.questionerName
            && likeCount == other.likeCount
            && channelId == other.channelId
            && status == other.status
            && id == other.id
}

fun QuestionPayload.compare(other: QuestionPayload): Boolean {
    return content == other.content
            && questionerName == other.questionerName
            && likeCount == other.likeCount
            && id == other.id
}

suspend fun <T> FlowTurbine<T>.expectNumberOfEvents(times: Int) {
    repeat(times) {
        expectEvent()
    }
}

fun Question.toEditVM(token: String, action: Action): EditQuestionVM = EditQuestionVM(
    id = id!!,
    token = token,
    action = action.name
)


fun Instant.compareTimeWithTolerance(other: Instant): Boolean {
    val diff = abs(epochSecond - other.epochSecond)
    // acceptable
    return diff <= 5
}