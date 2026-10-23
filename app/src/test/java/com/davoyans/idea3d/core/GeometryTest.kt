package com.davoyans.idea3d.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class UnitsTest {
    @Test fun unitConversionChangesPresentationOnly() {
        val mm = 254.0
        assertEquals("254 mm", formatLength(mm, LengthUnit.MM))
        assertEquals("25.4 cm", formatLength(mm, LengthUnit.CM))
        assertEquals("0.25 m", formatLength(mm, LengthUnit.M))
        assertEquals("10 in", formatLength(mm, LengthUnit.IN))
        assertEquals("0.83 ft", formatLength(mm, LengthUnit.FT))
        LengthUnit.entries.forEach { assertEquals(mm, it.toMm(it.fromMm(mm)), 1e-9) }
    }

    @Test fun decimalsAreConfigurableFromZeroToTwo() {
        assertEquals("13 mm", formatLength(12.5, LengthUnit.MM, 0))
        assertEquals("12.5 mm", formatLength(12.5, LengthUnit.MM, 1))
        assertEquals("12.35", formatLength(12.345, LengthUnit.MM, 2, withUnit = false))
        assertEquals("12.35", formatLength(12.345, LengthUnit.MM, 5, withUnit = false))
        assertEquals("0 mm", formatLength(-0.001, LengthUnit.MM, 2))
    }

    @Test fun defaultUnitIsMillimeters() = assertEquals(LengthUnit.MM, LengthUnit.of("unknown"))

    @Test fun parsesTypedLengthsIntoMillimeters() {
        assertEquals(25.4, parseLengthMm("1", LengthUnit.IN)!!, 1e-9)
        assertEquals(15.0, parseLengthMm("1,5", LengthUnit.CM)!!, 1e-9)
        assertNull(parseLengthMm("abc", LengthUnit.MM))
    }

    @Test fun pointNamesContinueBeyondZ() {
        assertEquals(listOf("A", "B", "Z", "AA", "AB", "AZ", "BA", "ZZ", "AAA"), listOf(1, 2, 26, 27, 28, 52, 53, 702, 703).map(::pointName))
    }
}

class SpaceTest {
    @Test fun axesAreRightHanded() {
        val x = Vec3(1.0, 0.0, 0.0); val y = Vec3(0.0, 1.0, 0.0)
        assertEquals(Vec3(0.0, 0.0, 1.0), x cross y) // +Z toward viewer of the front view
    }

    @Test fun trueLengthUsesPythagorasWithZ() {
        val s = segmentLength(Vec3(0.0, 0.0, 0.0), Vec3(30.0, 40.0, 120.0))
        assertEquals(50.0, s.frontProjectionMm, 1e-9)
        assertEquals(130.0, s.trueMm, 1e-9)
        assertTrue(s.projectionIsShorter)
        assertFalse(segmentLength(Vec3(0.0, 0.0, 5.0), Vec3(3.0, 4.0, 5.0)).projectionIsShorter)
    }

    @Test fun fixedLengthConstrainsProjectionWhenZChanges() {
        assertEquals(80.0, projectedLengthForFixedTrue(100.0, 60.0)!!, 1e-9)
        assertNull(projectedLengthForFixedTrue(100.0, 120.0)) // conflict must be resolved by the user
    }
}

class CalibrationTest {
    @Test fun missingCalibrationInventsNoSize() {
        val c = Calibration.from(emptyList())
        assertEquals(CalibrationStatus.Missing, c.status)
        assertNull(c.lengthMm(100.0, 0.0))
    }

    @Test fun singleKnownDistanceIsUniform() {
        val c = Calibration.from(listOf(CalibrationReference(300.0, 400.0, 100.0)))
        assertEquals(CalibrationStatus.Uniform, c.status)
        assertEquals(5.0, c.pxPerMmX!!, 1e-9); assertEquals(5.0, c.pxPerMmY!!, 1e-9)
        // A square stays square under uniform scale.
        assertEquals(c.lengthMm(250.0, 0.0)!!, c.lengthMm(0.0, 250.0)!!, 1e-9)
    }

    @Test fun imageYDownBecomesModelYUp() {
        val c = Calibration.from(listOf(CalibrationReference(10.0, 0.0, 10.0)))
        val (x, y) = c.toModelMm(20.0, 30.0)!!
        assertEquals(20.0, x, 1e-9); assertEquals(-30.0, y, 1e-9)
    }

