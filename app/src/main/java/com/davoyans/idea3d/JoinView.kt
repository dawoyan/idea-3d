package com.davoyans.idea3d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.davoyans.idea3d.core.Vec3
import java.io.ByteArrayOutputStream
import java.text.DecimalFormat
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** A piece as the join view draws it: its centre line, half its thickness and its colour. */
internal data class JoinViewBar(val key: String, val label: String, val points: List<Vec3>, val halfMm: Double, val argb: Int, val info: String)
/** A numbered join, drawn solid on top of the see-through pieces. */
internal data class JoinViewJoin(val key: String, val number: Int, val at: Vec3, val radiusMm: Double, val info: String)
internal data class JoinScene(val bars: List<JoinViewBar>, val joins: List<JoinViewJoin>)

/** What the join view shows, kept per project so the export draws the same picture. */
internal class JoinViewState {
    var open by mutableStateOf(false)
    var yaw by mutableStateOf(35f)
    var pitch by mutableStateOf(25f)
    var zoom by mutableStateOf(1f)
    var panX by mutableStateOf(0f)
    var panY by mutableStateOf(0f)
    var showJoins by mutableStateOf(true)
    var showNumbers by mutableStateOf(true)
    var showPieces by mutableStateOf(true)
    var showLists by mutableStateOf(true)
    var hiddenJoins by mutableStateOf(emptySet<String>())
    var hiddenJoinLabels by mutableStateOf(emptySet<String>())
    var hiddenPieceLabels by mutableStateOf(emptySet<String>())
    var picked by mutableStateOf<String?>(null)
    private var projectId: String? = null

    fun joinShown(key: String) = showJoins && key !in hiddenJoins
    fun joinNumberShown(key: String) = showNumbers && key !in hiddenJoinLabels
    fun pieceLabelShown(key: String) = showPieces && key !in hiddenPieceLabels
    /** A frozen copy for drawing the export off the main thread. */
    fun copyForExport() = JoinViewState().also { c ->
        c.yaw = yaw; c.pitch = pitch; c.zoom = zoom; c.showJoins = showJoins; c.showNumbers = showNumbers; c.showPieces = showPieces
        c.hiddenJoins = hiddenJoins; c.hiddenJoinLabels = hiddenJoinLabels; c.hiddenPieceLabels = hiddenPieceLabels
    }
    fun resetView() { yaw = 35f; pitch = 25f; zoom = 1f; panX = 0f; panY = 0f }

    private fun prefs(context: Context) = context.getSharedPreferences("join-view", Context.MODE_PRIVATE)
    fun load(context: Context, id: String?) {
        if (id == projectId) return
        projectId = id; picked = null
        val p = prefs(context); val k = id ?: "none"
        yaw = p.getFloat("$k.yaw", 35f); pitch = p.getFloat("$k.pitch", 25f); zoom = p.getFloat("$k.zoom", 1f); panX = 0f; panY = 0f
        showJoins = p.getBoolean("$k.joins", true); showNumbers = p.getBoolean("$k.numbers", true); showPieces = p.getBoolean("$k.pieces", true)
        hiddenJoins = p.getStringSet("$k.hiddenJoins", emptySet())!!.toSet()
        hiddenJoinLabels = p.getStringSet("$k.hiddenJoinLabels", emptySet())!!.toSet()
        hiddenPieceLabels = p.getStringSet("$k.hiddenPieceLabels", emptySet())!!.toSet()
    }
    fun save(context: Context) {
        val k = projectId ?: "none"
        prefs(context).edit().putFloat("$k.yaw", yaw).putFloat("$k.pitch", pitch).putFloat("$k.zoom", zoom)
            .putBoolean("$k.joins", showJoins).putBoolean("$k.numbers", showNumbers).putBoolean("$k.pieces", showPieces)
            .putStringSet("$k.hiddenJoins", hiddenJoins).putStringSet("$k.hiddenJoinLabels", hiddenJoinLabels).putStringSet("$k.hiddenPieceLabels", hiddenPieceLabels).apply()
    }
}
internal val joinView = JoinViewState()

