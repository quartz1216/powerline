package com.quartz.powerline.ui

// Keyboard navigation over the standby grid: one row of recent apps, then all apps in rows of `columns`.
// Indexes are flat: recent apps first, then all apps.
class StandbyGrid(
    val recentCount: Int,
    val appCount: Int,
    val columns: Int
) {
    val size: Int get() = recentCount + appCount

    private val rows: List<IntRange> = buildList {
        val total = recentCount + appCount
        if (recentCount > 0) add(0 until recentCount)
        var start = recentCount
        while (start < total) {
            add(start until minOf(start + columns, total))
            start += columns
        }
    }

    fun moveVertical(index: Int, delta: Int): Int {
        val row = rows.indexOfFirst { index in it }
        if (row < 0) return 0
        val target = rows.getOrNull(row + delta) ?: return index
        val column = index - rows[row].first
        return target.first + minOf(column, target.count() - 1)
    }

    fun moveHorizontal(index: Int, delta: Int): Int {
        if (size == 0) return 0
        return (index + delta).coerceIn(0, size - 1)
    }

    // Position in the LazyVerticalGrid, which also holds a divider after the recent row
    fun gridItemIndex(index: Int): Int {
        return if (recentCount > 0 && index >= recentCount) index + 1 else index
    }
}