    @Test fun perAxisCalibrationIsExplicitAndExplained() {
        val c = Calibration.from(listOf(CalibrationReference(200.0, 10.0, 100.0, CalibrationAxis.X), CalibrationReference(5.0, 300.0, 100.0, CalibrationAxis.Y)))
        assertTrue(c.status is CalibrationStatus.PerAxis)
        assertEquals(2.0, c.pxPerMmX!!, 1e-9); assertEquals(3.0, c.pxPerMmY!!, 1e-9)
        assertEquals(1.5, (c.status as CalibrationStatus.PerAxis).verticalStretch, 1e-9)
        assertTrue(c.explanation().contains("stretched"))
    }

    @Test fun onlyOneAxisIsIncomplete() {
        val c = Calibration.from(listOf(CalibrationReference(200.0, 0.0, 100.0, CalibrationAxis.X)))
        assertEquals(CalibrationStatus.Incomplete(CalibrationAxis.Y), c.status)
        assertFalse(c.isUsable)
    }

    @Test fun contradictoryReferencesAreReported() {
        val c = Calibration.from(listOf(CalibrationReference(100.0, 0.0, 100.0), CalibrationReference(150.0, 0.0, 100.0)))
        assertTrue(c.status is CalibrationStatus.Contradictory)
        assertFalse(c.isUsable)
        assertTrue(c.explanation().contains("disagree"))
    }

    @Test fun consistentReferencesAverage() {
        val c = Calibration.from(listOf(CalibrationReference(100.0, 0.0, 100.0), CalibrationReference(0.0, 101.0, 100.0)))
        assertEquals(CalibrationStatus.Uniform, c.status)
        assertEquals(1.005, c.pxPerMmX!!, 1e-9)
    }

    @Test fun uniformReferenceContradictingAxisIsReported() {
        val c = Calibration.from(listOf(CalibrationReference(100.0, 0.0, 100.0), CalibrationReference(200.0, 0.0, 100.0, CalibrationAxis.X)))
        assertTrue(c.status is CalibrationStatus.Contradictory)
    }

    @Test fun zeroOrNegativeKnownLengthIsIgnored() {
        assertEquals(CalibrationStatus.Missing, Calibration.from(listOf(CalibrationReference(100.0, 0.0, 0.0))).status)
    }
}

class MeshTest {
    private fun rect(w: Double, h: Double, thickness: Double = 10.0, z: Double = 0.0, dx: Double = 0.0) =
        listOf(OutlinePoint(dx, 0.0, z, thickness), OutlinePoint(dx + w, 0.0, z, thickness), OutlinePoint(dx + w, h, z, thickness), OutlinePoint(dx, h, z, thickness))

    private fun assertClosedOutward(mesh: Mesh) {
        assertEquals(emptyList<MeshIssue>(), MeshBuilder.validate(mesh))
        assertTrue(mesh.signedVolume > 0)
    }

    @Test fun boxHasExactDimensionsAndVolume() {
        val mesh = MeshBuilder.extrude(SolidSpec("Box", rect(40.0, 20.0, 10.0), 0))
        assertClosedOutward(mesh)
        assertEquals(8000.0, mesh.signedVolume, 1e-6)
        val (min, max) = mesh.bounds
        assertEquals(Vec3(0.0, 0.0, -10.0), min); assertEquals(Vec3(40.0, 20.0, 0.0), max)
    }

    @Test fun frontFaceIsAtZAndPointsTowardViewer() {
        val mesh = MeshBuilder.extrude(SolidSpec("Box", rect(10.0, 10.0, 4.0, z = 6.0), 0))
        val front = mesh.triangles.filter { t -> listOf(t.a, t.b, t.c).all { mesh.vertices[it].z == 6.0 } }
        assertTrue(front.isNotEmpty())
        front.forEach { assertEquals(1.0, mesh.normal(it).z, 1e-9) }
        assertEquals(2.0, mesh.bounds.first.z, 1e-9)
    }

    @Test fun clockwiseOutlineIsReoriented() = assertClosedOutward(MeshBuilder.extrude(SolidSpec("CW", rect(10.0, 10.0).reversed(), 0)))

