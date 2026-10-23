package com.davoyans.idea3d.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * One vertex of a solid's outline in model millimeters. [z] is the front surface;
 * the solid extends back to z - [thicknessMm].
 */
data class OutlinePoint(val x: Double, val y: Double, val z: Double, val thicknessMm: Double)

/** An extruded object to export: its outline, display name and ARGB color. */
data class SolidSpec(val name: String, val outline: List<OutlinePoint>, val argb: Int)

data class Triangle(val a: Int, val b: Int, val c: Int)

data class Mesh(val name: String, val vertices: List<Vec3>, val triangles: List<Triangle>, val argb: Int) {
    fun normal(t: Triangle): Vec3 = ((vertices[t.b] - vertices[t.a]) cross (vertices[t.c] - vertices[t.a])).normalized()

    /** Signed volume; positive when all faces point outward. */
    val signedVolume: Double get() = triangles.sumOf { t -> vertices[t.a] dot (vertices[t.b] cross vertices[t.c]) } / 6.0

    val bounds: Pair<Vec3, Vec3> get() = Pair(
        Vec3(vertices.minOf { it.x }, vertices.minOf { it.y }, vertices.minOf { it.z }),
        Vec3(vertices.maxOf { it.x }, vertices.maxOf { it.y }, vertices.maxOf { it.z }),
    )
}

/** A problem that blocks or qualifies export, tied to the object it concerns. */
data class MeshIssue(val objectName: String, val message: String, val blocking: Boolean = true) {
    override fun toString() = "$objectName: $message"
}

class MeshException(val issues: List<MeshIssue>) : IllegalStateException(issues.joinToString("\n"))

object Polygon {
    fun signedArea(points: List<Vec2>): Double =
        points.indices.sumOf { i -> val a = points[i]; val b = points[(i + 1) % points.size]; a.x * b.y - b.x * a.y } / 2.0

    private fun orient(a: Vec2, b: Vec2, c: Vec2) = (b - a) cross (c - a)

    private fun properIntersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): Boolean {
        val d1 = orient(c, d, a); val d2 = orient(c, d, b); val d3 = orient(a, b, c); val d4 = orient(a, b, d)
        val eps = 1e-12 * max(1.0, max(abs(a.x), abs(a.y)))
        if (((d1 > eps && d2 < -eps) || (d1 < -eps && d2 > eps)) && ((d3 > eps && d4 < -eps) || (d3 < -eps && d4 > eps))) return true
        fun onSegment(p: Vec2, q: Vec2, r: Vec2) = abs(orient(p, q, r)) <= eps && min(p.x, q.x) - eps <= r.x && r.x <= max(p.x, q.x) + eps && min(p.y, q.y) - eps <= r.y && r.y <= max(p.y, q.y) + eps
        return onSegment(c, d, a) || onSegment(c, d, b) || onSegment(a, b, c) || onSegment(a, b, d)
    }

    /** Index pairs of non-adjacent edges that touch or cross. Empty for a simple polygon. */
    fun selfIntersections(points: List<Vec2>): List<Pair<Int, Int>> {
        val n = points.size
        val result = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until n) for (j in i + 1 until n) {
            if (j == i + 1 || (i == 0 && j == n - 1)) continue
            if (properIntersect(points[i], points[(i + 1) % n], points[j], points[(j + 1) % n])) result.add(i to j)
        }
        return result
    }

    /**
     * Ear-clipping triangulation of a simple polygon (convex or concave) given in
     * counter-clockwise order. Returns index triples, or null when no ear can be found.
     */
    fun triangulate(points: List<Vec2>): List<Triangle>? {
        val n = points.size
        if (n < 3) return null
        val remaining = (0 until n).toMutableList()
        val result = mutableListOf<Triangle>()
        var guard = 0
        while (remaining.size > 3) {
            if (guard++ > n * n) return null
            var clipped = false
            for (i in remaining.indices) {
                val ia = remaining[(i + remaining.size - 1) % remaining.size]; val ib = remaining[i]; val ic = remaining[(i + 1) % remaining.size]
                val a = points[ia]; val b = points[ib]; val c = points[ic]
                if (orient(a, b, c) <= 1e-12) continue
                val blocked = remaining.any { p ->
                    p != ia && p != ib && p != ic && points[p].let { q -> orient(a, b, q) >= -1e-12 && orient(b, c, q) >= -1e-12 && orient(c, a, q) >= -1e-12 }
                }
                if (blocked) continue
                result.add(Triangle(ia, ib, ic)); remaining.removeAt(i); clipped = true; break
            }
            if (!clipped) return null
        }
        if (orient(points[remaining[0]], points[remaining[1]], points[remaining[2]]) <= 0) return null
        result.add(Triangle(remaining[0], remaining[1], remaining[2]))
        return result
    }
}

