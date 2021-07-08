package org.qask.backend

import kotlinx.coroutines.flow.collect
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlow

suspend fun RSocketRequester.send(routeUrl: String) {
    route(routeUrl)
        .retrieveFlow<Void>()
        .collect()
}