package com.davoyans.idea3d.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceTest {
    private fun volume(m: Mesh) = m.triangles.sumOf { t -> m.vertices[t.a] dot (m.vertices[t.b] cross m.vertices[t.c]) } / 6.0

    @Test fun straightLineMakesPanel() {
        val m = SurfaceBuilder.build("s", listOf(Vec3(0.0, 0.0, 0.0), Vec3(100.0, 0.0, 0.0)), Vec3(0.0, 0.0, 1.0), 50.0, 2.0, 0)
        assertEquals(100.0 * 50.0 * 2.0, volume(m), 1e-6)
        assertTrue(MeshBuilder.validate(m).none { it.blocking })
    }

    @Test fun bentLineKeepsOutwardWinding() {
        val path = listOf(Vec3(0.0, 0.0, 0.0), Vec3(100.0, 0.0, 0.0), Vec3(100.0, 0.0, 80.0))
        val m = SurfaceBuilder.build("s", path, Vec3(0.0, 1.0, 0.0), 40.0, 3.0, 0)
        assertTrue(volume(m) > 0)
        assertEquals(180.0 * 40.0 * 3.0, volume(m), 180.0 * 40.0 * 3.0 * 0.05)
    }

    @Test fun piecesJoinIntoOneRun() {
        val a = listOf(Vec3(0.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0)); val b = listOf(Vec3(2.0, 0.0, 0.0), Vec3(1.0, 0.0, 0.0))
        val runs = SurfaceBuilder.chains(listOf(a, b))
        assertEquals(1, runs.size); assertEquals(3, runs[0].size)
    }
}
