package dev.quxomiya.powerline.plugins.impl

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import dev.quxomiya.powerline.data.PowerlineDefaults
import dev.quxomiya.powerline.data.UserDataRepository
import dev.quxomiya.powerline.domain.models.AppInfo
import dev.quxomiya.powerline.domain.models.ResultType
import dev.quxomiya.powerline.domain.models.SearchResult
import dev.quxomiya.powerline.plugins.Plugin
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

class AppLauncherPlugin(
    private val userDataRepository: UserDataRepository,
    private val visibleAppsFlow: StateFlow<List<AppInfo>>
) : Plugin {
    override val id = "app_launcher"
    override val name = "Apps"
    override val description = "Search and launch installed applications"
    override val icon = Icons.Default.Apps

    override suspend fun search(query: String, context: Context): List<SearchResult> {
        val visibleApps = visibleAppsFlow.value

        if (query.isBlank()) return emptyList()

        val trimmedQuery = query.trim()
        val lowerQuery = trimmedQuery.lowercase()

        val favorites = userDataRepository.getFavoriteApps()
        val weights = userDataRepository.getAppWeights()

        val appMatches = visibleApps.mapNotNull { app ->
            val isFavorite = favorites.contains(app.packageName)

            val nameScore = fuzzyMatch(lowerQuery, app.label)

            var pkgScore = 0
            if (PowerlineDefaults.ENABLE_PACKAGE_SEARCH) {
                val searchPkg = if (app.packageName.contains('.')) {
                    app.packageName.substringAfter('.')
                } else {
                    app.packageName
                }
                pkgScore = fuzzyMatch(lowerQuery, searchPkg)
            }

            val matchScore = max(nameScore, pkgScore)
            if (matchScore <= 0) return@mapNotNull null

            var finalScore = matchScore
            if (PowerlineDefaults.ENABLE_WEIGHT_SEARCH) {
                finalScore += (weights[app.packageName] ?: 0)
            }
            if (isFavorite) {
                finalScore += 100_000
            }

            app to finalScore
        }.sortedWith(
            compareByDescending<Pair<AppInfo, Int>> { it.second }
                .thenBy { it.first.label }
        )

        return appMatches.map { (app, _) ->
            SearchResult(
                id = app.packageName,
                pluginId = id,
                title = app.label,
                subtitle = app.packageName,
                icon = app.icon,
                type = ResultType.APP,
                packageName = app.packageName,
                className = app.className
            )
        }
    }

    private fun fuzzyMatch(lowerQuery: String, target: String): Int {
        if (lowerQuery.isEmpty()) return 0
        val t = target.lowercase()

        // Exact, Prefix, Substring
        if (t.contains(lowerQuery)) return 100

        // Discontinuous (Subsequence)
        var qIdx = 0
        var tIdx = 0
        while (qIdx < lowerQuery.length && tIdx < t.length) {
            if (lowerQuery[qIdx] == t[tIdx]) {
                qIdx++
            }
            tIdx++
        }
        if (qIdx == lowerQuery.length) return 50

        return 0
    }

    override fun execute(context: Context, result: SearchResult): Boolean {
        if (result.packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(result.packageName)
            if (intent != null) {
                context.startActivity(intent)

                // Update weights
                if (PowerlineDefaults.ENABLE_WEIGHT_SEARCH) {
                    val weights = userDataRepository.getAppWeights().toMutableMap()

                    // Decay all existing weights and add newly executed app if missing
                    val allApps = weights.keys.toMutableSet()
                    allApps.add(result.packageName)

                    for (pkg in allApps) {
                        val current = weights[pkg] ?: 0
                        var next = (current * 0.95).toInt()

                        if (pkg == result.packageName) {
                            next += 5
                        }

                        // Optional: Clean up 0 weights to keep map small?
                        // Requirement doesn't say, but keeping them allows them to decay properly if they were high.
                        // However, 0 * 0.95 is 0.
                        if (next > 0) {
                            weights[pkg] = next
                        } else {
                            weights.remove(pkg)
                        }
                    }

                    userDataRepository.setAppWeights(weights)
                }

                return true
            } else {
                Toast.makeText(context, "Cannot launch app", Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }
}
