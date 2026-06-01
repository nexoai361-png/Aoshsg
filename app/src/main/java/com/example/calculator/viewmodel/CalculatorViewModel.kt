package com.example.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.engine.CalculatorEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CalculatorViewModel : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression = _expression.asStateFlow()

    private val _previewResult = MutableStateFlow("")
    val previewResult = _previewResult.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError = _isError.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history = _history.asStateFlow()

    private val _isAdvancedMode = MutableStateFlow(false)
    val isAdvancedMode = _isAdvancedMode.asStateFlow()

    fun toggleAdvancedMode() {
        _isAdvancedMode.value = !_isAdvancedMode.value
    }

    fun onKeyPress(key: String) {
        _isError.value = false
        when (key) {
            "C" -> {
                _expression.value = ""
                _previewResult.value = ""
            }
            "⌫" -> {
                val current = _expression.value
                if (current.isNotEmpty()) {
                    val functionsWithParens = listOf("sin(", "cos(", "tan(", "log(", "ln(")
                    var deleted = false
                    for (func in functionsWithParens) {
                        if (current.endsWith(func)) {
                            _expression.value = current.substring(0, current.length - func.length)
                            deleted = true
                            break
                        }
                    }
                    if (!deleted) {
                        _expression.value = current.dropLast(1)
                    }
                    updateLivePreview()
                }
            }
            "±" -> {
                _expression.value = toggleLastNumberSign(_expression.value)
                updateLivePreview()
            }
            "=" -> {
                evaluateFinal()
            }
            else -> {
                _expression.value += key
                updateLivePreview()
            }
        }
    }

    fun selectHistoryItem(item: HistoryItem) {
        _expression.value = item.expression
        _previewResult.value = ""
        _isError.value = false
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    private fun updateLivePreview() {
        val currentExpression = _expression.value
        if (currentExpression.isBlank()) {
            _previewResult.value = ""
            return
        }

        // Don't show preview for single number
        val mappedExpr = currentExpression.replace("(-", "-").replace(")", "")
        if (mappedExpr.toDoubleOrNull() != null) {
            _previewResult.value = ""
            return
        }

        viewModelScope.launch {
            try {
                // Validate expression structure before run
                val balance = currentExpression.count { it == '(' } - currentExpression.count { it == ')' }
                val exprToEval = if (balance > 0) {
                    currentExpression + ")".repeat(balance)
                } else {
                    currentExpression
                }
                
                val res = CalculatorEngine.evaluate(exprToEval)
                if (!res.isNaN() && !res.isInfinite()) {
                    _previewResult.value = CalculatorEngine.formatResult(res)
                } else {
                    _previewResult.value = ""
                }
            } catch (e: Exception) {
                _previewResult.value = ""
            }
        }
    }

    private fun evaluateFinal() {
        var currentExpression = _expression.value
        if (currentExpression.isBlank()) return

        try {
            // Automatically close paren balances on equal press for convenience
            val balance = currentExpression.count { it == '(' } - currentExpression.count { it == ')' }
            if (balance > 0) {
                currentExpression += ")".repeat(balance)
            }
            
            val res = CalculatorEngine.evaluate(currentExpression)
            val formatted = CalculatorEngine.formatResult(res)
            
            val newItem = HistoryItem(
                expression = currentExpression,
                result = formatted
            )
            _history.value = listOf(newItem) + _history.value

            _expression.value = formatted
            _previewResult.value = ""
            _isError.value = false
        } catch (e: Exception) {
            _isError.value = true
        }
    }

    private fun toggleLastNumberSign(expr: String): String {
        if (expr.isEmpty()) return "-"
        
        // Match standard negative parenthesis number pattern at the end: e.g. (-25.3)
        val matchMinusParen = "\\(-\\d+\\.?\\d*\\)$".toRegex()
        val matchParen = matchMinusParen.find(expr)
        if (matchParen != null) {
            val numberOnly = expr.substring(matchParen.range.first + 2, expr.length - 1)
            return expr.substring(0, matchParen.range.first) + numberOnly
        }

        // Match standalone positive number at the end: e.g. 25
        val matchPositiveNumber = "\\d+\\.?\\d*$".toRegex()
        val matchPos = matchPositiveNumber.find(expr)
        if (matchPos != null) {
            val prefix = expr.substring(0, matchPos.range.first)
            val num = matchPos.value
            return "$prefix(-$num)"
        }

        if (expr.startsWith("-") && expr.substring(1).toDoubleOrNull() != null) {
            return expr.substring(1)
        }

        return if (expr.endsWith("+") || expr.endsWith("-") || expr.endsWith("×") || expr.endsWith("÷") || expr.endsWith("^") || expr.endsWith("(")) {
            "$expr-"
        } else {
            "$expr-"
        }
    }
}
