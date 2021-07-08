package org.qask.backend.services

import org.qask.backend.models.viewModels.ChannelDetailForHostVM
import org.qask.backend.models.viewModels.ChannelDetailVM
import org.qask.backend.models.viewModels.CreateChannelVM

interface IReactiveChannelService {

    suspend fun createChannel(createChannelVM: CreateChannelVM): ChannelDetailForHostVM

    suspend fun getChannelDetail(token: String): ChannelDetailVM
}