    @Test fun concaveLShapeTriangulatesWithCorrectArea() {
        val l = listOf(0.0 to 0.0, 30.0 to 0.0, 30.0 to 10.0, 10.0 to 10.0, 10.0 to 30.0, 0.0 to 30.0).map { OutlinePoint(it.first, it.second, 0.0, 5.0) }
        val mesh = MeshBuilder.extrude(SolidSpec("L", l, 0))
        assertClosedOutward(mesh)
        assertEquals(500.0 * 5.0, mesh.signedVolume, 1e-6)
    }

    @Test fun deeplyConcaveStarIsValid() {
        val star = (0 until 10).map { i ->
            val r = if (i % 2 == 0) 50.0 else 15.0; val a = PI / 2 + i * PI / 5
            OutlinePoint(r * cos(a), r * sin(a), 0.0, 3.0)
        }
        assertClosedOutward(MeshBuilder.extrude(SolidSpec("Star", star, 0)))
    }

    @Test fun curvedOutlineFollowsTheCurve() {
        val circle = (0 until 128).map { i -> val a = 2 * PI * i / 128; OutlinePoint(20 * cos(a), 20 * sin(a), 0.0, 2.0) }
        val mesh = MeshBuilder.extrude(SolidSpec("Disc", circle, 0))
        assertClosedOutward(mesh)
        assertEquals(PI * 400 * 2, mesh.signedVolume, PI * 400 * 2 * 0.002)
    }

    @Test fun collinearPointsAreDropped() {
        val outline = listOf(OutlinePoint(0.0, 0.0, 0.0, 1.0), OutlinePoint(5.0, 0.0, 0.0, 1.0), OutlinePoint(10.0, 0.0, 0.0, 1.0), OutlinePoint(10.0, 10.0, 0.0, 1.0), OutlinePoint(0.0, 10.0, 0.0, 1.0))
        assertEquals(4, MeshBuilder.simplify(outline).size)
    }

    @Test fun variableDepthPointsArePreserved() {
        val outline = listOf(OutlinePoint(0.0, 0.0, 0.0, 2.0), OutlinePoint(10.0, 0.0, 5.0, 2.0), OutlinePoint(10.0, 10.0, 5.0, 2.0), OutlinePoint(0.0, 10.0, 0.0, 2.0))
        val mesh = MeshBuilder.extrude(SolidSpec("Ramp", outline, 0))
        assertClosedOutward(mesh)
        assertEquals(200.0, mesh.signedVolume, 1e-6)
        assertEquals(5.0, mesh.bounds.second.z, 1e-9)
    }

    @Test fun selfIntersectingOutlineIsExplained() {
        val bowtie = listOf(0.0 to 0.0, 10.0 to 10.0, 10.0 to 0.0, 0.0 to 10.0).map { OutlinePoint(it.first, it.second, 0.0, 1.0) }
        try { MeshBuilder.extrude(SolidSpec("Bowtie", bowtie, 0)); fail() } catch (e: MeshException) {
            assertEquals("Bowtie", e.issues.single().objectName)
            assertTrue(e.issues.single().message.contains("crosses itself"))
        }
    }

    @Test fun zeroThicknessIsRejected() {
        try { MeshBuilder.extrude(SolidSpec("Flat", rect(10.0, 10.0, 0.0), 0)); fail() } catch (e: MeshException) {
            assertTrue(e.issues.single().message.contains("positive thickness"))
        }
    }

    @Test fun degenerateOutlineIsRejected() {
        val line = listOf(OutlinePoint(0.0, 0.0, 0.0, 1.0), OutlinePoint(10.0, 0.0, 0.0, 1.0), OutlinePoint(20.0, 0.0, 0.0, 1.0))
        try { MeshBuilder.extrude(SolidSpec("Line", line, 0)); fail() } catch (e: MeshException) { assertEquals("Line", e.issues.single().objectName) }
    }

    @Test fun openMeshIsDetected() {
        val mesh = MeshBuilder.extrude(SolidSpec("Box", rect(10.0, 10.0), 0))
        val broken = mesh.copy(triangles = mesh.triangles.drop(1))
        assertTrue(MeshBuilder.validate(broken).any { it.message.contains("not closed") })
    }

    @Test fun invertedMeshIsDetected() {
        val mesh = MeshBuilder.extrude(SolidSpec("Box", rect(10.0, 10.0), 0))
        val inverted = mesh.copy(triangles = mesh.triangles.map { Triangle(it.a, it.c, it.b) })
        assertTrue(MeshBuilder.validate(inverted).any { it.message.contains("inward") })
    }

