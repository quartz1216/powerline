package com.quartz.powerline.plugins.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import com.quartz.powerline.data.PowerlineDefaults
import com.quartz.powerline.domain.models.ResultType
import com.quartz.powerline.domain.models.SearchResult
import com.quartz.powerline.plugins.Plugin

class WebSearchPlugin : Plugin {
    override val id = "web_search"
    override val name = "Web Search"
    override val description = "Search the web or open URLs"
    override val icon = Icons.Default.Search

    private val urlPattern = java.util.regex.Pattern.compile(
        "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,6}(/.*)?$"
    )

    override suspend fun search(query: String, context: Context): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val trimmedQuery = query.trim()
        val results = mutableListOf<SearchResult>()

        // Direct URL
        if (urlPattern.matcher(trimmedQuery).matches()) {
            results.add(
                SearchResult(
                    id = "web_direct",
                    pluginId = id,
                    title = "Go to $trimmedQuery",
                    subtitle = "Web Address",
                    type = ResultType.WEB,
                    payload = if (trimmedQuery.startsWith("http")) trimmedQuery else "https://$trimmedQuery"
                )
            )
        }

        // Search Engines
        val engines = PowerlineDefaults.SEARCH_ENGINES.filter { it.isEnabled }

        // Check for aliases (prefix + space)
        val aliasMatches = engines.filter { !it.alias.isNullOrBlank() && query.startsWith("${it.alias} ") }

        if (aliasMatches.isNotEmpty()) {
            // Use specific engine(s)
            for (engine in aliasMatches) {
                val realQuery = query.substring(engine.alias!!.length + 1).trim()
                if (realQuery.isBlank()) continue

                val engineUrl = if (engine.url.startsWith("http")) engine.url else "https://${engine.url}"
                val finalUrl = engineUrl.replace("%s", Uri.encode(realQuery))
                val domain = Uri.parse(engineUrl).host
                val faviconUrl = if (domain != null) "https://www.google.com/s2/favicons?sz=64&domain_url=$domain" else null

                results.add(
                    SearchResult(
                        id = "web_search:${engine.id}:$realQuery",
                        pluginId = id,
                        title = realQuery,
                        subtitle = engine.name,
                        type = ResultType.WEB,
                        payload = finalUrl,
                        faviconUrl = faviconUrl
                    )
                )
            }
        } else {
            // Use default engines (no alias required)
            val defaultEngines = engines.filter { it.alias.isNullOrBlank() }

            for (engine in defaultEngines) {
                val engineUrl = if (engine.url.startsWith("http")) engine.url else "https://${engine.url}"
                val finalUrl = engineUrl.replace("%s", Uri.encode(trimmedQuery))
                val domain = Uri.parse(engineUrl).host
                val faviconUrl = if (domain != null) "https://www.google.com/s2/favicons?sz=64&domain_url=$domain" else null

                results.add(
                    SearchResult(
                        id = "web_search:${engine.id}:$trimmedQuery",
                        pluginId = id,
                        title = trimmedQuery,
                        subtitle = engine.name,
                        type = ResultType.WEB,
                        payload = finalUrl,
                        faviconUrl = faviconUrl
                    )
                )
            }
        }

        return results
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        val url = if (result.payload!!.startsWith("http")) result.payload else "https://www.google.com/search?q=${Uri.encode(result.payload)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
