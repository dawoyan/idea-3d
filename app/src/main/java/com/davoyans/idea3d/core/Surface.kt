package com.davoyans.idea3d.core

/**
 * A surface grown from an open line: the line, made [thicknessMm] thick across itself, pushed [depthMm] away from
 * the viewer of its drawing (against [toward]). A straight line gives a flat panel, a curve or broken line a bent
 * sheet. The result is one closed solid, wound outward.
 */
object SurfaceBuilder {
    fun build(name: String, path: List<Vec3>, toward: Vec3, depthMm: Double, thicknessMm: Double, argb: Int): Mesh {
        val pts = path.fold(mutableListOf<Vec3>()) { acc, p -> if (acc.isEmpty() || (p - acc.last()).length > 1e-6) acc.add(p); acc }
        if (pts.size < 2) throw MeshException(listOf(MeshIssue(name, "line has no length")))
        if (!(depthMm > 0.0) || !(thicknessMm > 0.0)) throw MeshException(listOf(MeshIssue(name, "surface needs a depth and a thickness")))
        val k = toward.normalized()
        // Across the line, in the drawing's plane; mitred at bends so the sheet keeps its thickness.
        fun across(a: Vec3, b: Vec3): Vec3 { val c = k cross (b - a); return if (c.length < 1e-9) Vec3(0.0, 0.0, 0.0) else c.normalized() }
        val n = pts.size
        val normals = List(n) { i ->
            when (i) {
                0 -> across(pts[0], pts[1])
                n - 1 -> across(pts[n - 2], pts[n - 1])
                else -> {
                    val a = across(pts[i - 1], pts[i]); val b = across(pts[i], pts[i + 1])
                    val m = a + b
                    if (m.length < 1e-9) a else m.normalized().let { it * (1.0 / (it dot a).coerceAtLeast(0.35)) }
                }
            }
        }
        val half = thicknessMm / 2
        val back = k * -depthMm
        val p = pts.indices.map { pts[it] - normals[it] * half }
        val q = pts.indices.map { pts[it] + normals[it] * half }
        val vertices = p + q + p.map { it + back } + q.map { it + back }
        fun P(i: Int) = i
        fun Q(i: Int) = n + i
        fun Pb(i: Int) = 2 * n + i
        fun Qb(i: Int) = 3 * n + i
        val tris = ArrayList<Triangle>()
        fun quad(a: Int, b: Int, c: Int, d: Int) { tris += Triangle(a, b, c); tris += Triangle(a, c, d) }
        for (i in 0 until n - 1) {
            quad(P(i), P(i + 1), Q(i + 1), Q(i))          // front
            quad(Pb(i), Qb(i), Qb(i + 1), Pb(i + 1))      // back
            quad(P(i), Pb(i), Pb(i + 1), P(i + 1))        // one side
            quad(Q(i), Q(i + 1), Qb(i + 1), Qb(i))        // other side
        }
        quad(P(0), Q(0), Qb(0), Pb(0))
        quad(P(n - 1), Pb(n - 1), Qb(n - 1), Q(n - 1))
        // Outward winding whatever the drawing's handedness.
        val volume = tris.sumOf { t -> vertices[t.a] dot (vertices[t.b] cross vertices[t.c]) }
        val wound = if (volume < 0) tris.map { Triangle(it.a, it.c, it.b) } else tris
        return Mesh(name, vertices, wound, argb)
    }

    /**
     * Joins line pieces (each a list of points) end to end into as few runs as possible, turning pieces round where
     * needed. Ends closer than [tolerance] count as joined.
     */
    fun chains(pieces: List<List<Vec3>>, tolerance: Double = 0.5): List<List<Vec3>> {
        val left = pieces.filter { it.size >= 2 }.toMutableList()
        val out = mutableListOf<List<Vec3>>()
        while (left.isNotEmpty()) {
            var chain = left.removeAt(0).toMutableList()
            var grew = true
            while (grew) {
                grew = false
                val it = left.iterator()
                while (it.hasNext()) {
                    val s = it.next()
                    fun near(a: Vec3, b: Vec3) = (a - b).length <= tolerance
                    when {
                        near(chain.last(), s.first()) -> chain.addAll(s.drop(1))
                        near(chain.last(), s.last()) -> chain.addAll(s.reversed().drop(1))
                        near(chain.first(), s.last()) -> chain = (s.dropLast(1) + chain).toMutableList()
                        near(chain.first(), s.first()) -> chain = (s.reversed().dropLast(1) + chain).toMutableList()
                        else -> continue
                    }
                    it.remove(); grew = true
                }
            }
            out += chain
        }
        return out
    }
}
