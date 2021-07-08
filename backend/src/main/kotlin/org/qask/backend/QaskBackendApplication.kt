package org.qask.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class QaskBackendApplication

fun main(args: Array<String>) {
    runApplication<QaskBackendApplication>(*args)
}
