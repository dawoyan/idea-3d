package com.davoyans.idea3d

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class CollisionTest {
    /** A box of size w×h×d at the origin corner, turned by [deg] round Z, then moved by (x, y). */
    private fun box(w: Float, h: Float, d: Float, deg: Double, x: Float, y: Float): List<FloatArray> {
        val c = (0..7).map { b -> floatArrayOf(if (b and 1 == 0) 0f else w, if (b and 2 == 0) 0f else h, if (b and 4 == 0) 0f else d) }.map { p ->
            val r = Math.toRadians(deg); floatArrayOf((p[0] * cos(r) - p[1] * sin(r)).toFloat() + x, (p[0] * sin(r) + p[1] * cos(r)).toFloat() + y, p[2]) }
        val faces = listOf(intArrayOf(0, 1, 3, 2), intArrayOf(4, 6, 7, 5), intArrayOf(0, 4, 5, 1), intArrayOf(2, 3, 7, 6), intArrayOf(0, 2, 6, 4), intArrayOf(1, 5, 7, 3))
        return faces.flatMap { f -> listOf(c[f[0]], c[f[1]], c[f[2]], c[f[0]], c[f[2]], c[f[3]]) }
    }
    private fun overlapping(vararg boxes: List<FloatArray>): Int {
        val tris = boxes.sumOf { it.size / 3 }
        val buf = ByteBuffer.allocate(84 + tris * 50).order(ByteOrder.LITTLE_ENDIAN); buf.position(80); buf.putInt(tris)
        val objects = IntArray(tris); var t = 0
        boxes.forEachIndexed { i, b -> b.chunked(3).forEach { tri -> repeat(3) { buf.putFloat(0f) }; tri.forEach { p -> p.forEach(buf::putFloat) }; buf.putShort(0); objects[t++] = i } }
        val mesh = ViewerMesh.fromStl(buf.array(), List(tris) { 0 }, triangleObjects = objects, objectNames = boxes.indices.map { "o$it" })
        return mesh.collisionBoxes.size
    }
    @Test fun slopedBeamRestingOnPostsDoesNotOverlap() {
        val postL = box(50f, 2000f, 50f, 0.0, 0f, 0f); val postR = box(50f, 1900f, 50f, 0.0, 1950f, 0f)
        // Beam lying on both post tops, sloping down: its lower face passes through (0,2000)… just above both corners.
        val deg = Math.toDegrees(kotlin.math.atan2(-100.0, 2000.0))
        val beam = box(2600f, 100f, 50f, deg, -300f, 2016f)
        val rail = box(1900f, 80f, 50f, 0.0, 50f, 1700f)
        assertEquals(0, overlapping(postL, postR, beam, rail))
    }
    @Test fun realOverlapIsFound() {
        assertEquals(2, overlapping(box(100f, 100f, 100f, 0.0, 0f, 0f), box(100f, 100f, 100f, 30.0, 50f, 50f)))
    }
}