/** Turns model millimeters into screen pixels for one camera, fitted to the whole scene. */
internal class JoinProjection(scene: JoinScene, private val yaw: Float, private val pitch: Float, zoom: Float, panX: Float, panY: Float, width: Int, height: Int, margin: Float) {
    private val cy = cos(Math.toRadians(yaw.toDouble())); private val sy = sin(Math.toRadians(yaw.toDouble()))
    private val cp = cos(Math.toRadians(pitch.toDouble())); private val sp = sin(Math.toRadians(pitch.toDouble()))
    /** Screen x, screen y (down) and depth (larger = nearer) before fitting. */
    private fun raw(v: Vec3): Triple<Double, Double, Double> {
        val x = v.x * cy + v.z * sy; val z = -v.x * sy + v.z * cy
        val y = v.y * cp - z * sp; val d = v.y * sp + z * cp
        return Triple(x, -y, d)
    }
    val scale: Double; private val ox: Double; private val oy: Double
    init {
        val pts = scene.bars.flatMap { it.points } + scene.joins.map { it.at }
        val r = pts.map(::raw)
        val minX = r.minOfOrNull { it.first } ?: 0.0; val maxX = r.maxOfOrNull { it.first } ?: 1.0
        val minY = r.minOfOrNull { it.second } ?: 0.0; val maxY = r.maxOfOrNull { it.second } ?: 1.0
        scale = min((width - 2 * margin) / (maxX - minX).coerceAtLeast(1.0), (height - 2 * margin) / (maxY - minY).coerceAtLeast(1.0)) * zoom
        ox = width / 2.0 + panX - (minX + maxX) / 2 * scale; oy = height / 2.0 + panY - (minY + maxY) / 2 * scale
    }
    fun screen(v: Vec3): Triple<Float, Float, Double> = raw(v).let { (x, y, d) -> Triple((ox + x * scale).toFloat(), (oy + y * scale).toFloat(), d) }
}

/** Draws the join view: pieces at half opacity, joins solid with their numbers, piece labels on top. */
internal fun drawJoinScene(canvas: android.graphics.Canvas, width: Int, height: Int, scene: JoinScene, state: JoinViewState, density: Float, picked: String? = null) {
    val proj = JoinProjection(scene, state.yaw, state.pitch, state.zoom, state.panX, state.panY, width, height, 40f * density)
    val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.BUTT; strokeJoin = Paint.Join.ROUND }
    val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f * density; color = 0x66202020 }
    // Far pieces first, so nearer ones lie over them.
    scene.bars.sortedBy { b -> b.points.sumOf { proj.screen(it).third } / b.points.size }.forEach { b ->
        bar.color = (b.argb and 0x00ffffff) or 0x80000000.toInt()
        bar.strokeWidth = max(2f * density, (2 * b.halfMm * proj.scale).toFloat())
        val path = android.graphics.Path()
        b.points.forEachIndexed { i, p -> val (x, y) = proj.screen(p); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
        canvas.drawPath(path, bar); canvas.drawPath(path, edge)
    }
    val solid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFD9480F.toInt() }
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = android.graphics.Color.WHITE; strokeWidth = 2f * density }
    val pickRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFF1463B8.toInt(); strokeWidth = 3f * density }
    val number = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    scene.joins.filter { state.joinShown(it.key) }.forEach { j ->
        val (x, y) = proj.screen(j.at)
        val r = max(9f * density, (j.radiusMm * 1.15 * proj.scale).toFloat()).coerceAtMost(40f * density)
        canvas.drawCircle(x, y, r, solid); canvas.drawCircle(x, y, r, ring)
        if (j.key == picked) canvas.drawCircle(x, y, r + 4 * density, pickRing)
        if (state.joinNumberShown(j.key)) {
            number.textSize = (r * 0.95f).coerceIn(9f * density, 18f * density)
            canvas.drawText("${j.number}", x, y + number.textSize * 0.36f, number)
        }
    }
    val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F2A28.toInt(); textSize = 12f * density; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6FFFFFF.toInt() }
    scene.bars.filter { state.pieceLabelShown(it.key) }.forEach { b ->
        val mid = b.points[b.points.size / 2].let { if (b.points.size == 2) (b.points[0] + b.points[1]) * 0.5 else it }
        val (x, y) = proj.screen(mid)
        val w = label.measureText(b.label) / 2 + 4 * density
        canvas.drawRoundRect(RectF(x - w, y - 10 * density, x + w, y + 5 * density), 4 * density, 4 * density, back)
        canvas.drawText(b.label, x, y + 1 * density, label)
    }
}

