package com.davoyans.idea3d.core

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Model space is right-handed and measured in millimeters: X to the right, Y up,
 * Z toward the viewer of the front view. The front drawing plane is Z = 0.
 *
 * Image space (pixels, Y down) is converted to model space only through a
 * [Calibration]; corrected-image pixels never leak into exported geometry.
 */
data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length get() = sqrt(this dot this)
    fun normalized(): Vec3 = length.let { if (it < 1e-15) this else Vec3(x / it, y / it, z / it) }

    companion object { val ZERO = Vec3(0.0, 0.0, 0.0) }
}

data class Vec2(val x: Double, val y: Double) {
    operator fun minus(o: Vec2) = Vec2(x - o.x, y - o.y)
    infix fun cross(o: Vec2) = x * o.y - y * o.x
}

enum class Axis(val label: String, val direction: String) {
    X("X", "right"), Y("Y", "up"), Z("Z", "toward viewer");
}

/** A segment's real length, its front (XY) projection and their relationship. */
data class SegmentLength(val trueMm: Double, val frontProjectionMm: Double, val deltaZMm: Double) {
    /** The front view shows a shorter line than the real one when the ends differ in Z. */
    val projectionIsShorter get() = abs(deltaZMm) > 1e-6 && trueMm - frontProjectionMm > 1e-6
}

fun segmentLength(a: Vec3, b: Vec3): SegmentLength {
    val projected = kotlin.math.hypot(b.x - a.x, b.y - a.y)
    val dz = b.z - a.z
    return SegmentLength(sqrt(projected * projected + dz * dz), projected, dz)
}

/**
 * Projected length needed to keep a fixed true length after the Z difference changes.
 * Returns null when the requested Z difference is longer than the fixed length, which
 * is a constraint conflict the user has to resolve.
 */
fun projectedLengthForFixedTrue(trueMm: Double, deltaZMm: Double): Double? {
    val remaining = trueMm * trueMm - deltaZMm * deltaZMm
    return if (remaining < -1e-9) null else sqrt(remaining.coerceAtLeast(0.0))
}
