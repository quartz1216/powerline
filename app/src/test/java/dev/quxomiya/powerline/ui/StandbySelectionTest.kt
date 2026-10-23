package dev.quxomiya.powerline.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StandbySelectionTest {

    // Recent row: 0..2, all apps: 3..7 | 8..12 | 13
    private val grid = StandbyGrid(recentCount = 3, appCount = 11, columns = 5)
    private val chips = 4

    @Test
    fun upFromTopRowReachesChipsAndDownLeavesThem() {
        val onChip = StandbySelection(tile = 1).up(grid, chips)
        assertEquals(StandbySelection(tile = 1, chip = 0), onChip)
        assertEquals(StandbySelection(tile = 1), onChip.down(grid))
    }

    @Test
    fun upFromTopRowStaysWithoutChips() {
        assertEquals(StandbySelection(tile = 1), StandbySelection(tile = 1).up(grid, chipCount = 0))
    }

    @Test
    fun leftAndRightMoveAlongChipsAndStopAtTheEnds() {
        val first = StandbySelection(chip = 0)
        assertEquals(first, first.left(grid))
        assertEquals(StandbySelection(chip = 1), first.right(grid, chips))
        assertEquals(StandbySelection(chip = 3), StandbySelection(chip = 3).right(grid, chips))
    }

    @Test
    fun arrowsMoveOverTiles() {
        assertEquals(StandbySelection(tile = 4), StandbySelection(tile = 1).down(grid))
        assertEquals(StandbySelection(tile = 2), StandbySelection(tile = 1).right(grid, chips))
    }

    // chips → recent → all apps → chips
    @Test
    fun tabCyclesSections() {
        val recent = StandbySelection(chip = 0).tab(grid, chips, backward = false)
        assertEquals(StandbySelection(tile = 0), recent)
        val all = recent.tab(grid, chips, backward = false)
        assertEquals(StandbySelection(tile = 3), all)
        assertEquals(StandbySelection(tile = 3, chip = 0), all.tab(grid, chips, backward = false))
    }

    @Test
    fun shiftTabFromRecentReachesChips() {
        assertEquals(StandbySelection(tile = 1, chip = 0), StandbySelection(tile = 1).tab(grid, chips, backward = true))
    }

    @Test
    fun tabWithoutChipsWraps() {
        assertEquals(StandbySelection(tile = 0), StandbySelection(tile = 5).tab(grid, chipCount = 0, backward = false))
    }

    @Test
    fun tileIsClampedToAShrunkGrid() {
        assertEquals(13, StandbySelection(tile = 40).tileIn(grid))
        assertEquals(0, StandbySelection(tile = 3).tileIn(StandbyGrid(0, 0, 5)))
    }
}
