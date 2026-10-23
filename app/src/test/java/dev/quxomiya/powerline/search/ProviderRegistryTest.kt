package dev.quxomiya.powerline.search

import android.content.Context
import dev.quxomiya.powerline.data.PowerlineDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderRegistryTest {

    private val claude = ShareTarget("claude", "Claude", "cl", "com.anthropic.claude", "com.anthropic.claude.Share")
    private val parser = QueryParser(PowerlineDefaults.SEARCH_ENGINES, listOf(claude))
    private val providers = ResultSection.entries.map(::SectionProvider)

    private fun sections(query: String, disabled: Set<BuiltIn> = emptySet()) =
        ProviderRegistry(providers) { it !in disabled }.providersFor(parser.parse(query)).map { it.section }

    @Test
    fun plainTextRunsEverySection() {
        assertEquals(ResultSection.entries, sections("hello"))
    }

    @Test
    fun aliasRunsOnlyItsSection() {
        assertEquals(listOf(ResultSection.WEB), sections("yt lofi"))
        assertEquals(listOf(ResultSection.SHARE), sections("cl hello"))
    }

    // A switched-off built-in's provider does not run, alias or not
    @Test
    fun switchedOffBuiltInsDoNotRun() {
        assertEquals(
            listOf(ResultSection.CALCULATOR, ResultSection.SHARE),
            sections("hello", disabled = setOf(BuiltIn.APPS, BuiltIn.WEB))
        )
        assertEquals(emptyList<ResultSection>(), sections("yt lofi", disabled = setOf(BuiltIn.WEB)))
        assertEquals(emptyList<ResultSection>(), sections("cl hello", disabled = setOf(BuiltIn.SHARE)))
    }

    private class SectionProvider(override val section: ResultSection) : SearchProvider {
        override val id = section.name
        override val priority = 0
        override suspend fun search(request: SearchRequest, context: Context) = emptyList<SearchItem>()
    }
}
