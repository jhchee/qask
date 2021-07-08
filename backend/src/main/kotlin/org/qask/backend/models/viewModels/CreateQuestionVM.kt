package org.qask.backend.models.viewModels

// used in post method
// post by questioner
data class CreateQuestionVM(
    val token: String,
    val content: String? = "",
    val questionerName: String? = null
)