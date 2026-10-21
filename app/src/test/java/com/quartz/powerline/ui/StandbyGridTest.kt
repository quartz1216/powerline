package com.quartz.powerline.ui

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
}
