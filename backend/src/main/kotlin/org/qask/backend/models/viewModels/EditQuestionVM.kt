package org.qask.backend.models.viewModels

// used in put method
data class EditQuestionVM(
    val token: String,
    val id: String,
    val action: String? = null
)