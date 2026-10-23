package dev.quxomiya.powerline.ui

// Keyboard selection in the standby state: an alias chip (`chip`), or else a tile of the grid
// (`tile`, flat index into StandbyGrid). Each key returns the next selection.
data class StandbySelection(
    val tile: Int = 0,
    val chip: Int? = null
) {
    // The tile index within the grid as it is now (apps can be removed while the overlay is open)
    fun tileIn(grid: StandbyGrid): Int = tile.coerceIn(0, maxOf(grid.size - 1, 0))

    fun down(grid: StandbyGrid): StandbySelection =
        if (chip != null) copy(chip = null) else copy(tile = grid.moveVertical(tileIn(grid), 1))

    // Up from the top row reaches the chips
    fun up(grid: StandbyGrid, chipCount: Int): StandbySelection {
        if (chip != null) return this
        val from = tileIn(grid)
        val moved = grid.moveVertical(from, -1)
        return if (moved == from && chipCount > 0) copy(chip = 0) else copy(tile = moved)
    }

    fun left(grid: StandbyGrid): StandbySelection =
        if (chip != null) copy(chip = maxOf(chip - 1, 0)) else copy(tile = grid.moveHorizontal(tileIn(grid), -1))

    fun right(grid: StandbyGrid, chipCount: Int): StandbySelection =
        if (chip != null) copy(chip = minOf(chip + 1, chipCount - 1))
        else copy(tile = grid.moveHorizontal(tileIn(grid), 1))

    // Tab / Shift+Tab over the sections in order: chips, recent apps, all apps (wrapping)
    fun tab(grid: StandbyGrid, chipCount: Int, backward: Boolean): StandbySelection {
        val from = if (chip != null) null else tileIn(grid)
        val target = if (backward) grid.previousSection(from) else grid.nextSection(from)
        return when {
            target != null -> StandbySelection(tile = target)
            from != null && chipCount > 0 -> copy(chip = 0)
            else -> (if (backward) grid.previousSection(null) else grid.nextSection(null))
                ?.let { copy(tile = it) } ?: this
        }
    }
}
