package com.davoyans.idea3d.core

import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** The DXF layers Idea 3D writes. */
object DxfLayers {
    const val GEOMETRY = "Geometry"
    const val HOLES = "Holes"
    const val MEASUREMENTS = "Measurements"
    const val MATERIAL_LABELS = "Material labels"
    /** Name and AutoCAD color index of each layer, in the order written. */
    val all = listOf(GEOMETRY to 7, HOLES to 1, MEASUREMENTS to 4, MATERIAL_LABELS to 5)
}

/** A DXF entity in drawing millimeters (X right, Y up). */
sealed interface DxfEntity { val layer: String }
data class DxfLine(override val layer: String, val a: Vec2, val b: Vec2) : DxfEntity
data class DxfCircle(override val layer: String, val center: Vec2, val radius: Double) : DxfEntity
/** Counterclockwise from [startDeg] to [endDeg], as DXF draws arcs. */
data class DxfArc(override val layer: String, val center: Vec2, val radius: Double, val startDeg: Double, val endDeg: Double) : DxfEntity
/** [bulges]`[i]` belongs to the segment from vertex i to the next (tan of a quarter of its arc's angle; 0 = straight). */
data class DxfPolyline(override val layer: String, val vertices: List<Vec2>, val bulges: List<Double>, val closed: Boolean) : DxfEntity
data class DxfText(override val layer: String, val at: Vec2, val height: Double, val text: String) : DxfEntity

/**
 * One drawn piece in drawing millimeters: straight from [a] to [b], or (with [mid]) the quadratic curve from [a]
 * through [mid] (its halfway point) to [b], the way Idea 3D draws curves.
 */
data class DxfSegment(val a: Vec2, val b: Vec2, val mid: Vec2? = null)

/**
 * Turns Idea 3D's 2D geometry into DXF (R2000, millimeters).
 *
 * Curves: a curve is written as an exact ARC (or an arc bulge inside a polyline) when the circular arc through its
 * start, middle and end stays within [arcTolerance] of it everywhere: [TOLERANCE_MM] or [ARC_TOLERANCE_RATIO] of the
 * radius, whichever is larger. Idea 3D draws a circle as 8 curved pieces that stay within 0.073% of the true circle,
 * so a drawn circle is written as the circle meant. Any other curve is written as straight pieces whose distance from
 * the curve is at most [TOLERANCE_MM]. A closed outline whose pieces all fit one circle is written as a CIRCLE.
 */
object DxfExport {
    const val TOLERANCE_MM = 0.1
    const val ARC_TOLERANCE_RATIO = 0.001
    fun arcTolerance(radius: Double) = max(TOLERANCE_MM, radius * ARC_TOLERANCE_RATIO)

    private fun ok(v: Vec2) = v.x.isFinite() && v.y.isFinite()
    private fun dist(a: Vec2, b: Vec2) = hypot(a.x - b.x, a.y - b.y)

    /** The curve's control point (Idea 3D stores the curve's middle point instead). */
    private fun control(s: DxfSegment): Vec2 { val m = s.mid!!; return Vec2(2 * m.x - (s.a.x + s.b.x) / 2, 2 * m.y - (s.a.y + s.b.y) / 2) }
    fun pointAt(s: DxfSegment, t: Double): Vec2 {
        if (s.mid == null) return Vec2(s.a.x + (s.b.x - s.a.x) * t, s.a.y + (s.b.y - s.a.y) * t)
        val c = control(s); val u = 1 - t
        return Vec2(u * u * s.a.x + 2 * u * t * c.x + t * t * s.b.x, u * u * s.a.y + 2 * u * t * c.y + t * t * s.b.y)
    }

    /** A circular arc through a segment's start, middle and end: center, radius and signed sweep (radians, + = counterclockwise). */
    data class ArcFit(val center: Vec2, val radius: Double, val startAngle: Double, val sweep: Double)

