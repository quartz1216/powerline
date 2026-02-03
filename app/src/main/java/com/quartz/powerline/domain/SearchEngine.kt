package com.quartz.powerline.domain

import android.content.Context
import android.util.Patterns
import androidx.core.content.ContextCompat
import com.quartz.powerline.data.SettingsRepository
import com.quartz.powerline.domain.models.AppInfo
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import java.util.UUID

class SearchEngine(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val urlPattern = java.util.regex.Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(/.*)?$"
    )

    fun search(query: String, apps: List<AppInfo>): List<SearchResult> {
        val hiddenApps = settingsRepository.getHiddenApps()
        val visibleApps = apps.filter { !hiddenApps.contains(it.packageName) }

        // 0. Empty Query -> Recent History
        if (query.isBlank()) {
            val history = settingsRepository.getHistory()
            val maxRecent = settingsRepository.getMaxRecentItems()

            // Deduplicate by ID to show unique recent items
            val distinctHistory = history.distinctBy { it.id }.take(maxRecent)

            return distinctHistory.mapNotNull { item ->
                when (item.type) {
                    ResultType.APP -> {
                        // Check if it's the special Settings action
                        if (item.id == "settings_action") {
                             SearchResult(
                                id = "settings_action",
                                title = "Settings",
                                subtitle = "Recent",
                                icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_preferences),
                                type = ResultType.APP,
                                payload = "settings_action"
                            )
                        } else {
                            val app = visibleApps.find { it.packageName == item.id }
                            if (app != null) {
                                SearchResult(
                                    id = app.packageName,
                                    title = app.label,
                                    subtitle = "Recent",
                                    icon = app.icon,
                                    type = ResultType.APP,
                                    packageName = app.packageName,
                                    className = app.className
                                )
                            } else null
                        }
                    }
                    ResultType.WEB -> {
                        SearchResult(
                            id = item.id,
                            title = item.title,
                            subtitle = "Recent Web",
                            type = ResultType.WEB,
                            payload = item.title
                        )
                    }
                    ResultType.CALC -> {
                         SearchResult(
                            id = item.id,
                            title = item.title,
                            subtitle = "Recent Calc",
                            type = ResultType.CALC,
                            payload = item.title
                        )
                    }
                }
            }
        }

        val trimmedQuery = query.trim()
        val lowerQuery = trimmedQuery.lowercase()
        val results = mutableListOf<SearchResult>()

        // 1. Settings Command
        if (lowerQuery == "settings") {
            results.add(
                SearchResult(
                    id = "settings_action",
                    title = "Settings",
                    subtitle = "Open Settings",
                    icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_preferences),
                    type = ResultType.APP,
                    payload = "settings_action"
                )
            )
        }

        // 2. Calculation
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

        // 3. Apps (with Aliases & Frequency)
        val aliases = settingsRepository.getAliases()
        val history = settingsRepository.getHistory()
        val scores = history.groupingBy { it.id }.eachCount()

        val aliasPackage = aliases[trimmedQuery]

        val appMatches = visibleApps.filter { app ->
            val matchName = app.label.contains(trimmedQuery, ignoreCase = true)
            val matchAlias = aliasPackage == app.packageName
            matchName || matchAlias
        }.sortedWith(
             compareByDescending<AppInfo> { aliasPackage == it.packageName }
                .thenByDescending { scores[it.packageName] ?: 0 }
                .thenBy { it.label }
        )

        results.addAll(appMatches.map { app ->
            SearchResult(
                id = app.packageName,
                title = app.label,
                subtitle = if (aliasPackage == app.packageName) "Alias" else "Application",
                icon = app.icon,
                type = ResultType.APP,
                packageName = app.packageName,
                className = app.className
            )
        })

        // 4. Web Search & Direct URL
        if (urlPattern.matcher(trimmedQuery).matches()) {
             results.add(
                SearchResult(
                    id = "web_direct",
                    title = "Go to $trimmedQuery",
                    subtitle = "Web Address",
                    type = ResultType.WEB,
                    payload = if (trimmedQuery.startsWith("http")) trimmedQuery else "https://$trimmedQuery"
                )
            )
        }

        // Search Web (Fallthrough)
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
