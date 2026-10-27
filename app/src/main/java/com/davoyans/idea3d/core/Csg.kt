package com.davoyans.idea3d.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Solid booleans (cut and join) on closed meshes, using binary space partitioning as in Evan Wallace's csg.js.
 * Results are welded and their T-junctions split, so they pass [MeshBuilder.validate] like any extruded solid.
 */
object Csg {
    private const val EPSILON = 1e-5

    private class Plane(val normal: Vec3, val w: Double) {
        fun flipped() = Plane(normal * -1.0, -w)

        /** Sorts [polygon] into the four lists, splitting it when it crosses this plane. */
        fun split(polygon: Poly, coplanarFront: MutableList<Poly>, coplanarBack: MutableList<Poly>, front: MutableList<Poly>, back: MutableList<Poly>) {
            val types = polygon.vertices.map { v -> val t = (normal dot v) - w; if (t < -EPSILON) BACK else if (t > EPSILON) FRONT else COPLANAR }
            when (types.fold(0) { acc, t -> acc or t }) {
                COPLANAR -> (if ((normal dot polygon.plane.normal) > 0) coplanarFront else coplanarBack).add(polygon)
                FRONT -> front.add(polygon)
                BACK -> back.add(polygon)
                else -> {
                    val f = mutableListOf<Vec3>(); val b = mutableListOf<Vec3>()
                    for (i in polygon.vertices.indices) {
                        val j = (i + 1) % polygon.vertices.size
                        val ti = types[i]; val tj = types[j]; val vi = polygon.vertices[i]; val vj = polygon.vertices[j]
                        if (ti != BACK) f.add(vi)
                        if (ti != FRONT) b.add(vi)
                        if ((ti or tj) == SPANNING) {
                            val t = (w - (normal dot vi)) / (normal dot (vj - vi))
                            val v = vi + (vj - vi) * t
                            f.add(v); b.add(v)
                        }
                    }
                    if (f.size >= 3) front.add(Poly(f, polygon.plane))
                    if (b.size >= 3) back.add(Poly(b, polygon.plane))
                }
            }
        }

        companion object {
            const val COPLANAR = 0; const val FRONT = 1; const val BACK = 2; const val SPANNING = 3
            fun of(a: Vec3, b: Vec3, c: Vec3): Plane? {
                val n = (b - a) cross (c - a)
                if (n.length < 1e-12) return null
                val unit = n.normalized()
                return Plane(unit, unit dot a)
            }
        }
    }

    private class Poly(val vertices: List<Vec3>, val plane: Plane) {
        fun flipped() = Poly(vertices.reversed(), plane.flipped())
    }

    private class Node(polygons: List<Poly> = emptyList()) {
        var plane: Plane? = null
        var front: Node? = null
        var back: Node? = null
        var polygons = mutableListOf<Poly>()

        init { if (polygons.isNotEmpty()) build(polygons) }

        fun invert() {
            polygons = polygons.map { it.flipped() }.toMutableList()
            plane = plane?.flipped()
            front?.invert(); back?.invert()
            val t = front; front = back; back = t
        }

        fun clipPolygons(input: List<Poly>): List<Poly> {
            val p = plane ?: return input.toList()
            var f = mutableListOf<Poly>(); var b = mutableListOf<Poly>()
            input.forEach { p.split(it, f, b, f, b) }
            f = front?.clipPolygons(f)?.toMutableList() ?: f
            b = back?.clipPolygons(b)?.toMutableList() ?: mutableListOf()
            return f + b
        }

        fun clipTo(other: Node) {
            polygons = other.clipPolygons(polygons).toMutableList()
            front?.clipTo(other); back?.clipTo(other)
        }

        fun allPolygons(): List<Poly> = polygons + (front?.allPolygons() ?: emptyList()) + (back?.allPolygons() ?: emptyList())

        fun build(input: List<Poly>) {
            if (input.isEmpty()) return
            val p = plane ?: input.first().plane.also { plane = it }
            val f = mutableListOf<Poly>(); val b = mutableListOf<Poly>()
            input.forEach { p.split(it, polygons, polygons, f, b) }
            if (f.isNotEmpty()) (front ?: Node().also { front = it }).build(f)
            if (b.isNotEmpty()) (back ?: Node().also { back = it }).build(b)
        }
    }

    private fun polygons(mesh: Mesh): List<Poly> = mesh.triangles.mapNotNull { t ->
        val a = mesh.vertices[t.a]; val b = mesh.vertices[t.b]; val c = mesh.vertices[t.c]
        Plane.of(a, b, c)?.let { Poly(listOf(a, b, c), it) }
    }

    /** [a] with [b] removed. */
    fun subtract(a: Mesh, b: Mesh): Mesh {
        val na = Node(polygons(a)); val nb = Node(polygons(b))
        na.invert(); na.clipTo(nb); nb.clipTo(na); nb.invert(); nb.clipTo(na); nb.invert(); na.build(nb.allPolygons()); na.invert()
        return toMesh(a.name, na.allPolygons(), a.argb)
    }

