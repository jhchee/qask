package org.qask.backend.repositories

import kotlinx.coroutines.flow.Flow
import org.qask.backend.models.domainModels.Question
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository


interface QuestionRepository: ReactiveMongoRepository<Question, String> {

    @Query(value = "{'channelId': ?0}", sort = "{'sent': 1}")
    fun findByChannelIdOrderBySentAsc(channelId: String): Flow<Question>

}