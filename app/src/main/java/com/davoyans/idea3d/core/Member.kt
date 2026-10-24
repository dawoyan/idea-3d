package com.davoyans.idea3d.core

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Cross-section of a bar or pipe. Round profiles use [widthMm] as the outer diameter. */
enum class ProfileKind(val label: String, val hollow: Boolean, val round: Boolean) {
    RECT_TUBE("Rectangular tube", true, false),
    RECT_BAR("Rectangular bar", false, false),
    ROUND_TUBE("Round tube", true, true),
    ROUND_BAR("Round bar", false, true),
    /** Any closed outline drawn in a project, with optional holes. */
    DRAWN("Drawn shape", false, false);

    companion object { fun of(name: String?) = entries.firstOrNull { it.name == name } ?: RECT_TUBE }
}

/**
 * A profile's sizes in millimeters: width across the drawing, height along its depth axis, and wall thickness for tubes.
 * A [ProfileKind.DRAWN] profile instead has an [outline] and [holes] (x across, y up, centered on the line).
 */
data class MemberProfile(val kind: ProfileKind, val widthMm: Double, val heightMm: Double, val wallMm: Double = 0.0, val outline: List<Vec2> = emptyList(), val holes: List<List<Vec2>> = emptyList()) {
    /** Problems that make the profile impossible to build, or null when it is fine. */
    fun problem(): String? = when {
        kind == ProfileKind.DRAWN -> if (outline.size < 3 || kotlin.math.abs(Polygon.signedArea(outline)) < 1e-9) "The drawn outline has no area" else null
        widthMm <= 0 || (!kind.round && heightMm <= 0) -> "Sizes must be positive"
        kind.hollow && wallMm <= 0 -> "Wall thickness must be positive"
        kind.hollow && (2 * wallMm >= widthMm || (!kind.round && 2 * wallMm >= heightMm)) -> "The wall is too thick for this size"
        else -> null
    }

    /** Area of the cross-section, square millimeters. */
    val areaMm2: Double get() {
        if (kind == ProfileKind.DRAWN) return kotlin.math.abs(Polygon.signedArea(outline)) - holes.sumOf { kotlin.math.abs(Polygon.signedArea(it)) }
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

    /** [turnDeg] turns the profile around the line, for example to show a 20 × 40 tube's narrow side to the front. */
    fun build(name: String, a: Vec3, b: Vec3, up: Vec3, profile: MemberProfile, argb: Int, turnDeg: Double = 0.0): Mesh {
        profile.problem()?.let { throw MeshException(listOf(MeshIssue(name, it))) }
        val along = b - a
        if (along.length < 1e-6) throw MeshException(listOf(MeshIssue(name, "line has no length")))
        val u = along.normalized()
        // Height follows the drawing's depth axis unless the line itself points that way.
        var v = up - u * (up dot u)
        if (v.length < 1e-6) v = (if (kotlin.math.abs(u.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).let { it - u * (it dot u) }
        v = v.normalized()
        var w = u cross v
        if (turnDeg != 0.0) {
            val t = Math.toRadians(turnDeg); val c = kotlin.math.cos(t); val s = kotlin.math.sin(t)
            val w2 = w * c + v * s; v = v * c - w * s; w = w2
        }
        // Holes run a little past both ends so the cuts are clean.
        val extra = u * 1.0
        if (profile.kind == ProfileKind.DRAWN) {
            val outer = prism(name, a, b, u, v, w, profile.outline.map { it.x to it.y }, argb)
            return profile.holes.fold(outer) { solid, hole -> Csg.subtract(solid, prism(name, a - extra, b + extra, u, v, w, hole.map { it.x to it.y }, argb)) }
        }
        val outer = prism(name, a, b, u, v, w, ring(profile.kind.round, profile.widthMm, profile.heightMm), argb)
        if (!profile.kind.hollow) return outer
        val inner = prism(name, a - extra, b + extra, u, v, w, ring(profile.kind.round, profile.widthMm - 2 * profile.wallMm, profile.heightMm - 2 * profile.wallMm), argb)
        return Csg.subtract(outer, inner)
    }

    /**
     * The cut face of a member at [a]: its outline and holes as 3D points, turned like [build] turns it.
     * Used to show a bar's or pipe's shape where a line points into the screen.
     */
    fun section(a: Vec3, b: Vec3, up: Vec3, profile: MemberProfile, turnDeg: Double = 0.0): List<List<Vec3>> {
        if (profile.problem() != null) return emptyList()
        val along = b - a
        if (along.length < 1e-6) return emptyList()
        val u = along.normalized()
        var v = up - u * (up dot u)
        if (v.length < 1e-6) v = (if (kotlin.math.abs(u.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).let { it - u * (it dot u) }
        v = v.normalized()
        var w = u cross v
        if (turnDeg != 0.0) {
            val t = Math.toRadians(turnDeg); val c = kotlin.math.cos(t); val s = kotlin.math.sin(t)
            val w2 = w * c + v * s; v = v * c - w * s; w = w2
        }
        val loops = if (profile.kind == ProfileKind.DRAWN) listOf(profile.outline.map { it.x to it.y }) + profile.holes.map { h -> h.map { it.x to it.y } }
            else listOf(ring(profile.kind.round, profile.widthMm, profile.heightMm)) +
                if (profile.kind.hollow) listOf(ring(profile.kind.round, profile.widthMm - 2 * profile.wallMm, profile.heightMm - 2 * profile.wallMm)) else emptyList()
        return loops.map { loop -> loop.map { (x, y) -> a + w * x + v * y } }
    }

    /** Cross-section corners as (across, up) pairs. */
    private fun ring(round: Boolean, width: Double, height: Double): List<Pair<Double, Double>> =
        if (round) (0 until ROUND_SEGMENTS).map { i -> val t = 2 * PI * i / ROUND_SEGMENTS; width / 2 * cos(t) to width / 2 * sin(t) }
        else listOf(-width / 2 to -height / 2, width / 2 to -height / 2, width / 2 to height / 2, -width / 2 to height / 2)

    private fun prism(name: String, a: Vec3, b: Vec3, u: Vec3, v: Vec3, w: Vec3, input: List<Pair<Double, Double>>, argb: Int): Mesh {
        // Ear clipping needs counter-clockwise corners; it also fills concave outlines.
        val ring = if (Polygon.signedArea(input.map { Vec2(it.first, it.second) }) < 0) input.reversed() else input
        val n = ring.size
        val caps = Polygon.triangulate(ring.map { Vec2(it.first, it.second) }) ?: throw MeshException(listOf(MeshIssue(name, "profile outline could not be filled")))
        val vertices = ring.map { (x, y) -> a + w * x + v * y } + ring.map { (x, y) -> b + w * x + v * y }
        val triangles = mutableListOf<Triangle>()
        caps.forEach { t -> triangles.add(Triangle(t.a, t.c, t.b)); triangles.add(Triangle(n + t.a, n + t.b, n + t.c)) }
        for (i in 0 until n) { val j = (i + 1) % n; triangles.add(Triangle(i, j, j + n)); triangles.add(Triangle(i, j + n, i + n)) }
        val mesh = Mesh(name, vertices, triangles, argb)
        // Wind the faces outward whichever way the axes turned out.
        return if (mesh.signedVolume < 0) mesh.copy(triangles = triangles.map { Triangle(it.a, it.c, it.b) }) else mesh
    }
}