    @Test fun overlappingSolidsAreReportedButStackedOrSeparateAreFine() {
        val a = SolidSpec("Shape 1", rect(10.0, 10.0, 5.0), 0)
        val overlapping = SolidSpec("Shape 2", rect(10.0, 10.0, 5.0, dx = 5.0), 0)
        val stacked = SolidSpec("Shape 3", rect(10.0, 10.0, 5.0, z = 5.0), 0)
        val apart = SolidSpec("Shape 4", rect(10.0, 10.0, 5.0, dx = 20.0), 0)
        val inside = SolidSpec("Shape 5", rect(2.0, 2.0, 5.0, dx = 4.0).map { it.copy(y = it.y + 4) }, 0)
        assertEquals("Shape 1", MeshBuilder.overlapIssues(listOf(a, overlapping)).single().objectName)
        assertTrue(MeshBuilder.overlapIssues(listOf(a, stacked)).isEmpty())
        assertTrue(MeshBuilder.overlapIssues(listOf(a, apart)).isEmpty())
        assertEquals(1, MeshBuilder.overlapIssues(listOf(a, inside)).size)
    }

    @Test fun buildAllCollectsEveryProblem() {
        val bowtie = listOf(0.0 to 0.0, 10.0 to 10.0, 10.0 to 0.0, 0.0 to 10.0).map { OutlinePoint(it.first, it.second, 0.0, 1.0) }
        try { MeshBuilder.buildAll(listOf(SolidSpec("A", bowtie, 0), SolidSpec("B", rect(5.0, 5.0, -1.0), 0))); fail() } catch (e: MeshException) {
            assertEquals(setOf("A", "B"), e.issues.map { it.objectName }.toSet())
        }
        try { MeshBuilder.buildAll(emptyList()); fail() } catch (e: MeshException) { assertTrue(e.message!!.contains("no 3D objects")) }
    }
}

class ExportTest {
    private val box = MeshBuilder.extrude(SolidSpec("Box <1>", listOf(OutlinePoint(0.0, 0.0, 0.0, 10.0), OutlinePoint(40.0, 0.0, 0.0, 10.0), OutlinePoint(40.0, 20.0, 0.0, 10.0), OutlinePoint(0.0, 20.0, 0.0, 10.0)), 0xFF0066BE.toInt()))

    @Test fun stlIsBinaryWithMillimeterCoordinates() {
        val bytes = ModelExport.stl(listOf(box))
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(80)
        val count = buffer.int
        assertEquals(12, count)
        assertEquals(84 + 50 * count, bytes.size)
        val xs = mutableListOf<Float>(); val zs = mutableListOf<Float>()
        repeat(count) {
            val nx = buffer.float; val ny = buffer.float; val nz = buffer.float
            assertEquals(1.0, sqrt((nx * nx + ny * ny + nz * nz).toDouble()), 1e-5)
            repeat(3) { xs += buffer.float; buffer.float; zs += buffer.float }
            buffer.short
        }
        assertEquals(0f, xs.min()); assertEquals(40f, xs.max()); assertEquals(-10f, zs.min()); assertEquals(0f, zs.max())
    }

    @Test fun threeMfPackagesMillimeterModelWithColor() {
        val bytes = ModelExport.threeMf(listOf(box))
        val entries = mutableMapOf<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip -> generateSequence { zip.nextEntry }.forEach { entries[it.name] = zip.readBytes().decodeToString() } }
        assertEquals(setOf("[Content_Types].xml", "_rels/.rels", "3D/3dmodel.model"), entries.keys)
        val model = entries.getValue("3D/3dmodel.model")
        assertTrue(model.contains("unit=\"millimeter\""))
        assertTrue(model.contains("displaycolor=\"#0066BEFF\""))
        assertTrue(model.contains("name=\"Box &lt;1&gt;\""))
        assertEquals(8, Regex("<vertex ").findAll(model).count())
        assertEquals(12, Regex("<triangle ").findAll(model).count())
        assertTrue(model.contains("x=\"40\""))
        assertNotNull(Regex("z=\"-10\"").find(model))
    }
}

class PlaneTest {
    private fun box(plane: ModelPlane) = SolidSpec("Box", listOf(OutlinePoint(0.0, 0.0, 5.0, 10.0), OutlinePoint(20.0, 0.0, 5.0, 10.0), OutlinePoint(20.0, 30.0, 5.0, 10.0), OutlinePoint(0.0, 30.0, 5.0, 10.0)), 0, plane)

