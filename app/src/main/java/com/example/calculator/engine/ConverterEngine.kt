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
        displayName = "Temperature",
        icon = "🌡️",
        defaultFrom = "°C",
        defaultTo = "°F",
        units = listOf("°C", "°F", "K")
    ),
    DATA(
        displayName = "Data",
        icon = "💾",
        defaultFrom = "MB",
        defaultTo = "GB",
        units = listOf("B", "KB", "MB", "GB", "TB")
    ),
    AREA(
        displayName = "Area",
        icon = "📐",
        defaultFrom = "m²",
        defaultTo = "hectare",
        units = listOf("m²", "km²", "cm²", "sq in", "sq ft", "acre", "hectare")
    ),
    VOLUME(
        displayName = "Volume",
        icon = "🧪",
        defaultFrom = "L",
        defaultTo = "mL",
        units = listOf("mL", "L", "cup", "pt", "qt", "gal")
    ),
    SPEED(
        displayName = "Speed",
        icon = "🚀",
        defaultFrom = "km/h",
        defaultTo = "m/s",
        units = listOf("m/s", "km/h", "mph", "knot")
    ),
    TIME(
        displayName = "Time",
        icon = "⏱️",
        defaultFrom = "min",
        defaultTo = "h",
        units = listOf("s", "min", "h", "day", "week", "year")
    ),
    ANGLE(
        displayName = "Angle",
        icon = "🧭",
        defaultFrom = "deg",
        defaultTo = "rad",
        units = listOf("deg", "rad", "grad")
    ),
    PRESSURE(
        displayName = "Pressure",
        icon = "💨",
        defaultFrom = "kPa",
        defaultTo = "bar",
        units = listOf("Pa", "kPa", "bar", "psi", "atm")
    ),
    CURRENCY(
        displayName = "Currency",
        icon = "🌐",
        defaultFrom = "USD",
        defaultTo = "INR",
        units = listOf("USD", "EUR", "GBP", "INR", "JPY", "CAD", "AUD", "BDT")
    ),
    GST(
        displayName = "GST",
        icon = "📊",
        defaultFrom = "%",
        defaultTo = "%",
        units = listOf("%")
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

    val dataUnits = mapOf(
        "B" to 1.0,
        "KB" to 1024.0,
        "MB" to 1024.0 * 1024.0,
        "GB" to 1024.0 * 1024.0 * 1024.0,
        "TB" to 1024.0 * 1024.0 * 1024.0 * 1024.0
    )

    val areaUnits = mapOf(
        "m²" to 1.0,
        "km²" to 1000000.0,
        "cm²" to 0.0001,
        "sq in" to 0.00064516,
        "sq ft" to 0.09290304,
        "acre" to 4046.8564224,
        "hectare" to 10000.0
    )

    val speedUnits = mapOf(
        "m/s" to 1.0,
        "km/h" to 0.2777777777777778,
        "mph" to 0.44704,
        "knot" to 0.5144444444444445
    )

    val timeUnits = mapOf(
        "s" to 1.0,
        "min" to 60.0,
        "h" to 3600.0,
        "day" to 86400.0,
        "week" to 604800.0,
        "year" to 31536000.0
    )

    val angleUnits = mapOf(
        "deg" to 1.0,
        "rad" to 57.29577951308232,
        "grad" to 0.9
    )

    val pressureUnits = mapOf(
        "Pa" to 1.0,
        "kPa" to 1000.0,
        "bar" to 100000.0,
        "psi" to 6894.757293168,
        "atm" to 101325.0
    )

    val currencyUnits = mapOf(
        "USD" to 1.0,
        "EUR" to 1.09,
        "GBP" to 1.27,
        "INR" to 0.012,
        "JPY" to 0.0064,
        "CAD" to 0.73,
        "AUD" to 0.66,
        "BDT" to 0.0085
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
            ConverterCategory.DATA -> {
                val fromRatio = dataUnits[from] ?: return Double.NaN
                val toRatio = dataUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.AREA -> {
                val fromRatio = areaUnits[from] ?: return Double.NaN
                val toRatio = areaUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.SPEED -> {
                val fromRatio = speedUnits[from] ?: return Double.NaN
                val toRatio = speedUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.TIME -> {
                val fromRatio = timeUnits[from] ?: return Double.NaN
                val toRatio = timeUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.ANGLE -> {
                val fromRatio = angleUnits[from] ?: return Double.NaN
                val toRatio = angleUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.PRESSURE -> {
                val fromRatio = pressureUnits[from] ?: return Double.NaN
                val toRatio = pressureUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.CURRENCY -> {
                val fromRatio = currencyUnits[from] ?: return Double.NaN
                val toRatio = currencyUnits[to] ?: return Double.NaN
                value * (fromRatio / toRatio)
            }
            ConverterCategory.GST -> value
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