/** The join view as a picture for the export, drawn with the same camera and the same shown/hidden choices. */
internal fun joinScenePng(scene: JoinScene, state: JoinViewState, title: String): ByteArray {
    val width = 1800; val height = 1300
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(image)
    canvas.drawColor(android.graphics.Color.WHITE)
    canvas.save(); canvas.translate(0f, 70f)
    drawJoinScene(canvas, width, height - 70, scene, state, 2.4f)
    canvas.restore()
    canvas.drawText(title, 40f, 52f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF203530.toInt(); textSize = 38f; typeface = android.graphics.Typeface.DEFAULT_BOLD })
    return ByteArrayOutputStream().use { out -> image.compress(Bitmap.CompressFormat.PNG, 100, out); out.toByteArray() }
}

internal fun joinSceneSvg(scene: JoinScene, state: JoinViewState, title: String, esc: (String) -> String): String {
    val width = 1800; val height = 1300; val d = 2.4f
    val proj = JoinProjection(scene, state.yaw, state.pitch, state.zoom, state.panX, state.panY, width, height - 70, 40f * d)
    fun n(v: Float) = DecimalFormat("0.#").format(v)
    return buildString {
        append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $width $height\"><rect width=\"$width\" height=\"$height\" fill=\"white\"/>")
        append("<text x=\"40\" y=\"52\" font-family=\"sans-serif\" font-size=\"38\" font-weight=\"bold\" fill=\"#203530\">${esc(title)}</text><g transform=\"translate(0 70)\">")
        scene.bars.sortedBy { b -> b.points.sumOf { proj.screen(it).third } / b.points.size }.forEach { b ->
            val pts = b.points.joinToString(" L ") { val (x, y) = proj.screen(it); "${n(x)} ${n(y)}" }
            val w = max(2f * d, (2 * b.halfMm * proj.scale).toFloat())
            append("<path d=\"M $pts\" fill=\"none\" stroke=\"#%06X\" stroke-opacity=\".5\" stroke-width=\"${n(w)}\" stroke-linejoin=\"round\"/>".format(b.argb and 0xffffff))
        }
        scene.joins.filter { state.joinShown(it.key) }.forEach { j ->
            val (x, y) = proj.screen(j.at)
            val r = max(9f * d, (j.radiusMm * 1.15 * proj.scale).toFloat()).coerceAtMost(40f * d)
            append("<circle cx=\"${n(x)}\" cy=\"${n(y)}\" r=\"${n(r)}\" fill=\"#D9480F\" stroke=\"white\" stroke-width=\"${n(2 * d)}\"/>")
            if (state.joinNumberShown(j.key)) append("<text x=\"${n(x)}\" y=\"${n(y + r * 0.34f)}\" text-anchor=\"middle\" font-family=\"sans-serif\" font-weight=\"bold\" font-size=\"${n((r * 0.95f).coerceIn(9f * d, 18f * d))}\" fill=\"white\">${j.number}</text>")
        }
        scene.bars.filter { state.pieceLabelShown(it.key) }.forEach { b ->
            val mid = if (b.points.size == 2) (b.points[0] + b.points[1]) * 0.5 else b.points[b.points.size / 2]
            val (x, y) = proj.screen(mid)
            append("<text x=\"${n(x)}\" y=\"${n(y)}\" text-anchor=\"middle\" font-family=\"sans-serif\" font-weight=\"bold\" font-size=\"${n(12 * d)}\" fill=\"#1F2A28\" stroke=\"white\" stroke-width=\"${n(3 * d)}\" paint-order=\"stroke\">${esc(b.label)}</text>")
        }
        append("</g></svg>")
    }
}

