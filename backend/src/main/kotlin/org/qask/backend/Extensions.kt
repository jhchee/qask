package org.qask.backend

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.qask.backend.models.domainModels.Channel
import org.qask.backend.models.domainModels.Question
import org.qask.backend.models.viewModels.*
import java.time.Duration
import java.time.Instant

fun Question.toPayload(): QuestionPayload {
    return QuestionPayload(
        content = content,
        status = status,
        questionerName = questionerName,
        likeCount = likeCount,
        isDeleted = isDeleted,
        isHidden = isHidden,
        sent = sent,
        id = id,
    )
}

fun Flow<Question>.toPayloads(): Flow<QuestionPayload> = map { it.toPayload() }


fun CreateQuestionVM.toDomainModel(channelId: String): Question {
    return Question(
        content = content ?: "",
        channelId = channelId,
        sent = Instant.now(),
        questionerName = questionerName ?: "Anonymous"
    )
}

fun CreateChannelVM.toDomainModel(presenterToken: String, audienceToken: String): Channel {
    return Channel(
        name = name,
        description = description,
        endTime = Instant.now() + Duration.ofMinutes(durationInMinute.toLong()),
        presenterToken = presenterToken,
        audienceToken = audienceToken,
    )
}

fun Channel.toDetail(): ChannelDetailVM {
    return ChannelDetailVM(
        name = name,
        description = description,
        endTime = endTime
    )
}

fun Channel.toHostDetail(): ChannelDetailForHostVM {
    return ChannelDetailForHostVM(
        name = name,
        description = description,
        endTime = endTime,
        presenterToken = presenterToken,
        audienceToken = audienceToken
    )
}
