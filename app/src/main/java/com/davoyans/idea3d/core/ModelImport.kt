package com.davoyans.idea3d.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream
import kotlin.math.cos
import kotlin.math.sin

/**
 * Reads 3D models downloaded from the web: STL (binary or text), OBJ and 3MF. The result is one mesh in
 * millimeters, centered on its bounding box, with equal corners welded.
 */
object ModelImport {
    /** Larger models are refused; a phone can't show or combine them smoothly. */
    const val MAX_TRIANGLES = 400_000

    fun read(fileName: String, bytes: ByteArray): Mesh {
        val name = fileName.substringBeforeLast('.').ifBlank { "Model" }
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val text = { String(bytes, Charsets.UTF_8) }
        val builder = when {
            ext == "3mf" || bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte() -> threeMf(bytes)
            ext == "obj" -> obj(text())
            isBinaryStl(bytes) -> binaryStl(bytes)
            ext == "stl" || text().trimStart().startsWith("solid") -> asciiStl(text())
            else -> obj(text())
        }
        require(builder.triangles.isNotEmpty()) { "No triangles found in $fileName" }
        require(builder.triangles.size <= MAX_TRIANGLES) { "The model has ${builder.triangles.size} triangles; at most $MAX_TRIANGLES can be used" }
        return centered(Mesh(name, builder.vertices, builder.triangles, 0xFF9E9E9E.toInt()))
    }

    /** [mesh] moved so its bounding box is centered on the origin. */
    fun centered(mesh: Mesh): Mesh {
        val (low, high) = mesh.bounds
        val c = (low + high) * 0.5
        return mesh.copy(vertices = mesh.vertices.map { it - c })
    }

    /** [v] turned [deg] degrees about the line through the origin along [axis] (counterclockwise looking against it). */
    fun turn(v: Vec3, axis: Vec3, deg: Double): Vec3 {
        val k = axis.normalized(); val t = Math.toRadians(deg); val c = cos(t); val s = sin(t)
        return v * c + (k cross v) * s + k * ((k dot v) * (1 - c))
    }

    /**
     * Places a centered model: scaled by [scale], turned [rx], [ry] then [rz] degrees about X, Y and Z, then by each
     * of [turns] (axis, degrees) in order, and moved so its center is at [center].
     */
    fun placed(mesh: Mesh, scale: Double, rx: Double, ry: Double, rz: Double, center: Vec3, turns: List<Pair<Vec3, Double>> = emptyList()): Mesh {
        fun turn(v: Vec3, deg: Double, axis: Int): Vec3 {
            if (deg == 0.0) return v
            val t = Math.toRadians(deg); val c = cos(t); val s = sin(t)
            return when (axis) {
                0 -> Vec3(v.x, v.y * c - v.z * s, v.y * s + v.z * c)
                1 -> Vec3(v.x * c + v.z * s, v.y, -v.x * s + v.z * c)
                else -> Vec3(v.x * c - v.y * s, v.x * s + v.y * c, v.z)
            }
        }
        val flip = scale < 0
        val moved = mesh.vertices.map { v -> turns.fold(turn(turn(turn(v * scale, rx, 0), ry, 1), rz, 2)) { p, (axis, deg) -> turn(p, axis, deg) } + center }
        return mesh.copy(vertices = moved, triangles = if (flip) mesh.triangles.map { Triangle(it.a, it.c, it.b) } else mesh.triangles)
    }

    private class Builder {
        val vertices = ArrayList<Vec3>()
        val triangles = ArrayList<Triangle>()
        private val index = HashMap<Triple<Long, Long, Long>, Int>()
        fun vertex(v: Vec3): Int = index.getOrPut(Triple(Math.round(v.x * 1e5), Math.round(v.y * 1e5), Math.round(v.z * 1e5))) { vertices.add(v); vertices.size - 1 }
        fun add(a: Int, b: Int, c: Int) { if (a != b && b != c && a != c) triangles.add(Triangle(a, b, c)) }
    }

