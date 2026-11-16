package com.davoyans.idea3d.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Additive pipe: a cross-section swept along a path of points, like FreeCAD's Additive Pipe.
 *
 * The profile stays perpendicular to the path. Its frame is carried along without twisting (a rotation-minimizing
 * frame), starting with its height along [up], the drawing's depth axis, as bars do. Corners are mitered: the joint is
 * the plane halfway between the two segments, so neighbouring pieces meet exactly with no gap and no overlap.
 */
object SweepBuilder {
    fun build(name: String, input: List<Vec3>, closed: Boolean, up: Vec3, profile: MemberProfile, argb: Int, turnDeg: Double = 0.0): Mesh {
        profile.problem()?.let { throw MeshException(listOf(MeshIssue(name, it))) }
        val outer: List<Pair<Double, Double>>
        val holes: List<List<Pair<Double, Double>>>
        if (profile.kind == ProfileKind.DRAWN) {
            outer = profile.outline.map { it.x to it.y }; holes = profile.holes.map { h -> h.map { it.x to it.y } }
        } else {
            outer = MemberBuilder.ring(profile.kind.round, profile.widthMm, profile.heightMm)
            holes = if (profile.kind.hollow) listOf(MemberBuilder.ring(profile.kind.round, profile.widthMm - 2 * profile.wallMm, profile.heightMm - 2 * profile.wallMm)) else emptyList()
        }
        val path = clean(input, closed)
        val solid = sweep(name, path, closed, up, outer, argb, turnDeg, 0.0)
        // Holes run a little past open ends so the cuts are clean.
        return holes.fold(solid) { s, hole -> Csg.subtract(s, sweep(name, path, closed, up, hole, argb, turnDeg, 1.0)) }
    }

    private fun clean(input: List<Vec3>, closed: Boolean): List<Vec3> {
        val out = mutableListOf<Vec3>()
        input.forEach { p -> if (out.isEmpty() || (p - out.last()).length > 1e-6) out += p }
        if (closed && out.size > 1 && (out.first() - out.last()).length < 1e-6) out.removeAt(out.lastIndex)
        // Points in a straight line add nothing.
        fun straight(a: Vec3, b: Vec3, c: Vec3) = ((b - a).normalized() cross (c - b).normalized()).length < 1e-9 && ((b - a) dot (c - b)) > 0
        var changed = true
        while (changed && out.size > (if (closed) 3 else 2)) {
            changed = false
            val range = if (closed) out.indices else 1 until out.size - 1
            for (i in range) {
                val a = out[(i - 1 + out.size) % out.size]; val c = out[(i + 1) % out.size]
                if (straight(a, out[i], c)) { out.removeAt(i); changed = true; break }
            }
        }
        return out
    }

    /** Turns [x] around unit axis [k] by the rotation that takes unit [from] onto unit [to]. */
    private fun transport(x: Vec3, from: Vec3, to: Vec3): Vec3 {
        val k = from cross to; val s = k.length; val c = from dot to
        if (s < 1e-12) return x
        val axis = k * (1.0 / s)
        return x * c + (axis cross x) * s + axis * ((axis dot x) * (1 - c))
    }

    private fun sweep(name: String, path: List<Vec3>, closed: Boolean, up: Vec3, input: List<Pair<Double, Double>>, argb: Int, turnDeg: Double, extraEnds: Double): Mesh {
        val minPoints = if (closed) 3 else 2
        if (path.size < minPoints) throw MeshException(listOf(MeshIssue(name, "the path needs at least $minPoints points")))
        val ring = if (Polygon.signedArea(input.map { Vec2(it.first, it.second) }) < 0) input.reversed() else input
        val segments = if (closed) path.size else path.size - 1
        val dirs = (0 until segments).map { (path[(it + 1) % path.size] - path[it]).normalized() }
        // Frame of the first segment: height along the drawing's depth axis, as for bars.
        val u0 = dirs[0]
        var v = up - u0 * (up dot u0)
        if (v.length < 1e-6) v = (if (abs(u0.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).let { it - u0 * (it dot u0) }
        v = v.normalized()
        var w = u0 cross v
        if (turnDeg != 0.0) {
            val t = Math.toRadians(turnDeg); val c = cos(t); val s = sin(t)
            val w2 = w * c + v * s; v = v * c - w * s; w = w2
        }
        val vs = mutableListOf(v); val ws = mutableListOf(w)
        for (i in 1 until segments) { vs += transport(vs.last(), dirs[i - 1], dirs[i]).normalized(); ws += transport(ws.last(), dirs[i - 1], dirs[i]).normalized() }
        fun section(i: Int, at: Vec3) = ring.map { (x, y) -> at + ws[i] * x + vs[i] * y }
        /** The profile of segment [i] where it meets the next one, cut by the plane halfway between their directions. */
        fun miter(i: Int, next: Int, corner: Vec3): List<Vec3> {
            val normal = (dirs[i] + dirs[next]).normalized()
            if ((dirs[i] dot dirs[next]) < -0.9) throw MeshException(listOf(MeshIssue(name, "the path turns back too sharply for a pipe")))
            return section(i, corner).map { q -> q + dirs[i] * (-((q - corner) dot normal) / (dirs[i] dot normal)) }
        }
        val rings = mutableListOf<List<Vec3>>()
        if (closed) {
            for (k in path.indices) rings += miter((k - 1 + segments) % segments, k, path[k])
        } else {
            rings += section(0, path[0] - dirs[0] * extraEnds)
            for (k in 1 until path.size - 1) rings += miter(k - 1, k, path[k])
            rings += section(segments - 1, path.last() + dirs.last() * extraEnds)
        }
        val n = ring.size
        val vertices = rings.flatten()
        val triangles = mutableListOf<Triangle>()
        val spans = if (closed) rings.size else rings.size - 1
        for (r in 0 until spans) {
            val a = r * n; val b = ((r + 1) % rings.size) * n
            for (i in 0 until n) { val j = (i + 1) % n; triangles += Triangle(a + i, a + j, b + j); triangles += Triangle(a + i, b + j, b + i) }
        }
        if (!closed) {
            val caps = Polygon.triangulate(ring.map { Vec2(it.first, it.second) }) ?: throw MeshException(listOf(MeshIssue(name, "profile outline could not be filled")))
            val last = (rings.size - 1) * n
            caps.forEach { t -> triangles += Triangle(t.a, t.c, t.b); triangles += Triangle(last + t.a, last + t.b, last + t.c) }
        }
        val mesh = Mesh(name, vertices, triangles, argb)
        return if (mesh.signedVolume < 0) mesh.copy(triangles = triangles.map { Triangle(it.a, it.c, it.b) }) else mesh
    }
}
