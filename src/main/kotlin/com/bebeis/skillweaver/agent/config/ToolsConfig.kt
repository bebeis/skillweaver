package com.bebeis.skillweaver.agent.config

import com.embabel.agent.core.CoreToolGroups
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
     * 웹 검색용 MCP Tool Group (Brave/Tavily 등)
     * - Brave가 rate limit에 걸릴 경우 Tavily를 통한 대체 검색 가능
     */
    @Bean
    fun mcpWebSearchToolsGroup(): ToolGroup {
        return McpToolGroup(
            description = CoreToolGroups.WEB_DESCRIPTION,
            name = "websearch-mcp",
            provider = "Docker",
            permissions = setOf(
                ToolGroupPermission.INTERNET_ACCESS
            ),
            clients = mcpSyncClients,
            filter = {
                val name = it.toolDefinition.name().lowercase()
                val isBrave = name.contains("brave")
                val isTavily = name.contains("tavily")
                val isWebSearch = name.contains("web_search") ||
                    name.contains("web-search") ||
                    (name.contains("search") && name.contains("web"))
                // Prefer Tavily to avoid Brave rate limits; keep other non-Brave web-search tools as fallback
                // (Brave is excluded from this group).
                isTavily || (isWebSearch && !isBrave)
            },
        )
    }

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
