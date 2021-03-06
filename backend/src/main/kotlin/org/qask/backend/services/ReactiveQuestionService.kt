package org.qask.backend.services;

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.qask.backend.exceptions.UnauthorizedException
import org.qask.backend.models.domainModels.Channel
import org.qask.backend.models.domainModels.Question
import org.qask.backend.models.enums.Action
import org.qask.backend.models.enums.Status
import org.qask.backend.models.viewModels.*
import org.qask.backend.repositories.ChannelRepository
import org.qask.backend.repositories.QuestionRepository
import org.qask.backend.toDomainModel
import org.qask.backend.toInput
import org.qask.backend.toPayload
import org.qask.backend.toPayloads
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@ExperimentalCoroutinesApi
@Primary
@Service
class ReactiveQuestionService(
    val questionRepository: QuestionRepository,
    val channelRepository: ChannelRepository,
) : IReactiveQuestionService {
    private val channelsMap = ConcurrentHashMap<String, MutableSharedFlow<QuestionPayload>>()
    private val timer = Timer("Dispose task timer")

    override fun latest(channelId: String): Flow<QuestionPayload> {
        return questionRepository.findByChannelIdOrderBySentAsc(channelId).toPayloads()
    }

    override suspend fun getChannelWithAnyToken(token: String): Channel? {
        return channelRepository.findChannelByPresenterTokenOrAudienceToken(token, token).firstOrNull()
    }

    override fun stream(channelId: String): Flow<QuestionPayload> {
        return channelRepository.findById(channelId)
            .asFlow()
            .onEmpty { // on empty channel return
                emitAll(emptyFlow())
            }
            .flatMapLatest { // if not empty
                if (it.endTime > Instant.now() && it.id != null) {
                    channelsMap.getOrPut(it.id) {
                        setupTimerToRemoveMap(it.id, it.endTime)
                        MutableSharedFlow()
                    }
                } else {
                    emptyFlow() // empty flow indicates the session has ended
                }
            }
    }

    suspend fun getChannelWithAudienceToken(token: String): Channel? {
        return channelRepository.findChannelByAudienceToken(token).firstOrNull()
    }

    suspend fun getChannelWithPresenterToken(token: String): Channel? {
        return channelRepository.findChannelByPresenterToken(token).firstOrNull()
    }

    override suspend fun postQuestion(createQuestionVM: CreateQuestionVM) {
        // assume audience that is posting the question
        val channel = getChannelWithAudienceToken(createQuestionVM.token)

        // simple validation, when the channel is null, the audience token is not valid
        if (channel == null) throw UnauthorizedException("You are not allowed to not perform this action.")

        val channelId = channel.id!!

        val savedQuestion = questionRepository.insert(createQuestionVM.toDomainModel(channelId = channelId))
            .awaitSingle()

        // post to kafka queue for inference
        sendQuestionProducer(savedQuestion)

        savedQuestion.pushQuestion(channelId = channelId)
    }

    suspend fun Question.pushQuestion(channelId: String) {
        val payload = this.toPayload()
        channelsMap[channelId]?.emit(payload)
    }

    fun Question.pushQuestionBlock(channelId: String) {
        val payload = this.toPayload()
        runBlocking {
            channelsMap[channelId]?.emit(payload)
        }
    }

    @KafkaListener(
        topics = ["\${insincere.questions.topic.name}"],
        groupId = "insincere-question-spring-consumer",
        containerFactory = "insincereQuestionListenerContainerFactory"
    )
    fun listenInsincereQuestionConsumer(@Payload outputs: List<QuestionInferOutput>) {
        for (output in outputs) {
            val entity = questionRepository.findById(output.id).block()
            if (entity != null) {
                println("Score for question id #${entity.id}: ${output.isInsincereScore}")
                entity.isInsincere = output.isInsincereScore > 0.35
                val savedQuestion = questionRepository.save(entity).block()
                savedQuestion?.pushQuestionBlock(output.channelId)
            }
        }
    }


    @Autowired
    lateinit var questionInputTemplate: KafkaTemplate<String, QuestionInput>

    @Value(value = "\${questions.topic.name}")
    lateinit var questionsTopic: String

    suspend fun sendQuestionProducer(question: Question) {
        println("Sending for question id #${question.id} for inference")
        questionInputTemplate.send(questionsTopic, question.toInput())
    }

    override suspend fun hostEdit(editQuestionVM: EditQuestionVM) {
        // assume host that is editing the question
        val channel = getChannelWithPresenterToken(editQuestionVM.token)

        // if the channel is null, this means the audience token is not valid
        if (channel == null) throw UnauthorizedException("You are not allowed to not perform this action.")

        val entity = questionRepository.findById(editQuestionVM.id).awaitFirstOrNull()
            ?: throw NoSuchElementException("Question with question id ${editQuestionVM.id} not found")

        val channelId = channel.id!!

        // we are not allowing people using token of some other channel
        if (entity.channelId != channelId) {
            throw UnauthorizedException("You are not allowed to not perform this action.")
        }

        val action = Action.values().find { it.name == editQuestionVM.action }
            ?: throw IllegalAccessException("No action is identified")

        val newEntity = entity.applyHostAction(action)

        val savedQuestion = questionRepository.save(newEntity).awaitSingle()

        savedQuestion.pushQuestion(channelId)
    }

    override suspend fun audienceEdit(editQuestionVM: EditQuestionVM) {
        // assume host that is editing the question
        val channel = getChannelWithAudienceToken(editQuestionVM.token)

        // if the channel is null, this means the audience token is not valid
        if (channel == null) throw UnauthorizedException("You are not allowed to not perform this action.")

        val entity = questionRepository.findById(editQuestionVM.id).awaitFirstOrNull()
            ?: throw NoSuchElementException("Question with question id ${editQuestionVM.id} not found")

        val channelId = channel.id!!

        val action = Action.values().find { it.name == editQuestionVM.action }
            ?: throw IllegalAccessException("No action is identified")

        val newEntity = entity.applyQuestionerAction(action)

        val savedQuestion = questionRepository.save(newEntity).awaitSingle()

        savedQuestion.pushQuestion(channelId)
    }

    // scope host's action
    fun Question.applyHostAction(action: Action?): Question {
        when (action) {
            Action.QUEUE -> {
                status = Status.QUEUED.name
            }
            Action.ANSWERED -> {
                status = Status.ANSWERED.name
            }
            Action.VOTE_UP -> {
                likeCount++
            }
            Action.VOTE_DOWN -> {
                if (likeCount > 0) likeCount--
            }
            Action.DELETE -> {
                isDeleted = true
            }
            else -> {

            }
        }
        return this
    }

    // scope questioner's action
    fun Question.applyQuestionerAction(action: Action?): Question {
        when (action) {
            Action.VOTE_UP -> {
                likeCount++
            }
            Action.VOTE_DOWN -> {
                if (likeCount > 0) likeCount--
            }
            else -> {
            }
        }
        return this
    }

    // setup a timer to remove channel reference
    private fun setupTimerToRemoveMap(channelId: String, endTime: Instant) {
        val disposeTask = object : TimerTask() {
            override fun run() {
                channelsMap.remove(channelId)
            }
        }
        timer.schedule(disposeTask, Date.from(endTime))
    }

}
