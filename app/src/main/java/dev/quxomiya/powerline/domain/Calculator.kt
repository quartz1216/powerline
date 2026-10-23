package dev.quxomiya.powerline.domain

import java.math.BigDecimal
import java.math.MathContext

// + - * / (also × ÷), parentheses, postfix percent (50% = 0.5), unary minus, decimals.
// Returns null unless the input is a complete expression with at least one operator,
// so plain numbers and half-typed input ("12+") produce no result.
object Calculator {

    private val allowed = setOf('+', '-', '*', '/', '×', '÷', '%', '(', ')', '.')

    // Enough digits to hide binary rounding noise (0.1 + 0.2 = 0.3)
    private val precision = MathContext(12)

    fun evaluate(expression: String): String? {
        val input = expression.filterNot { it.isWhitespace() }
        if (input.isEmpty()) return null
        if (!input.all { it in '0'..'9' || it in allowed }) return null

        val parser = Parser(input)
        val result = try {
            parser.parse()
        } catch (e: IllegalArgumentException) {
            return null
        }
        if (!parser.sawOperator || result.isNaN() || result.isInfinite()) return null
        return format(result)
    }

    private fun format(value: Double): String {
        if (value == 0.0) return "0" // also -0.0
        return BigDecimal(value).round(precision).stripTrailingZeros().toPlainString()
    }

    private class Parser(private val input: String) {
        private var pos = 0
        var sawOperator = false
            private set

        fun parse(): Double {
            val value = expression()
            require(pos == input.length) { "Unexpected '${input[pos]}'" }
            return value
        }

        private fun eat(vararg chars: Char): Boolean {
            if (pos < input.length && input[pos] in chars) {
                pos++
                return true
            }
            return false
        }

        private fun expression(): Double {
            var x = term()
            while (true) {
                x = when {
                    eat('+') -> x + term()
                    eat('-') -> x - term()
                    else -> return x
                }
                sawOperator = true
            }
        }

        private fun term(): Double {
            var x = factor()
            while (true) {
                x = when {
                    eat('*', '×') -> x * factor()
                    eat('/', '÷') -> x / factor()
                    else -> return x
                }
                sawOperator = true
            }
        }

        private fun factor(): Double {
            if (eat('+')) return factor()
            if (eat('-')) return -factor()

            var x = if (eat('(')) {
                val inner = expression()
                require(eat(')')) { "Missing ')'" }
                inner
            } else {
                number()
            }
            while (eat('%')) {
                x /= 100
                sawOperator = true
            }
            return x
        }

        private fun number(): Double {
            val start = pos
            while (pos < input.length && (input[pos] in '0'..'9' || input[pos] == '.')) pos++
            require(pos > start) { "Expected a number" }
            return input.substring(start, pos).toDoubleOrNull()
                ?: throw IllegalArgumentException("Bad number")
        }
    }
}
