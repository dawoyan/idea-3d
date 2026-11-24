package com.davoyans.idea3d.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

/** One bar or pipe of the model as a workshop cuts it: its centre line in model millimeters and its profile. */
data class CutBar(
    val key: Pair<ModelPlane, Int>,
    val name: String,
    val material: String,
    val points: List<Vec3>,
    val up: Vec3,
    val profile: MemberProfile,
    val turnDeg: Double = 0.0,
    /** Lines of one broken line of one material share this: they meet in mitred corners. */
    val pathKey: Any? = null,
    /** Bars welded together share this. */
    val weldGroup: Any? = null,
    /** Lower is older: at a plain corner the oldest bar runs through. */
    val order: Int = 0,
    val curved: Boolean = false,
    val argb: Int = 0xFF808080.toInt(),
)

/** A join the user recorded between two bars, with its fastening. */
data class RecordedJoin(val id: Int, val a: Pair<ModelPlane, Int>, val b: Pair<ModelPlane, Int>, val at: Vec3, val typeId: String, val typeName: String, val detail: String, val count: Int)

enum class CutKind(val label: String) { SQUARE("Square"), MITER("Miter"), BUTT("Butt"), COPE("Cope"), THROUGH("Runs through") }

/**
 * How one end of a piece is cut. [angleDeg] is between the piece's axis and the cut face: 90 is a square cut, 80 a cut
 * leaning 10° off square. [sawDeg] is the same as a mitre-saw setting (0 = square). [face] tells on which side of a
 * rectangular profile the slope shows: "W" across the width, "H" across the height, or a turn for a compound cut.
 */
data class EndCut(val kind: CutKind, val angleDeg: Double, val face: String, val partner: Pair<ModelPlane, Int>?, val adjustMm: Double, val longExtraMm: Double, val shortExtraMm: Double) {
    val sawDeg get() = 90.0 - angleDeg
    val square get() = abs(sawDeg) < 0.25
}

data class PieceCut(val bar: CutBar, val centreMm: Double, val start: EndCut, val end: EndCut) {
    /** Length along the centre line after fitting to the pieces it meets. */
    val cutMm get() = centreMm + start.adjustMm + end.adjustMm
    /** The longest edge, where a slanted cut is measured on the saw. */
    val longPointMm get() = cutMm + start.longExtraMm + end.longExtraMm
    val shortPointMm get() = cutMm + start.shortExtraMm + end.shortExtraMm
}

/** A numbered join between two pieces: as found in the geometry, with the fastening recorded for it if any. */
data class Connection(val number: Int, val a: Pair<ModelPlane, Int>, val b: Pair<ModelPlane, Int>, val at: Vec3, val kind: String, val angleDeg: Double?, val recorded: RecordedJoin?, val autoType: String, val radiusMm: Double) {
    val typeName get() = recorded?.typeName ?: autoType
    val detail get() = recorded?.detail.orEmpty()
    val count get() = recorded?.count ?: 1
    /** Stable across renumbering: the two pieces and the place they meet. */
    val stableKey get() = listOf(a, b).sortedWith(compareBy({ it.first.ordinal }, { it.second })).joinToString("|") { "${it.first.name}:${it.second}" } + "@" + "%.0f,%.0f,%.0f".format(java.util.Locale.ROOT, at.x, at.y, at.z)
}

data class CutListResult(val pieces: List<PieceCut>, val connections: List<Connection>)

/** The local frame of a bar like [MemberBuilder] builds it: u along, v the height side, w the width side. */
data class BarFrame(val u: Vec3, val v: Vec3, val w: Vec3)

