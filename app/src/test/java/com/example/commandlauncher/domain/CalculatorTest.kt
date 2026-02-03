package com.example.commandlauncher.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {

    @Test
    fun testSimpleAddition() {
        assertEquals("2", Calculator.evaluate("1+1"))
    }

    @Test
    fun testMixedOperations() {
        // 10 + 2 * 5 = 20
        assertEquals("20", Calculator.evaluate("10+2*5"))
    }

    @Test
    fun testParentheses() {
        // (10 + 2) * 5 = 60
        assertEquals("60", Calculator.evaluate("(10+2)*5"))
    }

    @Test
    fun testDecimal() {
        assertEquals("2.5", Calculator.evaluate("5/2"))
    }

    @Test
    fun testInvalid() {
        assertNull(Calculator.evaluate("abc"))
        assertNull(Calculator.evaluate("10+")) // Trailing operator might crash or return null depending on impl.
        // My impl throws or returns null on exception.
    }

    @Test
    fun testNoOperator() {
        assertNull(Calculator.evaluate("123"))
    }
}
