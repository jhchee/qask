package org.qask.backend.controllers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart
import org.qask.backend.exceptions.UnauthorizedException
import org.qask.backend.models.viewModels.CreateQuestionVM
import org.qask.backend.models.viewModels.EditQuestionVM
import org.qask.backend.models.viewModels.QuestionPayload
import org.qask.backend.services.IReactiveQuestionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

// if you have question, ask!
@Controller
@MessageMapping("api.v1.qask")
@CrossOrigin(origins = arrayOf("http://localhost:3000"))
class WebsocketResource(val questionService: IReactiveQuestionService) {

    @MessageMapping("stream/{token}")
    suspend fun stream(@DestinationVariable token: String): Flow<QuestionPayload> {
        val channel = questionService.getChannelWithAnyToken(token)
            ?: return emptyFlow()
        val channelId = channel.id!!

        return questionService
            .stream(channelId)
            .onStart { emitAll(questionService.latest(channelId)) }
    }

}

@RestController
@RequestMapping("api.v1.qask/question")
@CrossOrigin(origins = arrayOf("http://localhost:3000"))
class QuestionRestResource(
    val questionService: IReactiveQuestionService,
) {

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(java.lang.IllegalAccessException::class)
    fun handleBadRequest(e: IllegalAccessException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedRequest(e: UnauthorizedException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.BAD_REQUEST)
    }

    @PostMapping("/host/edit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun hostEditQuestion(@RequestBody editQuestionVM: EditQuestionVM) {
        questionService.hostEdit(editQuestionVM)
    }

    @PostMapping("/audience/edit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    suspend fun questionerEditQuestion(@RequestBody editQuestionVM: EditQuestionVM) {
        questionService.audienceEdit(editQuestionVM)
    }

    @PostMapping("/audience/create")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createQuestion(@RequestBody createQuestionVM: CreateQuestionVM) {
        questionService.postQuestion(createQuestionVM)
    }
}