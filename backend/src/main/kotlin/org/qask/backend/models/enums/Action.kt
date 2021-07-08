package org.qask.backend.models.enums

// series of possible actions
enum class Action {
    VOTE_UP, // increment vote count
    VOTE_DOWN, // decrement vote count
    QUEUE, // make status DEFAULT -> QUEUED
    DELETE, // remove the question
    ANSWERED, // makke status QUEUED -> ANSWERED
}