    @Test fun planeFramesAreRightHanded() {
        ModelPlane.entries.forEach { assertEquals(0.0, ((it.right cross it.up) - it.toward).length, 1e-12) }
    }

    @Test fun topPlaneExtrudesAlongYWithFrontAtTheBottom() {
        val mesh = MeshBuilder.extrude(box(ModelPlane.TOP))
        assertTrue(MeshBuilder.validate(mesh).isEmpty())
        assertTrue(mesh.signedVolume > 0)
        val (low, high) = mesh.bounds
        assertEquals(Vec3(0.0, -5.0, -30.0), low); assertEquals(Vec3(20.0, 5.0, 0.0), high)
    }

    @Test fun profilePlaneExtrudesAlongXWithFrontOnTheLeft() {
        val mesh = MeshBuilder.extrude(box(ModelPlane.PROFILE))
        assertTrue(MeshBuilder.validate(mesh).isEmpty())
        assertEquals(6000.0, mesh.signedVolume, 1e-6)
        val (low, high) = mesh.bounds
        assertEquals(Vec3(-5.0, 0.0, -20.0), low); assertEquals(Vec3(5.0, 30.0, 0.0), high)
    }

    @Test fun solidsFromDifferentPlanesOnlyWarnWhenTheyMayIntersect() {
        val issues = MeshBuilder.overlapIssues(listOf(box(ModelPlane.FRONT), box(ModelPlane.TOP)))
        assertEquals(1, issues.size); assertFalse(issues.single().blocking)
        assertTrue(MeshBuilder.buildAll(listOf(box(ModelPlane.FRONT), box(ModelPlane.TOP))).size == 2)
    }
}

class CsgTest {
    private fun cube(x: Double, y: Double, z: Double, size: Double) = MeshBuilder.extrude(SolidSpec("Cube", listOf(
        OutlinePoint(x, y, z + size, size), OutlinePoint(x + size, y, z + size, size), OutlinePoint(x + size, y + size, z + size, size), OutlinePoint(x, y + size, z + size, size)), 0))

    @Test fun cuttingACornerRemovesItsVolumeAndStaysClosed() {
        val result = Csg.subtract(cube(0.0, 0.0, 0.0, 10.0), cube(5.0, 5.0, 5.0, 10.0))
        assertEquals(875.0, result.signedVolume, 1e-6)
        assertTrue(MeshBuilder.validate(result).isEmpty())
    }

    @Test fun cuttingAHoleThroughLeavesATube() {
        val result = Csg.subtract(cube(0.0, 0.0, 0.0, 10.0), MeshBuilder.extrude(SolidSpec("Pin", listOf(
            OutlinePoint(4.0, 4.0, 20.0, 30.0), OutlinePoint(6.0, 4.0, 20.0, 30.0), OutlinePoint(6.0, 6.0, 20.0, 30.0), OutlinePoint(4.0, 6.0, 20.0, 30.0)), 0)))
        assertEquals(1000.0 - 40.0, result.signedVolume, 1e-6)
        assertTrue(MeshBuilder.validate(result).isEmpty())
    }

    @Test fun joiningOverlappingSolidsCountsTheSharedPartOnce() {
        val result = Csg.union(cube(0.0, 0.0, 0.0, 10.0), cube(5.0, 5.0, 5.0, 10.0))
        assertEquals(2000.0 - 125.0, result.signedVolume, 1e-6)
        assertTrue(MeshBuilder.validate(result).isEmpty())
    }

    @Test fun combinerRemovesUsedToolsUnlessKept() {
        val a = cube(0.0, 0.0, 0.0, 10.0); val b = cube(5.0, 5.0, 5.0, 10.0); val c = cube(20.0, 0.0, 0.0, 5.0)
        val cut = SolidCombiner.apply(listOf(a, b, c), listOf(SolidOperation(0, 1, join = false)))
        assertEquals(875.0, cut[0]!!.signedVolume, 1e-6); assertNull(cut[1]); assertNotNull(cut[2])
        val kept = SolidCombiner.apply(listOf(a, b), listOf(SolidOperation(0, 1, join = false, keepTool = true)))
        assertNotNull(kept[1])
    }

