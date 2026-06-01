package com.example.calculator.engine

import kotlin.math.*

object CalculatorEngine {

    fun evaluate(expression: String): Double {
        if (expression.isBlank()) return 0.0
        
        // Remove spaces and map display symbols to parseable ones
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("π", "pi")
            .replace("e", "e")
        
        return Parser(sanitized).parse()
    }

    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
        
        // Round to avoid IEEE 754 precision issues (e.g., 0.1 + 0.2)
        val epsilon = 1e-11
        val nearestLong = Math.round(value)
        if (Math.abs(value - nearestLong) < epsilon) {
            return nearestLong.toString()
        }
        
        val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
        val df = java.text.DecimalFormat("#.##########", symbols)
        df.roundingMode = java.math.RoundingMode.HALF_UP
        return df.format(value)
    }

    private class Parser(val str: String) {
        var pos = -1
        var ch = (-1).toChar()

        fun nextChar() {
            ch = if (++pos < str.length) str[pos] else (-1).toChar()
        }

        fun eat(charToEat: Char): Boolean {
            while (ch == ' ') nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) {
                val remaining = str.substring(pos).trim()
                if (remaining.isNotEmpty()) {
                    throw IllegalArgumentException("Syntax error: '$remaining'")
                }
            }
            return x
        }

        fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+')) x += parseTerm()
                else if (eat('-')) x -= parseTerm()
                else break
            }
            return x
        }

        fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*')) {
                    x *= parseFactor()
                } else if (eat('/')) {
                    val y = parseFactor()
                    if (y == 0.0) throw ArithmeticException("Division by zero")
                    x /= y
                } else {
                    break
                }
            }
            return x
        }

        fun parseFactor(): Double {
            if (eat('+')) return parseFactor() // Unary plus
            if (eat('-')) return -parseFactor() // Unary minus

            var x: Double
            val startPos = pos

            if (eat('(')) { // Parentheses
                x = parseExpression()
                if (!eat(')')) throw IllegalArgumentException("Missing closed parenthesis")
            } else if (ch == '√') { // Unary square root prefix
                nextChar()
                x = parseFactor()
                if (x < 0.0) throw ArithmeticException("Square root of negative")
                x = sqrt(x)
            } else if (ch in '0'..'9' || ch == '.') { // Numeric values
                while (ch in '0'..'9' || ch == '.') nextChar()
                val numStr = str.substring(startPos, pos)
                x = numStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid number: $numStr")
            } else if (ch in 'a'..'z' || ch in 'A'..'Z') { // Scientific functions and constants
                while (ch in 'a'..'z' || ch in 'A'..'Z') nextChar()
                val item = str.substring(startPos, pos).lowercase()
                
                if (item == "pi") {
                    x = Math.PI
                } else if (item == "e") {
                    x = Math.E
                } else if (eat('(')) {
                    val arg = parseExpression()
                    if (!eat(')')) throw IllegalArgumentException("Missing closed parenthesis for $item")
                    x = when (item) {
                        "sin" -> sin(Math.toRadians(arg))
                        "cos" -> cos(Math.toRadians(arg))
                        "tan" -> tan(Math.toRadians(arg))
                        "sqrt" -> {
                            if (arg < 0.0) throw ArithmeticException("Square root of negative")
                            sqrt(arg)
                        }
                        "ln" -> {
                            if (arg <= 0.0) throw ArithmeticException("Natural log of non-positive")
                            ln(arg)
                        }
                        "log" -> {
                            if (arg <= 0.0) throw ArithmeticException("Log of non-positive")
                            log10(arg)
                        }
                        else -> throw IllegalArgumentException("Unknown function: $item")
                    }
                } else {
                    throw IllegalArgumentException("Unknown keyword: $item")
                }
            } else {
                throw IllegalArgumentException("Unexpected symbol: " + if (ch == (-1).toChar()) "end" else "'$ch'")
            }

            if (eat('^')) {
                x = x.pow(parseFactor())
            }

            while (eat('%')) {
                x /= 100.0
            }

            return x
        }
    }
}
