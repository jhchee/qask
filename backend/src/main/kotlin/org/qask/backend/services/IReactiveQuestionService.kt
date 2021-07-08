package org.qask.backend.services

import kotlinx.coroutines.flow.Flow
import org.qask.backend.models.domainModels.Channel
import org.qask.backend.models.viewModels.CreateQuestionVM
import org.qask.backend.models.viewModels.EditQuestionVM
import org.qask.backend.models.viewModels.QuestionPayload

interface IReactiveQuestionService {

    fun stream(channelId: String): Flow<QuestionPayload>

    fun latest(channelId: String): Flow<QuestionPayload>

    suspend fun getChannelWithAnyToken(token: String): Channel?

    suspend fun postQuestion(createQuestionVM: CreateQuestionVM)

    suspend fun hostEdit(editQuestionVM: EditQuestionVM)

    suspend fun audienceEdit(editQuestionVM: EditQuestionVM)

}