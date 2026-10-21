package dev.quxomiya.powerline.domain

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

    @Test
    fun testPercent() {
        assertEquals("0.5", Calculator.evaluate("50%"))
        assertEquals("20", Calculator.evaluate("200*10%"))
    }

    @Test
    fun testNegativeAndDecimal() {
        assertEquals("-3", Calculator.evaluate("-1-2"))
        assertEquals("0.3", Calculator.evaluate("0.1+0.2"))
        assertNull(Calculator.evaluate("-5")) // a lone negative number is not a calculation
    }

    @Test
    fun testUnicodeOperators() {
        assertEquals("6", Calculator.evaluate("2×3"))
        assertEquals("2", Calculator.evaluate("6÷3"))
    }

    @Test
    fun testErrors() {
        assertNull(Calculator.evaluate("1/0"))
        assertNull(Calculator.evaluate("(1+2"))
        assertNull(Calculator.evaluate("1..2+1"))
    }

    @Test
    fun testLargeResultIsPlain() {
        assertEquals("10000000000", Calculator.evaluate("100000*100000"))
    }
}
