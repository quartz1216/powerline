package dev.quxomiya.powerline.search.providers

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import dev.quxomiya.powerline.domain.Calculator
import dev.quxomiya.powerline.search.ResultSection
import dev.quxomiya.powerline.search.SearchAction
import dev.quxomiya.powerline.search.SearchItem
import dev.quxomiya.powerline.search.SearchMode
import dev.quxomiya.powerline.search.SearchProvider
import dev.quxomiya.powerline.search.SearchRequest

// Arithmetic on the query; running the item copies the result. Off with the Built-in switch.
class CalculatorProvider(private val enabled: () -> Boolean = { true }) : SearchProvider {
    override val id = "calculator"
    override val priority = 30
    private val icon = Icons.Default.Calculate
    override val section = ResultSection.CALCULATOR

    override suspend fun search(request: SearchRequest, context: Context): List<SearchItem> {
        if (request.mode != SearchMode.TEXT || !enabled()) return emptyList()
        // Incomplete or invalid input ("12+", "1/0") shows no result rather than an error
        val result = Calculator.evaluate(request.text) ?: return emptyList()
        return listOf(
            SearchItem(
                id = "calc:${request.text}",
                providerId = id,
                title = result,
                subtitle = "${request.text} · Copy result",
                icon = null,
                section = section,
                score = 0,
                action = SearchAction.CopyText(result),
                vectorIcon = icon
            )
        )
    }
}
