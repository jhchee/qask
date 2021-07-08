package org.qask.backend.controllers

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HtmlController {

    // get mapped to localhost:8080
    @GetMapping("/")
    suspend fun index(model: Model): String {
        return "chatrs"
    }
}