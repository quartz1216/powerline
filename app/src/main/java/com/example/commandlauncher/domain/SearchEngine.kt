package com.example.commandlauncher.domain

import android.content.Context
import com.example.commandlauncher.domain.models.AppInfo
import com.example.commandlauncher.domain.models.ResultType
import com.example.commandlauncher.domain.models.SearchResult
import java.util.UUID

class SearchEngine {

    fun search(query: String, apps: List<AppInfo>): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchResult>()
        val trimmedQuery = query.trim()

        // 1. Calculation
        val calcResult = Calculator.evaluate(trimmedQuery)
        if (calcResult != null) {
            results.add(
                SearchResult(
                    id = "calc_${UUID.randomUUID()}",
                    title = calcResult,
                    subtitle = "Calculate",
                    type = ResultType.CALC,
                    payload = calcResult
                )
            )
        }

        // 2. Apps
        val appMatches = apps.filter {
            it.label.contains(trimmedQuery, ignoreCase = true)
        }.take(5) // Limit to 5 app matches to avoid clutter? Or show all? Spotlight shows many. I'll show reasonable amount.

        results.addAll(appMatches.map { app ->
            SearchResult(
                id = app.packageName,
                title = app.label,
                subtitle = "Application",
                icon = app.icon,
                type = ResultType.APP,
                packageName = app.packageName,
                className = app.className
            )
        })

        // 3. Web Search
        // Always add web search
        results.add(
            SearchResult(
                id = "web_search",
                title = "Search Web",
                subtitle = trimmedQuery,
                type = ResultType.WEB,
                payload = trimmedQuery
            )
        )

        return results
    }
}
