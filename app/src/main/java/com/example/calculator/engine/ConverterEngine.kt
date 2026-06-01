package com.example.calculator.engine

enum class ConverterCategory(
    val displayName: String,
    val icon: String,
    val defaultFrom: String,
    val defaultTo: String,
    val units: List<String>
) {
    LENGTH(
        displayName = "Length",
        icon = "📏",
        defaultFrom = "m",
        defaultTo = "km",
        units = listOf("mm", "cm", "m", "km", "in", "ft", "yd", "mi")
    ),
    WEIGHT(
        displayName = "Weight",
        icon = "⚖️",
        defaultFrom = "kg",
        defaultTo = "g",
        units = listOf("mg", "g", "kg", "oz", "lb")
    ),
    TEMPERATURE(
        displayName = "Temp",
        icon = "🌡️",
        defaultFrom = "°C",
        defaultTo = "°F",
        units = listOf("°C", "°F", "K")
    ),
    VOLUME(
        displayName = "Volume",
        icon = "🧪",
        defaultFrom = "L",
        defaultTo = "mL",
        units = listOf("mL", "L", "cup", "pt", "qt", "gal")
    )
}

object ConverterEngine {
    val lengthUnits = mapOf(
        "mm" to 0.001,
        "cm" to 0.01,
        "m" to 1.0,
        "km" to 1000.0,
        "in" to 0.0254,
        "ft" to 0.3048,
        "yd" to 0.9144,
        "mi" to 1609.344
    )

    val weightUnits = mapOf(
        "mg" to 1e-6,
        "g" to 0.001,
        "kg" to 1.0,
        "oz" to 0.028349523125,
        "lb" to 0.45359237
    )

    val volumeUnits = mapOf(
        "mL" to 0.001,
        "L" to 1.0,
        "cup" to 0.2365882365,
        "pt" to 0.473176473,
        "qt" to 0.946352946,
        "gal" to 3.785411784
    )

    fun convert(value: Double, from: String, to: String, category: ConverterCategory): Double {
        if (from == to) return value
        if (value.isNaN() || value.isInfinite()) return Double.NaN

        return when (category) {
            ConverterCategory.LENGTH -> {
                val fromRatio = lengthUnits[from] ?: return Double.NaN
                val toRatio = lengthUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.WEIGHT -> {
                val fromRatio = weightUnits[from] ?: return Double.NaN
                val toRatio = weightUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.VOLUME -> {
                val fromRatio = volumeUnits[from] ?: return Double.NaN
                val toRatio = volumeUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.TEMPERATURE -> {
                when (from) {
                    "°C" -> when (to) {
                        "°F" -> value * 9.0 / 5.0 + 32.0
                        "K" -> value + 273.15
                        else -> Double.NaN
                    }
                    "°F" -> when (to) {
                        "°C" -> (value - 32.0) * 5.0 / 9.0
                        "K" -> (value - 32.0) * 5.0 / 9.0 + 273.15
                        else -> Double.NaN
                    }
                    "K" -> when (to) {
                        "°C" -> value - 273.15
                        "°F" -> (value - 273.15) * 9.0 / 5.0 + 32.0
                        else -> Double.NaN
                    }
                    else -> Double.NaN
                }
            }
        }
    }

    fun formatResult(value: Double): String {
        if (value.isNaN()) return "Error"
        if (value.isInfinite()) return if (value < 0) "-Infinity" else "Infinity"
        
        // Round extremely close values to avoid Floating point errors
        val epsilon = 1e-9
        val nearestLong = Math.round(value)
        if (Math.abs(value - nearestLong) < epsilon) {
            return nearestLong.toString()
        }

        val symbols = java.text.DecimalFormatSymbols(java.util.Locale.US)
        val df = java.text.DecimalFormat("#.######", symbols)
        df.roundingMode = java.math.RoundingMode.HALF_UP
        return df.format(value)
    }
}
