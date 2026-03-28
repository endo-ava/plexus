package dev.plexus.shared.core.domain.repository

import dev.plexus.shared.core.domain.model.SystemPromptName
import dev.plexus.shared.core.domain.model.SystemPromptResponse

interface SystemPromptRepository {
    suspend fun getSystemPrompt(name: SystemPromptName): RepositoryResult<SystemPromptResponse>

    suspend fun updateSystemPrompt(
        name: SystemPromptName,
        content: String,
    ): RepositoryResult<SystemPromptResponse>
}
