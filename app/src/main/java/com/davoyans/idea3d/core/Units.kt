package com.davoyans.idea3d.core

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Physical lengths are always stored in millimeters. A unit only changes how a
 * length is shown or typed, never the stored geometry.
 */
enum class LengthUnit(val symbol: String, val mmPerUnit: Double) {
    MM("mm", 1.0), CM("cm", 10.0), M("m", 1000.0), IN("in", 25.4), FT("ft", 304.8);

    fun toMm(value: Double) = value * mmPerUnit
    fun fromMm(mm: Double) = mm / mmPerUnit

    companion object {
        val DEFAULT = MM
        fun of(symbol: String): LengthUnit = entries.firstOrNull { it.symbol == symbol } ?: DEFAULT
    }
}

/** Formats [mm] in [unit] with at most [decimals] (0-2) digits after the point, trimming trailing zeros. */
fun formatLength(mm: Double, unit: LengthUnit, decimals: Int = 2, withUnit: Boolean = true): String {
    val places = decimals.coerceIn(0, 2)
    val value = unit.fromMm(mm)
    if (!value.isFinite()) return "?"
    val text = BigDecimal(value).setScale(places, RoundingMode.HALF_UP).stripTrailingZeros().let {
        if (it.signum() == 0) "0" else it.toPlainString()
    }
    return if (withUnit) "$text ${unit.symbol}" else text
}

/** Parses a typed length in [unit] into millimeters, or null when the text is not a finite number. */
fun parseLengthMm(text: String, unit: LengthUnit): Double? =
    text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }?.let(unit::toMm)

/** Spreadsheet-style vertex names: 1 -> A, 26 -> Z, 27 -> AA, 28 -> AB. */
fun pointName(index: Int): String {
    var value = index.coerceAtLeast(1)
    val name = StringBuilder()
    while (value > 0) {
        value--
        name.insert(0, ('A'.code + value % 26).toChar())
        value /= 26
    }
    return name.toString()
}