object MeshBuilder {
    /** Drops repeated points and straight-line interior points whose Z and thickness are interpolated anyway. */
    fun simplify(outline: List<OutlinePoint>): List<OutlinePoint> {
        val points = outline.toMutableList()
        var changed = true
        while (changed && points.size > 3) {
            changed = false
            for (i in points.indices) {
                val a = points[(i + points.size - 1) % points.size]; val b = points[i]; val c = points[(i + 1) % points.size]
                val ab = kotlin.math.hypot(a.x - b.x, a.y - b.y); val ac = kotlin.math.hypot(a.x - c.x, a.y - c.y)
                val t = if (ac > 1e-9) ab / ac else 0.0
                val collinear = abs((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)) < 1e-9 * max(1.0, ac * ac)
                val linear = abs(b.z - (a.z * (1 - t) + c.z * t)) < 1e-6 && abs(b.thicknessMm - (a.thicknessMm * (1 - t) + c.thicknessMm * t)) < 1e-6
                if (ab < 1e-7 || collinear && linear && t in 0.0..1.0) { points.removeAt(i); changed = true; break }
            }
        }
        return points
    }

    /** Builds a closed prism-like solid from [spec], or throws [MeshException] explaining why it cannot. */
    fun extrude(spec: SolidSpec): Mesh {
        val issues = mutableListOf<MeshIssue>()
        fun fail(message: String): Nothing = throw MeshException(issues + MeshIssue(spec.name, message))
        if (spec.outline.any { !it.x.isFinite() || !it.y.isFinite() || !it.z.isFinite() || !it.thicknessMm.isFinite() }) fail("has invalid coordinates")
        val bad = spec.outline.count { it.thicknessMm <= 0.0 }
        if (bad > 0) fail("needs a positive thickness at every point ($bad point${if (bad == 1) "" else "s"} at zero or below)")
        var points = simplify(spec.outline)
        if (points.size < 3) fail("outline is too small")
        val flat = points.map { Vec2(it.x, it.y) }
        if (Polygon.selfIntersections(flat).isNotEmpty()) fail("outline crosses itself; separate the loops or redraw the crossing")
        val area = Polygon.signedArea(flat)
        if (abs(area) < 1e-6) fail("outline has no area")
        if (area < 0) points = points.reversed()
        val plan = points.map { Vec2(it.x, it.y) }
        val caps = Polygon.triangulate(plan) ?: fail("outline could not be filled")
        val n = points.size
        val vertices = points.map { Vec3(it.x, it.y, it.z) } + points.map { Vec3(it.x, it.y, it.z - it.thicknessMm) }
        val triangles = mutableListOf<Triangle>()
        caps.forEach { t ->
            triangles.add(Triangle(t.a, t.b, t.c)) // front faces +Z
            triangles.add(Triangle(t.c + n, t.b + n, t.a + n)) // back faces -Z
        }
        for (i in 0 until n) {
            val j = (i + 1) % n
            triangles.add(Triangle(i + n, j + n, j))
            triangles.add(Triangle(i + n, j, i))
        }
        return Mesh(spec.name, vertices, triangles, spec.argb)
    }

    /** Checks a mesh is closed, consistently oriented outward, and free of degenerate faces. */
    fun validate(mesh: Mesh): List<MeshIssue> {
        val issues = mutableListOf<MeshIssue>()
        val directed = HashMap<Pair<Int, Int>, Int>()
        var degenerate = 0
        mesh.triangles.forEach { t ->
            if (t.a == t.b || t.b == t.c || t.a == t.c || ((mesh.vertices[t.b] - mesh.vertices[t.a]) cross (mesh.vertices[t.c] - mesh.vertices[t.a])).length < 1e-12) degenerate++
            listOf(t.a to t.b, t.b to t.c, t.c to t.a).forEach { directed[it] = (directed[it] ?: 0) + 1 }
        }
        if (degenerate > 0) issues.add(MeshIssue(mesh.name, "$degenerate degenerate triangle${if (degenerate == 1) "" else "s"}"))
        val open = directed.keys.count { (a, b) -> !directed.containsKey(b to a) }
        val flipped = directed.values.count { it > 1 }
        if (open > 0) issues.add(MeshIssue(mesh.name, "mesh is not closed ($open open edge${if (open == 1) "" else "s"})"))
        if (flipped > 0) issues.add(MeshIssue(mesh.name, "faces have inconsistent normals"))
        if (open == 0 && flipped == 0 && mesh.signedVolume <= 0) issues.add(MeshIssue(mesh.name, "normals point inward or the volume is zero"))
        return issues
    }