fun barFrame(a: Vec3, b: Vec3, up: Vec3, turnDeg: Double): BarFrame {
    val u = (b - a).normalized()
    var v = up - u * (up dot u)
    if (v.length < 1e-6) v = (if (abs(u.x) < 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).let { it - u * (it dot u) }
    v = v.normalized()
    var w = u cross v
    if (turnDeg != 0.0) {
        val t = Math.toRadians(turnDeg); val c = kotlin.math.cos(t); val s = kotlin.math.sin(t)
        val w2 = w * c + v * s; v = v * c - w * s; w = w2
    }
    return BarFrame(u, v, w)
}

object CutList {
    private const val TOL = 3.0

    private fun halfW(p: MemberProfile) = if (p.kind == ProfileKind.DRAWN) p.outline.maxOfOrNull { abs(it.x) } ?: 0.0 else p.widthMm / 2
    private fun halfH(p: MemberProfile) = if (p.kind == ProfileKind.DRAWN) p.outline.maxOfOrNull { abs(it.y) } ?: 0.0 else if (p.kind.round) p.widthMm / 2 else p.heightMm / 2
    fun halfMax(p: MemberProfile) = max(halfW(p), halfH(p))

    /** Corners of the cross-section as offsets from the axis (a ring for round profiles). */
    private fun corners(bar: CutBar, f: BarFrame): List<Vec3> = when {
        bar.profile.kind == ProfileKind.DRAWN -> bar.profile.outline.map { f.w * it.x + f.v * it.y }
        bar.profile.kind.round -> (0 until 24).map { i -> val t = 2 * PI * i / 24; f.w * (bar.profile.widthMm / 2 * kotlin.math.cos(t)) + f.v * (bar.profile.widthMm / 2 * kotlin.math.sin(t)) }
        else -> listOf(-1.0 to -1.0, 1.0 to -1.0, 1.0 to 1.0, -1.0 to 1.0).map { (x, y) -> f.w * (x * bar.profile.widthMm / 2) + f.v * (y * bar.profile.heightMm / 2) }
    }

    private class End(val bar: CutBar, val atStart: Boolean) {
        val at: Vec3 = if (atStart) bar.points.first() else bar.points.last()
        /** Pointing from this end into the piece. */
        val inward: Vec3 = (if (atStart) bar.points[1] - bar.points[0] else bar.points[bar.points.size - 2] - bar.points.last()).normalized()
        fun frame() = if (atStart) barFrame(bar.points[0], bar.points[1], bar.up, bar.turnDeg) else barFrame(bar.points[bar.points.size - 2], bar.points.last(), bar.up, bar.turnDeg)
    }

    /** Closest point on a bar's centre line to [p]: the point, its segment's direction and whether it is near an end. */
    private class Near(val point: Vec3, val dir: Vec3, val frame: BarFrame, val distance: Double, val nearEnd: Boolean)
    private fun nearest(bar: CutBar, p: Vec3): Near {
        var best: Near? = null
        val total = bar.points.zipWithNext().sumOf { (a, b) -> (b - a).length }
        var walked = 0.0
        bar.points.zipWithNext().forEach { (a, b) ->
            val d = b - a; val len = d.length
            if (len < 1e-9) return@forEach
            val u = d * (1 / len)
            val t = ((p - a) dot u).coerceIn(0.0, len)
            val q = a + u * t; val dist = (p - q).length
            val along = walked + t
            if (best == null || dist < best!!.distance) best = Near(q, u, barFrame(a, b, bar.up, bar.turnDeg), dist, along < TOL || along > total - TOL)
            walked += len
        }
        return best ?: Near(bar.points.first(), Vec3(1.0, 0.0, 0.0), barFrame(bar.points.first(), bar.points.first() + Vec3(1.0, 0.0, 0.0), bar.up, 0.0), Double.MAX_VALUE, true)
    }

    /** The face of [bar] (near [near]) that faces a piece running away along [inward], as normal and half size. */
    private fun face(bar: CutBar, near: Near, inward: Vec3, from: Vec3): Pair<Vec3, Double> {
        if (bar.profile.kind.round) {
            var n = inward - near.dir * (inward dot near.dir)
            if (n.length < 1e-6) n = (from - near.point).let { it - near.dir * (it dot near.dir) }
            return n.normalized() to bar.profile.widthMm / 2
        }
        val f = near.frame
        return listOf(f.w to halfW(bar.profile), f.w * -1.0 to halfW(bar.profile), f.v to halfH(bar.profile), f.v * -1.0 to halfH(bar.profile)).maxBy { it.first dot inward }
    }

    /** On which side of a piece's profile a cut with normal [n] slopes. */
    private fun faceOf(bar: CutBar, f: BarFrame, n: Vec3): String {
        if (bar.profile.kind.round) return ""
        val d = n - f.u * (n dot f.u)
        if (d.length < 0.02) return ""
        val dn = d.normalized()
        val onW = dn dot f.w; val onH = dn dot f.v
        if (abs(onW) > 0.985) return "W"
        if (abs(onH) > 0.985) return "H"
        return "compound, turned ${"%.0f".format(java.util.Locale.ROOT, Math.toDegrees(atan2(abs(onH), abs(onW))))}° from W"
    }

    /** Cut through [end] with the plane of normal [n] (u·n > 0) passing [shift] millimeters in from the end. */
    private fun cutBy(end: End, kind: CutKind, n0: Vec3, adjust: Double, partner: Pair<ModelPlane, Int>?): EndCut {
        val f = end.frame(); val u = end.inward
        val n = (if ((n0 dot u) < 0) n0 * -1.0 else n0).normalized()
        val un = (n dot u).coerceIn(1e-3, 1.0)
        val angle = Math.toDegrees(asin(un))
        val ext = corners(end.bar, f).map { r -> (r dot n) / un }
        return EndCut(kind, angle, faceOf(end.bar, f, n), partner, adjust, ext.maxOrNull()?.coerceAtLeast(0.0) ?: 0.0, ext.minOrNull()?.coerceAtMost(0.0) ?: 0.0)
    }

    private fun square(end: End, kind: CutKind = CutKind.SQUARE, adjust: Double = 0.0, partner: Pair<ModelPlane, Int>? = null) =
        EndCut(kind, 90.0, "", partner, adjust, 0.0, 0.0)

    private class Found(val cut: EndCut, val partner: CutBar?, val kind: String, val angle: Double?, val at: Vec3)

    fun compute(bars: List<CutBar>, joins: List<RecordedJoin>): CutListResult {
        val usable = bars.filter { it.points.size >= 2 && (it.points.last() - it.points.first()).length > 1e-6 }
        val found = HashMap<Pair<Pair<ModelPlane, Int>, Boolean>, Found>()
        fun recordedBetween(a: CutBar, b: CutBar) = joins.firstOrNull { (it.a == a.key && it.b == b.key) || (it.a == b.key && it.b == a.key) }
        for (bar in usable) for (atStart in listOf(true, false)) {
            val end = End(bar, atStart)
            val u = end.inward
            val ending = mutableListOf<Pair<CutBar, Vec3>>() // bar, its inward direction at this point
            val passing = mutableListOf<Pair<CutBar, Near>>()
            for (other in usable) {
                if (other === bar) continue
                val tol = max(TOL, 0.25 * min(halfMax(bar.profile), halfMax(other.profile)))
                val oEnd = listOf(End(other, true), End(other, false)).firstOrNull { (it.at - end.at).length < tol }
                if (oEnd != null) { ending += other to oEnd.inward; continue }
                val near = nearest(other, end.at)
                val reach = halfMax(other.profile) + tol
                val touching = near.distance < tol || (near.distance <= reach && ((end.at - near.point) dot u) >= -tol) ||
                    (recordedBetween(bar, other) != null && near.distance <= reach + halfMax(bar.profile))
                if (touching && !near.nearEnd && abs(near.dir dot u) < 0.985) passing += other to near
            }
            fun angleBetween(a: Vec3, b: Vec3) = Math.toDegrees(kotlin.math.acos((a dot b).coerceIn(-1.0, 1.0)))
            val result: Found? = run {
                // One broken line of one material: a mitred corner.
                ending.firstOrNull { (o, _) -> bar.pathKey != null && o.pathKey == bar.pathKey }?.let { (o, ou) ->
                    val between = angleBetween(u, ou)
                    if (between > 179.5) return@run Found(square(end, partner = o.key), o, "In line", 180.0, end.at)
                    return@run Found(cutBy(end, CutKind.MITER, u - ou, 0.0, o.key), o, "Miter", between, end.at)
                }
                // Continuing another bar in a straight line: a square splice.
                ending.firstOrNull { (_, ou) -> angleBetween(u, ou) > 177.0 }?.let { (o, _) -> return@run Found(square(end, partner = o.key), o, "In line", 180.0, end.at) }
                fun butt(q: CutBar, near: Near): Found {
                    val (n, h) = face(q, near, u, end.at)
                    val un = (n dot u).coerceAtLeast(0.05)
                    val gap = ((end.at - near.point) dot n) - h
                    val adjust = (gap / un).coerceAtLeast(-0.45 * (bar.points.zipWithNext().sumOf { (a, b) -> (b - a).length }))
                    val between = angleBetween(u, near.dir).let { if (it > 90) 180 - it else it }
                    val kind = if (q.profile.kind.round) CutKind.COPE else CutKind.BUTT
                    return Found(cutBy(end, kind, n, adjust, q.key), q, if (kind == CutKind.COPE) "Cope" else "Butt", between, end.at + u * (-adjust))
                }
                passing.minByOrNull { it.second.distance }?.let { (q, near) -> return@run butt(q, near) }
                if (ending.isEmpty()) return@run null
                // A corner: a run through this point stays whole and the rest stop at it; else the oldest runs through.
                val run = ending.firstOrNull { (a, au) -> ending.any { (b, bu) -> b !== a && angleBetween(au, bu) > 177.0 } }
                val through = run?.first ?: (ending.map { it.first } + bar).minBy { it.order }
                if (through === bar) {
                    val (o, ou) = ending.maxBy { (_, ou) -> 1 - abs(ou dot u) }
                    val near = nearest(o, end.at)
                    val (n, h) = face(o, Near(near.point, ou, End(o, (o.points.first() - end.at).length < (o.points.last() - end.at).length).frame(), 0.0, false), u * -1.0, end.at)
                    val un = (n dot (u * -1.0)).coerceAtLeast(0.05)
                    return@run Found(square(end, CutKind.THROUGH, h / un, o.key), o, "Corner", angleBetween(u, ou).let { if (it > 90) 180 - it else it }, end.at)
                }
                val near = nearest(through, end.at)
                val dir = ending.firstOrNull { it.first === through }?.second ?: near.dir
                butt(through, Near(near.point, dir, near.frame, 0.0, false)).let { Found(it.cut, it.partner, "Corner", it.angle, it.at) }
            }
            if (result != null) found[bar.key to atStart] = result
        }
        val pieces = usable.map { bar ->
            val length = bar.points.zipWithNext().sumOf { (a, b) -> (b - a).length }
            PieceCut(bar, length, found[bar.key to true]?.cut ?: square(End(bar, true)), found[bar.key to false]?.cut ?: square(End(bar, false)))
        }
        // One join per pair of pieces meeting at one place; a recorded join takes the fastening.
        data class Raw(val a: CutBar, val b: CutBar, val at: Vec3, val kind: String, val angle: Double?)
        val raws = mutableListOf<Raw>()
        usable.forEach { bar -> listOf(true, false).forEach { s ->
            val f = found[bar.key to s] ?: return@forEach
            val p = f.partner ?: return@forEach
            val r = max(halfMax(bar.profile), halfMax(p.profile)) * 2 + TOL
            if (raws.none { (it.a === p && it.b === bar || it.a === bar && it.b === p) && (it.at - f.at).length < r }) raws += Raw(bar, p, f.at, f.kind, f.angle)
        } }
        val used = HashSet<Int>()
        val byKey = usable.associateBy { it.key }
        val connections = raws.map { raw ->
            val rec = joins.filter { it.id !in used && ((it.a == raw.a.key && it.b == raw.b.key) || (it.a == raw.b.key && it.b == raw.a.key)) }.minByOrNull { (it.at - raw.at).length }
            rec?.let { used += it.id }
            Triple(raw, rec, rec?.at ?: raw.at)
        }.map { (raw, rec, at) ->
            val auto = when {
                raw.a.pathKey != null && raw.a.pathKey == raw.b.pathKey -> "Weld"
                raw.a.weldGroup != null && raw.a.weldGroup == raw.b.weldGroup -> "Weld"
                else -> "Not set"
            }
            Connection(0, raw.a.key, raw.b.key, at, raw.kind, raw.angle, rec, auto, max(halfMax(raw.a.profile), halfMax(raw.b.profile)))
        } + joins.filter { it.id !in used }.map { j ->
            // Recorded where the geometry finds no meeting end: pieces crossing or lying on each other.
            val r = listOfNotNull(byKey[j.a], byKey[j.b]).maxOfOrNull { halfMax(it.profile) } ?: 10.0
            val angle = if (byKey[j.a] != null && byKey[j.b] != null) {
                val da = nearest(byKey[j.a]!!, j.at).dir; val db = nearest(byKey[j.b]!!, j.at).dir
                Math.toDegrees(kotlin.math.acos(abs(da dot db).coerceIn(0.0, 1.0)))
            } else null
            Connection(0, j.a, j.b, j.at, "Cross / lap", angle, j, "", r)
        }
        val order = usable.withIndex().associate { it.value.key to it.index }
        return CutListResult(pieces, connections.sortedWith(compareBy({ min(order[it.a] ?: 9999, order[it.b] ?: 9999) }, { max(order[it.a] ?: 9999, order[it.b] ?: 9999) })).mapIndexed { i, c -> c.copy(number = i + 1) })
    }

    /** Rough weld seam length round the end of a piece cut at [angleDeg] off its axis. */
    fun weldSeamMm(profile: MemberProfile, angleDeg: Double): Double {
        val perimeter = when {
            profile.kind == ProfileKind.DRAWN -> profile.outline.zipWithNext().sumOf { (a, b) -> kotlin.math.hypot(b.x - a.x, b.y - a.y) }
            profile.kind.round -> PI * profile.widthMm
            else -> 2 * (profile.widthMm + profile.heightMm)
        }
        val s = kotlin.math.sin(Math.toRadians(angleDeg.coerceIn(5.0, 90.0)))
        return perimeter * (1 + 1 / s) / 2
    }
}
