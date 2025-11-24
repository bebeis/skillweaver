package com.bebeis.skillweaver.agent.config

import com.embabel.agent.core.ToolGroup
import com.embabel.agent.core.ToolGroupDescription
import com.embabel.agent.core.ToolGroupPermission
import com.embabel.agent.tools.mcp.McpToolGroup
import io.modelcontextprotocol.client.McpSyncClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ToolsConfig(
    private val mcpSyncClients: List<McpSyncClient>,
) {

    /**
     * GitHub 저장소 검색 및 코드 예제 수집을 위한 MCP Tool Group
     * - 공식 저장소 및 학습 자료 검색
     * - 예제 코드 및 프로젝트 템플릿 탐색
     * - 릴리스 정보 및 버전 확인
     */
    @Bean
    fun mcpGitHubToolsGroup(): ToolGroup {
        return McpToolGroup(
            description = ToolGroupDescription(
                description = "GitHub repository search and code examples for learning resources",
                role = GITHUB
            ),
            name = "github-mcp",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS
            ),
            clients = mcpSyncClients,
            filter = {
                it.toolDefinition.name().contains("github", ignoreCase = true) ||
                it.toolDefinition.name().contains("repository", ignoreCase = true) ||
                it.toolDefinition.name().contains("search_code", ignoreCase = true)
            },
        )
    }

    /**
     * YouTube 비디오 검색을 위한 MCP Tool Group
     * - 기술 튜토리얼 및 강의 영상 검색
     * - 한국어/영어 학습 자료 필터링
     * - 영상 길이 기반 학습 시간 예측
     */
    @Bean
    fun mcpYouTubeToolsGroup(): ToolGroup {
        return McpToolGroup(
            description = ToolGroupDescription(
                description = "YouTube video search for learning tutorials and courses",
                role = YOUTUBE
            ),
            name = "youtube-mcp",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS
            ),
            clients = mcpSyncClients,
            filter = {
                it.toolDefinition.name().contains("youtube", ignoreCase = true) ||
                it.toolDefinition.name().contains("video", ignoreCase = true)
            },
        )
    }

    companion object {
        const val GITHUB = "github"
        const val YOUTUBE = "youtube"
    }
}
