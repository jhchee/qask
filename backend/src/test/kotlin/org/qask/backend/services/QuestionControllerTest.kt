package org.qask.backend.services

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.qask.backend.expectNumberOfEvents
import org.qask.backend.models.domainModels.Channel
import org.qask.backend.models.domainModels.Question
import org.qask.backend.models.enums.Action
import org.qask.backend.models.enums.Status
import org.qask.backend.models.viewModels.CreateQuestionVM
import org.qask.backend.models.viewModels.QuestionPayload
import org.qask.backend.repositories.ChannelRepository
import org.qask.backend.repositories.QuestionRepository
import org.qask.backend.toEditVM
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlow
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.URI
import java.time.Instant
import kotlin.time.seconds

@ExperimentalCoroutinesApi
@AutoConfigureDataMongo
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
internal class QuestionControllerTest(
    @Autowired val questionRepository: QuestionRepository,
    @Autowired val channelRepository: ChannelRepository,
    @Autowired val rsocketBuilder: RSocketRequester.Builder,
    @Autowired val client: WebTestClient,
    @LocalServerPort serverPort: Int
) {
    private val baseUrl = "api.v1.qask"
    private val wsURI = "ws://localhost:${serverPort}/rsocket"
    private val restURI = "$baseUrl/question"
    private val routeUrl = "${baseUrl}.stream"

    private val TEST_CHANNEL_ID = "testChannelId"

    private val now: Instant = Instant.now()

    private val questions = listOf(
        Question(
            content = "Content 1",
            questionerName = "Name 1",
            sent = now,
            channelId = TEST_CHANNEL_ID,
            id = "1"
        ),
        Question(
            content = "Content 2",
            questionerName = "Name 2",
            sent = now.plusSeconds(1),
            channelId = TEST_CHANNEL_ID,
            id = "2"
        ),
    )

    val sampleQuestionSize = questions.size

    val channel = Channel(
        name = "Channel 1",
        description = "Description 1",
        presenterToken = "presenter-token",
        audienceToken = "audience-token",
        endTime = now.plusSeconds(60L * 60L), // 1 hour
        id = TEST_CHANNEL_ID
    )

    lateinit var savedChannel: Channel


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test audience only actions")
    inner class AudienceAction {
        @BeforeEach
        fun setup() {
            savedChannel = channelRepository.save(channel).block()!!
        }

        @AfterEach
        fun tearDown() {
            channelRepository.deleteAll().block()
            questionRepository.deleteAll().block()
        }

        @Test
        fun `Test the questions API is streaming latest questions`() {
            runBlocking {
                questionRepository.saveAll(questions).collectList().block()
                channelRepository.save(channel).block()

                val rSocketRequester = rsocketBuilder.websocket(URI(wsURI))
                val latestMessage = CreateQuestionVM(
                    content = "Latest content",
                    questionerName = "Latest name",
                    token = savedChannel.audienceToken
                )

                rSocketRequester
                    .route("$routeUrl/${savedChannel.audienceToken}")
                    .retrieveFlow<QuestionPayload>()
                    .test {
                        expectNumberOfEvents(sampleQuestionSize)
                        expectNoEvents()

                        // create new question
                        launch {
                            client.post().uri("$restURI/audience/create")
                                .bodyValue(latestMessage)
                                .exchange()
                                .expectStatus().isCreated
                        }

                        assertThat(expectItem())
                            .matches {
                                it.content == latestMessage.content
                            }

                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }


        @Test
        fun `Test up-voting an existing question`() {

            runBlocking {
                val initialVoteCount = 5
                val editInstance = Question(
                    content = "",
                    channelId = TEST_CHANNEL_ID,
                    questionerName = "",
                    likeCount = initialVoteCount,
                    sent = now,
                    id = "Test"
                )

                questionRepository.save(editInstance).block()

                delay(2.seconds)

                val rSocketRequester = rsocketBuilder.websocket(URI(wsURI))

                rSocketRequester
                    .route("$routeUrl/${savedChannel.audienceToken}")
                    .retrieveFlow<QuestionPayload>()
                    .test {
                        expectNumberOfEvents(1)
                        expectNoEvents()

                        // up-vote a question
                        val requestBody = editInstance.toEditVM(savedChannel.audienceToken, Action.VOTE_UP)

                        launch {
                            client.post().uri("$restURI/audience/edit")
                                .bodyValue(requestBody)
                                .exchange()
                                .expectStatus().isNoContent
                        }

                        assertThat(expectItem()).matches {
                            it.id == editInstance.id && it.likeCount == initialVoteCount + 1
                        }

                        cancelAndIgnoreRemainingEvents()
                    }
            }
        }

    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test host only actions")
    inner class HostAction {
        @BeforeEach
        fun setup() {
            savedChannel = channelRepository.save(channel).block()!!
        }

        @AfterEach
        fun tearDown() {
            channelRepository.deleteAll().block()
            questionRepository.deleteAll().block()
        }

        @Test
        fun `Test question marking is working`() {
            runBlocking {
                val editInstance = Question(
                    content = "",
                    channelId = TEST_CHANNEL_ID,
                    questionerName = "",
                    likeCount = 5,
                    sent = now,
                    id = "Test"
                )

                questionRepository.save(editInstance).block()

                delay(2.seconds)

                val rSocketRequester = rsocketBuilder.websocket(URI(wsURI))

                rSocketRequester
                    .route("$routeUrl/${savedChannel.audienceToken}")
                    .retrieveFlow<QuestionPayload>()
                    .test {
                        expectNumberOfEvents(1)
                        expectNoEvents()

                        val requestBody = editInstance.toEditVM(savedChannel.presenterToken, Action.QUEUE)

                        // edit a question
                        launch {
                            client.post().uri("$restURI/host/edit")
                                .bodyValue(requestBody)
                                .exchange()
                                .expectStatus().isNoContent
                        }

                        assertThat(expectItem()).matches {
                            it.id == editInstance.id && it.status == Status.QUEUED.name
                        }

                        cancelAndIgnoreRemainingEvents()
                    }
            }

        }


        @Test
        fun `Test question answered is working`() {
            runBlocking {
                val editInstance = Question(
                    content = "",
                    channelId = TEST_CHANNEL_ID,
                    questionerName = "",
                    likeCount = 5,
                    sent = now,
                    id = "Test"
                )

                questionRepository.save(editInstance).block()

                delay(2.seconds)

                val rSocketRequester = rsocketBuilder.websocket(URI(wsURI))

                rSocketRequester
                    .route("$routeUrl/${savedChannel.audienceToken}")
                    .retrieveFlow<QuestionPayload>()
                    .test {
                        expectNumberOfEvents(1)
                        expectNoEvents()

                        // answer a question
                        val requestBody = editInstance.toEditVM(savedChannel.presenterToken, Action.ANSWERED)

                        launch {
                            client.post().uri("$restURI/host/edit")
                                .bodyValue(requestBody)
                                .exchange()
                                .expectStatus().isNoContent
                        }

                        assertThat(expectItem()).matches {
                            it.id == editInstance.id && it.status == Status.ANSWERED.name
                        }

                        cancelAndIgnoreRemainingEvents()
                    }
            }

        }

    }

}