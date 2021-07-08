package org.qask.backend.repositories

import kotlinx.coroutines.flow.Flow
import org.qask.backend.models.domainModels.Channel
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface ChannelRepository : ReactiveMongoRepository<Channel, String> {

    fun findChannelByAudienceToken(audienceToken: String): Flow<Channel>

    fun findChannelByPresenterToken(presenterToken: String): Flow<Channel>

    fun findChannelByPresenterTokenOrAudienceToken(presenterToken: String, audienceToken: String): Flow<Channel>
}