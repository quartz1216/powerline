package dev.quxomiya.powerline.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StandbyGridTest {

    // Recent row: 0..2, all apps: 3..7 | 8..12 | 13
    private val grid = StandbyGrid(recentCount = 3, appCount = 11, columns = 5)

    @Test
    fun downFromRecentKeepsColumn() {
        assertEquals(4, grid.moveVertical(1, 1))
    }

    @Test
    fun downClampsToShortLastRow() {
        assertEquals(13, grid.moveVertical(12, 1))
    }

    @Test
    fun upFromAppsBeyondRecentWidthClampsToLastRecent() {
        assertEquals(2, grid.moveVertical(7, -1))
    }

    @Test
    fun verticalMoveStopsAtEdges() {
        assertEquals(1, grid.moveVertical(1, -1))
        assertEquals(13, grid.moveVertical(13, 1))
    }

    @Test
    fun horizontalMoveIsClamped() {
        assertEquals(0, grid.moveHorizontal(0, -1))
        assertEquals(3, grid.moveHorizontal(2, 1))
        assertEquals(13, grid.moveHorizontal(13, 1))
    }

    @Test
    fun gridItemIndexSkipsDivider() {
        assertEquals(0, grid.gridItemIndex(0))
        assertEquals(4, grid.gridItemIndex(3))
        assertEquals(0, StandbyGrid(0, 4, 5).gridItemIndex(0))
    }

    @Test
    fun tabWalksRecentThenAppsThenChips() {
        // null = the chips row
        assertEquals(0, grid.nextSection(null))
        assertEquals(3, grid.nextSection(1))
        assertEquals(null, grid.nextSection(9))
    }

    @Test
    fun shiftTabWalksBackwards() {
        assertEquals(3, grid.previousSection(null))
        assertEquals(0, grid.previousSection(9))
        assertEquals(null, grid.previousSection(2))
    }

    @Test
    fun tabWithoutRecentRow() {
        val noRecent = StandbyGrid(recentCount = 0, appCount = 4, columns = 5)
        assertEquals(0, noRecent.nextSection(null))
        assertEquals(null, noRecent.nextSection(2))
        assertEquals(0, noRecent.previousSection(null))
    }
}
