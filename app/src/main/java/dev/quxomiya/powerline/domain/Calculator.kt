package dev.quxomiya.powerline.domain

import java.text.DecimalFormat

object Calculator {

    fun evaluate(expression: String): String? {
        // Basic validation: only numbers and operators allowed
        val sanitized = expression.replace(" ", "")
        if (sanitized.isEmpty()) return null
        // Must contain at least one operator to be considered a calculation for our launcher
        if (!sanitized.any { it in setOf('+', '-', '*', '/') }) return null
        if (!sanitized.all { it.isDigit() || it in setOf('.', '+', '-', '*', '/', '(', ')') }) return null

        return try {
            val result = object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < sanitized.length) sanitized[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < sanitized.length) throw RuntimeException("Unexpected: " + ch.toChar())
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        if      (eat('+'.code)) x += parseTerm() // addition
                        else if (eat('-'.code)) x -= parseTerm() // subtraction
                        else return x
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        if      (eat('*'.code)) x *= parseFactor() // multiplication
                        else if (eat('/'.code)) x /= parseFactor() // division
                        else return x
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor() // unary plus
                    if (eat('-'.code)) return -parseFactor() // unary minus

                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) { // parentheses
                        x = parseExpression()
                        eat(')'.code)
                    } else if (ch in '0'.code..'9'.code || ch == '.'.code) { // numbers
                        while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                        x = sanitized.substring(startPos, pos).toDouble()
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }
                    return x
                }
            }.parse()

            // Format result (remove .0 if integer)
            if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                result.toString()
            }
        } catch (e: Exception) {
            null
        }
    }
}