    /** The arc through the curve's three points when the curve stays within the tolerance of it, else null. */
    fun arcOf(s: DxfSegment): ArcFit? {
        val m = s.mid ?: return null
        val a = s.a; val b = s.b
        val d = 2 * (a.x * (m.y - b.y) + m.x * (b.y - a.y) + b.x * (a.y - m.y))
        if (abs(d) < 1e-9) return null
        val a2 = a.x * a.x + a.y * a.y; val m2 = m.x * m.x + m.y * m.y; val b2 = b.x * b.x + b.y * b.y
        val center = Vec2((a2 * (m.y - b.y) + m2 * (b.y - a.y) + b2 * (a.y - m.y)) / d, (a2 * (b.x - m.x) + m2 * (a.x - b.x) + b2 * (m.x - a.x)) / d)
        val r = dist(center, a)
        if (!r.isFinite() || r < 1e-9) return null
        if ((0..32).any { i -> abs(dist(pointAt(s, i / 32.0), center) - r) > arcTolerance(r) }) return null
        val ccw = ((m.x - a.x) * (b.y - m.y) - (m.y - a.y) * (b.x - m.x)) > 0
        val sa = atan2(a.y - center.y, a.x - center.x); val sb = atan2(b.y - center.y, b.x - center.x)
        fun norm(x: Double) = ((x % (2 * PI)) + 2 * PI) % (2 * PI)
        val sweep = if (ccw) norm(sb - sa) else -norm(sa - sb)
        return ArcFit(center, r, sa, sweep)
    }

    /** The curve as points (start included, end included) whose chords stay within the tolerance of it. */
    fun flatten(s: DxfSegment): List<Vec2> {
        if (s.mid == null) return listOf(s.a, s.b)
        val c = control(s)
        // For a quadratic curve the chord error with n equal steps is at most |a - 2c + b| / (4 n²).
        val second = hypot(s.a.x - 2 * c.x + s.b.x, s.a.y - 2 * c.y + s.b.y)
        val n = min(2048, max(1, ceil(sqrt(second / (4 * TOLERANCE_MM))).toInt()))
        return (0..n).map { pointAt(s, it.toDouble() / n) }
    }

    /** An open piece: a LINE, an ARC, or a polyline approximating the curve. */
    fun openEntity(layer: String, s: DxfSegment): DxfEntity {
        require(ok(s.a) && ok(s.b) && (s.mid == null || ok(s.mid))) { "invalid coordinates" }
        if (s.mid == null) {
            require(dist(s.a, s.b) > 1e-9) { "line has no length" }
            return DxfLine(layer, s.a, s.b)
        }
        arcOf(s)?.let { f ->
            val start = if (f.sweep >= 0) f.startAngle else f.startAngle + f.sweep
            return DxfArc(layer, f.center, f.radius, Math.toDegrees(start).normalizedDegrees(), Math.toDegrees(start + abs(f.sweep)).normalizedDegrees())
        }
        val pts = flatten(s)
        return DxfPolyline(layer, pts, List(pts.size) { 0.0 }, closed = false)
    }

    /** A closed outline (its pieces in order, each starting where the last ended): a CIRCLE or a closed LWPOLYLINE. */
    fun closedEntity(layer: String, segments: List<DxfSegment>): DxfEntity {
        require(segments.isNotEmpty()) { "empty outline" }
        require(segments.all { ok(it.a) && ok(it.b) && (it.mid == null || ok(it.mid)) }) { "invalid coordinates" }
        val arcs = segments.map { if (it.mid != null) arcOf(it) else null }
        // One circle all round: every piece an arc of the same circle, sweeping a full turn together.
        if (arcs.all { it != null }) {
            val first = arcs.first()!!
            val tol = arcTolerance(first.radius)
            val same = arcs.all { f -> dist(f!!.center, first.center) <= tol && abs(f.radius - first.radius) <= tol }
            if (same && abs(abs(arcs.sumOf { it!!.sweep }) - 2 * PI) < 1e-3) return DxfCircle(layer, first.center, first.radius)
        }
        val vertices = mutableListOf<Vec2>(); val bulges = mutableListOf<Double>()
        segments.forEachIndexed { i, s ->
            val f = arcs[i]
            when {
                s.mid == null -> { vertices += s.a; bulges += 0.0 }
                f != null -> { vertices += s.a; bulges += tan(f.sweep / 4) }
                else -> flatten(s).dropLast(1).forEach { vertices += it; bulges += 0.0 }
            }
        }
        require(vertices.size >= 2) { "outline is too small" }
        return DxfPolyline(layer, vertices, bulges, closed = true)
    }

    private fun Double.normalizedDegrees() = ((this % 360.0) + 360.0) % 360.0

