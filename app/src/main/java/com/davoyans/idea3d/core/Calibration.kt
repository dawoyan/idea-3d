package com.davoyans.idea3d.core

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Which model axis a calibration reference measures. [UNIFORM] (the default) sets one
 * scale for X, Y and Z. [X] and [Y] are the advanced per-axis workflow on the front
 * plane; the axis is always chosen by the user, never guessed from the line angle.
 */
enum class CalibrationAxis(val key: String, val label: String) {
    UNIFORM("", "All axes"), X("x", "X only"), Y("y", "Y only");

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } ?: UNIFORM }
}

/** A reference drawn on the corrected image, from pixel [dxPx], [dyPx] with a known real length. */
data class CalibrationReference(val dxPx: Double, val dyPx: Double, val knownMm: Double, val axis: CalibrationAxis = CalibrationAxis.UNIFORM) {
    val pixelLength get() = when (axis) {
        CalibrationAxis.UNIFORM -> hypot(dxPx, dyPx)
        CalibrationAxis.X -> abs(dxPx)
        CalibrationAxis.Y -> abs(dyPx)
    }
    /** Pixels per millimeter implied by this reference alone, or null when it is unusable. */
    val pxPerMm get() = (pixelLength / knownMm).takeIf { knownMm > 0 && it.isFinite() && it > 0 }
}

sealed class CalibrationStatus {
    data object Missing : CalibrationStatus()
    data object Uniform : CalibrationStatus()
    /** X and Y differ; used to compensate for a stretched or distorted image. [verticalStretch] is Y px/mm ÷ X px/mm. */
    data class PerAxis(val verticalStretch: Double) : CalibrationStatus()
    /** Only one axis is known in the per-axis workflow. */
    data class Incomplete(val missing: CalibrationAxis) : CalibrationStatus()
    /** References for the same axis disagree by more than the tolerance. */
    data class Contradictory(val axis: CalibrationAxis, val spread: Double) : CalibrationStatus()
}

/**
 * Pixel-to-model scale. [pxPerMmX] and [pxPerMmY] are equal for uniform calibration.
 * Z is never read from a single front photo: depth always comes from entered values,
 * so Z needs no image scale.
 */
data class Calibration(val pxPerMmX: Double?, val pxPerMmY: Double?, val status: CalibrationStatus) {
    val isUsable get() = pxPerMmX != null && pxPerMmY != null && status !is CalibrationStatus.Contradictory
    val isPerAxis get() = status is CalibrationStatus.PerAxis

    /** Converts an image-pixel delta to a front-plane millimeter delta (image Y is down, model Y is up). */
    fun toModelMm(dxPx: Double, dyPx: Double): Pair<Double, Double>? {
        if (!isUsable) return null
        return dxPx / pxPerMmX!! to -dyPx / pxPerMmY!!
    }

    /** Real front-plane length of a pixel segment. */
    fun lengthMm(dxPx: Double, dyPx: Double): Double? = toModelMm(dxPx, dyPx)?.let { hypot(it.first, it.second) }

    /** Single scale for code paths that need one number: the geometric mean of the axes. */
    val meanPxPerMm get() = if (pxPerMmX != null && pxPerMmY != null) sqrt(pxPerMmX * pxPerMmY) else null

    fun explanation(): String = when (val s = status) {
        CalibrationStatus.Missing -> "No calibration. Sizes stay unknown until you set a known length."
        CalibrationStatus.Uniform -> "One scale is used for X, Y and Z."
        is CalibrationStatus.PerAxis -> "X and Y use separate scales because the photo is stretched ${formatRatio(s.verticalStretch)}× vertically compared to horizontally. This compensates for a stretched or distorted photo. Depth (Z) still comes only from values you enter."
        is CalibrationStatus.Incomplete -> "Per-axis calibration needs a ${s.missing.label.substringBefore(' ')} reference too."
        is CalibrationStatus.Contradictory -> "${s.axis.label} references disagree by ${formatRatio(s.spread * 100)}%. Edit or delete one of them."
    }

    companion object {
        /** References on the same axis may differ by this much before they count as contradictory. */
        const val TOLERANCE = 0.03

        fun from(references: List<CalibrationReference>): Calibration {
            val usable = references.filter { it.pxPerMm != null }
            if (usable.isEmpty()) return Calibration(null, null, CalibrationStatus.Missing)
            fun group(axis: CalibrationAxis): Pair<Double, Double>? {
                val values = usable.filter { it.axis == axis }.mapNotNull { it.pxPerMm }
                if (values.isEmpty()) return null
                val mean = values.average()
                return mean to (values.max() - values.min()) / mean
            }
            val uniform = group(CalibrationAxis.UNIFORM)
            val x = group(CalibrationAxis.X)
            val y = group(CalibrationAxis.Y)
            listOf(CalibrationAxis.UNIFORM to uniform, CalibrationAxis.X to x, CalibrationAxis.Y to y).forEach { (axis, g) ->
                if (g != null && g.second > TOLERANCE) return Calibration(x?.first ?: uniform?.first, y?.first ?: uniform?.first, CalibrationStatus.Contradictory(axis, g.second))
            }
            if (x == null && y == null) return Calibration(uniform!!.first, uniform.first, CalibrationStatus.Uniform)
            val sx = x?.first ?: uniform?.first
            val sy = y?.first ?: uniform?.first
            if (sx == null) return Calibration(null, sy, CalibrationStatus.Incomplete(CalibrationAxis.X))
            if (sy == null) return Calibration(sx, null, CalibrationStatus.Incomplete(CalibrationAxis.Y))
            // A uniform reference that disagrees with explicit axes is a contradiction, not something to average away.
            if (uniform != null && (x != null && abs(uniform.first - x.first) / x.first > TOLERANCE || y != null && abs(uniform.first - y.first) / y.first > TOLERANCE))
                return Calibration(sx, sy, CalibrationStatus.Contradictory(CalibrationAxis.UNIFORM, abs(uniform.first - (x ?: y)!!.first) / (x ?: y)!!.first))
            val ratio = sy / sx
            return if (abs(ratio - 1.0) <= 1e-9) Calibration(sx, sy, CalibrationStatus.Uniform) else Calibration(sx, sy, CalibrationStatus.PerAxis(ratio))
        }

        private fun formatRatio(value: Double) = java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
