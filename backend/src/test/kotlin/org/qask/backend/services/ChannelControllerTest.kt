package org.qask.backend.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.qask.backend.compareTimeWithTolerance
import org.qask.backend.models.domainModels.Channel
import org.qask.backend.models.viewModels.ChannelDetailVM
import org.qask.backend.models.viewModels.CreateChannelVM
import org.qask.backend.repositories.ChannelRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.time.Instant
import kotlin.time.ExperimentalTime


@ExperimentalTime
@ExperimentalCoroutinesApi
@AutoConfigureWebTestClient
@AutoConfigureDataMongo
@SpringBootTest
internal class ChannelControllerTest @Autowired constructor(
    val client: WebTestClient,
) {
    private val baseUrl = "/api.v1.qask/channel"

    @Autowired
    private lateinit var actualRepository: ChannelRepository

//    private lateinit var channelService: ReactiveChannelServiceImpl
//
//    private lateinit var channelRestController: ChannelRestController
//
//    @BeforeAll
//    fun setup() {
//        channelService = ReactiveChannelServiceImpl(channelRepository)
//        channelRestController = ChannelRestController(channelService)
//    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("POST /create")
    inner class CreateChannel {
        @Test
        fun `Should add new channel`() {
            val durationInMinutes = 100
            val newChannel = CreateChannelVM(
                name = "Channel 1",
                durationInMinute = durationInMinutes,
                description = "This is a description"
            )

            // compute the end time
            val now = Instant.now()
            val expectedEndTime = now.plusSeconds(durationInMinutes.toLong() * 60)

            client.post().uri("$baseUrl/host/create")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(newChannel)
                .exchange()
                .expectStatus().isCreated
                .expectBody<ChannelDetailVM>().consumeWith { response ->
                    val result = response.responseBody
                    assertNotNull(result)
                    result!!.let {
                        assertThat(it.name).matches(newChannel.name)
                        assertThat(it.description).matches(newChannel.description)
                        assertThat(it.endTime).matches { actualEndTime ->
                            actualEndTime.compareTimeWithTolerance(
                                expectedEndTime
                            )
                        }
                    }
                }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("GET /channel")
    inner class GetChannel {
        val exampleChannel = Channel(
            name = "This is a channel name",
            description = "This is some description",
            endTime = Instant.now(),
            presenterToken = "presenter-token",
            audienceToken = "audience-token"
        )

        lateinit var savedChannel: Channel

        @BeforeAll
        fun setup() {
            savedChannel = actualRepository.save(exampleChannel).block()!!
        }

        @AfterAll
        fun tearDown() {
            actualRepository.deleteAll().block()
        }

        @Test
        fun `Get existing channel with presenter token OR audience token`() {
            val audienceToken = savedChannel.audienceToken
            val presenterToken = savedChannel.presenterToken

            client.get()
                .uri("$baseUrl/$audienceToken")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<ChannelDetailVM>().consumeWith { response ->
                    val result = response.responseBody
                    assertNotNull(result)
                    result!!.let {
                        assertThat(it.name).matches(savedChannel.name)
                        assertThat(it.description).matches(savedChannel.description)
                    }
                }

            client.get()
                .uri("$baseUrl/$presenterToken")
                .exchange()
                .expectStatus().is2xxSuccessful
                .expectBody<ChannelDetailVM>().consumeWith { response ->
                    val result = response.responseBody
                    assertNotNull(result)
                    result!!.let {
                        assertThat(it.name).matches(savedChannel.name)
                        assertThat(it.description).matches(savedChannel.description)
                    }
                }
        }

        @Test
        fun `Get channel with wrong presenter token OR audience token`() {
            val token = "fake 1"

            client.get()
                .uri("$baseUrl/$token")
                .exchange()
                .expectStatus().isNotFound
        }
    }

}