package com.davoyans.idea3d.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Cross-section of a bar or pipe. Round profiles use [widthMm] as the outer diameter. */
enum class ProfileKind(val label: String, val hollow: Boolean, val round: Boolean) {
    RECT_TUBE("Rectangular tube", true, false),
    RECT_BAR("Rectangular bar", false, false),
    ROUND_TUBE("Round tube", true, true),
    ROUND_BAR("Round bar", false, true);

    companion object { fun of(name: String?) = entries.firstOrNull { it.name == name } ?: RECT_TUBE }
}

/** A profile's sizes in millimeters: width across the drawing, height along its depth axis, and wall thickness for tubes. */
data class MemberProfile(val kind: ProfileKind, val widthMm: Double, val heightMm: Double, val wallMm: Double = 0.0) {
    /** Problems that make the profile impossible to build, or null when it is fine. */
    fun problem(): String? = when {
        widthMm <= 0 || (!kind.round && heightMm <= 0) -> "Sizes must be positive"
        kind.hollow && wallMm <= 0 -> "Wall thickness must be positive"
        kind.hollow && (2 * wallMm >= widthMm || (!kind.round && 2 * wallMm >= heightMm)) -> "The wall is too thick for this size"
        else -> null
    }

    /** Area of the cross-section, square millimeters. */
    val areaMm2: Double get() {
        fun area(w: Double, h: Double) = if (kind.round) PI * w * w / 4 else w * h
        return area(widthMm, heightMm) - if (kind.hollow) area(widthMm - 2 * wallMm, heightMm - 2 * wallMm) else 0.0
    }
}

/**
 * Bars and pipes along a line: the profile is centered on the line, its height runs along [up] (the drawing's depth
 * axis) and its width across the line within the drawing.
 */
object MemberBuilder {
    private const val ROUND_SEGMENTS = 24

    fun build(name: String, a: Vec3, b: Vec3, up: Vec3, profile: MemberProfile, argb: Int): Mesh {
        profile.problem()?.let { throw MeshException(listOf(MeshIssue(name, it))) }
        val along = b - a
        if (along.length < 1e-6) throw MeshException(listOf(MeshIssue(name, "line has no length")))
        val u = along.normalized()
        // Height follows the drawing's depth axis unless the line itself points that way.
        var v = up - u * (up dot u)
        if (v.length < 1e-6) v = (if (kotlin.math.abs(u.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).let { it - u * (it dot u) }
        v = v.normalized()
        val w = u cross v
        val outer = prism(name, a, b, u, v, w, ring(profile.kind.round, profile.widthMm, profile.heightMm), argb)
        if (!profile.kind.hollow) return outer
        // The hole runs a little past both ends so the cut is clean.
        val extra = u * 1.0
        val inner = prism(name, a - extra, b + extra, u, v, w, ring(profile.kind.round, profile.widthMm - 2 * profile.wallMm, profile.heightMm - 2 * profile.wallMm), argb)
        return Csg.subtract(outer, inner)
    }

    /** Cross-section corners as (across, up) pairs. */
    private fun ring(round: Boolean, width: Double, height: Double): List<Pair<Double, Double>> =
        if (round) (0 until ROUND_SEGMENTS).map { i -> val t = 2 * PI * i / ROUND_SEGMENTS; width / 2 * cos(t) to width / 2 * sin(t) }
        else listOf(-width / 2 to -height / 2, width / 2 to -height / 2, width / 2 to height / 2, -width / 2 to height / 2)

    private fun prism(name: String, a: Vec3, b: Vec3, u: Vec3, v: Vec3, w: Vec3, ring: List<Pair<Double, Double>>, argb: Int): Mesh {
        val n = ring.size
        val vertices = ring.map { (x, y) -> a + w * x + v * y } + ring.map { (x, y) -> b + w * x + v * y }
        val triangles = mutableListOf<Triangle>()
        for (i in 1 until n - 1) { triangles.add(Triangle(0, i + 1, i)); triangles.add(Triangle(n, n + i, n + i + 1)) }
        for (i in 0 until n) { val j = (i + 1) % n; triangles.add(Triangle(i, j, j + n)); triangles.add(Triangle(i, j + n, i + n)) }
        val mesh = Mesh(name, vertices, triangles, argb)
        // Wind the faces outward whichever way the axes turned out.
        return if (mesh.signedVolume < 0) mesh.copy(triangles = triangles.map { Triangle(it.a, it.c, it.b) }) else mesh
    }
}
