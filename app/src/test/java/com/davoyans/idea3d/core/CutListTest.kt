package com.davoyans.idea3d.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CutListTest {
    private val tube = MemberProfile(ProfileKind.RECT_TUBE, 40.0, 40.0, 2.0)
    private fun bar(id: Int, a: Vec3, b: Vec3, path: Any? = null, order: Int = id) =
        CutBar(ModelPlane.FRONT to id, "P$id", "Tube", listOf(a, b), ModelPlane.FRONT.toward, tube, pathKey = path, order = order)

    @Test fun mitredCornerOfOneBrokenLineIs45() {
        val r = CutList.compute(listOf(bar(1, Vec3(0.0, 0.0, 0.0), Vec3(1000.0, 0.0, 0.0), "p"), bar(2, Vec3(1000.0, 0.0, 0.0), Vec3(1000.0, 500.0, 0.0), "p")), emptyList())
        val p1 = r.pieces[0]
        assertEquals(CutKind.MITER, p1.end.kind)
        assertEquals(45.0, p1.end.angleDeg, 0.01)
        assertEquals(1000.0, p1.cutMm, 0.01)
        assertEquals(1020.0, p1.longPointMm, 0.01)
        assertEquals(1, r.connections.size)
        assertEquals("Weld", r.connections[0].typeName)
    }

    @Test fun braceMeetingAPostAt80DegreesIsCutAt80() {
        val t = Math.toRadians(80.0)
        val post = bar(1, Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1000.0, 0.0))
        // The brace runs from a point on the post's centre line out to the right, 80° from the post.
        val start = Vec3(0.0, 500.0, 0.0)
        val brace = bar(2, start, start + Vec3(sin(t), -cos(t), 0.0) * 600.0)
        val r = CutList.compute(listOf(post, brace), emptyList())
        val cut = r.pieces[1].start
        assertEquals(CutKind.BUTT, cut.kind)
        assertEquals(80.0, cut.angleDeg, 0.01)
        assertEquals(10.0, cut.sawDeg, 0.01)
        assertEquals("W", cut.face)
        // It stops at the post's face, 20 mm from its centre line, measured along the brace.
        assertEquals(600.0 - 20.0 / sin(t), r.pieces[1].cutMm, 0.01)
        assertTrue(r.pieces[0].start.square && r.pieces[0].end.square)
        assertEquals("Butt", r.connections.single().kind)
    }

    @Test fun plainCornerOldestRunsThrough() {
        val r = CutList.compute(listOf(bar(1, Vec3(0.0, 0.0, 0.0), Vec3(1000.0, 0.0, 0.0)), bar(2, Vec3(1000.0, 0.0, 0.0), Vec3(1000.0, 500.0, 0.0))), emptyList())
        assertEquals(CutKind.THROUGH, r.pieces[0].end.kind)
        assertEquals(1020.0, r.pieces[0].cutMm, 0.01)
        assertEquals(CutKind.BUTT, r.pieces[1].start.kind)
        assertEquals(480.0, r.pieces[1].cutMm, 0.01)
    }

    @Test fun recordedJoinGivesTheFastening() {
        val post = bar(1, Vec3(0.0, 0.0, 0.0), Vec3(0.0, 1000.0, 0.0))
        val rail = bar(2, Vec3(0.0, 500.0, 0.0), Vec3(800.0, 500.0, 0.0))
        val join = RecordedJoin(7, rail.key, post.key, Vec3(20.0, 500.0, 0.0), "screw", "Wood screw", "5 × 60", 4)
        val c = CutList.compute(listOf(post, rail), listOf(join)).connections.single()
        assertEquals(1, c.number)
        assertEquals("Wood screw", c.typeName)
        assertEquals(4, c.count)
    }
}