    @Test fun solidsFromDifferentPlanesCanBeCut() {
        val slab = MeshBuilder.extrude(SolidSpec("Slab", listOf(OutlinePoint(0.0, 0.0, 10.0, 10.0), OutlinePoint(10.0, 0.0, 10.0, 10.0), OutlinePoint(10.0, -10.0, 10.0, 10.0), OutlinePoint(0.0, -10.0, 10.0, 10.0)), 0, ModelPlane.TOP))
        val result = Csg.subtract(slab, cube(-1.0, -1.0, -5.0, 6.0))
        assertTrue(result.signedVolume < 1000.0 && result.signedVolume > 0)
        assertTrue(MeshBuilder.validate(result).isEmpty())
    }
}

class MemberTest {
    private val a = Vec3(0.0, 0.0, 0.0); private val b = Vec3(1000.0, 0.0, 0.0); private val up = Vec3(0.0, 0.0, 1.0)

    @Test fun rectangularBarHasItsFullVolume() {
        val mesh = MemberBuilder.build("Bar", a, b, up, MemberProfile(ProfileKind.RECT_BAR, 40.0, 20.0), 0)
        assertEquals(40.0 * 20.0 * 1000.0, mesh.signedVolume, 1e-3)
        assertTrue(MeshBuilder.validate(mesh).isEmpty())
        val (low, high) = mesh.bounds
        assertEquals(20.0, high.z - low.z, 1e-9); assertEquals(40.0, high.y - low.y, 1e-9)
    }

    @Test fun rectangularTubeIsHollowAndClosed() {
        val profile = MemberProfile(ProfileKind.RECT_TUBE, 40.0, 20.0, 2.0)
        val mesh = MemberBuilder.build("Pipe", a, b, up, profile, 0)
        assertEquals(profile.areaMm2 * 1000.0, mesh.signedVolume, 1e-3)
        assertEquals((40.0 * 20.0 - 36.0 * 16.0), profile.areaMm2, 1e-9)
        assertTrue(MeshBuilder.validate(mesh).isEmpty())
    }

    @Test fun roundTubeFollowsASlantedLine() {
        val mesh = MemberBuilder.build("Round", Vec3(0.0, 0.0, 0.0), Vec3(300.0, 400.0, 0.0), up, MemberProfile(ProfileKind.ROUND_TUBE, 25.0, 25.0, 2.0), 0)
        assertTrue(mesh.signedVolume > 0); assertTrue(MeshBuilder.validate(mesh).isEmpty())
    }

    @Test fun impossibleProfilesAreRejected() {
        assertNotNull(MemberProfile(ProfileKind.RECT_TUBE, 10.0, 10.0, 6.0).problem())
        try { MemberBuilder.build("Bad", a, a, up, MemberProfile(ProfileKind.RECT_BAR, 10.0, 10.0), 0); fail() } catch (e: MeshException) { }
    }
}

class DrawnProfileTest {
    private val a = Vec3(0.0, 0.0, 0.0); private val b = Vec3(0.0, 500.0, 0.0); private val up = Vec3(0.0, 0.0, 1.0)
    private fun square(size: Double) = listOf(Vec2(-size / 2, -size / 2), Vec2(size / 2, -size / 2), Vec2(size / 2, size / 2), Vec2(-size / 2, size / 2))

    @Test fun drawnSquareWithAHoleIsATube() {
        val profile = MemberProfile(ProfileKind.DRAWN, 40.0, 40.0, outline = square(40.0), holes = listOf(square(36.0)))
        val mesh = MemberBuilder.build("Tube 4*4", a, b, up, profile, 0)
        assertEquals((1600.0 - 1296.0) * 500.0, mesh.signedVolume, 1e-3)
        assertEquals(1600.0 - 1296.0, profile.areaMm2, 1e-9)
        assertTrue(MeshBuilder.validate(mesh).isEmpty())
    }

    @Test fun concaveDrawnOutlineIsFilledCorrectly() {
        // An L, drawn clockwise.
        val l = listOf(Vec2(0.0, 0.0), Vec2(0.0, 30.0), Vec2(10.0, 30.0), Vec2(10.0, 10.0), Vec2(30.0, 10.0), Vec2(30.0, 0.0))
        val mesh = MemberBuilder.build("Angle", a, b, up, MemberProfile(ProfileKind.DRAWN, 30.0, 30.0, outline = l), 0)
        assertEquals(500.0 * 500.0, mesh.signedVolume, 1e-3)
        assertTrue(MeshBuilder.validate(mesh).isEmpty())
    }
}