/** The join view: the model with see-through pieces, solid numbered joins, and lists to show or hide each one. */
@Composable internal fun JoinViewDialog(scene: JoinScene, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val s = joinView
    val density = LocalDensity.current.density
    fun close() { s.save(context); onDismiss() }
    Dialog(onDismissRequest = ::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFFF4F7F6)) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Join view", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                    Text("  ${scene.bars.size} pieces · ${scene.joins.size} joins", fontSize = 13.sp, color = Color(0xFF53615D))
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { s.resetView() }) { Text("Reset view") }
                    TextButton(onClick = ::close) { Text("Close") }
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(s.showJoins, { s.showJoins = !s.showJoins }, { Text("● Joins") })
                    FilterChip(s.showNumbers, { s.showNumbers = !s.showNumbers }, { Text("№ Numbers") })
                    FilterChip(s.showPieces, { s.showPieces = !s.showPieces }, { Text("P Piece labels") })
                    FilterChip(s.showLists, { s.showLists = !s.showLists }, { Text("☰ Lists") })
                }
                Box(Modifier.fillMaxWidth().weight(1f).background(Color.White)
                    .pointerInput(scene) { detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom != 1f) s.zoom = (s.zoom * zoom).coerceIn(0.2f, 12f)
                        else { s.yaw += pan.x * 0.35f; s.pitch = (s.pitch + pan.y * 0.35f).coerceIn(-89f, 89f) }
                    } }
                    .pointerInput(scene) { detectTapGestures { tap ->
                        val proj = JoinProjection(scene, s.yaw, s.pitch, s.zoom, s.panX, s.panY, size.width, size.height, 40f * density)
                        val hit = scene.joins.filter { s.joinShown(it.key) }.minByOrNull { j -> val (x, y) = proj.screen(j.at); kotlin.math.hypot(x - tap.x, y - tap.y) }
                            ?.takeIf { j -> val (x, y) = proj.screen(j.at); kotlin.math.hypot(x - tap.x, y - tap.y) < 32 * density }
                        s.picked = if (hit?.key == s.picked) null else hit?.key
                    } }) {
                    Canvas(Modifier.fillMaxSize()) {
                        // Read the state so the picture redraws when it changes.
                        s.yaw; s.pitch; s.zoom; s.showJoins; s.showNumbers; s.showPieces; s.hiddenJoins; s.hiddenJoinLabels; s.hiddenPieceLabels
                        drawIntoCanvas { drawJoinScene(it.nativeCanvas, size.width.toInt(), size.height.toInt(), scene, s, density, s.picked) }
                    }
                    if (scene.joins.isEmpty() && scene.bars.isEmpty()) Text("No bars or pipes with a material yet.", Modifier.align(Alignment.Center), color = Color(0xFF53615D))
                    s.picked?.let { key -> scene.joins.firstOrNull { it.key == key } }?.let { j ->
                        Surface(Modifier.align(Alignment.BottomStart).padding(8.dp), color = Color(0xF0FFFFFF), shadowElevation = 2.dp) {
                            Text("${j.number}. ${j.info}", Modifier.padding(8.dp), fontSize = 13.sp)
                        }
                    }
                    Text("Drag to turn · pinch to zoom · tap a join", Modifier.align(Alignment.TopEnd).padding(6.dp), fontSize = 11.sp, color = Color(0xFF7A8784))
                }
                if (s.showLists) {
                    HorizontalDivider()
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp).padding(horizontal = 8.dp)) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Joins", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("show  №", fontSize = 11.sp, color = Color(0xFF53615D))
                                TextButton(onClick = { s.hiddenJoins = emptySet(); s.hiddenJoinLabels = emptySet() }) { Text("All") }
                                TextButton(onClick = { s.hiddenJoins = scene.joins.map { it.key }.toSet() }) { Text("None") }
                            }
                        }
                        items(scene.joins, key = { "j" + it.key }) { j ->
                            Row(Modifier.fillMaxWidth().clickable { s.picked = if (s.picked == j.key) null else j.key }.background(if (s.picked == j.key) Color(0xFFE1ECF8) else Color.Transparent), verticalAlignment = Alignment.CenterVertically) {
                                Text("${j.number}", fontWeight = FontWeight.Bold, color = Color(0xFFD9480F), modifier = Modifier.width(30.dp))
                                Text(j.info, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Checkbox(j.key !in s.hiddenJoins, { on -> s.hiddenJoins = if (on) s.hiddenJoins - j.key else s.hiddenJoins + j.key })
                                Checkbox(j.key !in s.hiddenJoinLabels, { on -> s.hiddenJoinLabels = if (on) s.hiddenJoinLabels - j.key else s.hiddenJoinLabels + j.key })
                            }
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pieces", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(top = 6.dp))
                                Text("label", fontSize = 11.sp, color = Color(0xFF53615D))
                                TextButton(onClick = { s.hiddenPieceLabels = emptySet() }) { Text("All") }
                                TextButton(onClick = { s.hiddenPieceLabels = scene.bars.map { it.key }.toSet() }) { Text("None") }
                            }
                        }
                        items(scene.bars, key = { "p" + it.key }) { b ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(b.label, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
                                Text(b.info, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Checkbox(b.key !in s.hiddenPieceLabels, { on -> s.hiddenPieceLabels = if (on) s.hiddenPieceLabels - b.key else s.hiddenPieceLabels + b.key })
                            }
                        }
                    }
                }
            }
        }
    }
}
