package org.qask.backend.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.qask.backend.bloomfilter.BloomFilter
import org.qask.backend.models.domainModels.Channel
import org.qask.backend.models.viewModels.ChannelDetailForHostVM
import org.qask.backend.models.viewModels.ChannelDetailVM
import org.qask.backend.models.viewModels.CreateChannelVM
import org.qask.backend.repositories.ChannelRepository
import org.qask.backend.toDetail
import org.qask.backend.toDomainModel
import org.qask.backend.toHostDetail
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.util.*

@ExperimentalCoroutinesApi
@Primary
@Service
class ReactiveChannelService(
    val channelRepository: ChannelRepository
) : IReactiveChannelService {

    val filter = BloomFilter(0.01, 1_000_000)

    override suspend fun createChannel(createChannelVM: CreateChannelVM): ChannelDetailForHostVM {
        // generate until unique token is obtained
        var presenterToken = UUID.randomUUID().toString()

        while (filter.isPresent(presenterToken)) {
            presenterToken = UUID.randomUUID().toString()
        }

        var audienceToken = UUID.randomUUID().toString()

        while (filter.isPresent(audienceToken)) {
            audienceToken = UUID.randomUUID().toString()
        }

        val savedChannel = channelRepository
            .save(createChannelVM.toDomainModel(presenterToken, audienceToken))
            .awaitSingle()

        return savedChannel.toHostDetail()
    }

    suspend fun getChannelWithToken(token: String): Channel? {
        return channelRepository.findChannelByPresenterTokenOrAudienceToken(token, token).firstOrNull()
    }

    override suspend fun getChannelDetail(token: String): ChannelDetailVM {
        val channel = getChannelWithToken(token) ?: throw NoSuchElementException("Channel not found")
        return channel.toDetail()
    }
}