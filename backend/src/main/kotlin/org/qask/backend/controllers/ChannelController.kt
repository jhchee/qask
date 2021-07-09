package org.qask.backend.controllers

import org.qask.backend.exceptions.UnauthorizedException
import org.qask.backend.models.viewModels.ChannelDetailForHostVM
import org.qask.backend.models.viewModels.ChannelDetailVM
import org.qask.backend.models.viewModels.CreateChannelVM
import org.qask.backend.services.IReactiveChannelService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("api.v1.qask/channel")
@CrossOrigin(origins = arrayOf("http://127.0.0.1:3000", "http://localhost:3000"))
class ChannelRestController(
    val channelService: IReactiveChannelService,
) {

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(e: NoSuchElementException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(java.lang.IllegalArgumentException::class)
    fun handleBadRequest(e: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedRequest(e: UnauthorizedException): ResponseEntity<String> {
        return ResponseEntity(e.message, HttpStatus.BAD_REQUEST)
    }


    @PostMapping("/host/create")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createChannel(@RequestBody createChannelVM: CreateChannelVM): ChannelDetailForHostVM {
        return channelService.createChannel(createChannelVM)
    }

    @GetMapping("/{token}")
    suspend fun getChannel(@PathVariable token: String): ChannelDetailVM {
        return channelService.getChannelDetail(token)
    }

}