    // ---- Writing ----

    private fun num(v: Double) = String.format(Locale.US, "%.6f", v).trimEnd('0').trimEnd('.').let { if (it == "-0") "0" else it }

    /** DXF text: plain ASCII; other characters as \U+XXXX, which CAD programs show as the character. */
    fun escape(text: String): String = buildString {
        text.replace('\n', ' ').replace('\r', ' ').forEach { c -> if (c.code in 32..126) append(c) else append("\\U+%04X".format(Locale.US, c.code)) }
    }

    /** The whole DXF file (R2000, millimeters) for [entities]. */
    fun write(entities: List<DxfEntity>): String {
        val out = StringBuilder()
        var handle = 0x20
        fun next() = Integer.toHexString(handle++).uppercase()
        fun g(code: Int, value: Any) { out.append(code).append('\n').append(if (value is Double) num(value) else value.toString()).append('\n') }
        fun xy(code: Int, p: Vec2) { g(code, p.x); g(code + 10, p.y); g(code + 20, 0.0) }

        val points = entities.flatMap { e -> when (e) {
            is DxfLine -> listOf(e.a, e.b)
            is DxfCircle -> listOf(Vec2(e.center.x - e.radius, e.center.y - e.radius), Vec2(e.center.x + e.radius, e.center.y + e.radius))
            is DxfArc -> listOf(Vec2(e.center.x - e.radius, e.center.y - e.radius), Vec2(e.center.x + e.radius, e.center.y + e.radius))
            is DxfPolyline -> e.vertices
            is DxfText -> listOf(e.at)
        } }
        val low = Vec2(points.minOfOrNull { it.x } ?: 0.0, points.minOfOrNull { it.y } ?: 0.0)
        val high = Vec2(points.maxOfOrNull { it.x } ?: 0.0, points.maxOfOrNull { it.y } ?: 0.0)

        // Handles are fixed up front for the records others point to.
        val blockRecordTable = next(); val modelRecord = next(); val paperRecord = next()
        val layerTable = next(); val ltypeTable = next(); val styleTable = next(); val appidTable = next(); val dimstyleTable = next()
        val vportTable = next(); val viewTable = next(); val ucsTable = next()
        val rootDict = next(); val groupDict = next()

        g(0, "SECTION"); g(2, "HEADER")
        g(9, "\$ACADVER"); g(1, "AC1015")
        g(9, "\$DWGCODEPAGE"); g(3, "ANSI_1252")
        g(9, "\$INSUNITS"); g(70, 4)
        g(9, "\$MEASUREMENT"); g(70, 1)
        g(9, "\$EXTMIN"); xy(10, low)
        g(9, "\$EXTMAX"); xy(10, high)
        val handseedAt = out.length
        g(0, "ENDSEC")

        g(0, "SECTION"); g(2, "TABLES")
        fun table(name: String, h: String, count: Int) { g(0, "TABLE"); g(2, name); g(5, h); g(330, 0); g(100, "AcDbSymbolTable"); g(70, count) }
        table("VPORT", vportTable, 0); g(0, "ENDTAB")
        table("LTYPE", ltypeTable, 3)
        listOf("ByBlock", "ByLayer", "Continuous").forEach { name ->
            g(0, "LTYPE"); g(5, next()); g(330, ltypeTable); g(100, "AcDbSymbolTableRecord"); g(100, "AcDbLinetypeTableRecord")
            g(2, name); g(70, 0); g(3, if (name == "Continuous") "Solid line" else ""); g(72, 65); g(73, 0); g(40, 0.0)
        }
        g(0, "ENDTAB")
        table("LAYER", layerTable, DxfLayers.all.size + 1)
        (listOf("0" to 7) + DxfLayers.all).forEach { (name, color) ->
            g(0, "LAYER"); g(5, next()); g(330, layerTable); g(100, "AcDbSymbolTableRecord"); g(100, "AcDbLayerTableRecord")
            g(2, name); g(70, 0); g(62, color); g(6, "Continuous")
        }
        g(0, "ENDTAB")
        table("STYLE", styleTable, 1)
        g(0, "STYLE"); g(5, next()); g(330, styleTable); g(100, "AcDbSymbolTableRecord"); g(100, "AcDbTextStyleTableRecord")
        g(2, "Standard"); g(70, 0); g(40, 0.0); g(41, 1.0); g(50, 0.0); g(71, 0); g(42, 2.5); g(3, "txt"); g(4, "")
        g(0, "ENDTAB")
        table("VIEW", viewTable, 0); g(0, "ENDTAB")
        table("UCS", ucsTable, 0); g(0, "ENDTAB")
        table("APPID", appidTable, 1)
        g(0, "APPID"); g(5, next()); g(330, appidTable); g(100, "AcDbSymbolTableRecord"); g(100, "AcDbRegAppTableRecord"); g(2, "ACAD"); g(70, 0)
        g(0, "ENDTAB")
        table("DIMSTYLE", dimstyleTable, 1); g(100, "AcDbDimStyleTable")
        g(0, "DIMSTYLE"); g(105, next()); g(330, dimstyleTable); g(100, "AcDbSymbolTableRecord"); g(100, "AcDbDimStyleTableRecord"); g(2, "Standard"); g(70, 0)
        g(0, "ENDTAB")
        table("BLOCK_RECORD", blockRecordTable, 2)
        listOf(modelRecord to "*Model_Space", paperRecord to "*Paper_Space").forEach { (h, name) ->
            g(0, "BLOCK_RECORD"); g(5, h); g(330, blockRecordTable); g(100, "AcDbSymbolTableRecord"); g(100, "AcDbBlockTableRecord"); g(2, name)
        }
        g(0, "ENDTAB")
        g(0, "ENDSEC")

        g(0, "SECTION"); g(2, "BLOCKS")
        listOf(modelRecord to "*Model_Space", paperRecord to "*Paper_Space").forEach { (owner, name) ->
            g(0, "BLOCK"); g(5, next()); g(330, owner); g(100, "AcDbEntity"); g(8, "0"); g(100, "AcDbBlockBegin"); g(2, name); g(70, 0); xy(10, Vec2(0.0, 0.0)); g(3, name); g(1, "")
            g(0, "ENDBLK"); g(5, next()); g(330, owner); g(100, "AcDbEntity"); g(8, "0"); g(100, "AcDbBlockEnd")
        }
        g(0, "ENDSEC")

        g(0, "SECTION"); g(2, "ENTITIES")
        fun head(type: String, layer: String) { g(0, type); g(5, next()); g(330, modelRecord); g(100, "AcDbEntity"); g(8, layer) }
        entities.forEach { e ->
            when (e) {
                is DxfLine -> { head("LINE", e.layer); g(100, "AcDbLine"); xy(10, e.a); xy(11, e.b) }
                is DxfCircle -> { head("CIRCLE", e.layer); g(100, "AcDbCircle"); xy(10, e.center); g(40, e.radius) }
                is DxfArc -> { head("ARC", e.layer); g(100, "AcDbCircle"); xy(10, e.center); g(40, e.radius); g(100, "AcDbArc"); g(50, e.startDeg); g(51, e.endDeg) }
                is DxfPolyline -> {
                    head("LWPOLYLINE", e.layer); g(100, "AcDbPolyline"); g(90, e.vertices.size); g(70, if (e.closed) 1 else 0); g(43, 0.0)
                    e.vertices.forEachIndexed { i, v -> g(10, v.x); g(20, v.y); val bulge = e.bulges.getOrElse(i) { 0.0 }; if (abs(bulge) > 1e-12) g(42, bulge) }
                }
                is DxfText -> { head("TEXT", e.layer); g(100, "AcDbText"); xy(10, e.at); g(40, e.height); g(1, escape(e.text)); g(7, "Standard"); g(100, "AcDbText") }
            }
        }
        g(0, "ENDSEC")

        g(0, "SECTION"); g(2, "OBJECTS")
        g(0, "DICTIONARY"); g(5, rootDict); g(330, 0); g(100, "AcDbDictionary"); g(281, 1); g(3, "ACAD_GROUP"); g(350, groupDict)
        g(0, "DICTIONARY"); g(5, groupDict); g(330, rootDict); g(100, "AcDbDictionary"); g(281, 1)
        g(0, "ENDSEC")
        g(0, "EOF")

        // The next free handle, now that all are given out.
        out.insert(handseedAt, "9\n\$HANDSEED\n5\n${Integer.toHexString(handle).uppercase()}\n")
        return out.toString()
    }
}
