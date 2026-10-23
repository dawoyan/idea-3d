package com.davoyans.idea3d.core

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writers for printable geometry. All coordinates are millimeters in model space. */
object ModelExport {
    /** Binary STL. STL has no unit field or standard color, so only geometry is written. */
    fun stl(meshes: List<Mesh>, header: String = "Idea 3D binary STL (millimeters)"): ByteArray {
        val count = meshes.sumOf { it.triangles.size }
        val buffer = ByteBuffer.allocate(84 + 50 * count).order(ByteOrder.LITTLE_ENDIAN)
        val title = header.toByteArray(Charsets.US_ASCII).copyOf(80)
        buffer.put(title); buffer.putInt(count)
        meshes.forEach { mesh ->
            mesh.triangles.forEach { t ->
                val n = mesh.normal(t)
                buffer.putFloat(n.x.toFloat()); buffer.putFloat(n.y.toFloat()); buffer.putFloat(n.z.toFloat())
                listOf(t.a, t.b, t.c).forEach { i -> val v = mesh.vertices[i]; buffer.putFloat(v.x.toFloat()); buffer.putFloat(v.y.toFloat()); buffer.putFloat(v.z.toFloat()) }
                buffer.putShort(0)
            }
        }
        return buffer.array()
    }

    /** Minimal 3MF package (core spec) with millimeter units and one base material per object color. */
    fun threeMf(meshes: List<Mesh>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, text: String) { zip.putNextEntry(ZipEntry(name)); zip.write(text.toByteArray(Charsets.UTF_8)); zip.closeEntry() }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="model" ContentType="application/vnd.ms-package.3dmanufacturing-3dmodel+xml"/>
</Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Target="/3D/3dmodel.model" Id="rel0" Type="http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"/>
</Relationships>""")
            entry("3D/3dmodel.model", modelXml(meshes))
        }
        return out.toByteArray()
    }

    fun modelXml(meshes: List<Mesh>): String {
        fun num(v: Double) = String.format(Locale.US, "%.5f", v).trimEnd('0').trimEnd('.').let { if (it == "-0") "0" else it }
        fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
        fun color(argb: Int) = String.format(Locale.US, "#%02X%02X%02X%02X", (argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF, (argb ushr 24) and 0xFF)
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<model unit="millimeter" xml:lang="en-US" xmlns="http://schemas.microsoft.com/3dmanufacturing/core/2015/02">
  <metadata name="Application">Idea 3D</metadata>
  <resources>
    <basematerials id="1">
""")
        meshes.forEach { sb.append("      <base name=\"${escape(it.name)}\" displaycolor=\"${color(it.argb)}\"/>\n") }
        sb.append("    </basematerials>\n")
        meshes.forEachIndexed { index, mesh ->
            sb.append("    <object id=\"${index + 2}\" type=\"model\" name=\"${escape(mesh.name)}\" pid=\"1\" pindex=\"$index\">\n      <mesh>\n        <vertices>\n")
            mesh.vertices.forEach { v -> sb.append("          <vertex x=\"${num(v.x)}\" y=\"${num(v.y)}\" z=\"${num(v.z)}\"/>\n") }
            sb.append("        </vertices>\n        <triangles>\n")
            mesh.triangles.forEach { t -> sb.append("          <triangle v1=\"${t.a}\" v2=\"${t.b}\" v3=\"${t.c}\"/>\n") }
            sb.append("        </triangles>\n      </mesh>\n    </object>\n")
        }
        sb.append("  </resources>\n  <build>\n")
        meshes.indices.forEach { sb.append("    <item objectid=\"${it + 2}\"/>\n") }
        sb.append("  </build>\n</model>\n")
        return sb.toString()
    }
}