    /** [a] and [b] merged into one shell. */
    fun union(a: Mesh, b: Mesh): Mesh {
        val na = Node(polygons(a)); val nb = Node(polygons(b))
        na.clipTo(nb); nb.clipTo(na); nb.invert(); nb.clipTo(na); nb.invert(); na.build(nb.allPolygons())
        return toMesh(a.name, na.allPolygons(), a.argb)
    }

    /** Only the volume shared by [a] and [b]. */
    fun intersect(a: Mesh, b: Mesh): Mesh {
        val na = Node(polygons(a)); val nb = Node(polygons(b))
        na.invert(); nb.clipTo(na); nb.invert(); na.clipTo(nb); nb.clipTo(na); na.build(nb.allPolygons()); na.invert()
        return toMesh(a.name, na.allPolygons(), a.argb)
    }

    /** Welds equal points, fans each polygon into triangles and splits edges that pass through other vertices. */
    private fun toMesh(name: String, polygons: List<Poly>, argb: Int): Mesh {
        val scale = max(1.0, polygons.flatMap { it.vertices }.maxOfOrNull { max(abs(it.x), max(abs(it.y), abs(it.z))) } ?: 1.0)
        val quantum = scale * 1e-9
        val index = HashMap<Triple<Long, Long, Long>, Int>()
        val vertices = mutableListOf<Vec3>()
        fun id(v: Vec3) = index.getOrPut(Triple((v.x / quantum).roundToLong(), (v.y / quantum).roundToLong(), (v.z / quantum).roundToLong())) { vertices.add(v); vertices.size - 1 }
        val triangles = mutableListOf<Triangle>()
        polygons.forEach { p ->
            val ids = p.vertices.map(::id).fold(mutableListOf<Int>()) { acc, i -> if (acc.lastOrNull() != i) acc.add(i); acc }
            if (ids.size > 1 && ids.first() == ids.last()) ids.removeAt(ids.size - 1)
            for (k in 1 until ids.size - 1) if (ids[0] != ids[k] && ids[k] != ids[k + 1] && ids[0] != ids[k + 1]) triangles.add(Triangle(ids[0], ids[k], ids[k + 1]))
        }
        return Mesh(name, vertices, splitTJunctions(vertices, triangles, scale), argb)
    }

    /**
     * BSP splitting leaves vertices in the middle of neighbouring triangles' edges. Splitting those triangles at such
     * vertices makes every edge shared by exactly two triangles, so the result is closed by index.
     */
    private fun splitTJunctions(vertices: List<Vec3>, input: List<Triangle>, scale: Double): List<Triangle> {
        val tolerance = scale * 1e-7
        var triangles = input
        repeat(64) {
            var changed = false
            val next = ArrayList<Triangle>(triangles.size)
            for (t in triangles) {
                val corners = intArrayOf(t.a, t.b, t.c)
                var split: Triangle? = null; var other: Triangle? = null
                loop@ for (e in 0 until 3) {
                    val i = corners[e]; val j = corners[(e + 1) % 3]; val k = corners[(e + 2) % 3]
                    val a = vertices[i]; val b = vertices[j]; val ab = b - a; val length2 = ab dot ab
                    if (length2 < tolerance * tolerance) continue
                    for (v in vertices.indices) {
                        if (v == i || v == j || v == k) continue
                        val p = vertices[v]; val s = ((p - a) dot ab) / length2
                        if (s <= 1e-9 || s >= 1 - 1e-9) continue
                        if ((a + ab * s - p).length > tolerance) continue
                        split = Triangle(i, v, k); other = Triangle(v, j, k); break@loop
                    }
                }
                if (split != null && other != null) { next.add(split); next.add(other); changed = true } else next.add(t)
            }
            triangles = next
            if (!changed) return triangles
        }
        return triangles
    }
}

/** A cut or join between two solids, by their index in the list being combined. */
data class SolidOperation(val target: Int, val tool: Int, val join: Boolean, val keepTool: Boolean = false, val common: Boolean = false)

object SolidCombiner {
    /**
     * Applies [operations] in order. Returns one mesh per input solid, or null where a solid was used up: every
     * joined tool, and cut tools that are not kept.
     */
    fun apply(meshes: List<Mesh>, operations: List<SolidOperation>): List<Mesh?> {
        val shapes = meshes.toMutableList()
        val removed = BooleanArray(meshes.size)
        operations.forEach { op ->
            if (op.target !in shapes.indices || op.tool !in shapes.indices || op.target == op.tool) return@forEach
            shapes[op.target] = when { op.common -> Csg.intersect(shapes[op.target], shapes[op.tool]); op.join -> Csg.union(shapes[op.target], shapes[op.tool]); else -> Csg.subtract(shapes[op.target], shapes[op.tool]) }
            if (op.join || op.common || !op.keepTool) removed[op.tool] = true
        }
        return shapes.mapIndexed { i, mesh -> mesh.takeUnless { removed[i] } }
    }
}
