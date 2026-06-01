package com.example.calculator.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.engine.CalculatorEngine
import com.example.calculator.engine.ConverterCategory
import com.example.calculator.engine.ConverterEngine
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

enum class AppMode {
    CALCULATOR, CONVERTER
}

class CalculatorViewModel : ViewModel() {

    // App Mode Controller
    private val _appMode = MutableStateFlow(AppMode.CALCULATOR)
    val appMode = _appMode.asStateFlow()

    // --- Calculator State ---
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

    // --- Converter State ---
    private val _converterCategory = MutableStateFlow(ConverterCategory.LENGTH)
    val converterCategory = _converterCategory.asStateFlow()

    private val _converterInputValue = MutableStateFlow("1")
    val converterInputValue = _converterInputValue.asStateFlow()

    private val _converterFromUnit = MutableStateFlow("m")
    val converterFromUnit = _converterFromUnit.asStateFlow()

    private val _converterToUnit = MutableStateFlow("km")
    val converterToUnit = _converterToUnit.asStateFlow()

    private val _converterResult = MutableStateFlow("0.001")
    val converterResult = _converterResult.asStateFlow()

    fun setAppMode(mode: AppMode) {
        _appMode.value = mode
    }

    fun toggleAdvancedMode() {
        _isAdvancedMode.value = !_isAdvancedMode.value
    }

    fun setConverterCategory(category: ConverterCategory) {
        _converterCategory.value = category
        _converterFromUnit.value = category.defaultFrom
        _converterToUnit.value = category.defaultTo
        recalculateConversion()
    }

    fun setConverterFromUnit(unit: String) {
        _converterFromUnit.value = unit
        recalculateConversion()
    }

    fun setConverterToUnit(unit: String) {
        _converterToUnit.value = unit
        recalculateConversion()
    }

    fun setConverterInputValue(value: String) {
        _converterInputValue.value = value
        recalculateConversion()
    }

    fun swapConverterUnits() {
        val oldFrom = _converterFromUnit.value
        _converterFromUnit.value = _converterToUnit.value
        _converterToUnit.value = oldFrom
        recalculateConversion()
    }

    private fun recalculateConversion() {
        val inputStr = _converterInputValue.value
        if (inputStr.isBlank() || inputStr == "-") {
            _converterResult.value = ""
            return
        }
        val parsed = inputStr.toDoubleOrNull()
        if (parsed == null) {
            _converterResult.value = "Error"
            return
        }
        val computed = ConverterEngine.convert(
            value = parsed,
            from = _converterFromUnit.value,
            to = _converterToUnit.value,
            category = _converterCategory.value
        )
        _converterResult.value = ConverterEngine.formatResult(computed)
    }

    fun onKeyPress(key: String) {
        if (_appMode.value == AppMode.CONVERTER) {
            handleConverterKeyPress(key)
            return
        }

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
                // Ignore keys that don't belong to basic math on screen in non-advanced mode to keep screen safe
                _expression.value += key
                updateLivePreview()
            }
        }
    }

    private fun handleConverterKeyPress(key: String) {
        val current = _converterInputValue.value
        when (key) {
            "C" -> {
                _converterInputValue.value = ""
                recalculateConversion()
            }
            "⌫" -> {
                if (current.isNotEmpty()) {
                    _converterInputValue.value = current.dropLast(1)
                    recalculateConversion()
                }
            }
            "±" -> {
                if (current.startsWith("-")) {
                    _converterInputValue.value = current.substring(1)
                } else if (current.isNotEmpty() && current != "0") {
                    _converterInputValue.value = "-$current"
                } else {
                    _converterInputValue.value = "-"
                }
                recalculateConversion()
            }
            "." -> {
                if (!current.contains(".")) {
                    _converterInputValue.value = if (current.isEmpty() || current == "-") {
                        "${current}0."
                    } else {
                        "$current."
                    }
                    recalculateConversion()
                }
            }
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                if (current == "0") {
                    _converterInputValue.value = key
                } else {
                    _converterInputValue.value = current + key
                }
                recalculateConversion()
            }
            // Ignore math operators in converter mode unless they are useful
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