    private fun isBinaryStl(bytes: ByteArray): Boolean {
        if (bytes.size < 84) return false
        val count = ByteBuffer.wrap(bytes, 80, 4).order(ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xFFFFFFFFL
        return 84L + count * 50L == bytes.size.toLong()
    }

    private fun binaryStl(bytes: ByteArray): Builder {
        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        data.position(80)
        val count = data.int
        require(count <= MAX_TRIANGLES) { "The model has $count triangles; at most $MAX_TRIANGLES can be used" }
        val b = Builder()
        repeat(count) {
            data.position(data.position() + 12)
            val ids = IntArray(3) { val x = data.float.toDouble(); val y = data.float.toDouble(); val z = data.float.toDouble(); require(x.isFinite() && y.isFinite() && z.isFinite()) { "Invalid coordinate" }; b.vertex(Vec3(x, y, z)) }
            data.short
            b.add(ids[0], ids[1], ids[2])
        }
        return b
    }

    private val number = "[-+]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][-+]?\\d+)?"

    private fun asciiStl(text: String): Builder {
        val b = Builder()
        val corners = Regex("vertex\\s+($number)\\s+($number)\\s+($number)").findAll(text).map { m -> b.vertex(Vec3(m.groupValues[1].toDouble(), m.groupValues[2].toDouble(), m.groupValues[3].toDouble())) }.toList()
        for (i in 0 until corners.size / 3) b.add(corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2])
        return b
    }

    private fun obj(text: String): Builder {
        val b = Builder()
        val ids = ArrayList<Int>()
        text.lineSequence().forEach { raw ->
            val parts = raw.trim().split(Regex("\\s+"))
            when (parts.firstOrNull()) {
                "v" -> if (parts.size >= 4) ids.add(b.vertex(Vec3(parts[1].toDouble(), parts[2].toDouble(), parts[3].toDouble())))
                "f" -> {
                    // Faces may use v, v/vt, v/vt/vn or v//vn, counted from 1 or (negative) from the end.
                    val face = parts.drop(1).mapNotNull { p -> p.substringBefore('/').toIntOrNull()?.let { if (it < 0) ids.size + it else it - 1 } }.filter { it in ids.indices }.map { ids[it] }
                    for (k in 1 until face.size - 1) b.add(face[0], face[k], face[k + 1])
                }
            }
        }
        return b
    }

    private fun threeMf(bytes: ByteArray): Builder {
        val b = Builder()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name.endsWith(".model", ignoreCase = true)) {
                    val out = ByteArrayOutputStream(); zip.copyTo(out)
                    val xml = out.toString("UTF-8")
                    val unit = Regex("<model[^>]*\\sunit=\"(\\w+)\"").find(xml)?.groupValues?.get(1)
                    val mm = when (unit) { "micron" -> 0.001; "centimeter" -> 10.0; "inch" -> 25.4; "foot" -> 304.8; "meter" -> 1000.0; else -> 1.0 }
                    fun attr(tag: String, name: String) = Regex("\\s$name=\"($number)\"").find(tag)?.groupValues?.get(1)
                    Regex("<mesh>(.*?)</mesh>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { mesh ->
                        val ids = Regex("<vertex\\s[^>]*>").findAll(mesh.groupValues[1]).map { v ->
                            b.vertex(Vec3((attr(v.value, "x") ?: "0").toDouble() * mm, (attr(v.value, "y") ?: "0").toDouble() * mm, (attr(v.value, "z") ?: "0").toDouble() * mm))
                        }.toList()
                        Regex("<triangle\\s[^>]*>").findAll(mesh.groupValues[1]).forEach { t ->
                            val v = listOf("v1", "v2", "v3").mapNotNull { attr(t.value, it)?.toIntOrNull() }
                            if (v.size == 3 && v.all { it in ids.indices }) b.add(ids[v[0]], ids[v[1]], ids[v[2]])
                        }
                    }
                }
                zip.closeEntry()
            }
        }
        return b
    }
}