    /**
     * Rejects solids whose outlines overlap in plan while their Z ranges overlap, because
     * slicers treat intersecting shells unpredictably. Touching faces are allowed.
     */
    fun overlapIssues(specs: List<SolidSpec>): List<MeshIssue> {
        val issues = mutableListOf<MeshIssue>()
        fun zRange(s: SolidSpec) = s.outline.minOf { it.z - it.thicknessMm } to s.outline.maxOf { it.z }
        for (i in specs.indices) for (j in i + 1 until specs.size) {
            val a = specs[i]; val b = specs[j]
            val za = zRange(a); val zb = zRange(b)
            if (za.second <= zb.first + 1e-6 || zb.second <= za.first + 1e-6) continue
            val pa = a.outline.map { Vec2(it.x, it.y) }; val pb = b.outline.map { Vec2(it.x, it.y) }
            if (outlinesOverlap(pa, pb)) issues.add(MeshIssue(a.name, "intersects ${b.name}; merge them or move one in Z"))
        }
        return issues
    }

    /** True when two simple outlines share a region of positive area (touching edges do not count). */
    fun outlinesOverlap(a: List<Vec2>, b: List<Vec2>): Boolean {
        val ta = ccwTriangles(a) ?: return false
        val tb = ccwTriangles(b) ?: return false
        val scale = max(abs(Polygon.signedArea(a)), abs(Polygon.signedArea(b)))
        return ta.any { x -> tb.any { y -> abs(Polygon.signedArea(clipConvex(x, y))) > 1e-9 * max(1.0, scale) } }
    }

    private fun ccwTriangles(points: List<Vec2>): List<List<Vec2>>? {
        val ccw = if (Polygon.signedArea(points) < 0) points.reversed() else points
        return Polygon.triangulate(ccw)?.map { listOf(ccw[it.a], ccw[it.b], ccw[it.c]) }
    }

    /** Sutherland-Hodgman clipping of [subject] by the counter-clockwise convex polygon [clip]. */
    private fun clipConvex(subject: List<Vec2>, clip: List<Vec2>): List<Vec2> {
        var output = subject
        for (i in clip.indices) {
            if (output.isEmpty()) break
            val c1 = clip[i]; val c2 = clip[(i + 1) % clip.size]
            fun inside(p: Vec2) = ((c2 - c1) cross (p - c1)) >= 0
            fun cut(p: Vec2, q: Vec2): Vec2 {
                val d1 = (c2 - c1) cross (p - c1); val d2 = (c2 - c1) cross (q - c1)
                val t = d1 / (d1 - d2)
                return Vec2(p.x + (q.x - p.x) * t, p.y + (q.y - p.y) * t)
            }
            val input = output; output = mutableListOf()
            for (j in input.indices) {
                val cur = input[j]; val prev = input[(j + input.size - 1) % input.size]
                if (inside(cur)) { if (!inside(prev)) output.add(cut(prev, cur)); output.add(cur) }
                else if (inside(prev)) output.add(cut(prev, cur))
            }
        }
        return output
    }

    /** Builds and validates every solid, collecting all problems before failing. */
    fun buildAll(specs: List<SolidSpec>): List<Mesh> {
        if (specs.isEmpty()) throw MeshException(listOf(MeshIssue("Model", "has no 3D objects. Extrude a closed shape first")))
        val issues = mutableListOf<MeshIssue>()
        val meshes = specs.mapNotNull { spec ->
            try { extrude(spec).also { issues += validate(it) } } catch (e: MeshException) { issues += e.issues; null }
        }
        issues += overlapIssues(specs)
        if (issues.any { it.blocking }) throw MeshException(issues)
        return meshes
    }
}
