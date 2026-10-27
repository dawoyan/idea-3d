package com.davoyans.idea3d

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Matrix as AndroidMatrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import android.database.Cursor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.Date
import java.util.Locale
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import com.davoyans.idea3d.core.Calibration
import com.davoyans.idea3d.core.CalibrationAxis
import com.davoyans.idea3d.core.CalibrationReference
import com.davoyans.idea3d.core.CalibrationStatus
import com.davoyans.idea3d.core.LengthUnit
import com.davoyans.idea3d.core.Mesh
import com.davoyans.idea3d.core.ModelPlane
import com.davoyans.idea3d.core.formatLength
import com.davoyans.idea3d.core.parseLengthMm
import com.davoyans.idea3d.core.projectedLengthForFixedTrue
import com.davoyans.idea3d.core.MeshBuilder
import com.davoyans.idea3d.core.MeshException
import com.davoyans.idea3d.core.MemberBuilder
import com.davoyans.idea3d.core.MemberProfile
import com.davoyans.idea3d.core.ProfileKind
import com.davoyans.idea3d.core.Vec2
import com.davoyans.idea3d.core.Csg
import com.davoyans.idea3d.core.MeshIssue
import com.davoyans.idea3d.core.ModelExport
import com.davoyans.idea3d.core.ModelImport
import com.davoyans.idea3d.core.DxfEntity
import com.davoyans.idea3d.core.DxfExport
import com.davoyans.idea3d.core.DxfLayers
import com.davoyans.idea3d.core.DxfLine
import com.davoyans.idea3d.core.DxfSegment
import com.davoyans.idea3d.core.DxfText
import com.davoyans.idea3d.core.OutlinePoint
import com.davoyans.idea3d.core.SolidSpec
import com.davoyans.idea3d.core.SolidCombiner
import com.davoyans.idea3d.core.SolidOperation
import com.davoyans.idea3d.core.Vec3

private enum class Tool { NONE, CALIBRATE, MEASURE, LINES, ADJUST, CROP }
/**
 * An arrow for growing a shape into a 3D object (or changing its depth): [anchor] and [dir] in this view's canvas.
 * [ownView] is the single arrow in the shape's own view (drag up = deeper); otherwise [front] says which side it pulls.
 */
private data class GrowHandle(val key: Pair<ModelPlane, Int>, val front: Boolean, val ownView: Boolean, val anchor: Pt, val dir: Pt)
private data class GrowDrag(val handle: GrowHandle, val start: Pt, val face: Double, val depth: Double, val extrusionId: Int)

private enum class LineDrawing { STRAIGHT, BROKEN, CURVE, CIRCLE, RECTANGLE }

/** Within 4% of a square, a rectangle from [start] to [end] snaps to one; returns the end to use and whether it is square. */
private fun squareEnd(start: Pt, end: Pt): Pair<Pt, Boolean> {
    val w = end.x - start.x; val h = end.y - start.y
    val big = max(abs(w), abs(h)); if (big < 1f) return end to false
    if (abs(abs(w) - abs(h)) > 0.04f * big) return end to false
    val side = (abs(w) + abs(h)) / 2f
    return Pt(start.x + if (w < 0) -side else side, start.y + if (h < 0) -side else side) to true
}
private enum class PointKind { BREAKPOINT, CURVE }
private enum class CropEdge { LEFT, TOP, RIGHT, BOTTOM }
private enum class LengthSource { CALCULATED, MANUAL, MEASURED, UNKNOWN }
private data class Pt(val x: Float, val y: Float)
private data class CropFrame(val left: Float, val top: Float, val right: Float, val bottom: Float)
private data class FeatureSnap(val point: Pt, val found: Boolean)
private data class ScreenLine(val id: Int, val start: Offset, val end: Offset)
private data class MeasureLine(val id: Int, val start: Pt, val end: Pt, val value: Double, val isReference: Boolean, val label: String, val colorIndex: Int = 0, val isScaleReference: Boolean = false, val labelOffset: Float = 0f, val pathId: Int? = null, val segmentIndex: Int = 0, val lengthSource: LengthSource = LengthSource.CALCULATED, val startZMm: Double = 0.0, val endZMm: Double = 0.0, val axis: String = "", val plane: ModelPlane = ModelPlane.FRONT)
private fun MeasureLine.hasDepth() = abs(startZMm) > 0.000001 || abs(endZMm) > 0.000001
private data class GuideLine(val id: Int, val start: Pt, val end: Pt, val manual: Boolean, val pathId: Int? = null, val colorIndex: Int = 0, val strokeWidth: Float = 5f, val curveMid: Pt? = null, val fillTransparency: Int? = null, val groupId: Int? = null, val lengthMm: Double? = null, val shapeWidthMm: Double? = null, val shapeHeightMm: Double? = null, val startZMm: Double = 0.0, val endZMm: Double = 0.0, val startThicknessMm: Double? = null, val endThicknessMm: Double? = null, val plane: ModelPlane = ModelPlane.FRONT, val materialId: String? = null, val materialTurnDeg: Double = 0.0, val midDepthMm: Double? = null, val hole: Boolean = false)
private data class AlignmentReference(val guide: Boolean, val id: Int, val point: Pt, val description: String)
private data class SelectedAlignmentObject(val guideIds: Set<Int>, val lineIds: Set<Int>, val points: List<Pt>)
private data class Extrusion(val id: Int, val guideIds: List<Int>, val origin: Pt, val handle: Pt, val vanishing: Pt, val sun: Pt = vanishing, val depthMm: Double? = null, val positionZMm: Double = 0.0, val plane: ModelPlane = ModelPlane.FRONT)
private enum class JunctionKind { SPLIT_MEASUREMENT, SPLIT_GUIDE, JOIN_MEASUREMENTS, JOIN_GUIDES, JOIN_MEASURE_ENDPOINTS, JOIN_GUIDE_ENDPOINTS, MERGE_SHAPE_GUIDE, CLOSE_GUIDE_PATH, SHAPE_OVERLAP, CUT_SHAPE_LINE }
private data class JunctionAction(val kind: JunctionKind, val firstId: Int, val secondId: Int, val point: Pt)
private data class JunctionArm(val lineId: Int, val endpoint: Pt, val color: Color)
/** Joining two crossing lines on the canvas: the parts (arms) blink until two are chosen to keep. */
private data class JoinPick(val action: JunctionAction, val arms: List<JunctionArm>, val selected: Set<Int> = emptySet(), val keepRemaining: Boolean = false, val paths: List<List<Pt>> = emptyList())
private data class UndoSnapshot(val image: Bitmap?, val lines: List<MeasureLine>, val guides: List<GuideLine>, val guidesDetected: Boolean, val rotation: Float, val unit: String, val tool: Tool, val extrusions: List<Extrusion> = emptyList(), val plane: ModelPlane = ModelPlane.FRONT, val stash: Map<ModelPlane, PlaneGeometry> = emptyMap(), val frames: Map<ModelPlane, PlaneFrame> = emptyMap(), val solidOps: List<SolidOp> = emptyList(), val itemGroups: List<List<Pair<ModelPlane, Int>>> = emptyList(), val models: List<ImportedModel> = emptyList())
/**
 * Where a plane's canvas sits in model space: canvas pixel ([originU], [originV]) is the plane's (0, 0) and the
 * canvas is [width] x [height] pixels. Cropping or growing a canvas moves the origin with the drawing, so the
 * model stays where it is.
 */
private data class PlaneFrame(val originU: Double, val originV: Double, val width: Int, val height: Int)
private fun defaultFrame(plane: ModelPlane, width: Int, height: Int) = when (plane) {
    ModelPlane.FRONT -> PlaneFrame(0.0, height.toDouble(), width, height)
    ModelPlane.TOP -> PlaneFrame(0.0, height / 2.0, width, height)
    ModelPlane.PROFILE -> PlaneFrame(width / 2.0, height.toDouble(), width, height)
}

/** Everything needed to turn canvas pixels of any plane into model millimeters. */
private class ModelSetup(val frames: Map<ModelPlane, PlaneFrame>, val scales: Map<ModelPlane, Double?>, val axisScale: Pair<Double, Double>?, val unit: String) {
    fun mapping(plane: ModelPlane) = frames[plane]?.let { planeMapping(plane, it, scales[plane], unit, if (plane == ModelPlane.FRONT) axisScale else null) }
}

/** One entry in the finder: a line, shape or object, named by its points (A–B–C…), with a small drawing. */
private data class FinderItem(val plane: ModelPlane, val guideIds: List<Int>, val name: String, val kind: String, val outline: List<List<Pt>>, val bounds: FloatArray)

/** A 3D cut or join between two objects, each named by its drawing plane and object id. A kept cut tool stays in the model. */
private data class SolidOp(val target: Pair<ModelPlane, Int>, val tool: Pair<ModelPlane, Int>, val join: Boolean, val keepTool: Boolean = false)

/** Geometry of a drawing plane that is not the one being edited; items keep their [ModelPlane] tag. */
private data class PlaneGeometry(val lines: List<MeasureLine> = emptyList(), val guides: List<GuideLine> = emptyList(), val extrusions: List<Extrusion> = emptyList())

/** The same drawing moved by (dx, dy) canvas pixels. */
private fun PlaneGeometry.translated(dx: Float, dy: Float): PlaneGeometry {
    fun moved(point: Pt) = Pt(point.x + dx, point.y + dy)
    return PlaneGeometry(lines.map { it.copy(start = moved(it.start), end = moved(it.end)) },
        guides.map { it.copy(start = moved(it.start), end = moved(it.end), curveMid = it.curveMid?.let(::moved)) },
        extrusions.map { it.copy(origin = moved(it.origin), handle = moved(it.handle), vanishing = moved(it.vanishing), sun = moved(it.sun)) })
}

/**
 * An object drawn in another plane, flattened into the canvas being edited so it can be moved and reshaped there.
 * The two planes share one model axis ([across]); the object's extrusion axis ([depth]) is the other visible one.
 */
private class ForeignObject(
    val plane: ModelPlane, val extrusionId: Int, val name: String, val argb: Int,
    /** Triangle vertices: x, y in this canvas, the same in the object's own canvas, and whether each is on the far side. */
    val canvas: FloatArray, val own: FloatArray, val back: BooleanArray,
    /** Outline endpoints on both the facing and the far side. */
    val points: List<ForeignPoint>,
    val across: Vec3, val depth: Vec3, val acrossMin: Double, val acrossMax: Double, val faceMm: Double, val backMm: Double, val minThicknessMm: Double,
) { val key get() = plane to extrusionId }

/** An outline endpoint of a foreign object: where it is in its own canvas, where it shows here, and which side it is on. */
private data class ForeignPoint(val own: Pt, val at: Pt, val back: Boolean)
private enum class ForeignHandle { MOVE, FACE, BACK, ACROSS_MIN, ACROSS_MAX, POINTS }
/** [points] are own-canvas endpoints being dragged; [back] says whether their far-side corners were grabbed. */
private data class ForeignDrag(val target: ForeignObject, val handle: ForeignHandle, val start: Pt, val current: Pt, val points: List<Pt> = emptyList(), val back: Boolean = false)
/** A selected outline point (or points that line up) of a foreign object, kept by key so it survives rebuilds. */
private data class ForeignPointSelection(val key: Pair<ModelPlane, Int>, val points: List<Pt>, val back: Boolean)
/** What the editor draws for the selected foreign object: its preview triangles, handles (anchor and outward direction) and a live value. */
private class ForeignOverlay(val triangles: FloatArray, val handles: List<Triple<ForeignHandle, Pt, Pt>>, val points: List<Pair<Pt, Boolean>>, val label: String?)
private const val BOSCH_COLOR_INDEX = 4
private val lineColors = listOf(Color(0xFF00E5FF), Color(0xFFFF4FA3), Color(0xFF91FF3D), Color(0xFFFFA726), Color(0xFF1565C0))
private fun lineColor(index: Int) = lineColors[index.mod(lineColors.size)]
private val guideColors = listOf(Color.Black, Color(0xFF0066BE), Color(0xFFCF2020), Color(0xFF008A48), Color(0xFFE08A00))
private fun guideColor(index: Int) = guideColors[index.mod(guideColors.size)]
private val decimalInputPattern = Regex("\\d*(\\.\\d*)?")
private fun measurementColor(line: MeasureLine): Color = when {
    line.isReference || line.pathId != null && !line.isScaleReference -> Color(0xFFFFD400)
    else -> lineColor(line.colorIndex)
}
private enum class ComparisonFigure(val description: String, val sizeMm: Double, val widthRatio: Float) {
    BEE("Bee in profile", 10.0, 1.5f), SPARROW("Sparrow in profile", 95.0, 1.6f), HAND("Hand palm", 180.0, 0.8f), CAT("Cat", 250.0, 1f), DOG("Dog", 600.0, 1.1f),
    STANDING("Standing person", 1700.0, 0.28f), SITTING("Sitting person", 1250.0, 0.75f), TOP("Person viewed from above", 500.0, 1f)
}
private data class PictureOptions(val figure: ComparisonFigure? = null, val sum: Boolean = false, val legend: Boolean = false, val guess: Boolean = false, val transparency: Int = 0)
private data class SavedProject(val id: String, val name: String, val unit: String, val lines: List<MeasureLine>, val guides: List<GuideLine> = emptyList(), val guidesDetected: Boolean = false, val capturedAt: Long, val rotation: Float = 0f, val options: PictureOptions = PictureOptions(), val extrusions: List<Extrusion> = emptyList(), val blankScene: Boolean = false, val frames: Map<ModelPlane, PlaneFrame> = emptyMap(), val solidOps: List<SolidOp> = emptyList(), val itemGroups: List<List<Pair<ModelPlane, Int>>> = emptyList(), val models: List<ImportedModel> = emptyList())

/**
 * A 3D model file brought in from outside (STL, OBJ or 3MF), kept as one solid piece: its mesh is stored next to
 * the project, centered, and placed here by [center] (model millimeters), [scale] and turns about X, Y and Z.
 */
private data class ImportedModel(val id: Int, val name: String, val center: Vec3, val scale: Double = 1.0, val rx: Double = 0.0, val ry: Double = 0.0, val rz: Double = 0.0, val argb: Int = 0xFF9E9E9E.toInt(), val finish: Finish = Finish.PLAIN, val turns: List<Pair<Int, Double>> = emptyList()) {
    /** Turns about model axes (0 = X, 1 = Y, 2 = Z) made after [rx], [ry] and [rz], in order. */
    val turnAxes: List<Pair<Vec3, Double>> get() = turns.map { (axis, deg) -> listOf(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Vec3(0.0, 0.0, 1.0))[axis] to deg }
}

/** Corners of the smallest convex outline around [points]. */
private fun convexHull(points: List<Pt>): List<Pt> {
    val sorted = points.distinct().sortedWith(compareBy({ it.x }, { it.y }))
    if (sorted.size < 3) return sorted
    fun cross(o: Pt, a: Pt, b: Pt) = (a.x - o.x).toDouble() * (b.y - o.y) - (a.y - o.y).toDouble() * (b.x - o.x)
    val lower = mutableListOf<Pt>(); val upper = mutableListOf<Pt>()
    sorted.forEach { p -> while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0) lower.removeAt(lower.lastIndex); lower.add(p) }
    sorted.asReversed().forEach { p -> while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0) upper.removeAt(upper.lastIndex); upper.add(p) }
    return lower.dropLast(1) + upper.dropLast(1)
}

/** Words for the depth axis of the plane being edited, so dialogs say Z, Y or X as appropriate. */
private object DepthWords {
    var plane by mutableStateOf(ModelPlane.FRONT)
    val axis get() = plane.normalAxis.label
    val toward get() = when (plane) { ModelPlane.FRONT -> "Bring front"; ModelPlane.TOP -> "Raise"; ModelPlane.PROFILE -> "Move right" }
    val away get() = when (plane) { ModelPlane.FRONT -> "Move back"; ModelPlane.TOP -> "Lower"; ModelPlane.PROFILE -> "Move left" }
    val surface get() = when (plane) { ModelPlane.FRONT -> "the picture surface (Z = 0)"; ModelPlane.TOP -> "Y = 0 (the bottom of the front drawing)"; ModelPlane.PROFILE -> "X = 0 (the left edge of the front drawing)" }
}

private object MeasurementPrecision {
    val places = mutableStateMapOf("mm" to 0, "cm" to 2, "m" to 2, "in" to 2, "ft" to 2)
    fun load(context: Context) {
        val preferences = context.getSharedPreferences("idea3d-settings", Context.MODE_PRIVATE)
        places.keys.toList().forEach { unit -> places[unit] = preferences.getInt("precision-$unit", if (unit == "mm") 0 else 2).coerceIn(0, 2) }
    }
}

private object BoschDebugLog {
    private const val maxEntries = 300
    private val entries = mutableListOf<String>()

    @Synchronized fun clear() {
        entries.clear()
        add("Diagnostic session started")
    }

    @Synchronized fun add(message: String) {
        val stamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        entries += "$stamp  $message"
        if (entries.size > maxEntries) entries.removeAt(0)
    }

    @Synchronized fun exportText(): String = buildString {
        appendLine("Idea 3D Bosch GLM diagnostic log")
        appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())}")
        appendLine("Entries are limited to the latest $maxEntries events.")
        appendLine()
        entries.forEach(::appendLine)
    }
}

private data class BoschReading(val meters: Double, val sequence: Long)

/** Single application-level Bosch connection shared by Settings and line editors. */
private object BoschGlmConnection {
    private var client: BoschGlmClient? = null
    var status by mutableStateOf("Not connected")
        private set
    var ready by mutableStateOf(false)
        private set
    var lastReading by mutableStateOf<BoschReading?>(null)
        private set

    fun connect(context: Context) {
        if (ready) return
        val activeClient = client ?: BoschGlmClient(context.applicationContext).also { client = it }
        activeClient.connect(
            status = { text ->
                status = text
                ready = text.startsWith("Bosch ready")
            },
            measurementMeters = { meters ->
                lastReading = BoschReading(meters, System.currentTimeMillis())
                status = "Bosch connected. Last measurement: ${format(meters)} m"
                ready = true
            }
        )
    }

    fun permissionDenied() {
        status = "Bluetooth permission is required"
        ready = false
    }
}

private class BoschGlmClient(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val manager get() = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var gatt: BluetoothGatt? = null
    private var classicSocket: BluetoothSocket? = null
    private var scanning = false
    private var classicReceiverRegistered = false
    private var scanGeneration = 0
    private var devicesSeen = 0
    private val loggedDevices = mutableSetOf<String>()
    private var onStatus: (String) -> Unit = {}
    private var onMeasurementMeters: (Double) -> Unit = {}

    fun connect(status: (String) -> Unit, measurementMeters: (Double) -> Unit) {
        close()
        BoschDebugLog.clear()
        onStatus = status
        onMeasurementMeters = measurementMeters
        val adapter = manager.adapter
        BoschDebugLog.add("Adapter present=${adapter != null}, enabled=${adapter?.isEnabled}")
        if (adapter == null || !adapter.isEnabled) { report("Turn Bluetooth on, then try again"); return }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) { report("Bluetooth LE scanner is unavailable"); return }
        val generation = ++scanGeneration
        devicesSeen = 0
        loggedDevices.clear()
        scanning = true
        report("Searching for Bosch GLM. Keep its Bluetooth screen open…")
        val pairedGlm = runCatching {
            adapter.bondedDevices.firstOrNull { device -> device.isBoschGlm() }
        }.getOrNull()
        if (pairedGlm != null) {
            BoschDebugLog.add("Found paired Bosch candidate: ${pairedGlm.displayName()} at ${pairedGlm.address}")
            connectClassic(pairedGlm)
            return
        }
        startClassicDiscovery(adapter)
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        runCatching { scanner.startScan(emptyList(), settings, scanCallback) }
            .onFailure { scanning = false; report("Bluetooth scan could not start. Allow Nearby devices permission.") }
        handler.postDelayed({
            if (scanning && generation == scanGeneration) {
                stopScanning()
                report("Bosch GLM not found after scanning $devicesSeen devices. Keep the GLM Bluetooth screen open and retry.")
            }
        }, 20_000L)
    }

    fun close() {
        scanGeneration++
        stopScanning()
        gatt?.close()
        gatt = null
        runCatching { classicSocket?.close() }
        classicSocket = null
    }

    private fun startClassicDiscovery(adapter: BluetoothAdapter) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(classicReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            else context.registerReceiver(classicReceiver, filter)
            classicReceiverRegistered = true
            val started = adapter.startDiscovery()
            BoschDebugLog.add("Classic Bluetooth discovery started=$started")
        }.onFailure { BoschDebugLog.add("Classic Bluetooth discovery failed: ${it.javaClass.simpleName}: ${it.message}") }
    }

    private fun stopScanning() {
        if (scanning) {
            runCatching { manager.adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
            runCatching { manager.adapter?.cancelDiscovery() }
        }
        scanning = false
        if (classicReceiverRegistered) {
            runCatching { context.unregisterReceiver(classicReceiver) }
            classicReceiverRegistered = false
        }
    }

    private fun report(text: String) {
        BoschDebugLog.add(text)
        handler.post { onStatus(text) }
    }
    private fun receive(meters: Double) = handler.post { onMeasurementMeters(meters) }

    private fun BluetoothDevice.displayName(): String = runCatching { name }.getOrNull().orEmpty().ifBlank { "Bosch GLM" }
    private fun BluetoothDevice.isBoschGlm(): Boolean {
        val deviceName = runCatching { name }.getOrNull().orEmpty()
        return deviceName.contains("GLM", ignoreCase = true) || deviceName.contains("BOSCH", ignoreCase = true)
    }

    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(receiverContext: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED) {
                BoschDebugLog.add("Classic Bluetooth discovery finished")
                return
            }
            if (intent.action != BluetoothDevice.ACTION_FOUND || !scanning) return
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device == null) return
            BoschDebugLog.add("Classic device: name='${device.displayName()}', address=${device.address}, BoschCandidate=${device.isBoschGlm()}")
            if (device.isBoschGlm()) connectClassic(device)
        }
    }

    private fun connectClassic(device: BluetoothDevice) {
        if (!scanning) return
        stopScanning()
        report("Connecting to ${device.displayName()} by Bluetooth Classic…")
        Thread {
            val socket = runCatching { device.createInsecureRfcommSocketToServiceRecord(SPP_UUID) }
                .getOrElse { error -> BoschDebugLog.add("Could not create SPP socket: ${error.message}"); null }
                ?: return@Thread
            classicSocket = socket
            val connected = runCatching { socket.connect(); true }.getOrElse { error ->
                BoschDebugLog.add("Classic SPP connection failed: ${error.javaClass.simpleName}: ${error.message}")
                report("Bosch Classic connection failed")
                false
            }
            if (!connected) { runCatching { socket.close() }; return@Thread }
            BoschDebugLog.add("Classic SPP connected")
            report("Bosch connected. Preparing measurement…")
            runCatching {
                socket.outputStream.write(AUTO_SYNC_COMMAND)
                socket.outputStream.flush()
                BoschDebugLog.add("Classic AutoSync command sent")
                report("Bosch ready. Take a measurement on the GLM.")
                readClassicFrames(socket)
            }.onFailure { error ->
                BoschDebugLog.add("Classic measurement stream ended: ${error.javaClass.simpleName}: ${error.message}")
                report("Bosch disconnected")
            }
        }.apply { name = "bosch-glm-classic"; start() }
    }

    private fun readClassicFrames(socket: BluetoothSocket) {
        val pending = ArrayList<Byte>()
        val buffer = ByteArray(256)
        while (true) {
            val count = socket.inputStream.read(buffer)
            if (count <= 0) break
            pending.addAll(buffer.copyOf(count).toList())
            while (pending.size >= 20) {
                val start = pending.indices.firstOrNull { it + 20 <= pending.size && pending[it] == 0xC0.toByte() && pending[it + 1] == 0x55.toByte() }
                if (start == null) { pending.clear(); break }
                if (start > 0) repeat(start) { pending.removeAt(0) }
                if (pending.size < 20) break
                val frame = ByteArray(20) { pending[it] }
                repeat(20) { pending.removeAt(0) }
                BoschDebugLog.add("Classic Bosch frame: ${frame.joinToString("") { "%02X".format(it) }}")
                receiveBoschDistance(frame, "Classic")
            }
        }
    }

    private fun receiveBoschDistance(frame: ByteArray, transport: String) {
        if (frame.size < 11) return
        val type = frame[3].toInt() and 0xFF
        if (type != 0x06 && type != 0x0A) {
            BoschDebugLog.add("$transport frame type 0x${type.toString(16).padStart(2, '0')} is not a distance reading")
            return
        }
        val meters = ByteBuffer.wrap(frame, 7, 4).order(ByteOrder.LITTLE_ENDIAN).float.toDouble()
        if (meters.isFinite() && meters > 0.0001 && meters < 1_000.0) {
            BoschDebugLog.add("$transport distance decoded: $meters m (type 0x${type.toString(16).padStart(2, '0')})")
            receive(meters)
        } else {
            BoschDebugLog.add("$transport distance rejected: $meters m")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            devicesSeen++
            val deviceName = runCatching { result.device.name }.getOrNull().orEmpty()
            val advertisedName = result.scanRecord?.deviceName.orEmpty()
            val address = runCatching { result.device.address }.getOrNull().orEmpty()
            val services = result.scanRecord?.serviceUuids?.joinToString { it.uuid.toString() }.orEmpty()
            val hasBoschService = result.scanRecord?.serviceUuids?.any {
                it.uuid == SERVICE_UUID || it.uuid.toString().startsWith("00005301", ignoreCase = true)
            } == true
            val hasBoschName = listOf(deviceName, advertisedName).any {
                it.contains("GLM", ignoreCase = true) || it.contains("BOSCH", ignoreCase = true)
            }
            if (loggedDevices.add(address.ifBlank { "scan-$devicesSeen" })) {
                val displayName = deviceName.ifBlank { advertisedName }.ifBlank { "(none)" }
                val displayAddress = address.ifBlank { "(unavailable)" }
                val displayServices = services.ifBlank { "(none)" }
                BoschDebugLog.add("BLE device: name='$displayName', address=$displayAddress, rssi=${result.rssi}, services=$displayServices, BoschCandidate=${hasBoschService || hasBoschName}")
            }
            if (!hasBoschService && !hasBoschName) return
            if (!scanning) return
            stopScanning()
            val displayName = deviceName.ifBlank { advertisedName }.ifBlank { "Bosch GLM" }
            BoschDebugLog.add("Selected BLE candidate: $displayName at ${address.ifBlank { "unknown address" }}")
            report("Connecting to $displayName…")
            gatt = result.device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            val reason = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "another scan is already running"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "Bluetooth scan registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Bluetooth LE scanning is unsupported"
                SCAN_FAILED_INTERNAL_ERROR -> "an internal Bluetooth error occurred"
                else -> "error $errorCode"
            }
            report("Bluetooth scan failed: $reason")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(connection: BluetoothGatt, status: Int, newState: Int) {
            BoschDebugLog.add("GATT state change: status=$status, state=$newState")
            if (status != BluetoothGatt.GATT_SUCCESS) { report("Bosch connection failed ($status)"); connection.close(); return }
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                report("Connected. Preparing measurement…")
                connection.discoverServices()
            } else {
                report("Bosch disconnected")
                connection.close()
            }
        }

        override fun onServicesDiscovered(connection: BluetoothGatt, status: Int) {
            BoschDebugLog.add("GATT services: status=$status, ${connection.services.joinToString { it.uuid.toString() }}")
            val characteristic = connection.getService(SERVICE_UUID)?.getCharacteristic(MEASUREMENT_UUID)
            if (status != BluetoothGatt.GATT_SUCCESS || characteristic == null) { report("This device does not expose the Bosch GLM service"); return }
            connection.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCCD_UUID)
            if (descriptor == null) { report("Bosch measurement notifications are unavailable"); return }
            descriptor.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            connection.writeDescriptor(descriptor)
        }

        override fun onDescriptorWrite(connection: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            BoschDebugLog.add("GATT notification descriptor write: ${descriptor.uuid}, status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) { report("Could not enable Bosch measurements"); return }
            val characteristic = connection.getService(SERVICE_UUID)?.getCharacteristic(MEASUREMENT_UUID) ?: return
            characteristic.value = AUTO_SYNC_COMMAND
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            connection.writeCharacteristic(characteristic)
            report("Bosch ready. Take a measurement on the GLM.")
        }

        override fun onCharacteristicChanged(connection: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid != MEASUREMENT_UUID) return
            val data = characteristic.value ?: return
            BoschDebugLog.add("Bosch indication: ${data.joinToString("") { "%02X".format(it) }}")
            if (data.size < 11 || data[0] != 0xC0.toByte() || data[1] != 0x55.toByte()) return
            receiveBoschDistance(data, "BLE")
        }
    }

    private companion object {
        val SERVICE_UUID: UUID = UUID.fromString("00005301-0000-0041-5253-534f46540000")
        val MEASUREMENT_UUID: UUID = UUID.fromString("00004301-0000-0041-5253-534f46540000")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val AUTO_SYNC_COMMAND = byteArrayOf(0xC0.toByte(), 0x55, 0x02, 0x01, 0x00, 0x1A)
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }
}

private object ProjectStore {
    private fun directory(context: Context) = File(context.filesDir, "idea3d-projects").apply { mkdirs() }
    fun imageFile(context: Context, id: String) = File(directory(context), "$id.jpg")
    fun thumbnailFile(context: Context, id: String) = File(directory(context), "$id-thumb.png")
    /** An imported model's mesh, as binary STL. */
    fun modelFile(context: Context, id: String, modelId: Int) = File(directory(context), "$id-model-$modelId.stl")
    private fun modelFiles(dir: File, id: String) = dir.listFiles { f -> f.name.startsWith("$id-model-") }.orEmpty().toList()
    /** Library preview: the picture with the drawn shapes and lines on top, so empty scenes are recognizable too. */
    fun writeThumbnail(context: Context, id: String, image: Bitmap, guides: List<GuideLine>, lines: List<MeasureLine>) = runCatching {
        val size = 192f
        val factor = size / max(image.width, image.height)
        val out = Bitmap.createBitmap((image.width * factor).roundToInt().coerceAtLeast(1), (image.height * factor).roundToInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        canvas.drawBitmap(Bitmap.createScaledBitmap(image, out.width, out.height, true), 0f, 0f, null)
        canvas.scale(factor, factor)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawableGuideShapes(guides).forEach { shape ->
            paint.style = Paint.Style.FILL; paint.color = guideColor(shape.first().colorIndex).copy(alpha = 0.35f).toArgb()
            canvas.drawPath(shapePath(shape), paint)
        }
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f / factor
        guides.forEach { guide -> paint.color = guideColor(guide.colorIndex).toArgb(); guidePoints(guide).zipWithNext().forEach { (a, b) -> canvas.drawLine(a.x, a.y, b.x, b.y, paint) } }
        lines.forEach { line -> paint.color = measurementColor(line).toArgb(); canvas.drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint) }
        thumbnailFile(context, id).outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    fun thumbnail(context: Context, id: String): Bitmap? {
        thumbnailFile(context, id).takeIf { it.isFile }?.let { file -> BitmapFactory.decodeFile(file.absolutePath)?.let { return it } }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imageFile(context, id).absolutePath, options)
        options.inSampleSize = generateSequence(1) { it * 2 }.takeWhile { options.outWidth / it > 180 || options.outHeight / it > 180 }.lastOrNull() ?: 1
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imageFile(context, id).absolutePath, options)
    }
    fun list(context: Context): List<SavedProject> = directory(context).listFiles { file -> file.extension == "json" }?.mapNotNull { file -> runCatching { read(file) }.getOrNull() }?.sortedByDescending { File(directory(context), "${it.id}.json").lastModified() } ?: emptyList()
    fun save(context: Context, project: SavedProject) {
        val values = JSONArray()
        project.lines.forEach { line -> values.put(JSONObject().apply {
            put("id", line.id); put("sx", line.start.x); put("sy", line.start.y); put("ex", line.end.x); put("ey", line.end.y)
            put("value", line.value); put("reference", line.isReference); put("label", line.label); put("color", line.colorIndex); put("scaleReference", line.isScaleReference); put("labelOffset", line.labelOffset)
            put("lengthSource", line.lengthSource.name)
            put("startZMm", line.startZMm); put("endZMm", line.endZMm)
            if (line.axis.isNotEmpty()) put("axis", line.axis)
            if (line.plane != ModelPlane.FRONT) put("plane", line.plane.name)
            line.pathId?.let { put("pathId", it); put("segmentIndex", line.segmentIndex) }
        }) }
        val guides = JSONArray()
        project.guides.forEach { guide -> guides.put(JSONObject().apply {
            put("id", guide.id); put("sx", guide.start.x); put("sy", guide.start.y); put("ex", guide.end.x); put("ey", guide.end.y); put("manual", guide.manual); guide.pathId?.let { put("pathId", it) }; put("color", guide.colorIndex); put("width", guide.strokeWidth)
            guide.curveMid?.let { put("curveX", it.x); put("curveY", it.y) }
            guide.fillTransparency?.let { put("fillTransparency", it) }
            guide.groupId?.let { put("groupId", it) }
            guide.lengthMm?.let { put("lengthMm", it) }
            guide.shapeWidthMm?.let { put("shapeWidthMm", it) }; guide.shapeHeightMm?.let { put("shapeHeightMm", it) }
            put("startZMm", guide.startZMm); put("endZMm", guide.endZMm)
            guide.startThicknessMm?.let { put("startThicknessMm", it) }; guide.endThicknessMm?.let { put("endThicknessMm", it) }
            if (guide.plane != ModelPlane.FRONT) put("plane", guide.plane.name)
            guide.materialId?.let { put("material", it) }
            if (guide.materialTurnDeg != 0.0) put("materialTurn", guide.materialTurnDeg)
            if (guide.hole) put("hole", true)
        }) }
        val extrusions = JSONArray()
        project.extrusions.forEach { e -> extrusions.put(JSONObject().apply { put("id", e.id); put("guides", JSONArray(e.guideIds)); put("ox", e.origin.x); put("oy", e.origin.y); put("hx", e.handle.x); put("hy", e.handle.y); put("vx", e.vanishing.x); put("vy", e.vanishing.y); put("sunX", e.sun.x); put("sunY", e.sun.y); e.depthMm?.let { put("depthMm", it) }; put("positionZMm", e.positionZMm); if (e.plane != ModelPlane.FRONT) put("plane", e.plane.name) }) }
        File(directory(context), "${project.id}.json").writeText(JSONObject().apply { put("id", project.id); put("name", project.name); put("unit", project.unit); put("capturedAt", project.capturedAt); put("rotation", project.rotation); put("lines", values); put("guides", guides); put("guidesDetected", project.guidesDetected); put("extrusions", extrusions); put("figure", project.options.figure?.name ?: ""); put("sum", project.options.sum); put("legend", project.options.legend); put("guess", project.options.guess); put("transparency", project.options.transparency); put("blankScene", project.blankScene); put("frames", JSONObject().apply { project.frames.forEach { (plane, f) -> put(plane.name, JSONObject().apply { put("u", f.originU); put("v", f.originV); put("w", f.width); put("h", f.height) }) } }); put("solidOps", JSONArray().apply { project.solidOps.forEach { op -> put(JSONObject().apply { put("targetPlane", op.target.first.name); put("target", op.target.second); put("toolPlane", op.tool.first.name); put("tool", op.tool.second); put("join", op.join); put("keepTool", op.keepTool) }) } }); put("groups", JSONArray().apply { project.itemGroups.forEach { group -> put(JSONArray().apply { group.forEach { (plane, id) -> put(JSONObject().apply { put("plane", plane.name); put("id", id) }) } }) } })
            put("models", JSONArray().apply { project.models.forEach { m -> put(JSONObject().apply { put("id", m.id); put("name", m.name); put("x", m.center.x); put("y", m.center.y); put("z", m.center.z); put("scale", m.scale); put("rx", m.rx); put("ry", m.ry); put("rz", m.rz); put("color", m.argb); put("finish", m.finish.name)
                put("turns", JSONArray().apply { m.turns.forEach { (axis, deg) -> put(JSONArray().apply { put(axis); put(deg) }) } }) }) } }) }.toString())
    }
    private fun trash(context: Context) = File(directory(context), "trash").apply { mkdirs() }
    private fun projectFiles(dir: File, id: String) = listOf("$id.json", "$id.jpg", "$id-thumb.png").map { File(dir, it) }
    /** Moves a project aside so the delete can be undone; only the most recent deletion is kept. */
    fun moveToTrash(context: Context, id: String) {
        trash(context).listFiles()?.forEach { it.delete() }
        projectFiles(directory(context), id).zip(projectFiles(trash(context), id)).forEach { (from, to) -> if (from.isFile) from.renameTo(to) }
        modelFiles(directory(context), id).forEach { it.renameTo(File(trash(context), it.name)) }
    }
    fun restoreFromTrash(context: Context, id: String) {
        projectFiles(trash(context), id).zip(projectFiles(directory(context), id)).forEach { (from, to) -> if (from.isFile) from.renameTo(to) }
        modelFiles(trash(context), id).forEach { it.renameTo(File(directory(context), it.name)) }
    }
    fun delete(context: Context, id: String) { File(directory(context), "$id.json").delete(); imageFile(context, id).delete(); thumbnailFile(context, id).delete(); modelFiles(directory(context), id).forEach { it.delete() } }
    fun exportProject(context: Context, id: String, uri: Uri) {
        val json = File(directory(context), "$id.json")
        val image = imageFile(context, id)
        require(json.isFile && image.isFile) { "Project files are missing" }
        ZipOutputStream(context.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")).use { zip ->
            (listOf("project.json" to json, "image" to image) + modelFiles(directory(context), id).map { "model-${it.name.removePrefix("$id-model-")}" to it }).forEach { (name, file) ->
                zip.putNextEntry(ZipEntry(name)); file.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
            }
        }
    }
    private fun copyLimited(input: InputStream, output: OutputStream, limit: Long) {
        val buffer = ByteArray(8192); var total = 0L
        while (true) {
            val count = input.read(buffer); if (count < 0) break
            total += count; require(total <= limit) { "Project file is too large" }
            output.write(buffer, 0, count)
        }
    }
    fun importProject(context: Context, uri: Uri) {
        val id = UUID.randomUUID().toString()
        val image = imageFile(context, id)
        var json: JSONObject? = null
        var hasImage = false
        try {
            ZipInputStream(context.contentResolver.openInputStream(uri) ?: error("Cannot open project")).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        "project.json" -> {
                            require(json == null) { "Duplicate project data" }
                            val bytes = ByteArrayOutputStream(); copyLimited(zip, bytes, 8L * 1024 * 1024)
                            json = JSONObject(bytes.toString("UTF-8"))
                        }
                        "image" -> {
                            require(!hasImage) { "Duplicate image" }
                            image.outputStream().use { copyLimited(zip, it, 200L * 1024 * 1024) }; hasImage = true
                        }
                        else -> {
                            val modelId = Regex("model-(\\d+)\\.stl").matchEntire(entry.name)?.groupValues?.get(1)?.toIntOrNull() ?: error("Unexpected project entry")
                            modelFile(context, id, modelId).outputStream().use { copyLimited(zip, it, 200L * 1024 * 1024) }
                        }
                    }
                    zip.closeEntry()
                }
            }
            val data = json ?: error("Missing project data")
            require(hasImage) { "Missing project image" }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(image.absolutePath, options)
            require(options.outWidth > 0 && options.outHeight > 0) { "Invalid project image" }
            data.put("id", id)
            save(context, decode(data, System.currentTimeMillis()).withPositiveIds())
        } catch (error: Exception) {
            delete(context, id)
            throw error
        }
    }
    private fun read(file: File): SavedProject = decode(JSONObject(file.readText()), file.lastModified()).withPositiveIds()
    private fun decode(json: JSONObject, fallbackTime: Long): SavedProject {
        val values = json.getJSONArray("lines")
        val lines = buildList { for (index in 0 until values.length()) { val line = values.getJSONObject(index); val fixed = line.optBoolean("scaleReference", line.getBoolean("reference")); val source = LengthSource.entries.firstOrNull { it.name == line.optString("lengthSource") } ?: if (fixed) LengthSource.MANUAL else LengthSource.CALCULATED; add(MeasureLine(line.getInt("id"), Pt(line.getDouble("sx").toFloat(), line.getDouble("sy").toFloat()), Pt(line.getDouble("ex").toFloat(), line.getDouble("ey").toFloat()), line.getDouble("value"), line.getBoolean("reference"), line.optString("label"), line.optInt("color"), fixed, line.optDouble("labelOffset", 0.0).toFloat(), line.takeIf { it.has("pathId") }?.getInt("pathId"), line.optInt("segmentIndex", 0), source, line.optDouble("startZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, line.optDouble("endZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, CalibrationAxis.of(line.optString("axis")).key, ModelPlane.of(line.optString("plane")))) } }
        val guideValues = json.optJSONArray("guides") ?: JSONArray()
        val guides = buildList { for (index in 0 until guideValues.length()) { val guide = guideValues.getJSONObject(index); add(GuideLine(guide.getInt("id"), Pt(guide.getDouble("sx").toFloat(), guide.getDouble("sy").toFloat()), Pt(guide.getDouble("ex").toFloat(), guide.getDouble("ey").toFloat()), guide.optBoolean("manual", false), guide.takeIf { it.has("pathId") }?.getInt("pathId"), guide.optInt("color", 0), guide.optDouble("width", 5.0).toFloat(), if (guide.has("curveX") && guide.has("curveY")) Pt(guide.getDouble("curveX").toFloat(), guide.getDouble("curveY").toFloat()) else null, guide.optInt("fillTransparency", -1).takeIf { it in listOf(0, 20, 50, 90, 100) }, guide.takeIf { it.has("groupId") }?.getInt("groupId"), guide.takeIf { it.has("lengthMm") }?.optDouble("lengthMm")?.takeIf { it.isFinite() && it > 0 }, guide.takeIf { it.has("shapeWidthMm") }?.optDouble("shapeWidthMm")?.takeIf { it.isFinite() && it > 0 }, guide.takeIf { it.has("shapeHeightMm") }?.optDouble("shapeHeightMm")?.takeIf { it.isFinite() && it > 0 }, guide.optDouble("startZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, guide.optDouble("endZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, guide.takeIf { it.has("startThicknessMm") }?.optDouble("startThicknessMm")?.takeIf { it.isFinite() && it > 0 }, guide.takeIf { it.has("endThicknessMm") }?.optDouble("endThicknessMm")?.takeIf { it.isFinite() && it > 0 }, ModelPlane.of(guide.optString("plane")), guide.optString("material").ifBlank { null }, guide.optDouble("materialTurn", 0.0).takeIf { it.isFinite() } ?: 0.0, hole = guide.optBoolean("hole", false))) } }
        val figure = ComparisonFigure.entries.firstOrNull { it.name == json.optString("figure") } ?: if (!json.has("figure") && json.optBoolean("silhouette")) ComparisonFigure.STANDING else null
        val transparency = json.optInt("transparency", 0).takeIf { it in listOf(0, 10, 90, 100) } ?: 0
        val extrusionValues = json.optJSONArray("extrusions") ?: JSONArray()
        val extrusions = buildList { for (index in 0 until extrusionValues.length()) { val item = extrusionValues.getJSONObject(index); val ids = item.getJSONArray("guides"); add(Extrusion(item.getInt("id"), (0 until ids.length()).map { ids.getInt(it) }, Pt(item.getDouble("ox").toFloat(), item.getDouble("oy").toFloat()), Pt(item.getDouble("hx").toFloat(), item.getDouble("hy").toFloat()), Pt(item.getDouble("vx").toFloat(), item.getDouble("vy").toFloat()), Pt(item.optDouble("sunX", item.getDouble("vx") * 0.3).toFloat(), item.optDouble("sunY", item.getDouble("vy")).toFloat()), item.takeIf { it.has("depthMm") }?.optDouble("depthMm")?.takeIf { it.isFinite() && it >= 0 }, item.optDouble("positionZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, ModelPlane.of(item.optString("plane")))) } }
        return SavedProject(json.getString("id"), json.getString("name"), json.optString("unit", "mm"), lines, guides, json.optBoolean("guidesDetected", false), json.optLong("capturedAt", fallbackTime), json.optDouble("rotation", 0.0).toFloat(), PictureOptions(figure, json.optBoolean("sum"), json.optBoolean("legend"), json.optBoolean("guess"), transparency), extrusions, json.optBoolean("blankScene", false), json.optJSONObject("frames")?.let { frames ->
            ModelPlane.entries.mapNotNull { plane -> frames.optJSONObject(plane.name)?.let { f -> plane to PlaneFrame(f.getDouble("u"), f.getDouble("v"), f.getInt("w"), f.getInt("h")) } }.toMap()
        }.orEmpty(), json.optJSONArray("solidOps")?.let { ops -> (0 until ops.length()).map { i -> ops.getJSONObject(i).let { o ->
            SolidOp(ModelPlane.of(o.optString("targetPlane")) to o.getInt("target"), ModelPlane.of(o.optString("toolPlane")) to o.getInt("tool"), o.optBoolean("join"), o.optBoolean("keepTool")) } } }.orEmpty(),
            json.optJSONArray("groups")?.let { groups -> (0 until groups.length()).map { i -> groups.getJSONArray(i).let { g -> (0 until g.length()).map { k -> g.getJSONObject(k).let { ModelPlane.of(it.optString("plane")) to it.getInt("id") } } } } }.orEmpty(),
            json.optJSONArray("models")?.let { models -> (0 until models.length()).map { i -> models.getJSONObject(i).let { m ->
                ImportedModel(m.getInt("id"), m.optString("name", "Model"), Vec3(m.optDouble("x", 0.0), m.optDouble("y", 0.0), m.optDouble("z", 0.0)), m.optDouble("scale", 1.0), m.optDouble("rx", 0.0), m.optDouble("ry", 0.0), m.optDouble("rz", 0.0), m.optInt("color", 0xFF9E9E9E.toInt()), Finish.of(m.optString("finish")),
                    m.optJSONArray("turns")?.let { t -> (0 until t.length()).map { k -> t.getJSONArray(k).let { it.getInt(0) to it.getDouble(1) } } }.orEmpty())
            } } }.orEmpty())
    }
}

class MainActivity : ComponentActivity() {
    private var sharedImage by mutableStateOf<Uri?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedImage = imageFromIntent(intent)
        setContent { Idea3dTheme { Idea3dApp(sharedImage) { sharedImage = null } } }
    }
    override fun onNewIntent(intent: android.content.Intent) { super.onNewIntent(intent); sharedImage = imageFromIntent(intent) }
}
private fun imageFromIntent(intent: android.content.Intent?): Uri? = if (intent?.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) else null

@Composable private fun Idea3dTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(
        primary = Color(0xFF006E67), secondary = Color(0xFFB3261E), tertiary = Color(0xFF4D5F00),
        background = Color(0xFFF7F8F7), surface = Color.White, surfaceVariant = Color(0xFFE2ECE9)
    ), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Idea3dApp(sharedImage: Uri?, onSharedImageConsumed: () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("idea3d-settings", Context.MODE_PRIVATE) }
    remember { ActionLog.start(context) }
    remember { MeasurementPrecision.load(context) }
    val scope = rememberCoroutineScope()
    var projectExportId by remember { mutableStateOf<String?>(null) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var projectId by remember { mutableStateOf<String?>(null) }
    var projectName by remember { mutableStateOf("") }
    var blankScene by remember { mutableStateOf(false) }
    var projectCapturedAt by remember { mutableLongStateOf(0L) }
    var levelRotation by remember { mutableFloatStateOf(0f) }
    var undoHistory by remember { mutableStateOf(emptyList<UndoSnapshot>()) }
    var library by remember { mutableStateOf(ProjectStore.list(context)) }
    var tool by remember { mutableStateOf(Tool.NONE) }
    var draftStart by remember { mutableStateOf<Pt?>(null) }
    var pendingEnd by remember { mutableStateOf<Pt?>(null) }
    var referenceCount by remember { mutableIntStateOf(0) }
    var lines by remember { mutableStateOf(listOf<MeasureLine>()) }
    var guides by remember { mutableStateOf(listOf<GuideLine>()) }
    var guidesDetected by remember { mutableStateOf(false) }
    var lineDrawing by remember { mutableStateOf(LineDrawing.STRAIGHT) }
    var measureDrawing by remember { mutableStateOf(LineDrawing.STRAIGHT) }
    var activeMeasurePathId by remember { mutableStateOf<Int?>(null) }
    var guideColorIndex by remember { mutableIntStateOf(0) }
    var guideStrokeWidth by remember { mutableFloatStateOf(5f) }
    var activePolylineId by remember { mutableStateOf<Int?>(null) }
    var selectedGuideId by remember { mutableStateOf<Int?>(null) }
    var guideDeleteCandidateId by remember { mutableStateOf<Int?>(null) }
    var activeGuideEndpoint by remember { mutableStateOf<GuideEndpoint?>(null) }
    var activeGuideVertexPoint by remember { mutableStateOf<Pt?>(null) }
    var selectedGuideEndpoint by remember { mutableStateOf<GuideEndpoint?>(null) }
    var selectedWholeGuideId by remember { mutableStateOf<Int?>(null) }
    var activeWholeGuideId by remember { mutableStateOf<Int?>(null) }
    var activeWholeGuideLastPoint by remember { mutableStateOf<Pt?>(null) }
    var activeCurveId by remember { mutableStateOf<Int?>(null) }
    var selectedCurveId by remember { mutableStateOf<Int?>(null) }
    var pointKind by remember { mutableStateOf(PointKind.BREAKPOINT) }
    var guideLengthEditId by remember { mutableStateOf<Int?>(null) }
    // The line whose material is being chosen (a short dialog of its own).
    var lineMaterialId by remember { mutableStateOf<Int?>(null) }
    var pendingAlignment by remember { mutableStateOf<Boolean?>(null) }
    var selectedVertices by remember { mutableStateOf(emptyList<Pt>()) }
    var panOnEmptyDrag by remember { mutableStateOf(false) }
    var selectionStart by remember { mutableStateOf<Pt?>(null) }
    var selectionEnd by remember { mutableStateOf<Pt?>(null) }
    var groupDragLast by remember { mutableStateOf<Pt?>(null) }
    var curveDragLast by remember { mutableStateOf<Pt?>(null) }
    var guideDetectionRunning by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf<Double?>(null) } // pixels per selected unit
    var axisScale by remember { mutableStateOf<Pair<Double, Double>?>(null) } // X and Y pixels per unit, only for per-axis calibration
    var modelReport by remember { mutableStateOf<ModelReport?>(null) }
    var unit by remember { mutableStateOf("mm") }
    var defaultUnit by remember { mutableStateOf("mm") }
    var defaultColor by remember { mutableIntStateOf(preferences.getInt("default-color", 0)) }
    var unitVisibility by remember { mutableStateOf(preferences.getString("unit-visibility", "all") ?: "all") }
    var exportGuides by remember { mutableStateOf(preferences.getBoolean("export-guides", false)) }
    var renameTarget by remember { mutableStateOf<SavedProject?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showDistanceEntry by remember { mutableStateOf(false) }
    var pendingBrokenSegment by remember { mutableStateOf(false) }
    var unknownSize by remember { mutableStateOf(preferences.getBoolean("unknown-size", false)) }
    var pictureOptions by remember { mutableStateOf(PictureOptions()) }
    var extrusions by remember { mutableStateOf(emptyList<Extrusion>()) }
    var selectedExtrusionId by remember { mutableStateOf<Int?>(null) }
    var draggingExtrusion by remember { mutableStateOf(false) }
    var vanishingDragLast by remember { mutableStateOf<Pt?>(null) }
    var shapeEditId by remember { mutableStateOf<Int?>(null) }
    var extrusionEditId by remember { mutableStateOf<Int?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var activePoint by remember { mutableStateOf<Endpoint?>(null) }
    var activeWholeLineId by remember { mutableStateOf<Int?>(null) }
    var activeWholeLineLastPoint by remember { mutableStateOf<Pt?>(null) }
    var selectedEndpoint by remember { mutableStateOf<Endpoint?>(null) }
    var selectedLabelId by remember { mutableStateOf<Int?>(null) }
    var selectedWholeLineId by remember { mutableStateOf<Int?>(null) }
    var magnifierPoint by remember { mutableStateOf<Pt?>(null) }
    var selectedLineId by remember { mutableStateOf<Int?>(null) }
    var deleteCandidateId by remember { mutableStateOf<Int?>(null) }
    var alignedSegments by remember { mutableStateOf(emptySet<Int>()) }
    var alignmentLineIds by remember { mutableStateOf(emptySet<Int>()) }
    var alignmentFlashToken by remember { mutableIntStateOf(0) }
    var alignmentPauseUntil by remember { mutableLongStateOf(0L) }
    var edgeSnapKey by remember { mutableStateOf<String?>(null) }
    var edgeSnapPoint by remember { mutableStateOf<Pt?>(null) }
    var edgeFlashToken by remember { mutableIntStateOf(0) }
    var edgePauseUntil by remember { mutableLongStateOf(0L) }
    var adjustmentIndicatorPoint by remember { mutableStateOf<Pt?>(null) }
    var adjustmentLineId by remember { mutableStateOf<Int?>(null) }
    var adjustmentGuideId by remember { mutableStateOf<Int?>(null) }
    var adjustmentFlashToken by remember { mutableIntStateOf(0) }
    var showLineEdit by remember { mutableStateOf(false) }
    var junctionAction by remember { mutableStateOf<JunctionAction?>(null) }
    var joinPick by remember { mutableStateOf<JoinPick?>(null) }
    var materials by remember { mutableStateOf(MaterialStore.load(context)) }
    var showMaterials by remember { mutableStateOf(false) }
    var saveMaterialShape by remember { mutableStateOf<List<GuideLine>?>(null) }
    var showMaterialList by remember { mutableStateOf(false) }
    var showFinder by remember { mutableStateOf(false) }
    // Canvas area to zoom to once a plane switch has settled (from the finder).
    var pendingFocus by remember { mutableStateOf<FloatArray?>(null) }
    var ghostLines by remember { mutableStateOf<List<Pair<Pair<ModelPlane, Int>, FloatArray>>>(emptyList()) }
    // The line selected before switching views: highlighted in the other views and selected again on return.
    var crossViewSelection by remember { mutableStateOf<Pair<ModelPlane, Int>?>(null) }
    // An end point of a line drawn in another view, selected here: (its view, line id, start?).
    var selectedGhostPoint by remember { mutableStateOf<Triple<ModelPlane, Int, Boolean>?>(null) }
    var ghostDragLast by remember { mutableStateOf<Pt?>(null) }
    var showPictureEdit by remember { mutableStateOf(false) }
    var showPointDepth by remember { mutableStateOf(false) }
    var viewerModel by remember { mutableStateOf<ViewerMesh?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    var viewerObjects by remember { mutableStateOf<List<Pair<ModelPlane, Int>>>(emptyList()) }
    var show3d by remember { mutableStateOf(false) }
    val viewerCamera = remember { ViewerCamera() }
    var viewerEditObject by remember { mutableStateOf<Int?>(null) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showMeasurements by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var presentation by remember { mutableStateOf(false) }
    var cropFrame by remember { mutableStateOf<CropFrame?>(null) }
    var showCanvasSize by remember { mutableStateOf(false) }
    var activeCropEdge by remember { mutableStateOf<CropEdge?>(null) }
    var cropSession by remember { mutableStateOf<UndoSnapshot?>(null) }
    // The editor works on one drawing plane at a time; the others wait in planeStash with their plane tag.
    var editPlane by remember { mutableStateOf(ModelPlane.FRONT) }
    var planeStash by remember { mutableStateOf(emptyMap<ModelPlane, PlaneGeometry>()) }
    SideEffect { DepthWords.plane = editPlane }
    /** Measurements of a plane (they stay in their own view). */
    fun planeLines(plane: ModelPlane): List<MeasureLine> = if (plane == editPlane) lines.map { it.copy(plane = plane) } else planeStash[plane]?.lines.orEmpty()
    var planeFrames by remember { mutableStateOf(emptyMap<ModelPlane, PlaneFrame>()) }
    var solidOps by remember { mutableStateOf(emptyList<SolidOp>()) }
    // Items (lines, shapes, objects) grouped together, from any view: each item by its view and one of its line ids.
    var itemGroups by remember { mutableStateOf(emptyList<List<Pair<ModelPlane, Int>>>()) }
    // Select mode: several items from any view, for group, merge, cut, arrange, move and delete.
    var selectMode by remember { mutableStateOf(false) }
    var pickedItems by remember { mutableStateOf(emptyList<Pair<ModelPlane, Int>>()) }
    var itemDragLast by remember { mutableStateOf<Pt?>(null) }
    var itemRectStart by remember { mutableStateOf<Pt?>(null) }
    var itemRectEnd by remember { mutableStateOf<Pt?>(null) }
    var selectedGhostPoints by remember { mutableStateOf(emptyList<Triple<ModelPlane, Int, Boolean>>()) }
    // Points picked in Select mode, from any view (including this one): (view, line id, start?).
    var pickedPoints by remember { mutableStateOf(emptyList<Triple<ModelPlane, Int, Boolean>>()) }
    // What a box picks in Select mode: the points inside it (default) or the whole items inside it.
    var boxPicksItems by remember { mutableStateOf(false) }
    // Size labels on lines and shapes can be hidden to see the drawing clearly.
    var showSizes by remember { mutableStateOf(preferences.getBoolean("show_sizes", true)) }
    // Grow arrows (front and back of a shape or object) show only after the Grow button is tapped.
    var growMode by remember { mutableStateOf(false) }
    // Models imported from files (STL, OBJ, 3MF): their placement, their centered meshes, and their outline in this view.
    var importedModels by remember { mutableStateOf(emptyList<ImportedModel>()) }
    var importedMeshes by remember { mutableStateOf(emptyMap<Int, Mesh>()) }
    var modelOutlines by remember { mutableStateOf(emptyList<Triple<Int, Int, List<Pt>>>()) }
    var selectedModelId by remember { mutableStateOf<Int?>(null) }
    var modelDialogId by remember { mutableStateOf<Int?>(null) }
    var modelDragLast by remember { mutableStateOf<Pt?>(null) }
    // A rectangle or circle being dragged out (draftStart to pendingEnd).
    var shapeDragging by remember { mutableStateOf(false) }
    // Views shown from the other side (Back, Bottom, Left): the same plane, mirrored.
    var mirroredViews by remember { mutableStateOf(emptySet<ModelPlane>()) }
    // How far one tap on a turn button turns: 15°, 90° or 5°.
    var turnStep by remember { mutableStateOf(15.0) }
    /** The front canvas is the picture; Top and Profile canvases start at the picture's size and can be cropped or grown. */
    fun frameOf(plane: ModelPlane): PlaneFrame {
        val width = bitmap?.width ?: 1; val height = bitmap?.height ?: 1
        val frame = planeFrames[plane] ?: defaultFrame(plane, width, height)
        return if (plane == ModelPlane.FRONT) frame.copy(width = width, height = height) else frame
    }
    // A plane uses its own calibration lines; without any, Top and Profile reuse the front scale and a blank scene is 1 pixel per unit.
    fun scaleFor(input: List<MeasureLine>, plane: ModelPlane): Double? = recomputeScale(input, unit)
        ?: (if (plane != ModelPlane.FRONT) recomputeScale(planeLines(ModelPlane.FRONT), unit) else null)
        ?: if (blankScene) unitFactorMm(unit) else null
    fun frontAxes() = axisScaleOf(planeLines(ModelPlane.FRONT), unit)
    fun modelSetup() = ModelSetup(ModelPlane.entries.associateWith { frameOf(it) }, ModelPlane.entries.associateWith { scaleFor(planeLines(it), it) }, frontAxes(), unit)

    /*
     * Every view edits all lines, from every view, as its own. Lines drawn in another view are projected into this
     * one: they keep that view in [GuideLine.plane], get negative ids (and path and group ids), and their depth
     * along this view. Everything else about them works like a line drawn here. They are converted back whenever the
     * other view's geometry is read (planeGeometry), so saving, 3D and export always see each line where it lives.
     * Lines that outline a 3D object stay with their object in the other view.
     */
    val projectedIdSpan = 100_000_000
    fun encodedId(plane: ModelPlane, id: Int) = -((plane.ordinal + 1) * projectedIdSpan + id)
    fun decodedId(id: Int): Int = (-id) % projectedIdSpan
    /** New lines made from a projected one (a split, say) get ids after everything already in their view. */
    fun newIdBase(plane: ModelPlane, values: (GuideLine) -> Int?): Int =
        ((planeStash[plane]?.guides.orEmpty().mapNotNull(values)) + guides.filter { it.plane == plane }.mapNotNull(values).filter { it < 0 }.map(::decodedId)).maxOrNull() ?: 0
    fun homeId(value: Int?, base: Int): Int? = value?.let { if (it < 0) decodedId(it) else base + it }
    fun projectGuide(g: GuideLine, from: ModelPlane, setup: ModelSetup): GuideLine? {
        val own = setup.mapping(from) ?: return null; val here = setup.mapping(editPlane) ?: return null
        val a = own.model(g.start, g.startZMm); val b = own.model(g.end, g.endZMm)
        val m = g.curveMid?.let { own.model(it, (g.startZMm + g.endZMm) / 2) }
        val n = editPlane.toward
        return g.copy(id = encodedId(from, g.id), start = here.pixel(a), end = here.pixel(b), curveMid = m?.let { here.pixel(it) }, startZMm = a dot n, endZMm = b dot n, midDepthMm = m?.let { it dot n },
            pathId = g.pathId?.let { encodedId(from, it) }, groupId = g.groupId?.let { encodedId(from, it) }, plane = from)
    }
    fun unprojectGuide(g: GuideLine, setup: ModelSetup, ids: Int, paths: Int, groups: Int): GuideLine? {
        val home = g.plane
        val own = setup.mapping(home) ?: return null; val here = setup.mapping(editPlane) ?: return null
        val a = here.model(g.start, g.startZMm); val b = here.model(g.end, g.endZMm)
        // An untouched projected curve knows its exact middle depth; pieces made from it use their ends.
        val m = g.curveMid?.let { here.model(it, if (g.id < 0 && g.midDepthMm != null) g.midDepthMm else (g.startZMm + g.endZMm) / 2) }
        val n = home.toward
        return g.copy(id = homeId(g.id, ids)!!, start = own.pixel(a), end = own.pixel(b), curveMid = m?.let { own.pixel(it) }, startZMm = a dot n, endZMm = b dot n, midDepthMm = null,
            pathId = homeId(g.pathId, paths), groupId = homeId(g.groupId, groups))
    }
    fun planeGeometry(plane: ModelPlane): PlaneGeometry {
        if (plane == editPlane) return PlaneGeometry(lines.map { it.copy(plane = plane) }, guides.filter { it.plane == plane }, extrusions.map { it.copy(plane = plane) })
        val stash = planeStash[plane] ?: PlaneGeometry()
        val shown = guides.filter { it.plane == plane }
        if (shown.isEmpty()) return stash
        val setup = modelSetup()
        val ids = newIdBase(plane) { it.id }; val paths = newIdBase(plane) { it.pathId }; val groups = newIdBase(plane) { it.groupId }
        return stash.copy(guides = stash.guides + shown.mapNotNull { unprojectGuide(it, setup, ids, paths, groups) })
    }
    fun allLines() = ModelPlane.entries.flatMap { planeGeometry(it).lines }
    fun allGuides() = ModelPlane.entries.flatMap { planeGeometry(it).guides }
    fun allExtrusions() = ModelPlane.entries.flatMap { planeGeometry(it).extrusions }
    /** Lines of [full] that outline one of its 3D objects (these stay in their view, edited through the object). */
    fun objectLines(full: PlaneGeometry): Set<Int> = full.extrusions.flatMap { e -> val outline = extrusionGuides(e, full.guides); (outline + holesInside(outline, full.guides)).map { it.id } }.toSet()
    /** Shows [next]: its own geometry plus every other view's lines projected into it. [full] is every view's complete geometry. */
    fun showView(next: ModelPlane, full: Map<ModelPlane, PlaneGeometry>) {
        // Every view's calibration has to be in place before converting, or lines change size on each switch.
        editPlane = next
        val own = full[next] ?: PlaneGeometry()
        lines = own.lines.map { it.copy(plane = next) }
        planeStash = ModelPlane.entries.filter { it != next }.associateWith { full[it] ?: PlaneGeometry() }
        val setup = modelSetup()
        val canProject = { p: ModelPlane -> setup.mapping(p) != null && setup.mapping(next) != null }
        val projected = ModelPlane.entries.filter { it != next && canProject(it) }.flatMap { p ->
            val g = full[p] ?: return@flatMap emptyList()
            val keep = objectLines(g)
            g.guides.filterNot { it.id in keep }.mapNotNull { projectGuide(it, p, setup) }
        }
        planeStash = ModelPlane.entries.filter { it != next }.associateWith { p ->
            val g = full[p] ?: PlaneGeometry()
            val keep = objectLines(g)
            g.copy(guides = if (canProject(p)) g.guides.filter { it.id in keep } else g.guides)
        }.filterValues { it != PlaneGeometry() }
        guides = own.guides.map { it.copy(plane = next) } + projected; extrusions = own.extrusions
    }

    // Top and Profile (and blank scenes) draw on a plain white canvas.
    val editFrame = frameOf(editPlane)
    val planeCanvas = remember(bitmap, editPlane == ModelPlane.FRONT, editFrame.width, editFrame.height) {
        bitmap?.let { image -> if (editPlane == ModelPlane.FRONT) image else Bitmap.createBitmap(editFrame.width, editFrame.height, Bitmap.Config.RGB_565).apply { eraseColor(AndroidColor.WHITE) } }
    }
    var foreign by remember { mutableStateOf<List<ForeignObject>>(emptyList()) }
    // Bars and pipes (lines with a material), flattened into this plane's canvas.
    var memberGhosts by remember { mutableStateOf<List<Pair<Int, FloatArray>>>(emptyList()) }
    // The same names the 3D view and export use ("Shape 1", "Top Shape 2"), for dialogs and hints.
    val objectNames = remember(guides, extrusions, planeStash, planeFrames, scale, unit, editPlane) {
        runCatching {
            val objects = mutableListOf<Extrusion>()
            val specs = buildSolidSpecs(allGuides(), allExtrusions(), modelSetup(), objects, strict = false)
            objects.zip(specs).associate { (e, spec) -> (e.plane to e.id) to spec.name }
        }.getOrDefault(emptyMap())
    }
    var selectedForeign by remember { mutableStateOf<Pair<ModelPlane, Int>?>(null) }
    var foreignDrag by remember { mutableStateOf<ForeignDrag?>(null) }
    var foreignDialog by remember { mutableStateOf<Pair<ModelPlane, Int>?>(null) }
    var selectedForeignPoint by remember { mutableStateOf<ForeignPointSelection?>(null) }
    // Choosing the second object of a 3D cut or join: (join, first object).
    var solidPick by remember { mutableStateOf<Pair<Boolean, Pair<ModelPlane, Int>>?>(null) }
    var pendingCut by remember { mutableStateOf<SolidOp?>(null) }
    var movePointDialog by remember { mutableStateOf(false) }
    var editorViewSize by remember { mutableStateOf(Size.Zero) }
    // Set when a plane or project opens; the view zooms to its content once the other planes' shapes are known.
    var fitPending by remember { mutableStateOf(true) }
    fun recalculate(input: List<MeasureLine>): List<MeasureLine> {
        val nextScale = scaleFor(input, editPlane)
        val nextAxes = if (editPlane != ModelPlane.FRONT) null else axisScaleOf(input, unit)
        scale = nextScale
        axisScale = nextAxes
        return input.map { line ->
            when {
                line.isReference || line.isScaleReference -> line
                line.lengthSource == LengthSource.UNKNOWN -> line
                nextScale != null -> line.copy(value = hypot(frontLength(line.start, line.end, nextScale, nextAxes), (line.endZMm - line.startZMm) / unitFactorMm(unit)))
                else -> line
            }
        }
    }
    fun pushUndo(includeImage: Boolean = false) {
        val snapshot = UndoSnapshot(if (includeImage) bitmap?.copy(Bitmap.Config.ARGB_8888, true) else null, lines, guides, guidesDetected, levelRotation, unit, tool, extrusions, editPlane, planeStash, planeFrames, solidOps, itemGroups, importedModels)
        undoHistory = undoHistory + snapshot
    }
    fun changePictureUnit(nextUnit: String) {
        if (unit == nextUnit) return
        pushUndo()
        val multiplier = unitFactorMm(unit) / unitFactorMm(nextUnit)
        lines = lines.map { it.copy(value = it.value * multiplier) }
        unit = nextUnit
    }
    fun startCrop(point: Pt, hitRadius: Double) {
        val frame = cropFrame ?: return
        val distances = listOf(CropEdge.LEFT to kotlin.math.abs(point.x - frame.left), CropEdge.RIGHT to kotlin.math.abs(point.x - frame.right), CropEdge.TOP to kotlin.math.abs(point.y - frame.top), CropEdge.BOTTOM to kotlin.math.abs(point.y - frame.bottom))
        activeCropEdge = distances.minBy { it.second }.takeIf { it.second <= hitRadius * 2.5 }?.first
    }
    fun dragCrop(point: Pt) {
        val frame = cropFrame ?: return
        val minSize = 16f
        cropFrame = when (activeCropEdge) {
            CropEdge.LEFT -> frame.copy(left = point.x.coerceIn(0f, frame.right - minSize))
            CropEdge.RIGHT -> frame.copy(right = point.x.coerceIn(frame.left + minSize, planeCanvas?.width?.toFloat() ?: frame.right))
            CropEdge.TOP -> frame.copy(top = point.y.coerceIn(0f, frame.bottom - minSize))
            CropEdge.BOTTOM -> frame.copy(bottom = point.y.coerceIn(frame.top + minSize, planeCanvas?.height?.toFloat() ?: frame.bottom))
            null -> frame
        }
    }
    fun finishCropDrag() { activeCropEdge = null }
    /** Moves the current plane's drawing on its canvas and moves the plane's origin with it, so nothing moves in 3D. */
    fun shiftPlaneCanvas(dx: Float, dy: Float, width: Int, height: Int) {
        val moved = PlaneGeometry(lines, guides, extrusions).translated(dx, dy)
        lines = recalculate(moved.lines); guides = moved.guides; extrusions = moved.extrusions
        val frame = frameOf(editPlane)
        planeFrames = planeFrames + (editPlane to PlaneFrame(frame.originU + dx, frame.originV + dy, width, height))
    }
    /** Bounds (canvas pixels) of everything in the plane being edited, including shapes from the other planes. */
    fun contentBounds(): FloatArray? {
        var minX = Float.POSITIVE_INFINITY; var minY = Float.POSITIVE_INFINITY; var maxX = Float.NEGATIVE_INFINITY; var maxY = Float.NEGATIVE_INFINITY
        fun add(x: Float, y: Float) { minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y) }
        guides.forEach { g -> listOfNotNull(g.start, g.end, g.curveMid).forEach { add(it.x, it.y) } }
        lines.forEach { l -> add(l.start.x, l.start.y); add(l.end.x, l.end.y) }
        foreign.forEach { o -> var i = 0; while (i + 1 < o.canvas.size) { add(o.canvas[i], o.canvas[i + 1]); i += 2 } }
        ghostLines.forEach { (_, pts) -> var i = 0; while (i + 1 < pts.size) { add(pts[i], pts[i + 1]); i += 2 } }
        modelOutlines.forEach { (_, _, hull) -> hull.forEach { add(it.x, it.y) } }
        return if (minX.isFinite()) floatArrayOf(minX, minY, maxX, maxY) else null
    }
    fun isBlankCanvas() = editPlane != ModelPlane.FRONT || blankScene
    /** Remembers the editor's size for Fit. Blank canvases are drawn white across the whole view, so they never need to grow. */
    fun fitCanvasToView(viewW: Float, viewH: Float) { if (!presentation) editorViewSize = Size(viewW, viewH) }
    fun startCrop() {
        val snapshot = UndoSnapshot(if (editPlane == ModelPlane.FRONT) bitmap?.copy(Bitmap.Config.ARGB_8888, true) else null, lines, guides, guidesDetected, levelRotation, unit, tool, extrusions, editPlane, planeStash, planeFrames, solidOps, itemGroups)
        val canvas = planeCanvas ?: return
        cropSession = snapshot; cropFrame = CropFrame(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat()); tool = Tool.CROP; draftStart = null; pendingEnd = null
    }
    fun applyCrop() {
        if (editPlane != ModelPlane.FRONT) {
            // Top and Profile canvases are blank: cropping only changes their size.
            val frame = cropFrame ?: return; val canvas = planeCanvas ?: return
            val left = frame.left.toInt().coerceIn(0, canvas.width - 1); val top = frame.top.toInt().coerceIn(0, canvas.height - 1)
            val right = frame.right.toInt().coerceIn(left + 1, canvas.width); val bottom = frame.bottom.toInt().coerceIn(top + 1, canvas.height)
            cropSession?.let { snapshot -> undoHistory = undoHistory + snapshot } ?: pushUndo()
            shiftPlaneCanvas(-left.toFloat(), -top.toFloat(), right - left, bottom - top)
            cropFrame = null; cropSession = null; tool = Tool.ADJUST; zoom = 1f; pan = Offset.Zero
            return
        }
        val source = bitmap ?: return; val frame = cropFrame ?: return
        val left = frame.left.toInt().coerceIn(0, source.width - 1); val top = frame.top.toInt().coerceIn(0, source.height - 1)
        val right = frame.right.toInt().coerceIn(left + 1, source.width); val bottom = frame.bottom.toInt().coerceIn(top + 1, source.height)
        cropSession?.let { snapshot -> undoHistory = undoHistory + snapshot } ?: pushUndo(includeImage = true)
        val cropped = Bitmap.createBitmap(source, left, top, right - left, bottom - top)
        fun moved(point: Pt) = Pt((point.x - left).coerceIn(0f, cropped.width.toFloat()), (point.y - top).coerceIn(0f, cropped.height.toFloat()))
        bitmap = cropped; lines = recalculate(lines.map { line -> line.copy(start = moved(line.start), end = moved(line.end)) }); guides = guides.map { guide -> guide.copy(start = moved(guide.start), end = moved(guide.end), curveMid = guide.curveMid?.let(::moved)) }
        fun translated(point: Pt) = Pt(point.x - left, point.y - top)
        extrusions = extrusions.map { it.copy(origin = moved(it.origin), handle = moved(it.handle), vanishing = translated(it.vanishing), sun = translated(it.sun)) }
        projectId?.let { id -> ProjectStore.imageFile(context, id).outputStream().use { cropped.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        // The front origin follows the photo, so front objects stay put relative to the other planes.
        frameOf(ModelPlane.FRONT).let { f -> planeFrames = planeFrames + (ModelPlane.FRONT to f.copy(originU = f.originU - left, originV = f.originV - top)) }
        cropFrame = null; cropSession = null; tool = if (scale == null) Tool.CALIBRATE else Tool.MEASURE; zoom = 1f; pan = Offset.Zero
    }
    fun rotateCrop(clockwise: Boolean) {
        val source = bitmap ?: return
        val rotated = Bitmap.createBitmap(source.height, source.width, Bitmap.Config.ARGB_8888)
        val matrix = AndroidMatrix().apply {
            if (clockwise) { postRotate(90f); postTranslate(source.height.toFloat(), 0f) }
            else { postRotate(-90f); postTranslate(0f, source.width.toFloat()) }
        }
        AndroidCanvas(rotated).drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        fun map(point: Pt) = if (clockwise) Pt(source.height - point.y, point.x) else Pt(point.y, source.width - point.x)
        bitmap = rotated; lines = recalculate(lines.map { line -> line.copy(start = map(line.start), end = map(line.end)) }); guides = guides.map { guide -> guide.copy(start = map(guide.start), end = map(guide.end), curveMid = guide.curveMid?.let(::map)) }
        extrusions = extrusions.map { it.copy(origin = map(it.origin), handle = map(it.handle), vanishing = map(it.vanishing), sun = map(it.sun)) }
        planeFrames = planeFrames - ModelPlane.FRONT
        cropFrame = CropFrame(0f, 0f, rotated.width.toFloat(), rotated.height.toFloat()); zoom = 1f; pan = Offset.Zero
    }
    fun expandCanvas(left: Int, top: Int, right: Int, bottom: Int) {
        val frame = cropFrame ?: return
        if (editPlane != ModelPlane.FRONT) {
            val canvas = planeCanvas ?: return
            val width = canvas.width.toLong() + left + right; val height = canvas.height.toLong() + top + bottom
            if (width > 32767 || height > 32767 || width * height > 40_000_000L) { logToast(context, "Canvas is too large. Use smaller margins.", Toast.LENGTH_LONG).show(); return }
            shiftPlaneCanvas(left.toFloat(), top.toFloat(), width.toInt(), height.toInt())
            cropFrame = CropFrame(frame.left, frame.top, frame.right + left + right, frame.bottom + top + bottom)
            selectedVertices = emptyList(); zoom = 1f; pan = Offset.Zero; showCanvasSize = false
            return
        }
        val source = bitmap ?: return
        val width = source.width.toLong() + left + right; val height = source.height.toLong() + top + bottom
        if (width > 32767 || height > 32767 || width * height > 40_000_000L) {
            logToast(context, "Canvas is too large. Use smaller margins.", Toast.LENGTH_LONG).show(); return
        }
        val expanded = try {
            Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888).also { output ->
                AndroidCanvas(output).apply { drawColor(AndroidColor.WHITE); drawBitmap(source, left.toFloat(), top.toFloat(), null) }
            }
        } catch (_: OutOfMemoryError) {
            logToast(context, "Not enough memory for that canvas size.", Toast.LENGTH_LONG).show(); return
        }
        fun moved(point: Pt) = Pt(point.x + left, point.y + top)
        bitmap = expanded
        frameOf(ModelPlane.FRONT).let { f -> planeFrames = planeFrames + (ModelPlane.FRONT to f.copy(originU = f.originU + left, originV = f.originV + top)) }
        lines = lines.map { it.copy(start = moved(it.start), end = moved(it.end)) }
        guides = guides.map { it.copy(start = moved(it.start), end = moved(it.end), curveMid = it.curveMid?.let(::moved)) }
        extrusions = extrusions.map { it.copy(origin = moved(it.origin), handle = moved(it.handle), vanishing = moved(it.vanishing), sun = moved(it.sun)) }
        cropFrame = CropFrame(frame.left, frame.top, frame.right + left + right, frame.bottom + top + bottom)
        selectedVertices = emptyList(); zoom = 1f; pan = Offset.Zero; showCanvasSize = false
    }
    fun cancelCrop() {
        cropSession?.let { snapshot -> snapshot.image?.let { bitmap = it }; planeFrames = snapshot.frames; lines = recalculate(snapshot.lines); guides = snapshot.guides; guidesDetected = snapshot.guidesDetected; levelRotation = snapshot.rotation; unit = snapshot.unit; tool = snapshot.tool; extrusions = snapshot.extrusions }
        cropFrame = null; cropSession = null; activeCropEdge = null
    }
    val activity = context as ComponentActivity
    fun setPresentation(enabled: Boolean) {
        presentation = enabled
        activity.requestedOrientation = if (enabled) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        @Suppress("DEPRECATION")
        run { activity.window.decorView.systemUiVisibility = if (enabled) View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY else View.SYSTEM_UI_FLAG_VISIBLE }
    }
    fun importPicture(uri: Uri) {
            blankScene = false
            val id = UUID.randomUUID().toString()
            val destination = ProjectStore.imageFile(context, id)
            destination.outputStream().use { output -> context.contentResolver.openInputStream(uri)?.use { source -> source.copyTo(output) } }
            bitmap = BitmapFactory.decodeFile(destination.absolutePath)
            projectId = id; projectName = imageDisplayName(context, uri).ifBlank { "Picture ${library.size + 1}" }
            projectCapturedAt = imageCaptureTime(destination)
            unit = defaultUnit; editPlane = ModelPlane.FRONT; planeStash = emptyMap(); planeFrames = emptyMap(); solidOps = emptyList(); itemGroups = emptyList(); importedModels = emptyList(); importedMeshes = emptyMap(); selectedModelId = null; selectMode = false; pickedItems = emptyList(); lines = emptyList(); guides = emptyList(); guidesDetected = false; extrusions = emptyList(); selectedExtrusionId = null; scale = null; referenceCount = 0; draftStart = null; pendingEnd = null; pictureOptions = PictureOptions()
            tool = Tool.NONE; zoom = 1f; pan = Offset.Zero; levelRotation = 0f; undoHistory = emptyList(); selectedLineId = null; selectedEndpoint = null
            ProjectStore.save(context, SavedProject(id, imageDisplayName(context, uri).ifBlank { "Picture ${library.size + 1}" }, unit, emptyList(), emptyList(), false, projectCapturedAt, levelRotation))
            library = ProjectStore.list(context)
    }
    fun newScene() {
        val id = UUID.randomUUID().toString()
        val image = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888).apply { eraseColor(AndroidColor.WHITE) }
        ProjectStore.imageFile(context, id).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        blankScene = true; bitmap = image; projectId = id; projectName = "Scene ${library.count { it.blankScene } + 1}"; projectCapturedAt = System.currentTimeMillis()
        unit = defaultUnit; editPlane = ModelPlane.FRONT; planeStash = emptyMap(); planeFrames = emptyMap(); solidOps = emptyList(); itemGroups = emptyList(); importedModels = emptyList(); importedMeshes = emptyMap(); selectedModelId = null; selectMode = false; pickedItems = emptyList(); lines = emptyList(); guides = emptyList(); guidesDetected = true; extrusions = emptyList(); selectedExtrusionId = null
        scale = unitFactorMm(unit); referenceCount = 0; draftStart = null; pendingEnd = null; pictureOptions = PictureOptions()
        tool = Tool.NONE; zoom = 1f; pan = Offset.Zero; levelRotation = 0f; undoHistory = emptyList(); selectedLineId = null; selectedEndpoint = null; selectedVertices = emptyList()
        selectedGuideId = null; selectedGuideEndpoint = null; selectedWholeGuideId = null; selectedWholeLineId = null; selectedCurveId = null; selectedLabelId = null
        deleteCandidateId = null; guideDeleteCandidateId = null; activePolylineId = null; activeMeasurePathId = null
        ProjectStore.save(context, SavedProject(id, projectName, unit, emptyList(), emptyList(), true, projectCapturedAt, blankScene = true))
        library = ProjectStore.list(context)
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(::importPicture) }
    val projectPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ProjectStore.importProject(context, uri) } }
            result.onSuccess { library = ProjectStore.list(context); logToast(context, "Project imported", Toast.LENGTH_SHORT).show() }
                .onFailure { logToast(context, "Could not import project: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
    val projectExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val id = projectExportId; projectExportId = null
        if (uri != null && id != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ProjectStore.exportProject(context, id, uri) } }
            result.onSuccess { logToast(context, "Project exported", Toast.LENGTH_SHORT).show() }
                .onFailure { logToast(context, "Could not export project: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
    fun writeStagedModel(uri: Uri?, extension: String) {
        val staged = File(context.cacheDir, "pending-3d-export.$extension")
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                require(staged.isFile && staged.length() > 0) { "The prepared model is unavailable. Please export again." }
                val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot open the selected file")
                staged.inputStream().use { input -> stream.use { output -> input.copyTo(output) } }
            } }
            result.onSuccess { staged.delete(); logToast(context, "${extension.uppercase()} model exported", Toast.LENGTH_LONG).show() }
                .onFailure { logToast(context, "3D export failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        } else staged.delete()
    }
    /** Reads a downloaded 3D model and places it, one solid piece, in the middle of this view. */
    fun importModel(uri: Uri) {
        val id = projectId ?: return
        scope.launch {
            val fileName = imageDisplayName(context, uri).ifBlank { "Model.stl" }
            val result = withContext(Dispatchers.IO) { runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    val out = ByteArrayOutputStream(); val buffer = ByteArray(65536); var total = 0L
                    while (true) { val n = input.read(buffer); if (n < 0) break; total += n; require(total <= 150L * 1024 * 1024) { "The file is larger than 150 MB" }; out.write(buffer, 0, n) }
                    out.toByteArray()
                } ?: error("Cannot open the file")
                ModelImport.read(fileName, bytes)
            } }
            result.onSuccess { mesh ->
                val modelId = (importedModels.maxOfOrNull { it.id } ?: 0) + 1
                withContext(Dispatchers.IO) { ProjectStore.modelFile(context, id, modelId).writeBytes(ModelExport.stl(listOf(mesh))) }
                val frame = frameOf(editPlane)
                val center = modelSetup().mapping(editPlane)?.model(Pt(frame.width / 2f, frame.height / 2f), 0.0) ?: Vec3(0.0, 0.0, 0.0)
                // STL and 3MF files are usually Z-up (3D printing); this model is Y-up, so they are stood upright.
                val upright = !fileName.lowercase().endsWith(".obj")
                pushUndo()
                importedMeshes = importedMeshes + (modelId to mesh)
                importedModels = importedModels + ImportedModel(modelId, mesh.name, center, rx = if (upright) -90.0 else 0.0)
                selectedModelId = modelId
                val (low, high) = mesh.bounds; val f = java.text.DecimalFormat("0.#")
                logToast(context, "Imported ${mesh.name}: ${mesh.triangles.size} triangles, ${f.format(high.x - low.x)} × ${f.format(high.y - low.y)} × ${f.format(high.z - low.z)} mm. Tap it twice for its size and position.", Toast.LENGTH_LONG).show()
            }.onFailure { logToast(context, "Could not import: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(::importModel) }
    /** Every imported model placed in model space, with its name and color. */
    fun placedModels(models: List<ImportedModel> = importedModels): List<Pair<ImportedModel, Mesh>> = models.mapNotNull { m ->
        importedMeshes[m.id]?.let { m to ModelImport.placed(it, m.scale, m.rx, m.ry, m.rz, m.center, m.turnAxes).copy(name = m.name, argb = m.argb) }
    }
    // Each imported model shows in every view as the outline of its shadow on that view.
    LaunchedEffect(importedModels, importedMeshes, editPlane, planeStash, planeFrames, lines, unit, bitmap) {
        val setup = modelSetup(); val models = importedModels; val meshes = importedMeshes
        modelOutlines = withContext(Dispatchers.Default) { runCatching {
            val here = setup.mapping(editPlane) ?: return@runCatching emptyList()
            models.mapNotNull { m ->
                val base = meshes[m.id] ?: return@mapNotNull null
                val step = (base.vertices.size / 20000).coerceAtLeast(1)
                val sample = base.copy(vertices = base.vertices.filterIndexed { i, _ -> i % step == 0 }, triangles = emptyList())
                Triple(m.id, m.argb, convexHull(ModelImport.placed(sample, m.scale, m.rx, m.ry, m.rz, m.center, m.turnAxes).vertices.map { here.pixel(it) }))
            }
        }.getOrDefault(emptyList()) }
    }
    fun modelAt(point: Pt): Int? = modelOutlines.lastOrNull { it.third.size >= 3 && pointInsidePolygon(point, it.third) }?.first
    // 2D DXF export of the view on screen: the dialog, then the file the system picker creates.
    var showDxfExport by remember { mutableStateOf(false) }
    var pendingDxf by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }
    /**
     * The view on screen as DXF entities, in millimeters along the view's own axes as seen (mirrored for Back,
     * Bottom and Left): lines and curves of this view and those shown here from the other views, closed outlines,
     * holes, measurements, and (with [includeLabels]) measurement and material labels. An item that can't be
     * written is skipped and counted; imported meshes are not part of the 2D drawing.
     */
    fun dxfOfView(includeLabels: Boolean): Pair<List<DxfEntity>, Int> {
        val here = modelSetup().mapping(editPlane) ?: error("Calibrate this view first: its drawing has no real size yet")
        val mirrored = editPlane in mirroredViews
        val sx = if (mirrored && editPlane != ModelPlane.TOP) -1.0 else 1.0
        val sy = if (mirrored && editPlane == ModelPlane.TOP) -1.0 else 1.0
        fun at(p: Pt, z: Double) = here.model(p, z).let { m -> Vec2((m dot editPlane.right) * sx, (m dot editPlane.up) * sy) }
        fun seg(g: GuideLine) = DxfSegment(at(g.start, g.startZMm), at(g.end, g.endZMm), g.curveMid?.let { at(it, (g.startZMm + g.endZMm) / 2) })
        fun pointLike(s: DxfSegment) = s.mid == null && hypot(s.a.x - s.b.x, s.a.y - s.b.y) < 1e-6
        val out = mutableListOf<DxfEntity>(); var skipped = 0
        fun attempt(block: () -> Unit) { try { block() } catch (e: Exception) { skipped++ } }
        val shapes = closedGuideShapes(guides)
        val inShapes = shapes.flatten().map { it.id }.toSet()
        shapes.forEach { path -> attempt { out += DxfExport.closedEntity(if (path.all { it.hole }) DxfLayers.HOLES else DxfLayers.GEOMETRY, path.map(::seg)) } }
        // A line pointing straight into the screen is a point here: nothing to draw, not a failure.
        guides.filterNot { it.id in inShapes }.forEach { g -> attempt { val s = seg(g); if (!pointLike(s)) out += DxfExport.openEntity(if (g.hole) DxfLayers.HOLES else DxfLayers.GEOMETRY, s) } }
        lines.forEach { l -> attempt { val a = at(l.start, l.startZMm); val b = at(l.end, l.endZMm); if (hypot(a.x - b.x, a.y - b.y) > 1e-6) out += DxfLine(DxfLayers.MEASUREMENTS, a, b) } }
        if (includeLabels && out.isNotEmpty()) {
            val pts = guides.flatMap { listOf(it.start, it.end) } + lines.flatMap { listOf(it.start, it.end) }
            val extent = pts.map { at(it, 0.0) }.let { v -> max((v.maxOfOrNull { it.x } ?: 0.0) - (v.minOfOrNull { it.x } ?: 0.0), (v.maxOfOrNull { it.y } ?: 0.0) - (v.minOfOrNull { it.y } ?: 0.0)) }
            val height = max(2.5, extent * 0.015)
            lines.forEach { l -> attempt {
                val value = if (l.lengthSource == LengthSource.UNKNOWN) "?" else "${formatMeasure(l.value, unit)} $unit"
                val a = at(l.start, l.startZMm); val b = at(l.end, l.endZMm)
                out += DxfText(DxfLayers.MEASUREMENTS, Vec2((a.x + b.x) / 2, (a.y + b.y) / 2 + height * 0.4), height, listOf(l.label, value).filter { it.isNotBlank() }.joinToString(": "))
            } }
            // One material label per object: at a shape's middle, or a line's middle.
            val labeled = mutableSetOf<Int>()
            guides.filter { it.materialId != null && !it.hole }.forEach { g -> attempt {
                if (guides.any { o -> o.id in labeled && sameGuideObject(o, g) }) return@attempt
                val name = materials.firstOrNull { it.id == g.materialId }?.name ?: return@attempt
                labeled += g.id
                val shape = shapes.firstOrNull { p -> p.any { it.id == g.id } }
                val spot = if (shape != null) shapePoints(shape).map { at(it, g.startZMm) }.let { v -> Vec2(v.map { it.x }.average(), v.map { it.y }.average()) }
                    else seg(g).let { s -> DxfExport.pointAt(s, 0.5) }
                out += DxfText(DxfLayers.MATERIAL_LABELS, spot, height, name)
            } }
        }
        return out to skipped
    }
    val dxfExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/dxf")) { uri ->
        val (content, count, skipped) = pendingDxf ?: return@rememberLauncherForActivityResult
        pendingDxf = null
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { (context.contentResolver.openOutputStream(uri) ?: error("Cannot open the selected file")).use { it.write(content.toByteArray(Charsets.US_ASCII)) } } }
            result.onSuccess { logToast(context, "DXF exported: $count item${if (count == 1) "" else "s"}" + if (skipped > 0) ", $skipped skipped (couldn't be written)" else "", Toast.LENGTH_LONG).show() }
                .onFailure { logToast(context, "Could not save the DXF: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
    val stlExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("model/stl")) { writeStagedModel(it, "stl") }
    val threeMfExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("model/3mf")) { writeStagedModel(it, "3mf") }
    LaunchedEffect(sharedImage) { sharedImage?.let { importPicture(it); onSharedImageConsumed() } }
    LaunchedEffect(tool, projectId, guidesDetected) {
        val image = bitmap
        if (tool == Tool.LINES && editPlane == ModelPlane.FRONT && image != null && !guidesDetected && !guideDetectionRunning) {
            guideDetectionRunning = true
            val detected = withContext(Dispatchers.Default) { detectStrongGuideLines(image) }
            pushUndo()
            guides = detected
            guidesDetected = true
            guideDetectionRunning = false
        }
    }
    LaunchedEffect(projectId, scale, unit) {
        if (projectId != null && scale != null) extrusions = extrusions.map { extrusion -> extrusion.depthMm?.let { extrusionWithDepth(extrusion, it, scale, unit) } ?: extrusion }
    }
    LaunchedEffect(projectId, projectName, lines, guides, guidesDetected, unit, projectCapturedAt, levelRotation, pictureOptions, extrusions, blankScene, editPlane, planeStash, planeFrames, solidOps, itemGroups, importedModels) {
        scale = scaleFor(lines, editPlane)
        val front = planeGeometry(ModelPlane.FRONT)
        projectId?.let { id -> ProjectStore.save(context, SavedProject(id, projectName, unit, allLines(), allGuides(), guidesDetected, projectCapturedAt, levelRotation, pictureOptions, allExtrusions(), blankScene, planeFrames, solidOps, itemGroups, importedModels)); bitmap?.let { ProjectStore.writeThumbnail(context, id, it, front.guides, front.lines) }; library = ProjectStore.list(context) }
    }
    /** Zooms and centers the canvas on everything in this plane, including shapes from the other planes. */
    /** How far out the view may zoom: until everything drawn fits on screen (a bit of margin), no further. */
    fun minZoomForContent(): Float {
        val canvas = planeCanvas ?: return 0.1f
        val viewW = editorViewSize.width; val viewH = editorViewSize.height
        if (viewW < 10f || viewH < 10f) return 0.1f
        val b = contentBounds() ?: return 0.1f
        val baseFit = min(viewW / canvas.width, viewH / canvas.height)
        return (min(viewW * 0.72f / ((b[2] - b[0]).coerceAtLeast(1f) * baseFit), viewH * 0.72f / ((b[3] - b[1]).coerceAtLeast(1f) * baseFit)) * 0.8f).coerceIn(0.005f, 1f)
    }
    fun fitView(focus: FloatArray? = null) {
        val canvas = planeCanvas ?: return
        val viewW = editorViewSize.width; val viewH = editorViewSize.height
        if (viewW < 10f || viewH < 10f) return
        val raw = focus ?: contentBounds() ?: run { zoom = 1f; pan = Offset.Zero; return }
        // A mirrored view (Back, Bottom, Left) shows the content mirrored on the canvas.
        val flipX = editPlane in mirroredViews && editPlane != ModelPlane.TOP; val flipY = editPlane == ModelPlane.TOP && editPlane in mirroredViews
        val bounds = floatArrayOf(if (flipX) canvas.width - raw[2] else raw[0], if (flipY) canvas.height - raw[3] else raw[1], if (flipX) canvas.width - raw[0] else raw[2], if (flipY) canvas.height - raw[1] else raw[3])
        val minX = bounds[0]; val minY = bounds[1]; val maxX = bounds[2]; val maxY = bounds[3]
        val baseFit = min(viewW / canvas.width, viewH / canvas.height)
        // Leave room around the content for handles.
        val nextZoom = min(viewW * 0.72f / ((maxX - minX).coerceAtLeast(1f) * baseFit), viewH * 0.72f / ((maxY - minY).coerceAtLeast(1f) * baseFit)).coerceIn(0.005f, 400f)
        val fit = baseFit * nextZoom
        zoom = nextZoom
        pan = Offset(viewW / 2f - (minX + maxX) / 2f * fit - (viewW - canvas.width * fit) / 2f, viewH / 2f - (minY + maxY) / 2f * fit - (viewH - canvas.height * fit) / 2f)
    }
    // Objects from the other planes, flattened into this plane's canvas so they can be lined up with, selected and reshaped here.
    LaunchedEffect(projectId, editPlane, planeStash, planeFrames, scale, unit, bitmap, guides, materials, solidOps) {
        if (bitmap == null) return@LaunchedEffect
        val memberGuides = allGuides().filter { it.materialId != null }; val memberSetup = modelSetup(); val memberPlane = editPlane; val materialMap = materials.associateBy { it.id }
        // Every line of the other views, flattened into this one, so plain lines can be found and lined up with too.
        // Lines of other views are shown here as normal lines; only 3D objects' outlines remain in their view.
        // A hole is part of its object; it is never drawn as a line of its own in other views.
        val otherGuides = ModelPlane.entries.filter { it != editPlane }.flatMap { planeStash[it]?.guides.orEmpty() }.filterNot { it.hole }
        // An object's lines, and its holes' lines, sit on its face.
        val faces = ModelPlane.entries.filter { it != editPlane }.flatMap { p -> planeStash[p]?.let { g -> g.extrusions.flatMap { e -> val outline = extrusionGuides(e, g.guides); (outline + holesInside(outline, g.guides)).map { (p to it.id) to facePositionMm(e) } } }.orEmpty() }.toMap()
        ghostLines = withContext(Dispatchers.Default) { runCatching {
            val here = memberSetup.mapping(memberPlane) ?: return@runCatching emptyList()
            otherGuides.mapNotNull { g ->
                val own = memberSetup.mapping(g.plane) ?: return@mapNotNull null
                val pts = if (g.curveMid == null) listOf(g.start, g.end) else (0..16).map { guidePointAt(g, it / 16f) }
                val out = FloatArray(pts.size * 2)
                // Lines of a 3D object sit on its face, where the object is drawn.
                val f = faces[g.plane to g.id] ?: 0.0
                pts.forEachIndexed { i, p -> val t = if (pts.size > 1) i.toDouble() / (pts.size - 1) else 0.0; val q = here.pixel(own.model(p, f + g.startZMm + (g.endZMm - g.startZMm) * t)); out[i * 2] = q.x; out[i * 2 + 1] = q.y }
                (g.plane to g.id) to out
            }
        }.getOrDefault(emptyList()) }
        memberGhosts = withContext(Dispatchers.Default) { runCatching {
            val here = memberSetup.mapping(memberPlane) ?: return@runCatching emptyList()
            buildMembers(memberGuides, materialMap, memberSetup).map { (_, mesh) ->
                val triangles = FloatArray(mesh.triangles.size * 6)
                mesh.triangles.forEachIndexed { t, tri -> listOf(tri.a, tri.b, tri.c).forEachIndexed { k, i -> val p = here.pixel(mesh.vertices[i]); triangles[t * 6 + k * 2] = p.x; triangles[t * 6 + k * 2 + 1] = p.y } }
                mesh.argb to triangles
            }
        }.getOrDefault(emptyList()) }
        val others = ModelPlane.entries.filter { it != editPlane }.map { planeGeometry(it) }
        val plane = editPlane; val setup = modelSetup(); val solidOpsNow = solidOps
        foreign = withContext(Dispatchers.Default) { runCatching {
            val here = setup.mapping(plane) ?: return@runCatching emptyList()
            val objects = mutableListOf<Extrusion>()
            val specs = buildSolidSpecs(others.flatMap { it.guides }, others.flatMap { it.extrusions }, setup, objects, strict = false)
            val axes = listOf(Vec3(1.0, 0.0, 0.0), Vec3(0.0, 1.0, 0.0), Vec3(0.0, 0.0, 1.0))
            // A 3D cut's or join's tool that was used up is gone from the model, so it isn't shown here either.
            val usedUp = solidOpsNow.filter { it.join || !it.keepTool }.map { it.tool }.toSet()
            specs.zip(objects).mapNotNull { (spec, extrusion) ->
                if ((extrusion.plane to extrusion.id) in usedUp) return@mapNotNull null
                val mesh = runCatching { MeshBuilder.extrude(spec) }.getOrNull() ?: return@mapNotNull null
                val own = setup.mapping(extrusion.plane) ?: return@mapNotNull null
                val half = mesh.vertices.size / 2
                val count = mesh.triangles.size * 3
                val canvas = FloatArray(count * 2); val ownPoints = FloatArray(count * 2); val back = BooleanArray(count)
                mesh.triangles.forEachIndexed { t, triangle ->
                    listOf(triangle.a, triangle.b, triangle.c).forEachIndexed { k, index ->
                        val v = t * 3 + k; val model = mesh.vertices[index]
                        val c = here.pixel(model); val o = own.pixel(model)
                        canvas[v * 2] = c.x; canvas[v * 2 + 1] = c.y; ownPoints[v * 2] = o.x; ownPoints[v * 2 + 1] = o.y; back[v] = index >= half
                    }
                }
                val depth = extrusion.plane.toward
                val across = axes.first { abs(it dot plane.toward) < 0.5 && abs(it dot depth) < 0.5 }
                val planeGuides = others.flatMap { it.guides }.filter { it.plane == extrusion.plane }
                val endpoints = mutableListOf<Triple<Pt, Double, Double>>()
                val depthMm = extrusion.depthMm ?: 0.0
                // A circle's joints aren't points to move; it is resized as a whole.
                extrusionGuides(extrusion, planeGuides).takeUnless(::isCircleShape).orEmpty().forEach { edge ->
                    listOf(Triple(edge.start, edge.startZMm, edge.startThicknessMm ?: depthMm), Triple(edge.end, edge.endZMm, edge.endThicknessMm ?: depthMm)).forEach { e -> if (endpoints.none { distance(it.first, e.first) < 0.5 }) endpoints += e }
                }
                val face = facePositionMm(extrusion)
                ForeignObject(extrusion.plane, extrusion.id, spec.name, spec.argb, canvas, ownPoints, back,
                    endpoints.flatMap { (p, z, t) -> listOf(ForeignPoint(p, here.pixel(own.model(p, face + z)), false), ForeignPoint(p, here.pixel(own.model(p, face + z - t)), true)) }, across, depth,
                    mesh.vertices.minOf { it dot across }, mesh.vertices.maxOf { it dot across },
                    mesh.vertices.take(half).maxOf { it dot depth }, mesh.vertices.drop(half).minOf { it dot depth }, spec.outline.minOf { it.thicknessMm })
            }
        }.getOrDefault(emptyList()) }
        if (foreign.none { it.key == selectedForeign }) selectedForeign = null
        if (fitPending && editorViewSize.width > 10f) {
            // A grown canvas rebuilds these shapes first; the next pass fits the view.
            fitView(pendingFocus); fitPending = false; pendingFocus = null
        }
    }
    fun selectedForeignObject() = foreign.firstOrNull { it.key == selectedForeign }
    /** Replaces one plane's guides and objects, whether it is the plane being edited or a stashed one. */
    fun setPlaneShapes(plane: ModelPlane, nextGuides: List<GuideLine>, nextExtrusions: List<Extrusion>) {
        if (plane == editPlane) { guides = nextGuides.map { it.copy(plane = plane) } + guides.filter { it.plane != plane }; extrusions = nextExtrusions; return }
        // Another view: its object outlines stay there; its other lines are shown here, projected.
        val full = PlaneGeometry(planeStash[plane]?.lines.orEmpty(), nextGuides.map { it.copy(plane = plane) }, nextExtrusions.map { it.copy(plane = plane) })
        val keep = objectLines(full); val setup = modelSetup()
        val canProject = setup.mapping(plane) != null && setup.mapping(editPlane) != null
        planeStash = planeStash + (plane to full.copy(guides = if (canProject) full.guides.filter { it.id in keep } else full.guides))
        guides = guides.filter { it.plane != plane } + if (canProject) full.guides.filterNot { it.id in keep }.mapNotNull { projectGuide(it, plane, setup) } else emptyList()
    }


    /** Canvas direction (unit) of a model axis in the plane being edited. */
    fun canvasDirection(axis: Vec3): Pt {
        val d = modelSetup().mapping(editPlane)?.pixelDelta(axis) ?: return Pt(0f, 0f)
        val length = hypot(d.x.toDouble(), d.y.toDouble()).toFloat().coerceAtLeast(1e-6f)
        return Pt(d.x / length, d.y / length)
    }
    /** Handles of a foreign object: anchor on the object and the outward direction the handle sits along. */
    fun foreignHandles(target: ForeignObject): List<Triple<ForeignHandle, Pt, Pt>> {
        val here = modelSetup().mapping(editPlane) ?: return emptyList()
        val a = canvasDirection(target.across); val d = canvasDirection(target.depth)
        val midAcross = (target.acrossMin + target.acrossMax) / 2; val midDepth = (target.faceMm + target.backMm) / 2
        fun at(acrossMm: Double, depthMm: Double) = here.pixel(target.across * acrossMm + target.depth * depthMm)
        // Front and back (depth) handles only while Grow is on.
        return (if (growMode) listOf(Triple(ForeignHandle.FACE, at(midAcross, target.faceMm), d), Triple(ForeignHandle.BACK, at(midAcross, target.backMm), Pt(-d.x, -d.y))) else emptyList()) + listOf(
            Triple(ForeignHandle.ACROSS_MIN, at(target.acrossMin, midDepth), Pt(-a.x, -a.y)), Triple(ForeignHandle.ACROSS_MAX, at(target.acrossMax, midDepth), a),
        )
    }
    fun handlePosition(anchor: Pt, direction: Pt, hitRadius: Double) = Pt((anchor.x + direction.x * hitRadius * 2.2).toFloat(), (anchor.y + direction.y * hitRadius * 2.2).toFloat())
    fun insideForeign(target: ForeignObject, p: Pt): Boolean {
        val c = target.canvas
        var i = 0
        while (i + 5 < c.size) {
            val d1 = (p.x - c[i + 2]) * (c[i + 1] - c[i + 3]) - (c[i] - c[i + 2]) * (p.y - c[i + 3])
            val d2 = (p.x - c[i + 4]) * (c[i + 3] - c[i + 5]) - (c[i + 2] - c[i + 4]) * (p.y - c[i + 5])
            val d3 = (p.x - c[i]) * (c[i + 5] - c[i + 1]) - (c[i + 4] - c[i]) * (p.y - c[i + 1])
            if (!((d1 < 0 || d2 < 0 || d3 < 0) && (d1 > 0 || d2 > 0 || d3 > 0))) return true
            i += 6
        }
        return false
    }
    fun ownGeometryNear(point: Pt, hitRadius: Double) = closestGuideEndpoint(point, pointGuides(guides), hitRadius) != null || closestEndpoint(point, lines, hitRadius) != null ||
        closestGuide(point, guides, hitRadius * 1.7) != null || closedGuideShapes(guides).any { pointInsidePolygon(point, shapePoints(it)) }
    fun clearOwnSelection() {
        selectedExtrusionId = null; selectedCurveId = null; selectedVertices = emptyList(); selectedLineId = null; selectedEndpoint = null; selectedGuideId = null
        selectedGuideEndpoint = null; selectedWholeGuideId = null; selectedWholeLineId = null; selectedLabelId = null; deleteCandidateId = null
    }
    /** Movement of a drag split into the shared axis and the object's depth axis, rounded to the display step and kept valid. */
    fun foreignDelta(drag: ForeignDrag): Pair<Double, Double> {
        val here = modelSetup().mapping(editPlane) ?: return 0.0 to 0.0
        val move = here.modelDelta((drag.current.x - drag.start.x).toDouble(), (drag.current.y - drag.start.y).toDouble())
        val step = unitFactorMm(unit) / Math.pow(10.0, (MeasurementPrecision.places[unit] ?: 2).toDouble())
        fun rounded(v: Double) = Math.round(v / step) * step
        val t = drag.target; val extent = t.acrossMax - t.acrossMin
        var across = rounded(move dot t.across); var depth = rounded(move dot t.depth)
        when (drag.handle) {
            ForeignHandle.FACE -> { across = 0.0; depth = max(depth, 0.1 - t.minThicknessMm) }
            ForeignHandle.BACK -> { across = 0.0; depth = min(depth, t.minThicknessMm - 0.1) }
            ForeignHandle.ACROSS_MIN -> { depth = 0.0; across = min(across, extent - 1.0) }
            ForeignHandle.ACROSS_MAX -> { depth = 0.0; across = max(across, 1.0 - extent) }
            ForeignHandle.POINTS -> if (drag.back) depth = min(depth, t.minThicknessMm - 0.1)
            else -> {}
        }
        return across to depth
    }
    /** Preview triangles, handles and live value for the selected foreign object, following the current drag. */
    fun foreignOverlay(): ForeignOverlay? {
        val target = selectedForeignObject() ?: return null
        val here = modelSetup().mapping(editPlane) ?: return null
        val drag = foreignDrag?.takeIf { it.target.key == target.key }
        val (da, dd) = drag?.let(::foreignDelta) ?: (0.0 to 0.0)
        val kind = drag?.handle
        val shift = here.pixelDelta(target.across * da + target.depth * dd)
        val acrossShift = here.pixelDelta(target.across * da)
        val extent = target.acrossMax - target.acrossMin
        val (anchor, k) = when (kind) { ForeignHandle.ACROSS_MIN -> target.acrossMax to (extent - da) / extent; ForeignHandle.ACROSS_MAX -> target.acrossMin to (extent + da) / extent; else -> 0.0 to 1.0 }
        fun moved(p: Pt, own: Pt?, back: Boolean?): Pt = when (kind) {
            null -> p
            ForeignHandle.MOVE -> Pt(p.x + shift.x, p.y + shift.y)
            ForeignHandle.FACE -> if (back == true) p else Pt(p.x + shift.x, p.y + shift.y)
            ForeignHandle.BACK -> if (back == false) p else Pt(p.x + shift.x, p.y + shift.y)
            // A facing-side point moves the whole outline point; a far-side one also slides sideways but only changes thickness in depth.
            ForeignHandle.POINTS -> if (own == null || drag.points.none { distance(it, own) < 0.5 }) p
                else if (!drag.back || back == true) Pt(p.x + shift.x, p.y + shift.y) else Pt(p.x + acrossShift.x, p.y + acrossShift.y)
            ForeignHandle.ACROSS_MIN, ForeignHandle.ACROSS_MAX -> { val a = here.model(p, 0.0) dot target.across; here.pixelDelta(target.across * ((anchor + (a - anchor) * k) - a)).let { Pt(p.x + it.x, p.y + it.y) } }
        }
        val triangles = FloatArray(target.canvas.size)
        for (v in 0 until target.back.size) {
            val p = moved(Pt(target.canvas[v * 2], target.canvas[v * 2 + 1]), Pt(target.own[v * 2], target.own[v * 2 + 1]), target.back[v])
            triangles[v * 2] = p.x; triangles[v * 2 + 1] = p.y
        }
        val handles = foreignHandles(target).map { (handle, at, dir) ->
            val follows = kind == ForeignHandle.MOVE || kind == handle && kind != ForeignHandle.ACROSS_MIN && kind != ForeignHandle.ACROSS_MAX
            val across = (kind == ForeignHandle.ACROSS_MIN && handle == ForeignHandle.ACROSS_MIN) || (kind == ForeignHandle.ACROSS_MAX && handle == ForeignHandle.ACROSS_MAX)
            Triple(handle, if (follows || across) Pt(at.x + shift.x, at.y + shift.y) else at, dir)
        }
        val chosen = selectedForeignPoint?.takeIf { it.key == target.key }
        val points = target.points.map { point -> moved(point.at, point.own, point.back) to ((kind == ForeignHandle.POINTS && point.back == drag.back && drag.points.any { distance(it, point.own) < 0.5 }) ||
            (chosen != null && point.back == chosen.back && chosen.points.any { distance(it, point.own) < 0.5 })) }
        val format = { mm: Double -> formatLength(mm, LengthUnit.of(unit), MeasurementPrecision.places[unit] ?: 2) }
        fun signed(v: Double) = (if (v >= 0) "+" else "−") + format(abs(v))
        val acrossName = listOf("X", "Y", "Z")[listOf(target.across.x, target.across.y, target.across.z).indexOfFirst { abs(it) > 0.5 }]
        val depthName = target.plane.normalAxis.label
        val label = when (kind) {
            null -> null
            ForeignHandle.MOVE -> "$acrossName ${signed(da)} · $depthName ${signed(dd)}"
            ForeignHandle.POINTS -> if (drag.back) "$acrossName ${signed(da)} · far side $depthName ${signed(dd)}" else "$acrossName ${signed(da)} · $depthName ${signed(dd)}"
            ForeignHandle.FACE -> "${target.name}: depth ${format(target.faceMm - target.backMm + dd)}"
            ForeignHandle.BACK -> "${target.name}: depth ${format(target.faceMm - target.backMm - dd)}"
            ForeignHandle.ACROSS_MIN -> "${target.name}: $acrossName size ${format(extent - da)}"
            ForeignHandle.ACROSS_MAX -> "${target.name}: $acrossName size ${format(extent + da)}"
        }
        return ForeignOverlay(triangles, handles, points, label)
    }
    fun startForeignDrag(point: Pt, hitRadius: Double): Boolean {
        val selected = selectedForeignObject()
        /** Grabs the corner nearest [point]; corners on the same side that line up in this view move together. */
        fun grabPoint(target: ForeignObject): Boolean {
            val nearest = target.points.minByOrNull { distance(it.at, point) } ?: return false
            if (distance(nearest.at, point) > hitRadius) return false
            val group = target.points.filter { it.back == nearest.back && distance(it.at, nearest.at) < hitRadius * 0.15 }.map { it.own }
            foreignDrag = ForeignDrag(target, ForeignHandle.POINTS, point, point, group, nearest.back)
            return true
        }
        if (selected != null) {
            if (grabPoint(selected)) return true
            foreignHandles(selected).firstOrNull { (_, at, dir) -> distance(handlePosition(at, dir, hitRadius), point) <= hitRadius * 1.2 }?.let { (handle, _, _) ->
                foreignDrag = ForeignDrag(selected, handle, point, point); return true
            }
            // Your own points and lines inside a grey shape stay reachable.
            if (ownGeometryNear(point, hitRadius)) return false
            if (insideForeign(selected, point)) { foreignDrag = ForeignDrag(selected, ForeignHandle.MOVE, point, point); return true }
        }
        if (ownGeometryNear(point, hitRadius)) return false
        // A corner of any grey shape can be dragged straight away.
        foreign.lastOrNull { o -> o.points.any { distance(it.at, point) <= hitRadius } }?.let { target ->
            clearOwnSelection(); selectedForeign = target.key
            if (grabPoint(target)) return true
        }
        val hit = foreign.lastOrNull { insideForeign(it, point) } ?: return false
        clearOwnSelection(); selectedForeign = hit.key
        foreignDrag = ForeignDrag(hit, ForeignHandle.MOVE, point, point)
        return true
    }
    fun handleForeignTap(point: Pt, hitRadius: Double): Boolean {
        val selected = selectedForeignObject()
        // Tapping a dot selects that point (and the ones lined up with it) for the arrows and exact moves.
        selected?.points?.filter { distance(it.at, point) <= hitRadius }?.minByOrNull { distance(it.at, point) }?.let { nearest ->
            selectedForeignPoint = ForeignPointSelection(selected.key, selected.points.filter { it.back == nearest.back && distance(it.at, nearest.at) < hitRadius * 0.15 }.map { it.own }, nearest.back)
            return true
        }
        selectedForeignPoint = null
        if (selected != null && (selected.points.any { distance(it.at, point) <= hitRadius } || foreignHandles(selected).any { (_, at, dir) -> distance(handlePosition(at, dir, hitRadius), point) <= hitRadius * 1.2 })) return true
        if (ownGeometryNear(point, hitRadius)) { selectedForeign = null; return false }
        val hit = foreign.lastOrNull { insideForeign(it, point) }
        if (hit == null) { selectedForeign = null; return false }
        if (hit.key == selectedForeign) foreignDialog = hit.key else { clearOwnSelection(); selectedForeign = hit.key; panOnEmptyDrag = false }
        return true
    }
    /** Applies a finished drag to the object's own plane as one undo step. */
    /** Moves part of a foreign object by [da] along the shared axis and [dd] along its depth axis (millimeters). */
    fun applyForeignChange(drag: ForeignDrag, da: Double, dd: Double) {
        if (abs(da) < 1e-9 && abs(dd) < 1e-9) return
        val t = drag.target
        val setup = modelSetup(); val own = setup.mapping(t.plane) ?: return
        val geometry = planeGeometry(t.plane)
        val extrusion = geometry.extrusions.firstOrNull { it.id == t.extrusionId } ?: return
        val depth = extrusion.depthMm ?: return
        val planeScale = setup.scales[t.plane]
        val outline = extrusionGuides(extrusion, geometry.guides)
        val edgeIds = outline.map { it.id }.toSet()
        // Holes move and resize with their object (sideways only; they have no depth of their own).
        val holeIds = holesInside(outline, geometry.guides).map { it.id }.toSet()
        val shift = own.pixelDelta(t.across * da)
        fun shifted(p: Pt) = Pt(p.x + shift.x, p.y + shift.y)
        val extent = t.acrossMax - t.acrossMin
        val (anchor, k) = if (drag.handle == ForeignHandle.ACROSS_MIN) t.acrossMax to (extent - da) / extent else t.acrossMin to (extent + da) / extent
        fun scaled(p: Pt): Pt { val a = own.model(p, 0.0) dot t.across; val d = own.pixelDelta(t.across * ((anchor + (a - anchor) * k) - a)); return Pt(p.x + d.x, p.y + d.y) }
        fun resized(e: Extrusion, d: Double) = extrusionWithDepth(e, d, planeScale, unit)
        pushUndo()
        val face = facePositionMm(extrusion)
        var nextGuides = geometry.guides
        fun edges(change: (GuideLine) -> GuideLine) { nextGuides = nextGuides.map { if (it.id in edgeIds) change(it) else it } }
        fun withHoles(change: (GuideLine) -> GuideLine) { nextGuides = nextGuides.map { if (it.id in edgeIds || it.id in holeIds) change(it) else it } }
        val next = when (drag.handle) {
            ForeignHandle.MOVE -> {
                withHoles { g -> g.copy(start = shifted(g.start), end = shifted(g.end), curveMid = g.curveMid?.let(::shifted)) }
                resized(extrusion.copy(origin = shifted(extrusion.origin), handle = shifted(extrusion.handle)), depth).copy(positionZMm = extrusion.positionZMm + dd)
            }
            ForeignHandle.FACE -> { edges { g -> g.copy(startThicknessMm = g.startThicknessMm?.plus(dd), endThicknessMm = g.endThicknessMm?.plus(dd)) }; withFacePosition(resized(extrusion, depth + dd), face + dd, depth + dd) }
            ForeignHandle.BACK -> { edges { g -> g.copy(startThicknessMm = g.startThicknessMm?.minus(dd), endThicknessMm = g.endThicknessMm?.minus(dd)) }; withFacePosition(resized(extrusion, depth - dd), face, depth - dd) }
            ForeignHandle.ACROSS_MIN, ForeignHandle.ACROSS_MAX -> {
                // A typed size no longer holds once the shape is stretched here.
                withHoles { g -> g.copy(start = scaled(g.start), end = scaled(g.end), curveMid = g.curveMid?.let(::scaled), lengthMm = null, shapeWidthMm = null, shapeHeightMm = null) }
                resized(extrusion.copy(origin = scaled(extrusion.origin), handle = scaled(extrusion.handle)), depth)
            }
            ForeignHandle.POINTS -> {
                fun near(p: Pt) = drag.points.any { distance(it, p) < 0.5 }
                // Facing side: the point moves in depth with its thickness. Far side: only the thickness at the point changes.
                if (!drag.back) edges { g -> g.copy(start = if (near(g.start)) shifted(g.start) else g.start, end = if (near(g.end)) shifted(g.end) else g.end,
                    startZMm = if (near(g.start)) g.startZMm + dd else g.startZMm, endZMm = if (near(g.end)) g.endZMm + dd else g.endZMm, lengthMm = null) }
                else edges { g -> g.copy(start = if (near(g.start)) shifted(g.start) else g.start, end = if (near(g.end)) shifted(g.end) else g.end,
                    startThicknessMm = if (near(g.start)) max(0.1, (g.startThicknessMm ?: depth) - dd) else g.startThicknessMm,
                    endThicknessMm = if (near(g.end)) max(0.1, (g.endThicknessMm ?: depth) - dd) else g.endThicknessMm, lengthMm = null) }
                extrusion
            }
        }
        setPlaneShapes(t.plane, nextGuides, geometry.extrusions.map { if (it.id == extrusion.id) next else it })
    }
    fun applyForeignEdit(drag: ForeignDrag) { val (da, dd) = foreignDelta(drag); applyForeignChange(drag, da, dd) }
    /** Moves the selected foreign point(s) by [da] / [dd] millimeters and keeps them selected. */
    fun moveForeignPoint(selection: ForeignPointSelection, da: Double, dd: Double) {
        val target = foreign.firstOrNull { it.key == selection.key } ?: return
        val anchor = target.points.firstOrNull { it.back == selection.back && selection.points.any { p -> distance(p, it.own) < 0.5 } }?.at ?: return
        applyForeignChange(ForeignDrag(target, ForeignHandle.POINTS, anchor, anchor, selection.points, selection.back), da, if (selection.back) min(dd, target.minThicknessMm - 0.1) else dd)
        val shift = modelSetup().mapping(target.plane)?.pixelDelta(target.across * da) ?: return
        selectedForeignPoint = selection.copy(points = selection.points.map { Pt(it.x + shift.x, it.y + shift.y) })
    }
    /** One arrow step moves a foreign point by one display step along whichever visible axis the arrow points at. */
    fun nudgeForeignPoint(selection: ForeignPointSelection, dx: Float, dy: Float) {
        val target = foreign.firstOrNull { it.key == selection.key } ?: return
        val here = modelSetup().mapping(editPlane) ?: return
        val move = here.modelDelta(dx.toDouble(), dy.toDouble())
        val step = unitFactorMm(unit) / Math.pow(10.0, (MeasurementPrecision.places[unit] ?: 2).toDouble())
        val a = move dot target.across; val d = move dot target.depth
        if (abs(a) >= abs(d)) moveForeignPoint(selection, Math.signum(a) * step, 0.0) else moveForeignPoint(selection, 0.0, Math.signum(d) * step)
    }

    fun ownObjectAt(point: Pt): Pair<ModelPlane, Int>? {
        val shape = closedGuideShapes(guides).lastOrNull { pointInsidePolygon(point, shapePoints(it)) } ?: return null
        val extrusion = extrusions.firstOrNull { e -> extrusionGuides(e, guides).any { g -> shape.any { it.id == g.id } } } ?: return null
        return editPlane to extrusion.id
    }
    fun objectAt(point: Pt): Pair<ModelPlane, Int>? = ownObjectAt(point) ?: foreign.lastOrNull { insideForeign(it, point) }?.key
    fun ownSelectedObject(): Pair<ModelPlane, Int>? {
        val id = selectedWholeGuideId ?: return null
        return extrusions.firstOrNull { e -> extrusionGuides(e, guides).any { it.id == id } }?.let { editPlane to it.id }
    }
    fun objectName(key: Pair<ModelPlane, Int>): String = objectNames[key] ?: "Shape"
    /** Starts a 3D cut (or join) of [target]; the next object tapped, in the canvas or the 3D view, is the other one. */
    fun startSolidOp(join: Boolean, target: Pair<ModelPlane, Int>) { solidPick = join to target }
    fun finishSolidOp(other: Pair<ModelPlane, Int>?) {
        val (join, target) = solidPick ?: return
        solidPick = null
        if (other == null) return
        if (other == target) { logToast(context, "Pick a different object", Toast.LENGTH_SHORT).show(); return }
        val op = SolidOp(target, other, join)
        if (join) { pushUndo(); solidOps = solidOps + op } else pendingCut = op
    }
    /** Lists the cuts and joins [key] takes part in, each with a way to undo it. */
    fun solidOpsOf(key: Pair<ModelPlane, Int>): List<Pair<String, () -> Unit>> = solidOps.filter { it.target == key || it.tool == key }.map { op ->
        val text = if (op.target == key) { if (op.join) "Joined with ${objectName(op.tool)}" else "Cut by ${objectName(op.tool)}${if (op.keepTool) " (kept)" else ""}" }
            else { if (op.join) "Joined into ${objectName(op.target)}" else "Cuts ${objectName(op.target)}${if (op.keepTool) "" else " (hidden in 3D)"}" }
        text to { pushUndo(); solidOps = solidOps - op }
    }

    /** Where an end point of a line from another view shows in this view. */
    /**
     * Where a line's 3D object puts its drawing along the view's depth (0 for lines of no object). Points, outlines
     * and turns add it, so they sit exactly on the object as it is shown, not a depth-length away.
     */
    fun faceOffset(plane: ModelPlane, guideId: Int): Double {
        val g = planeGeometry(plane)
        // A hole sits on its shape's face.
        val id = g.guides.firstOrNull { it.id == guideId }?.takeIf { it.hole }?.let { holeContainer(it, g.guides)?.id } ?: guideId
        return g.extrusions.firstOrNull { e -> extrusionGuides(e, g.guides).any { it.id == id } }?.let(::facePositionMm) ?: 0.0
    }
    fun ghostPointHere(sel: Triple<ModelPlane, Int, Boolean>): Pt? {
        val setup = modelSetup(); val here = setup.mapping(editPlane) ?: return null; val own = setup.mapping(sel.first) ?: return null
        val g = planeGeometry(sel.first).guides.firstOrNull { it.id == sel.second } ?: return null
        val f = faceOffset(sel.first, sel.second)
        return here.pixel(own.model(if (sel.third) g.start else g.end, (if (sel.third) g.startZMm else g.endZMm) + f))
    }
    /** The end point of another view's line nearest [point], within [radius]. */
    fun ghostPointAt(point: Pt, radius: Double): Triple<ModelPlane, Int, Boolean>? =
        ModelPlane.entries.filter { it != editPlane }.flatMap { plane -> pointGuides(planeGeometry(plane).guides).flatMap { g -> listOf(Triple(plane, g.id, true), Triple(plane, g.id, false)) } }
            .mapNotNull { sel -> ghostPointHere(sel)?.let { sel to distance(it, point) } }.filter { it.second <= radius }.minByOrNull { it.second }?.first
    /** Moves an end point of another view's line by a model-space [delta]: along its own drawing and in its depth (Z for Front). */
    fun moveGhostPoint(sel: Triple<ModelPlane, Int, Boolean>, delta: Vec3) {
        val own = modelSetup().mapping(sel.first) ?: return
        val geometry = planeGeometry(sel.first)
        val line = geometry.guides.firstOrNull { it.id == sel.second } ?: return
        val vertex = if (sel.third) line.start else line.end
        val shift = own.pixelDelta(delta); val dz = delta dot sel.first.toward
        fun moved(p: Pt) = Pt(p.x + shift.x, p.y + shift.y)
        val next = geometry.guides.map { g ->
            if (!sameGuideObject(g, line)) g else {
                val s0 = distance(g.start, vertex) < 0.6; val e0 = distance(g.end, vertex) < 0.6
                g.copy(start = if (s0) moved(g.start) else g.start, end = if (e0) moved(g.end) else g.end,
                    startZMm = if (s0) g.startZMm + dz else g.startZMm, endZMm = if (e0) g.endZMm + dz else g.endZMm, lengthMm = if (s0 || e0) null else g.lengthMm)
            }
        }
        setPlaneShapes(sel.first, next, geometry.extrusions)
    }

    /** Every line end in every view (one per spot of each line object), as it shows here. */
    fun allPointsHere(): List<Pair<Triple<ModelPlane, Int, Boolean>, Pt>> {
        val out = mutableListOf<Pair<Triple<ModelPlane, Int, Boolean>, Pt>>()
        ModelPlane.entries.forEach { plane -> pointGuides(planeGeometry(plane).guides).forEach { g ->
            listOf(Triple(plane, g.id, true), Triple(plane, g.id, false)).forEach { sel ->
                val at = ghostPointHere(sel) ?: return@forEach
                // Shared corners are one point.
                if (out.none { (o, p) -> o.first == plane && distance(p, at) < 0.6 }) out += sel to at
            }
        } }
        return out
    }
    fun samePoint(a: Triple<ModelPlane, Int, Boolean>, b: Triple<ModelPlane, Int, Boolean>): Boolean {
        if (a.first != b.first) return false
        val pa = ghostPointHere(a) ?: return false; val pb = ghostPointHere(b) ?: return false
        return distance(pa, pb) < 0.6
    }
    fun togglePoint(sel: Triple<ModelPlane, Int, Boolean>) {
        pickedPoints = if (pickedPoints.any { samePoint(it, sel) }) pickedPoints.filterNot { samePoint(it, sel) } else pickedPoints + sel
    }
    /** All lines of an item (a line, broken line, shape or object) in its own view. */
    /** An item's lines in its own view; a shape's holes are part of it, and picking a hole picks its shape. */
    fun itemGuides(key: Pair<ModelPlane, Int>): List<GuideLine> {
        val all = planeGeometry(key.first).guides
        var seed = all.firstOrNull { it.id == key.second } ?: return emptyList()
        if (seed.hole) holeContainer(seed, all)?.let { seed = it }
        val base = all.filter { sameGuideObject(it, seed) }
        return base + holesInside(base, all).filterNot { h -> base.any { it.id == h.id } }
    }
    /** An item's outline as polylines in this view's canvas pixels. */
    fun itemOutlineHere(key: Pair<ModelPlane, Int>): List<List<Pt>> {
        val setup = modelSetup(); val here = setup.mapping(editPlane) ?: return emptyList(); val own = setup.mapping(key.first) ?: return emptyList()
        val f = itemGuides(key).firstOrNull()?.let { faceOffset(key.first, it.id) } ?: 0.0
        fun loop(at: Double) = itemGuides(key).map { g -> val pts = guidePoints(g); val n = (pts.size - 1).coerceAtLeast(1); pts.mapIndexed { i, p -> here.pixel(own.model(p, at + g.startZMm + (g.endZMm - g.startZMm) * i / n)) } }
        val front = loop(f)
        // A 3D object: its front and back outlines joined at the corners, so the whole object is framed in any view.
        val depth = itemGuides(key).firstOrNull()?.let { g0 -> planeGeometry(key.first).let { geo -> geo.extrusions.firstOrNull { e -> extrusionGuides(e, geo.guides).any { it.id == g0.id } } } }?.depthMm ?: 0.0
        if (depth <= 0.0) return front
        val back = loop(f - depth)
        val sides = itemGuides(key).map { g -> listOf(here.pixel(own.model(g.start, f + g.startZMm)), here.pixel(own.model(g.start, f - depth + g.startZMm))) }
        return front + back + sides
    }
    /** The item under [point]: one drawn here, or one from another view as it shows here. */
    /** The item key of a line shown here: its own view and its id there. */
    fun homeKey(g: GuideLine): Pair<ModelPlane, Int> = if (g.plane == editPlane) editPlane to g.id else g.plane to (homeId(g.id, newIdBase(g.plane) { it.id }) ?: g.id)
    fun itemAt(point: Pt, radius: Double): Pair<ModelPlane, Int>? {
        closestGuide(point, guides, radius * 1.7)?.let { return homeKey(it) }
        fun segmentDistance(a: Pt, b: Pt): Double { val dx = b.x - a.x; val dy = b.y - a.y; val t = (((point.x - a.x) * dx + (point.y - a.y) * dy) / (dx * dx + dy * dy).coerceAtLeast(1e-6f)).coerceIn(0f, 1f); return distance(point, Pt(a.x + dx * t, a.y + dy * t)) }
        ModelPlane.entries.filter { it != editPlane }.flatMap { plane -> planeGeometry(plane).guides.map { plane to it.id } }
            .mapNotNull { key -> itemOutlineHere(key).flatMap { it.zipWithNext() }.minOfOrNull { (a, b) -> segmentDistance(a, b) }?.let { key to it } }
            .filter { it.second <= radius * 1.7 }.minByOrNull { it.second }?.let { return it.first }
        closedGuideShapes(guides).lastOrNull { pointInsidePolygon(point, shapePoints(it)) }?.let { return homeKey(it.first()) }
        foreign.lastOrNull { insideForeign(it, point) }?.let { o -> planeGeometry(o.plane).extrusions.firstOrNull { it.id == o.extrusionId }?.let { e -> extrusionGuides(e, planeGeometry(o.plane).guides).firstOrNull()?.let { return o.plane to it.id } } }
        return null
    }
    fun sameItem(a: Pair<ModelPlane, Int>, b: Pair<ModelPlane, Int>) = a.first == b.first && itemGuides(a).any { it.id == b.second }
    /** An item together with everything grouped with it. */
    fun withGroup(key: Pair<ModelPlane, Int>): List<Pair<ModelPlane, Int>> = itemGroups.firstOrNull { group -> group.any { sameItem(key, it) } } ?: listOf(key)
    fun togglePicked(key: Pair<ModelPlane, Int>) {
        val members = withGroup(key)
        pickedItems = if (pickedItems.any { sameItem(key, it) }) pickedItems.filterNot { p -> members.any { sameItem(it, p) } } else pickedItems + members.filterNot { m -> pickedItems.any { sameItem(m, it) } }
    }
    /** Moves a whole item by a model-space [delta]: its drawing in its own view and its depth. */
    fun moveItem(key: Pair<ModelPlane, Int>, delta: Vec3) {
        val own = modelSetup().mapping(key.first) ?: return
        val geometry = planeGeometry(key.first)
        val ids = itemGuides(key).map { it.id }.toSet()
        val shift = own.pixelDelta(delta); val dz = delta dot key.first.toward
        fun moved(p: Pt) = Pt(p.x + shift.x, p.y + shift.y)
        val nextGuides = geometry.guides.map { g -> if (g.id !in ids) g else g.copy(start = moved(g.start), end = moved(g.end), curveMid = g.curveMid?.let(::moved), startZMm = g.startZMm + dz, endZMm = g.endZMm + dz) }
        // Depth moves through the lines' Z alone; the object's face follows them (moving its position too doubled it).
        val nextExtrusions = geometry.extrusions.map { e -> if (e.guideIds.none { it in ids }) e else e.copy(origin = moved(e.origin), handle = moved(e.handle)) }
        setPlaneShapes(key.first, nextGuides, nextExtrusions)
    }
    /** Picked items once each (a shape and its hole, or two picks of one group member, are one item). */
    fun distinctPicked() = pickedItems.distinctBy { k -> k.first to (itemGuides(k).minOfOrNull { it.id } ?: k.second) }
    fun movePicked(delta: Vec3) {
        distinctPicked().forEach { moveItem(it, delta) }
        // Points of items already moved above stay where the item took them.
        pickedPoints.filterNot { p -> pickedItems.any { it.first == p.first && itemGuides(it).any { g -> g.id == p.second } } }.forEach { moveGhostPoint(it, delta) }
    }
    /** Picks every item lying fully inside the box from [a] to [b]. */
    fun pickInRect(a: Pt, b: Pt) {
        val x1 = min(a.x, b.x); val x2 = max(a.x, b.x); val y1 = min(a.y, b.y); val y2 = max(a.y, b.y)
        val seen = mutableListOf<Pair<ModelPlane, Int>>()
        ModelPlane.entries.forEach { plane -> planeGeometry(plane).guides.forEach { g ->
            val key = plane to g.id
            if (seen.any { sameItem(it, key) }) return@forEach
            val pts = itemOutlineHere(key).flatten()
            if (pts.isNotEmpty() && pts.all { it.x in x1..x2 && it.y in y1..y2 }) seen += key
        } }
        // Anything fully inside the box is picked whole, so Align and Even move it without changing its size.
        seen.forEach { key -> if (pickedItems.none { sameItem(key, it) }) togglePicked(key) }
        if (boxPicksItems) return
        // Only points of items that stick out of the box are picked one by one.
        allPointsHere().filter { (sel, at) -> at.x in x1..x2 && at.y in y1..y2 && pickedItems.none { it.first == sel.first && itemGuides(it).any { g -> g.id == sel.second } } }
            .forEach { (sel, _) -> if (pickedPoints.none { samePoint(it, sel) }) pickedPoints = pickedPoints + sel }
    }
    /** End points of other views' lines inside a box, for multi-point selection. */
    fun ghostPointsInRect(a: Pt, b: Pt): List<Triple<ModelPlane, Int, Boolean>> {
        val x1 = min(a.x, b.x); val x2 = max(a.x, b.x); val y1 = min(a.y, b.y); val y2 = max(a.y, b.y)
        return ModelPlane.entries.filter { it != editPlane }.flatMap { plane -> pointGuides(planeGeometry(plane).guides).flatMap { g -> listOf(Triple(plane, g.id, true), Triple(plane, g.id, false)) } }
            .filter { sel -> ghostPointHere(sel)?.let { it.x in x1..x2 && it.y in y1..y2 } == true }
    }

    /**
     * What the Properties button opens for the current selection: a point's exact position, a shape's size, color,
     * transparency and depth, a line's length, material, color and thickness, or a measurement's details.
     */
    fun openProperties(): (() -> Unit)? {
        selectedForeign?.let { key -> return { foreignDialog = key } }
        if (selectedGhostPoint != null || selectedForeignPoint != null || selectedGuideEndpoint != null || selectedEndpoint != null) return { movePointDialog = true }
        (selectedWholeGuideId ?: selectedGuideId)?.let { id ->
            val guide = guides.firstOrNull { it.id == id } ?: return@let
            val inShape = closedGuideShapes(guides).any { path -> sameGuideObject(path.first(), guide) }
            return { if (inShape) shapeEditId = id else guideLengthEditId = id }
        }
        (selectedWholeLineId ?: selectedLineId)?.takeIf { id -> lines.any { it.id == id } }?.let { id -> return { selectedLineId = id; showLineEdit = true } }
        return null
    }

    /** Crossing lines are joined right on the canvas; every other junction keeps its dialog. */
    fun onJunction(action: JunctionAction) {
        if (action.kind != JunctionKind.JOIN_GUIDES && action.kind != JunctionKind.JOIN_MEASUREMENTS) { junctionAction = action; return }
        val arms = if (action.kind == JunctionKind.JOIN_MEASUREMENTS) {
            lines.filter { it.id == action.firstId || it.id == action.secondId }.flatMap { line -> listOf(line.start, line.end).filter { distance(it, action.point) >= 0.6 }.map { JunctionArm(line.id, it, measurementColor(line)) } }
        } else {
            guides.filter { it.id == action.firstId || it.id == action.secondId }.flatMap { line -> listOf(line.start, line.end).filter { distance(it, action.point) >= 0.6 }.map { JunctionArm(line.id, it, guideColor(line.colorIndex)) } }
        }
        // Each part follows its line from the crossing to its end, along the curve when the line is curved.
        val paths = arms.map { arm ->
            val guide = if (action.kind == JunctionKind.JOIN_GUIDES) guides.firstOrNull { it.id == arm.lineId } else null
            if (guide?.curveMid == null) listOf(action.point, arm.endpoint) else {
                val t = guideNearestParameter(guide, action.point)
                val to = if (distance(arm.endpoint, guide.start) <= distance(arm.endpoint, guide.end)) 0f else 1f
                (0..24).map { i -> guidePointAt(guide, t + (to - t) * i / 24f) }
            }
        }
        joinPick = JoinPick(action, arms, paths = paths)
    }
    /** Toggles the arm nearest [point]; at most two can be kept. */
    fun pickJoinArm(pick: JoinPick, point: Pt, hitRadius: Double) {
        fun segmentDistance(a: Pt, b: Pt): Double {
            val dx = b.x - a.x; val dy = b.y - a.y
            val t = (((point.x - a.x) * dx + (point.y - a.y) * dy) / (dx * dx + dy * dy).coerceAtLeast(1e-6f)).coerceIn(0f, 1f)
            return distance(point, Pt(a.x + dx * t, a.y + dy * t))
        }
        fun armDistance(i: Int) = (pick.paths.getOrNull(i) ?: listOf(pick.action.point, pick.arms[i].endpoint)).zipWithNext().minOf { (a, b) -> segmentDistance(a, b) }
        val index = pick.arms.indices.minByOrNull(::armDistance) ?: return
        if (armDistance(index) > hitRadius * 2.5) return
        joinPick = pick.copy(selected = if (index in pick.selected) pick.selected - index else if (pick.selected.size < 2) pick.selected + index else pick.selected)
    }

    /** A tap while drawing lands on a nearby line: on its end point when close to one, else on the line itself. */
    fun snapToLines(point: Pt, hitRadius: Double): Pt {
        closestGuideEndpoint(point, pointGuides(guides), hitRadius * 1.2)?.let { e -> guides.firstOrNull { it.id == e.guideId }?.let { return if (e.isStart) it.start else it.end } }
        closestGuide(point, guides, hitRadius * 0.8)?.let { g -> return guidePointAt(g, guideNearestParameter(g, point)) }
        return point
    }
    /** A closed circle: eight curved pieces around [center], through [rim]. */
    fun addCircle(center: Pt, rim: Pt) {
        val radius = distance(center, rim)
        if (radius < 2.0) return
        pushUndo()
        val pathId = nextGuidePathId(guides); val firstId = nextGuideId(guides)
        fun at(angle: Double) = Pt((center.x + radius * cos(angle)).toFloat(), (center.y + radius * sin(angle)).toFloat())
        val pieces = (0 until 8).map { i -> val a = 2 * PI * i / 8; val b = 2 * PI * (i + 1) / 8
            GuideLine(firstId + i, at(a), if (i == 7) at(0.0) else at(b), manual = true, plane = editPlane, pathId = pathId, colorIndex = guideColorIndex, strokeWidth = guideStrokeWidth, curveMid = at((a + b) / 2)) }
        guides = guides + pieces
        // One circle per use: the tool switches off and the circle stays selected (for Material, Grow or Modify),
        // so a tap on it can't start another circle.
        tool = Tool.NONE; selectedGuideId = firstId; selectedWholeGuideId = null; guideDeleteCandidateId = firstId
    }
    /** A closed rectangle from one corner to the opposite one (a square when nearly square). */
    fun addRectangle(corner: Pt, opposite: Pt) {
        val (end, square) = squareEnd(corner, opposite)
        if (abs(end.x - corner.x) < 2f || abs(end.y - corner.y) < 2f) return
        pushUndo()
        val pathId = nextGuidePathId(guides); val firstId = nextGuideId(guides)
        val pts = listOf(corner, Pt(end.x, corner.y), end, Pt(corner.x, end.y))
        guides = guides + pts.indices.map { i -> GuideLine(firstId + i, pts[i], pts[(i + 1) % 4], manual = true, plane = editPlane, pathId = pathId, colorIndex = guideColorIndex, strokeWidth = guideStrokeWidth) }
        tool = Tool.NONE; selectedGuideId = firstId; selectedWholeGuideId = null; guideDeleteCandidateId = firstId
        if (square) logToast(context, "Square", Toast.LENGTH_SHORT).show()
    }
    fun handleTap(point: Pt, hitRadius: Double) {
        if (selectMode) {
            // A point under the finger wins over the line it belongs to.
            allPointsHere().filter { distance(it.second, point) <= hitRadius }.minByOrNull { distance(it.second, point) }?.let { togglePoint(it.first); return }
            itemAt(point, hitRadius)?.let(::togglePicked); return
        }
        joinPick?.let { pickJoinArm(it, point, hitRadius); return }
        solidPick?.let { finishSolidOp(objectAt(point)); return }
        // An imported model: the first tap selects it, the next opens its size and position.
        if (tool == Tool.NONE || tool == Tool.ADJUST) {
            val model = modelAt(point)
            if (model != null && closestGuide(point, guides, hitRadius) == null) { if (selectedModelId == model) modelDialogId = model; selectedModelId = model; return }
            selectedModelId = null
        }
        when (tool) {
            Tool.NONE -> {
                val selected = selectableMeasurement(point, lines, hitRadius)
                if (selected != null) {
                    selectedLineId = selected.id
                    guideDeleteCandidateId = null
                    if (selected.value <= 0.0) { deleteCandidateId = null; showLineEdit = true }
                    else deleteCandidateId = selected.id
                } else {
                    deleteCandidateId = null
                    val guide = closestGuide(point, guides, hitRadius * 1.7)
                    // A new tap replaces the whole selection (Properties and Material then act on this one).
                    selectedWholeGuideId = null
                    selectedGuideId = guide?.id
                    guideDeleteCandidateId = guide?.id
                }
            }
            Tool.ADJUST -> {
                panOnEmptyDrag = false
                // End points of lines from other views can be selected here too.
                selectedGhostPoint = null
                if (!ownGeometryNear(point, hitRadius)) ghostPointAt(point, hitRadius * 1.5)?.let { sel -> clearOwnSelection(); selectedForeign = null; selectedGhostPoint = sel; return }
                if (handleForeignTap(point, hitRadius)) return
                if (selectedVertices.isNotEmpty()) {
                    val nearest = selectedVertices.minByOrNull { distance(point, it) }
                    if (nearest != null && distance(point, nearest) <= hitRadius) {
                        selectedVertices = selectedVertices.filterNot { distance(it, nearest) < 0.6 }
                        return
                    }
                    selectedVertices = emptyList()
                }
                guides.firstOrNull { it.id == selectedGuideId && it.curveMid != null && distance(point, it.curveMid) <= hitRadius * 1.5 }?.let { curve ->
                    selectedCurveId = curve.id; selectedExtrusionId = null; selectedEndpoint = null; selectedGuideEndpoint = null; selectedWholeLineId = null; selectedWholeGuideId = null; selectedLabelId = null
                    return
                }
                selectedCurveId = null
                selectedExtrusionId = null
                val priorLineId = selectedWholeLineId ?: selectedEndpoint?.lineId ?: selectedLineId.takeIf { selectedGuideId == null }
                val priorGuideId = selectedWholeGuideId ?: selectedGuideEndpoint?.guideId ?: selectedGuideId.takeIf { selectedWholeLineId == null && selectedEndpoint == null }
                val preferredEndpoint = priorLineId?.let { id -> closestEndpoint(point, lines.filter { it.id == id }, hitRadius) }
                val preferredGuideEndpoint = priorGuideId?.let { id -> closestGuideEndpoint(point, pointGuides(guides).filter { it.id == id }, hitRadius) }
                val endpoint = preferredEndpoint ?: if (preferredGuideEndpoint == null) closestEndpoint(point, lines, hitRadius) else null
                val guideEndpoint = preferredGuideEndpoint ?: if (preferredEndpoint == null) closestGuideEndpoint(point, pointGuides(guides), hitRadius) else null
                selectedLabelId = null; selectedWholeLineId = null; selectedWholeGuideId = null; selectedGuideEndpoint = null
                if (endpoint != null) { selectedEndpoint = endpoint; selectedLineId = endpoint.lineId; selectedGuideId = null }
                else if (guideEndpoint != null) {
                    selectedEndpoint = null
                    selectedGuideEndpoint = guideEndpoint
                    selectedGuideId = guideEndpoint.guideId
                }
                else {
                    val line = closestLine(point, lines)?.takeIf { distanceToLine(point, it) <= hitRadius * 2.2 }
                    val label = closestLabel(point, lines, hitRadius)
                    selectedEndpoint = null
                    selectedGuideEndpoint = null
                    if (label != null && (line == null || distance(point, labelAnchor(label)) <= distanceToLine(point, line))) {
                        selectedLineId = label.id
                        selectedLabelId = label.id
                    } else if (line != null) {
                        selectedLineId = line.id
                        selectedWholeLineId = line.id
                    } else {
                        val guide = closestGuide(point, guides, hitRadius * 2.2) ?: closedGuideShapes(guides).lastOrNull { pointInsidePolygon(point, shapePoints(it)) }?.firstOrNull()
                        if (guide != null) {
                            selectedGuideId = guide.id
                            selectedWholeGuideId = guide.id
                        } else { selectedGuideId = null; selectedLineId = null; deleteCandidateId = null; guideDeleteCandidateId = null; panOnEmptyDrag = true }
                    }
                }
            }
            Tool.CALIBRATE -> if (draftStart == null) draftStart = point else {
                pendingEnd = point
                showDistanceEntry = true
            }
            Tool.MEASURE -> if (measureDrawing == LineDrawing.BROKEN && draftStart != null) {
                if (distance(draftStart!!, point) > 4.0) {
                    val pathId = activeMeasurePathId ?: return
                    val index = lines.count { it.pathId == pathId } + 1
                    if (unknownSize || pictureOptions.guess && scale != null) {
                        val segment = MeasureLine(nextId(lines), draftStart!!, point, 0.0, false, "", pathId = pathId, segmentIndex = index, lengthSource = if (unknownSize) LengthSource.UNKNOWN else LengthSource.CALCULATED)
                        pushUndo()
                        lines = recalculate(lines + segment)
                        draftStart = point
                    } else {
                        pendingEnd = point; pendingBrokenSegment = true; showDistanceEntry = true
                    }
                }
            } else if (draftStart == null) {
                val selected = selectableMeasurement(point, lines, hitRadius)
                if (selected != null) {
                    selectedLineId = selected.id
                    if (selected.value <= 0.0) { deleteCandidateId = null; showLineEdit = true }
                    else deleteCandidateId = selected.id
                } else {
                    deleteCandidateId = null
                    if (measureDrawing == LineDrawing.BROKEN) activeMeasurePathId = (lines.mapNotNull { it.pathId }.maxOrNull() ?: 0) + 1
                    draftStart = point
                }
            } else {
                pendingEnd = point
                if (unknownSize || pictureOptions.guess && scale != null) {
                    val line = MeasureLine(nextId(lines), draftStart!!, point, if (unknownSize) 0.0 else frontLength(draftStart!!, point, scale!!, axisScale), false, "", defaultColor, lengthSource = if (unknownSize) LengthSource.UNKNOWN else LengthSource.CALCULATED)
                    pushUndo()
                    lines = lines + line
                    selectedLineId = line.id
                    draftStart = null; pendingEnd = null
                } else { pendingBrokenSegment = false; showDistanceEntry = true }
            }
            Tool.LINES -> if (lineDrawing == LineDrawing.BROKEN) {
                val point = snapToLines(point, hitRadius)
                if (draftStart == null) {
                    activePolylineId = nextGuidePathId(guides)
                    draftStart = point
                } else if (distance(draftStart!!, point) > 4.0) {
                    pushUndo()
                    guides = guides + GuideLine(nextGuideId(guides), draftStart!!, point, manual = true, plane = editPlane, pathId = activePolylineId, colorIndex = guideColorIndex, strokeWidth = guideStrokeWidth)
                    draftStart = point
                }
            } else if (draftStart == null) {
                // Drawing: a tap on an existing line starts the new line there; it doesn't select that line.
                selectedGuideId = null
                guideDeleteCandidateId = null
                // A circle's center stays where tapped; other drawings start on the line under the finger.
                draftStart = if (lineDrawing == LineDrawing.CIRCLE) point else snapToLines(point, hitRadius)
            } else {
                val point = snapToLines(point, hitRadius)
                if (distance(draftStart!!, point) > 4.0 && lineDrawing == LineDrawing.CIRCLE) addCircle(draftStart!!, point)
                else if (distance(draftStart!!, point) > 4.0 && lineDrawing == LineDrawing.RECTANGLE) addRectangle(draftStart!!, point)
                else if (distance(draftStart!!, point) > 4.0) {
                    pushUndo()
                    val start = draftStart!!
                    val guide = GuideLine(nextGuideId(guides), start, point, manual = true, plane = editPlane, colorIndex = guideColorIndex, strokeWidth = guideStrokeWidth, curveMid = if (lineDrawing == LineDrawing.CURVE) Pt((start.x + point.x) / 2f, (start.y + point.y) / 2f) else null)
                    guides = guides + guide
                    selectedGuideId = if (lineDrawing == LineDrawing.CURVE) guide.id else null
                }
                draftStart = null; pendingEnd = null
            }
            Tool.CROP -> Unit
        }
    }
    fun deleteMeasurement(id: Int) {
        pushUndo()
        val pathId = lines.firstOrNull { it.id == id }?.pathId
        lines = recalculate(lines.filterNot { it.id == id || pathId != null && it.pathId == pathId })
        selectedLineId = null
        deleteCandidateId = null
    }
    fun deleteGuide(id: Int) {
        pushUndo()
        val selected = guides.firstOrNull { it.id == id }
        guides = guides.filterNot { selected != null && sameGuideObject(it, selected) }
        val remainingIds = guides.map { it.id }.toSet()
        extrusions = extrusions.map { it.copy(guideIds = it.guideIds.filter { guideId -> guideId in remainingIds }) }.filter { it.guideIds.isNotEmpty() }
        selectedExtrusionId = selectedExtrusionId?.takeIf { selected -> extrusions.any { it.id == selected } }
        selectedGuideId = null
        selectedWholeGuideId = null
        guideDeleteCandidateId = null
    }
    fun deleteSelectedGuidePoint() {
        selectedCurveId?.let { id ->
            val guide = guides.firstOrNull { it.id == id && it.curveMid != null } ?: return
            pushUndo(); guides = guides.map { if (it.id == guide.id) it.copy(curveMid = null) else it }; selectedCurveId = null; selectedWholeGuideId = id
            return
        }
        val endpoint = selectedGuideEndpoint ?: return
        val guide = guides.firstOrNull { it.id == endpoint.guideId } ?: return
        val path = guide.pathId?.let { id -> guides.filter { it.pathId == id } } ?: listOf(guide)
        val vertex = if (endpoint.isStart) guide.start else guide.end
        val before = path.firstOrNull { distance(it.end, vertex) < 0.6 }
        val after = path.firstOrNull { distance(it.start, vertex) < 0.6 }
        pushUndo()
        if (before != null && after != null && before.id != after.id) {
            val merged = before.copy(end = after.end, curveMid = null, lengthMm = null, endZMm = after.endZMm, endThicknessMm = after.endThicknessMm)
            guides = guides.mapNotNull { when (it.id) { before.id -> merged; after.id -> null; else -> it } }
            extrusions = extrusions.map { extrusion -> if (after.id in extrusion.guideIds) extrusion.copy(guideIds = (extrusion.guideIds.filterNot { it == after.id } + before.id).distinct()) else extrusion }
            selectedGuideId = merged.id; selectedWholeGuideId = merged.id
        } else {
            guides = guides.filterNot { it.id == guide.id }
            extrusions = extrusions.map { it.copy(guideIds = it.guideIds.filterNot { id -> id == guide.id }) }.filter { it.guideIds.isNotEmpty() }
            selectedGuideId = null; selectedWholeGuideId = null
        }
        selectedGuideEndpoint = null
    }
    fun finishBrokenGuide() {
        draftStart = null
        pendingEnd = null
        activePolylineId = null
        activeMeasurePathId = null
    }
    fun activateExtrusion(point: Pt, radius: Double, targetId: Int? = null) {
        val image = bitmap ?: return
        var guide = (if (targetId != null) guides.firstOrNull { it.id == targetId } else closestGuide(point, guides, radius * 2)) ?: return
        if (guide.plane != editPlane) {
            // A shape drawn in another view becomes this view's shape, extruded along this view's depth.
            val source = guides.filter { sameGuideObject(it, guide) }
            var next = nextGuideId(guides); val path = nextGuidePathId(guides)
            val renamed = source.associate { it.id to next++ }
            guides = guides.filterNot { g -> source.any { it.id == g.id } } + source.map { it.copy(id = renamed.getValue(it.id), plane = editPlane, pathId = if (it.pathId != null) path else null, groupId = null, midDepthMm = null) }
            guide = guides.first { it.id == renamed.getValue(guide.id) }
        }
        val ids = guides.filter { sameGuideObject(it, guide) }.map { it.id }
        val existing = extrusions.firstOrNull { extrusionGuides(it, guides).map { guide -> guide.id }.toSet() == ids.toSet() }
        val selected = existing ?: run {
            val vertices = guides.filter { it.id in ids }.flatMap { listOf(it.start, it.end) }
            val origin = Pt(vertices.map { it.x }.average().toFloat(), vertices.map { it.y }.average().toFloat())
            val vanishing = extrusions.firstOrNull()?.vanishing ?: Pt(image.width * 0.5f, image.height * 0.06f)
            val sun = extrusions.firstOrNull()?.sun ?: Pt(image.width * 0.15f, image.height * 0.08f)
            val handle = Pt(origin.x + (vanishing.x - origin.x) * 0.15f, origin.y + (vanishing.y - origin.y) * 0.15f)
            val draft = Extrusion((extrusions.maxOfOrNull { it.id } ?: 0) + 1, ids, origin, handle, vanishing, sun)
            val extrusion = draft.copy(depthMm = depthMmForHandle(draft, handle, scale, unit))
            pushUndo(); extrusions = extrusions + extrusion; extrusion
        }
        selectedExtrusionId = selected.id; selectedCurveId = null
        selectedEndpoint = null; selectedGuideEndpoint = null; selectedWholeLineId = null; selectedWholeGuideId = null; selectedLabelId = null
    }
    fun addGuidePoint(point: Pt, hitRadius: Double) {
        if (tool != Tool.ADJUST) return
        val selected = selectedGuideId?.let { id -> guides.firstOrNull { it.id == id } }
        val guide = selected?.let { closestGuide(point, guides.filter { candidate -> sameGuideObject(candidate, it) }, hitRadius) }
            ?: closestGuide(point, guides, hitRadius) ?: return
        if (distance(point, guide.start) <= hitRadius || distance(point, guide.end) <= hitRadius) return
        if (pointKind == PointKind.CURVE) {
            if (guide.curveMid == null) { pushUndo(); guides = guides.map { if (it.id == guide.id) it.copy(curveMid = Pt((it.start.x + it.end.x) / 2f, (it.start.y + it.end.y) / 2f)) else it } }
            selectedGuideId = guide.id; selectedCurveId = guide.id; selectedWholeGuideId = null; selectedGuideEndpoint = null; selectedEndpoint = null
            return
        }
        val t = guideNearestParameter(guide, point)
        if (t <= 0.001f || t >= 0.999f) return
        val vertex = guidePointAt(guide, t)
        val newId = nextGuideId(guides); val path = guide.pathId ?: nextGuidePathId(guides)
        val splitZ = guide.startZMm * (1 - t) + guide.endZMm * t
        val defaultDepth = extrusions.firstOrNull { ex -> extrusionGuides(ex, guides).any { it.id == guide.id } }?.depthMm
        val splitThickness = if (guide.startThicknessMm == null && guide.endThicknessMm == null || defaultDepth == null) null else (guide.startThicknessMm ?: defaultDepth) * (1 - t) + (guide.endThicknessMm ?: defaultDepth) * t
        val first = guide.copy(end = vertex, pathId = path, curveMid = guide.curveMid?.let { guidePointAt(guide, t / 2f) }, lengthMm = null, endZMm = splitZ, endThicknessMm = splitThickness)
        val second = guide.copy(id = newId, start = vertex, pathId = path, curveMid = guide.curveMid?.let { guidePointAt(guide, (t + 1f) / 2f) }, lengthMm = null, startZMm = splitZ, startThicknessMm = splitThickness)
        pushUndo(); guides = guides.flatMap { if (it.id == guide.id) listOf(first, second) else listOf(it) }
        extrusions = extrusions.map { if (guide.id in it.guideIds) it.copy(guideIds = it.guideIds + newId) else it }
        selectedGuideId = guide.id; selectedCurveId = null; selectedGuideEndpoint = GuideEndpoint(guide.id, false); selectedWholeGuideId = null; selectedEndpoint = null
    }
    /**
     * A selected circle in Modify gets four handles on its rim: left and right set its width, top and bottom its
     * height (the center stays). Different width and height make an oval.
     */
    fun ovalPieces(): List<GuideLine>? {
        if (tool != Tool.ADJUST || selectMode) return null
        val guide = (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } } ?: return null
        return circleOf(guide, guides)
    }
    fun ovalHandles(): List<Pair<Pt, Boolean>> {
        val pieces = ovalPieces() ?: return emptyList()
        val pts = pieces.map { it.start }
        val minX = pts.minOf { it.x }; val maxX = pts.maxOf { it.x }; val minY = pts.minOf { it.y }; val maxY = pts.maxOf { it.y }
        val cx = (minX + maxX) / 2f; val cy = (minY + maxY) / 2f
        return listOf(Pt(maxX, cy) to true, Pt(minX, cy) to true, Pt(cx, minY) to false, Pt(cx, maxY) to false)
    }
    // While a rim handle is dragged: the pieces as they were, the center, the half size then, and which way it stretches.
    var ovalDrag by remember { mutableStateOf<Triple<List<GuideLine>, Pt, Pair<Float, Boolean>>?>(null) }
    // Growing a shape by its arrows; the functions are set further down, where everything they use is defined.
    var growDrag by remember { mutableStateOf<GrowDrag?>(null) }
    // Picking something else or leaving Modify turns Grow off.
    LaunchedEffect(selectedWholeGuideId, selectedGuideId, selectedForeign, tool, selectMode, editPlane) { if (growDrag == null) growMode = false }
    var growStart: ((Pt, Double) -> Boolean)? = null
    var growMove: ((Pt) -> Unit)? = null
    fun handleDragStart(sourcePoint: Pt, hitRadius: Double) {
        if (selectMode) {
            // Dragging a picked item moves all of them; dragging elsewhere draws a box to pick items.
            val hit = itemAt(sourcePoint, hitRadius)
            val onPoint = pickedPoints.any { sel -> ghostPointHere(sel)?.let { distance(it, sourcePoint) <= hitRadius * 1.5 } == true }
            if (onPoint || hit != null && pickedItems.any { sameItem(hit, it) }) { itemDragLast = sourcePoint; pushUndo() } else { itemRectStart = sourcePoint; itemRectEnd = sourcePoint }
            return
        }
        if (tool == Tool.ADJUST && !selectMode && growStart?.invoke(sourcePoint, hitRadius) == true) return
        if (tool == Tool.ADJUST && !selectMode) ovalPieces()?.let { pieces ->
            ovalHandles().firstOrNull { distance(it.first, sourcePoint) <= hitRadius * 1.3 }?.let { (at, horizontal) ->
                val pts = pieces.map { it.start }
                val center = Pt((pts.minOf { it.x } + pts.maxOf { it.x }) / 2f, (pts.minOf { it.y } + pts.maxOf { it.y }) / 2f)
                val half = if (horizontal) abs(at.x - center.x) else abs(at.y - center.y)
                if (half > 0.5f) { pushUndo(); ovalDrag = Triple(pieces, center, half to horizontal); return }
            }
        }
        // Rectangle and Circle can be dragged out: from a corner (or the center) to where the finger lifts.
        if (tool == Tool.LINES && draftStart == null && (lineDrawing == LineDrawing.RECTANGLE || lineDrawing == LineDrawing.CIRCLE) && closestGuideEndpoint(sourcePoint, pointGuides(guides), hitRadius * 1.5) == null) {
            shapeDragging = true; draftStart = if (lineDrawing == LineDrawing.CIRCLE) sourcePoint else snapToLines(sourcePoint, hitRadius); pendingEnd = draftStart; return
        }
        // Dragging the selected imported model moves it within this view.
        if (selectedModelId != null && modelAt(sourcePoint) == selectedModelId) { pushUndo(); modelDragLast = sourcePoint; return }
        if (tool == Tool.ADJUST && !ownGeometryNear(sourcePoint, hitRadius)) ghostPointAt(sourcePoint, hitRadius * 1.5)?.let { sel ->
            clearOwnSelection(); selectedForeign = null; selectedGhostPoint = sel; ghostDragLast = sourcePoint; pushUndo(); return
        }
        if (tool == Tool.ADJUST && startForeignDrag(sourcePoint, hitRadius)) return
        // Lines and Measure: an existing end point under the finger is moved, not drawn from.
        if (tool == Tool.LINES && draftStart == null) closestGuideEndpoint(sourcePoint, pointGuides(guides), hitRadius * 1.5)?.let { e ->
            val guide = guides.firstOrNull { it.id == e.guideId } ?: return@let
            selectedGuideEndpoint = e; selectedGuideId = e.guideId; activeGuideEndpoint = e; activeGuideVertexPoint = if (e.isStart) guide.start else guide.end; pushUndo(); return
        }
        if (tool == Tool.MEASURE && draftStart == null) closestEndpoint(sourcePoint, lines, hitRadius * 1.5)?.let { e -> selectedEndpoint = e; selectedLineId = e.lineId; activePoint = e; pushUndo(); return }
        if (tool == Tool.ADJUST && (selectedVertices.any { distance(sourcePoint, it) <= hitRadius * 1.5 } || selectedGhostPoints.any { sel -> ghostPointHere(sel)?.let { distance(sourcePoint, it) <= hitRadius * 1.5 } == true })) {
            groupDragLast = sourcePoint; pushUndo(); return
        }
        if (tool == Tool.ADJUST) {
            // Corners can be dragged straight away, without tapping them first.
            val heldVertex = selectedGuideEndpoint?.let { e -> guides.firstOrNull { it.id == e.guideId }?.let { if (e.isStart) it.start else it.end } }
            val heldPoint = selectedEndpoint?.let { e -> lines.firstOrNull { it.id == e.lineId }?.let { if (e.isStart) it.start else it.end } }
            if ((heldVertex == null || distance(sourcePoint, heldVertex) > hitRadius) && (heldPoint == null || distance(sourcePoint, heldPoint) > hitRadius)) {
                closestGuideEndpoint(sourcePoint, pointGuides(guides), hitRadius)?.let { e -> clearOwnSelection(); selectedForeign = null; selectedGuideEndpoint = e; selectedGuideId = e.guideId }
                    ?: closestEndpoint(sourcePoint, lines, hitRadius)?.let { e -> clearOwnSelection(); selectedForeign = null; selectedEndpoint = e; selectedLineId = e.lineId }
            }
        }
        if (tool == Tool.ADJUST && selectedWholeGuideId != null) {
            val selected = guides.firstOrNull { it.id == selectedWholeGuideId }
            val objectGuides = selected?.let { guide -> guides.filter { sameGuideObject(it, guide) } }.orEmpty()
            val shape = closedGuideShapes(guides).firstOrNull { path -> selected != null && sameGuideObject(path.first(), selected) }
            if (objectGuides.any { distance(sourcePoint, projectToGuide(sourcePoint, it)) <= hitRadius * 1.7 } || shape != null && pointInsidePolygon(sourcePoint, shapePoints(shape))) {
                activeWholeGuideId = selectedWholeGuideId; activeWholeGuideLastPoint = sourcePoint; pushUndo(); return
            }
        }
        if (tool == Tool.ADJUST && closestEndpoint(sourcePoint, lines, hitRadius) == null && closestGuideEndpoint(sourcePoint, pointGuides(guides), hitRadius) == null && closestLine(sourcePoint, lines)?.let { distanceToLine(sourcePoint, it) <= hitRadius } != true && closestGuide(sourcePoint, guides, hitRadius) == null && selectedExtrusionId == null) {
            selectionStart = sourcePoint; selectionEnd = sourcePoint; return
        }
        if (tool == Tool.LINES || tool == Tool.ADJUST) {
            val curve = guides.firstOrNull { it.id == selectedGuideId && it.curveMid != null && distance(sourcePoint, it.curveMid) <= hitRadius * 1.5 }
            if (curve != null) { activeCurveId = curve.id; if (tool == Tool.ADJUST) { selectedCurveId = curve.id; selectedExtrusionId = null; selectedGuideEndpoint = null; selectedWholeGuideId = null }; curveDragLast = sourcePoint; pushUndo(); return }
        }
        if (tool != Tool.ADJUST) return
        selectedEndpoint?.let { endpoint ->
            val line = lines.firstOrNull { it.id == endpoint.lineId } ?: return
            if (distance(sourcePoint, if (endpoint.isStart) line.start else line.end) <= hitRadius * 2) { activePoint = endpoint; pushUndo() }
            return
        }
        selectedGuideEndpoint?.let { endpoint ->
            val guide = guides.firstOrNull { it.id == endpoint.guideId } ?: return
            val vertex = if (endpoint.isStart) guide.start else guide.end
            if (distance(sourcePoint, vertex) <= hitRadius * 2) { activeGuideEndpoint = endpoint; activeGuideVertexPoint = vertex; pushUndo() }
            return
        }
        selectedWholeLineId?.let { id ->
            val line = lines.firstOrNull { it.id == id } ?: return
            if (distanceToLine(sourcePoint, line) <= hitRadius * 2) { activeWholeLineId = id; activeWholeLineLastPoint = sourcePoint; pushUndo() }
            return
        }
        selectedWholeGuideId?.let { id ->
            val guide = guides.firstOrNull { it.id == id } ?: return
            if (distance(sourcePoint, projectToGuide(sourcePoint, guide)) <= hitRadius * 2) { activeWholeGuideId = id; activeWholeGuideLastPoint = sourcePoint; pushUndo() }
            return
        }
    }
    fun handleLongPress(point: Pt, hitRadius: Double) {
        selectedCurveId = null
        if (tool == Tool.LINES) {
            closestGuide(point, guides, hitRadius * 1.7)?.let { guide -> selectedGuideId = guide.id; guideDeleteCandidateId = guide.id }
            return
        }
        closestLine(point, lines)?.takeIf { distanceToLine(point, it) <= hitRadius * 1.7 }?.let { line ->
            selectedLineId = line.id
            if (tool == Tool.ADJUST) {
                selectedEndpoint = null
                selectedLabelId = null
                selectedWholeLineId = line.id
            } else showLineEdit = true
        }
        if (tool == Tool.ADJUST && selectedWholeLineId == null) {
            closestGuide(point, guides, hitRadius * 1.7)?.let { guide ->
                selectedEndpoint = null; selectedGuideEndpoint = null; selectedLabelId = null
                selectedGuideId = guide.id; selectedWholeGuideId = guide.id
            }
        }
    }
    fun applyJunction(action: JunctionAction, chosenArms: List<JunctionArm> = emptyList(), keepRemaining: Boolean = false) {
        pushUndo()
        when (action.kind) {
            JunctionKind.CLOSE_GUIDE_PATH -> {
                val first = guides.firstOrNull { it.id == action.firstId } ?: return
                val last = guides.firstOrNull { it.id == action.secondId } ?: return
                val path = first.pathId ?: return
                if (last.pathId != path || first.id == last.id) return
                fun shifted(mid: Pt?, old: Pt) = mid?.let { Pt(it.x + (action.point.x - old.x) / 2f, it.y + (action.point.y - old.y) / 2f) }
                guides = guides.map { guide -> when (guide.id) {
                    first.id -> guide.copy(start = action.point, curveMid = shifted(guide.curveMid, guide.start))
                    last.id -> guide.copy(end = action.point, curveMid = shifted(guide.curveMid, guide.end))
                    else -> guide
                } }
            }
            JunctionKind.SHAPE_OVERLAP, JunctionKind.CUT_SHAPE_LINE -> return
            JunctionKind.MERGE_SHAPE_GUIDE -> {
                val shape = closedGuideShapes(guides).firstOrNull { path -> path.any { it.id == action.firstId } } ?: return
                val first = guides.firstOrNull { it.id == action.secondId } ?: return
                val chain = guides.filter { it.id == first.id || first.pathId != null && it.pathId == first.pathId }
                val a = shapeAttachment(shape, chain.first().start) ?: return
                val b = shapeAttachment(shape, chain.last().end) ?: return
                val oldIds = (shape + chain).map { it.id }.toSet()
                val existingGroups = (shape + chain).mapNotNull { it.groupId }.toSet()
                val allIds = guides.filter { it.id in oldIds || it.groupId != null && it.groupId in existingGroups }.map { it.id }.toSet()
                val groupId = existingGroups.firstOrNull() ?: ((guides.mapNotNull { it.groupId }.filter { it > 0 }.maxOrNull() ?: 0) + 1)
                var nextId = nextGuideId(guides)
                val splitShape = shape.flatMap { segment ->
                    val cuts = listOf(a, b).filter { it.borderId == segment.id }.map { it.parameter }.filter { it > 0.0001f && it < 0.9999f }.sorted().fold(mutableListOf<Float>()) { acc, t -> if (acc.isEmpty() || t - acc.last() > 0.0001f) acc.add(t); acc }
                    val stops = listOf(0f) + cuts + listOf(1f)
                    stops.zipWithNext().mapIndexed { index, (from, to) -> segment.copy(id = if (index == 0) segment.id else nextId++, start = guidePointAt(segment, from), end = guidePointAt(segment, to), curveMid = if (segment.curveMid != null) guidePointAt(segment, (from + to) / 2f) else null, groupId = groupId) }
                }
                val attached = chain.mapIndexed { index, guide ->
                    val start = if (index == 0) a.point else guide.start
                    val end = if (index == chain.lastIndex) b.point else guide.end
                    val midpoint = guide.curveMid?.let { Pt(it.x + ((start.x - guide.start.x) + (end.x - guide.end.x)) / 2f, it.y + ((start.y - guide.start.y) + (end.y - guide.end.y)) / 2f) }
                    guide.copy(start = start, end = end, curveMid = midpoint, groupId = groupId, colorIndex = shape.first().colorIndex)
                }
                val replacements = (splitShape + attached).associateBy { it.id }
                val shapeIds = shape.map { it.id }.toSet()
                val other = guides.filterNot { it.id in shapeIds }.map { guide -> replacements[guide.id] ?: if (guide.id in allIds) guide.copy(groupId = groupId, colorIndex = shape.first().colorIndex) else guide }
                guides = other + splitShape
                val mergedIds = guides.filter { it.groupId == groupId }.map { it.id }
                val affected = extrusions.filter { e -> e.guideIds.any { it in allIds } }
                if (affected.isNotEmpty()) {
                    val kept = affected.first().copy(guideIds = mergedIds)
                    extrusions = extrusions.filterNot { it in affected } + kept
                    selectedExtrusionId = selectedExtrusionId?.let { if (affected.any { e -> e.id == it }) kept.id else it }
                }
                selectedGuideId = shape.first().id
            }
            JunctionKind.JOIN_GUIDE_ENDPOINTS -> {
                val first = guides.firstOrNull { it.id == action.firstId } ?: return
                val second = guides.firstOrNull { it.id == action.secondId } ?: return
                fun chain(item: GuideLine) = guides.filter { it.id == item.id || item.pathId != null && it.pathId == item.pathId }
                fun reverse(items: List<GuideLine>) = items.asReversed().map { it.copy(start = it.end, end = it.start, startZMm = it.endZMm, endZMm = it.startZMm, startThicknessMm = it.endThicknessMm, endThicknessMm = it.startThicknessMm) }
                var a = chain(first); var b = chain(second)
                if (distance(a.first().start, action.point) < distance(a.last().end, action.point)) a = reverse(a)
                if (distance(b.last().end, action.point) < distance(b.first().start, action.point)) b = reverse(b)
                fun movedMid(mid: Pt?, old: Pt) = mid?.let { Pt(it.x + (action.point.x - old.x) / 2f, it.y + (action.point.y - old.y) / 2f) }
                a = a.dropLast(1) + a.last().let { it.copy(end = action.point, curveMid = movedMid(it.curveMid, it.end)) }
                b = listOf(b.first().let { it.copy(start = action.point, curveMid = movedMid(it.curveMid, it.start)) }) + b.drop(1)
                val path = nextGuidePathId(guides)
                val ids = (a + b).map { it.id }.toSet()
                guides = guides.filterNot { it.id in ids } + (a + b).map { it.copy(pathId = path) }
            }
            JunctionKind.JOIN_MEASURE_ENDPOINTS -> {
                val first = lines.firstOrNull { it.id == action.firstId } ?: return
                val second = lines.firstOrNull { it.id == action.secondId } ?: return
                fun chain(item: MeasureLine) = lines.filter { it.id == item.id || item.pathId != null && it.pathId == item.pathId }.sortedBy { it.segmentIndex }
                fun reverse(items: List<MeasureLine>) = items.asReversed().map { it.copy(start = it.end, end = it.start, startZMm = it.endZMm, endZMm = it.startZMm) }
                var a = chain(first); var b = chain(second)
                if (distance(a.first().start, action.point) < distance(a.last().end, action.point)) a = reverse(a)
                if (distance(b.last().end, action.point) < distance(b.first().start, action.point)) b = reverse(b)
                a = a.dropLast(1) + a.last().copy(end = action.point)
                b = listOf(b.first().copy(start = action.point)) + b.drop(1)
                val path = (lines.mapNotNull { it.pathId }.maxOrNull() ?: 0) + 1
                val ids = (a + b).map { it.id }.toSet()
                lines = recalculate(lines.filterNot { it.id in ids } + (a + b).mapIndexed { index, line -> line.copy(pathId = path, segmentIndex = index + 1) })
            }
            JunctionKind.SPLIT_MEASUREMENT -> {
                val first = lines.first { it.id == action.firstId }
                val path = first.pathId ?: return
                val segments = lines.filter { it.pathId == path }.sortedBy { it.segmentIndex }
                val cut = segments.indexOfFirst { it.id == action.secondId }
                if (cut <= 0) return
                val before = segments.take(cut); val after = segments.drop(cut)
                val newPath = (lines.mapNotNull { it.pathId }.maxOrNull() ?: 0) + 1
                val replacements = (before.mapIndexed { index, item -> item.id to item.copy(pathId = if (before.size == 1) null else path, segmentIndex = if (before.size == 1) 0 else index + 1) } +
                    after.mapIndexed { index, item -> item.id to item.copy(pathId = if (after.size == 1) null else newPath, segmentIndex = if (after.size == 1) 0 else index + 1) }).toMap()
                lines = lines.map { replacements[it.id] ?: it }
            }
            JunctionKind.SPLIT_GUIDE -> {
                val first = guides.first { it.id == action.firstId }
                val path = first.pathId ?: return
                val segments = guides.filter { it.pathId == path }
                val cut = segments.indexOfFirst { it.id == action.secondId }
                if (cut <= 0) return
                val before = segments.take(cut); val after = segments.drop(cut)
                val newPath = nextGuidePathId(guides)
                val replacements = (before.map { it.id to it.copy(pathId = if (before.size == 1) null else path) } + after.map { it.id to it.copy(pathId = if (after.size == 1) null else newPath) }).toMap()
                guides = guides.map { replacements[it.id] ?: it }
            }
            JunctionKind.JOIN_MEASUREMENTS -> {
                val originals = listOfNotNull(lines.firstOrNull { it.id == action.firstId }, lines.firstOrNull { it.id == action.secondId })
                if (originals.size != 2 || originals.any { it.isReference } || chosenArms.size != 2) return
                var next = nextId(lines)
                var nextPath = (lines.mapNotNull { it.pathId }.maxOrNull() ?: 0) + 1
                val removedIds = originals.map { it.id }.toSet()
                val affectedPaths = originals.mapNotNull { it.pathId }.toSet()
                val replacements = mutableMapOf<Int, MeasureLine>()
                affectedPaths.forEach { pathId ->
                    val segments = lines.filter { it.pathId == pathId }.sortedBy { it.segmentIndex }
                    val runs = mutableListOf<MutableList<MeasureLine>>()
                    segments.forEach { segment ->
                        if (segment.id !in removedIds) {
                            if (runs.isEmpty() || runs.last().lastOrNull()?.let { distance(it.end, segment.start) >= 0.6 } != false) runs.add(mutableListOf())
                            runs.last().add(segment)
                        } else if (runs.lastOrNull()?.isNotEmpty() == true) runs.add(mutableListOf())
                    }
                    runs.filter { it.isNotEmpty() }.forEach { run ->
                        val id = if (run.size > 1) nextPath++ else null
                        run.forEachIndexed { index, segment -> replacements[segment.id] = segment.copy(pathId = id, segmentIndex = if (id == null) 0 else index + 1) }
                    }
                }
                val newPath = nextPath++
                val used = mutableSetOf<Int>()
                val joined = chosenArms.mapIndexed { index, arm ->
                    val original = originals.first { it.id == arm.lineId }
                    val cut = distance(original.start, action.point) >= 0.6 && distance(original.end, action.point) >= 0.6
                    val id = if (used.add(original.id)) original.id else next++
                    original.copy(id = id, start = if (index == 0) arm.endpoint else action.point, end = if (index == 0) action.point else arm.endpoint, pathId = newPath, segmentIndex = index + 1, value = if (cut) 0.0 else original.value, isScaleReference = if (cut) false else original.isScaleReference, lengthSource = if (cut) LengthSource.UNKNOWN else original.lengthSource)
                }
                val remaining = if (keepRemaining) originals.flatMap { original ->
                    listOf(original.start, original.end).filter { endpoint ->
                        distance(endpoint, action.point) >= 0.6 && chosenArms.none { it.lineId == original.id && it.endpoint == endpoint }
                    }.map { endpoint ->
                        val cut = distance(original.start, action.point) >= 0.6 && distance(original.end, action.point) >= 0.6
                        original.copy(id = next++, start = action.point, end = endpoint, pathId = null, segmentIndex = 0, value = if (cut) 0.0 else original.value, isScaleReference = if (cut) false else original.isScaleReference, lengthSource = if (cut) LengthSource.UNKNOWN else original.lengthSource)
                    }
                } else emptyList()
                lines = lines.filterNot { it.id in removedIds }.map { replacements[it.id] ?: it } + joined + remaining
                scale = scaleFor(lines, editPlane)
                axisScale = if (editPlane != ModelPlane.FRONT) null else axisScaleOf(lines, unit)
            }
            JunctionKind.JOIN_GUIDES -> {
                val originals = listOfNotNull(guides.firstOrNull { it.id == action.firstId }, guides.firstOrNull { it.id == action.secondId })
                if (originals.size != 2 || chosenArms.size != 2) return
                var next = nextGuideId(guides)
                var nextPath = nextGuidePathId(guides)
                val removedIds = originals.map { it.id }.toSet()
                val affectedPaths = originals.mapNotNull { it.pathId }.toSet()
                val replacements = mutableMapOf<Int, GuideLine>()
                affectedPaths.forEach { pathId ->
                    val runs = mutableListOf<MutableList<GuideLine>>()
                    guides.filter { it.pathId == pathId }.forEach { segment ->
                        if (segment.id !in removedIds) {
                            if (runs.isEmpty() || runs.last().lastOrNull()?.let { distance(it.end, segment.start) >= 0.6 } != false) runs.add(mutableListOf())
                            runs.last().add(segment)
                        } else if (runs.lastOrNull()?.isNotEmpty() == true) runs.add(mutableListOf())
                    }
                    runs.filter { it.isNotEmpty() }.forEach { run ->
                        val id = if (run.size > 1) nextPath++ else null
                        run.forEach { segment -> replacements[segment.id] = segment.copy(pathId = id) }
                    }
                }
                val newPath = nextPath++
                val used = mutableSetOf<Int>()
                fun armPart(original: GuideLine, endpoint: Pt, fromEndpoint: Boolean, id: Int, path: Int?): GuideLine {
                    val t = guideNearestParameter(original, action.point)
                    val atStart = distance(endpoint, original.start) <= distance(endpoint, original.end)
                    val from = if (atStart) 0f else t
                    val to = if (atStart) t else 1f
                    val mid = if (original.curveMid != null) guidePointAt(original, (from + to) / 2f) else null
                    return original.copy(id = id, start = if (fromEndpoint) endpoint else action.point, end = if (fromEndpoint) action.point else endpoint, curveMid = mid, pathId = path)
                }
                val joined = chosenArms.mapIndexed { index, arm ->
                    val original = originals.first { it.id == arm.lineId }
                    val id = if (used.add(original.id)) original.id else next++
                    armPart(original, arm.endpoint, index == 0, id, newPath)
                }
                val remaining = if (keepRemaining) originals.flatMap { original ->
                    listOf(original.start, original.end).filter { endpoint ->
                        distance(endpoint, action.point) >= 0.6 && chosenArms.none { it.lineId == original.id && it.endpoint == endpoint }
                    }.map { endpoint -> armPart(original, endpoint, false, next++, null) }
                } else emptyList()
                guides = guides.filterNot { it.id in removedIds }.map { replacements[it.id] ?: it } + joined + remaining
                extrusions = extrusions.filterNot { extrusion -> extrusion.guideIds.any { it in removedIds } }
            }
        }
        selectedLineId = null; selectedGuideId = null; selectedEndpoint = null; selectedGuideEndpoint = null
        selectedWholeLineId = null; selectedWholeGuideId = null; deleteCandidateId = null; guideDeleteCandidateId = null
        junctionAction = null
    }
    fun finishJoin(pick: JoinPick) {
        if (pick.selected.size != 2) return
        joinPick = null
        applyJunction(pick.action, pick.selected.sorted().map { pick.arms[it] }, pick.keepRemaining)
    }
    fun applyShapeOperation(action: JunctionAction, operation: String) {
        val shapes = closedGuideShapes(guides)
        val first = shapes.firstOrNull { shape -> shape.any { it.id == action.firstId } } ?: return
        val second = shapes.firstOrNull { shape -> shape.any { it.id == action.secondId } }
        val cutter = guides.firstOrNull { it.id == action.secondId }
        val target = if (operation == "cut-first") second else first
        val razor = if (operation == "cut-first") first else second
        // A cutter lying wholly inside the shape makes a hole in it (a ring from two circles, say): the cutter stays
        // as the hole's outline, grouped with the shape, and the hole goes through it in 3D.
        if ((operation == "cut-first" || operation == "cut-second") && target != null && razor != null && shapePoints(razor).all { pointInsidePolygon(it, shapePoints(target)) }) {
            pushUndo()
            val holeIds = razor.map { it.id }.toSet()
            val z = target.map { (it.startZMm + it.endZMm) / 2 }.average()
            guides = guides.map { if (it.id in holeIds) it.copy(hole = true, fillTransparency = null, materialId = null, startZMm = z, endZMm = z) else it }
            val gone = extrusions.filter { e -> e.guideIds.any { it in holeIds } }.map { editPlane to it.id }.toSet()
            extrusions = extrusions.filterNot { e -> e.guideIds.any { it in holeIds } }
            solidOps = solidOps.filterNot { it.target in gone || it.tool in gone }
            logToast(context, "Cut a hole: the inner shape is now a hole that goes through in 3D", Toast.LENGTH_SHORT).show()
            return
        }
        val result = when (operation) {
            "merge" -> second?.let { shapeBoolean(first, it, android.graphics.Path.Op.UNION) }
            "cut-first", "cut-second" -> if (target != null && razor != null) shapeBoolean(target, razor, android.graphics.Path.Op.DIFFERENCE) else null
            "line" -> if (cutter != null) cutShapeByLine(first, cutter, bitmap?.width ?: 1, bitmap?.height ?: 1) else null
            else -> null
        }
        if (result.isNullOrEmpty()) {
            logToast(context, "Cannot create editable shapes from this cut.", Toast.LENGTH_LONG).show()
            return
        }
        val removed = if (operation == "merge") first + second.orEmpty() else target ?: return
        var id = nextGuideId(guides)
        var pathId = nextGuidePathId(guides)
        val style = if (operation == "merge") first.first() else target!!.first()
        val newShapes = result.map { contour ->
            val path = pathId++
            val sources = if (operation == "merge") first + second.orEmpty() else target!!
            val curves = sources.filter { it.curveMid != null }
            val edges = contour.indices.map { i ->
                val a = contour[i]; val b = contour[(i + 1) % contour.size]
                curves.minByOrNull { curve -> distance(a, projectToGuide(a, curve)) + distance(b, projectToGuide(b, curve)) }
                    ?.takeIf { curve -> distance(a, projectToGuide(a, curve)) < 0.8 && distance(b, projectToGuide(b, curve)) < 0.8 }
            }
            val segments = mutableListOf<GuideLine>()
            var i = 0
            while (i < contour.size) {
                val curve = edges[i]
                var end = i + 1
                if (curve != null) while (end < contour.size && edges[end]?.id == curve.id) end++
                val startPoint = contour[i]; val endPoint = contour[end % contour.size]
                val mid = curve?.let { guidePointAt(it, (guideNearestParameter(it, startPoint) + guideNearestParameter(it, endPoint)) / 2f) }
                segments.add(style.copy(id = id++, start = startPoint, end = endPoint, curveMid = mid, pathId = path, groupId = null))
                i = end
            }
            segments
        }
        val removedIds = removed.map { it.id }.toSet()
        val affected = extrusions.filter { e -> e.guideIds.any { it in removedIds } }
        pushUndo()
        guides = guides.filterNot { it.id in removedIds } + newShapes.flatten()
        extrusions = extrusions.filterNot { it in affected } + affected.flatMapIndexed { index, extrusion ->
            newShapes.mapIndexed { shapeIndex, shape -> extrusion.copy(id = if (index == 0 && shapeIndex == 0) extrusion.id else (extrusions.maxOfOrNull { it.id } ?: 0) + 1 + index * newShapes.size + shapeIndex, guideIds = shape.map { it.id }) }
        }
        selectedGuideId = null; selectedWholeGuideId = null; selectedGuideEndpoint = null; selectedExtrusionId = null
        junctionAction = null
    }
    fun updateAlignment(alignment: Set<Int>): Boolean {
        val newlyAligned = alignment - alignedSegments
        alignedSegments = alignment
        if (newlyAligned.isNotEmpty()) {
            alignmentLineIds = newlyAligned
            alignmentFlashToken++
            return true
        }
        return false
    }
    fun transformSelectedVertices(transform: (Pt) -> Pt) {
        val old = selectedVertices
        val moved = old.map(transform)
        fun movedPoint(point: Pt): Pt = old.indexOfFirst { distance(point, it) < 0.6 }.let { if (it >= 0) moved[it] else point }
        val before = lines.associateBy { it.id }
        lines = recalculate(lines.map { it.copy(start = movedPoint(it.start), end = movedPoint(it.end)) }).map { line ->
            val previous = before[line.id]
            if (previous != null && (previous.lengthSource == LengthSource.MANUAL || previous.lengthSource == LengthSource.MEASURED)) line.copy(value = previous.value) else line
        }
        guides = guides.map { guide ->
            val start = movedPoint(guide.start); val end = movedPoint(guide.end)
            val mid = guide.curveMid?.let { Pt(it.x + ((start.x - guide.start.x) + (end.x - guide.end.x)) / 2f, it.y + ((start.y - guide.start.y) + (end.y - guide.end.y)) / 2f) }
            guide.copy(start = start, end = end, curveMid = mid)
        }
        selectedVertices = moved
    }
    fun translateGuideObject(id: Int, dx: Float, dy: Float) {
        val selected = guides.firstOrNull { it.id == id } ?: return
        // The shape and its holes, as they show in this view.
        val key = homeKey(selected)
        val ids = guides.filter { sameGuideObject(it, selected) }.map { it.id }.toSet() + itemGuides(key).map { if (key.first == editPlane) it.id else encodedId(key.first, it.id) }
        fun move(p: Pt) = Pt(p.x + dx, p.y + dy)
        guides = guides.map { guide -> if (guide.id in ids) guide.copy(start = move(guide.start), end = move(guide.end), curveMid = guide.curveMid?.let(::move)) else guide }
        extrusions = extrusions.map { e -> if (e.guideIds.any { it in ids }) { val moved = e.copy(origin = move(e.origin), handle = move(e.handle)); moved.depthMm?.let { extrusionWithDepth(moved, it, scale, unit) } ?: moved } else e }
    }
    fun alignmentReferences(): List<AlignmentReference> {
        fun selectedPoint(a: Pt, b: Pt) = selectedVertices.firstOrNull { p -> distance(p, a) < 0.6 || distance(p, b) < 0.6 }
        val measurements = lines.mapNotNull { line ->
            val point = selectedPoint(line.start, line.end)
            if (point != null && line.value > 0 && (line.isReference && line.isScaleReference || line.lengthSource == LengthSource.MANUAL || line.lengthSource == LengthSource.MEASURED)) AlignmentReference(false, line.id, point, "${if (line.isReference) "Calibration" else "Measurement"} ${line.id}: ${formatMeasure(line.value, unit)} $unit") else null
        }
        val guideReferences = guides.mapNotNull { guide ->
            val point = selectedPoint(guide.start, guide.end); val mm = guide.lengthMm
            if (point != null && mm != null) AlignmentReference(true, guide.id, point, "Line ${guide.id}: ${formatMeasure(mm / unitFactorMm(unit), unit)} $unit") else null
        }
        return measurements + guideReferences
    }
    fun selectedAlignmentObjects(): List<SelectedAlignmentObject> {
        fun contains(point: Pt) = selectedVertices.any { distance(it, point) < 0.6 }
        val guideObjects = guides.groupBy { guide -> guide.groupId?.let { "g$it" } ?: guide.pathId?.let { "p$it" } ?: "i${guide.id}" }.values.mapNotNull { group ->
            val endpoints = group.flatMap { listOf(it.start, it.end) }
            if (endpoints.all(::contains)) SelectedAlignmentObject(group.map { it.id }.toSet(), emptySet(), group.flatMap(::guidePoints)) else null
        }
        val lineObjects = lines.groupBy { line -> line.pathId?.let { "p$it" } ?: "i${line.id}" }.values.mapNotNull { group ->
            val endpoints = group.flatMap { listOf(it.start, it.end) }
            if (endpoints.all(::contains)) SelectedAlignmentObject(emptySet(), group.map { it.id }.toSet(), endpoints) else null
        }
        return guideObjects + lineObjects
    }
    fun alignSelectedObjects(horizontal: Boolean, objects: List<SelectedAlignmentObject>) {
        fun center(item: SelectedAlignmentObject) = if (horizontal) (item.points.minOf { it.y } + item.points.maxOf { it.y }) / 2f else (item.points.minOf { it.x } + item.points.maxOf { it.x }) / 2f
        val target = objects.map(::center).average().toFloat()
        val shifts = objects.map { target - center(it) }
        val originalGuides = guides
        val originalLines = lines
        fun move(point: Pt, offset: Float) = if (horizontal) Pt(point.x, point.y + offset) else Pt(point.x + offset, point.y)
        pushUndo()
        guides = guides.map { guide ->
            val index = objects.indexOfFirst { guide.id in it.guideIds }
            if (index < 0) guide else guide.copy(start = move(guide.start, shifts[index]), end = move(guide.end, shifts[index]), curveMid = guide.curveMid?.let { move(it, shifts[index]) })
        }
        lines = lines.map { line ->
            val index = objects.indexOfFirst { line.id in it.lineIds }
            if (index < 0) line else line.copy(start = move(line.start, shifts[index]), end = move(line.end, shifts[index]))
        }
        extrusions = extrusions.map { extrusion ->
            val index = objects.indexOfFirst { item -> extrusion.guideIds.any { it in item.guideIds } }
            if (index < 0) extrusion else { val moved = extrusion.copy(origin = move(extrusion.origin, shifts[index]), handle = move(extrusion.handle, shifts[index])); moved.depthMm?.let { extrusionWithDepth(moved, it, scale, unit) } ?: moved }
        }
        selectedVertices = selectedVertices.flatMap { point ->
            val indexes = objects.indices.filter { index ->
                val item = objects[index]
                originalGuides.any { it.id in item.guideIds && (distance(point, it.start) < 0.6 || distance(point, it.end) < 0.6) } ||
                    originalLines.any { it.id in item.lineIds && (distance(point, it.start) < 0.6 || distance(point, it.end) < 0.6) }
            }
            if (indexes.isEmpty()) listOf(point) else indexes.map { move(point, shifts[it]) }
        }.fold(emptyList()) { acc, point -> if (acc.any { distance(it, point) < 0.6 }) acc else acc + point }
    }
    fun alignSelectedVertices(horizontal: Boolean, reference: AlignmentReference? = null) {
        if (selectedVertices.size < 2) return
        val axis = if (reference != null) { if (horizontal) reference.point.y else reference.point.x } else if (horizontal) selectedVertices.map { it.y }.average().toFloat() else selectedVertices.map { it.x }.average().toFloat()
        val referenceEnds = reference?.let { ref -> if (ref.guide) guides.firstOrNull { it.id == ref.id }?.let { it.start to it.end } else lines.firstOrNull { it.id == ref.id }?.let { it.start to it.end } }
        val other = referenceEnds?.let { (a, b) -> if (distance(a, reference!!.point) < 0.6) b else a }
        val otherSelected = other?.let { p -> selectedVertices.any { distance(p, it) < 0.6 } } == true
        val referenceLength = referenceEnds?.let { distance(it.first, it.second).toFloat() } ?: 0f
        val previousPoints = selectedVertices
        fun touched(a: Pt, b: Pt) = previousPoints.any { distance(it, a) < 0.6 || distance(it, b) < 0.6 }
        val affectedGuides = guides.filter { touched(it.start, it.end) }.map { it.id }.toSet()
        val affectedLines = lines.filter { touched(it.start, it.end) }.map { it.id }.toSet()
        pushUndo()
        transformSelectedVertices { point ->
            if (otherSelected && other != null && distance(point, other) < 0.6 && reference != null) {
                if (horizontal) Pt(reference.point.x + (if (other.x >= reference.point.x) 1 else -1) * referenceLength, axis)
                else Pt(axis, reference.point.y + (if (other.y >= reference.point.y) 1 else -1) * referenceLength)
            } else if (horizontal) Pt(point.x, axis) else Pt(axis, point.y)
        }
        guides = guides.map { guide -> if (guide.lengthMm != null && !(reference?.guide == true && reference.id == guide.id) && guide.id in affectedGuides) guide.copy(lengthMm = null) else guide }
        val cleared = lines.map { line ->
            if (!(reference?.guide == false && reference.id == line.id) && (line.isScaleReference || line.lengthSource == LengthSource.MANUAL || line.lengthSource == LengthSource.MEASURED) && line.id in affectedLines) line.copy(value = 0.0, isScaleReference = false, lengthSource = LengthSource.UNKNOWN) else line
        }
        lines = recalculate(cleared).map { line -> cleared.firstOrNull { it.id == line.id && (it.lengthSource == LengthSource.MANUAL || it.lengthSource == LengthSource.MEASURED) }?.let { line.copy(value = it.value) } ?: line }
        pendingAlignment = null
    }
    fun requestAlignment(horizontal: Boolean) {
        if (selectedGhostPoints.isNotEmpty()) {
            // Points from several views: line them all up on this view's screen axis, at their average.
            val here = modelSetup().mapping(editPlane) ?: return
            val ghosts = selectedGhostPoints.mapNotNull { sel -> ghostPointHere(sel)?.let { sel to it } }
            val all = selectedVertices + ghosts.map { it.second }
            if (all.size < 2) return
            val target = if (horizontal) all.map { it.y }.average().toFloat() else all.map { it.x }.average().toFloat()
            pushUndo()
            transformSelectedVertices { if (horizontal) Pt(it.x, target) else Pt(target, it.y) }
            ghosts.forEach { (sel, p) -> moveGhostPoint(sel, if (horizontal) here.modelDelta(0.0, (target - p.y).toDouble()) else here.modelDelta((target - p.x).toDouble(), 0.0)) }
            return
        }
        val objects = selectedAlignmentObjects()
        if (objects.size >= 2) { alignSelectedObjects(horizontal, objects); return }
        val refs = alignmentReferences()
        if (refs.size > 1) pendingAlignment = horizontal else alignSelectedVertices(horizontal, refs.firstOrNull())
    }
    fun handleDrag(point: Pt, snappedToFeature: Boolean) {
        ovalDrag?.let { (pieces, c, h) ->
            val (half, horizontal) = h
            val k = ((if (horizontal) abs(point.x - c.x) else abs(point.y - c.y)) / half).coerceAtLeast(0.02f)
            fun stretch(p: Pt) = if (horizontal) Pt(c.x + (p.x - c.x) * k, p.y) else Pt(p.x, c.y + (p.y - c.y) * k)
            val byId = pieces.associateBy { it.id }
            guides = guides.map { g -> byId[g.id]?.let { o -> g.copy(start = stretch(o.start), end = stretch(o.end), curveMid = o.curveMid?.let(::stretch), shapeWidthMm = null, shapeHeightMm = null) } ?: g }
            return
        }
        if (growDrag != null) { growMove?.invoke(point); return }
        if (shapeDragging) { pendingEnd = draftStart?.let { s -> if (lineDrawing == LineDrawing.RECTANGLE) squareEnd(s, point).first else point } ?: point; return }
        modelDragLast?.let { last ->
            modelSetup().mapping(editPlane)?.modelDelta((point.x - last.x).toDouble(), (point.y - last.y).toDouble())?.let { d -> importedModels = importedModels.map { if (it.id == selectedModelId) it.copy(center = it.center + d) else it } }
            modelDragLast = point; return
        }
        itemDragLast?.let { last -> modelSetup().mapping(editPlane)?.modelDelta((point.x - last.x).toDouble(), (point.y - last.y).toDouble())?.let(::movePicked); itemDragLast = point; return }
        if (itemRectStart != null) { itemRectEnd = point; return }
        foreignDrag?.let { foreignDrag = it.copy(current = point); return }
        ghostDragLast?.let { last ->
            val sel = selectedGhostPoint ?: return
            modelSetup().mapping(editPlane)?.modelDelta((point.x - last.x).toDouble(), (point.y - last.y).toDouble())?.let { moveGhostPoint(sel, it) }
            ghostDragLast = point; return
        }
        if (selectionStart != null) { selectionEnd = point; return }
        groupDragLast?.let { previous ->
            val dx = point.x - previous.x; val dy = point.y - previous.y
            transformSelectedVertices { Pt(it.x + dx, it.y + dy) }
            modelSetup().mapping(editPlane)?.modelDelta(dx.toDouble(), dy.toDouble())?.let { d -> selectedGhostPoints.forEach { moveGhostPoint(it, d) } }
            groupDragLast = point; return
        }
        activeCurveId?.let { id ->
            val previous = curveDragLast ?: point
            guides = guides.map { guide -> if (guide.id == id) guide.copy(curveMid = guide.curveMid?.let { Pt(it.x + point.x - previous.x, it.y + point.y - previous.y) }) else guide }
            curveDragLast = point
            return
        }
        vanishingDragLast?.let { previous ->
            val dx = point.x - previous.x; val dy = point.y - previous.y
            extrusions = extrusions.map { extrusion ->
                val oldLength = distance(extrusion.origin, extrusion.vanishing)
                val fraction = if (oldLength > 0.001) (distance(extrusion.origin, extrusion.handle) / oldLength).toFloat().coerceIn(0f, 1f) else 0f
                val vanishing = Pt(extrusion.vanishing.x + dx, extrusion.vanishing.y + dy)
                val updated = extrusion.copy(vanishing = vanishing, handle = Pt(extrusion.origin.x + (vanishing.x - extrusion.origin.x) * fraction, extrusion.origin.y + (vanishing.y - extrusion.origin.y) * fraction))
                updated.depthMm?.let { extrusionWithDepth(updated, it, scale, unit) } ?: updated
            }
            vanishingDragLast = point
            return
        }
        if (tool == Tool.ADJUST && draggingExtrusion) {
            extrusions = extrusions.map { if (it.id == selectedExtrusionId) { val handle = depthHandle(it, point); it.copy(handle = handle, depthMm = depthMmForHandle(it, handle, scale, unit)) } else it }
            return
        }
        if (tool == Tool.ADJUST || tool == Tool.MEASURE) activePoint?.let { endpoint ->
            if (System.currentTimeMillis() < edgePauseUntil) return@let
            val line = lines.firstOrNull { it.id == endpoint.lineId } ?: return@let
            val (snappedPoint, alignment) = snapEndpointAlignment(line, endpoint, point, lines, levelRotation)
            updateAlignment(alignment)
            if (snappedToFeature) {
                val key = "${endpoint.lineId}:${endpoint.isStart}:${snappedPoint.x.toInt()}:${snappedPoint.y.toInt()}"
                if (edgeSnapKey != key) {
                    edgeSnapKey = key
                    edgeSnapPoint = snappedPoint
                    edgeFlashToken++
                    edgePauseUntil = System.currentTimeMillis() + 2000L
                }
            } else edgeSnapKey = null
            val previous = if (endpoint.isStart) line.start else line.end
            lines = recalculate(lines.map { item ->
                if (line.pathId != null && item.pathId == line.pathId) item.copy(start = if (distance(item.start, previous) < 0.6) snappedPoint else item.start, end = if (distance(item.end, previous) < 0.6) snappedPoint else item.end)
                else if (item.id != endpoint.lineId) item else if (endpoint.isStart) item.copy(start = snappedPoint) else item.copy(end = snappedPoint)
            })
            selectedLineId = endpoint.lineId
            selectedEndpoint = endpoint
            magnifierPoint = snappedPoint
            adjustmentIndicatorPoint = snappedPoint
            adjustmentLineId = endpoint.lineId
            adjustmentGuideId = null
            adjustmentFlashToken++
        }
        if (tool == Tool.ADJUST || tool == Tool.LINES) activeGuideEndpoint?.let { endpoint ->
            val guide = guides.firstOrNull { it.id == endpoint.guideId } ?: return@let
            val vertex = activeGuideVertexPoint ?: if (endpoint.isStart) guide.start else guide.end
            guides = guides.map { item ->
                if (sameGuideObject(item, guide)) item.copy(
                    start = if (distance(item.start, vertex) < 0.6) point else item.start,
                    end = if (distance(item.end, vertex) < 0.6) point else item.end
                ) else if (item.id == guide.id) {
                    if (endpoint.isStart) item.copy(start = point) else item.copy(end = point)
                } else item
            }
            activeGuideVertexPoint = point
            selectedGuideId = guide.id
            selectedGuideEndpoint = endpoint
            magnifierPoint = point
            adjustmentIndicatorPoint = point
            adjustmentLineId = null
            adjustmentGuideId = guide.id
            adjustmentFlashToken++
        }
        if (tool == Tool.ADJUST) activeWholeLineId?.let { id ->
            val previous = activeWholeLineLastPoint ?: point
            val dx = point.x - previous.x
            val dy = point.y - previous.y
            if (dx != 0f || dy != 0f) {
                val pathId = lines.firstOrNull { it.id == id }?.pathId
                lines = recalculate(lines.map { line ->
                    if (line.id == id || pathId != null && line.pathId == pathId) line.copy(start = Pt(line.start.x + dx, line.start.y + dy), end = Pt(line.end.x + dx, line.end.y + dy)) else line
                })
                lines.firstOrNull { it.id == id }?.let { line ->
                    adjustmentIndicatorPoint = Pt((line.start.x + line.end.x) / 2f, (line.start.y + line.end.y) / 2f)
                }
                adjustmentLineId = id
                adjustmentGuideId = null
                adjustmentFlashToken++
            }
            activeWholeLineLastPoint = point
        }
        if (tool == Tool.ADJUST) activeWholeGuideId?.let { id ->
            val previous = activeWholeGuideLastPoint ?: point
            val dx = point.x - previous.x
            val dy = point.y - previous.y
            if (dx != 0f || dy != 0f) {
                translateGuideObject(id, dx, dy)
                guides.firstOrNull { it.id == id }?.let { guide ->
                    adjustmentIndicatorPoint = Pt((guide.start.x + guide.end.x) / 2f, (guide.start.y + guide.end.y) / 2f)
                }
                adjustmentLineId = null
                adjustmentGuideId = id
                adjustmentFlashToken++
            }
            activeWholeGuideLastPoint = point
        }
    }
    fun handleDragEnd() {
        if (ovalDrag != null) { ovalDrag = null; return }
        if (growDrag != null) { growDrag = null; return }
        if (shapeDragging) {
            shapeDragging = false
            val start = draftStart; val end = pendingEnd
            draftStart = null; pendingEnd = null
            if (start != null && end != null && distance(start, end) > 4.0) { if (lineDrawing == LineDrawing.RECTANGLE) addRectangle(start, end) else addCircle(start, end) }
            return
        }
        if (modelDragLast != null) { modelDragLast = null; return }
        if (itemDragLast != null) { itemDragLast = null; return }
        itemRectStart?.let { a -> itemRectEnd?.let { b -> pickInRect(a, b) }; itemRectStart = null; itemRectEnd = null; return }
        foreignDrag?.let { drag -> foreignDrag = null; applyForeignEdit(drag); return }
        if (ghostDragLast != null) { ghostDragLast = null; return }
        val start = selectionStart; val end = selectionEnd
        if (start != null && end != null) {
            selectedGhostPoints = ghostPointsInRect(start, end)
            val x1 = min(start.x, end.x); val x2 = max(start.x, end.x); val y1 = min(start.y, end.y); val y2 = max(start.y, end.y)
            selectedVertices = (lines.flatMap { listOf(it.start, it.end) } + guides.flatMap { listOf(it.start, it.end) }).filter { it.x in x1..x2 && it.y in y1..y2 }.fold(emptyList()) { acc, p -> if (acc.any { distance(it, p) < 0.6 }) acc else acc + p }
            selectedEndpoint = null; selectedGuideEndpoint = null; selectedWholeLineId = null; selectedWholeGuideId = null; selectedCurveId = null; selectedExtrusionId = null; selectedGuideId = null; selectedLabelId = null
        }
        selectionStart = null; selectionEnd = null; groupDragLast = null; activeCurveId = null; curveDragLast = null; draggingExtrusion = false; vanishingDragLast = null; activePoint = null; activeGuideEndpoint = null; activeGuideVertexPoint = null; activeWholeLineId = null; activeWholeLineLastPoint = null; activeWholeGuideId = null; activeWholeGuideLastPoint = null; magnifierPoint = null
    }
    fun handleDragCancel() { foreignDrag = null; selectionStart = null; selectionEnd = null; handleDragEnd() }
    fun nudgeSelected(dx: Float, dy: Float) {
        selectedForeignPoint?.let { nudgeForeignPoint(it, dx, dy); return }
        // A whole 3D object from another view, or an imported model: one step in the arrow's direction.
        val objectStep = unitFactorMm(unit) / Math.pow(10.0, (MeasurementPrecision.places[unit] ?: 2).toDouble())
        selectedForeign?.let { (plane, extrusionId) ->
            val g = planeGeometry(plane)
            val seed = g.extrusions.firstOrNull { it.id == extrusionId }?.let { e -> extrusionGuides(e, g.guides).firstOrNull() } ?: return
            modelSetup().mapping(editPlane)?.modelDelta(dx.toDouble(), dy.toDouble())?.let { pushUndo(); moveItem(plane to seed.id, it.normalized() * objectStep) }
            return
        }
        selectedModelId?.let { id ->
            modelSetup().mapping(editPlane)?.modelDelta(dx.toDouble(), dy.toDouble())?.let { d -> pushUndo(); importedModels = importedModels.map { if (it.id == id) it.copy(center = it.center + d.normalized() * objectStep) else it } }
            return
        }
        selectedGhostPoint?.let { sel ->
            val step = unitFactorMm(unit) / Math.pow(10.0, (MeasurementPrecision.places[unit] ?: 2).toDouble())
            modelSetup().mapping(editPlane)?.modelDelta(dx.toDouble(), dy.toDouble())?.let { pushUndo(); moveGhostPoint(sel, it.normalized() * step) }
            return
        }
        if (selectMode && (pickedItems.isNotEmpty() || pickedPoints.isNotEmpty())) {
            val step = unitFactorMm(unit) / Math.pow(10.0, (MeasurementPrecision.places[unit] ?: 2).toDouble())
            modelSetup().mapping(editPlane)?.modelDelta(dx.toDouble(), dy.toDouble())?.let { pushUndo(); movePicked(it.normalized() * step) }
            return
        }
        if (selectedVertices.isNotEmpty() || selectedGhostPoints.isNotEmpty()) {
            pushUndo(); transformSelectedVertices { Pt(it.x + dx, it.y + dy) }
            modelSetup().mapping(editPlane)?.modelDelta(dx.toDouble(), dy.toDouble())?.let { d -> selectedGhostPoints.forEach { moveGhostPoint(it, d) } }
            return
        }
        selectedCurveId?.let { id ->
            val guide = guides.firstOrNull { it.id == id } ?: return@let
            val mid = guide.curveMid ?: return@let
            val image = bitmap ?: return@let
            val target = Pt((mid.x + dx).coerceIn(0f, image.width.toFloat()), (mid.y + dy).coerceIn(0f, image.height.toFloat()))
            pushUndo()
            guides = guides.map { if (it.id == id) it.copy(curveMid = target) else it }
            adjustmentIndicatorPoint = target; adjustmentLineId = null; adjustmentGuideId = id; adjustmentFlashToken++
            return
        }
        selectedExtrusionId?.let { id ->
            pushUndo()
            extrusions = extrusions.map { if (it.id == id) { val handle = depthHandle(it, Pt(it.handle.x + dx, it.handle.y + dy)); it.copy(handle = handle, depthMm = depthMmForHandle(it, handle, scale, unit)) } else it }
            return
        }
        selectedEndpoint?.let { endpoint ->
            if (System.currentTimeMillis() < alignmentPauseUntil) return@let
            val line = lines.firstOrNull { it.id == endpoint.lineId } ?: return@let
            val current = if (endpoint.isStart) line.start else line.end
            val (snappedPoint, alignment) = snapEndpointAlignment(line, endpoint, Pt(current.x + dx, current.y + dy), lines, levelRotation)
            if (updateAlignment(alignment)) alignmentPauseUntil = System.currentTimeMillis() + 1200L
            pushUndo()
            lines = recalculate(lines.map { item ->
                if (line.pathId != null && item.pathId == line.pathId) item.copy(start = if (distance(item.start, current) < 0.6) snappedPoint else item.start, end = if (distance(item.end, current) < 0.6) snappedPoint else item.end)
                else if (item.id != endpoint.lineId) item else if (endpoint.isStart) item.copy(start = snappedPoint) else item.copy(end = snappedPoint)
            })
            adjustmentIndicatorPoint = snappedPoint
            adjustmentLineId = endpoint.lineId
            adjustmentGuideId = null
            adjustmentFlashToken++
            return
        }
        selectedGuideEndpoint?.let { endpoint ->
            val selectedGuide = guides.firstOrNull { it.id == endpoint.guideId } ?: return@let
            val vertex = if (endpoint.isStart) selectedGuide.start else selectedGuide.end
            val target = Pt(vertex.x + dx, vertex.y + dy)
            pushUndo()
            guides = guides.map { guide ->
                if (sameGuideObject(guide, selectedGuide)) guide.copy(
                    start = if (distance(guide.start, vertex) < 0.6) target else guide.start,
                    end = if (distance(guide.end, vertex) < 0.6) target else guide.end
                ) else if (guide.id == endpoint.guideId) {
                    if (endpoint.isStart) guide.copy(start = target) else guide.copy(end = target)
                } else guide
            }
            adjustmentIndicatorPoint = target
            adjustmentLineId = null
            adjustmentGuideId = endpoint.guideId
            adjustmentFlashToken++
            return
        }
        selectedLabelId?.let { id ->
            pushUndo()
            lines = lines.map { line -> if (line.id != id) line else {
                val length = distance(line.start, line.end).toFloat().coerceAtLeast(1f)
                val along = ((line.end.x - line.start.x) * dx + (line.end.y - line.start.y) * dy) / length
                line.copy(labelOffset = (line.labelOffset + along).coerceIn(-length * 0.45f, length * 0.45f))
            } }
            return
        }
        selectedWholeLineId?.let { id ->
            pushUndo()
            val pathId = lines.firstOrNull { it.id == id }?.pathId
            lines = recalculate(lines.map { line -> if (line.id == id || pathId != null && line.pathId == pathId) line.copy(start = Pt(line.start.x + dx, line.start.y + dy), end = Pt(line.end.x + dx, line.end.y + dy)) else line })
            lines.firstOrNull { it.id == id }?.let { line -> adjustmentIndicatorPoint = Pt((line.start.x + line.end.x) / 2f, (line.start.y + line.end.y) / 2f) }
            adjustmentLineId = id
            adjustmentGuideId = null
            adjustmentFlashToken++
            return
        }
        selectedWholeGuideId?.let { id ->
            pushUndo()
            translateGuideObject(id, dx, dy)
            guides.firstOrNull { it.id == id }?.let { guide -> adjustmentIndicatorPoint = Pt((guide.start.x + guide.end.x) / 2f, (guide.start.y + guide.end.y) / 2f) }
            adjustmentLineId = null
            adjustmentGuideId = id
            adjustmentFlashToken++
        }
    }
    val selectedPoint = selectedEndpoint?.let { endpoint -> lines.firstOrNull { it.id == endpoint.lineId }?.let { if (endpoint.isStart) it.start else it.end } }
        ?: selectedGuideEndpoint?.let { endpoint -> guides.firstOrNull { it.id == endpoint.guideId }?.let { if (endpoint.isStart) it.start else it.end } }
        ?: selectedCurveId?.let { id -> guides.firstOrNull { it.id == id }?.curveMid }

    /** Puts the current plane's geometry aside and edits [next]; nothing is lost and undo still covers both. */
    fun switchPlane(next: ModelPlane) {
        if (next == editPlane) return
        // What is selected, by its own view and id, so it stays selected (with its fine-move arrows) in the next view.
        val keepGuide = (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } }?.let(::homeKey)
        val keepWhole = selectedWholeGuideId != null
        val keepEnd = selectedGuideEndpoint?.let { e -> guides.firstOrNull { it.id == e.guideId }?.let { g -> homeKey(g).let { Triple(it.first, it.second, e.isStart) } } }
        val keepPoints = selectedVertices.mapNotNull { v -> guides.firstOrNull { distance(it.start, v) < 0.6 || distance(it.end, v) < 0.6 }?.let { g -> homeKey(g).let { Triple(it.first, it.second, distance(g.start, v) < 0.6) } } } +
            selectedGhostPoints + listOfNotNull(selectedGhostPoint)
        val keepForeign = selectedForeign
        val full = ModelPlane.entries.associateWith { planeGeometry(it) }
        // Measurements stay in their view; lines from every view come along, projected.
        showView(next, full)
        lines = recalculate(lines)
        referenceCount = lines.count { it.isReference }
        selectedExtrusionId = null; selectedCurveId = null; selectedVertices = emptyList(); selectedLineId = null; selectedEndpoint = null; selectedGuideId = null; selectedGuideEndpoint = null
        selectedWholeGuideId = null; selectedWholeLineId = null; selectedLabelId = null; draftStart = null; pendingEnd = null; activePolylineId = null; activeMeasurePathId = null; deleteCandidateId = null
        if (tool == Tool.CALIBRATE || tool == Tool.CROP) tool = Tool.NONE
        selectedForeign = null; foreignDrag = null; selectedForeignPoint = null; solidPick = null; joinPick = null; selectedGhostPoint = null; ghostDragLast = null
        crossViewSelection?.takeIf { it.first == next && guides.any { g -> g.id == it.second } }?.let { selectedWholeGuideId = it.second; selectedGuideId = it.second }
        // The kept selection as it shows here: an own line, a line projected from its view, or a 3D object from there.
        fun shownId(home: Pair<ModelPlane, Int>): Int? = (if (home.first == next) home.second else encodedId(home.first, home.second)).takeIf { id -> guides.any { it.id == id } }
        val keptEnd = keepEnd?.let { (plane, id, start) -> shownId(plane to id)?.let { selectedGuideEndpoint = GuideEndpoint(it, start); selectedGuideId = it; true } }
        if (keepEnd != null && keptEnd == null) selectedGhostPoint = keepEnd
        if (keepEnd == null) keepGuide?.let { home ->
            val id = shownId(home)
            if (id != null) { selectedGuideId = id; if (keepWhole) selectedWholeGuideId = id }
            else planeGeometry(home.first).let { g -> g.extrusions.firstOrNull { e -> extrusionGuides(e, g.guides).any { it.id == home.second } } }?.let { selectedForeign = home.first to it.id }
        }
        if (keepPoints.isNotEmpty()) {
            val own = keepPoints.filter { it.first == next }
            selectedVertices = own.mapNotNull { (_, id, start) -> guides.firstOrNull { it.id == id }?.let { if (start) it.start else it.end } }
            selectedGhostPoints = keepPoints.filter { it.first != next }
        }
        keepForeign?.let { (plane, extrusionId) ->
            // An object drawn in the next view is selected there as its own shape.
            if (plane == next) extrusions.firstOrNull { it.id == extrusionId }?.let { e -> extrusionGuides(e, guides).firstOrNull()?.let { selectedWholeGuideId = it.id; selectedGuideId = it.id } }
            else selectedForeign = keepForeign
        }
        zoom = 1f; pan = Offset.Zero; fitPending = true
    }

    fun restoreLastUndo() {
        val previous = undoHistory.lastOrNull() ?: return
        previous.image?.let { restored ->
            bitmap = restored
            projectId?.let { id -> ProjectStore.imageFile(context, id).outputStream().use { restored.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        editPlane = previous.plane; planeStash = previous.stash; planeFrames = previous.frames; solidOps = previous.solidOps; itemGroups = previous.itemGroups; importedModels = previous.models
        pickedItems = emptyList(); selectedGhostPoints = emptyList(); pickedPoints = emptyList()
        lines = recalculate(previous.lines).map { line -> previous.lines.firstOrNull { it.id == line.id && (it.lengthSource == LengthSource.MANUAL || it.lengthSource == LengthSource.MEASURED) }?.let { line.copy(value = it.value) } ?: line }; guides = previous.guides; guidesDetected = previous.guidesDetected; extrusions = previous.extrusions; selectedExtrusionId = selectedExtrusionId?.takeIf { id -> extrusions.any { it.id == id } }; levelRotation = previous.rotation; unit = previous.unit; tool = previous.tool; referenceCount = lines.count { it.isReference }
        undoHistory = undoHistory.dropLast(1)
    }

    /** Builds the 3D model from every plane and shows it in place of the canvas. The camera and frame stay put across rebuilds. */
    fun open3dView() {
        val placedSnapshot = placedModels()
        val guideSnapshot = allGuides(); val extrusionSnapshot = allExtrusions(); val opsSnapshot = solidOps; val materialSnapshot = materials.associateBy { it.id }; val setup = modelSetup(); val modelUnit = unit; val measurementSnapshot = allLines().filterNot { it.isReference }; val openedProject = projectId
        val frame = viewerModel?.frame
        scope.launch {
            val result = withContext(Dispatchers.Default) { runCatching {
                val colors = mutableListOf<Int>()
                // Intersecting objects are shown with a warning so they can be moved apart or joined; export still refuses them.
                // Imported models join the drawing; a project of only imported models has no shapes to build.
                val built = try { buildModel(guideSnapshot, extrusionSnapshot, opsSnapshot, setup, strict = false, materials = materialSnapshot) }
                    catch (e: IllegalStateException) { if (e is MeshException || placedSnapshot.isEmpty()) throw e; ModelBuild(emptyList(), emptyList(), emptyList()) }
                val model = ModelBuild(built.meshes + placedSnapshot.map { it.second }, built.objects, built.warnings, built.meshes.indices.map { built.finishes.getOrNull(it) ?: Finish.PLAIN } + placedSnapshot.map { it.first.finish })
                val meshes = model.meshes
                val warnings = model.warnings.map { it.toString() }
                meshes.forEach { mesh -> repeat(mesh.triangles.size) { colors.add(mesh.argb) } }
                val finishes = meshes.flatMapIndexed { i, mesh -> List(mesh.triangles.size) { model.finishes.getOrNull(i) ?: Finish.PLAIN } }
                val stl = ModelExport.stl(meshes)
                // Imported models can't be tapped for exact values in 3D (index -1); their dialog is in the 2D views.
                val triangleObjects = meshes.flatMapIndexed { index, mesh -> List(mesh.triangles.size) { if (index < built.meshes.size) index else -1 } }.toIntArray()
                val measurements = measurementSnapshot.mapNotNull { line ->
                    val mapping = setup.mapping(line.plane) ?: return@mapNotNull null
                    val value = if (line.lengthSource == LengthSource.UNKNOWN) "?" else "${formatMeasure(line.value, modelUnit)}${if (line.hasDepth()) "*" else ""} $modelUnit"
                    val a = mapping.model(line.start, line.startZMm); val b = mapping.model(line.end, line.endZMm)
                    ViewerMeasurement(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), listOf(line.label, value).filter { it.isNotBlank() }.joinToString(": "), measurementColor(line).toArgb(), a.z.toFloat(), b.z.toFloat())
                }
                ViewerMesh.fromStl(stl, colors, measurements, triangleObjects, meshes.map { it.name }, warnings, frame, finishes) to model.objects
            } }
            if (projectId != openedProject) return@launch
            result.onSuccess { (mesh, ids) -> viewerModel = mesh; viewerObjects = ids; show3d = true }
                .onFailure { error -> logToast(context, "3D view: ${error.localizedMessage}", Toast.LENGTH_LONG).show(); if (viewerModel == null) show3d = false }
        }
    }
    // While the 3D view is open, every change (undo, the exact-values dialog) rebuilds it.
    LaunchedEffect(lines, guides, extrusions, planeStash, planeFrames, solidOps, materials, unit, importedModels) { if (show3d) open3dView() }
    fun leave3d() { show3d = false; viewerModel = null; viewerEditObject = null }
    /** Switches to the view a foreign object was drawn in and selects it there, where all drawing tools apply. */
    fun openInOwnView(key: Pair<ModelPlane, Int>) {
        leave3d(); switchPlane(key.first); tool = Tool.ADJUST
        extrusions.firstOrNull { it.id == key.second }?.let { e -> extrusionGuides(e, guides).firstOrNull()?.let { selectedWholeGuideId = it.id; selectedGuideId = it.id } }
    }

    /** Screen box (this view's canvas pixels) of an item. */
    fun itemBox(key: Pair<ModelPlane, Int>): FloatArray? {
        val pts = itemOutlineHere(key).flatten(); if (pts.isEmpty()) return null
        return floatArrayOf(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y })
    }
    fun extrusionOf(key: Pair<ModelPlane, Int>): Extrusion? { val ids = itemGuides(key).map { it.id }.toSet(); return planeGeometry(key.first).extrusions.firstOrNull { e -> e.guideIds.any { it in ids } } }
    fun isClosedShape(key: Pair<ModelPlane, Int>) = closedGuideShapes(planeGeometry(key.first).guides).any { path -> path.any { g -> g.id == key.second } || itemGuides(key).any { g -> path.any { it.id == g.id } } }
    /**
     * Turns items and loose points [deg] degrees around this view's line of sight, about their common center
     * (counterclockwise on screen for positive [deg]). A shape that ends up lying flat in another view moves to that
     * view, with its 3D depth, groups, cuts and joins, so it stays a proper shape there.
     */
    fun turnItems(keys: List<Pair<ModelPlane, Int>>, points: List<Triple<ModelPlane, Int, Boolean>>, deg: Double) {
        val setup = modelSetup(); val axis = editPlane.toward
        fun pointModel(sel: Triple<ModelPlane, Int, Boolean>): Vec3? {
            val g = planeGeometry(sel.first).guides.firstOrNull { it.id == sel.second } ?: return null
            val own = setup.mapping(sel.first) ?: return null
            val f = faceOffset(sel.first, sel.second)
            return if (sel.third) own.model(g.start, g.startZMm + f) else own.model(g.end, g.endZMm + f)
        }
        val items = keys.distinctBy { k -> itemGuides(k).minOfOrNull { it.id }?.let { k.first to it } ?: k }
        val loose = points.filterNot { p -> items.any { it.first == p.first && itemGuides(it).any { g -> g.id == p.second } } }
        fun itemFace(key: Pair<ModelPlane, Int>) = itemGuides(key).firstOrNull()?.let { faceOffset(key.first, it.id) } ?: 0.0
        val all = items.flatMap { key -> val own = setup.mapping(key.first) ?: return@flatMap emptyList(); val f = itemFace(key); itemGuides(key).flatMap { g -> listOf(own.model(g.start, g.startZMm + f), own.model(g.end, g.endZMm + f)) } } + loose.mapNotNull(::pointModel)
        if (all.isEmpty()) return
        val center = Vec3((all.minOf { it.x } + all.maxOf { it.x }) / 2, (all.minOf { it.y } + all.maxOf { it.y }) / 2, (all.minOf { it.z } + all.maxOf { it.z }) / 2)
        fun turned(v: Vec3) = center + ModelImport.turn(v - center, axis, deg)
        pushUndo()
        val pointMoves = loose.mapNotNull { sel -> pointModel(sel)?.let { sel to (turned(it) - it) } }
        items.forEach { key ->
            val plane = key.first; val own = setup.mapping(plane) ?: return@forEach
            val geometry = planeGeometry(plane)
            val ids = itemGuides(key).map { it.id }.toSet()
            if (ids.isEmpty()) return@forEach
            val face = itemFace(key)
            // Every point of the item, turned, with its model position.
            val moved = geometry.guides.filter { it.id in ids }.associate { g ->
                val mid = g.curveMid?.let { turned(own.model(it, (g.startZMm + g.endZMm) / 2 + face)) }
                g.id to Triple(turned(own.model(g.start, g.startZMm + face)), turned(own.model(g.end, g.endZMm + face)), mid)
            }
            val modelPoints = moved.values.flatMap { listOfNotNull(it.first, it.second, it.third) }
            fun flatIn(p: ModelPlane) = modelPoints.map { it dot p.toward }.let { it.max() - it.min() < 0.5 }
            val target = if (isClosedShape(key) && !flatIn(plane)) ModelPlane.entries.firstOrNull { it != plane && flatIn(it) } else null
            val itemExtrusions = geometry.extrusions.filter { e -> e.guideIds.any { it in ids } }
            val there = target?.let { setup.mapping(it) }
            if (target == null || there == null) {
                // Stays in its view: its lines move there, deeper or shallower where the turn tilts them.
                val nextGuides = geometry.guides.map { g -> moved[g.id]?.let { (a, b, m) -> g.copy(start = own.pixel(a), end = own.pixel(b), curveMid = m?.let { own.pixel(it) }, startZMm = (a dot plane.toward) - face, endZMm = (b dot plane.toward) - face) } ?: g }
                val nextExtrusions = geometry.extrusions.map { e -> if (e !in itemExtrusions) e else e.copy(origin = own.pixel(turned(own.model(e.origin, e.positionZMm))), handle = own.pixel(turned(own.model(e.handle, e.positionZMm)))) }
                setPlaneShapes(plane, nextGuides, nextExtrusions)
                return@forEach
            }
            // Moves to the view it now lies flat in, under new ids there.
            setPlaneShapes(plane, geometry.guides.filterNot { it.id in ids }, geometry.extrusions.filterNot { it in itemExtrusions })
            val dest = planeGeometry(target)
            var nextId = (dest.guides.maxOfOrNull { it.id } ?: 0) + 1
            val newIds = ids.sorted().associateWith { nextId++ }
            val pathBase = dest.guides.mapNotNull { it.pathId }.maxOrNull() ?: 0
            val groupBase = dest.guides.mapNotNull { it.groupId }.maxOrNull() ?: 0
            val pathIds = geometry.guides.filter { it.id in ids }.mapNotNull { it.pathId }.distinct().withIndex().associate { (i, p) -> p to pathBase + i + 1 }
            val groupIds = geometry.guides.filter { it.id in ids }.mapNotNull { it.groupId }.distinct().withIndex().associate { (i, g) -> g to groupBase + i + 1 }
            // In its new view the object's drawing sits at its face: Front faces at the drawing, Top and Profile a depth away.
            val newFace = if (target == ModelPlane.FRONT) 0.0 else itemExtrusions.firstOrNull()?.depthMm ?: 0.0
            val newGuides = geometry.guides.filter { it.id in ids }.map { g -> val (a, b, m) = moved.getValue(g.id)
                g.copy(id = newIds.getValue(g.id), start = there.pixel(a), end = there.pixel(b), curveMid = m?.let { there.pixel(it) }, startZMm = (a dot target.toward) - newFace, endZMm = (b dot target.toward) - newFace,
                    pathId = g.pathId?.let(pathIds::get), groupId = g.groupId?.let(groupIds::get), plane = target, midDepthMm = null) }
            var nextExtrusion = (dest.extrusions.maxOfOrNull { it.id } ?: 0) + 1
            val pts = newGuides.flatMap { listOf(it.start, it.end) }
            val middle = Pt(pts.map { it.x }.average().toFloat(), pts.map { it.y }.average().toFloat())
            val extrusionIds = HashMap<Int, Int>()
            val newExtrusions = itemExtrusions.map { e -> val id = nextExtrusion++; extrusionIds[e.id] = id
                e.copy(id = id, guideIds = e.guideIds.mapNotNull(newIds::get), origin = middle, handle = middle, positionZMm = 0.0, plane = target) }
            setPlaneShapes(target, dest.guides + newGuides, dest.extrusions + newExtrusions)
            // Groups, cuts and joins follow it to its new view.
            itemGroups = itemGroups.map { group -> group.map { m -> if (m.first == plane && m.second in ids) target to newIds.getValue(m.second) else m } }
            pickedItems = pickedItems.map { m -> if (m.first == plane && m.second in ids) target to newIds.getValue(m.second) else m }
            solidOps = solidOps.map { op ->
                fun remap(k: Pair<ModelPlane, Int>) = if (k.first == plane) extrusionIds[k.second]?.let { target to it } ?: k else k
                op.copy(target = remap(op.target), tool = remap(op.tool))
            }
            logToast(context, "It now lies flat in ${target.label}, so it moved to the ${target.label} view", Toast.LENGTH_SHORT).show()
        }
        pointMoves.forEach { (sel, d) -> moveGhostPoint(sel, d) }
    }
    /** What the turn buttons turn: the selected model, or else the selected items (with their groups) and points. */
    fun turnSelection(deg: Double) {
        selectedModelId?.let { id ->
            pushUndo()
            val axis = when (editPlane) { ModelPlane.FRONT -> 2; ModelPlane.TOP -> 1; ModelPlane.PROFILE -> 0 }
            importedModels = importedModels.map { if (it.id == id) it.copy(turns = it.turns + (axis to deg)) else it }
            return
        }
        if (selectMode) { turnItems(pickedItems, pickedPoints, deg); return }
        val key = (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } }?.let(::homeKey)
            ?: selectedForeign?.let { (plane, extrusionId) -> planeGeometry(plane).let { g -> g.extrusions.firstOrNull { it.id == extrusionId }?.guideIds?.firstOrNull()?.let { plane to it } } }
        if (key != null) { turnItems(withGroup(key), emptyList(), deg); return }
        if (selectedVertices.isNotEmpty()) {
            val here = modelSetup().mapping(editPlane) ?: return
            val c = Pt(selectedVertices.map { it.x }.average().toFloat(), selectedVertices.map { it.y }.average().toFloat())
            val center = here.model(c, 0.0)
            pushUndo()
            transformSelectedVertices { p -> here.pixel(center + ModelImport.turn(here.model(p, 0.0) - center, editPlane.toward, deg)) }
        }
    }
    fun groupPicked() {
        if (pickedItems.size < 2) return
        pushUndo()
        val inGroup = itemGroups.filter { group -> group.any { m -> pickedItems.any { sameItem(m, it) } } }
        if (inGroup.size == 1 && pickedItems.all { p -> inGroup[0].any { sameItem(p, it) } }) { itemGroups = itemGroups.filterNot { it === inGroup[0] }; logToast(context, "Ungrouped", Toast.LENGTH_SHORT).show(); return }
        val merged: List<Pair<ModelPlane, Int>> = (inGroup.flatten() + pickedItems).fold(emptyList()) { acc, k -> if (acc.any { sameItem(it, k) }) acc else acc + k }
        itemGroups = itemGroups.filterNot { it in inGroup } + listOf(merged)
        logToast(context, "Grouped ${pickedItems.size} items: they are picked and moved together", Toast.LENGTH_SHORT).show()
    }
    /** Removes the explicit Select-mode group containing [key], without changing any lines. */
    fun ungroupItem(key: Pair<ModelPlane, Int>) {
        val group = itemGroups.firstOrNull { members -> members.any { sameItem(it, key) } }
        if (group == null) {
            logToast(context, "This item is not in a group", Toast.LENGTH_SHORT).show()
            return
        }
        pushUndo()
        itemGroups = itemGroups.filterNot { it === group }
        logToast(context, "Ungrouped: the items can move separately again", Toast.LENGTH_SHORT).show()
    }
    fun pickedIsGroup() = pickedItems.size >= 2 && itemGroups.any { group -> pickedItems.all { p -> group.any { sameItem(p, it) } } }
    /** Merge: two 3D objects become one solid; two shapes in the same view become one outline. */
    /**
     * Welds lines that touch: where an end of one lies on another line (at its end or anywhere along it), that line is
     * split there so both share the point, and all touching items become one object that moves together. With
     * [anchored], only items touching the first one are welded to it. Returns whether anything was welded.
     */
    fun weldItems(keys: List<Pair<ModelPlane, Int>>, anchored: Boolean): Boolean {
        var any = false
        var undoPushed = false
        keys.groupBy { it.first }.forEach { (plane, planeKeys) ->
            val items = planeKeys.distinctBy { k -> itemGuides(k).minOfOrNull { it.id } ?: k.second }
            if (items.size < 2) return@forEach
            val geometry = planeGeometry(plane)
            val frame = frameOf(plane)
            val tolerance = max(3.0, min(frame.width, frame.height) * 0.004)
            val guidesNow = geometry.guides.toMutableList()
            val ids = items.map { k -> itemGuides(k).map { it.id }.toMutableSet() }
            var nextId = (guidesNow.maxOfOrNull { it.id } ?: 0) + 1
            val welded = mutableSetOf<Int>()
            for (i in ids.indices) for (j in ids.indices) {
                if (i == j || anchored && i != 0 && j != 0) continue
                val ends = guidesNow.filter { it.id in ids[i] }.flatMap { listOf(it.start, it.end) }.distinctBy { (it.x * 10).roundToInt() to (it.y * 10).roundToInt() }
                for (p in ends) {
                    val target = guidesNow.filter { it.id in ids[j] }.minByOrNull { g -> distance(p, guidePointAt(g, guideNearestParameter(g, p))) } ?: continue
                    val q = guidePointAt(target, guideNearestParameter(target, p))
                    if (distance(p, q) > tolerance) continue
                    welded += i; welded += j
                    // The touching end moves exactly onto the line.
                    for (k in guidesNow.indices) { val g = guidesNow[k]; if (g.id in ids[i]) guidesNow[k] = g.copy(start = if (distance(g.start, p) < 0.01) q else g.start, end = if (distance(g.end, p) < 0.01) q else g.end) }
                    // A straight line touched in its middle is split there, so the two share a corner.
                    if (target.curveMid == null && distance(q, target.start) > tolerance && distance(q, target.end) > tolerance) {
                        val at = guidesNow.indexOfFirst { it.id == target.id }
                        val second = target.copy(id = nextId++, start = q)
                        guidesNow[at] = target.copy(end = q); guidesNow.add(at + 1, second)
                        ids[j] += second.id
                    }
                }
            }
            if (welded.size < 2) return@forEach
            if (!undoPushed) { pushUndo(); undoPushed = true }
            val group = (guidesNow.mapNotNull { it.groupId }.maxOrNull() ?: 0) + 1
            val members = welded.flatMap { ids[it] }.toSet()
            setPlaneShapes(plane, guidesNow.map { if (it.id in members) it.copy(groupId = group) else it }, geometry.extrusions)
            any = true
        }
        if (any) logToast(context, "Welded: they share the touching points and move together", Toast.LENGTH_SHORT).show()
        return any
    }
    /** Separates lines previously connected by Weld without changing their endpoints or dimensions. */
    fun unweldGuide(id: Int) {
        val guide = guides.firstOrNull { it.id == id } ?: return
        val group = guide.groupId
        if (group == null) {
            logToast(context, "This line is not welded to another line", Toast.LENGTH_SHORT).show()
            return
        }
        pushUndo()
        guides = guides.map { if (it.groupId == group) it.copy(groupId = null) else it }
        logToast(context, "Unwelded: the lines can move independently again", Toast.LENGTH_SHORT).show()
    }
    fun mergePicked() {
        if (pickedItems.size < 2) { logToast(context, "Pick at least two items", Toast.LENGTH_SHORT).show(); return }
        val first = pickedItems.first(); val rest = pickedItems.drop(1)
        val firstSolid = extrusionOf(first)
        if (firstSolid != null && rest.all { extrusionOf(it) != null }) {
            pushUndo(); solidOps = solidOps + rest.map { SolidOp(first.first to firstSolid.id, it.first to extrusionOf(it)!!.id, join = true) }
            pickedItems = emptyList(); logToast(context, "Joined in 3D", Toast.LENGTH_SHORT).show(); return
        }
        if (rest.size == 1 && rest[0].first == first.first && isClosedShape(first) && isClosedShape(rest[0])) {
            if (editPlane != first.first) switchPlane(first.first)
            applyShapeOperation(JunctionAction(JunctionKind.SHAPE_OVERLAP, first.second, rest[0].second, Pt(0f, 0f)), "merge")
            pickedItems = emptyList(); return
        }
        if (weldItems(pickedItems, anchored = false)) { pickedItems = emptyList(); return }
        logToast(context, "Merge needs lines that touch (an end on the other line), two overlapping shapes in the same view, or 3D objects", Toast.LENGTH_LONG).show()
    }
    /**
     * Makes a new, closed face from selected vertices.  It deliberately does not reuse the
     * selected guides: one corner can belong to several faces (the top of a pyramid, for
     * example), while each face still needs its own closed path and material.
     *
     * The points are copied through model space, rather than from the pixels presently on
     * screen.  Thus a point picked from another view keeps its real depth when the new face
     * is made in this view.
     */
    fun combinePickedPoints() {
        if (pickedPoints.size < 3) {
            logToast(context, "Pick at least three points to make a face", Toast.LENGTH_SHORT).show()
            return
        }
        val setup = modelSetup()
        val here = setup.mapping(editPlane)
        if (here == null) {
            logToast(context, "Add a calibration length before combining points from different views", Toast.LENGTH_LONG).show()
            return
        }
        fun modelPoint(sel: Triple<ModelPlane, Int, Boolean>): Vec3? {
            val guide = planeGeometry(sel.first).guides.firstOrNull { it.id == sel.second } ?: return null
            val own = setup.mapping(sel.first) ?: return null
            val depth = if (sel.third) guide.startZMm else guide.endZMm
            return own.model(if (sel.third) guide.start else guide.end, depth + faceOffset(sel.first, sel.second))
        }
        // A shared corner can have several selectable line ends.  It must occur only once
        // in the resulting contour.
        val points = pickedPoints.mapNotNull(::modelPoint).fold(mutableListOf<Vec3>()) { result, point ->
            if (result.none { (it - point).length < 0.01 }) result.add(point)
            result
        }
        if (points.size < 3) {
            logToast(context, "Pick three different points to make a face", Toast.LENGTH_SHORT).show()
            return
        }
        val projected = points.map { it to here.pixel(it) }
        val center = Pt(projected.map { it.second.x }.average().toFloat(), projected.map { it.second.y }.average().toFloat())
        // Selection order should not matter.  Ordering around the visible center gives a
        // non-crossing perimeter for ordinary triangles, quads, and polygonal panels.
        val ordered = projected.sortedBy { (_, point) -> atan2((point.y - center.y).toDouble(), (point.x - center.x).toDouble()) }
        val area2 = ordered.indices.sumOf { i ->
            val a = ordered[i].second; val b = ordered[(i + 1) % ordered.size].second
            (a.x * b.y - b.x * a.y).toDouble()
        }
        if (abs(area2) < 0.01) {
            logToast(context, "Those points are edge-on in this view; combine them from another view", Toast.LENGTH_LONG).show()
            return
        }
        pushUndo()
        val firstId = nextGuideId(guides)
        val pathId = nextGuidePathId(guides)
        val face = ordered.indices.map { i ->
            val start = ordered[i]; val end = ordered[(i + 1) % ordered.size]
            GuideLine(
                id = firstId + i,
                start = start.second,
                end = end.second,
                manual = true,
                pathId = pathId,
                colorIndex = guideColorIndex,
                strokeWidth = guideStrokeWidth,
                // A subtle fill makes a newly combined panel immediately readable; its
                // material or transparency can be changed in the editor that opens next.
                fillTransparency = if (i == 0) 50 else null,
                startZMm = start.first dot editPlane.toward,
                endZMm = end.first dot editPlane.toward,
                plane = editPlane
            )
        }
        guides = guides + face
        pickedItems = emptyList()
        pickedPoints = emptyList()
        selectedGuideId = firstId
        selectedWholeGuideId = firstId
        shapeEditId = firstId
        logToast(context, "Face combined: choose its material and thickness", Toast.LENGTH_SHORT).show()
    }
    /** Cut: the second picked item is cut out of the first. */
    fun cutPicked() {
        // The first pick is cut; the cutter is the last pick outside the first one's group (a ring's hole comes with it).
        val target = pickedItems.firstOrNull()
        val cutter = target?.let { t -> val group = withGroup(t); pickedItems.lastOrNull { p -> group.none { sameItem(it, p) } } }
        if (target == null || cutter == null) { logToast(context, "Pick two items: first the one to cut, then the cutter", Toast.LENGTH_LONG).show(); return }
        // Two shapes drawn in the same view are cut right in the drawing (3D follows, depth and material kept): a
        // cutter wholly inside becomes a hole, otherwise its bite is taken out and the cutter is used up.
        if (target.first == cutter.first && isClosedShape(target)) {
            if (editPlane != target.first) switchPlane(target.first)
            val closed = isClosedShape(cutter)
            val targetIds = itemGuides(target).map { it.id }.toSet(); val cutterIds = itemGuides(cutter).map { it.id }.toSet()
            applyShapeOperation(JunctionAction(if (closed) JunctionKind.SHAPE_OVERLAP else JunctionKind.CUT_SHAPE_LINE, target.second, cutter.second, Pt(0f, 0f)), if (closed) "cut-second" else "line")
            val cut = guides.none { it.id in targetIds }
            if (closed && cut) {
                guides = guides.filterNot { it.id in cutterIds && !it.hole }
                extrusions = extrusions.filter { e -> e.guideIds.none { it in cutterIds } || guides.any { g -> g.id in e.guideIds } }
                logToast(context, "Cut: the bite is taken out and the cutter is used up (Undo brings it back)", Toast.LENGTH_SHORT).show()
            }
            pickedItems = emptyList(); return
        }
        val targetSolid = extrusionOf(target); val cutterSolid = extrusionOf(cutter)
        if (targetSolid != null && cutterSolid != null) { pendingCut = SolidOp(target.first to targetSolid.id, cutter.first to cutterSolid.id, join = false); pickedItems = emptyList(); return }
        logToast(context, "Cut needs a shape and a shape or straight line in the same view, or two 3D objects", Toast.LENGTH_LONG).show()
    }
    /** Arrange: line up the picked items' centers on this view's horizontal or vertical, or space them evenly. */
    fun arrangePicked(horizontal: Boolean, distribute: Boolean) {
        val here = modelSetup().mapping(editPlane) ?: return
        val itemBoxes: List<Pair<Any, FloatArray>> = distinctPicked().mapNotNull { k -> itemBox(k)?.let { k to it } }
        val pointBoxes: List<Pair<Any, FloatArray>> = pickedPoints.filterNot { p -> pickedItems.any { it.first == p.first && itemGuides(it).any { g -> g.id == p.second } } }.mapNotNull { p -> ghostPointHere(p)?.let { at -> p to floatArrayOf(at.x, at.y, at.x, at.y) } }
        val boxes = itemBoxes + pointBoxes
        @Suppress("UNCHECKED_CAST")
        fun moveAny(k: Any, d: Vec3) = if (k is Triple<*, *, *>) moveGhostPoint(k as Triple<ModelPlane, Int, Boolean>, d) else moveItem(k as Pair<ModelPlane, Int>, d)
        if (boxes.size < 2 || distribute && boxes.size < 3) { logToast(context, if (distribute) "Pick at least three items to space them" else "Pick at least two items", Toast.LENGTH_SHORT).show(); return }
        pushUndo()
        fun cx(b: FloatArray) = (b[0] + b[2]) / 2.0
        fun cy(b: FloatArray) = (b[1] + b[3]) / 2.0
        if (!distribute) {
            val target = if (horizontal) boxes.map { cy(it.second) }.average() else boxes.map { cx(it.second) }.average()
            boxes.forEach { (k, b) -> moveAny(k, if (horizontal) here.modelDelta(0.0, target - cy(b)) else here.modelDelta(target - cx(b), 0.0)) }
        } else {
            val sorted = boxes.sortedBy { if (horizontal) cx(it.second) else cy(it.second) }
            val from = if (horizontal) cx(sorted.first().second) else cy(sorted.first().second); val to = if (horizontal) cx(sorted.last().second) else cy(sorted.last().second)
            sorted.forEachIndexed { i, (k, b) -> val target = from + (to - from) * i / (sorted.size - 1); moveAny(k, if (horizontal) here.modelDelta((target - cx(b)).toDouble(), 0.0) else here.modelDelta(0.0, (target - cy(b)).toDouble())) }
        }
    }
    fun deletePicked() {
        if (pickedItems.isEmpty() && pickedPoints.isEmpty()) return
        pushUndo()
        // A picked point removes the lines that end at it.
        pickedPoints.groupBy { it.first }.forEach { (plane, sels) ->
            val geometry = planeGeometry(plane)
            val spots = sels.mapNotNull { sel -> geometry.guides.firstOrNull { it.id == sel.second }?.let { if (sel.third) it.start else it.end } }
            val gone = geometry.guides.filter { g -> spots.any { distance(it, g.start) < 0.6 || distance(it, g.end) < 0.6 } }.map { it.id }.toSet()
            setPlaneShapes(plane, geometry.guides.filterNot { it.id in gone }, geometry.extrusions.map { it.copy(guideIds = it.guideIds.filterNot { id -> id in gone }) }.filter { it.guideIds.isNotEmpty() })
        }
        pickedPoints = emptyList()
        pickedItems.groupBy { it.first }.forEach { (plane, keys) ->
            val ids = keys.flatMap { k -> itemGuides(k).map { it.id } }.toSet()
            val geometry = planeGeometry(plane)
            setPlaneShapes(plane, geometry.guides.filterNot { it.id in ids }, geometry.extrusions.map { it.copy(guideIds = it.guideIds.filterNot { id -> id in ids }) }.filter { it.guideIds.isNotEmpty() })
        }
        itemGroups = itemGroups.map { g -> g.filterNot { m -> pickedItems.any { sameItem(m, it) } } }.filter { it.size >= 2 }
        pickedItems = emptyList()
    }
    fun setSelectMode(on: Boolean) {
        selectMode = on; pickedItems = emptyList(); pickedPoints = emptyList(); itemRectStart = null; itemRectEnd = null; selectedGhostPoints = emptyList()
        if (on) { tool = Tool.ADJUST; clearOwnSelection(); selectedForeign = null; selectedGhostPoint = null; panOnEmptyDrag = false }
    }
    /** Copies of the picked items, a little down and right, in their own views; the copies are picked. */
    /**
     * Copies items into their own view, a little down and right of the original, with new ids and new path and
     * group numbers so copy and original stay separate shapes. [full] keeps the material, fill and 3D depth;
     * otherwise the copy is just its lines. Returns the copies' keys.
     */
    fun copyItems(keys: List<Pair<ModelPlane, Int>>, full: Boolean, inPlace: Boolean = false): List<Pair<ModelPlane, Int>> {
        val setup = modelSetup(); val here = setup.mapping(editPlane)
        val frame = frameOf(editPlane)
        val step = if (inPlace) 0.0 else (min(frame.width, frame.height) * 0.03).coerceAtLeast(20.0)
        val offset = here?.modelDelta(step, step)
        val copies = mutableListOf<Pair<ModelPlane, Int>>()
        keys.distinctBy { k -> itemGuides(k).minOfOrNull { it.id }?.let { k.first to it } ?: k }.forEach { key ->
            val geometry = planeGeometry(key.first); val own = setup.mapping(key.first) ?: return@forEach
            val source = itemGuides(key)
            if (source.isEmpty()) return@forEach
            val shift = offset?.let { own.pixelDelta(it) } ?: Pt(step.toFloat(), step.toFloat())
            val dz = offset?.let { it dot key.first.toward } ?: 0.0
            fun moved(p: Pt) = Pt(p.x + shift.x, p.y + shift.y)
            var id = (geometry.guides.maxOfOrNull { it.id } ?: 0) + 1
            var path = (geometry.guides.mapNotNull { it.pathId }.maxOrNull() ?: 0) + 1
            var group = (geometry.guides.mapNotNull { it.groupId }.maxOrNull() ?: 0) + 1
            val ids = source.associate { it.id to id++ }
            val paths = source.mapNotNull { it.pathId }.distinct().associateWith { path++ }
            val groups = source.mapNotNull { it.groupId }.distinct().associateWith { group++ }
            val copied = source.map { g -> g.copy(id = ids.getValue(g.id), start = moved(g.start), end = moved(g.end), curveMid = g.curveMid?.let(::moved), startZMm = g.startZMm + dz, endZMm = g.endZMm + dz,
                pathId = g.pathId?.let(paths::get), groupId = g.groupId?.let(groups::get), manual = true, midDepthMm = null,
                materialId = if (full) g.materialId else null, materialTurnDeg = if (full) g.materialTurnDeg else 0.0, fillTransparency = if (full) g.fillTransparency else null) }
            val extrusion = if (!full) null else extrusionOf(key)?.let { e -> e.copy(id = (geometry.extrusions.maxOfOrNull { it.id } ?: 0) + 1, guideIds = e.guideIds.mapNotNull { ids[it] }, origin = moved(e.origin), handle = moved(e.handle)) }
            setPlaneShapes(key.first, geometry.guides + copied, geometry.extrusions + listOfNotNull(extrusion))
            copies += key.first to copied.first().id
        }
        return copies
    }
    fun copyPicked(full: Boolean) {
        if (pickedItems.isEmpty()) return
        pushUndo()
        pickedItems = copyItems(pickedItems, full); pickedPoints = emptyList()
    }
    /**
     * Outline: the selected shape's or object's border as plain lines, in the same place (the original keeps its fill,
     * material and depth). The new outline is selected, ready to move or use.
     */
    fun outlineOf(keys: List<Pair<ModelPlane, Int>>) {
        if (keys.isEmpty()) return
        pushUndo()
        val copies = copyItems(keys, full = false, inPlace = true)
        // Lines only: no fill, so it stays a figure of lines even where the original is solid.
        copies.forEach { key -> val g = planeGeometry(key.first); val ids = itemGuides(key).map { it.id }.toSet()
            setPlaneShapes(key.first, g.guides.map { if (it.id in ids) it.copy(fillTransparency = 100) else it }, g.extrusions) }
        copies.firstOrNull()?.let { copy -> (if (copy.first == editPlane) copy.second else encodedId(copy.first, copy.second)).let { id -> if (guides.any { it.id == id }) { selectedForeign = null; selectedWholeGuideId = id; selectedGuideId = id } } }
        logToast(context, "Outline made: plain lines over the original", Toast.LENGTH_SHORT).show()
    }
    /** Clone in Adjust: the selected line or shape (with its group); [full] copies material, fill and depth too. */
    fun cloneSelected(full: Boolean) {
        val guide = (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } } ?: return
        pushUndo()
        val copy = copyItems(withGroup(homeKey(guide)), full).firstOrNull() ?: return
        // Select the copy as it shows in this view.
        val shownId = if (copy.first == editPlane) copy.second else encodedId(copy.first, copy.second)
        selectedExtrusionId = null; selectedGuideEndpoint = null
        guides.firstOrNull { it.id == shownId }?.let { selectedWholeGuideId = it.id; selectedGuideId = it.id }
    }
    /** Makes picked closed shapes 3D (20 mm deep, editable in Properties). */
    /** Turns closed shapes into 3D objects [depthMm] deep. */
    fun giveDepth(shapes: List<Pair<ModelPlane, Int>>, depthMm: Double = 20.0) {
        shapes.forEach { key ->
            val geometry = planeGeometry(key.first)
            val ids = itemGuides(key).map { it.id }
            val pts = itemGuides(key).flatMap { listOf(it.start, it.end) }
            val origin = Pt(pts.map { it.x }.average().toFloat(), pts.map { it.y }.average().toFloat())
            val template = allExtrusions().firstOrNull()
            val e = Extrusion((geometry.extrusions.maxOfOrNull { it.id } ?: 0) + 1, ids, origin, origin, template?.vanishing ?: origin, template?.sun ?: origin, depthMm = depthMm, plane = key.first)
            setPlaneShapes(key.first, geometry.guides, geometry.extrusions + e)
        }
    }
    /** The cut face of each bar or pipe whose line points into the screen, so a post seen from above shows its tube. */
    fun memberSections(): List<Pair<Int, List<List<Pt>>>> {
        val setup = modelSetup(); val here = setup.mapping(editPlane) ?: return emptyList()
        return guides.filter { it.materialId != null && it.curveMid == null }.mapNotNull { g ->
            val material = materials.firstOrNull { it.id == g.materialId && !it.isSheet } ?: return@mapNotNull null
            val a = here.model(g.start, g.startZMm); val b = here.model(g.end, g.endZMm)
            val along = b - a
            if (along.length < 1e-6 || abs(along.normalized() dot editPlane.toward) < 0.7) return@mapNotNull null
            // The end nearer the viewer is the one seen.
            val near = if ((b dot editPlane.toward) > (a dot editPlane.toward)) b else a
            val loops = MemberBuilder.section(a, b, g.plane.toward, material.profile, g.materialTurnDeg).map { loop -> loop.map { here.pixel(it - a + near) } }
            loops.takeIf { it.isNotEmpty() }?.let { material.argb to it }
        }
    }
    /** Grow arrows for the closed shape selected in Modify (shapes that are already 3D in another view have their own). */
    fun growHandles(): List<GrowHandle> {
        if (!growMode || tool != Tool.ADJUST || selectMode || selectedForeign != null || selectedGuideEndpoint != null || selectedCurveId != null) return emptyList()
        val guide = (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } } ?: return emptyList()
        val key = homeKey(guide)
        if (!isClosedShape(key)) return emptyList()
        val setup = modelSetup(); val own = setup.mapping(key.first) ?: return emptyList(); val here = setup.mapping(editPlane) ?: return emptyList()
        val shape = itemGuides(key)
        val pts = shape.flatMap { listOf(it.start, it.end) }
        val c = Pt(pts.map { it.x }.average().toFloat(), pts.map { it.y }.average().toFloat())
        val z = shape.map { (it.startZMm + it.endZMm) / 2 }.average()
        val e = extrusionOf(key)
        val face = e?.let(::facePositionMm) ?: 0.0; val depth = e?.depthMm ?: 0.0
        val front = own.model(c, z + face); val back = front - key.first.toward * depth
        if (key.first == editPlane) return listOf(GrowHandle(key, front = true, ownView = true, anchor = here.pixel(front), dir = Pt(0f, -1f)))
        val d = canvasDirection(key.first.toward)
        if (hypot(d.x.toDouble(), d.y.toDouble()) < 0.5) return emptyList()
        return listOf(GrowHandle(key, true, false, here.pixel(front), d), GrowHandle(key, false, false, here.pixel(back), Pt(-d.x, -d.y)))
    }
    growStart = { point, hitRadius ->
        val hit = growHandles().firstOrNull { distance(handlePosition(it.anchor, it.dir, hitRadius), point) <= hitRadius * 1.6 }
        if (hit == null) false else {
            pushUndo()
            if (extrusionOf(hit.key) == null) giveDepth(listOf(hit.key), 1.0)
            val e = extrusionOf(hit.key)
            if (e != null) growDrag = GrowDrag(hit, point, facePositionMm(e), e.depthMm ?: 1.0, e.id)
            e != null
        }
    }
    growMove = move@{ point ->
        val g = growDrag ?: return@move
        val here = modelSetup().mapping(editPlane) ?: return@move
        val mmPerPixel = here.modelDelta(1.0, 0.0).length
        val along = ((point.x - g.start.x) * g.handle.dir.x + (point.y - g.start.y) * g.handle.dir.y) * mmPerPixel
        val depth = (g.depth + along).coerceAtLeast(0.5)
        // The front arrow moves the front side; the others keep it and move the back.
        val face = if (!g.handle.ownView && g.handle.front) g.face + (depth - g.depth) else g.face
        val plane = g.handle.key.first
        val geometry = planeGeometry(plane)
        val planeScale = scaleFor(planeLines(plane), plane)
        setPlaneShapes(plane, geometry.guides, geometry.extrusions.map { if (it.id == g.extrusionId) withFacePosition(extrusionWithDepth(it, depth, planeScale, unit), face, depth) else it })
    }
    /** Properties of the one picked item or point; items from another view open in their view. */
    fun propertiesPicked() {
        val point = pickedPoints.singleOrNull().takeIf { pickedItems.isEmpty() }
        val item = pickedItems.singleOrNull().takeIf { pickedPoints.isEmpty() }
        val plane = point?.first ?: item?.first ?: return
        setSelectMode(false)
        if (plane != editPlane) switchPlane(plane)
        tool = Tool.ADJUST
        if (point != null) { if (point.first == editPlane) { selectedGuideEndpoint = GuideEndpoint(point.second, point.third); selectedGuideId = point.second }; movePointDialog = true }
        else if (item != null) { selectedWholeGuideId = item.second; selectedGuideId = item.second; openProperties()?.invoke() }
    }
    /** A short name for an item: its 3D object's name, Circle, Oval, Shape, Broken line or Line, with its material. */
    fun itemName(key: Pair<ModelPlane, Int>): String {
        val pieces = itemGuides(key)
        val material = pieces.firstNotNullOfOrNull { g -> materials.firstOrNull { it.id == g.materialId } }?.name
        val base = extrusionOf(key)?.let { objectName(key.first to it.id) }
            ?: pieces.firstOrNull()?.let { g -> circleOf(g, planeGeometry(key.first).guides) }?.let { c -> if (c.any { it.hole }) "Hole" else "Circle" }
            ?: if (isClosedShape(key)) "Shape" else if (pieces.size > 1) "Broken line" else "Line"
        return base + (material?.let { " · $it" } ?: "")
    }
    /**
     * The name of what is selected, shown in red in place of the file name (Shape 2, Circle, Line, Point…), or null
     * when nothing is selected.
     */
    fun selectionName(): String? {
        if (show3d) return null
        if (selectMode) return when (val n = pickedItems.size + pickedPoints.size) { 0 -> null; 1 -> pickedItems.singleOrNull()?.let { itemName(it) } ?: "Point"; else -> "$n picked" }
        selectedModelId?.let { id -> return importedModels.firstOrNull { it.id == id }?.name ?: "Model" }
        selectedForeign?.let { return objectName(it) }
        if (selectedGuideEndpoint != null || selectedEndpoint != null || selectedVertices.isNotEmpty()) return if (selectedVertices.size > 1) "${selectedVertices.size} points" else "Point"
        (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } }?.let { return itemName(homeKey(it)) }
        selectedLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { return it.label.ifBlank { "Measurement" } }
        return null
    }
    /** Opens the selected thing's own properties (what tapping its name does). */
    fun openSelection() {
        if (selectMode) { if (pickedItems.size + pickedPoints.size == 1) propertiesPicked(); return }
        selectedModelId?.let { modelDialogId = it; return }
        selectedForeign?.let { foreignDialog = it; return }
        openProperties()?.invoke() ?: selectedLineId?.let { showLineEdit = true }
    }
    /** Deletes what is selected (undo brings it back). */
    fun deleteSelection() {
        if (selectMode) { deletePicked(); return }
        selectedModelId?.let { id -> pushUndo(); importedModels = importedModels.filterNot { it.id == id }; selectedModelId = null; return }
        selectedForeign?.let { (plane, extrusionId) ->
            val g = planeGeometry(plane)
            val ids = g.extrusions.firstOrNull { it.id == extrusionId }?.let { e -> extrusionGuides(e, g.guides).map { it.id }.toSet() } ?: return
            pushUndo(); setPlaneShapes(plane, g.guides.filterNot { it.id in ids }, g.extrusions.filterNot { it.id == extrusionId }); selectedForeign = null; return
        }
        if (selectedGuideEndpoint != null || selectedCurveId != null) { deleteSelectedGuidePoint(); return }
        (selectedWholeGuideId ?: selectedGuideId)?.let { id ->
            // A shape's holes go with it.
            val shape = closedGuideShapes(guides).firstOrNull { p -> p.any { it.id == id } }
            val holes = shape?.let { s -> closedGuideShapes(guides).filter { h -> h.all { it.hole } && shapePoints(h).all { pointInsidePolygon(it, shapePoints(s)) } } }.orEmpty().flatten().map { it.id }.toSet()
            deleteGuide(id)
            // deleteGuide already saved the undo step, so one Undo brings back the shape and its holes.
            if (holes.isNotEmpty()) guides = guides.filterNot { it.id in holes }
            selectedWholeGuideId = null; selectedGuideId = null; guideDeleteCandidateId = null; return
        }
        selectedLineId?.let { deleteMeasurement(it); selectedLineId = null }
    }

    BackHandler(enabled = presentation || bitmap != null || showSettings || showMaterials) {
        when {
            presentation -> setPresentation(false)
            showMaterials -> showMaterials = false
            showSettings -> showSettings = false
            showMeasurements -> showMeasurements = false
            bitmap != null -> {
                bitmap = null; projectId = null; projectName = ""; selectedLineId = null; selectedVertices = emptyList(); leave3d()
            }
        }
    }

    if (presentation && bitmap != null) {
        Box(Modifier.fillMaxSize()) {
            ImageEditor(
                modifier = Modifier.fillMaxSize(), bitmap = planeCanvas!!, lines = lines, guides = guides, ghosts = foreign.map { it.argb to it.canvas }, showSizes = showSizes, minZoom = minZoomForContent(), growArrows = growHandles().map { it.anchor to it.dir }, rimHandles = ovalHandles().map { it.first }, growOn = growMode, dragToDraw = tool == Tool.LINES && (lineDrawing == LineDrawing.RECTANGLE || lineDrawing == LineDrawing.CIRCLE), draftShape = if (tool == Tool.LINES) lineDrawing else null, mirrorX = editPlane in mirroredViews && editPlane != ModelPlane.TOP, mirrorY = editPlane == ModelPlane.TOP && editPlane in mirroredViews, sections = memberSections(), models = modelOutlines, selectedModelId = selectedModelId, foreignOverlay = if (tool == Tool.ADJUST) foreignOverlay() else null, joinPick = joinPick, blankCanvas = isBlankCanvas(), materialNames = materials.associate { it.id to it.name }, ghostLines = ghostLines, ghostNoDots = remember(planeStash) { ModelPlane.entries.flatMap { p -> planeStash[p]?.guides.orEmpty().let { g -> smoothOutlineIds(g).map { p to it } } }.toSet() }, highlightGhost = crossViewSelection, ghostPointMark = selectedGhostPoint?.let(::ghostPointHere),
                pickedOutlines = if (selectMode) pickedItems.flatMap { key -> val whole = itemGuides(key).size > 1 || isClosedShape(key) || extrusionOf(key) != null; itemOutlineHere(key).map { it to whole } } else emptyList(), itemRect = itemRectStart?.let { a -> itemRectEnd?.let { a to it } },
                ghostPointMarks = (selectedGhostPoints + if (selectMode) pickedPoints else emptyList()).mapNotNull(::ghostPointHere), onViewSize = ::fitCanvasToView, selectedGuideId = selectedGuideId, unit = unit, drawingScale = scale, draftStart = draftStart, pendingEnd = pendingEnd, tool = tool,
                selectedEndpoint = selectedEndpoint, magnifierPoint = selectedPoint ?: magnifierPoint, deleteCandidateId = deleteCandidateId, alignmentLineIds = alignmentLineIds, alignmentFlashToken = alignmentFlashToken, edgeSnapPoint = edgeSnapPoint, edgeFlashToken = edgeFlashToken, adjustmentIndicatorPoint = adjustmentIndicatorPoint, adjustmentLineId = adjustmentLineId, adjustmentGuideId = adjustmentGuideId, adjustmentFlashToken = adjustmentFlashToken, guideColorIndex = guideColorIndex, guideStrokeWidth = guideStrokeWidth, rotation = if (editPlane == ModelPlane.FRONT) levelRotation else 0f, unitVisibility = unitVisibility, zoom = zoom, pan = pan, cropFrame = cropFrame, pictureOptions = pictureOptions, extrusions = extrusions, selectedExtrusionId = selectedExtrusionId, onTripleTap = { point, radius -> activateExtrusion(point, radius) },
                onTransform = { nextZoom, nextPan -> zoom = nextZoom; pan = nextPan },
                onDoubleTap = { setPresentation(false) }, doubleTapEnabled = true,
                onJunction = ::onJunction, onAddPoint = ::addGuidePoint, onTap = ::handleTap, onLongPress = ::handleLongPress, onDragStart = ::handleDragStart, onDrag = ::handleDrag, onDragEnd = ::handleDragEnd, onDragCancel = ::handleDragCancel, onCropStart = ::startCrop, onCropDrag = ::dragCrop, onCropEnd = ::finishCropDrag,
                selectedCurveId = selectedCurveId, selectedLabelId = selectedLabelId, selectedWholeLineId = selectedWholeLineId, selectedGuideEndpoint = selectedGuideEndpoint, selectedWholeGuideId = selectedWholeGuideId, selectedVertices = selectedVertices, selectionStart = selectionStart, selectionEnd = selectionEnd, panOnEmptyDrag = panOnEmptyDrag, showFineControls = tool == Tool.ADJUST && (selectMode && (pickedItems.isNotEmpty() || pickedPoints.isNotEmpty()) || selectedGhostPoints.isNotEmpty() || selectedGhostPoint != null || selectedForeignPoint != null || selectedVertices.isNotEmpty() || selectedCurveId != null || selectedExtrusionId != null || selectedEndpoint != null || selectedLabelId != null || selectedWholeLineId != null || selectedGuideEndpoint != null || selectedWholeGuideId != null || selectedForeign != null || selectedModelId != null), onNudge = ::nudgeSelected
            )
            IconButton(onClick = { setPresentation(false) }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color(0xCC17211E), CircleShape)) {
                Icon(painterResource(R.drawable.ic_fullscreen_exit), contentDescription = "Exit full screen", tint = Color.White)
            }
        }
    } else if (showMeasurements && bitmap != null) {
        MeasurementsScreen(lines = lines, unit = unit, sumEnabled = pictureOptions.sum, onBack = { showMeasurements = false }, onEdit = { line -> selectedLineId = line.id; showLineEdit = true })
    } else if (showMaterials) {
        MaterialsScreen(materials, onChange = { materials = it; MaterialStore.save(context, it) }, onBack = { showMaterials = false })
    } else if (showSettings) {
        SettingsScreen(
            currentUnit = defaultUnit, colorIndex = defaultColor, unitVisibility = unitVisibility, exportGuides = exportGuides, hasProject = bitmap != null,
            onUnit = { next -> defaultUnit = next; preferences.edit().putString("default-unit", next).apply() },
            onPrecision = { measureUnit, places -> MeasurementPrecision.places[measureUnit] = places; preferences.edit().putInt("precision-$measureUnit", places).apply() },
            onColor = { next -> defaultColor = next; preferences.edit().putInt("default-color", next).apply() },
            onUnitVisibility = { next -> unitVisibility = next; preferences.edit().putString("unit-visibility", next).apply() },
            onExportGuides = { enabled -> exportGuides = enabled; preferences.edit().putBoolean("export-guides", enabled).apply() },
            onCopyDebugLog = {
                val summary = buildString {
                    append("Idea 3D ${runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()}\n")
                    if (bitmap != null) {
                        append("Project \"$projectName\", view ${editPlane.label}${if (editPlane in mirroredViews) " (other side)" else ""}, tool $tool${if (selectMode) ", select mode" else ""}\n")
                        ModelPlane.entries.forEach { p -> val g = planeGeometry(p); append("${p.label}: ${g.guides.size} lines, ${closedGuideShapes(g.guides).size} shapes, ${g.extrusions.size} 3D objects, ${g.lines.size} measurements\n") }
                        append("Selected: line ${selectedWholeGuideId ?: selectedGuideId}, object $selectedForeign, model $selectedModelId, picked $pickedItems\n")
                    }
                    append("Last actions:\n"); append(ActionLog.text())
                }
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("Idea 3D debug log", summary))
                logToast(context, "Debug log copied: paste it into your message", Toast.LENGTH_LONG).show()
            },
            onExportLog = { if (bitmap != null) { exportMeasurementLog(context, lines, scale, unit); logToast(context, "Measurement log saved", Toast.LENGTH_SHORT).show() } },
            onBack = { showSettings = false }
        )
    } else Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { if (bitmap != null) TopAppBar(title = {
            // With something selected, its name (in red, slowly blinking) stands here and opens its properties;
            // otherwise the file name, which opens the file menu.
            val selected = if (tool != Tool.CROP) selectionName() else null
            if (selected != null) SelectionTitle(selected) { openSelection() }
            else if (tool != Tool.CROP) OutlinedButton(onClick = { showPictureEdit = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(projectName.ifBlank { "Idea 3D" }, fontWeight = FontWeight.Bold, letterSpacing = 0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }, actions = {
            if (tool == Tool.CROP) {
                TextButton(onClick = ::cancelCrop) { Text("Cancel") }
                if (editPlane == ModelPlane.FRONT) IconButton(onClick = { rotateCrop(clockwise = false) }) { Icon(painterResource(R.drawable.ic_rotate_left), contentDescription = "Rotate left 90 degrees") }
                if (editPlane == ModelPlane.FRONT) IconButton(onClick = { rotateCrop(clockwise = true) }) { Icon(painterResource(R.drawable.ic_rotate_right), contentDescription = "Rotate right 90 degrees") }
                Button(onClick = ::applyCrop, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Confirm", fontSize = 12.sp) }
            } else {
            IconButton(onClick = {
                selectedVertices = emptyList()
                if (draftStart != null) { draftStart = null; pendingEnd = null }
                else if (undoHistory.isNotEmpty()) restoreLastUndo()
                else if (lines.isNotEmpty()) {
                    lines = lines.dropLast(1)
                    referenceCount = lines.count { it.isReference }
                    lines = recalculate(lines)
                    if (scale == null) tool = Tool.CALIBRATE
                }
            }, enabled = draftStart != null || undoHistory.isNotEmpty() || lines.isNotEmpty() || guides.isNotEmpty()) { Icon(painterResource(R.drawable.ic_undo), contentDescription = "Undo", tint = Color(0xFFB3261E)) }
            // Delete what is selected, right by Undo (which brings it back).
            if (selectionName() != null) IconButton(onClick = { ActionLog.add("tap Delete"); deleteSelection() }) { Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete selected", tint = Color(0xFFB3261E)) }
            }
        }) },
        bottomBar = { if (bitmap != null && tool == Tool.CROP) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), horizontalArrangement = Arrangement.Center) { OutlinedButton(onClick = { showCanvasSize = true }) { Text("Canvas size") } }
        else if (bitmap != null && !show3d) BottomControls(tool, lineDrawing, measureDrawing, (activePolylineId != null || activeMeasurePathId != null) && draftStart != null, guideColorIndex, guideStrokeWidth, unknownSize, onUnknownSize = { unknownSize = it; preferences.edit().putBoolean("unknown-size", it).apply() }, selectActive = selectMode, onSelect = { setSelectMode(!selectMode) }, onFind = { showFinder = true }, onTool = { if (selectMode) setSelectMode(false)
            // Select first, then Modify: the selected line or shape stays selected, as a whole.
            if (it == Tool.ADJUST && selectedGuideId != null && selectedWholeGuideId == null) selectedWholeGuideId = selectedGuideId
            panOnEmptyDrag = false; selectedVertices = emptyList(); selectedCurveId = null; tool = it; draftStart = null; pendingEnd = null; activePolylineId = null; activeMeasurePathId = null; deleteCandidateId = null; guideDeleteCandidateId = null }, onFinishBroken = ::finishBrokenGuide, onLineDrawing = { lineDrawing = it }, onMeasureDrawing = { measureDrawing = it }, onGuideStyle = { color, width -> guideColorIndex = color; guideStrokeWidth = width }) }
    ) { padding ->
        if (bitmap == null) LibraryScreen(Modifier.padding(padding), library, { picker.launch("image/*") }, ::newScene, { projectPicker.launch(arrayOf("application/zip", "application/octet-stream")) }, { showSettings = true }, { showMaterials = true }, { project -> renameTarget = project }, { project ->
            projectExportId = project.id
            projectExporter.launch("${project.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "Idea-3D" }}.zip")
        }, { project, nextUnit ->
            if (project.unit != nextUnit) {
                val multiplier = unitFactorMm(project.unit) / unitFactorMm(nextUnit)
                ProjectStore.save(context, project.copy(unit = nextUnit, lines = project.lines.map { it.copy(value = it.value * multiplier) }))
                library = ProjectStore.list(context)
            }
        }, { project ->
            BitmapFactory.decodeFile(ProjectStore.imageFile(context, project.id).absolutePath)?.let { loaded ->
                blankScene = project.blankScene; bitmap = loaded; projectId = project.id; projectName = project.name; projectCapturedAt = project.capturedAt; levelRotation = project.rotation; undoHistory = emptyList(); unit = project.unit; editPlane = ModelPlane.FRONT; planeFrames = project.frames; solidOps = project.solidOps; itemGroups = project.itemGroups; selectedModelId = null
                importedModels = project.models
                importedMeshes = project.models.mapNotNull { m -> runCatching { m.id to ModelImport.read("model.stl", ProjectStore.modelFile(context, project.id, m.id).readBytes()) }.getOrNull() }.toMap(); selectMode = false; pickedItems = emptyList(); fitPending = true
                planeStash = ModelPlane.entries.filter { it != ModelPlane.FRONT }.associateWith { plane -> PlaneGeometry(project.lines.filter { it.plane == plane }, project.guides.filter { it.plane == plane }, project.extrusions.filter { it.plane == plane }) }.filterValues { it != PlaneGeometry() }
                lines = project.lines.filter { it.plane == ModelPlane.FRONT }; guides = project.guides.filter { it.plane == ModelPlane.FRONT }; guidesDetected = project.guidesDetected; extrusions = project.extrusions.filter { it.plane == ModelPlane.FRONT }; selectedExtrusionId = null; selectedCurveId = null; pictureOptions = project.options; referenceCount = project.lines.count { it.isReference && it.plane == ModelPlane.FRONT }; scale = recomputeScale(project.lines.filter { it.plane == ModelPlane.FRONT }, project.unit) ?: if (project.blankScene) unitFactorMm(project.unit) else null; axisScale = axisScaleOf(project.lines.filter { it.plane == ModelPlane.FRONT }, project.unit); tool = Tool.NONE; zoom = 1f; pan = Offset.Zero
                // Show every view's lines in Front too.
                showView(ModelPlane.FRONT, ModelPlane.entries.associateWith { plane -> PlaneGeometry(project.lines.filter { it.plane == plane }, project.guides.filter { it.plane == plane }, project.extrusions.filter { it.plane == plane }) })
            }
        }, { project ->
            ProjectStore.moveToTrash(context, project.id); library = ProjectStore.list(context)
            scope.launch {
                if (snackbarHost.showSnackbar("Deleted \"${project.name}\"", actionLabel = "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) {
                    ProjectStore.restoreFromTrash(context, project.id); library = ProjectStore.list(context)
                }
            }
        })
        else Column(Modifier.padding(padding).fillMaxSize()) {
            val editHint = when {
                selectMode -> if (pickedItems.isEmpty() && pickedPoints.isEmpty()) "Drag a box to pick ${if (boxPicksItems) "whole items" else "whole items and loose points"} inside it, or tap points, lines, shapes or objects. Then Align or Space them from the top row" else "${pickedItems.size} item${if (pickedItems.size == 1) "" else "s"}, ${pickedPoints.size} point${if (pickedPoints.size == 1) "" else "s"} · drag one to move all, arrows step"
                growDrag != null -> growDrag!!.let { g -> planeGeometry(g.handle.key.first).extrusions.firstOrNull { it.id == g.extrusionId }?.depthMm?.let { "Depth ${formatMeasure(it / unitFactorMm(unit), unit)} $unit · drag along the arrow" } ?: "Drag along the arrow" }
                ovalDrag != null -> "Let go when the size is right"
                !show3d && tool == Tool.ADJUST && !selectMode && !growMode && ovalPieces() != null -> "Drag a square handle to resize the circle (side ones: width, top/bottom: height, for an oval), or set sizes in Properties"
                tool == Tool.ADJUST && !selectMode && (growHandles().isNotEmpty() || growMode && selectedForeign != null) -> "Drag a blinking orange arrow to grow it. Tap Grow again to finish"
                tool == Tool.LINES && lineDrawing == LineDrawing.CIRCLE -> if (draftStart == null) "Circle: drag from its center, or tap the center" else "Tap a point on the circle (sets the radius)"
                tool == Tool.LINES && lineDrawing == LineDrawing.RECTANGLE -> if (draftStart != null && pendingEnd != null && squareEnd(draftStart!!, pendingEnd!!).second) "Square" else if (draftStart == null) "Rectangle: drag corner to corner, or tap two corners. It blinks when square" else "Tap the opposite corner"
                joinPick != null -> if (joinPick!!.selected.size < 2) "Tap the two blinking parts you want to keep, then Finish" else if (joinPick!!.keepRemaining) "These two parts join; the others stay. Tap Finish" else "Green parts join; red blinking parts are deleted. Tap Finish"
                solidPick != null -> solidPick!!.let { (join, target) -> if (join) "Tap the object to join with ${objectName(target)}. Tap empty space to cancel" else "Tap the object that cuts ${objectName(target)}. Tap empty space to cancel" }
                selectedForeignPoint != null -> "Arrows move the point one step · the move button moves it by an exact size · drag a dot to move freely"
                show3d -> "3D: drag to orbit, two fingers to pan and zoom, tap a shape for exact sizes"
                tool == Tool.ADJUST && selectedForeign != null -> "Drag the shape to move it · round handles resize · dots move points · tap it again for exact sizes"
                tool == Tool.ADJUST && foreign.isNotEmpty() && selectedEndpoint == null && selectedGuideEndpoint == null && selectedWholeGuideId == null -> "Tap a shape to select it. Grey shapes were drawn in another view and can be moved and resized here"
                tool == Tool.NONE && foreign.isNotEmpty() -> "Tap a line or shape, then Modify to change it (grey ones too)"
                else -> null
            }
            val axesHint = if (show3d) "" else if (editPlane in mirroredViews) when (editPlane) { ModelPlane.FRONT -> "X ←  Y ↑  from behind"; ModelPlane.TOP -> "X →  Z ↑ front  from below"; ModelPlane.PROFILE -> "Z → front  Y ↑  from the left" }
                else when (editPlane) { ModelPlane.FRONT -> "X →  Y ↑  depth Z"; ModelPlane.TOP -> "X →  Z ↓ front  depth Y"; ModelPlane.PROFILE -> "Z ← front  Y ↑  depth X" }
            StatusStrip(tool, drawingEnd = draftStart != null, hint = editHint, primaryAction = if (selectMode) ({
                // Keep this out of the scrolling command row: a face starts with points,
                // not whole items, and must be discoverable before anything is selected.
                ActionIcon("Combine ${pickedPoints.size}/3", R.drawable.ic_join) { combinePickedPoints() }
            }) else null) {
                val red = Color(0xFFB3261E)
                // Always first, in the same place: fit everything in view, and show or hide sizes.
                val pick = joinPick
                // Turn whatever is selected, like the picture's turn buttons when cropping.
                val canTurn = !show3d && pick == null && (selectedModelId != null || (selectMode && (pickedItems.isNotEmpty() || pickedPoints.size >= 2)) ||
                    !selectMode && tool == Tool.ADJUST && (selectedWholeGuideId != null || selectedGuideId != null || selectedForeign != null || selectedVertices.size >= 2))
                @Composable fun TurnIcons() {
                    if (!canTurn) return
                    ActionIcon("Turn ↺", R.drawable.ic_rotate_left) { turnSelection(turnStep) }
                    ActionIcon("Turn ↻", R.drawable.ic_rotate_right) { turnSelection(-turnStep) }
                    ActionIcon("${turnStep.toInt()}°", R.drawable.ic_sizes) { turnStep = if (turnStep == 15.0) 90.0 else if (turnStep == 90.0) 5.0 else 15.0 }
                }
                // View 3D, Fit and Sizes: first normally; last while Modify has something selected, so its own buttons lead.
                @Composable fun ViewIcons() {
                    // Only shows the model in 3D; nothing is converted.
                    if (show3d) ActionIcon("Back to 2D", R.drawable.ic_open_plane) { leave3d() }
                    else ActionIcon("View 3D", R.drawable.ic_cube) { open3dView() }
                    if (!show3d) {
                        ActionIcon("Fit", R.drawable.ic_fit) { fitView() }
                        ActionIcon("Sizes", R.drawable.ic_sizes, on = showSizes) { showSizes = !showSizes; preferences.edit().putBoolean("show_sizes", showSizes).apply() }
                    }
                }
                val modifying = !show3d && !selectMode && pick == null && tool == Tool.ADJUST &&
                    (selectedWholeGuideId != null || selectedGuideId != null || selectedForeign != null || selectedVertices.isNotEmpty() || selectedGuideEndpoint != null || selectedEndpoint != null || selectedModelId != null)
                // In Select mode, lead with the point-to-face action.  The row scrolls on
                // small screens, and burying this new command after the view controls made
                // it too easy to miss.
                if (!modifying && !selectMode) ViewIcons()
                when {
                    show3d -> {}
                    pick != null -> {
                        ActionIcon("Cancel", R.drawable.ic_close) { joinPick = null }
                        ActionIcon("Keep others", R.drawable.ic_clone, on = pick.keepRemaining) { joinPick = pick.copy(keepRemaining = !pick.keepRemaining) }
                        ActionIcon("Finish", R.drawable.ic_check, enabled = pick.selected.size == 2) { finishJoin(pick) }
                    }
                    selectMode -> {
                        val n = pickedItems.size; val total = n + pickedPoints.size
                        // One picked line or shape: choose what it is made of.
                        val pickedGuide = pickedItems.singleOrNull()?.takeIf { pickedPoints.isEmpty() }?.let { (plane, id) -> guides.firstOrNull { it.id == if (plane == editPlane) id else encodedId(plane, id) } }
                        ActionIcon("Material", R.drawable.ic_save_material, enabled = pickedGuide != null) {
                            pickedGuide?.let { g -> if (closedGuideShapes(guides).any { path -> sameGuideObject(path.first(), g) }) shapeEditId = g.id else lineMaterialId = g.id }
                        }
                        TurnIcons()
                        ActionIcon("Align H", R.drawable.ic_align_horizontal, enabled = total >= 2) { arrangePicked(horizontal = true, distribute = false) }
                        ActionIcon("Align V", R.drawable.ic_align_vertical, enabled = total >= 2) { arrangePicked(horizontal = false, distribute = false) }
                        ActionIcon("Space H", R.drawable.ic_even_h, enabled = total >= 3) { arrangePicked(horizontal = true, distribute = true) }
                        ActionIcon("Space V", R.drawable.ic_even_v, enabled = total >= 3) { arrangePicked(horizontal = false, distribute = true) }
                        ActionIcon(if (pickedIsGroup()) "Ungroup" else "Group", R.drawable.ic_group, enabled = n >= 2, onClick = ::groupPicked)
                        ActionIcon("Merge/weld", R.drawable.ic_merge, enabled = n >= 2, onClick = ::mergePicked)
                        ActionIcon("Cut 1st", R.drawable.ic_cut, enabled = n >= 2, onClick = ::cutPicked)
                        ActionIcon("Outline", R.drawable.ic_outline, enabled = n >= 1) { outlineOf(pickedItems); pickedItems = emptyList() }
                        ActionIcon("Copy lines", R.drawable.ic_clone, enabled = n >= 1) { copyPicked(false) }
                        ActionIcon("Copy all", R.drawable.ic_clone_filled, enabled = n >= 1) { copyPicked(true) }
                        ActionIcon("Box: ${if (boxPicksItems) "items" else "points"}", R.drawable.ic_select_area, on = boxPicksItems) { boxPicksItems = !boxPicksItems }
                        ActionIcon("Unselect", R.drawable.ic_close, enabled = total > 0) { pickedItems = emptyList(); pickedPoints = emptyList() }
                        ViewIcons()
                    }
                    else -> {
                        // Most used first: Material, Grow, Outline, Turn, Clone, Weld (Properties and Delete are in the title bar).
                        (selectedWholeGuideId ?: selectedGuideId)?.let { id -> guides.firstOrNull { it.id == id } }?.let { guide ->
                            val inShape = closedGuideShapes(guides).any { path -> sameGuideObject(path.first(), guide) }
                            ActionIcon("Material", R.drawable.ic_save_material) { if (inShape) shapeEditId = guide.id else lineMaterialId = guide.id }
                            val itemKey = homeKey(guide)
                            if (itemGroups.any { members -> members.any { sameItem(it, itemKey) } }) ActionIcon("Ungroup", R.drawable.ic_group) { ungroupItem(itemKey) }
                            if (guide.groupId != null) ActionIcon("Unweld", R.drawable.ic_close) { unweldGuide(guide.id) }
                            // Grow: shows arrows to pull the shape into a 3D object (or change its depth).
                            if (tool == Tool.ADJUST && inShape) ActionIcon("Grow", R.drawable.ic_cube, on = growMode) { growMode = !growMode }
                            if (inShape) ActionIcon("Outline", R.drawable.ic_outline) { outlineOf(listOf(homeKey(guide))) }
                        }
                        // A 3D object from another view: Grow shows its front and back arrows.
                        if (tool == Tool.ADJUST && selectedForeign != null) ActionIcon("Grow", R.drawable.ic_cube, on = growMode) { growMode = !growMode }
                        if (tool == Tool.ADJUST) selectedForeign?.let { (plane, extrusionId) ->
                            ActionIcon("Outline", R.drawable.ic_outline) { planeGeometry(plane).let { g -> g.extrusions.firstOrNull { it.id == extrusionId }?.let { e -> extrusionGuides(e, g.guides).firstOrNull() } }?.let { outlineOf(listOf(plane to it.id)) } }
                        }
                        TurnIcons()
                        if (tool == Tool.ADJUST && selectedGuideEndpoint == null && selectedCurveId == null) selectedWholeGuideId?.let { wholeId ->
                            ActionIcon("Clone lines", R.drawable.ic_clone) { cloneSelected(false) }
                            ActionIcon("Clone all", R.drawable.ic_clone_filled) { cloneSelected(true) }
                            // Weld this line to every line it touches (an end on the other line, or the other's end on it).
                            ActionIcon("Weld", R.drawable.ic_merge) {
                                guides.firstOrNull { it.id == wholeId }?.let { g ->
                                    val key = homeKey(g)
                                    val others = planeGeometry(key.first).guides.map { key.first to it.id }.filterNot { sameItem(key, it) }
                                    if (!weldItems(listOf(key) + others, anchored = true)) logToast(context, "Nothing touches this line: put an end of one line on the other", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        if (tool == Tool.ADJUST && selectedForeign == null && (selectedGuideEndpoint != null || selectedEndpoint != null)) ActionIcon("Move by", R.drawable.ic_move_point) { movePointDialog = true }
                        // The same Align buttons as in Select, doing the same thing.
                        if (tool == Tool.ADJUST && selectedVertices.size + selectedGhostPoints.size >= 2) {
                            ActionIcon("Align H", R.drawable.ic_align_horizontal) { requestAlignment(true) }
                            ActionIcon("Align V", R.drawable.ic_align_vertical) { requestAlignment(false) }
                            ActionIcon("Depth", R.drawable.ic_cube) { showPointDepth = true }
                        }
                        if (tool == Tool.ADJUST) selectedForeignObject()?.let { target ->
                            if (selectedForeignPoint != null) ActionIcon("Move by", R.drawable.ic_move_point) { movePointDialog = true }
                            ActionIcon("Position", R.drawable.ic_cube) { foreignDialog = target.key }
                            ActionIcon("Cut 3D", R.drawable.ic_scissors) { startSolidOp(false, target.key) }
                            ActionIcon("Join 3D", R.drawable.ic_join) { startSolidOp(true, target.key) }
                            ActionIcon("Its view", R.drawable.ic_open_plane) { openInOwnView(target.key) }
                        }
                        if (tool == Tool.ADJUST && selectedForeign == null && selectedGuideEndpoint == null && selectedCurveId == null) ownSelectedObject()?.let { key ->
                            ActionIcon("Cut 3D", R.drawable.ic_scissors) { startSolidOp(false, key) }
                            ActionIcon("Join 3D", R.drawable.ic_join) { startSolidOp(true, key) }
                        }
                        if (tool == Tool.ADJUST && selectedGuideId != null && selectedVertices.isEmpty()) {
                            ActionIcon("Break pt", R.drawable.ic_point_break, on = pointKind == PointKind.BREAKPOINT) { pointKind = PointKind.BREAKPOINT }
                            ActionIcon("Curve pt", R.drawable.ic_point_curve, on = pointKind == PointKind.CURVE) { pointKind = PointKind.CURVE }
                            if (selectedGuideEndpoint != null || selectedCurveId != null) ActionIcon("Del point", R.drawable.ic_delete_point, tint = red, onClick = ::deleteSelectedGuidePoint)
                        }
                        if (tool == Tool.ADJUST && panOnEmptyDrag) ActionIcon("Box", R.drawable.ic_select_area) { panOnEmptyDrag = false }
                        if (tool == Tool.ADJUST && selectedExtrusionId != null) ActionIcon("Depth", R.drawable.ic_cube) { extrusionEditId = selectedExtrusionId }
                        if (!isBlankCanvas() && selectedVertices.isEmpty() && (tool != Tool.ADJUST || selectedGuideId == null)) ActionIcon("Crop", R.drawable.ic_crop) { startCrop() }
                    }
                }
                if (modifying) ViewIcons()
            }
            PlaneSwitcher(editPlane, show3d, axesHint, mirroredViews, onPlane = { plane, flip ->
                mirroredViews = if (flip) mirroredViews + plane else mirroredViews - plane
                leave3d(); if (plane != editPlane) switchPlane(plane) else fitView()
            })
            val model = viewerModel
            if (show3d && model != null) ThreeDViewerPane(model, viewerCamera, initialSun = ((extrusions.firstOrNull()?.sun?.x ?: ((bitmap?.width ?: 1) * 0.15f)) / (bitmap?.width ?: 1)).coerceIn(0f, 1f), onSunChange = { position ->
                val width = bitmap?.width ?: 1
                if (extrusions.any { abs(it.sun.x - position * width) > 0.01f }) { pushUndo(); extrusions = extrusions.map { it.copy(sun = Pt(position * width, 0f)) } }
            }, onEditObject = { index -> if (solidPick != null) finishSolidOp(viewerObjects.getOrNull(index)) else viewerEditObject = index }, modifier = Modifier.fillMaxWidth().weight(1f))
            else {
            CalibrationBanner(remember(lines, unit) { calibrationOf(lines, unit) })
            Box(Modifier.fillMaxWidth().weight(1f)) {
            ImageEditor(
                modifier = Modifier.fillMaxSize(),
                bitmap = planeCanvas!!, lines = lines, guides = guides, ghosts = foreign.map { it.argb to it.canvas }, showSizes = showSizes, minZoom = minZoomForContent(), growArrows = growHandles().map { it.anchor to it.dir }, rimHandles = ovalHandles().map { it.first }, growOn = growMode, dragToDraw = tool == Tool.LINES && (lineDrawing == LineDrawing.RECTANGLE || lineDrawing == LineDrawing.CIRCLE), draftShape = if (tool == Tool.LINES) lineDrawing else null, mirrorX = editPlane in mirroredViews && editPlane != ModelPlane.TOP, mirrorY = editPlane == ModelPlane.TOP && editPlane in mirroredViews, sections = memberSections(), models = modelOutlines, selectedModelId = selectedModelId, foreignOverlay = if (tool == Tool.ADJUST) foreignOverlay() else null, joinPick = joinPick, blankCanvas = isBlankCanvas(), materialNames = materials.associate { it.id to it.name }, ghostLines = ghostLines, ghostNoDots = remember(planeStash) { ModelPlane.entries.flatMap { p -> planeStash[p]?.guides.orEmpty().let { g -> smoothOutlineIds(g).map { p to it } } }.toSet() }, highlightGhost = crossViewSelection, ghostPointMark = selectedGhostPoint?.let(::ghostPointHere),
                pickedOutlines = if (selectMode) pickedItems.flatMap { key -> val whole = itemGuides(key).size > 1 || isClosedShape(key) || extrusionOf(key) != null; itemOutlineHere(key).map { it to whole } } else emptyList(), itemRect = itemRectStart?.let { a -> itemRectEnd?.let { a to it } },
                ghostPointMarks = (selectedGhostPoints + if (selectMode) pickedPoints else emptyList()).mapNotNull(::ghostPointHere), onViewSize = ::fitCanvasToView, selectedGuideId = selectedGuideId, unit = unit, drawingScale = scale, draftStart = draftStart, pendingEnd = pendingEnd, tool = tool, zoom = zoom, pan = pan,
                selectedEndpoint = selectedEndpoint, magnifierPoint = selectedPoint ?: magnifierPoint, deleteCandidateId = deleteCandidateId, alignmentLineIds = alignmentLineIds, alignmentFlashToken = alignmentFlashToken, edgeSnapPoint = edgeSnapPoint, edgeFlashToken = edgeFlashToken, adjustmentIndicatorPoint = adjustmentIndicatorPoint, adjustmentLineId = adjustmentLineId, adjustmentGuideId = adjustmentGuideId, adjustmentFlashToken = adjustmentFlashToken, guideColorIndex = guideColorIndex, guideStrokeWidth = guideStrokeWidth, rotation = if (editPlane == ModelPlane.FRONT) levelRotation else 0f, unitVisibility = unitVisibility, cropFrame = cropFrame, pictureOptions = pictureOptions, extrusions = extrusions, selectedExtrusionId = selectedExtrusionId, onTripleTap = { point, radius -> activateExtrusion(point, radius) },
                onTransform = { nextZoom, nextPan -> zoom = nextZoom; pan = nextPan }, onDoubleTap = { }, doubleTapEnabled = false,
                onJunction = ::onJunction, onAddPoint = ::addGuidePoint, onTap = ::handleTap, onLongPress = ::handleLongPress, onDragStart = ::handleDragStart, onDrag = ::handleDrag, onDragEnd = ::handleDragEnd, onDragCancel = ::handleDragCancel, onCropStart = ::startCrop, onCropDrag = ::dragCrop, onCropEnd = ::finishCropDrag,
                selectedCurveId = selectedCurveId, selectedLabelId = selectedLabelId, selectedWholeLineId = selectedWholeLineId, selectedGuideEndpoint = selectedGuideEndpoint, selectedWholeGuideId = selectedWholeGuideId, selectedVertices = selectedVertices, selectionStart = selectionStart, selectionEnd = selectionEnd, panOnEmptyDrag = panOnEmptyDrag, showFineControls = tool == Tool.ADJUST && (selectMode && (pickedItems.isNotEmpty() || pickedPoints.isNotEmpty()) || selectedGhostPoints.isNotEmpty() || selectedGhostPoint != null || selectedForeignPoint != null || selectedVertices.isNotEmpty() || selectedCurveId != null || selectedExtrusionId != null || selectedEndpoint != null || selectedLabelId != null || selectedWholeLineId != null || selectedGuideEndpoint != null || selectedWholeGuideId != null || selectedForeign != null || selectedModelId != null), onNudge = ::nudgeSelected
            )
            }
            // Leveling and perspective correction work on the photo, so they belong to the front plane.
            if (tool == Tool.CALIBRATE && editPlane == ModelPlane.FRONT) LevelDragger(rotation = levelRotation, onRotation = { levelRotation = it }, onAuto = {
                val source = bitmap ?: return@LevelDragger
                val controls = extrusions.flatMapIndexed { index, e -> listOf(GuideLine(Int.MIN_VALUE + index * 3, e.origin, e.handle, true), GuideLine(Int.MIN_VALUE + index * 3 + 1, e.vanishing, e.vanishing, true), GuideLine(Int.MIN_VALUE + index * 3 + 2, e.sun, e.sun, true)) }
                val corrected = perspectiveCorrect(source, lines, guides + controls)
                if (corrected != null) {
                    pushUndo(includeImage = true)
                    bitmap = corrected.bitmap
                    lines = recalculate(corrected.lines); guides = corrected.guides.take(guides.size)
                    val mappedControls = corrected.guides.drop(guides.size)
                    extrusions = extrusions.mapIndexed { index, e -> e.copy(origin = mappedControls[index * 3].start, handle = mappedControls[index * 3].end, vanishing = mappedControls[index * 3 + 1].start, sun = mappedControls[index * 3 + 2].start) }
                    // Keep the user's manual level rotation; Auto only rectifies perspective.
                    zoom = 1f; pan = Offset.Zero
                    projectId?.let { id -> ProjectStore.imageFile(context, id).outputStream().use { corrected.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                } else logToast(context, "Add two parallel-side references and one connecting reference", Toast.LENGTH_SHORT).show()
            })
            // The end-point hint lives in the status strip so the canvas never shifts mid-gesture.
            if (tool == Tool.CALIBRATE) CalibrationList(lines, unit, Modifier.fillMaxWidth().heightIn(max = 96.dp)) { line -> selectedLineId = line.id; showLineEdit = true }
            }
        }
    }
    renameTarget?.let { project -> ProjectTitleDialog(project, onDismiss = { renameTarget = null }, onSave = { title ->
        val renamed = project.copy(name = title)
        ProjectStore.save(context, renamed)
        if (projectId == project.id) projectName = title
        library = ProjectStore.list(context)
        renameTarget = null
    }) }
    if (showDistanceEntry && draftStart != null && pendingEnd != null) DistanceDialog(
        isReference = tool == Tool.CALIBRATE, referenceCount = referenceCount, unit = unit, unknownSize = unknownSize, onUnknownSize = { unknownSize = it; preferences.edit().putBoolean("unknown-size", it).apply() },
        onDismiss = { showDistanceEntry = false; pendingEnd = null; if (!pendingBrokenSegment) draftStart = null; pendingBrokenSegment = false },
        onConfirm = { entered, label, enteredUnit, levelOnly, axis ->
            val ref = tool == Tool.CALIBRATE
            val storedValue = entered?.times(unitFactorMm(enteredUnit))?.div(unitFactorMm(unit)) ?: 0.0
            val pathId = if (!ref && pendingBrokenSegment) activeMeasurePathId else null
            val line = MeasureLine(nextId(lines), draftStart!!, pendingEnd!!, storedValue, ref, label, colorIndex = defaultColor, isScaleReference = !levelOnly && entered != null, pathId = pathId, segmentIndex = if (pathId != null) lines.count { it.pathId == pathId } + 1 else 0, lengthSource = if (!ref && entered == null) LengthSource.UNKNOWN else LengthSource.MANUAL, axis = if (ref) axis.key else "")
            pushUndo()
            val previousScale = scale
            val updated = recalculate(lines + line)
            lines = if (previousScale == null && scale != null && pictureOptions.guess && !unknownSize) recalculate(updated.map { if (!it.isReference && it.lengthSource == LengthSource.UNKNOWN) it.copy(lengthSource = LengthSource.CALCULATED) else it }) else updated
            if (ref) referenceCount++
            draftStart = if (pendingBrokenSegment) pendingEnd else null; pendingEnd = null; showDistanceEntry = false; pendingBrokenSegment = false
            if (ref && scale != null) tool = Tool.MEASURE
        }
    )
    if (showCanvasSize && planeCanvas != null && tool == Tool.CROP) CanvasSizeDialog(planeCanvas.width, planeCanvas.height, onDismiss = { showCanvasSize = false }, onApply = ::expandCanvas)
    if (showPointDepth) {
        val selected = selectedVertices.toList()
        val eligibleGuides = guides.filter { g -> extrusions.any { ex -> extrusionGuides(ex, guides).any { it.id == g.id } && ex.depthMm != null && ex.depthMm > 0 } }
        val eligiblePoints = selected.filter { p -> eligibleGuides.any { distance(p, it.start) < 0.6 || distance(p, it.end) < 0.6 } }
        val values = eligibleGuides.flatMap { guide ->
            val depth = extrusions.firstOrNull { it.depthMm != null && it.depthMm > 0 && extrusionGuides(it, guides).any { member -> member.id == guide.id } }?.depthMm
            listOfNotNull(
                if (eligiblePoints.any { distance(it, guide.start) < 0.6 }) guide.startZMm to (guide.startThicknessMm ?: depth) else null,
                if (eligiblePoints.any { distance(it, guide.end) < 0.6 }) guide.endZMm to (guide.endThicknessMm ?: depth) else null
            )
        }
        fun common(values: List<Double>): Double? = values.firstOrNull()?.takeIf { first -> values.all { abs(it - first) < 0.000001 } }
        val commonZ = common(values.map { it.first })?.div(unitFactorMm(unit))
        val commonThickness = if (values.all { it.second != null }) common(values.mapNotNull { it.second })?.div(unitFactorMm(unit)) else null
        PointDepthDialog(unit, eligiblePoints.size, commonZ, commonThickness, onDismiss = { showPointDepth = false }, onSave = { position, thickness ->
            pushUndo()
            val positionMm = position?.times(unitFactorMm(unit)); val thicknessMm = thickness?.times(unitFactorMm(unit))
            guides = guides.map { guide ->
                if (eligibleGuides.none { it.id == guide.id }) guide else {
                    val start = eligiblePoints.any { distance(it, guide.start) < 0.6 }; val end = eligiblePoints.any { distance(it, guide.end) < 0.6 }
                    guide.copy(startZMm = if (start && positionMm != null) positionMm else guide.startZMm, endZMm = if (end && positionMm != null) positionMm else guide.endZMm,
                        startThicknessMm = if (start && thicknessMm != null) thicknessMm else guide.startThicknessMm,
                        endThicknessMm = if (end && thicknessMm != null) thicknessMm else guide.endThicknessMm)
                }
            }
            showPointDepth = false
        })
    }
    if (showDxfExport) {
        val viewName = if (editPlane in mirroredViews) oppositeLabel(editPlane) else editPlane.label
        DxfExportDialog(viewName, hasMeshes = importedModels.isNotEmpty(), onDismiss = { showDxfExport = false }) { includeLabels ->
            showDxfExport = false
            runCatching { dxfOfView(includeLabels) }.onSuccess { (entities, skipped) ->
                if (entities.isEmpty()) logToast(context, "Nothing to export in the $viewName view" + if (skipped > 0) " ($skipped item${if (skipped == 1) "" else "s"} couldn't be written)" else "", Toast.LENGTH_LONG).show()
                else {
                    pendingDxf = Triple(DxfExport.write(entities), entities.size, skipped)
                    val base = projectName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "Idea-3D" }
                    dxfExporter.launch("$base-$viewName.dxf")
                }
            }.onFailure { logToast(context, "DXF export: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
    modelDialogId?.let { id ->
        val model = importedModels.firstOrNull { it.id == id }
        val base = importedMeshes[id]
        if (model == null || base == null) modelDialogId = null
        else ModelDialog(model, base, unit, onDismiss = { modelDialogId = null }, onDelete = {
            pushUndo(); importedModels = importedModels.filterNot { it.id == id }; selectedModelId = null; modelDialogId = null
        }) { updated -> pushUndo(); importedModels = importedModels.map { if (it.id == id) updated else it }; modelDialogId = null }
    }
    shapeEditId?.let { id ->
        val selected = guides.firstOrNull { it.id == id }
        val path = selected?.let { target -> closedGuideShapes(guides).firstOrNull { group -> sameGuideObject(group.first(), target) } }
        if (path != null) {
          val points = shapePoints(path)
          val left = points.minOf { it.x }; val top = points.minOf { it.y }
          val width = (points.maxOf { it.x } - left).coerceAtLeast(0.001f); val height = (points.maxOf { it.y } - top).coerceAtLeast(0.001f)
          val shapeIds = guides.filter { sameGuideObject(it, path.first()) }.map { it.id }.toSet()
          val shapeExtrusion = extrusions.firstOrNull { it.guideIds.any { id -> id in shapeIds } }
          val shapeGuides = guides.filter { sameGuideObject(it, path.first()) }
          val current = shapeGuides.firstNotNullOfOrNull { g -> materials.firstOrNull { it.id == g.materialId } }
          ShapeEditDialog(path.first(), shapeExtrusion, guides, extrusions, unit, scale, width, height, materials, MadeOf(current?.id, current != null && !current.isSheet), onDismiss = { shapeEditId = null }, onSave = { color, transparency, widthValue, heightValue, depthValue, depthEdited, positionZ, made ->
            pushUndo()
            val targetWidth = widthValue?.let { value -> scale?.let { value * it } }
            val targetHeight = heightValue?.let { value -> scale?.let { value * it } }
            val sx = (targetWidth?.div(width) ?: 1.0).toFloat()
            val sy = (targetHeight?.div(height) ?: if (scale == null && widthValue != null && heightValue != null) width * heightValue / (widthValue * height) else 1.0).toFloat()
            // A circle or oval keeps its center; other shapes keep their top-left corner.
            val round = isCircleShape(path)
            val ax = if (round) left + width / 2f else left; val ay = if (round) top + height / 2f else top
            fun resize(point: Pt) = Pt(ax + (point.x - ax) * sx, ay + (point.y - ay) * sy)
            val ids = guides.filter { sameGuideObject(it, path.first()) }.map { it.id }.toSet()
            val changed = abs(sx - 1f) > 0.0001f || abs(sy - 1f) > 0.0001f
            guides = guides.map { guide -> if (guide.id in ids) guide.copy(start = resize(guide.start), end = resize(guide.end), curveMid = guide.curveMid?.let(::resize), colorIndex = color, fillTransparency = if (guide.pathId == path.first().pathId) (if (transparency == 100 && made.materialId != null && !made.frame) null else transparency) else guide.fillTransparency, lengthMm = if (changed) null else guide.lengthMm, shapeWidthMm = if (guide.id == path.first().id) widthValue?.times(unitFactorMm(unit)) else guide.shapeWidthMm, shapeHeightMm = if (guide.id == path.first().id) heightValue?.times(unitFactorMm(unit)) else guide.shapeHeightMm) else guide }
            extrusions = extrusions.map { extrusion -> if (extrusion.guideIds.any { it in ids }) { val resized = extrusion.copy(origin = resize(extrusion.origin), handle = resize(extrusion.handle)); resized.depthMm?.let { extrusionWithDepth(resized, it, scale, unit) } ?: resized } else extrusion }
            if (depthEdited && shapeExtrusion != null) extrusions = extrusions.map { extrusion ->
                if (extrusion.id != shapeExtrusion.id) extrusion else if (depthValue == null) extrusion.copy(depthMm = null) else extrusionWithDepth(extrusion, depthValue * unitFactorMm(unit), scale, unit)
            }
            // A flat shape becomes a 3D object as soon as it has a depth.
            val linesOnly = transparency == 100 && made.materialId == null
            // Lines only: just the outline, so it has no 3D body.
            if (linesOnly && shapeExtrusion != null) extrusions = extrusions.filterNot { it.id == shapeExtrusion.id }
            if (!linesOnly && depthEdited && shapeExtrusion == null && depthValue != null && depthValue > 0) giveDepth(listOf(homeKey(path.first())), depthValue * unitFactorMm(unit))
            val madeOf = materials.firstOrNull { it.id == made.materialId }
            if (made != MadeOf(current?.id, current != null && !current.isSheet)) {
                guides = guides.map { if (it.id in ids) it.copy(materialId = madeOf?.id, materialTurnDeg = 0.0) else it }
                val sheetMm = madeOf?.sheetMm
                when {
                    // A sheet is a 3D object as thick as the sheet.
                    sheetMm != null -> if (shapeExtrusion != null) extrusions = extrusions.map { if (it.id == shapeExtrusion.id) extrusionWithDepth(it, sheetMm, scale, unit) else it }
                        else giveDepth(listOf(homeKey(path.first())), sheetMm)
                    // A frame is only its edges.
                    made.frame && shapeExtrusion != null -> extrusions = extrusions.filterNot { it.id == shapeExtrusion.id }
                }
            }
            if (shapeExtrusion != null && positionZ != null) extrusions = extrusions.map { if (it.id == shapeExtrusion.id) it.copy(positionZMm = positionZ * unitFactorMm(unit)) else it }
            shapeEditId = null
          })
        }
    }
    extrusionEditId?.let { id -> extrusions.firstOrNull { it.id == id }?.let { extrusion ->
        var depth by remember(id, extrusion.depthMm, unit) { mutableStateOf(extrusion.depthMm?.let { (it / unitFactorMm(unit)).toString() } ?: "") }
        val value = depth.replace(',', '.').toDoubleOrNull()
        AlertDialog(onDismissRequest = { extrusionEditId = null }, title = { Text("Edit 3D") },
            text = { Column {
                OutlinedTextField(depth, { if (decimalInputPattern.matches(it)) depth = it }, label = { Text("Depth ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (scale == null) Text("Calibrate to apply the depth to the drawing. The entered depth is still saved.")
                TextButton(onClick = { pushUndo(); extrusions = extrusions.filterNot { it.id == id }; selectedExtrusionId = null; extrusionEditId = null }) { Text("Remove 3D", color = Color(0xFFB3261E)) }
            } },
            confirmButton = { TextButton(enabled = value != null && value >= 0, onClick = {
                pushUndo()
                extrusions = extrusions.map { current -> if (current.id != id) current else {
                    extrusionWithDepth(current, value!! * unitFactorMm(unit), scale, unit)
                } }
                extrusionEditId = null
            }) { Text("Save") } }, dismissButton = { TextButton(onClick = { extrusionEditId = null }) { Text("Cancel") } })
    } }
    pendingAlignment?.let { horizontal ->
        AlertDialog(onDismissRequest = { pendingAlignment = null }, title = { Text("Choose alignment reference") },
            text = { Column {
                Text("The chosen size is kept. Other selected lines' set sizes will be cleared.")
                alignmentReferences().forEachIndexed { index, reference ->
                    TextButton(onClick = { alignSelectedVertices(horizontal, reference) }) { Text("${index + 1}. ${reference.description}") }
                }
            } }, confirmButton = {}, dismissButton = { TextButton(onClick = { pendingAlignment = null }) { Text("Cancel") } })
    }
    lineMaterialId?.let { id -> guides.firstOrNull { it.id == id }?.let { guide ->
        var chosen by remember(id) { mutableStateOf(guide.materialId) }
        var turn by remember(id) { mutableStateOf(guide.materialTurnDeg) }
        AlertDialog(onDismissRequest = { lineMaterialId = null }, title = { Text("Material of this line") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("The line becomes this bar or pipe in 3D, as long as the line.", fontSize = 12.sp, color = Color(0xFF4D5A57))
                (listOf<Material?>(null) + materials.filterNot { it.isSheet }).forEach { material ->
                    Row(Modifier.fillMaxWidth().clickable { chosen = material?.id }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = chosen == material?.id, onClick = { chosen = material?.id })
                        if (material != null) ProfilePreview(material, Modifier.size(32.dp), turn)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(material?.name ?: "None (plain line)", fontSize = 14.sp)
                            material?.let { Text(it.description, fontSize = 12.sp, color = Color(0xFF4D5A57)) }
                        }
                    }
                }
                if (chosen != null) {
                    Text("Turn around the line: ${java.text.DecimalFormat("0.#").format(((turn % 360) + 360) % 360)}°", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-90.0 to "−90°", -15.0 to "−15°", 15.0 to "+15°", 90.0 to "+90°").forEach { (step, label) ->
                            OutlinedButton(onClick = { turn = ((turn + step) % 360 + 360) % 360 }, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1f).height(38.dp)) { Text(label, fontSize = 12.sp, maxLines = 1) }
                        }
                    }
                }
                TextButton(onClick = { lineMaterialId = null; showMaterials = true }) { Text("Edit materials…") }
            }
        }, confirmButton = { TextButton(onClick = {
            pushUndo()
            guides = guides.map { if (sameGuideObject(it, guide)) it.copy(materialId = chosen, materialTurnDeg = turn) else it }
            lineMaterialId = null
        }) { Text("Save") } }, dismissButton = { TextButton(onClick = { lineMaterialId = null }) { Text("Cancel") } })
    } ?: run { lineMaterialId = null } }
    guideLengthEditId?.let { id -> guides.firstOrNull { it.id == id }?.let { guide ->
        var length by remember(id, unit) { mutableStateOf("") }
        var materialId by remember(id) { mutableStateOf(guide.materialId) }
        var turn by remember(id) { mutableStateOf(guide.materialTurnDeg) }
        var lineColorIndex by remember(id) { mutableIntStateOf(guide.colorIndex) }
        // The line's extent along each model axis, editable for straight lines (the start stays put).
        val here = modelSetup().mapping(editPlane)
        val a3 = here?.model(guide.start, guide.startZMm); val b3 = here?.model(guide.end, guide.endZMm)
        val extent = if (a3 != null && b3 != null) b3 - a3 else null
        fun shown(v: Double?) = v?.let { java.text.DecimalFormat("0.##").format(it / unitFactorMm(unit)) } ?: ""
        var dxText by remember(id) { mutableStateOf(shown(extent?.x)) }
        var dyText by remember(id) { mutableStateOf(shown(extent?.y)) }
        var dzText by remember(id) { mutableStateOf(shown(extent?.z)) }
        fun setExtent(target: Vec3) {
            val start = a3 ?: return; val map = here ?: return
            val endModel = start + target
            val oldEnd = guide.end
            val newEnd = map.pixel(endModel); val newEndZ = endModel dot editPlane.toward
            pushUndo()
            guides = guides.map { item ->
                if (!sameGuideObject(item, guide) && item.id != guide.id) item
                else item.copy(start = if (distance(item.start, oldEnd) < 0.6) newEnd else item.start, end = if (distance(item.end, oldEnd) < 0.6) newEnd else item.end,
                    startZMm = if (distance(item.start, oldEnd) < 0.6) newEndZ else item.startZMm, endZMm = if (distance(item.end, oldEnd) < 0.6) newEndZ else item.endZMm, lengthMm = null)
            }
            guideLengthEditId = null
        }
        var lineWidth by remember(id) { mutableStateOf(guide.strokeWidth) }
        val value = length.replace(',', '.').toDoubleOrNull()
        val currentMm = scale?.let { s -> hypot(guidePoints(guide).zipWithNext().sumOf { (a, b) -> frontLength(a, b, s, axisScale) } * unitFactorMm(unit), guide.endZMm - guide.startZMm) }
        /** Applies the chosen material to every segment of this (broken) line. */
        fun saveMaterial() {
            if (materialId == guide.materialId && turn == guide.materialTurnDeg && lineColorIndex == guide.colorIndex && lineWidth == guide.strokeWidth) return
            guides = guides.map { if (sameGuideObject(it, guide)) it.copy(materialId = materialId, materialTurnDeg = turn, colorIndex = lineColorIndex, strokeWidth = lineWidth) else it }
        }
        AlertDialog(onDismissRequest = { guideLengthEditId = null }, title = { Text("Line properties") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                currentMm?.let { Text("Now ${formatLength(it, LengthUnit.of(unit), MeasurementPrecision.places[unit] ?: 2)}", fontSize = 13.sp, color = Color(0xFF4D5A57)) }
                OutlinedTextField(length, { if (decimalInputPattern.matches(it)) length = it }, label = { Text("Length ($unit)") }, placeholder = { Text("Keep as is") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (scale == null) Text("Set a calibration length to resize the line to scale.")
                if (extent != null && guide.curveMid == null) {
                    // Size along X, Y and Z: what the line covers in each direction, wherever it was drawn.
                    Text("Size along each axis (${unit}), from its start", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(dxText, { dxText = it }, label = { Text("X") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                        OutlinedTextField(dyText, { dyText = it }, label = { Text("Y") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                        OutlinedTextField(dzText, { dzText = it }, label = { Text("Z") }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val parsed = listOf(dxText, dyText, dzText).map { it.replace(',', '.').toDoubleOrNull()?.times(unitFactorMm(unit)) }
                        OutlinedButton(enabled = parsed.all { it != null }, onClick = { setExtent(Vec3(parsed[0]!!, parsed[1]!!, parsed[2]!!)) }) { Text("Apply sizes", fontSize = 12.sp) }
                        // Removes the part going into depth here, so the line lies flat in this view.
                        val n = editPlane.toward
                        OutlinedButton(onClick = { setExtent(extent - n * (extent dot n)) }) { Text("No depth here", fontSize = 12.sp) }
                    }
                }
                Text("Color and thickness", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    guideColors.indices.forEach { i -> Box(Modifier.size(30.dp).background(guideColor(i), CircleShape).border(if (i == lineColorIndex) 3.dp else 0.dp, Color(0xFF00897B), CircleShape).clickable { lineColorIndex = i }) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3f to "Thin", 5f to "Medium", 8f to "Thick").forEach { (w, label) -> FilterChip(selected = lineWidth == w, onClick = { lineWidth = w }, label = { Text(label, fontSize = 13.sp) }) }
                }
                // A material makes the line a bar or pipe in 3D and in exports.
                Text("Material", fontWeight = FontWeight.Medium)
                (listOf<Material?>(null) + materials.filterNot { it.isSheet }).forEach { material ->
                    Row(Modifier.fillMaxWidth().clickable { materialId = material?.id }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = materialId == material?.id, onClick = { materialId = material?.id })
                        if (material != null) ProfilePreview(material, Modifier.size(32.dp))
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(material?.name ?: "None (plain line)", fontSize = 14.sp)
                            material?.let { Text(it.description, fontSize = 12.sp, color = Color(0xFF4D5A57)) }
                        }
                    }
                }
                materials.firstOrNull { it.id == materialId }?.let { chosen ->
                    // Turn the profile around the line: the picture is the end of the bar, with the front view's depth axis up.
                    Text("Turn around the line", fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfilePreview(chosen, Modifier.size(64.dp), turn)
                        Text("${java.text.DecimalFormat("0.#").format(((turn % 360) + 360) % 360)}°", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(-90.0 to "−90°", -15.0 to "−15°", 15.0 to "+15°", 90.0 to "+90°").forEach { (step, label) ->
                            OutlinedButton(onClick = { turn = ((turn + step) % 360 + 360) % 360 }, contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.weight(1f).height(38.dp)) { Text(label, fontSize = 12.sp, maxLines = 1) }
                        }
                    }
                }
                TextButton(onClick = { guideLengthEditId = null; showMaterials = true }) { Text("Edit materials…") }
            } },
            confirmButton = { TextButton(enabled = length.isBlank() || value != null && value > 0, onClick = {
                if (value == null || value <= 0) { pushUndo(); saveMaterial(); guideLengthEditId = null; return@TextButton }
                val mm = value!! * unitFactorMm(unit)
                val oldEnd = guide.end
                // Scale the whole line in 3D, its depth included, so it is this long in every view.
                val flatLength = scale?.let { s -> guidePoints(guide).zipWithNext().sumOf { (a, b) -> frontLength(a, b, s, axisScale) } } ?: 0.0
                val depthUnits = (guide.endZMm - guide.startZMm) / unitFactorMm(unit)
                val currentLength = hypot(flatLength, depthUnits)
                val factor = if (currentLength > 1e-9) value / currentLength else 1.0
                val newEnd = Pt((guide.start.x + (guide.end.x - guide.start.x) * factor).toFloat(), (guide.start.y + (guide.end.y - guide.start.y) * factor).toFloat())
                val newEndZ = guide.startZMm + (guide.endZMm - guide.startZMm) * factor
                pushUndo()
                guides = guides.map { item ->
                    if (item.id == guide.id) item.copy(end = newEnd, endZMm = newEndZ, curveMid = item.curveMid?.let { Pt((item.start.x + (it.x - item.start.x) * factor).toFloat(), (item.start.y + (it.y - item.start.y) * factor).toFloat()) }, lengthMm = mm)
                    else if (sameGuideObject(item, guide)) item.copy(start = if (distance(item.start, oldEnd) < 0.6) newEnd else item.start, end = if (distance(item.end, oldEnd) < 0.6) newEnd else item.end,
                        startZMm = if (distance(item.start, oldEnd) < 0.6) newEndZ else item.startZMm, endZMm = if (distance(item.end, oldEnd) < 0.6) newEndZ else item.endZMm, lengthMm = if (distance(item.start, oldEnd) < 0.6 || distance(item.end, oldEnd) < 0.6) null else item.lengthMm)
                    else item
                }
                saveMaterial()
                guideLengthEditId = null
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { guideLengthEditId = null }) { Text("Cancel") } })
    } }
    junctionAction?.let { action ->
        val split = action.kind == JunctionKind.SPLIT_MEASUREMENT || action.kind == JunctionKind.SPLIT_GUIDE
        if (action.kind == JunctionKind.SHAPE_OVERLAP) AlertDialog(
            onDismissRequest = { junctionAction = null }, title = { Text("Overlapping shapes") },
            text = { Column {
                val shapes = closedGuideShapes(guides)
                val first = shapes.firstOrNull { shape -> shape.any { it.id == action.firstId } }
                val second = shapes.firstOrNull { shape -> shape.any { it.id == action.secondId } }
                if (first != null && second != null) ShapePairPreview(first, second)
                Text("Choose how to combine the two shapes. The cutting shape remains unchanged.")
                TextButton(onClick = { applyShapeOperation(action, "merge") }) { Text("Merge shapes") }
                TextButton(onClick = { applyShapeOperation(action, "cut-first") }) { Text("Cut shape 2 with shape 1") }
                TextButton(onClick = { applyShapeOperation(action, "cut-second") }) { Text("Cut shape 1 with shape 2") }
            } },
            confirmButton = {}, dismissButton = { TextButton(onClick = { junctionAction = null }) { Text("Cancel") } }
        )
        else if (action.kind == JunctionKind.CUT_SHAPE_LINE) AlertDialog(
            onDismissRequest = { junctionAction = null }, title = { Text("Cut shape") },
            icon = { Icon(painterResource(R.drawable.ic_scissors), contentDescription = "Cut") },
            text = { Text("Cut the shape into separate pieces along this line? The cutting line stays in place.") },
            confirmButton = { TextButton(onClick = { applyShapeOperation(action, "line") }) { Text("Cut") } },
            dismissButton = { TextButton(onClick = { junctionAction = null }) { Text("Cancel") } }
        )
        else if (action.kind == JunctionKind.CLOSE_GUIDE_PATH) AlertDialog(
            onDismissRequest = { junctionAction = null }, title = { Text("Close shape") },
            text = { Text("Join the endpoints to make a closed, color-editable shape.") },
            confirmButton = { TextButton(onClick = { applyJunction(action) }) { Text("Close shape") } },
            dismissButton = { TextButton(onClick = { junctionAction = null }) { Text("Cancel") } }
        )
        else if (action.kind == JunctionKind.MERGE_SHAPE_GUIDE) AlertDialog(
            onDismissRequest = { junctionAction = null }, title = { Text("Merge with shape") },
            text = { Text("Join both ends to the shape and keep its color and fill.") },
            confirmButton = { TextButton(onClick = { applyJunction(action) }) { Text("Merge") } },
            dismissButton = { TextButton(onClick = { junctionAction = null }) { Text("Cancel") } }
        )
        else if (action.kind == JunctionKind.JOIN_GUIDE_ENDPOINTS || action.kind == JunctionKind.JOIN_MEASURE_ENDPOINTS) AlertDialog(
            onDismissRequest = { junctionAction = null },
            title = { Text("Join endpoints") },
            text = { Text("Connect these endpoints without deleting either line.") },
            confirmButton = { TextButton(onClick = { applyJunction(action) }) { Text("Join") } },
            dismissButton = { TextButton(onClick = { junctionAction = null }) { Text("Cancel") } }
        )
        else if (split) AlertDialog(
            onDismissRequest = { junctionAction = null },
            title = { Text("Split at point") },
            text = { Text("Split this broken line into two independently movable and deletable lines.") },
            confirmButton = { TextButton(onClick = { applyJunction(action) }) { Text("Split") } },
            dismissButton = { TextButton(onClick = { junctionAction = null }) { Text("Cancel") } }
        )
    }
    selectedLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
        if (showLineEdit) LineEditDialog(line, unit, measurementPointNames(lines)[line.id], onDismiss = { showLineEdit = false }, onSave = save@{ value, label, colorIndex, lengthEdited, startZ, endZ ->
            val startMm = startZ * unitFactorMm(unit); val endMm = endZ * unitFactorMm(unit)
            val depthChanged = startMm != line.startZMm || endMm != line.endZMm
            val fixed = value > 0 && (line.isScaleReference || lengthEdited)
            val dz = abs(endMm - startMm) / unitFactorMm(unit)
            if (fixed && dz > value) { logToast(context, "Z difference cannot exceed the measurement length", Toast.LENGTH_LONG).show(); return@save }
            if ((depthChanged || lengthEdited && line.hasDepth()) && fixed && scale == null) { logToast(context, "Set a calibration scale first", Toast.LENGTH_LONG).show(); return@save }
            var start = line.start; var end = line.end
            val moveStart = startMm != line.startZMm && endMm == line.endZMm
            if ((depthChanged || lengthEdited && line.hasDepth()) && fixed && scale != null) {
                val oldLength = frontLength(start, end, scale!!, axisScale)
                if (distance(start, end) < 0.00001) { logToast(context, "Cannot set direction for a zero-length line", Toast.LENGTH_LONG).show(); return@save }
                val projection = projectedLengthForFixedTrue(value, dz) ?: 0.0
                val ratio = (projection / oldLength).toFloat()
                if (moveStart) start = Pt(end.x + (start.x - end.x) * ratio, end.y + (start.y - end.y) * ratio)
                else end = Pt(start.x + (end.x - start.x) * ratio, start.y + (end.y - start.y) * ratio)
                val oldPoint = if (moveStart) line.start else line.end
                if (line.pathId != null && lines.any { it.id != line.id && it.pathId == line.pathId && it.isScaleReference && (distance(it.start, oldPoint) < 0.6 || distance(it.end, oldPoint) < 0.6) }) {
                    logToast(context, "Split this point from the adjoining fixed-length segment first", Toast.LENGTH_LONG).show(); return@save
                }
            }
            pushUndo()
            lines = recalculate(lines.map { item ->
                if (item.id == line.id) item.copy(start = start, end = end, startZMm = startMm, endZMm = endMm, value = value, label = label, colorIndex = colorIndex, isScaleReference = if (lengthEdited && value <= 0.0 && !item.isReference) false else item.isScaleReference || lengthEdited, lengthSource = if (!lengthEdited) item.lengthSource else if (value <= 0.0 && !item.isReference) LengthSource.UNKNOWN else LengthSource.MANUAL)
                else if (line.pathId != null && item.pathId == line.pathId) item.copy(
                    start = if (distance(item.start, line.start) < 0.6) start else if (distance(item.start, line.end) < 0.6) end else item.start,
                    end = if (distance(item.end, line.start) < 0.6) start else if (distance(item.end, line.end) < 0.6) end else item.end,
                    startZMm = if (distance(item.start, line.start) < 0.6) startMm else if (distance(item.start, line.end) < 0.6) endMm else item.startZMm,
                    endZMm = if (distance(item.end, line.start) < 0.6) startMm else if (distance(item.end, line.end) < 0.6) endMm else item.endZMm)
                else item
            })
            showLineEdit = false
        }, onDelete = {
            pushUndo()
            lines = lines.filterNot { it.id == line.id || line.pathId != null && it.pathId == line.pathId }
            referenceCount = lines.count { it.isReference }
            lines = recalculate(lines)
            if (scale == null) tool = Tool.CALIBRATE
            selectedLineId = null; showLineEdit = false
        })
    }
    modelReport?.let { report ->
        val safeName = projectName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "Idea3D" }
        ModelReportDialog(report, unit, onDismiss = { modelReport = null }, onStl = { modelReport = null; stlExporter.launch("$safeName.stl") }, on3mf = { modelReport = null; threeMfExporter.launch("$safeName.3mf") })
    }
    if (showPictureEdit && bitmap != null) PictureEditDialog(projectName, unit, pictureOptions, scale != null, onExportImage = {
        bitmap?.let { image -> exportAnnotated(context, image, lines, guides, exportGuides, unit, unitVisibility, levelRotation, pictureOptions, extrusions); logToast(context, "Saved to Pictures/Idea 3D", Toast.LENGTH_SHORT).show() }
    }, onExportDxf = { showPictureEdit = false; showDxfExport = true }, onImportModel = { showPictureEdit = false; modelPicker.launch(arrayOf("*/*")) }, onMaterialList = { showPictureEdit = false; showMaterialList = true }, onMeasurements = { showPictureEdit = false; showMeasurements = true }, onSaveMaterial = {
        // The selected shape, or else the biggest closed shape that isn't a hole inside another one.
        val shapes = closedGuideShapes(guides)
        val chosen = selectedGuideId?.let { id -> guides.firstOrNull { it.id == id } }?.let { g -> shapes.firstOrNull { path -> sameGuideObject(path.first(), g) } }
            ?: shapes.filter { shape -> shapes.none { other -> other !== shape && shapePoints(shape).all { pointInsidePolygon(it, shapePoints(other)) } } }
                .maxByOrNull { shape -> val p = shapePoints(shape); abs(p.indices.sumOf { i -> val a = p[i]; val b = p[(i + 1) % p.size]; (a.x * b.y - b.x * a.y).toDouble() }) }
        if (chosen == null) logToast(context, "Draw a closed shape first: the outline of the cross-section, with any holes inside it.", Toast.LENGTH_LONG).show()
        else { showPictureEdit = false; saveMaterialShape = chosen }
    }, onExport3d = {
        val exportGuidesSnapshot = allGuides(); val exportExtrusionsSnapshot = allExtrusions()
        val exportSetup = modelSetup(); val exportOps = solidOps; val exportMaterials = materials.associateBy { it.id }; val exportModels = placedModels().map { it.second }
        modelReport = ModelReport.Checking
        scope.launch {
            val result = withContext(Dispatchers.Default) { runCatching {
                val meshes = buildMeshes(exportGuidesSnapshot, exportExtrusionsSnapshot, exportOps, exportSetup, exportMaterials, exportModels)
                File(context.cacheDir, "pending-3d-export.stl").writeBytes(ModelExport.stl(meshes))
                File(context.cacheDir, "pending-3d-export.3mf").writeBytes(ModelExport.threeMf(meshes))
                meshes
            } }
            modelReport = result.fold(
                onSuccess = { ModelReport.Ready(it) },
                onFailure = { error -> ModelReport.Problems((error as? MeshException)?.issues ?: listOf(MeshIssue("Model", error.localizedMessage ?: "could not be built"))) },
            )
        }
    }, onDismiss = { showPictureEdit = false }, onSave = { name, nextUnit, precision, options ->
        projectName = name
        val enableGuess = options.guess && !pictureOptions.guess
        pictureOptions = options
        changePictureUnit(nextUnit)
        if (enableGuess) {
            unknownSize = false; preferences.edit().putBoolean("unknown-size", false).apply()
            if (scale != null) { pushUndo(); lines = recalculate(lines.map { if (!it.isReference && it.lengthSource == LengthSource.UNKNOWN) it.copy(lengthSource = LengthSource.CALCULATED) else it }) }
        }
        val editor = preferences.edit()
        precision.forEach { (measureUnit, places) -> MeasurementPrecision.places[measureUnit] = places; editor.putInt("precision-$measureUnit", places) }
        editor.apply()
        showPictureEdit = false
    })
    viewerEditObject?.let { index ->
        val target = viewerObjects.getOrNull(index)
        val geometry = target?.let { planeGeometry(it.first) }
        val extrusion = geometry?.extrusions?.firstOrNull { it.id == target.second }
        val name = viewerModel?.objectNames?.getOrNull(index) ?: "Object"
        if (target == null || geometry == null || extrusion == null) viewerEditObject = null
        else ObjectDepthDialog(name, target.first, facePositionMm(extrusion), extrusion.depthMm, unit, solidOpsOf(target), onCut = { viewerEditObject = null; startSolidOp(false, target) }, onJoin = { viewerEditObject = null; startSolidOp(true, target) }, onDismiss = { viewerEditObject = null }, onRemove3d = {
            pushUndo()
            setPlaneShapes(target.first, geometry.guides, geometry.extrusions.filterNot { it.id == extrusion.id })
            viewerEditObject = null
            if (allExtrusions().isEmpty()) leave3d()
        }) { faceMm, depthMm ->
            pushUndo()
            val resized = if (depthMm != null) extrusionWithDepth(extrusion, depthMm, modelSetup().scales[target.first], unit) else extrusion
            setPlaneShapes(target.first, geometry.guides, geometry.extrusions.map { if (it.id == extrusion.id) withFacePosition(resized, faceMm, resized.depthMm ?: 0.0) else it })
            viewerEditObject = null
        }
    }
    if (showFinder) {
        val items = remember(guides, planeStash, extrusions, materials) {
            var letter = 0
            val names = HashMap<Pair<ModelPlane, Pair<Int, Int>>, String>()
            fun pointName(plane: ModelPlane, p: Pt) = names.getOrPut(plane to (p.x.roundToInt() to p.y.roundToInt())) { pointName(++letter) }
            ModelPlane.entries.flatMap { plane ->
                val geometry = planeGeometry(plane)
                val pending = geometry.guides.toMutableList()
                val groups = mutableListOf<List<GuideLine>>()
                while (pending.isNotEmpty()) { val first = pending.first(); val group = pending.filter { sameGuideObject(it, first) }; pending.removeAll(group); groups += group }
                groups.map { group ->
                    val points = mutableListOf(group.first().start)
                    group.forEach { g -> if (distance(points.last(), g.start) > 0.6) points += g.start; points += g.end }
                    val closed = points.size > 2 && distance(points.first(), points.last()) < 0.6
                    if (closed) points.removeAt(points.lastIndex)
                    val letters = points.map { pointName(plane, it) }
                    val name = if (letters.size <= 6) letters.joinToString("–") else "${letters.take(3).joinToString("–")}…${letters.last()} (${letters.size} points)"
                    val extruded = geometry.extrusions.any { e -> extrusionGuides(e, geometry.guides).any { g -> group.any { it.id == g.id } } }
                    val material = group.firstNotNullOfOrNull { g -> materials.firstOrNull { it.id == g.materialId } }
                    val kind = when {
                        extruded -> "3D object"
                        closed -> "Shape"
                        group.any { it.curveMid != null } -> if (group.size > 1) "Broken line with curve" else "Curve"
                        group.size > 1 -> "Broken line"
                        else -> "Line"
                    } + (material?.let { " · ${it.name}" } ?: "")
                    val outline = group.map { g -> guidePoints(g) }
                    val all = outline.flatten()
                    val pad = max(all.maxOf { it.x } - all.minOf { it.x }, all.maxOf { it.y } - all.minOf { it.y }) * 0.25f + 20f
                    FinderItem(plane, group.map { it.id }, name, kind, outline, floatArrayOf(all.minOf { it.x } - pad, all.minOf { it.y } - pad, all.maxOf { it.x } + pad, all.maxOf { it.y } + pad))
                }
            }
        }
        AlertDialog(onDismissRequest = { showFinder = false }, title = { Text("Find") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (items.isEmpty()) Text("Nothing drawn yet.")
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth().clickable {
                        // Open its view, select it and zoom to it.
                        showFinder = false; leave3d()
                        if (item.plane != editPlane) { switchPlane(item.plane); pendingFocus = item.bounds } else fitView(item.bounds)
                        tool = Tool.ADJUST; clearOwnSelection(); selectedForeign = null
                        selectedWholeGuideId = item.guideIds.first(); selectedGuideId = item.guideIds.first()
                        crossViewSelection = item.plane to item.guideIds.first()
                    }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Canvas(Modifier.size(44.dp).background(Color(0xFFF2F5F4), RoundedCornerShape(6.dp))) {
                            val b = item.bounds
                            val k = min((size.width - 8f) / (b[2] - b[0]).coerceAtLeast(1f), (size.height - 8f) / (b[3] - b[1]).coerceAtLeast(1f))
                            val ox = (size.width - (b[2] - b[0]) * k) / 2f; val oy = (size.height - (b[3] - b[1]) * k) / 2f
                            item.outline.forEach { pts -> pts.zipWithNext().forEach { (a, c) -> drawLine(Color(0xFF1A2421), Offset(ox + (a.x - b[0]) * k, oy + (a.y - b[1]) * k), Offset(ox + (c.x - b[0]) * k, oy + (c.y - b[1]) * k), strokeWidth = 2.5f) } }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${item.kind} · ${item.plane.label}", fontSize = 12.sp, color = Color(0xFF4D5A57))
                        }
                    }
                }
            }
        }, confirmButton = {
            Row {
                TextButton(onClick = { showFinder = false; open3dView() }) { Text("3D view") }
                TextButton(onClick = { showFinder = false; setPresentation(true) }) { Text("Full view") }
                TextButton(onClick = { showFinder = false }) { Text("Close") }
            }
        })
    }
    if (showMaterialList) {
        // Every line with a material, in every view: its true length (curves and depth included), grouped by material.
        val setup = modelSetup()
        val format = { mm: Double -> formatLength(mm, LengthUnit.of(unit), MeasurementPrecision.places[unit] ?: 2) }
        val pieces = allGuides().mapNotNull { g ->
            val material = materials.firstOrNull { it.id == g.materialId && !it.isSheet } ?: return@mapNotNull null
            val mapping = setup.mapping(g.plane) ?: return@mapNotNull null
            val pts = guidePoints(g); val n = (pts.size - 1).coerceAtLeast(1)
            val length = pts.withIndex().zipWithNext().sumOf { (a, b) ->
                (mapping.model(b.value, g.startZMm + (g.endZMm - g.startZMm) * b.index / n) - mapping.model(a.value, g.startZMm + (g.endZMm - g.startZMm) * a.index / n)).length
            }
            Triple(material, g.plane, length)
        }
        // Sheets are counted by area: every closed shape made of one, in every view.
        val sheets = ModelPlane.entries.flatMap { plane ->
            val mapping = setup.mapping(plane) ?: return@flatMap emptyList()
            closedGuideShapes(planeGeometry(plane).guides).mapNotNull { shape ->
                val material = shape.firstNotNullOfOrNull { g -> materials.firstOrNull { it.id == g.materialId && it.isSheet } } ?: return@mapNotNull null
                val pts = shapePoints(shape).map { Vec2(mapping.right(it), mapping.up(it)) }
                Triple(material, plane, abs(pts.indices.sumOf { i -> val a = pts[i]; val b = pts[(i + 1) % pts.size]; a.x * b.y - b.x * a.y }) / 2)
            }
        }
        AlertDialog(onDismissRequest = { showMaterialList = false }, title = { Text("Materials used") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pieces.isEmpty() && sheets.isEmpty()) Text("Nothing has a material yet. Select a line or shape, then Material.")
                sheets.groupBy { it.first.id }.values.forEach { group ->
                    val material = group.first().first
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfilePreview(material, Modifier.size(36.dp))
                        Column(Modifier.weight(1f)) { Text(material.name, fontWeight = FontWeight.Bold); Text(material.description, fontSize = 12.sp, color = Color(0xFF4D5A57)) }
                    }
                    val area = { mm2: Double -> "${java.text.DecimalFormat("0.###").format(mm2 / 1e6)} m²" }
                    group.sortedByDescending { it.third }.forEachIndexed { i, (_, plane, a) -> Text("  ${i + 1}.  ${area(a)}${if (plane != ModelPlane.FRONT) "  (${plane.label})" else ""}", fontSize = 14.sp) }
                    Text("  Total: ${group.size} piece${if (group.size == 1) "" else "s"}, ${area(group.sumOf { it.third })}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                pieces.groupBy { it.first.id }.values.forEach { group ->
                    val material = group.first().first
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ProfilePreview(material, Modifier.size(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(material.name, fontWeight = FontWeight.Bold)
                            Text(material.description, fontSize = 12.sp, color = Color(0xFF4D5A57))
                        }
                    }
                    group.sortedByDescending { it.third }.forEachIndexed { i, (_, plane, length) ->
                        Text("  ${i + 1}.  ${format(length)}${if (plane != ModelPlane.FRONT) "  (${plane.label})" else ""}", fontSize = 14.sp)
                    }
                    Text("  Total: ${group.size} piece${if (group.size == 1) "" else "s"}, ${format(group.sumOf { it.third })}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }, confirmButton = { TextButton(onClick = { showMaterialList = false }) { Text("Done") } })
    }
    saveMaterialShape?.let { shape ->
        /** The selected shape as a cross-section in millimeters, centered, with the closed shapes inside it as holes. */
        val profile = remember(shape) { runCatching {
            val mapping = modelSetup().mapping(editPlane) ?: return@runCatching null
            fun ring(path: List<GuideLine>) = editableContours(shapePath(path))?.singleOrNull()
            val outer = ring(shape) ?: return@runCatching null
            val holes = closedGuideShapes(guides).filter { it !== shape && it.none { g -> shape.any { s -> s.id == g.id } } }
                .mapNotNull(::ring).filter { hole -> hole.all { pointInsidePolygon(it, outer) } }
            fun mm(points: List<Pt>) = points.map { Vec2(mapping.right(it), mapping.up(it)) }
            val outline = mm(outer)
            val cx = (outline.minOf { it.x } + outline.maxOf { it.x }) / 2; val cy = (outline.minOf { it.y } + outline.maxOf { it.y }) / 2
            fun centered(points: List<Vec2>) = points.map { Vec2(it.x - cx, it.y - cy) }
            MemberProfile(ProfileKind.DRAWN, outline.maxOf { it.x } - outline.minOf { it.x }, outline.maxOf { it.y } - outline.minOf { it.y }, outline = centered(outline), holes = holes.map { centered(mm(it)) })
        }.getOrNull() }
        val f = java.text.DecimalFormat("0.#")
        var name by remember(shape) { mutableStateOf(profile?.let { "Shape ${f.format(it.widthMm)}×${f.format(it.heightMm)}" } ?: "") }
        // The real width of the cross-section; the drawing is scaled to it, keeping its proportions and holes.
        var realWidth by remember(shape) { mutableStateOf(profile?.let { f.format(it.widthMm) } ?: "") }
        val realWidthMm = parseLengthMm(realWidth, LengthUnit.MM)
        // Saved as a bar or pipe with this cross-section, or as a sheet (steel plate, plywood, glass) of some thickness.
        var asSheet by remember(shape) { mutableStateOf(false) }
        var sheetThickness by remember(shape) { mutableStateOf("3") }
        val sheetMm = parseLengthMm(sheetThickness, LengthUnit.MM)
        var finish by remember(shape) { mutableStateOf(Finish.METAL) }
        val scaled = profile?.let { p ->
            val k = (realWidthMm ?: p.widthMm) / p.widthMm
            fun scale(points: List<Vec2>) = points.map { Vec2(it.x * k, it.y * k) }
            p.copy(widthMm = p.widthMm * k, heightMm = p.heightMm * k, outline = scale(p.outline), holes = p.holes.map(::scale))
        }
        AlertDialog(onDismissRequest = { saveMaterialShape = null }, title = { Text("Save as material") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (scaled == null) Text("This shape can't be used as a cross-section. Close its outline and make sure it doesn't cross itself.")
                else {
                    val preview = Material("preview", name, scaled, guideColor(shape.first().colorIndex).toArgb().let { if (it == AndroidColor.BLACK) 0xFF5F6B6D.toInt() else it })
                    ProfilePreview(preview, Modifier.fillMaxWidth().height(110.dp))
                    Text("${preview.description}. Closed shapes inside it become holes. The material is shared by all projects: pick it in a line's details and the line becomes this shape along its length.", fontSize = 13.sp, color = Color(0xFF4D5A57))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = !asSheet, onClick = { asSheet = false }, label = { Text("Bar or pipe", fontSize = 13.sp) })
                        FilterChip(selected = asSheet, onClick = { asSheet = true; if (name.startsWith("Shape ")) name = "Steel sheet" }, label = { Text("Sheet", fontSize = 13.sp) })
                    }
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, placeholder = { Text(if (asSheet) "Steel sheet 3 mm" else "Tube 4*4") }, singleLine = true)
                    if (asSheet) OutlinedTextField(sheetThickness, { sheetThickness = it }, label = { Text("Thickness (mm)") }, supportingText = { Text("Shapes made of this sheet get this thickness.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    FinishPicker(finish) { finish = it }
                    if (!asSheet) OutlinedTextField(realWidth, { realWidth = it }, label = { Text("Real width (mm)") }, supportingText = { Text("The drawing is scaled to this width; height and holes follow.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
            }
        }, confirmButton = {
            TextButton(enabled = name.isNotBlank() && if (asSheet) sheetMm != null && sheetMm > 0 else scaled != null && realWidthMm != null && realWidthMm > 0, onClick = {
                val drawnColor = guideColor(shape.first().colorIndex).toArgb().let { if (it == AndroidColor.BLACK) 0xFF5F6B6D.toInt() else it }
                // Plain black drawings take a color that suits the look.
                val color = if (drawnColor != 0xFF5F6B6D.toInt()) drawnColor else when (finish) { Finish.WOOD -> 0xFFC89B6A.toInt(); Finish.GLASS -> 0xFF9CC9E0.toInt(); Finish.STONE -> 0xFF9E9A94.toInt(); Finish.METAL -> 0xFF8A9497.toInt(); Finish.PLAIN -> drawnColor }
                materials = materials + if (asSheet) Material(UUID.randomUUID().toString(), name.trim(), sheetProfile, color, sheetMm, finish) else Material(UUID.randomUUID().toString(), name.trim(), scaled!!, color, finish = finish)
                MaterialStore.save(context, materials)
                logToast(context, "Saved \"${name.trim()}\" to Materials", Toast.LENGTH_SHORT).show()
                saveMaterialShape = null
            }) { Text("Save") }
        }, dismissButton = { TextButton(onClick = { saveMaterialShape = null }) { Text("Cancel") } })
    }
    pendingCut?.let { op ->
        val tool = objectName(op.tool); val target = objectName(op.target)
        AlertDialog(onDismissRequest = { pendingCut = null }, title = { Text("Cut $target") }, text = {
            Text("$tool is cut out of $target in 3D. Keep $tool as an object too, or use it up? It stays editable in its view either way, and you can undo.")
        }, confirmButton = {
            Row {
                TextButton(onClick = { pushUndo(); solidOps = solidOps + op.copy(keepTool = true); pendingCut = null }) { Text("Keep it") }
                TextButton(onClick = { pushUndo(); solidOps = solidOps + op; pendingCut = null }) { Text("Use it up") }
            }
        }, dismissButton = { TextButton(onClick = { pendingCut = null }) { Text("Cancel") } })
    }
    if (movePointDialog) {
        val foreignPoint = selectedForeignPoint
        val target = foreignPoint?.let { selection -> foreign.firstOrNull { it.key == selection.key } }
        if (foreignPoint != null && target != null) {
            val across = listOf("X", "Y", "Z")[listOf(target.across.x, target.across.y, target.across.z).indexOfFirst { abs(it) > 0.5 }]
            val depth = target.plane.normalAxis.label
            MovePointDialog(unit, "$across +", if (foreignPoint.back) "$depth + (far side: changes thickness)" else "$depth +", onDismiss = { movePointDialog = false }) { a, b ->
                moveForeignPoint(foreignPoint, a, b); movePointDialog = false
            }
        } else {
            // The canvas axes of this plane, named by the model axis they show.
            val (labelA, vectorA) = if (editPlane == ModelPlane.PROFILE) "Z + (toward the front, left)" to Vec3(0.0, 0.0, 1.0) else "X + (right)" to Vec3(1.0, 0.0, 0.0)
            val (labelB, vectorB) = if (editPlane == ModelPlane.TOP) "Z + (toward the front, down)" to Vec3(0.0, 0.0, 1.0) else "Y + (up)" to Vec3(0.0, 1.0, 0.0)
            MovePointDialog(unit, labelA, labelB, onDismiss = { movePointDialog = false }) { a, b ->
                val ghost = selectedGhostPoint
                if (ghost != null) { pushUndo(); moveGhostPoint(ghost, vectorA * a + vectorB * b) }
                else modelSetup().mapping(editPlane)?.pixelDelta(vectorA * a + vectorB * b)?.let { nudgeSelected(it.x, it.y) }
                movePointDialog = false
            }
        }
    }
    foreignDialog?.let { target ->
        val geometry = planeGeometry(target.first)
        val extrusion = geometry.extrusions.firstOrNull { it.id == target.second }
        val name = foreign.firstOrNull { it.key == target }?.name ?: "Object"
        if (extrusion == null) foreignDialog = null
        else ObjectDepthDialog(name, target.first, facePositionMm(extrusion), extrusion.depthMm, unit, solidOpsOf(target), onCut = { foreignDialog = null; startSolidOp(false, target) }, onJoin = { foreignDialog = null; startSolidOp(true, target) }, onDismiss = { foreignDialog = null }, onRemove3d = {
            pushUndo()
            setPlaneShapes(target.first, geometry.guides, geometry.extrusions.filterNot { it.id == extrusion.id })
            foreignDialog = null; selectedForeign = null
        }) { faceMm, depthMm ->
            pushUndo()
            val resized = if (depthMm != null) extrusionWithDepth(extrusion, depthMm, modelSetup().scales[target.first], unit) else extrusion
            setPlaneShapes(target.first, geometry.guides, geometry.extrusions.map { if (it.id == extrusion.id) withFacePosition(resized, faceMm, resized.depthMm ?: 0.0) else it })
            foreignDialog = null
        }
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Done") } }, title = { Text("Model in 3D") }, text = {
        Text("Axes: X right, Y up, Z toward you in the front view. The drawing plane is Z = 0. Lengths are stored in millimeters; changing units only changes how they are shown.\n\n1. Import a photo or start an empty scene. Use the crop tool to rotate, level, crop, expand or correct perspective first.\n\n2. Calibrate: tap two points and enter their real distance. One reference sets X, Y and Z. Advanced: mark references as X only or Y only to compensate for a stretched photo. A photo cannot show depth, so enter Z values yourself.\n\n3. Draw lines, broken lines and curves; close a shape to make it a face.\n\n4. Make 3D: extrude a closed shape, then set its depth and Z position. In the 3D viewer, tap an object to edit them.\n\n5. Export: STL or 3MF in millimeters after the model passes validation. Check it in your slicer before printing.")
    })
}

@Composable private fun EmptyState(modifier: Modifier, onImport: () -> Unit) {
    Column(modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Model in 3D from a photo", fontSize = 28.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text("Import a photo or start an empty scene. Calibrate a known distance, draw closed shapes, extrude them and export STL or 3MF.", textAlign = TextAlign.Center, color = Color(0xFF505957))
        Spacer(Modifier.height(26.dp))
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(8.dp)) { Text("Import picture", fontWeight = FontWeight.Bold) }
    }
}

@Composable private fun LibraryScreen(modifier: Modifier, projects: List<SavedProject>, onImport: () -> Unit, onNewScene: () -> Unit, onImportProject: () -> Unit, onSettings: () -> Unit, onMaterials: () -> Unit, onRename: (SavedProject) -> Unit, onExportProject: (SavedProject) -> Unit, onConvert: (SavedProject, String) -> Unit, onOpen: (SavedProject) -> Unit, onDelete: (SavedProject) -> Unit) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Library", Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.Black)
            Button(onClick = onImport, shape = RoundedCornerShape(8.dp)) { Text("Import picture") }
        }
        Row { TextButton(onClick = onNewScene) { Text("New empty scene") }; TextButton(onClick = onImportProject) { Text("Import saved project") } }
        // Materials are shared by all projects, so they live here rather than inside one.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMaterials, shape = RoundedCornerShape(8.dp)) { Text("Materials") }
            TextButton(onClick = onSettings) { Text("Settings") }
        }
        if (projects.isEmpty()) EmptyState(Modifier.weight(1f), onImport)
        else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            projects.forEach { project ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(project) }, shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        val thumbnail = remember(context, project) { ProjectStore.thumbnail(context, project.id) }
                        if (thumbnail != null) Image(
                            bitmap = thumbnail.asImageBitmap(), contentDescription = null,
                            modifier = Modifier.size(68.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFE2ECE9)),
                            contentScale = ContentScale.Crop
                        ) else Box(Modifier.size(68.dp).background(Color(0xFFE2ECE9), RoundedCornerShape(6.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(project.name, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(formatCaptureTime(project.capturedAt), fontSize = 12.sp, color = Color(0xFF5A6562))
                            Text("${project.lines.count { !it.isReference }} measurements  |  ${project.unit}", fontSize = 12.sp, color = Color(0xFF5A6562))
                        }
                        var menuOpen by remember(project.id) { mutableStateOf(false) }
                        var unitMenuOpen by remember(project.id) { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(painterResource(R.drawable.ic_more), contentDescription = "Picture actions") }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename(project) })
                                DropdownMenuItem(text = { Text("Export project") }, onClick = { menuOpen = false; onExportProject(project) })
                                DropdownMenuItem(text = { Text("Measurement units") }, onClick = { menuOpen = false; unitMenuOpen = true })
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("Delete", color = Color(0xFFB3261E)) }, onClick = { menuOpen = false; onDelete(project) })
                            }
                            DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                                Text("Measurement units", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
                                listOf("mm" to "Millimeters", "cm" to "Centimeters", "m" to "Meters", "in" to "Inches", "ft" to "Feet").forEach { (unit, name) ->
                                    DropdownMenuItem(
                                        text = { Text("$name ($unit)") },
                                        onClick = { unitMenuOpen = false; onConvert(project, unit) },
                                        enabled = project.unit != unit
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(
    currentUnit: String,
    colorIndex: Int,
    unitVisibility: String,
    exportGuides: Boolean,
    hasProject: Boolean,
    onUnit: (String) -> Unit,
    onPrecision: (String, Int) -> Unit,
    onColor: (Int) -> Unit,
    onUnitVisibility: (String) -> Unit,
    onExportGuides: (Boolean) -> Unit,
    onExportLog: () -> Unit,
    onCopyDebugLog: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Default measurement unit", fontWeight = FontWeight.Bold)
                Text("Used when you import a new picture.", fontSize = 13.sp, color = Color(0xFF53615D))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("mm", "cm", "m", "in", "ft").forEach { value ->
                        FilterChip(selected = currentUnit == value, onClick = { onUnit(value) }, label = { Text(value) })
                    }
                }
            }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Decimal places", fontWeight = FontWeight.Bold)
                Text("Maximum displayed digits after the decimal point. Saved measurements keep their full precision.", fontSize = 13.sp, color = Color(0xFF53615D))
                listOf("mm", "cm", "m", "in", "ft").forEach { measureUnit ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(measureUnit, Modifier.width(35.dp))
                        (0..2).forEach { places ->
                            FilterChip(selected = MeasurementPrecision.places[measureUnit] == places, onClick = { onPrecision(measureUnit, places) }, label = { Text(places.toString()) })
                        }
                    }
                }
            }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Guide lines in exports", fontWeight = FontWeight.Bold)
                Text("Guide lines always stay saved with the picture. Choose whether thin black guides are included in exported images.", fontSize = 13.sp, color = Color(0xFF53615D))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = exportGuides, onCheckedChange = onExportGuides)
                    Spacer(Modifier.width(10.dp))
                    Text(if (exportGuides) "Include guide lines" else "Hide guide lines")
                }
            }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Unit labels on picture", fontWeight = FontWeight.Bold)
                Text("Choose where cm, mm, and other unit suffixes appear.", fontSize = 13.sp, color = Color(0xFF53615D))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("all" to "All", "calibration" to "Calibration", "none" to "None").forEach { (value, title) ->
                        FilterChip(selected = unitVisibility == value, onClick = { onUnitVisibility(value) }, label = { Text(title, fontSize = 12.sp) })
                    }
                }
            }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Default measurement color", fontWeight = FontWeight.Bold)
                Text("Used for new measured lines.", fontSize = 13.sp, color = Color(0xFF53615D))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    lineColors.indices.forEach { index ->
                        Box(
                            Modifier.size(42.dp)
                                .clip(CircleShape)
                                .background(lineColor(index))
                                .border(if (colorIndex == index) 4.dp else 1.dp, if (colorIndex == index) Color(0xFF17211E) else Color(0xFF8A9691), CircleShape)
                                .clickable { onColor(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (colorIndex == index) Text("✓", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current picture", fontWeight = FontWeight.Bold)
                Text("Export a plain text record of the measurements on the open picture.", fontSize = 13.sp, color = Color(0xFF53615D))
                OutlinedButton(onClick = onExportLog, enabled = hasProject, shape = RoundedCornerShape(8.dp)) { Text("Export measurement log") }
                // The last actions and messages, to paste into a message when something goes wrong.
                Button(onClick = onCopyDebugLog, shape = RoundedCornerShape(8.dp)) { Text("Copy debug log (last actions)") }
            }
        }
    }
}

@Composable private fun BoschConnectionSettings(unit: String) {
    val context = LocalContext.current
    val status = BoschGlmConnection.status
    val ready = BoschGlmConnection.ready
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    fun connect() = BoschGlmConnection.connect(context.applicationContext)
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (requiredPermissions.all { granted[it] == true || context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) connect()
        else BoschGlmConnection.permissionDenied()
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bosch GLM", fontWeight = FontWeight.Bold)
        Text("Keep the GLM connected while editing. Its next reading fills an open measurement automatically.", fontSize = 13.sp, color = Color(0xFF53615D))
        OutlinedButton(onClick = {
            if (requiredPermissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) connect()
            else bluetoothPermission.launch(requiredPermissions)
        }) { Text(if (ready) "Bosch GLM connected" else "Connect Bosch GLM") }
        Text(status, fontSize = 12.sp, color = Color(0xFF4D5A57))
    }
}

private fun imageDisplayName(context: Context, uri: Uri): String {
    var result = ""
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) result = cursor.getString(0).substringBeforeLast('.')
    }
    return result
}

/** Sets an object's front-face Z (positive toward the viewer) and, only when edited, its depth. */
/** Moves the selected point by exact distances along two named axes; empty fields mean no move. */
@Composable private fun MovePointDialog(unit: String, labelA: String, labelB: String, onDismiss: () -> Unit, onApply: (Double, Double) -> Unit) {
    val lengthUnit = LengthUnit.of(unit)
    var a by remember { mutableStateOf("") }
    var b by remember { mutableStateOf("") }
    val aMm = if (a.isBlank()) 0.0 else parseLengthMm(a, lengthUnit)
    val bMm = if (b.isBlank()) 0.0 else parseLengthMm(b, lengthUnit)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Point: move by") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(a, { a = it }, label = { Text("$labelA (${lengthUnit.symbol})") }, supportingText = { Text("Use a minus sign to move the other way.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            OutlinedTextField(b, { b = it }, label = { Text("$labelB (${lengthUnit.symbol})") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
        }
    }, confirmButton = { TextButton(enabled = aMm != null && bMm != null && (aMm != 0.0 || bMm != 0.0), onClick = { onApply(aMm!!, bMm!!) }) { Text("Move") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun ObjectDepthDialog(name: String, plane: ModelPlane, zMm: Double, depthMm: Double?, unit: String, operations: List<Pair<String, () -> Unit>> = emptyList(), onCut: (() -> Unit)? = null, onJoin: (() -> Unit)? = null, onDismiss: () -> Unit, onRemove3d: () -> Unit, onSave: (Double, Double?) -> Unit) {
    val (faceLabel, faceHint) = when (plane) {
        ModelPlane.FRONT -> "Front face Z" to "Positive Z comes toward you in the front view; 0 is the drawing plane."
        ModelPlane.TOP -> "Top face Y" to "Drawn in the Top view, so it is extruded down along Y. 0 is the bottom of the front drawing."
        ModelPlane.PROFILE -> "Right face X" to "Drawn in the Profile view, so it is extruded left along X. 0 is the left edge of the front drawing."
    }
    val axis = plane.normalAxis.label
    val lengthUnit = LengthUnit.of(unit)
    var z by remember { mutableStateOf(formatLength(zMm, lengthUnit, 2, withUnit = false)) }
    var depth by remember { mutableStateOf(depthMm?.let { formatLength(it, lengthUnit, 2, withUnit = false) } ?: "") }
    var depthEdited by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val zValue = parseLengthMm(z.ifBlank { "0" }, lengthUnit)
    val depthValue = if (depthEdited) parseLengthMm(depth, lengthUnit) else depthMm
    val valid = zValue != null && (!depthEdited || depthValue != null && depthValue > 0)
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, title = { Text("Remove 3D from $name?") }, text = { Text("The flat shape stays in the drawing. You can undo this.") },
        confirmButton = { TextButton(onClick = onRemove3d) { Text("Remove 3D") } }, dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } })
    else AlertDialog(onDismissRequest = onDismiss, title = { Text(name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(z, { z = it }, label = { Text("$faceLabel (${lengthUnit.symbol})") }, supportingText = { Text(faceHint) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            OutlinedTextField(depth, { depth = it; depthEdited = true }, label = { Text("Depth behind that face (${lengthUnit.symbol})") }, supportingText = { Text(if (depthEdited) "Thickness will change." else "Kept as is unless you change it.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            zValue?.let { front -> (depthValue)?.let { d -> Text("Occupies $axis ${formatLength(front - d, lengthUnit)} to ${formatLength(front, lengthUnit)}.", fontSize = 12.sp, color = Color(0xFF4D5A57)) } }
            // 3D cuts and joins: start one (then tap the other object), or undo an existing one.
            if (onCut != null || onJoin != null) Row {
                onCut?.let { TextButton(onClick = it) { Text("Cut with…") } }
                onJoin?.let { TextButton(onClick = it) { Text("Join with…") } }
            }
            operations.forEach { (text, remove) ->
                Row(verticalAlignment = Alignment.CenterVertically) { Text(text, fontSize = 13.sp, modifier = Modifier.weight(1f)); TextButton(onClick = remove) { Text("Undo") } }
            }
            TextButton(onClick = { confirmRemove = true }) { Text("Remove 3D (keep shape)") }
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = { onSave(zValue!!, if (depthEdited) depthValue else null) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** Picks the drawing plane. Every plane has the same tools; shapes drawn in Top extrude along Y, in Profile along X. */
/** The view from the other side of each plane: Back, Bottom, Left. */
private fun oppositeLabel(plane: ModelPlane) = when (plane) { ModelPlane.FRONT -> "Back"; ModelPlane.TOP -> "Bottom"; ModelPlane.PROFILE -> "Left" }

/**
 * Front, Top and Profile. A tap shows a view from the side last chosen for it; a long press offers both sides
 * (Front or Back, Top or Bottom, Profile (right) or Left).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun PlaneSwitcher(plane: ModelPlane, showing3d: Boolean, axes: String, mirrored: Set<ModelPlane>, onPlane: (ModelPlane, Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF2F5F4)).padding(horizontal = 8.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        ModelPlane.entries.forEach { candidate ->
            var menu by remember { mutableStateOf(false) }
            val selected = !showing3d && plane == candidate
            val flipped = candidate in mirrored
            Box {
                Text((if (flipped) oppositeLabel(candidate) else candidate.label) + " ▾", fontSize = 13.sp, color = Color(0xFF1B2B28), maxLines = 1,
                    modifier = Modifier.background(if (selected) Color(0xFFE8DEF8) else Color.Transparent, RoundedCornerShape(8.dp)).border(1.dp, if (selected) Color.Transparent else Color(0xFF79747E), RoundedCornerShape(8.dp))
                        .combinedClickable(onClick = { onPlane(candidate, flipped) }, onLongClick = { menu = true }).padding(horizontal = 12.dp, vertical = 6.dp))
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text(if (candidate == ModelPlane.PROFILE) "Profile (from the right)" else candidate.label) }, onClick = { menu = false; onPlane(candidate, false) })
                    DropdownMenuItem(text = { Text("${oppositeLabel(candidate)} (mirrored ${candidate.label.lowercase()})") }, onClick = { menu = false; onPlane(candidate, true) })
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(axes, fontSize = 11.sp, color = Color(0xFF4D5A57), maxLines = 1)
    }
}

@Composable private fun CalibrationBanner(calibration: Calibration) {
    val warning = calibration.status is CalibrationStatus.Contradictory || calibration.status is CalibrationStatus.Incomplete
    if (!warning && calibration.status !is CalibrationStatus.PerAxis) return
    Text(calibration.explanation(), Modifier.fillMaxWidth().background(if (warning) Color(0xFFFFE3E0) else Color(0xFFFFF4D6)).padding(horizontal = 14.dp, vertical = 6.dp), fontSize = 12.sp, color = if (warning) Color(0xFF8A1C12) else Color(0xFF5C4400))
}

private sealed class ModelReport {
    data object Checking : ModelReport()
    data class Problems(val issues: List<MeshIssue>) : ModelReport()
    data class Ready(val meshes: List<Mesh>) : ModelReport()
}

@Composable private fun ModelReportDialog(report: ModelReport, unit: String, onDismiss: () -> Unit, onStl: () -> Unit, on3mf: () -> Unit) {
    val lengthUnit = LengthUnit.of(unit)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(when (report) { ModelReport.Checking -> "Checking model"; is ModelReport.Problems -> "Can't export yet"; is ModelReport.Ready -> "Export 3D model" }) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (report) {
                ModelReport.Checking -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(12.dp)); Text("Building and validating meshes…") }
                is ModelReport.Problems -> {
                    Text("Fix these before exporting:")
                    report.issues.forEach { Text("• ${it.objectName} ${it.message}", fontSize = 14.sp) }
                }
                is ModelReport.Ready -> {
                    report.meshes.forEach { mesh ->
                        val (min, max) = mesh.bounds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(12.dp).background(Color(mesh.argb), RoundedCornerShape(3.dp))); Spacer(Modifier.width(8.dp))
                            Text("${mesh.name}: ${formatLength(max.x - min.x, lengthUnit)} X × ${formatLength(max.y - min.y, lengthUnit)} Y × ${formatLength(max.z - min.z, lengthUnit)} Z", fontSize = 14.sp)
                        }
                    }
                    Text("Checked: closed surfaces, outward normals, positive thickness and no overlapping objects. Files are written in millimeters. Only 3D objects are included, not the photo, guides or measurements.", fontSize = 12.sp, color = Color(0xFF4D5A57))
                    Text("STL stores geometry only. 3MF also keeps each object's color. Open the file in your slicer and check it before printing.", fontSize = 12.sp, color = Color(0xFF4D5A57))
                }
            }
        }
    }, confirmButton = {
        if (report is ModelReport.Ready) Row { TextButton(onClick = onStl) { Text("STL") }; TextButton(onClick = on3mf) { Text("3MF") } }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(if (report is ModelReport.Ready) "Cancel" else "Close") } })
}

@Composable private fun StatusStrip(tool: Tool, drawingEnd: Boolean = false, hint: String? = null, primaryAction: (@Composable () -> Unit)? = null, actions: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp)) {
        val status = when { drawingEnd -> "Tap the end point"; tool == Tool.NONE -> "Choose a tool"; tool == Tool.CROP -> "Adjust crop, then confirm"; tool == Tool.LINES -> "Tap points to draw a guide"; tool == Tool.ADJUST -> "Select a point, line or label"; tool == Tool.CALIBRATE -> "Tap two points to calibrate"; else -> "Tap two points to measure" }
        // Always two lines tall, so a changing hint never resizes (and re-fits) the canvas below.
        Text(hint ?: status, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 16.sp, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
        // The primary Select action stays visible; all secondary actions may scroll.
        Row(Modifier.fillMaxWidth().height(46.dp), verticalAlignment = Alignment.CenterVertically) {
            primaryAction?.invoke()
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}

/**
 * The last actions and messages, for "Copy debug log" in Settings: taps on buttons and tools, and every message
 * shown. Kept in memory, newest last.
 */
internal object ActionLog {
    private val entries = ArrayDeque<String>()
    private val time = java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US)
    private var file: File? = null
    /** Keeps the log in the app's storage, so it survives a restart or a crash (whose details are logged too). */
    @Synchronized fun start(context: Context) {
        if (file != null) return
        file = File(context.filesDir, "action-log.txt").also { f -> runCatching { f.readLines().takeLast(60).forEach { entries.addLast(it) } } }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error -> add("CRASH: ${error.stackTraceToString().lines().take(12).joinToString(" | ")}"); previous?.uncaughtException(thread, error) }
    }
    @Synchronized fun add(entry: String) {
        entries.addLast("${time.format(java.util.Date())} $entry"); while (entries.size > 60) entries.removeFirst()
        runCatching { file?.writeText(entries.joinToString("\n")) }
    }
    @Synchronized fun text(): String = entries.joinToString("\n")
}

/** A message on screen, also written to the debug log. */
private fun logToast(context: Context, text: CharSequence, duration: Int): Toast { ActionLog.add("message: $text"); return Toast.makeText(context, text, duration) }

/** One action in the top row: a small icon with its name under it. */
@Composable private fun ActionIcon(label: String, icon: Int, enabled: Boolean = true, on: Boolean = false, tint: Color = Color(0xFF173C39), onClick: () -> Unit) {
    Column(Modifier.width(54.dp).background(if (on) Color(0xFFCFE3DF) else Color.Transparent, RoundedCornerShape(8.dp)).clickable(enabled = enabled) { ActionLog.add("tap $label"); onClick() }.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(icon), contentDescription = label, tint = if (enabled) tint else Color(0xFFB4BEBA), modifier = Modifier.size(22.dp))
        Text(label, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (enabled) Color(0xFF173C39) else Color(0xFFB4BEBA))
    }
}

private fun pointName(index: Int): String {
    var value = index.coerceAtLeast(1)
    val name = StringBuilder()
    while (value > 0) {
        value--
        name.insert(0, ('A'.code + value % 26).toChar())
        value /= 26
    }
    return name.toString()
}

private fun measurementPointNames(lines: List<MeasureLine>): Map<Int, Pair<String, String>> {
    val result = mutableMapOf<Int, Pair<String, String>>()
    val processedPaths = mutableSetOf<Int>()
    var nextPoint = 1
    lines.filterNot { it.isReference }.forEach { line ->
        val path = line.pathId
        if (path == null) {
            result[line.id] = pointName(nextPoint++) to pointName(nextPoint++)
        } else if (processedPaths.add(path)) {
            var previous: MeasureLine? = null
            var previousEnd = ""
            lines.filter { !it.isReference && it.pathId == path }.sortedBy { it.segmentIndex }.forEach { segment ->
                val start = if (previous != null && distance(previous!!.end, segment.start) < 0.6) previousEnd else pointName(nextPoint++)
                val end = pointName(nextPoint++)
                result[segment.id] = start to end
                previous = segment; previousEnd = end
            }
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun MeasurementsScreen(lines: List<MeasureLine>, unit: String, sumEnabled: Boolean, onBack: () -> Unit, onEdit: (MeasureLine) -> Unit) {
    val measurements = lines.filterNot { it.isReference }
    val pointNames = remember(lines) { measurementPointNames(lines) }
    val references = lines.filter { it.isReference }
    val excluded = remember { mutableStateMapOf<Int, Boolean>() }
    val selected = measurements.filter { excluded[it.id] != true }
    val known = selected.filter { it.value > 0.0 && it.value.isFinite() }
    val missing = selected.size - known.size
    val colorGroups = known.groupBy { measurementColor(it).toArgb() }
    val selectState = when {
        selected.isEmpty() -> ToggleableState.Off
        selected.size == measurements.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    LaunchedEffect(measurements.map { it.id }) {
        val ids = measurements.map { it.id }.toSet()
        excluded.keys.toList().filter { it !in ids }.forEach { excluded.remove(it) }
    }
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = { TopAppBar(title = { Text("Measurements", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (measurements.isEmpty() && references.isEmpty()) Text("No measurements yet.", color = Color(0xFF53615D))
            if (measurements.isNotEmpty() && sumEnabled) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(state = selectState, onClick = {
                        if (selectState == ToggleableState.On) measurements.forEach { excluded[it.id] = true }
                        else excluded.clear()
                    })
                    Text("Select all", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Text("${selected.size} / ${measurements.size}", color = Color(0xFF53615D), fontSize = 13.sp)
                }
                if (colorGroups.size > 1) colorGroups.forEach { (color, group) ->
                    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).background(Color(color), CircleShape).border(1.dp, Color(0xFF64716C), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text("Color subtotal", Modifier.weight(1f))
                        Text("${formatMeasure(group.sumOf { it.value }, unit)} $unit", fontWeight = FontWeight.Medium)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (missing > 0) "Total (partial)" else "Total", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text("${formatMeasure(known.sumOf { it.value }, unit)} $unit", fontWeight = FontWeight.Bold)
                }
                if (missing > 0) Text("$missing selected segment${if (missing == 1) " has" else "s have"} no length", fontSize = 12.sp, color = Color(0xFF8C4900))
                HorizontalDivider()
            }
            val standalone = measurements.filter { it.pathId == null }
            if (standalone.isNotEmpty()) {
                Text("Measurements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                standalone.forEach { line ->
                    val pair = pointNames[line.id]!!.let { "${it.first} - ${it.second}" }
                    MeasurementDetailRow(line, unit, line.label.ifBlank { pair }, if (line.label.isBlank()) "" else pair, onEdit, excluded[line.id] != true, R.drawable.ic_guide_line) { checked -> if (checked) excluded.remove(line.id) else excluded[line.id] = true }
                }
            }
            measurements.filter { it.pathId != null }.groupBy { it.pathId }.forEach { (pathId, segments) ->
                HorizontalDivider(Modifier.padding(top = 8.dp))
                val included = segments.filter { excluded[it.id] != true }
                val knownSegments = included.filter { it.value > 0.0 && it.value.isFinite() }
                val incomplete = knownSegments.size != included.size
                val total = knownSegments.sumOf { it.value }
                val groupState = when {
                    included.isEmpty() -> ToggleableState.Off
                    included.size == segments.size -> ToggleableState.On
                    else -> ToggleableState.Indeterminate
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TriStateCheckbox(state = groupState, onClick = {
                        if (groupState == ToggleableState.On) segments.forEach { excluded[it.id] = true }
                        else segments.forEach { excluded.remove(it.id) }
                    })
                    Icon(painterResource(R.drawable.ic_broken_line), contentDescription = "Broken line", modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("#$pathId", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (sumEnabled) Text(if (included.isEmpty()) "None selected" else if (knownSegments.isEmpty()) "Not measured" else "${if (incomplete) "Partial: " else "Total: "}${formatMeasure(total, unit)} $unit", fontWeight = FontWeight.Bold)
                }
                segments.sortedBy { it.segmentIndex }.forEach { segment ->
                    val pair = pointNames[segment.id]!!.let { "${it.first} - ${it.second}" }
                    MeasurementDetailRow(segment, unit, segment.label.ifBlank { pair }, if (segment.label.isBlank()) "" else pair, onEdit, excluded[segment.id] != true) { checked -> if (checked) excluded.remove(segment.id) else excluded[segment.id] = true }
                }
            }
            if (references.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(top = 8.dp))
                Text("Calibration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                references.forEachIndexed { index, line ->
                    MeasurementDetailRow(line, unit, line.label.ifBlank { "Reference ${index + 1}" }, "A - B", onEdit)
                }
            }
        }
    }
}

@Composable private fun MeasurementDetailRow(line: MeasureLine, unit: String, title: String, points: String, onEdit: (MeasureLine) -> Unit, checked: Boolean? = null, icon: Int? = null, onCheckedChange: (Boolean) -> Unit = {}) {
    val description = when {
        line.isReference && !line.isScaleReference -> "Level only"
        !line.isReference && line.value <= 0.0 -> "Unknown"
        line.lengthSource == LengthSource.MEASURED -> "Measured"
        line.lengthSource == LengthSource.MANUAL -> "Manual"
        else -> "Calculated"
    }
    val statusColor = when (description) { "Measured" -> Color(0xFF1565C0); "Manual" -> Color(0xFF6A1B9A); "Calculated" -> Color(0xFF9A4C00); else -> Color(0xFF53615D) }
    val length = if (line.isReference && !line.isScaleReference || !line.isReference && line.value <= 0.0) "—" else "${formatMeasure(line.value, unit)}${if (line.hasDepth()) "*" else ""} $unit"
    Row(Modifier.fillMaxWidth().clickable { onEdit(line) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (checked != null) Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        if (icon != null) { Icon(painterResource(icon), contentDescription = "Straight line", modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)) }
        Box(Modifier.size(14.dp).background(measurementColor(line), CircleShape).border(1.dp, Color(0xFF64716C), CircleShape))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (points.isNotBlank()) Text("$points  ·  ", fontSize = 12.sp, color = Color(0xFF53615D))
                Text(description, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = statusColor)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(length, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = Color(0xFFE1E6E3))
}

@Composable private fun LevelDragger(rotation: Float, onRotation: (Float) -> Unit, onAuto: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Level", color = Color(0xFFFFC400), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        TextButton(onClick = onAuto, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Auto", color = Color(0xFF8A6200), fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        Slider(value = rotation, onValueChange = onRotation, valueRange = -30f..30f, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        Text("${format(rotation.toDouble())}°", fontSize = 12.sp, color = Color(0xFF53615D))
    }
}

@Composable private fun BottomControls(tool: Tool, lineDrawing: LineDrawing, measureDrawing: LineDrawing, drawingBroken: Boolean, guideColorIndex: Int, guideStrokeWidth: Float, unknownSize: Boolean, onUnknownSize: (Boolean) -> Unit, selectActive: Boolean, onSelect: () -> Unit, onFind: () -> Unit, onTool: (Tool) -> Unit, onFinishBroken: () -> Unit, onLineDrawing: (LineDrawing) -> Unit, onMeasureDrawing: (LineDrawing) -> Unit, onGuideStyle: (Int, Float) -> Unit) {
    var lineMenuOpen by remember { mutableStateOf(false) }
    var measureMenuOpen by remember { mutableStateOf(false) }
    Surface(shadowElevation = 8.dp) { Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            ToolIconButton("Calibrate", R.drawable.ic_calibrate_level, tool == Tool.CALIBRATE, { onTool(if (tool == Tool.CALIBRATE) Tool.NONE else Tool.CALIBRATE) }, Modifier.weight(1f))
            Box(Modifier.weight(1f)) {
                ToolIconButton("Measure", R.drawable.ic_measure_straight, tool == Tool.MEASURE && measureDrawing == LineDrawing.STRAIGHT, { val selected = tool == Tool.MEASURE && measureDrawing == LineDrawing.STRAIGHT; onMeasureDrawing(LineDrawing.STRAIGHT); onTool(if (selected) Tool.NONE else Tool.MEASURE) }, Modifier.fillMaxWidth(), description = "Straight measurement", onLongPress = { measureMenuOpen = true })
                DropdownMenu(expanded = measureMenuOpen, onDismissRequest = { measureMenuOpen = false }) {
                    DropdownMenuItem(text = { Text(if (measureDrawing == LineDrawing.STRAIGHT) "Straight measurement  ✓" else "Straight measurement") }, onClick = { measureMenuOpen = false; onMeasureDrawing(LineDrawing.STRAIGHT); onTool(Tool.MEASURE) })
                    DropdownMenuItem(text = { Text(if (measureDrawing == LineDrawing.BROKEN) "Broken measurement  ✓" else "Broken measurement") }, onClick = { measureMenuOpen = false; onMeasureDrawing(LineDrawing.BROKEN); onTool(Tool.MEASURE) })
                    DropdownMenuItem(text = { Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = unknownSize, onCheckedChange = null); Text("Size unknown") } }, onClick = { onUnknownSize(!unknownSize) })
                }
            }
            if (tool == Tool.MEASURE) ToolIconButton("Broken", R.drawable.ic_measure_broken, measureDrawing == LineDrawing.BROKEN, { val selected = measureDrawing == LineDrawing.BROKEN; onMeasureDrawing(LineDrawing.BROKEN); onTool(if (selected) Tool.NONE else Tool.MEASURE) }, Modifier.weight(1f), description = "Broken measurement")
            Box(Modifier.weight(1f)) {
                // The button shows the line type in use: a straight line, a curve or a circle. Hold it to choose.
                val (lineLabel, lineIcon) = when (lineDrawing) { LineDrawing.CURVE -> "Curve ▾" to R.drawable.ic_curve; LineDrawing.CIRCLE -> "Circle ▾" to R.drawable.ic_circle; LineDrawing.RECTANGLE -> "Rect ▾" to R.drawable.ic_rect; else -> "Lines ▾" to R.drawable.ic_guide_line }
                ToolIconButton(lineLabel, lineIcon, tool == Tool.LINES && lineDrawing != LineDrawing.BROKEN, { val selected = tool == Tool.LINES && lineDrawing != LineDrawing.BROKEN; if (lineDrawing == LineDrawing.BROKEN) onLineDrawing(LineDrawing.STRAIGHT); onTool(if (selected) Tool.NONE else Tool.LINES) }, Modifier.fillMaxWidth(), description = "Guide line", onLongPress = { lineMenuOpen = true })
                DropdownMenu(expanded = lineMenuOpen, onDismissRequest = { lineMenuOpen = false }) {
                    Text("Line type", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.STRAIGHT) "Straight guide  ✓" else "Straight guide") }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.STRAIGHT); onTool(Tool.LINES) })
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.BROKEN) "Broken guide  ✓" else "Broken guide") }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.BROKEN); onTool(Tool.LINES) })
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.CURVE) "Curve guide  ✓" else "Curve guide") }, leadingIcon = { Icon(painterResource(R.drawable.ic_curve), null, Modifier.size(20.dp)) }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.CURVE); onTool(Tool.LINES) })
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.RECTANGLE) "Rectangle  ✓" else "Rectangle") }, leadingIcon = { Icon(painterResource(R.drawable.ic_rect), null, Modifier.size(20.dp)) }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.RECTANGLE); onTool(Tool.LINES) })
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.CIRCLE) "Circle  ✓" else "Circle") }, leadingIcon = { Icon(painterResource(R.drawable.ic_circle), null, Modifier.size(20.dp)) }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.CIRCLE); onTool(Tool.LINES) })
                    HorizontalDivider()
                    Text("Color", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        guideColors.indices.forEach { index ->
                            Box(Modifier.size(24.dp).background(guideColor(index), CircleShape).border(if (guideColorIndex == index) 3.dp else 1.dp, if (guideColorIndex == index) Color(0xFF006E67) else Color.LightGray, CircleShape).clickable { onGuideStyle(index, guideStrokeWidth) })
                        }
                    }
                    Text("Thickness", Modifier.padding(horizontal = 16.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(3f to "Thin", 5f to "Medium", 8f to "Thick").forEach { (width, label) ->
                            FilterChip(selected = guideStrokeWidth == width, onClick = { onGuideStyle(guideColorIndex, width) }, label = { Text(label, fontSize = 11.sp) })
                        }
                    }
                }
            }
            if (tool == Tool.LINES) ToolIconButton("Broken", R.drawable.ic_broken_line, lineDrawing == LineDrawing.BROKEN, { val selected = lineDrawing == LineDrawing.BROKEN; onLineDrawing(LineDrawing.BROKEN); onTool(if (selected) Tool.NONE else Tool.LINES) }, Modifier.weight(1f), description = "Broken guide line", onLongPress = { lineMenuOpen = true })
            // Picking several things, and finding anything, sit with the tools so they never move.
            ToolIconButton(if (selectActive) "Done" else "Select", R.drawable.ic_select, selectActive, onSelect, Modifier.weight(1f))
            ToolIconButton("Find", R.drawable.ic_finder, false, onFind, Modifier.weight(1f))
            if (drawingBroken) ToolButton("Finish", true, onFinishBroken, Modifier.weight(1f))
            else ModifyButton(tool == Tool.ADJUST, { onTool(if (tool == Tool.ADJUST) Tool.NONE else Tool.ADJUST) }, Modifier.weight(1f))
    } }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun ToolIconButton(label: String, icon: Int, selected: Boolean, action: () -> Unit, modifier: Modifier, enabled: Boolean = true, description: String = label, onLongPress: (() -> Unit)? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(42.dp)
                .background(if (selected) Color(0xFFE2ECE9) else Color.Transparent, CircleShape)
                .combinedClickable(enabled = enabled, onClick = { ActionLog.add("tool $label"); action() }, onLongClick = onLongPress?.let { f -> { ActionLog.add("hold $label"); f() } }),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(icon), contentDescription = description, tint = Color.Unspecified, modifier = Modifier.size(26.dp)) }
        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (enabled) Color(0xFF173C39) else Color(0xFF8A9691))
    }
}
/** Modify (the editing tool): an icon whose border blinks orange while it is on. */
/** The selected thing's name in the title bar: red, in a slowly blinking red border; a tap opens its properties. */
@Composable private fun SelectionTitle(name: String, onClick: () -> Unit) {
    var bright by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(900); bright = !bright } }
    OutlinedButton(onClick = { ActionLog.add("open $name"); onClick() }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (bright) Color(0xFFD32F2F) else Color(0xFFF2B8B5))) {
        Text(name, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun ModifyButton(on: Boolean, onClick: () -> Unit, modifier: Modifier) {
    var blink by remember { mutableStateOf(true) }
    LaunchedEffect(on) { blink = true; while (on) { delay(500); blink = !blink } }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(42.dp).background(if (on) Color(0xFFFFF0E0) else Color.Transparent, CircleShape)
            .border(if (on) 3.dp else 0.dp, if (on && blink) Color(0xFFFF8F00) else if (on) Color(0xFFFFD8A8) else Color.Transparent, CircleShape)
            .clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_modify), contentDescription = "Modify", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
        }
        Text("Modify", fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium, color = Color(0xFF173C39))
    }
}
@Composable private fun ToolButton(text: String, selected: Boolean, action: () -> Unit, modifier: Modifier, enabled: Boolean = true) {
    if (selected) Button(onClick = action, modifier = modifier, enabled = enabled, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(text, fontSize = 12.sp, maxLines = 1) }
    else OutlinedButton(onClick = action, modifier = modifier, enabled = enabled, contentPadding = PaddingValues(horizontal = 2.dp)) { Text(text, fontSize = 12.sp, maxLines = 1) }
}

private data class Endpoint(val lineId: Int, val isStart: Boolean)
private data class GuideEndpoint(val guideId: Int, val isStart: Boolean)

private fun vibrateSelection(context: Context): Boolean = runCatching {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (!vibrator.hasVibrator()) return@runCatching false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
    else { @Suppress("DEPRECATION") vibrator.vibrate(45) }
    true
}.getOrDefault(false)

/**
 * Triangles (x, y triples in canvas pixels) as one path in screen space, all wound the same way so the filled
 * union has no holes where opposite faces of a solid overlap.
 */
private fun trianglePath(triangles: FloatArray, left: Float, top: Float, fit: Float) = androidx.compose.ui.graphics.Path().apply {
    var i = 0
    while (i + 5 < triangles.size) {
        val ax = left + triangles[i] * fit; val ay = top + triangles[i + 1] * fit
        var bx = left + triangles[i + 2] * fit; var by = top + triangles[i + 3] * fit
        var cx = left + triangles[i + 4] * fit; var cy = top + triangles[i + 5] * fit
        val area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax)
        if (abs(area) > 0.01f) {
            if (area < 0) { val x = bx; val y = by; bx = cx; by = cy; cx = x; cy = y }
            moveTo(ax, ay); lineTo(bx, by); lineTo(cx, cy); close()
        }
        i += 6
    }
}

@Composable private fun ImageEditor(modifier: Modifier, bitmap: Bitmap, lines: List<MeasureLine>, guides: List<GuideLine>, ghosts: List<Pair<Int, FloatArray>> = emptyList(), showSizes: Boolean = true, minZoom: Float = 0.1f, growArrows: List<Pair<Pt, Pt>> = emptyList(), rimHandles: List<Pt> = emptyList(), growOn: Boolean = false, dragToDraw: Boolean = false, draftShape: LineDrawing? = null, mirrorX: Boolean = false, mirrorY: Boolean = false, sections: List<Pair<Int, List<List<Pt>>>> = emptyList(), models: List<Triple<Int, Int, List<Pt>>> = emptyList(), selectedModelId: Int? = null, materialNames: Map<String, String> = emptyMap(), ghostLines: List<Pair<Pair<ModelPlane, Int>, FloatArray>> = emptyList(), ghostNoDots: Set<Pair<ModelPlane, Int>> = emptySet(), highlightGhost: Pair<ModelPlane, Int>? = null, ghostPointMark: Pt? = null, pickedOutlines: List<Pair<List<Pt>, Boolean>> = emptyList(), itemRect: Pair<Pt, Pt>? = null, ghostPointMarks: List<Pt> = emptyList(), foreignOverlay: ForeignOverlay? = null, joinPick: JoinPick? = null, blankCanvas: Boolean = false, onViewSize: (Float, Float) -> Unit = { _, _ -> }, selectedGuideId: Int?, unit: String, drawingScale: Double?, draftStart: Pt?, pendingEnd: Pt?, tool: Tool, selectedEndpoint: Endpoint?, magnifierPoint: Pt?, deleteCandidateId: Int?, alignmentLineIds: Set<Int>, alignmentFlashToken: Int, edgeSnapPoint: Pt?, edgeFlashToken: Int, adjustmentIndicatorPoint: Pt?, adjustmentLineId: Int?, adjustmentGuideId: Int?, adjustmentFlashToken: Int, guideColorIndex: Int, guideStrokeWidth: Float, rotation: Float, unitVisibility: String, zoom: Float, pan: Offset, cropFrame: CropFrame?, onTransform: (Float, Offset) -> Unit, onDoubleTap: () -> Unit, doubleTapEnabled: Boolean, onJunction: (JunctionAction) -> Unit, onAddPoint: (Pt, Double) -> Unit, onTap: (Pt, Double) -> Unit, onLongPress: (Pt, Double) -> Unit, onDragStart: (Pt, Double) -> Unit, onDrag: (Pt, Boolean) -> Unit, onDragEnd: () -> Unit, onDragCancel: () -> Unit, onCropStart: (Pt, Double) -> Unit, onCropDrag: (Pt) -> Unit, onCropEnd: () -> Unit, selectedCurveId: Int?, selectedLabelId: Int?, selectedWholeLineId: Int?, selectedGuideEndpoint: GuideEndpoint?, selectedWholeGuideId: Int?, selectedVertices: List<Pt>, selectionStart: Pt?, selectionEnd: Pt?, panOnEmptyDrag: Boolean, showFineControls: Boolean, onNudge: (Float, Float) -> Unit, pictureOptions: PictureOptions, extrusions: List<Extrusion>, selectedExtrusionId: Int?, onTripleTap: (Pt, Double) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    fun selectionFeedback() { if (!vibrateSelection(context)) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    val holdScope = rememberCoroutineScope()
    // A blank canvas has no edge: it is white everywhere and points can go anywhere on it.
    BoxWithConstraints(modifier.background(if (blankCanvas) Color.White else Color(0xFF18201E)).clipToBounds()) {
        val viewW = constraints.maxWidth.toFloat(); val viewH = constraints.maxHeight.toFloat()
        LaunchedEffect(viewW, viewH, bitmap.width, bitmap.height) { onViewSize(viewW, viewH) }
        val blink by rememberInfiniteTransition(label = "blink").animateFloat(0.2f, 1f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "blink")
        val snapRadiusOnScreen = with(LocalDensity.current) { 15.dp.toPx() }
        val junctionRadiusOnScreen = with(LocalDensity.current) { 32.dp.toPx() }
        val baseFit = min(viewW / bitmap.width, viewH / bitmap.height)
        val fit = baseFit * zoom
        // Line widths and marks grow with zoom only up to a point, so zooming far in never makes them huge.
        val strokeZoom = zoom.coerceAtMost(2.5f)
        val imageW = bitmap.width * fit; val imageH = bitmap.height * fit
        val left = (viewW - imageW) / 2f + pan.x; val top = (viewH - imageH) / 2f + pan.y
        val pivot = Offset(viewW / 2f, viewH / 2f)
        fun rotateView(point: Offset, degrees: Float): Offset {
            val radians = Math.toRadians(degrees.toDouble())
            val x = point.x - pivot.x; val y = point.y - pivot.y
            return Offset(pivot.x + (x * cos(radians) - y * sin(radians)).toFloat(), pivot.y + (x * sin(radians) + y * cos(radians)).toFloat())
        }
        fun sourceAt(view: Offset, useZoom: Float = zoom, usePan: Offset = pan): Pt {
            val unrotatedView = rotateView(view, -rotation)
            val useFit = baseFit * useZoom
            val useLeft = (viewW - bitmap.width * useFit) / 2f + usePan.x
            val useTop = (viewH - bitmap.height * useFit) / 2f + usePan.y
            val shown = Pt((unrotatedView.x - useLeft) / useFit, (unrotatedView.y - useTop) / useFit)
            val x = if (mirrorX) bitmap.width - shown.x else shown.x; val y = if (mirrorY) bitmap.height - shown.y else shown.y
            return if (blankCanvas) Pt(x, y) else Pt(x.coerceIn(0f, bitmap.width.toFloat()), y.coerceIn(0f, bitmap.height.toFloat()))
        }
        // Back, Bottom and Left views show their plane mirrored: an image point and its mirror swap places on screen.
        fun mirrored(p: Pt) = Pt(if (mirrorX) bitmap.width - p.x else p.x, if (mirrorY) bitmap.height - p.y else p.y)
        fun viewAt(p: Pt): Offset = mirrored(p).let { Offset(left + it.x * fit, top + it.y * fit) }
        /** (x, y) pairs mirrored like [mirrored]. */
        fun mirroredPairs(values: FloatArray): FloatArray = if (!mirrorX && !mirrorY) values else FloatArray(values.size) { i -> if (i % 2 == 0) (if (mirrorX) bitmap.width - values[i] else values[i]) else (if (mirrorY) bitmap.height - values[i] else values[i]) }
        val latestZoom = rememberUpdatedState(zoom)
        val latestMinZoom = rememberUpdatedState(minZoom)
        val latestPan = rememberUpdatedState(pan)
        val latestDragStart = rememberUpdatedState(onDragStart)
        val latestDragToDraw = rememberUpdatedState(dragToDraw)
        // Grow arrows blink orange.
        var growBlink by remember { mutableStateOf(true) }
        val anyGrow = growArrows.isNotEmpty() || growOn && foreignOverlay != null
        LaunchedEffect(anyGrow) { growBlink = true; while (anyGrow) { delay(400); growBlink = !growBlink } }
        /** A canvas direction as it points on screen (mirrored views flip it). */
        fun screenDir(d: Pt) = Pt(if (mirrorX) -d.x else d.x, if (mirrorY) -d.y else d.y)
        /** A grow arrow from [anchor] along [dir] (screen), with a round grip at its tip. */
        fun DrawScope.growArrow(anchor: Offset, dir: Pt) {
            val color = if (growBlink) Color(0xFFFF8F00) else Color(0xFFFFCC80)
            val tip = Offset(anchor.x + dir.x * 61.6f, anchor.y + dir.y * 61.6f)
            drawLine(Color.White, anchor, tip, strokeWidth = 9f); drawLine(color, anchor, tip, strokeWidth = 5f)
            drawCircle(Color.White, 30f, tip); drawCircle(color, 30f, tip, style = Stroke(6f))
            // Arrowhead inside the grip, pointing out.
            val px = -dir.y; val py = dir.x
            val head = Offset(tip.x + dir.x * 14f, tip.y + dir.y * 14f)
            val back = Offset(tip.x - dir.x * 10f, tip.y - dir.y * 10f)
            drawLine(color, back, head, strokeWidth = 5f)
            drawLine(color, head, Offset(head.x - dir.x * 11f + px * 10f, head.y - dir.y * 11f + py * 10f), strokeWidth = 5f)
            drawLine(color, head, Offset(head.x - dir.x * 11f - px * 10f, head.y - dir.y * 11f - py * 10f), strokeWidth = 5f)
        }
        // Blinks the rectangle preview while it is a square.
        var squareBlink by remember { mutableStateOf(true) }
        val drawingSquare = draftShape == LineDrawing.RECTANGLE && draftStart != null && pendingEnd != null && squareEnd(draftStart, pendingEnd).second
        LaunchedEffect(drawingSquare) { squareBlink = true; while (drawingSquare) { delay(220); squareBlink = !squareBlink } }
        val latestDrag = rememberUpdatedState(onDrag)
        val latestDragEnd = rememberUpdatedState(onDragEnd)
        val latestDragCancel = rememberUpdatedState(onDragCancel)
        val latestLines = rememberUpdatedState(lines)
        val latestGuides = rememberUpdatedState(guides)
        val latestDraftStart = rememberUpdatedState(draftStart)
        val latestSelectedGuideId = rememberUpdatedState(selectedGuideId)
        val latestPanOnEmptyDrag = rememberUpdatedState(panOnEmptyDrag)
        val latestTap = rememberUpdatedState(onTap)
        val latestLongPress = rememberUpdatedState(onLongPress)
        val latestTripleTap = rememberUpdatedState(onTripleTap)
        val latestJunction = rememberUpdatedState(onJunction)
        val latestAddPoint = rememberUpdatedState(onAddPoint)
        var heldLineId by remember { mutableStateOf<Int?>(null) }
        var vanishingBlink by remember { mutableStateOf(true) }
        var curveBlink by remember { mutableStateOf(true) }
        LaunchedEffect(selectedGuideId, tool) {
            curveBlink = true
            while (selectedGuideId != null && (tool == Tool.LINES || tool == Tool.ADJUST)) { delay(450); curveBlink = !curveBlink }
        }
        LaunchedEffect(selectedExtrusionId, tool) {
            vanishingBlink = true
            while (selectedExtrusionId != null && tool == Tool.ADJUST) { delay(450); vanishingBlink = !vanishingBlink }
        }
        var junctionIndicator by remember { mutableStateOf<Pt?>(null) }
        var junctionIndicatorVisible by remember { mutableStateOf(false) }
        var heldGuideId by remember { mutableStateOf<Int?>(null) }
        var heldLineBlink by remember { mutableStateOf(false) }
        LaunchedEffect(heldLineId, heldGuideId) {
            heldLineBlink = false
            while (heldLineId != null || heldGuideId != null) {
                heldLineBlink = true; delay(240)
                heldLineBlink = false; delay(200)
            }
        }
        var selectionBlinkOn by remember(selectedEndpoint, selectedLabelId, selectedWholeLineId, selectedGuideEndpoint, selectedWholeGuideId) { mutableStateOf(false) }
        LaunchedEffect(selectedEndpoint, selectedLabelId, selectedWholeLineId, selectedGuideEndpoint, selectedWholeGuideId) {
            if (selectedEndpoint != null || selectedLabelId != null || selectedWholeLineId != null || selectedGuideEndpoint != null || selectedWholeGuideId != null) repeat(3) {
                selectionBlinkOn = true
                delay(420)
                selectionBlinkOn = false
                delay(260)
            }
        }
        var deleteBlinkOn by remember(deleteCandidateId) { mutableStateOf(false) }
        LaunchedEffect(deleteCandidateId) {
            while (deleteCandidateId != null) {
                deleteBlinkOn = true
                delay(380)
                deleteBlinkOn = false
                delay(220)
            }
        }
        var alignmentBlinkOn by remember { mutableStateOf(false) }
        LaunchedEffect(alignmentFlashToken) {
            if (alignmentFlashToken > 0) repeat(5) {
                alignmentBlinkOn = true
                delay(120)
                alignmentBlinkOn = false
                delay(120)
            }
        }
        var edgeBlinkOn by remember { mutableStateOf(false) }
        LaunchedEffect(edgeFlashToken) {
            if (edgeFlashToken > 0) repeat(5) {
                edgeBlinkOn = true
                delay(160)
                edgeBlinkOn = false
                delay(160)
            }
        }
        var adjustmentBlinkOn by remember { mutableStateOf(false) }
        val hasUnknown = lines.any { !it.isReference && it.value <= 0.0 }
        var unknownBlinkOn by remember { mutableStateOf(false) }
        LaunchedEffect(hasUnknown) {
            unknownBlinkOn = false
            while (hasUnknown) { unknownBlinkOn = true; delay(450); unknownBlinkOn = false; delay(450) }
        }
        LaunchedEffect(adjustmentFlashToken) {
            if (adjustmentFlashToken > 0) repeat(3) {
                adjustmentBlinkOn = true
                delay(140)
                adjustmentBlinkOn = false
                delay(140)
            }
        }
        val touch = Modifier.pointerInput(tool, bitmap, viewW, viewH, rotation, doubleTapEnabled) {
            var previousTapAt = 0L
            var previousTapPoint = Offset.Zero
            /** In Lines and Measure, a drag that starts on an existing end point moves it, as in Adjust. */
            fun nearEndpoint(p: Pt, zoomNow: Float, guidesNow: List<GuideLine>, linesNow: List<MeasureLine>): Boolean {
                val r = 42.0 / (baseFit * zoomNow)
                return when (tool) {
                    Tool.LINES -> guidesNow.any { distance(it.start, p) < r || distance(it.end, p) < r }
                    Tool.MEASURE -> linesNow.any { distance(it.start, p) < r || distance(it.end, p) < r }
                    else -> false
                }
            }
            var tapCount = 0
            var pendingTap: kotlinx.coroutines.Job? = null
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val gestureLines = latestLines.value
                val gestureGuides = latestGuides.value
                val noDraft = latestDraftStart.value == null
                val downAt = System.currentTimeMillis()
                var localZoom = latestZoom.value
                var localPan = latestPan.value
                var lastCentroid = down.position
                var lastDistance = 0f
                var moved = false
                var transformed = false
                var adjusting = false
                val holdJob = if (tool != Tool.ADJUST && tool != Tool.CROP && latestDraftStart.value == null) holdScope.launch {
                    delay(450)
                    val point = sourceAt(down.position, localZoom, localPan)
                    val radius = 28.0 / (baseFit * localZoom)
                    if (tool == Tool.LINES) {
                        closestGuide(point, gestureGuides, radius * 1.7)?.let { heldGuideId = it.id; selectionFeedback() }
                    } else {
                        val line = closestLine(point, gestureLines)?.takeIf { distanceToLine(point, it) <= radius * 1.7 }
                        if (line != null) { heldLineId = line.id; selectionFeedback() }
                        else if (tool == Tool.NONE) closestGuide(point, gestureGuides, radius * 1.7)?.let { heldGuideId = it.id; selectionFeedback() }
                    }
                } else null
                do {
                    val event = awaitPointerEvent()
                    val pressed = event.changes.filter { it.pressed }
                    if (pressed.size >= 2) {
                        holdJob?.cancel(); heldLineId = null; heldGuideId = null
                        if (adjusting && (tool == Tool.ADJUST || tool == Tool.LINES)) { latestDragCancel.value(); adjusting = false }
                        transformed = true
                        val centroid = Offset(pressed.map { it.position.x }.average().toFloat(), pressed.map { it.position.y }.average().toFloat())
                        val distance = pressed.map { hypot((it.position.x - centroid.x).toDouble(), (it.position.y - centroid.y).toDouble()).toFloat() }.average().toFloat()
                        if (lastDistance > 0f) {
                            // In as far as you like; out only until everything fits.
                            val nextZoom = (localZoom * (distance / lastDistance)).coerceIn(latestMinZoom.value, 400f)
                            val focus = sourceAt(lastCentroid, localZoom, localPan).let { Pt(if (mirrorX) bitmap.width - it.x else it.x, if (mirrorY) bitmap.height - it.y else it.y) }
                            val nextFit = baseFit * nextZoom
                            val unrotatedCentroid = rotateView(centroid, -rotation)
                            localPan = Offset(unrotatedCentroid.x - focus.x * nextFit - (viewW - bitmap.width * nextFit) / 2f, unrotatedCentroid.y - focus.y * nextFit - (viewH - bitmap.height * nextFit) / 2f)
                            localZoom = nextZoom
                            onTransform(localZoom, localPan)
                            moved = true; transformed = true
                        }
                        lastCentroid = centroid; lastDistance = distance
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    } else if (pressed.size == 1) {
                        val change = pressed.first()
                        val delta = change.position - change.previousPosition
                        // A slow drag on an end point in Lines or Measure moves only a few pixels per event, so it needs the low threshold too.
                val endpointGrab = (tool == Tool.LINES || tool == Tool.MEASURE) && noDraft && nearEndpoint(sourceAt(down.position, localZoom, localPan), localZoom, gestureGuides, gestureLines)
                val dragThreshold = if (adjusting && (tool == Tool.ADJUST || endpointGrab)) 0.0 else if (tool == Tool.ADJUST || tool == Tool.CROP || endpointGrab) 1.0 else 8.0
                        if (hypot(delta.x.toDouble(), delta.y.toDouble()) > dragThreshold) {
                            holdJob?.cancel(); heldLineId = null; heldGuideId = null
                            if (tool == Tool.ADJUST && !transformed) {
                                if (latestPanOnEmptyDrag.value) {
                                    localPan += delta; onTransform(localZoom, localPan)
                                } else {
                                    if (!adjusting) { latestDragStart.value(sourceAt(down.position, localZoom, localPan), 28.0 / (baseFit * localZoom)); adjusting = true }
                                    latestDrag.value(sourceAt(change.position, localZoom, localPan), false)
                                }
                            } else if (tool == Tool.CROP) {
                                if (!adjusting) { onCropStart(sourceAt(change.previousPosition, localZoom, localPan), 28.0 / (baseFit * localZoom)); adjusting = true }
                                onCropDrag(sourceAt(change.position, localZoom, localPan))
                            } else if ((tool == Tool.LINES || tool == Tool.MEASURE) && !transformed && (tool == Tool.LINES && latestDragToDraw.value && noDraft || gestureGuides.any { it.id == latestSelectedGuideId.value && it.curveMid?.let { mid -> distance(sourceAt(down.position, localZoom, localPan), mid) < 42.0 / (baseFit * localZoom) } == true } ||
                                    noDraft && nearEndpoint(sourceAt(down.position, localZoom, localPan), localZoom, gestureGuides, gestureLines))) {
                                if (!adjusting) { latestDragStart.value(sourceAt(down.position, localZoom, localPan), 28.0 / (baseFit * localZoom)); adjusting = true }
                                latestDrag.value(sourceAt(change.position, localZoom, localPan), false)
                        } else if (tool != Tool.ADJUST && localZoom > 1.01f) {
                                localPan = localPan + delta
                                onTransform(localZoom, localPan)
                            }
                            moved = true
                            if (tool != Tool.LINES || adjusting) change.consume()
                        }
                        lastDistance = 0f
                    }
                } while (event.changes.any { it.pressed })
                holdJob?.cancel()
                heldLineId = null
                heldGuideId = null
                if (adjusting) { if (tool == Tool.CROP) onCropEnd() else latestDragEnd.value() }
                if (!moved && !transformed && !adjusting) {
                    val now = System.currentTimeMillis()
                    val rawPoint = sourceAt(down.position, localZoom, localPan)
                    val hitRadius = 28.0 / (baseFit * localZoom)
                    val point = snapToGuide(rawPoint, gestureGuides, (snapRadiusOnScreen / (baseFit * localZoom)).toDouble())
                        ?: snapToFeature(bitmap, rawPoint, snapRadiusOnScreen / (baseFit * localZoom)).point
                    if (tool == Tool.ADJUST && now - downAt >= 360L) latestLongPress.value(rawPoint, hitRadius)
                    else if (now - downAt >= 450L) latestLongPress.value(rawPoint, hitRadius)
                    else {
                        val nearbyTap = now - previousTapAt < 320L && (down.position - previousTapPoint).getDistance() < junctionRadiusOnScreen
                        tapCount = if (nearbyTap) tapCount + 1 else 1
                        val junctionRadius = junctionRadiusOnScreen.toDouble() / (baseFit * localZoom)
                        val junction = if (tapCount == 2 && tool != Tool.CROP) findJunction(rawPoint, junctionRadius, gestureLines, gestureGuides, tool) else null
                        val tripleEligible = tool == Tool.ADJUST && closestGuide(rawPoint, gestureGuides, junctionRadius * 2) != null
                        if (tapCount >= 3 && tripleEligible) {
                            pendingTap?.cancel(); tapCount = 0; previousTapAt = 0L
                            latestTripleTap.value(rawPoint, junctionRadius)
                        } else if (tapCount == 2 && tripleEligible) {
                            pendingTap?.cancel()
                            previousTapAt = now; previousTapPoint = down.position
                            pendingTap = holdScope.launch {
                                delay(320)
                                if (junction != null) {
                                    junctionIndicator = junction.point
                                    repeat(3) { junctionIndicatorVisible = true; delay(150); junctionIndicatorVisible = false; delay(150) }
                                    junctionIndicator = null; latestJunction.value(junction)
                                } else latestAddPoint.value(rawPoint, hitRadius)
                                tapCount = 0
                            }
                        } else if (tapCount == 2 && (junction != null || doubleTapEnabled)) {
                            pendingTap?.cancel(); previousTapAt = 0L; tapCount = 0
                            if (junction != null) holdScope.launch {
                                junctionIndicator = junction.point
                                repeat(3) { junctionIndicatorVisible = true; delay(150); junctionIndicatorVisible = false; delay(150) }
                                junctionIndicator = null; latestJunction.value(junction)
                            } else onDoubleTap()
                        } else {
                            previousTapAt = now
                            previousTapPoint = down.position
                            pendingTap = holdScope.launch {
                                delay(320)
                                latestTap.value(if (tool == Tool.NONE || tool == Tool.ADJUST) rawPoint else point, hitRadius)
                            }
                        }
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxSize().clipToBounds().then(touch)) {
            withTransform({ rotate(rotation, pivot) }) {
                drawRect(Color.White, topLeft = Offset(left, top), size = Size(imageW, imageH))
                // A mirrored view shows the picture mirrored too.
                withTransform({ scale(if (mirrorX) -1f else 1f, if (mirrorY) -1f else 1f, Offset(left + imageW / 2f, top + imageH / 2f)) }) {
                    drawImage(bitmap.asImageBitmap(), dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()), dstSize = androidx.compose.ui.unit.IntSize(imageW.toInt(), imageH.toInt()), alpha = 1f - pictureOptions.transparency / 100f)
                }
                if (ghosts.isNotEmpty()) drawIntoCanvas { canvas ->
                    // Drawn opaque into a translucent layer so overlapping faces don't darken.
                    canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint().apply { alpha = 0.3f })
                    ghosts.forEach { (argb, triangles) -> drawPath(trianglePath(mirroredPairs(triangles), left, top, fit), Color(argb).copy(alpha = 1f)) }
                    canvas.restore()
                }
                // Top and Profile canvases put the front drawing plane (Z = 0) across their middle.
                if (DepthWords.plane != ModelPlane.FRONT) {
                    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                    val zeroColor = Color(0xFF607D8B)
                    if (DepthWords.plane == ModelPlane.TOP) drawLine(zeroColor, Offset(left, top + imageH / 2f), Offset(left + imageW, top + imageH / 2f), strokeWidth = 3f, pathEffect = dash)
                    else drawLine(zeroColor, Offset(left + imageW / 2f, top), Offset(left + imageW / 2f, top + imageH), strokeWidth = 3f, pathEffect = dash)
                }
                // Lines drawn in the other views, grey, above the Z = 0 line (a front line lies right on it seen from Top or Profile).
                ghostLines.forEach { (key, pts) ->
                    val chosen = key == highlightGhost
                    var i = 0
                    while (i + 3 < pts.size) { drawLine(if (chosen) Color(0xFF1565C0) else Color(0xFF37474F), viewAt(Pt(pts[i], pts[i + 1])), viewAt(Pt(pts[i + 2], pts[i + 3])), strokeWidth = if (chosen) 10f else 5f, cap = androidx.compose.ui.graphics.StrokeCap.Round); i += 2 }
                    // End dots show which points can be grabbed here (none along smooth outlines).
                    if (key !in ghostNoDots) { drawCircle(Color(0xFF455A64), 9f, viewAt(Pt(pts[0], pts[1]))); drawCircle(Color(0xFF455A64), 9f, viewAt(Pt(pts[pts.size - 2], pts[pts.size - 1]))) }
                    // A line seen end-on is a single point here: mark it so it can't vanish.
                    if (chosen || pts.size >= 4 && hypot((pts[pts.size - 2] - pts[0]).toDouble(), (pts[pts.size - 1] - pts[1]).toDouble()) * fit < 6) drawCircle(if (chosen) Color(0xFF1565C0) else Color(0xFF6E7A76), if (chosen) 12f else 8f, viewAt(Pt(pts[0], pts[1])))
                }
                // Picked items: a wide orange band for a whole object (shape, broken line, 3D object), a thin one for a single line.
                pickedOutlines.forEach { (pts, whole) -> pts.zipWithNext().forEach { (a, b) ->
                    val from = viewAt(Pt(a.x, a.y)); val to = viewAt(Pt(b.x, b.y))
                    if (whole) { drawLine(Color(0x88FF8F00), from, to, strokeWidth = 11.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round); drawLine(Color(0xFFE65100), from, to, strokeWidth = 1.5.dp.toPx()) }
                    else drawLine(Color(0xCCFF8F00), from, to, strokeWidth = 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                } }
                itemRect?.let { (a, b) ->
                    val tl = viewAt(a).let { p -> viewAt(b).let { q -> Offset(min(p.x, q.x), min(p.y, q.y)) } }
                    drawRect(Color(0x33FF8F00), tl, Size(abs(a.x - b.x) * fit, abs(a.y - b.y) * fit)); drawRect(Color(0xFFFF8F00), tl, Size(abs(a.x - b.x) * fit, abs(a.y - b.y) * fit), style = Stroke(3f))
                }
                // Picked points: a ring around each.
                ghostPointMarks.forEach { p -> val c = viewAt(Pt(p.x, p.y)); drawCircle(Color.White, 11.dp.toPx(), c, style = Stroke(4.dp.toPx())); drawCircle(Color(0xFFE65100), 11.dp.toPx(), c, style = Stroke(2.5.dp.toPx())) }
                ghostPointMark?.let { p -> val c = viewAt(Pt(p.x, p.y)); drawCircle(Color.White, 22f, c); drawCircle(Color(0xFFE65100), 22f, c, style = Stroke(7f)); drawCircle(Color(0xFFE65100), 8f, c) }
                growArrows.forEach { (anchor, dir) -> growArrow(viewAt(anchor), screenDir(dir)) }
                // A selected circle's size handles: small squares on its rim.
                rimHandles.forEach { p -> val c = viewAt(p); val s = 7.dp.toPx()
                    drawRect(Color.White, Offset(c.x - s, c.y - s), Size(2 * s, 2 * s)); drawRect(Color(0xFFCF2020), Offset(c.x - s, c.y - s), Size(2 * s, 2 * s), style = Stroke(2.dp.toPx())) }
                foreignOverlay?.let { overlay ->
                    // The selected object from another plane, its handles and its outline points.
                    val accent = Color(0xFF1565C0)
                    fun screen(p: Pt) = viewAt(Pt(p.x, p.y))
                    drawIntoCanvas { canvas ->
                        canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint().apply { alpha = 0.45f })
                        val path = trianglePath(mirroredPairs(overlay.triangles), left, top, fit)
                        drawPath(path, accent)
                        canvas.restore()
                    }
                    overlay.handles.forEach { (kind, anchor, dir) ->
                        // Front and back handles grow the object: blinking orange arrows. Side handles resize it.
                        if (growOn && (kind == ForeignHandle.FACE || kind == ForeignHandle.BACK)) growArrow(screen(anchor), screenDir(dir))
                        else {
                            val sd = screenDir(dir)
                            val a = screen(anchor); val h = Offset(a.x + sd.x * 61.6f, a.y + sd.y * 61.6f)
                            drawLine(accent, a, h, strokeWidth = 4f)
                            drawCircle(Color.White, 30f, h); drawCircle(accent, 30f, h, style = Stroke(6f))
                            val ax = sd.x * 16f; val ay = sd.y * 16f
                            drawLine(accent, Offset(h.x - ax, h.y - ay), Offset(h.x + ax, h.y + ay), strokeWidth = 5f)
                        }
                    }
                    overlay.points.forEach { (p, active) ->
                        val c = screen(p)
                        drawCircle(if (active) Color(0xFFE65100) else Color.White, 15f, c); drawCircle(if (active) Color(0xFFE65100) else accent, 15f, c, style = Stroke(5f))
                    }
                }
            }
            joinPick?.let { pick ->
                // The parts of the crossing lines: blinking ones can be chosen, green ones are kept, red blinking ones will be deleted.
                fun screen(p: Pt) = viewAt(Pt(p.x, p.y))
                val center = screen(pick.action.point)
                pick.arms.forEachIndexed { index, arm ->
                    val end = screen(arm.endpoint)
                    val trace = (pick.paths.getOrNull(index) ?: listOf(pick.action.point, arm.endpoint)).map(::screen)
                    val kept = index in pick.selected
                    val decided = pick.selected.size == 2
                    val color = when {
                        kept -> Color(0xFF00A152)
                        decided && pick.keepRemaining -> Color(0xFF7A8783)
                        decided -> Color(0xFFD50000).copy(alpha = blink)
                        else -> Color(0xFFFF8F00).copy(alpha = blink)
                    }
                    trace.zipWithNext().forEach { (a, b) -> drawLine(Color.White, a, b, strokeWidth = 18f, cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                    trace.zipWithNext().forEach { (a, b) -> drawLine(color, a, b, strokeWidth = 11f, cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                    drawCircle(Color.White, 22f, end); drawCircle(color, 22f, end, style = Stroke(6f))
                }
                drawCircle(Color(0xFF17211E), 9f, center)
            }
            foreignOverlay?.label?.let { text ->
                drawIntoCanvas { canvas ->
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 42f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
                    paint.style = Paint.Style.STROKE; paint.strokeWidth = 9f; paint.color = AndroidColor.WHITE; canvas.nativeCanvas.drawText(text, size.width / 2f, 64f, paint)
                    paint.style = Paint.Style.FILL; paint.color = 0xFF1A2421.toInt(); canvas.nativeCanvas.drawText(text, size.width / 2f, 64f, paint)
                }
            }
            withTransform({ rotate(rotation, pivot) }) {
                val gridScale = if (tool == Tool.ADJUST) drawingScale else null
                if (gridScale != null && gridScale.isFinite() && gridScale > 0 && fit > 0) {
                    val desired = max(70.0 / (gridScale * fit), max(bitmap.width, bitmap.height) / (300.0 * gridScale))
                    val power = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(desired)))
                    val step = power * when { desired / power <= 1 -> 1; desired / power <= 2 -> 2; desired / power <= 5 -> 5; else -> 10 }
                    val spacing = (step * gridScale * fit).toFloat()
                    if (spacing.isFinite() && spacing > 0f) {
                        var x = left
                        while (x <= left + imageW) { drawLine(Color.White.copy(alpha = 0.5f), Offset(x, top), Offset(x, top + imageH), 1.dp.toPx()); x += spacing }
                        var y = top
                        while (y <= top + imageH) { drawLine(Color.White.copy(alpha = 0.5f), Offset(left, y), Offset(left + imageW, y), 1.dp.toPx()); y += spacing }
                    }
                }
                pictureOptions.figure?.let { figure -> silhouetteHeight(lines, unit, figure)?.let { height ->
                    drawComparisonFigure(drawContext.canvas.nativeCanvas, figure, left + imageW * 0.06f, top + imageH * 0.95f, height * fit)
                } }
                val shapes = drawableGuideShapes(guides)
                val separators = shapes.flatMap { attachedRegions(it, guides) }.flatMap { it.separatorIds }.toSet()
                // Holes stay empty: no fill is painted inside them.
                drawContext.canvas.save()
                shapes.filter { s -> s.all { it.hole } }.forEach { hole ->
                    val pts = shapePoints(hole)
                    if (pts.isNotEmpty()) drawContext.canvas.clipPath(androidx.compose.ui.graphics.Path().apply { val f = viewAt(pts.first()); moveTo(f.x, f.y); pts.drop(1).forEach { viewAt(it).let { p -> lineTo(p.x, p.y) } }; close() }, androidx.compose.ui.graphics.ClipOp.Difference)
                }
                shapes.filterNot { s -> s.all { it.hole } }.forEach { shape ->
                    (shape.first().fillTransparency ?: if (extrusions.any { extrusion -> (extrusion.depthMm ?: 0.0) > 0.0 && shape.all { guide -> extrusionGuides(extrusion, guides).any { it.id == guide.id } } }) 90 else null)?.let { transparency ->
                        drawContext.canvas.save()
                        (listOf(shapePoints(shape)) + attachedRegions(shape, guides).map { it.points }).forEach { points ->
                            if (points.isNotEmpty()) {
                                val path = androidx.compose.ui.graphics.Path().apply { val first = viewAt(points.first()); moveTo(first.x, first.y); points.drop(1).forEach { point -> viewAt(point).let { lineTo(it.x, it.y) } }; close() }
                                drawPath(path, guideColor(shape.first().colorIndex).copy(alpha = 1f - transparency / 100f))
                                drawContext.canvas.clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference)
                            }
                        }
                        drawContext.canvas.restore()
                    }
                }
                drawContext.canvas.restore()
                drawContext.canvas.save()
                shapes.forEach { shape ->
                    (listOf(shapePoints(shape)) + attachedRegions(shape, guides).map { it.points }).forEach { points ->
                        if (points.isNotEmpty()) {
                            val path = androidx.compose.ui.graphics.Path().apply { val first = viewAt(points.first()); moveTo(first.x, first.y); points.drop(1).forEach { viewAt(it).let { p -> lineTo(p.x, p.y) } }; close() }
                            drawContext.canvas.clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference)
                        }
                    }
                }
                // Shapes with depth are drawn flat like any other shape; their 3D look is in View 3D.
                drawContext.canvas.restore()
                // Imported models: the outline of each one's shadow on this view.
                models.forEach { (id, argb, hull) ->
                    if (hull.size < 3) return@forEach
                    val path = androidx.compose.ui.graphics.Path().apply { hull.forEachIndexed { i, p -> val v = viewAt(p); if (i == 0) moveTo(v.x, v.y) else lineTo(v.x, v.y) }; close() }
                    drawPath(path, Color(argb).copy(alpha = 0.3f))
                    drawPath(path, if (id == selectedModelId) Color(0xFFCF2020) else Color(argb).copy(alpha = 1f), style = Stroke((if (id == selectedModelId) 3.0 else 1.5).dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 8f))))
                }
                // Bars and pipes seen end on: their cut shape, holes left open.
                sections.forEach { (argb, loops) ->
                    val path = androidx.compose.ui.graphics.Path().apply {
                        fillType = androidx.compose.ui.graphics.PathFillType.EvenOdd
                        loops.forEach { loop -> loop.forEachIndexed { i, p -> val v = viewAt(p); if (i == 0) moveTo(v.x, v.y) else lineTo(v.x, v.y) }; close() }
                    }
                    drawPath(path, Color(argb).copy(alpha = 0.45f))
                    drawPath(path, Color(argb).copy(alpha = 1f), style = Stroke(1.5.dp.toPx()))
                }
                val selectedGuide = selectedGuideId?.let { id -> guides.firstOrNull { it.id == id } }
                // Filled shapes (a fill, a sheet material or depth) get a thin single border; the thick double line is for
                // plain lines and unfilled outlines.
                // Closed outlines count even when seen edge-on from another view (then they have no area here).
                val filledIds = (drawableGuideShapes(guides) + closedGuideShapes(guides)).filter { shape ->
                    val t = shape.first().fillTransparency ?: if (extrusions.any { e -> (e.depthMm ?: 0.0) > 0.0 && shape.all { g -> extrusionGuides(e, guides).any { it.id == g.id } } }) 90 else null
                    t != null && t < 100
                }.flatten().map { it.id }.toSet() + guides.filter { it.hole }.map { it.id }
                // Circles already labeled (an outline lying exactly on its shape shares the label).
                val labeledCircles = mutableSetOf<List<Int>>()
                guides.forEach { guide ->
                    val start = viewAt(guide.start); val end = viewAt(guide.end)
                    val separator = guide.id in separators
                    val border = guide.id in filledIds
                    val width = if (separator) (guide.strokeWidth * 0.35f).coerceAtLeast(1f) else guide.strokeWidth
                    val lineColor = guideColor(guide.colorIndex).copy(alpha = if (separator) 0.45f else 1f)
                    if (guide.curveMid != null) {
                        val mid = viewAt(guide.curveMid)
                        val path = androidx.compose.ui.graphics.Path().apply { moveTo(start.x, start.y); quadraticTo(2f * mid.x - (start.x + end.x) / 2f, 2f * mid.y - (start.y + end.y) / 2f, end.x, end.y) }
                        // A selected curved shape (a circle, say) turns red as a whole.
                        val chosen = !separator && tool != Tool.LINES && selectedGuide != null && sameGuideObject(guide, selectedGuide)
                        if (border) drawPath(path, if (chosen) Color(0xFFCF2020) else lineColor, style = Stroke((if (chosen) 2.5 else 1.2).dp.toPx()))
                        else {
                            drawPath(path, if (chosen) Color(0xFFCF2020) else lineColor, style = Stroke(width * strokeZoom))
                            if (!separator) drawPath(path, Color.White, style = Stroke((guide.strokeWidth * 0.45f).coerceAtLeast(1.5f) * strokeZoom))
                        }
                        // A single curve shows its end dots and its bend handle; a circle's pieces don't.
                        val circlePiece = guide.pathId != null && guides.count { it.pathId == guide.pathId && it.plane == guide.plane } >= 6 && guides.filter { it.pathId == guide.pathId && it.plane == guide.plane }.all { it.curveMid != null }
                        if (!circlePiece && guide.id == selectedGuideId && (tool == Tool.LINES || tool == Tool.ADJUST)) {
                            listOf(start, end).forEach { drawCircle(guideColor(guide.colorIndex), 6.dp.toPx(), it); drawCircle(Color.White, 3.dp.toPx(), it) }
                            drawCircle(Color.White, 10.dp.toPx(), mid)
                            drawCircle(if (curveBlink) Color(0xFF0066BE) else Color.White, 8.dp.toPx(), mid)
                        }
                    } else if (border) {
                        val chosen = !separator && selectedGuide != null && sameGuideObject(guide, selectedGuide)
                        drawLine(if (chosen) Color(0xFFCF2020) else lineColor, start, end, (if (chosen) 2.5 else 1.2).dp.toPx())
                    } else if (!separator && selectedGuide != null && sameGuideObject(guide, selectedGuide)) {
                        drawLine(Color(0xFFCF2020), start, end, (guide.strokeWidth + 2f) * strokeZoom)
                        drawLine(Color.White, start, end, (guide.strokeWidth * 0.45f).coerceAtLeast(1.5f) * strokeZoom)
                    } else {
                        drawLine(lineColor, start, end, width * strokeZoom)
                        if (!separator) drawLine(Color.White, start, end, (guide.strokeWidth * 0.45f).coerceAtLeast(1.5f) * strokeZoom)
                    }
                    // Canvas labels are measurements only.  Material is intentionally kept in
                    // Properties/Material, rather than painted across the user's drawing.
                    // A circle gets a single "Ø diameter" label instead of one per piece.
                    val objectGuides = guides.filter { sameGuideObject(it, guide) }
                    val firstOfObject = objectGuides.minOf { it.id } == guide.id
                    val materialName: String? = null
                    val circle = guide.pathId != null && objectGuides.size >= 6 && objectGuides.all { it.curveMid != null }
                    val circleKey = if (circle) objectGuides.map { it.start }.let { p -> listOf(p.minOf { it.x }, p.minOf { it.y }, p.maxOf { it.x }, p.maxOf { it.y }).map { it.roundToInt() } } else null
                    // Seen edge-on (from another view) a circle is a line: no label there.
                    val edgeOn = circleKey != null && (circleKey[2] - circleKey[0] < 2 || circleKey[3] - circleKey[1] < 2)
                    if (circle && firstOfObject && showSizes && !edgeOn && labeledCircles.add(circleKey!!)) drawingScale?.takeIf { it > 0 }?.let { pxPerUnit ->
                        val pts = objectGuides.map { it.start }
                        val diameterPx = pts.maxOf { a -> pts.maxOf { b -> distance(a, b) } }
                        val center = viewAt(Pt(pts.map { it.x }.average().toFloat(), pts.minOf { it.y }))
                        val w = pts.maxOf { it.x } - pts.minOf { it.x }; val h = pts.maxOf { it.y } - pts.minOf { it.y }
                        val size = if (abs(w - h) <= 0.01f * max(w, h)) "Ø ${formatMeasure(diameterPx / pxPerUnit, unit)} $unit"
                            else "${formatMeasure(w / pxPerUnit, unit)} × ${formatMeasure(h / pxPerUnit, unit)} $unit oval"
                        val text = size + (materialName?.let { " · $it" } ?: "")
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 14.sp.toPx(); isFakeBoldText = true; style = Paint.Style.STROKE; strokeWidth = 4.dp.toPx(); color = AndroidColor.WHITE }
                        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y - 10.dp.toPx(), paint)
                        paint.style = Paint.Style.FILL; paint.color = AndroidColor.BLACK
                        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y - 10.dp.toPx(), paint)
                    }
                    if (!circle && showSizes && (guide.lengthMm != null || materialName != null)) drawingScale?.takeIf { it > 0 }?.let { pxPerUnit ->
                        val pts = guidePoints(guide); val n = (pts.size - 1).coerceAtLeast(1)
                        val seen = pts.zipWithNext().sumOf { (a, b) -> distance(a, b) } / pxPerUnit * unitFactorMm(unit)
                        val mm = pts.withIndex().zipWithNext().sumOf { (a, b) ->
                            val flat = distance(a.value, b.value) / pxPerUnit * unitFactorMm(unit)
                            hypot(flat, (guide.endZMm - guide.startZMm) * (b.index - a.index) / n)
                        }
                        // Above the middle of the line (beside it for steep lines), so it doesn't cover the line.
                        val mid = viewAt(guidePointAt(guide, 0.5f)); val a0 = viewAt(guide.start); val b0 = viewAt(guide.end)
                        val steep = abs(b0.y - a0.y) > abs(b0.x - a0.x)
                        // A line seen end on sits above its ring and cut shape.
                        val endOn = (b0 - a0).getDistance() < 4f
                        val center = if (endOn) Offset(mid.x, mid.y - 36.dp.toPx()) else if (steep) Offset(mid.x + 10.dp.toPx(), mid.y) else Offset(mid.x, mid.y - 10.dp.toPx())
                        // When part of the line goes into depth, say how long it looks here too.
                        // On a closed shape the material is named alone (one edge's length would mislead), unless that edge has a set length.
                        val inShape = guide.pathId != null && objectGuides.size > 1 && guide.lengthMm == null
                        val text = if (inShape && materialName != null) materialName
                            else "${formatMeasure(mm / unitFactorMm(unit), unit)} $unit" + (if (!endOn && mm - seen > 0.01 * mm) " (${formatMeasure(seen / unitFactorMm(unit), unit)} seen here)" else "") + (materialName?.let { " · $it" } ?: "")
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = if (steep) Paint.Align.LEFT else Paint.Align.CENTER; textSize = 14.sp.toPx(); isFakeBoldText = true; style = Paint.Style.STROKE; strokeWidth = 4.dp.toPx(); color = AndroidColor.WHITE }
                        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y, paint)
                        paint.style = Paint.Style.FILL; paint.color = AndroidColor.BLACK
                        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y, paint)
                    }
                }
                // Every line's ends get a dot, so a line seen end on (a point) is still visible.
                // Joints inside a smooth curved outline (a circle's pieces) get no dot.
                val smoothIds = smoothOutlineIds(guides)
                fun smoothJoint(p: Pt, g: GuideLine) = g.curveMid != null && g.pathId != null && guides.count { o -> o.pathId == g.pathId && o.curveMid != null && (distance(o.start, p) < 0.6 || distance(o.end, p) < 0.6) } >= 2
                guides.sortedBy { if (selectedGuide != null && sameGuideObject(it, selectedGuide)) 0 else 1 }
                    .flatMap { guide -> listOf(guide.start to guide, guide.end to guide) }.filterNot { (p, g) -> smoothJoint(p, g) || g.id in smoothIds || g.id in filledIds && !(selectedGuide != null && sameGuideObject(g, selectedGuide)) }
                    .fold(emptyList<Pair<Pt, GuideLine>>()) { vertices, vertex -> if (vertices.any { distance(it.first, vertex.first) < 0.6 }) vertices else vertices + vertex }
                    .forEach { (point, guide) ->
                        val center = viewAt(point)
                        val active = selectedGuide != null && sameGuideObject(guide, selectedGuide)
                        // The same size on screen at any zoom, so dots never swallow the drawing.
                        drawCircle(guideColor(guide.colorIndex), if (active) 6.dp.toPx() else 3.dp.toPx(), center)
                        if (active) drawCircle(Color.White, 2.5.dp.toPx(), center)
                        // A line pointing straight into the screen shows as a ring.
                        if ((viewAt(guide.start) - viewAt(guide.end)).getDistance() < 4f) drawCircle(guideColor(guide.colorIndex), 7.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                    }
                val screenLines = lines.map { ScreenLine(it.id, viewAt(it.start), viewAt(it.end)) }
                val occupiedLabels = mutableListOf<androidx.compose.ui.geometry.Rect>()
                val pointNames = measurementPointNames(lines)
                val drawnNames = mutableSetOf<String>()
                if (unknownBlinkOn) lines.filter { !it.isReference && it.value <= 0.0 }.forEach { line ->
                    drawLine(Color.White, viewAt(line.start), viewAt(line.end), 14f * strokeZoom)
                }
                lines.forEach { line ->
                    drawAnnotation(viewAt(line.start), viewAt(line.end), line.value, line.label, line.isReference, line.isScaleReference, line.colorIndex, line.labelOffset * fit, unit, unitVisibility, zoom, line.id, isHorizontalCalibration(line), screenLines, occupiedLabels, line.pathId != null && !line.isScaleReference, !line.isReference && line.value <= 0.0, line.hasDepth())
                    pointNames[line.id]?.let { names ->
                        fun vertexLabel(point: Offset, text: String) {
                            if (!drawnNames.add(text)) return
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f * strokeZoom.coerceIn(1f, 3f); isFakeBoldText = true }
                            val x = point.x + 12f * strokeZoom; val y = point.y - 12f * strokeZoom
                            drawContext.canvas.nativeCanvas.drawText(text, x, y, Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f * strokeZoom; color = AndroidColor.WHITE })
                            drawContext.canvas.nativeCanvas.drawText(text, x, y, paint.apply { color = AndroidColor.BLACK })
                        }
                        vertexLabel(viewAt(line.start), names.first)
                        vertexLabel(viewAt(line.end), names.second)
                    }
                }
            if (heldLineBlink) heldLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
                val a = viewAt(line.start); val b = viewAt(line.end)
                drawLine(Color.White, a, b, 14f * strokeZoom)
                drawLine(Color(0xFF0066BE), a, b, 8f * strokeZoom)
            }
            if (heldLineBlink) heldGuideId?.let { id -> guides.firstOrNull { it.id == id } }?.let { guide ->
                drawLine(Color.White, viewAt(guide.start), viewAt(guide.end), (guide.strokeWidth + 9f) * strokeZoom)
                drawLine(Color(0xFF0066BE), viewAt(guide.start), viewAt(guide.end), (guide.strokeWidth + 4f) * strokeZoom)
            }
            if (deleteBlinkOn) deleteCandidateId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
                val a = viewAt(line.start); val b = viewAt(line.end)
                drawLine(Color.Black, a, b, 11f * strokeZoom)
                drawLine(Color(0xFFCF2020), a, b, 6f * strokeZoom)
                drawCircle(Color(0xFFCF2020), 14f * strokeZoom, a)
                drawCircle(Color(0xFFCF2020), 14f * strokeZoom, b)
            }
            if (alignmentBlinkOn) lines.filter { it.id in alignmentLineIds }.forEach { line ->
                val a = viewAt(line.start); val b = viewAt(line.end)
                drawLine(Color.Black, a, b, 11f * strokeZoom)
                drawLine(Color(0xFF0066CC), a, b, 6f * strokeZoom)
                drawCircle(Color(0xFF0066CC), 14f * strokeZoom, a)
                drawCircle(Color(0xFF0066CC), 14f * strokeZoom, b)
            }
            if (edgeBlinkOn) edgeSnapPoint?.let { point ->
                val center = viewAt(point)
                drawCircle(Color.White, 18f * strokeZoom, center, style = Stroke(6f * strokeZoom))
                drawCircle(Color(0xFF19B45A), 18f * strokeZoom, center, style = Stroke(3f * strokeZoom))
            }
            if (adjustmentBlinkOn) {
                adjustmentLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
                    drawLine(Color.Black, viewAt(line.start), viewAt(line.end), 11f * strokeZoom)
                    drawLine(Color(0xFF0066BE), viewAt(line.start), viewAt(line.end), 6f * strokeZoom)
                }
                adjustmentGuideId?.let { id -> guides.firstOrNull { it.id == id } }?.let { guide ->
                    drawLine(Color.Black, viewAt(guide.start), viewAt(guide.end), 9f * strokeZoom)
                    drawLine(Color(0xFF0066BE), viewAt(guide.start), viewAt(guide.end), 4f * strokeZoom)
                }
                adjustmentIndicatorPoint?.let { point ->
                    val center = viewAt(point)
                    drawCircle(Color.White, 20f * strokeZoom, center, style = Stroke(7f * strokeZoom))
                    drawCircle(Color(0xFF0066BE), 20f * strokeZoom, center, style = Stroke(3f * strokeZoom))
                }
            }
            draftStart?.let { start -> drawPoint(viewAt(start), if (tool == Tool.LINES) guideColor(guideColorIndex) else Color(0xFFFFD400)) }
            if (draftStart != null && pendingEnd != null) when (draftShape) {
                // Rectangle preview: blinks green while it is a square.
                LineDrawing.RECTANGLE -> {
                    val a = viewAt(draftStart); val b = viewAt(pendingEnd!!)
                    val square = squareEnd(draftStart, pendingEnd!!).second
                    drawRect(if (square && squareBlink) Color(0xFF19B45A) else guideColor(guideColorIndex), Offset(min(a.x, b.x), min(a.y, b.y)), Size(abs(a.x - b.x), abs(a.y - b.y)), style = Stroke(if (square) guideStrokeWidth * 1.6f else guideStrokeWidth))
                }
                LineDrawing.CIRCLE -> drawCircle(guideColor(guideColorIndex), (viewAt(pendingEnd!!) - viewAt(draftStart)).getDistance(), viewAt(draftStart), style = Stroke(guideStrokeWidth))
                else -> drawLine(if (tool == Tool.LINES) guideColor(guideColorIndex) else Color(0xFFFFD400), viewAt(draftStart), viewAt(pendingEnd!!), if (tool == Tool.LINES) guideStrokeWidth else 3f)
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedEndpoint?.let { endpoint ->
                lines.firstOrNull { it.id == endpoint.lineId }?.let { line ->
                    drawLine(Color.Black, viewAt(line.start), viewAt(line.end), 10f * strokeZoom)
                    drawLine(Color(0xFF0066BE), viewAt(line.start), viewAt(line.end), 5f * strokeZoom)
                    val point = viewAt(if (endpoint.isStart) line.start else line.end)
                    drawCircle(Color.White, 20f * strokeZoom, point, style = Stroke(6f * strokeZoom))
                    drawCircle(Color(0xFF0066BE), 20f * strokeZoom, point, style = Stroke(3f * strokeZoom))
                }
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedGuideEndpoint?.let { endpoint ->
                guides.firstOrNull { it.id == endpoint.guideId }?.let { guide ->
                    val point = viewAt(if (endpoint.isStart) guide.start else guide.end)
                    drawCircle(Color.Black, 18f * strokeZoom, point, style = Stroke(6f * strokeZoom))
                    drawCircle(Color(0xFF0066BE), 18f * strokeZoom, point, style = Stroke(3f * strokeZoom))
                }
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedWholeLineId?.let { id ->
                val pathId = lines.firstOrNull { it.id == id }?.pathId
                lines.filter { it.id == id || pathId != null && it.pathId == pathId }.forEach { line ->
                    drawLine(Color(0xFF0066BE), viewAt(line.start), viewAt(line.end), 7f * strokeZoom)
                }
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedWholeGuideId?.let { id ->
                val pathId = guides.firstOrNull { it.id == id }?.pathId
                // The selection blinks along the real lines, curves included.
                guides.filter { it.id == id || pathId != null && it.pathId == pathId }.forEach { guide ->
                    val pts = guidePoints(guide).map(::viewAt)
                    pts.zipWithNext().forEach { (a, b) -> drawLine(Color(0xFF0066BE), a, b, 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round) }
                }
            }
            if (tool == Tool.ADJUST) {
                if (selectionStart != null && selectionEnd != null) {
                    val a = viewAt(selectionStart); val b = viewAt(selectionEnd)
                    val topLeft = Offset(min(a.x, b.x), min(a.y, b.y))
                    val rectSize = Size(abs(a.x - b.x), abs(a.y - b.y))
                    drawRect(Color(0xFF55BDF5).copy(alpha = 0.22f), topLeft, rectSize)
                    drawRect(Color(0xFF168AD0), topLeft, rectSize, style = Stroke(2.dp.toPx()))
                }
                selectedVertices.forEach { vertex ->
                    val center = viewAt(vertex)
                    drawCircle(Color.White, 9.dp.toPx(), center)
                    drawCircle(Color(0xFF168AD0), 7.dp.toPx(), center)
                    drawCircle(Color.White, 3.dp.toPx(), center)
                }
            }
            if (tool == Tool.CROP) cropFrame?.let { frame ->
                val a = viewAt(Pt(frame.left, frame.top)); val b = viewAt(Pt(frame.right, frame.bottom))
                drawLine(Color(0xFFCC2020), a, Offset(b.x, a.y), 4f)
                drawLine(Color(0xFFCC2020), Offset(b.x, a.y), b, 4f)
                drawLine(Color(0xFFCC2020), b, Offset(a.x, b.y), 4f)
                drawLine(Color(0xFFCC2020), Offset(a.x, b.y), a, 4f)
            }
            if (junctionIndicatorVisible) junctionIndicator?.let { point ->
                val center = viewAt(point)
                drawCircle(Color.White, 18.dp.toPx(), center, style = Stroke(6.dp.toPx()))
                drawCircle(Color(0xFF00823B), 18.dp.toPx(), center, style = Stroke(3.dp.toPx()))
            }
            }
            magnifierPoint?.let { point -> drawMagnifier(bitmap, point, Offset(viewW - 100f, 100f)) }
        }
        if (showFineControls) {
            val position = when {
                selectedVertices.isNotEmpty() -> Pt(selectedVertices.map { it.x }.average().toFloat(), selectedVertices.map { it.y }.average().toFloat())
                selectedCurveId != null -> guides.firstOrNull { it.id == selectedCurveId }?.curveMid
                selectedEndpoint != null -> lines.firstOrNull { it.id == selectedEndpoint.lineId }?.let { if (selectedEndpoint.isStart) it.start else it.end }
                selectedGuideEndpoint != null -> guides.firstOrNull { it.id == selectedGuideEndpoint.guideId }?.let { if (selectedGuideEndpoint.isStart) it.start else it.end }
                selectedWholeGuideId != null -> guides.firstOrNull { it.id == selectedWholeGuideId }?.start
                selectedWholeLineId != null -> lines.firstOrNull { it.id == selectedWholeLineId }?.start
                selectedLabelId != null -> lines.firstOrNull { it.id == selectedLabelId }?.let(::labelAnchor)
                selectedExtrusionId != null -> extrusions.firstOrNull { it.id == selectedExtrusionId }?.handle
                else -> null
            }
            if (position != null) {
                val initial = remember(selectedCurveId, selectedEndpoint, selectedGuideEndpoint, selectedWholeGuideId, selectedWholeLineId, selectedLabelId, selectedExtrusionId, selectedVertices.size) { position }
                val moveScale = drawingScale?.takeIf { it.isFinite() && it > 0 }
                val suffix = if (moveScale == null) "px" else unit
                fun amount(value: Double) = if (moveScale == null) format(value) else formatMeasure(value / moveScale, unit)
                val extrusion = extrusions.firstOrNull { it.id == selectedExtrusionId }
                val zMm = extrusion?.let { ex -> val current = depthMmForHandle(ex, position, moveScale, unit); val start = depthMmForHandle(ex, initial, moveScale, unit); if (current != null && start != null) current - start else null }
                Column(Modifier.align(Alignment.BottomEnd).padding(end = 170.dp, bottom = 20.dp).background(Color(0xCC18201E), RoundedCornerShape(4.dp)).padding(8.dp)) {
                    // Screen axes are X/Y in Front, X/Z in Top and Z (growing to the left)/Y in Profile.
                    val across = if (DepthWords.plane == ModelPlane.PROFILE) "Z" else "X"
                    val acrossSign = if (DepthWords.plane == ModelPlane.PROFILE) -1.0 else 1.0
                    val down = if (DepthWords.plane == ModelPlane.TOP) "Z" else "Y"
                    Text("Δ$across ${amount(acrossSign * (position.x - initial.x))} $suffix", color = Color.White, fontSize = 12.sp)
                    Text("Δ$down ${amount((position.y - initial.y).toDouble())} $suffix", color = Color.White, fontSize = 12.sp)
                    Text(if (zMm == null) "Δ${DepthWords.axis} —" else "Δ${DepthWords.axis} ${formatMeasure(zMm / unitFactorMm(unit), unit)} $unit", color = Color.White, fontSize = 12.sp)
                }
            }
            FineAdjustPad { dx, dy -> onNudge(if (mirrorX) -dx else dx, if (mirrorY) -dy else dy) }
        }
    }
}

private fun DrawScope.drawAnnotation(a: Offset, b: Offset, value: Double, label: String, reference: Boolean, manuallySet: Boolean, colorIndex: Int, manualLabelOffset: Float, unit: String, unitVisibility: String, zoom: Float, lineId: Int, horizontalCalibration: Boolean, allLines: List<ScreenLine>, occupiedLabels: MutableList<androidx.compose.ui.geometry.Rect>, autoBroken: Boolean = false, unknown: Boolean = false, depth: Boolean = false) {
    val color = if (reference || autoBroken) Color(0xFFFFD400) else lineColor(colorIndex)
    val size = zoom.coerceIn(1f, 5f)
    drawLine(Color.Black, a, b, 8f * size); drawLine(color, a, b, 4f * size)
    drawPoint(a, color, size); drawPoint(b, color, size)
    val unitText = displayedUnit(unit, unitVisibility, reference)
    val display = if (unknown) if (label.isBlank()) "?" else "$label: ?" else if (reference && !manuallySet) label.ifBlank { "Level" } else if (label.isBlank()) "${formatMeasure(value, unit)}${if (depth) "*" else ""}$unitText" else "$label: ${formatMeasure(value, unit)}${if (depth) "*" else ""}$unitText"
    val lineLength = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
    val unscaledWidth = Paint().apply { textSize = 13f }.measureText(display) + 16f
    val labelScale = minOf(size, ((lineLength - 26f * size) / unscaledWidth).coerceIn(0.55f, size))
    val textSize = 13f * labelScale
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = if (reference) AndroidColor.BLACK else color.toArgb(); this.textSize = textSize; isFakeBoldText = true }
    val width = textPaint.measureText(display) + 16f * labelScale
    var angle = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
    if (angle > 90f || angle < -90f) angle += 180f
    val midpoint = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
    val needsOffset = width + 26f * size > lineLength
    val radians = Math.toRadians(angle.toDouble())
    val normal = Offset((-sin(radians)).toFloat(), cos(radians).toFloat())
    val baseMid = if (needsOffset) midpoint + normal * (22f * size + 14f * labelScale) else midpoint
    if (horizontalCalibration) {
        val levelCenter = midpoint - normal * (24f * size)
        drawRoundRect(Color(0xFFFFD400), topLeft = levelCenter - Offset(13f * size, 6f * size), size = Size(26f * size, 12f * size), cornerRadius = CornerRadius(4f * size, 4f * size))
        drawLine(Color(0xFF202020), levelCenter - Offset(9f * size, 0f), levelCenter + Offset(9f * size, 0f), 2f * size)
        drawCircle(Color(0xFF202020), 3f * size, levelCenter)
    }
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    val textHeight = 18f * labelScale
    fun labelBounds(center: Offset): androidx.compose.ui.geometry.Rect {
        val halfWidth = width / 2f
        val halfHeight = textHeight / 2f
        val boundX = kotlin.math.abs(direction.x) * halfWidth + kotlin.math.abs(direction.y) * halfHeight + 4f
        val boundY = kotlin.math.abs(direction.y) * halfWidth + kotlin.math.abs(direction.x) * halfHeight + 4f
        return androidx.compose.ui.geometry.Rect(center.x - boundX, center.y - boundY, center.x + boundX, center.y + boundY)
    }
    val shifts = listOf(manualLabelOffset, 0f, -1f, 1f, -2f, 2f, -3f, 3f, -4f, 4f, -5f, 5f).map { shift -> if (shift == manualLabelOffset) shift else shift * (width * 0.55f + 14f * size) }
    val mid = shifts.asSequence().map { shift -> baseMid + direction * shift }.firstOrNull { candidate ->
        val bounds = labelBounds(candidate)
        occupiedLabels.none { it.overlaps(bounds) } && allLines.none { other -> other.id != lineId && segmentIntersectsRect(other.start, other.end, bounds) }
    } ?: baseMid + direction * shifts.last()
    occupiedLabels += labelBounds(mid)
    val native = drawContext.canvas.nativeCanvas
    native.save()
    native.rotate(angle, mid.x, mid.y)
    val textX = mid.x - width / 2f + 8f * labelScale
    val textY = mid.y + 5f * labelScale
    native.drawText(display, textX, textY, Paint(textPaint).apply { style = Paint.Style.STROKE; strokeWidth = 3.5f * labelScale; this.color = AndroidColor.WHITE })
    native.drawText(display, textX, textY, textPaint)
    native.restore()
}
private fun DrawScope.drawPoint(p: Offset, color: Color, zoom: Float = 1f) {
    drawCircle(Color.Black, 12f * zoom.coerceAtMost(2.5f), p); drawCircle(color, 8f * zoom.coerceAtMost(2.5f), p); drawCircle(Color(0xFF18201E), 3f * zoom.coerceAtMost(2.5f), p)
}

private fun segmentIntersectsRect(a: Offset, b: Offset, rect: androidx.compose.ui.geometry.Rect): Boolean {
    if (rect.contains(a) || rect.contains(b)) return true
    val topLeft = Offset(rect.left, rect.top)
    val topRight = Offset(rect.right, rect.top)
    val bottomLeft = Offset(rect.left, rect.bottom)
    val bottomRight = Offset(rect.right, rect.bottom)
    return segmentsIntersect(a, b, topLeft, topRight) || segmentsIntersect(a, b, topRight, bottomRight) ||
        segmentsIntersect(a, b, bottomRight, bottomLeft) || segmentsIntersect(a, b, bottomLeft, topLeft)
}

private fun segmentsIntersect(a: Offset, b: Offset, c: Offset, d: Offset): Boolean {
    fun cross(first: Offset, second: Offset, third: Offset): Float =
        (second.x - first.x) * (third.y - first.y) - (second.y - first.y) * (third.x - first.x)
    val first = cross(a, b, c)
    val second = cross(a, b, d)
    val third = cross(c, d, a)
    val fourth = cross(c, d, b)
    return (first > 0f) != (second > 0f) && (third > 0f) != (fourth > 0f)
}

private fun DrawScope.drawMagnifier(bitmap: Bitmap, p: Pt, center: Offset) {
    val r = 86f; drawCircle(Color.White, r + 3f, center); drawCircle(Color(0xFF18201E), r, center)
    clipPath(androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(center - Offset(r, r), center + Offset(r, r))) }) {
        val crop = 17f; val src = androidx.compose.ui.geometry.Rect(p.x - crop, p.y - crop, p.x + crop, p.y + crop)
        drawImage(bitmap.asImageBitmap(), srcOffset = androidx.compose.ui.unit.IntOffset(src.left.coerceAtLeast(0f).toInt(), src.top.coerceAtLeast(0f).toInt()), srcSize = androidx.compose.ui.unit.IntSize((src.width).toInt(), (src.height).toInt()), dstOffset = androidx.compose.ui.unit.IntOffset((center.x - r).toInt(), (center.y - r).toInt()), dstSize = androidx.compose.ui.unit.IntSize((r * 2).toInt(), (r * 2).toInt()))
        drawLine(Color(0xFFFFD400), center - Offset(12f, 0f), center + Offset(12f, 0f), 2f); drawLine(Color(0xFFFFD400), center - Offset(0f, 12f), center + Offset(0f, 12f), 2f)
    }
}

@Composable private fun BoxScope.FineAdjustPad(onNudge: (Float, Float) -> Unit) {
    Box(
        Modifier.align(Alignment.BottomEnd).padding(14.dp).size(150.dp)
            .background(Color(0xDD172220), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("hold", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        NudgeButton("^", Modifier.align(Alignment.TopCenter), { onNudge(0f, -1f) })
        NudgeButton("v", Modifier.align(Alignment.BottomCenter), { onNudge(0f, 1f) })
        NudgeButton("<", Modifier.align(Alignment.CenterStart), { onNudge(-1f, 0f) })
        NudgeButton(">", Modifier.align(Alignment.CenterEnd), { onNudge(1f, 0f) })
    }
}

@Composable private fun NudgeButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed) {
            delay(280)
            var interval = 70L
            while (pressed) {
                onClick()
                delay(interval)
                interval = (interval - 5L).coerceAtLeast(25L)
            }
        }
    }
    Button(
        onClick = onClick, modifier = modifier.padding(7.dp).size(42.dp), shape = CircleShape,
        interactionSource = interaction, contentPadding = PaddingValues(0.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006E67))
    ) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}

@Composable private fun CalibrationList(lines: List<MeasureLine>, unit: String, modifier: Modifier, onSelect: (MeasureLine) -> Unit) {
    val references = lines.filter { it.isReference }
    if (references.isEmpty()) Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No calibration lines yet", color = Color(0xFF68726F)) }
    else Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp)) {
        references.forEachIndexed { index, line -> Row(Modifier.fillMaxWidth().clickable { onSelect(line) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(Color(0xFFFFD400), RoundedCornerShape(5.dp))); Spacer(Modifier.width(8.dp)); Text(if (line.label.isBlank()) if (line.isScaleReference) "Calibration ${index + 1}" else "Level" else line.label, Modifier.weight(1f), fontWeight = FontWeight.Medium); Text(if (line.isScaleReference) "${formatMeasure(line.value, unit)} $unit${CalibrationAxis.of(line.axis).takeIf { it != CalibrationAxis.UNIFORM }?.let { " (${it.label})" } ?: ""}" else "Level", fontWeight = FontWeight.Bold)
        } }
    }
}

@Composable private fun DistanceDialog(isReference: Boolean, referenceCount: Int, unit: String, unknownSize: Boolean, onUnknownSize: (Boolean) -> Unit, onDismiss: () -> Unit, onConfirm: (Double?, String, String, Boolean, CalibrationAxis) -> Unit) {
    var value by remember { mutableStateOf("") }; var label by remember { mutableStateOf("") }
    var axis by remember { mutableStateOf(CalibrationAxis.UNIFORM) }
    var enteredUnit by remember { mutableStateOf(unit) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    var levelOnly by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isReference) if (referenceCount == 0) "Set scale" else "Add 90 degree reference" else "Enter measurement") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isReference) {
                Text("Calibration unit")
                Box {
                    OutlinedButton(onClick = { unitMenuOpen = true }) { Text(enteredUnit) }
                    DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                        listOf("mm", "cm", "m", "in", "ft").forEach { candidate ->
                            DropdownMenuItem(text = { Text(candidate) }, onClick = { enteredUnit = candidate; unitMenuOpen = false })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = levelOnly, onCheckedChange = { levelOnly = it })
                    Text("Level only (no distance)")
                }
                if (!levelOnly) {
                    Text("Applies to")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CalibrationAxis.entries.forEach { candidate ->
                            FilterChip(selected = axis == candidate, onClick = { axis = candidate }, label = { Text(candidate.label) })
                        }
                    }
                    Text(when (axis) {
                        CalibrationAxis.UNIFORM -> "One scale for X, Y and Z. Recommended."
                        CalibrationAxis.X -> "Advanced: only the horizontal (X) part of this line is used. Add a Y reference too. Use this only to compensate for a stretched photo."
                        CalibrationAxis.Y -> "Advanced: only the vertical (Y) part of this line is used. Add an X reference too. Use this only to compensate for a stretched photo."
                    }, fontSize = 12.sp, color = Color(0xFF4D5A57))
                }
            }
            if (!isReference) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onUnknownSize(!unknownSize) }) {
                Checkbox(checked = unknownSize, onCheckedChange = onUnknownSize)
                Text("Size unknown")
            }
            Text(if (isReference && levelOnly) "This reference is used for alignment only." else if (isReference) "Enter the known distance in $enteredUnit." else if (unknownSize) "Set the length later." else "Enter the known distance.")
            if (!levelOnly && (isReference || !unknownSize)) OutlinedTextField(value, { if (decimalInputPattern.matches(it)) value = it }, label = { Text("Distance (${if (isReference) enteredUnit else unit})") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(label, { label = it }, label = { Text("Label (optional)") }, singleLine = true)
        }
    }, confirmButton = { TextButton(onClick = { if (isReference && levelOnly) onConfirm(null, label, enteredUnit, true, CalibrationAxis.UNIFORM) else if (!isReference && unknownSize) onConfirm(null, label, unit, false, CalibrationAxis.UNIFORM) else value.toDoubleOrNull()?.takeIf { it > 0 }?.let { onConfirm(it, label, if (isReference) enteredUnit else unit, false, axis) } }, enabled = (isReference && levelOnly) || (!isReference && unknownSize) || value.toDoubleOrNull()?.let { it > 0 } == true) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun ShapePairPreview(first: List<GuideLine>, second: List<GuideLine>) {
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val paths = listOf(shapePoints(first), shapePoints(second))
        val all = paths.flatten()
        val minX = all.minOf { it.x }; val minY = all.minOf { it.y }
        val maxX = all.maxOf { it.x }; val maxY = all.maxOf { it.y }
        val scale = min((size.width - 40f) / (maxX - minX).coerceAtLeast(1f), (size.height - 40f) / (maxY - minY).coerceAtLeast(1f))
        paths.forEachIndexed { index, points ->
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(20f + (points.first().x - minX) * scale, 20f + (points.first().y - minY) * scale)
                points.drop(1).forEach { lineTo(20f + (it.x - minX) * scale, 20f + (it.y - minY) * scale) }; close()
            }
            val color = if (index == 0) Color(0xFF00796B) else Color(0xFFCB3C27)
            drawPath(path, color.copy(alpha = 0.2f)); drawPath(path, color, style = Stroke(3.dp.toPx()))
            val p = points.first()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toArgb(); textSize = 20.sp.toPx(); isFakeBoldText = true }
            drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), 20f + (p.x - minX) * scale, 20f + (p.y - minY) * scale, paint)
        }
    }
}


@Composable private fun ProjectTitleDialog(project: SavedProject, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember(project.id) { mutableStateOf(project.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Picture title") },
        text = { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(title.trim()) }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable private fun PictureEditDialog(currentName: String, currentUnit: String, currentOptions: PictureOptions, hasScale: Boolean, onExportImage: () -> Unit, onExport3d: () -> Unit, onExportDxf: () -> Unit, onImportModel: () -> Unit, onSaveMaterial: () -> Unit, onMaterialList: () -> Unit, onMeasurements: () -> Unit, onDismiss: () -> Unit, onSave: (String, String, Map<String, Int>, PictureOptions) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    var unit by remember { mutableStateOf(currentUnit) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf(currentOptions) }
    val precision = remember { mutableStateMapOf<String, Int>().apply { putAll(MeasurementPrecision.places) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit picture") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Picture name") }, singleLine = true)
                OutlinedButton(onClick = onExportImage) { Text("Export picture") }
                OutlinedButton(onClick = onExport3d) { Text("Export 3D model (STL or 3MF)") }
                OutlinedButton(onClick = onExportDxf) { Text("Export 2D drawing (DXF)") }
                OutlinedButton(onClick = onImportModel) { Text("Import 3D model (STL, OBJ, 3MF)") }
                OutlinedButton(onClick = onSaveMaterial) { Text("Save drawing as material") }
                OutlinedButton(onClick = onMaterialList) { Text("Materials used (lengths)") }
                OutlinedButton(onClick = onMeasurements) { Text("Measurements") }
                Text("Measurement unit", fontWeight = FontWeight.Medium)
                Box {
                    OutlinedButton(onClick = { unitMenuOpen = true }) { Text(unit) }
                    DropdownMenu(expanded = unitMenuOpen, onDismissRequest = { unitMenuOpen = false }) {
                        listOf("mm", "cm", "m", "in", "ft").forEach { candidate -> DropdownMenuItem(text = { Text(candidate) }, onClick = { unit = candidate; unitMenuOpen = false }) }
                    }
                }
                Text("Decimal places ($unit)", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..2).forEach { places ->
                        FilterChip(selected = precision[unit] == places, onClick = { precision[unit] = places }, label = { Text(places.toString()) })
                    }
                }
                Text("0 displays whole units. Saved measurements keep their precision.", fontSize = 12.sp, color = Color(0xFF53615D))
                Text("Scale comparison", fontWeight = FontWeight.Medium)
                ComparisonFigure.entries.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { figure ->
                            val selected = options.figure == figure
                            IconButton(onClick = { options = options.copy(figure = if (selected) null else figure) }, modifier = Modifier.size(48.dp).border(1.dp, if (selected) Color(0xFF006E67) else Color(0xFF9AA6A1), RoundedCornerShape(4.dp)).background(if (selected) Color(0xFFD9EEEA) else Color.Transparent).semantics { contentDescription = figure.description }) {
                                Canvas(Modifier.size(34.dp)) {
                                    val height = min(size.height, size.width / figure.widthRatio) * 0.9f
                                    drawComparisonFigure(drawContext.canvas.nativeCanvas, figure, (size.width - height * figure.widthRatio) / 2f, (size.height + height) / 2f, height, 255)
                                }
                            }
                        }
                    }
                }
                if (options.figure != null && !hasScale) Text("Set a known calibration length to show the silhouette.", fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { options = options.copy(guess = !options.guess) }) { Checkbox(options.guess, { options = options.copy(guess = it) }); Text("Guess unmeasured measurements") }
                Text("Picture transparency", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0, 10, 90, 100).forEach { transparency ->
                        FilterChip(selected = options.transparency == transparency, onClick = { options = options.copy(transparency = transparency) }, label = { Text("$transparency%", fontSize = 12.sp) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { options = options.copy(sum = !options.sum) }) { Checkbox(options.sum, { options = options.copy(sum = it) }); Text("Add measurement total") }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { options = options.copy(legend = !options.legend) }) { Checkbox(options.legend, { options = options.copy(legend = it) }); Text("Show legend table") }
            }
    },
        confirmButton = { TextButton(onClick = { onSave(name.trim().ifBlank { "Picture" }, unit, precision.toMap(), options) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable private fun CanvasSizeDialog(width: Int, height: Int, onDismiss: () -> Unit, onApply: (Int, Int, Int, Int) -> Unit) {
    var left by remember { mutableStateOf("") }; var top by remember { mutableStateOf("") }
    var right by remember { mutableStateOf("") }; var bottom by remember { mutableStateOf("") }
    fun number(value: String) = if (value.isBlank()) 0 else value.toIntOrNull()
    val margins = listOf(left, top, right, bottom).map(::number)
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Canvas size") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Current: $width × $height px")
            Text("Add white space (pixels)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(left, { if (it.length <= 6 && it.all(Char::isDigit)) left = it }, label = { Text("Left") }, placeholder = { Text("0") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedTextField(right, { if (it.length <= 6 && it.all(Char::isDigit)) right = it }, label = { Text("Right") }, placeholder = { Text("0") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(top, { if (it.length <= 6 && it.all(Char::isDigit)) top = it }, label = { Text("Top") }, placeholder = { Text("0") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedTextField(bottom, { if (it.length <= 6 && it.all(Char::isDigit)) bottom = it }, label = { Text("Bottom") }, placeholder = { Text("0") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            }
            if (margins.all { it != null }) Text("New: ${width.toLong() + margins[0]!! + margins[2]!!} × ${height.toLong() + margins[1]!! + margins[3]!!} px", fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(enabled = margins.all { it != null } && margins.any { it != null && it > 0 }, onClick = { onApply(margins[0]!!, margins[1]!!, margins[2]!!, margins[3]!!) }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun PointDepthDialog(unit: String, count: Int, initialZ: Double?, initialThickness: Double?, onDismiss: () -> Unit, onSave: (Double?, Double?) -> Unit) {
    fun editable(value: Double?) = value?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: ""
    var distance by remember { mutableStateOf(editable(initialZ?.let(::abs))) }
    var back by remember { mutableStateOf(initialZ != null && initialZ < 0) }
    var positionEdited by remember { mutableStateOf(false) }
    var thickness by remember { mutableStateOf(editable(initialThickness)) }
    var thicknessEdited by remember { mutableStateOf(false) }
    val value = distance.replace(',', '.').toDoubleOrNull()
    val thicknessValue = thickness.replace(',', '.').toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Move selected points in 3D") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("$count selected 3D points")
            if (count < 2) Text("Select at least two points on 3D shapes with a positive depth.")
            OutlinedTextField(distance, { if (decimalInputPattern.matches(it)) { distance = it; positionEdited = true } }, label = { Text(if (back) "${DepthWords.away} by ($unit)" else "${DepthWords.toward} by ($unit)") }, placeholder = { if (initialZ == null) Text("Mixed") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text("Offset along ${DepthWords.axis} from the shape's position", fontSize = 12.sp)
            ZDirectionPicker(back = back, onChange = { back = it; positionEdited = true })
            OutlinedTextField(thickness, { if (decimalInputPattern.matches(it)) { thickness = it; thicknessEdited = true } }, label = { Text("Thickness ($unit)") }, placeholder = { if (initialThickness == null) Text("Mixed") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text("Changes appear in the 3D view and STL export.", fontSize = 12.sp)
        }
    }, confirmButton = { TextButton(enabled = count >= 2 && (!positionEdited || value != null && value.isFinite()) && (!thicknessEdited || thicknessValue != null && thicknessValue.isFinite() && thicknessValue > 0), onClick = { onSave(if (!positionEdited) null else if (back) -value!! else value!!, if (thicknessEdited) thicknessValue else null) }) { Text("Apply") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun ZDirectionPicker(back: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(false to DepthWords.toward, true to DepthWords.away).forEach { (direction, label) ->
            val selected = back == direction
            Row(Modifier.weight(1f).border(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF168142) else Color(0xFF8A9691), RoundedCornerShape(4.dp)).clickable { onChange(direction) }.padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = { onChange(direction) }, modifier = Modifier.size(40.dp), colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF168142)))
                Text(label, fontSize = 12.sp)
            }
        }
    }
}

@Composable private fun ShapePositionPreview(guides: List<GuideLine>, extrusions: List<Extrusion>, selectedId: Int, positionMm: Double, depthMm: Double?, scale: Double, unit: String) {
    val factor = unitFactorMm(unit) / scale
    data class PreviewShape(val points: List<Pt>, val extrusion: Extrusion, val z: Double, val depth: Double, val color: Color)
    val shapes = drawableGuideShapes(guides).mapNotNull { shape ->
        val extrusion = extrusions.firstOrNull { ex -> extrusionGuides(ex, guides).any { g -> shape.any { it.id == g.id } } } ?: return@mapNotNull null
        val z = if (extrusion.id == selectedId) positionMm else extrusion.positionZMm
        val depth = (if (extrusion.id == selectedId) depthMm else extrusion.depthMm) ?: (distance(extrusion.origin, extrusion.handle) * factor)
        PreviewShape(shapePoints(shape), extrusion, z, depth.coerceAtLeast(0.0), guideColor(shape.first().colorIndex))
    }.filter { it.points.size >= 3 }
    Canvas(Modifier.fillMaxWidth().height(160.dp).background(Color(0xFFF0F4F2), RoundedCornerShape(4.dp))) {
        if (shapes.isEmpty()) return@Canvas
        fun project(p: Pt, z: Double): Offset = Offset((p.x * factor + z * 0.55).toFloat(), (p.y * factor - z * 0.35).toFloat())
        val boundsPoints = shapes.flatMap { shape -> shape.points.flatMap { p -> listOf(project(p, 0.0), project(p, shape.z), project(p, shape.z - shape.depth)) } }
        val left = boundsPoints.minOf { it.x }; val right = boundsPoints.maxOf { it.x }
        val top = boundsPoints.minOf { it.y }; val bottom = boundsPoints.maxOf { it.y }
        val margin = 12.dp.toPx()
        val fit = min((size.width - margin * 2f) / (right - left).coerceAtLeast(1f), (size.height - margin * 2f) / (bottom - top).coerceAtLeast(1f))
        fun view(p: Pt, z: Double): Offset { val point = project(p, z); return Offset((size.width - (right - left) * fit) / 2f + (point.x - left) * fit, (size.height - (bottom - top) * fit) / 2f + (point.y - top) * fit) }
        fun face(points: List<Offset>, color: Color, border: Color) {
            if (points.size < 3) return
            val path = androidx.compose.ui.graphics.Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
            drawPath(path, color); drawPath(path, border, style = Stroke(1.dp.toPx()))
        }
        shapes.filter { it.extrusion.id == selectedId && abs(it.z) > 0.0001 }.forEach { shape ->
            val points = shape.points.map { view(it, 0.0) }
            val reference = androidx.compose.ui.graphics.Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
            drawPath(reference, Color(0xFF8B9995), style = Stroke(1.dp.toPx()))
        }
        shapes.sortedByDescending { it.z }.forEach { shape ->
            val edge = if (shape.extrusion.id == selectedId) Color(0xFF006E67) else shape.color
            val front = shape.points.map { view(it, shape.z) }
            val back = shape.points.map { view(it, shape.z - shape.depth) }
            shape.points.indices.forEach { i -> val next = (i + 1) % shape.points.size; face(listOf(front[i], front[next], back[next], back[i]), shape.color.copy(alpha = 0.16f), edge.copy(alpha = 0.45f)) }
            face(front, shape.color.copy(alpha = 0.20f), edge)
        }
    }
}

/** Export the view on screen as a 2D DXF drawing: which view, labels on or off, and what is left out. */
@Composable private fun DxfExportDialog(viewName: String, hasMeshes: Boolean, onDismiss: () -> Unit, onExport: (Boolean) -> Unit) {
    var includeLabels by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Export 2D drawing (DXF)") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("The $viewName view, as it is on screen, in millimeters. Layers: Geometry, Holes, Measurements, Material labels.", fontSize = 13.sp)
            Row(Modifier.fillMaxWidth().clickable { includeLabels = !includeLabels }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = includeLabels, onCheckedChange = { includeLabels = it })
                Text("Include labels (measurement values and material names)")
            }
            Text("Imported 3D models (STL, OBJ, 3MF) are not included in the 2D DXF export${if (hasMeshes) "; this project has some" else ""}. 3D objects drawn in another view are exported from that view.", fontSize = 12.sp, color = Color(0xFF4D5A57))
        }
    }, confirmButton = { TextButton(onClick = { onExport(includeLabels) }) { Text("Export") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/**
 * An imported model's name, size (one size changes all three, keeping proportions), center position, quarter turns,
 * color and look.
 */
@Composable private fun ModelDialog(model: ImportedModel, base: Mesh, unit: String, onDismiss: () -> Unit, onDelete: () -> Unit, onSave: (ImportedModel) -> Unit) {
    var m by remember(model.id) { mutableStateOf(model) }
    val factor = unitFactorMm(unit)
    fun show(mm: Double) = formatMeasure(mm / factor, unit)
    // Size at scale 1 with the current turns; the fields below show it times the scale.
    val unitSize = remember(m.rx, m.ry, m.rz, m.turns) { ModelImport.placed(base, 1.0, m.rx, m.ry, m.rz, Vec3(0.0, 0.0, 0.0), m.turnAxes).bounds.let { (l, h) -> h - l } }
    var sizeText by remember(model.id, m.rx, m.ry, m.rz, m.turns) { mutableStateOf(listOf(unitSize.x, unitSize.y, unitSize.z).map { show(it * m.scale) }) }
    var posText by remember(model.id) { mutableStateOf(listOf(m.center.x, m.center.y, m.center.z).map { show(it) }) }
    fun number(s: String) = s.replace(',', '.').toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("3D model") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(m.name, { m = m.copy(name = it) }, label = { Text("Name") }, singleLine = true)
            Text("${base.triangles.size} triangles. It stays one solid piece: move it by dragging in any view, or type exact values.", fontSize = 12.sp, color = Color(0xFF4D5A57))
            Text("Size ($unit), proportions kept", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("X", "Y", "Z").forEachIndexed { i, axis ->
                    OutlinedTextField(sizeText[i], { text ->
                        sizeText = sizeText.toMutableList().also { it[i] = text }
                        val along = listOf(unitSize.x, unitSize.y, unitSize.z)[i]
                        number(text)?.takeIf { it > 0 && along > 1e-9 }?.let { value ->
                            val scale = value * factor / along
                            m = m.copy(scale = scale)
                            sizeText = listOf(unitSize.x, unitSize.y, unitSize.z).mapIndexed { k, s -> if (k == i) text else show(s * scale) }
                        }
                    }, label = { Text(axis) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
            }
            Text("Center position ($unit)", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("X", "Y", "Z").forEachIndexed { i, axis ->
                    OutlinedTextField(posText[i], { text ->
                        posText = posText.toMutableList().also { it[i] = text }
                        number(text)?.let { v -> val mm = v * factor; m = m.copy(center = when (i) { 0 -> m.center.copy(x = mm); 1 -> m.center.copy(y = mm); else -> m.center.copy(z = mm) }) }
                    }, label = { Text(axis) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
            }
            Text("Turn 90°", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("X", "Y", "Z").forEachIndexed { axis, label -> OutlinedButton(onClick = { m = m.copy(turns = m.turns + (axis to 90.0)) }, modifier = Modifier.weight(1f)) { Text(label) } }
            }
            Text("Color", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0xFF9E9E9E, 0xFF5F6B6D, 0xFFC89B6A, 0xFF9CC9E0, 0xFF1565C0, 0xFFC62828, 0xFF2E7D32).map { it.toInt() }.forEach { c ->
                    Box(Modifier.size(28.dp).background(Color(c), CircleShape).border(if (c == m.argb) 3.dp else 0.dp, Color(0xFF00897B), CircleShape).clickable { m = m.copy(argb = c) })
                }
            }
            FinishPicker(m.finish) { m = m.copy(finish = it) }
            TextButton(onClick = onDelete) { Text("Delete model", color = Color(0xFFB3261E)) }
        }
    }, confirmButton = { TextButton(onClick = { onSave(m) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** What a closed shape is made of: nothing, a sheet that fills it, or bars or pipes along its edges (a frame). */
private data class MadeOf(val materialId: String?, val frame: Boolean)

@Composable private fun ShapeEditDialog(guide: GuideLine, extrusion: Extrusion?, guides: List<GuideLine>, extrusions: List<Extrusion>, unit: String, scale: Double?, width: Float, height: Float, materials: List<Material>, madeOf: MadeOf, onDismiss: () -> Unit, onSave: (Int, Int?, Double?, Double?, Double?, Boolean, Double?, MadeOf) -> Unit) {
    var made by remember(guide.id) { mutableStateOf(madeOf) }
    var color by remember(guide.id) { mutableIntStateOf(guide.colorIndex) }
    // 100 means lines only: the outline is drawn but nothing fills it, and it has no 3D body.
    var transparency by remember(guide.id) { mutableStateOf(guide.fillTransparency ?: if (extrusion != null) 90 else 100) }
    val originalWidth = remember(guide.id, unit) { guide.shapeWidthMm?.div(unitFactorMm(unit)) ?: scale?.let { width / it } }
    val originalHeight = remember(guide.id, unit) { guide.shapeHeightMm?.div(unitFactorMm(unit)) ?: scale?.let { height / it } }
    var shapeWidth by remember(guide.id, unit) { mutableStateOf(originalWidth?.let { formatMeasure(it, unit) } ?: "") }
    var shapeHeight by remember(guide.id, unit) { mutableStateOf(originalHeight?.let { formatMeasure(it, unit) } ?: "") }
    var widthEdited by remember(guide.id, unit) { mutableStateOf(false) }
    var heightEdited by remember(guide.id, unit) { mutableStateOf(false) }
    val originalDepth = remember(guide.id, unit, extrusion?.id) { extrusion?.depthMm?.div(unitFactorMm(unit)) }
    var depth by remember(guide.id, unit, extrusion?.id) { mutableStateOf(originalDepth?.let { formatMeasure(it, unit) } ?: "") }
    var depthEdited by remember(guide.id, unit, extrusion?.id) { mutableStateOf(false) }
    val depthValue = if (depthEdited) depth.replace(',', '.').toDoubleOrNull() else originalDepth
    val originalPosition = remember(guide.id, unit, extrusion?.id) { extrusion?.positionZMm?.div(unitFactorMm(unit)) ?: 0.0 }
    var zDistance by remember(guide.id, unit, extrusion?.id) { mutableStateOf(formatMeasure(abs(originalPosition), unit)) }
    var zBack by remember(guide.id, unit, extrusion?.id) { mutableStateOf(originalPosition < 0) }
    var zEdited by remember(guide.id, unit, extrusion?.id) { mutableStateOf(false) }
    val positionValue = if (!zEdited) originalPosition else zDistance.replace(',', '.').toDoubleOrNull()?.let { if (zBack) -it else it }
    val widthValue = if (widthEdited) shapeWidth.replace(',', '.').toDoubleOrNull() else originalWidth
    val heightValue = if (heightEdited) shapeHeight.replace(',', '.').toDoubleOrNull() else originalHeight
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Shape properties") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(shapeWidth, { if (decimalInputPattern.matches(it)) { shapeWidth = it; widthEdited = true } }, label = { Text("Width ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(shapeHeight, { if (decimalInputPattern.matches(it)) { shapeHeight = it; heightEdited = true } }, label = { Text("Height ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            // A sheet fills the shape at its thickness; a frame turns each edge into a bar or pipe.
            Text("Made of", fontWeight = FontWeight.Medium)
            @Composable fun Choice(choice: MadeOf, material: Material?, title: String) {
                // A sheet fills the shape, so "Lines only" turns into a light fill.
                fun pick() { made = choice; if (choice.materialId != null && !choice.frame && transparency == 100) transparency = 90 }
                Row(Modifier.fillMaxWidth().clickable { pick() }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = made == choice, onClick = { pick() })
                    if (material != null) ProfilePreview(material, Modifier.size(28.dp))
                    Column(Modifier.padding(start = 6.dp)) {
                        Text(title, fontSize = 14.sp)
                        material?.let { Text(it.description, fontSize = 11.sp, color = Color(0xFF4D5A57)) }
                    }
                }
            }
            Choice(MadeOf(null, false), null, "Nothing (a plain shape)")
            materials.filter { it.isSheet }.forEach { Choice(MadeOf(it.id, false), it, "Sheet: ${it.name}") }
            materials.filterNot { it.isSheet }.forEach { Choice(MadeOf(it.id, true), it, "Frame of lines: ${it.name}") }
            if (made.frame) Text("Each edge becomes this bar or pipe; the shape itself has no body.", fontSize = 12.sp, color = Color(0xFF4D5A57))
            // Every closed shape can be a 3D object: give it a depth here (a sheet sets its own thickness).
            if (!made.frame && materials.none { it.id == made.materialId } && transparency != 100)
                OutlinedTextField(depth, { if (decimalInputPattern.matches(it)) { depth = it; depthEdited = true } }, label = { Text("Depth ${DepthWords.axis} ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (extrusion != null) {
                OutlinedTextField(zDistance, { if (decimalInputPattern.matches(it)) { zDistance = it; zEdited = true } }, label = { Text(if (zBack) "${DepthWords.away} by ($unit)" else "${DepthWords.toward} by ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text(if (DepthWords.plane == ModelPlane.FRONT) "Distance from ${DepthWords.surface}" else "Position of the far side, from ${DepthWords.surface}", fontSize = 12.sp)
                ZDirectionPicker(back = zBack, onChange = { zBack = it; zEdited = true })
                if (scale != null && scale.isFinite() && scale > 0 && positionValue != null) {
                    Text("3D position preview", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    ShapePositionPreview(guides, extrusions, extrusion.id, positionValue * unitFactorMm(unit), depthValue?.times(unitFactorMm(unit)), scale, unit)
                } else Text("Calibrate to preview the 3D position.", fontSize = 12.sp)
            }
            if (scale == null) Text("Calibrate for scaled dimensions.", fontSize = 12.sp)
            Text("Color")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                guideColors.indices.forEach { index ->
                    Box(Modifier.size(32.dp).background(guideColor(index), CircleShape).border(if (color == index) 3.dp else 1.dp, if (color == index) Color(0xFF006E67) else Color.LightGray, CircleShape).clickable { color = index })
                }
            }
            Text("Fill", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(100 to "Lines only", 0 to "Filled", 50 to "50%", 90 to "90%").forEach { (value, label) ->
                    FilterChip(selected = transparency == value, onClick = { transparency = value }, label = { Text(label, fontSize = 12.sp) })
                }
            }
        }
    }, confirmButton = { TextButton(enabled = (shapeWidth.isBlank() || widthValue != null && widthValue > 0) && (shapeHeight.isBlank() || heightValue != null && heightValue > 0) && (depth.isBlank() || depthValue != null && depthValue >= 0) && (extrusion == null || positionValue != null && positionValue.isFinite()), onClick = { onSave(color, transparency, widthValue, heightValue, depthValue, depthEdited, if (extrusion != null) positionValue else null, made) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun LineEditDialog(line: MeasureLine, unit: String, points: Pair<String, String>?, onDismiss: () -> Unit, onSave: (Double, String, Int, Boolean, Double, Double) -> Unit, onDelete: () -> Unit) {
    var value by remember(line.id) { mutableStateOf(if (!line.isReference && line.value <= 0.0) "" else format(line.value)) }
    var label by remember(line.id) { mutableStateOf(line.label) }
    var colorIndex by remember(line.id) { mutableIntStateOf(line.colorIndex) }
    var lengthEdited by remember(line.id) { mutableStateOf(false) }
    var startZ by remember(line.id, unit) { mutableStateOf(java.math.BigDecimal.valueOf(abs(line.startZMm) / unitFactorMm(unit)).stripTrailingZeros().toPlainString()) }
    var endZ by remember(line.id, unit) { mutableStateOf(java.math.BigDecimal.valueOf(abs(line.endZMm) / unitFactorMm(unit)).stripTrailingZeros().toPlainString()) }
    var startBack by remember(line.id) { mutableStateOf(line.startZMm < 0) }
    var endBack by remember(line.id) { mutableStateOf(line.endZMm < 0) }
    fun zValue(text: String, back: Boolean): Double? = text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }?.let { if (back) -it else it }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (line.isReference) "Edit reference" else points?.let { "Edit ${it.first} - ${it.second}" } ?: "Edit measurement") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A manually entered size stays fixed. Calibration lines always determine the scale for calculated lines.")
                OutlinedTextField(value = value, onValueChange = { if (decimalInputPattern.matches(it)) { value = it; lengthEdited = true } }, label = { Text("Size ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, singleLine = true)
                if (!line.isReference) {
                    Text("Point ${DepthWords.axis} offset", fontWeight = FontWeight.Medium)
                    OutlinedTextField(startZ, { if (decimalInputPattern.matches(it)) startZ = it }, label = { Text("${points?.first ?: "Start"} ${DepthWords.axis} ($unit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    ZDirectionPicker(startBack) { startBack = it }
                    OutlinedTextField(endZ, { if (decimalInputPattern.matches(it)) endZ = it }, label = { Text("${points?.second ?: "End"} ${DepthWords.axis} ($unit)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    ZDirectionPicker(endBack) { endBack = it }
                    val size = value.toDoubleOrNull()
                    val dz = zValue(startZ, startBack)?.let { a -> zValue(endZ, endBack)?.let { b -> abs(b - a) } }
                    if (size != null && size > 0 && dz != null && dz > 0) {
                        val projected = projectedLengthForFixedTrue(size, dz)
                        Text(if (projected == null) "The Z difference (${formatMeasure(dz, unit)} $unit) is longer than the size. Increase the size or reduce the Z difference."
                            else "True 3D length ${formatMeasure(size, unit)} $unit. The front view shows a shorter ${formatMeasure(projected, unit)} $unit, because √(${formatMeasure(projected, unit)}² + ${formatMeasure(dz, unit)}²) = ${formatMeasure(size, unit)}.",
                            fontSize = 12.sp, color = if (projected == null) Color(0xFFB3261E) else Color(0xFF4D5A57))
                    }
                    Text("Length source: ${when (line.lengthSource) { LengthSource.CALCULATED -> "calculated from calibration"; LengthSource.MANUAL -> "entered manually"; LengthSource.MEASURED -> "measured by laser"; LengthSource.UNKNOWN -> "unknown" }}", fontSize = 12.sp, color = Color(0xFF4D5A57))
                }
                if (!line.isReference) OutlinedButton(onClick = { val a = zValue(startZ, startBack); val b = zValue(endZ, endBack); if (a != null && b != null) onSave(0.0, label, colorIndex, true, a, b) }) { Text("Set unknown") }
                if (!line.isReference) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Color")
                    lineColors.indices.forEach { index ->
                        Box(Modifier.size(28.dp).background(lineColor(index), CircleShape).border(3.dp, if (colorIndex == index) Color.White else Color.Transparent, CircleShape).clickable { colorIndex = index })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { val a = zValue(startZ, startBack); val b = zValue(endZ, endBack); value.toDoubleOrNull()?.takeIf { it > 0 }?.let { if (a != null && b != null) onSave(if (lengthEdited) it else line.value, label, colorIndex, lengthEdited, a, b) } }, enabled = value.toDoubleOrNull()?.let { it > 0 } == true && zValue(startZ, startBack) != null && zValue(endZ, endBack) != null) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E))) { Text("Delete") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private fun distance(a: Pt, b: Pt) = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
private fun snapEndpointAlignment(line: MeasureLine, endpoint: Endpoint, candidate: Pt, lines: List<MeasureLine>, rotation: Float): Pair<Pt, Set<Int>> {
    val current = if (endpoint.isStart) line.start else line.end
    val connected = lines.filter { item -> item.id == line.id || line.pathId != null && item.pathId == line.pathId && (distance(item.start, current) < 0.6 || distance(item.end, current) < 0.6) }
    val angle = Math.toRadians(-rotation.toDouble())
    val directions = mutableListOf(cos(angle) to sin(angle), -sin(angle) to cos(angle))
    lines.filter { it.isReference && it.id !in connected.map { segment -> segment.id } }.forEach { reference ->
        val dx = (reference.end.x - reference.start.x).toDouble(); val dy = (reference.end.y - reference.start.y).toDouble()
        val length = hypot(dx, dy)
        if (length > 1.0) { directions.add(dx / length to dy / length); directions.add(-dy / length to dx / length) }
    }
    data class Match(val id: Int, val anchor: Pt, val nx: Double, val ny: Double, val offset: Double)
    val matches = mutableListOf<Match>()
    connected.forEach { segment ->
        val anchor = if (distance(segment.start, current) < 0.6) segment.end else segment.start
        if (distance(anchor, candidate) > 1.0) directions.forEach { (dx, dy) ->
            val nx = -dy; val ny = dx
            val oldOffset = (current.x - anchor.x) * nx + (current.y - anchor.y) * ny
            val offset = (candidate.x - anchor.x) * nx + (candidate.y - anchor.y) * ny
            if (abs(oldOffset) > 0.01 && abs(offset) <= 1.25) matches.add(Match(segment.id, anchor, nx, ny, offset))
        }
    }
    val best = matches.minByOrNull { abs(it.offset) } ?: return candidate to emptySet()
    val snapped = Pt((candidate.x - best.offset * best.nx).toFloat(), (candidate.y - best.offset * best.ny).toFloat())
    val aligned = matches.filter { abs((snapped.x - it.anchor.x) * it.nx + (snapped.y - it.anchor.y) * it.ny) < 0.01 }.map { it.id }.toSet()
    return snapped to aligned
}

private fun snapToFeature(bitmap: Bitmap, touch: Pt, requestedRadius: Float): FeatureSnap {
    val radius = requestedRadius.coerceIn(3f, 220f)
    val stride = kotlin.math.ceil(radius / 42f).toInt().coerceAtLeast(1)
    val centerX = touch.x.toInt().coerceIn(1, bitmap.width - 2)
    val centerY = touch.y.toInt().coerceIn(1, bitmap.height - 2)
    val minX = (centerX - radius).toInt().coerceAtLeast(1)
    val maxX = (centerX + radius).toInt().coerceAtMost(bitmap.width - 2)
    val minY = (centerY - radius).toInt().coerceAtLeast(1)
    val maxY = (centerY + radius).toInt().coerceAtMost(bitmap.height - 2)
    fun luminance(x: Int, y: Int): Int {
        val pixel = bitmap.getPixel(x, y)
        return (AndroidColor.red(pixel) * 30 + AndroidColor.green(pixel) * 59 + AndroidColor.blue(pixel) * 11) / 100
    }
    var bestX = centerX
    var bestY = centerY
    var strongest = 0
    var bestScore = Float.NEGATIVE_INFINITY
    for (y in minY..maxY step stride) for (x in minX..maxX step stride) {
        val horizontal = kotlin.math.abs(luminance((x + stride).coerceAtMost(bitmap.width - 1), y) - luminance((x - stride).coerceAtLeast(0), y))
        val vertical = kotlin.math.abs(luminance(x, (y + stride).coerceAtMost(bitmap.height - 1)) - luminance(x, (y - stride).coerceAtLeast(0)))
        val contrast = horizontal + vertical
        val fromTouch = hypot((x - touch.x).toDouble(), (y - touch.y).toDouble()).toFloat()
        val cornerBonus = minOf(horizontal, vertical)
        val score = contrast + cornerBonus * 0.7f - fromTouch / radius * 22f
        if (score > bestScore) {
            bestScore = score
            strongest = contrast
            bestX = x
            bestY = y
        }
    }
    return if (strongest >= 80) FeatureSnap(Pt(bestX.toFloat(), bestY.toFloat()), true) else FeatureSnap(touch, false)
}

// New ids count only this view's own lines (positive); lines shown from other views carry negative stand-in ids.
private fun nextGuideId(guides: List<GuideLine>) = (guides.filter { it.id > 0 }.maxOfOrNull { it.id } ?: 0) + 1
private fun nextGuidePathId(guides: List<GuideLine>) = (guides.mapNotNull { it.pathId }.filter { it > 0 }.maxOrNull() ?: 0) + 1

/**
 * Repairs projects saved while new lines could get negative ids (when a view showed only other views' lines): such
 * ids, path and group numbers get fresh positive ones in their view, and objects and groups follow.
 */
private fun SavedProject.withPositiveIds(): SavedProject = withPositiveIdsOnly().withHolesAttached()

/**
 * Makes every cut-out a real hole of one object, whatever older versions left behind:
 * - an old 3D-only cut whose cutter lies inside the shape (same view) becomes a hole in that shape;
 * - a hole never has a 3D body of its own, nor takes part in 3D cuts or joins;
 * - a hole sits at its shape's depth; the same hole twice is kept once;
 * - a hole no longer inside any shape becomes an ordinary shape again.
 */
private fun SavedProject.withHolesAttached(): SavedProject {
    var lines = guides
    var objects = extrusions
    var ops = solidOps
    fun outline(e: Extrusion): List<GuideLine>? = lines.filter { it.plane == e.plane }.let { planeLines -> val edges = extrusionGuides(e, planeLines); closedGuideShapes(planeLines).firstOrNull { p -> p.any { g -> edges.any { it.id == g.id } } } }
    // 1. Old 3D cuts with the cutter inside the shape become holes.
    ops.filter { !it.join && it.target.first == it.tool.first }.forEach { op ->
        val target = objects.firstOrNull { it.plane == op.target.first && it.id == op.target.second } ?: return@forEach
        val tool = objects.firstOrNull { it.plane == op.tool.first && it.id == op.tool.second } ?: return@forEach
        val outer = outline(target) ?: return@forEach; val inner = outline(tool) ?: return@forEach
        val outerPts = shapePoints(outer)
        if (!shapePoints(inner).all { pointInsidePolygon(it, outerPts) }) return@forEach
        val ids = inner.map { it.id }.toSet()
        lines = lines.map { if (it.plane == tool.plane && it.id in ids) it.copy(hole = true, materialId = null, fillTransparency = null) else it }
        objects = objects.filterNot { it === tool }
        ops = ops.filterNot { it === op }
    }
    // 2. Holes have no body of their own.
    val holeBodies = objects.filter { e -> val edges = extrusionGuides(e, lines.filter { it.plane == e.plane }); edges.isNotEmpty() && edges.all { it.hole } }
    objects = objects.filterNot { it in holeBodies }
    val keys = objects.map { it.plane to it.id }.toSet()
    ops = ops.filter { it.target in keys && it.tool in keys }
    // 3. Holes at their shape's depth, duplicates once, orphans plain shapes again.
    lines = lines.groupBy { it.plane }.flatMap { (_, list) ->
        val holePaths = closedGuideShapes(list).filter { p -> p.all { it.hole } }
        val seen = mutableSetOf<List<Int>>(); val dropped = mutableSetOf<Int>()
        holePaths.forEach { p -> val pts = shapePoints(p); val key = listOf(pts.minOf { it.x }, pts.minOf { it.y }, pts.maxOf { it.x }, pts.maxOf { it.y }).map { it.roundToInt() }
            if (!seen.add(key)) dropped += p.map { it.id } }
        list.filterNot { it.id in dropped }.map { g ->
            if (!g.hole) g else holeContainer(g, list)?.let { c -> val outer = list.filter { sameGuideObject(it, c) }; val z = outer.map { (it.startZMm + it.endZMm) / 2 }.average(); g.copy(startZMm = z, endZMm = z) } ?: g.copy(hole = false)
        }
    }
    return copy(guides = lines, extrusions = objects, solidOps = ops)
}

private fun SavedProject.withPositiveIdsOnly(): SavedProject {
    if (guides.none { it.id <= 0 || (it.pathId ?: 1) <= 0 || (it.groupId ?: 1) <= 0 }) return this
    val idMap = HashMap<Pair<ModelPlane, Int>, Int>()
    val fixed = guides.groupBy { it.plane }.flatMap { (plane, list) ->
        var nextId = (list.filter { it.id > 0 }.maxOfOrNull { it.id } ?: 0) + 1
        var nextPath = (list.mapNotNull { it.pathId }.filter { it > 0 }.maxOrNull() ?: 0) + 1
        var nextGroup = (list.mapNotNull { it.groupId }.filter { it > 0 }.maxOrNull() ?: 0) + 1
        val paths = list.mapNotNull { it.pathId }.filter { it <= 0 }.distinct().associateWith { nextPath++ }
        val groups = list.mapNotNull { it.groupId }.filter { it <= 0 }.distinct().associateWith { nextGroup++ }
        list.map { g ->
            val id = if (g.id > 0) g.id else nextId++
            idMap[plane to g.id] = id
            g.copy(id = id, pathId = g.pathId?.let { paths[it] ?: it }, groupId = g.groupId?.let { groups[it] ?: it })
        }
    }
    return copy(guides = fixed,
        extrusions = extrusions.map { e -> e.copy(guideIds = e.guideIds.map { idMap[e.plane to it] ?: it }) },
        itemGroups = itemGroups.map { group -> group.map { (p, id) -> p to (idMap[p to id] ?: id) } })
}

private data class ExtrusionFace(val points: List<Pt>, val guide: GuideLine, val opacity: Float, val light: Float)

private fun depthHandle(extrusion: Extrusion, point: Pt): Pt {
    val dx = extrusion.vanishing.x - extrusion.origin.x; val dy = extrusion.vanishing.y - extrusion.origin.y
    val length = dx * dx + dy * dy
    val t = if (length < 0.001f) 0f else (((point.x - extrusion.origin.x) * dx + (point.y - extrusion.origin.y) * dy) / length).coerceIn(0f, 0.999f)
    return Pt(extrusion.origin.x + t * dx, extrusion.origin.y + t * dy)
}

private fun extrusionDepth(extrusion: Extrusion, width: Int, height: Int): Float {
    val focalPixels = distance(extrusion.origin, extrusion.vanishing)
    return if (focalPixels < 0.001 || width <= 0 || height <= 0) 0f else (distance(extrusion.origin, depthHandle(extrusion, extrusion.handle)) / focalPixels).coerceIn(0.0, 0.999).toFloat()
}

private fun extrusionWithDepth(extrusion: Extrusion, depthMm: Double, scale: Double?, unit: String): Extrusion {
    val focalPixels = distance(extrusion.origin, extrusion.vanishing)
    if (scale == null || scale <= 0.0 || focalPixels < 0.001) return extrusion.copy(depthMm = depthMm)
    val depthPixels = depthMm / unitFactorMm(unit) * scale
    val t = (depthPixels / (focalPixels + depthPixels)).coerceIn(0.0, 0.999).toFloat()
    val handle = Pt(extrusion.origin.x + (extrusion.vanishing.x - extrusion.origin.x) * t, extrusion.origin.y + (extrusion.vanishing.y - extrusion.origin.y) * t)
    return extrusion.copy(depthMm = depthMm, handle = handle)
}

private fun depthMmForHandle(extrusion: Extrusion, handle: Pt, scale: Double?, unit: String): Double? {
    val focalPixels = distance(extrusion.origin, extrusion.vanishing)
    if (scale == null || scale <= 0.0 || focalPixels < 0.001) return null
    val t = (distance(extrusion.origin, depthHandle(extrusion, handle)) / focalPixels).coerceIn(0.0, 0.999)
    return focalPixels * t / (1.0 - t) / scale * unitFactorMm(unit)
}

private fun extrusionProject(point: Pt, extrusion: Extrusion, depth: Float) =
    Pt(point.x + (extrusion.vanishing.x - point.x) * depth, point.y + (extrusion.vanishing.y - point.y) * depth)

private fun guidePoints(guide: GuideLine): List<Pt> {
    val mid = guide.curveMid ?: return listOf(guide.start, guide.end)
    val cx = 2f * mid.x - (guide.start.x + guide.end.x) / 2f
    val cy = 2f * mid.y - (guide.start.y + guide.end.y) / 2f
    return (0..64).map { index ->
        val t = index / 64f; val s = 1f - t
        Pt(s * s * guide.start.x + 2f * s * t * cx + t * t * guide.end.x, s * s * guide.start.y + 2f * s * t * cy + t * t * guide.end.y)
    }
}

private fun closedGuideShapes(guides: List<GuideLine>): List<List<GuideLine>> =
    guides.filter { it.pathId != null }.groupBy { it.pathId }.values.filter { path ->
        path.size >= 2 && path.zipWithNext().all { (a, b) -> distance(a.end, b.start) < 0.6 } && distance(path.last().end, path.first().start) < 0.6
    }

private fun shapePoints(path: List<GuideLine>): List<Pt> = path.flatMap { guidePoints(it).dropLast(1) }

// Printable geometry is built by the tested core (com.davoyans.idea3d.core) in model millimeters:
// X right, Y up, Z toward the front-view viewer. Each drawing plane is a canvas of the picture's size:
// the front canvas holds the photo, the Top and Profile canvases share its X and Y ranges and put
// Z = 0 across their middle. A drawn shape is the surface facing that plane's viewer and its
// thickness extends away from the viewer.

/** Converts a plane's canvas pixels to millimeters in that plane's (right, up) coordinates and back. */
private class PlaneMapping(val plane: ModelPlane, private val originU: Double, private val originV: Double, private val mmPerPixelRight: Double, private val mmPerPixelUp: Double) {
    fun right(p: Pt) = (p.x - originU) * mmPerPixelRight
    fun up(p: Pt) = (originV - p.y) * mmPerPixelUp
    fun pixel(right: Double, up: Double) = Pt((originU + right / mmPerPixelRight).toFloat(), (originV - up / mmPerPixelUp).toFloat())
    /** Model point of a canvas pixel at [depthMm] along the plane's viewing direction. */
    fun model(p: Pt, depthMm: Double) = plane.toModel(right(p), up(p), depthMm)
    /** Canvas pixel of a model point (its depth is dropped). */
    fun pixel(model: Vec3) = pixel(model dot plane.right, model dot plane.up)
    /** Model-space movement of a canvas drag (it has no depth component). */
    fun modelDelta(du: Double, dv: Double): Vec3 = plane.right * (du * mmPerPixelRight) - plane.up * (dv * mmPerPixelUp)
    /** Canvas movement of a model-space movement; its depth component is dropped. */
    fun pixelDelta(delta: Vec3) = Pt(((delta dot plane.right) / mmPerPixelRight).toFloat(), (-(delta dot plane.up) / mmPerPixelUp).toFloat())
}

private fun planeMapping(plane: ModelPlane, frame: PlaneFrame, scale: Double?, unit: String, axisScale: Pair<Double, Double>? = null): PlaneMapping? {
    val pixelsPerUnit = scale?.takeIf { it.isFinite() && it > 0 } ?: return null
    val uniform = unitFactorMm(unit) / pixelsPerUnit
    // Only the front photo can be calibrated per axis; its X runs along the canvas width and Y up it.
    return PlaneMapping(plane, frame.originU, frame.originV, axisScale?.first?.let { unitFactorMm(unit) / it } ?: uniform, axisScale?.second?.let { unitFactorMm(unit) / it } ?: uniform)
}

/**
 * Position of the surface facing the plane's viewer. Front objects store that surface (Z of the front face,
 * so they start on the drawing plane); Top and Profile objects store their far side, so a new one rests on
 * Y = 0 or X = 0 instead of hanging below or behind the scene.
 */
private fun facePositionMm(extrusion: Extrusion): Double = if (extrusion.plane == ModelPlane.FRONT) extrusion.positionZMm else extrusion.positionZMm + (extrusion.depthMm ?: 0.0)
private fun withFacePosition(extrusion: Extrusion, faceMm: Double, depthMm: Double): Extrusion = extrusion.copy(positionZMm = if (extrusion.plane == ModelPlane.FRONT) faceMm else faceMm - depthMm)

private data class StlShape(val contour: List<Pt>, val depth: Double, val face: Double, val color: Int, val edges: List<GuideLine>, val extrusion: Extrusion)
/** Builds every plane's solids. With [strict] off, shapes that can't be built yet are skipped instead of reported. */
private fun buildSolidSpecs(guides: List<GuideLine>, extrusions: List<Extrusion>, setup: ModelSetup, objectsOut: MutableList<Extrusion>? = null, strict: Boolean = true): List<SolidSpec> {
    val issues = mutableListOf<MeshIssue>()
    val specs = mutableListOf<SolidSpec>()
    var count = 0; var solidCount = 0
    ModelPlane.entries.forEach { plane ->
        val planeGuides = guides.filter { it.plane == plane }
        val planeExtrusions = extrusions.filter { it.plane == plane }
        val shapes = drawableGuideShapes(planeGuides)
        val mapping = setup.mapping(plane)
        if (mapping == null) { if (shapes.isNotEmpty()) issues += MeshIssue("${plane.label} drawing", "needs a calibration length"); return@forEach }
        val models = shapes.mapNotNull { shape ->
            val name = "${if (plane == ModelPlane.FRONT) "" else "${plane.label} "}Shape ${++count}"
            val extrusion = planeExtrusions.firstOrNull { ex -> extrusionGuides(ex, planeGuides).any { g -> shape.any { it.id == g.id } } }
            // A flat shape (no depth) is simply not part of the 3D model; nor is a hole (it is cut out of its shape).
            if (extrusion == null || shape.all { it.hole }) return@mapNotNull null
            solidCount++
            val depth = extrusion.depthMm?.takeIf { it.isFinite() && it > 0 }
            if (depth == null) { issues += MeshIssue(name, "needs a positive depth"); return@mapNotNull null }
            val path = shapePath(shape)
            attachedRegions(shape, planeGuides).forEach { region ->
                val part = android.graphics.Path().apply {
                    region.points.firstOrNull()?.let { moveTo(it.x, it.y); region.points.drop(1).forEach { p -> lineTo(p.x, p.y) }; close() }
                }
                if (!path.op(part, android.graphics.Path.Op.UNION)) issues += MeshIssue(name, "attached regions could not be combined")
            }
            val contours = editableContours(path)
            if (contours == null) { issues += MeshIssue(name, "has a hole; holes are not supported for 3D export yet"); return@mapNotNull null }
            if (contours.size != 1) { issues += MeshIssue(name, "must have one continuous outline (found ${contours.size})"); return@mapNotNull null }
            val edges = extrusionGuides(extrusion, planeGuides)
            val contour = contours.single()
            val endpoints = edges.flatMap { listOf(it.start, it.end) }
            val enriched = contour.indices.flatMap { i ->
                val a = contour[i]; val b = contour[(i + 1) % contour.size]
                val dx = (b.x - a.x).toDouble(); val dy = (b.y - a.y).toDouble(); val length2 = dx * dx + dy * dy
                val interior = if (length2 <= 1e-10) emptyList() else endpoints.mapNotNull { p ->
                    val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / length2
                    if (t <= 0.000001 || t >= 0.999999) null else {
                        val projected = Pt((a.x + t * dx).toFloat(), (a.y + t * dy).toFloat())
                        if (distance(projected, p) < 0.01) t to p else null
                    }
                }.sortedBy { it.first }.fold(mutableListOf<Pair<Double, Pt>>()) { acc, point -> if (acc.none { distance(it.second, point.second) < 0.01 }) acc.add(point); acc }
                listOf(a) + interior.map { it.second }
            }
            name to StlShape(enriched, depth, facePositionMm(extrusion), guideColor(shape.first().colorIndex).toArgb(), edges, extrusion)
        }
        models.forEach { (name, model) ->
            val outline = model.contour.map { p ->
                val edge = model.edges.minByOrNull { distance(p, guidePointAt(it, guideNearestParameter(it, p))) } ?: error("Missing shape boundary")
                val t = guideNearestParameter(edge, p).toDouble()
                val pointZ = edge.startZMm * (1 - t) + edge.endZMm * t
                val thickness = (edge.startThicknessMm ?: model.depth) * (1 - t) + (edge.endThicknessMm ?: model.depth) * t
                OutlinePoint(mapping.right(p), mapping.up(p), model.face + pointZ, thickness)
            }
            specs += SolidSpec(name, simplifiedOutline(outline), model.color, plane)
            objectsOut?.add(model.extrusion.copy(plane = plane))
        }
    }
    if (!strict) return specs
    if (solidCount == 0) error("Nothing is 3D yet: select a shape, then Modify → Grow, or give it a sheet material")
    if (issues.isNotEmpty()) throw MeshException(issues)
    return specs
}

/**
 * Bars and pipes for lines that have a material: one mesh per line, following curves in short straight pieces.
 * Keys are (plane, -(line id + 1)) so they never clash with shape objects.
 */
private fun buildMembers(guides: List<GuideLine>, materials: Map<String, Material>, setup: ModelSetup, issues: MutableList<MeshIssue>? = null): List<Pair<Pair<ModelPlane, Int>, Mesh>> {
    val counts = HashMap<String, Int>()
    return guides.flatMap { guide ->
        // Sheets belong to closed shapes; only bars and pipes follow a line.
        val material = guide.materialId?.let(materials::get)?.takeUnless { it.isSheet } ?: return@flatMap emptyList()
        val mapping = setup.mapping(guide.plane) ?: return@flatMap emptyList()
        val name = "${material.name} ${counts.merge(material.id, 1, Int::plus)}"
        val steps = if (guide.curveMid == null) 1 else 12
        val points = (0..steps).map { i -> val t = i.toFloat() / steps; mapping.model(guidePointAt(guide, t), guide.startZMm + (guide.endZMm - guide.startZMm) * t) }
        // Curves are straight pieces, each a separate solid. Merging them was far too slow on a phone, so each piece is
        // lengthened past the bends instead, which closes the gaps on the outside of a bend.
        val half = max(material.profile.widthMm, material.profile.heightMm) / 2
        val extend = DoubleArray(points.size)
        for (j in 1 until points.lastIndex) {
            val d1 = (points[j] - points[j - 1]).normalized(); val d2 = (points[j + 1] - points[j]).normalized()
            val turn = kotlin.math.acos((d1 dot d2).coerceIn(-1.0, 1.0))
            extend[j] = half * kotlin.math.tan(turn / 2).coerceAtMost(3.0)
        }
        try {
            points.indices.toList().zipWithNext().map { (i, k) ->
                val dir = (points[k] - points[i]).normalized()
                (guide.plane to -(guide.id + 1)) to MemberBuilder.build(name, points[i] - dir * extend[i], points[k] + dir * extend[k], guide.plane.toward, material.profile, material.argb, guide.materialTurnDeg)
            }
        } catch (e: MeshException) { issues?.addAll(e.issues); emptyList() }
    }
}

/** The finished model: one mesh per remaining object, after cuts and joins, with what to warn about. */
private class ModelBuild(val meshes: List<Mesh>, val objects: List<Pair<ModelPlane, Int>>, val warnings: List<MeshIssue>, val finishes: List<Finish> = emptyList())

/**
 * Builds every plane's solids and applies the 3D cuts and joins. With [strict], anything that would make a bad
 * print (including objects that overlap without being joined) throws; otherwise overlaps come back as warnings.
 */
private fun buildModel(guides: List<GuideLine>, extrusions: List<Extrusion>, ops: List<SolidOp>, setup: ModelSetup, strict: Boolean, materials: Map<String, Material> = emptyMap()): ModelBuild {
    val objects = mutableListOf<Extrusion>()
    val members = buildMembers(guides, materials, setup)
    // A drawing made only of bars and pipes needs no closed shape.
    val specs = if (members.isNotEmpty()) buildSolidSpecs(guides, extrusions, setup, objects, strict = false) else buildSolidSpecs(guides, extrusions, setup, objects)
    val issues = mutableListOf<MeshIssue>()
    // Holes (shapes cut out of a shape and lying wholly inside it) are left open right through the object.
    val raw = specs.map { spec ->
        val mapping = setup.mapping(spec.plane)
        val outer = spec.outline.map { Pt(it.x.toFloat(), it.y.toFloat()) }
        val holes = if (mapping == null) emptyList() else closedGuideShapes(guides.filter { it.plane == spec.plane }).filter { s -> s.all { it.hole } }
            .map { hole -> shapePoints(hole).map { Pt(mapping.right(it).toFloat(), mapping.up(it).toFloat()) } }
            .filter { pts -> pts.size >= 3 && pts.all { pointInsidePolygon(it, outer) } }
            .map { pts -> pts.map { Vec2(it.x.toDouble(), it.y.toDouble()) } }
        try { if (holes.isEmpty()) MeshBuilder.extrude(spec) else MeshBuilder.extrudeWithHoles(spec, holes) } catch (e: MeshException) { issues += e.issues; null }
    }
    if (issues.any { it.blocking }) throw MeshException(issues)
    val keys = objects.map { it.plane to it.id }
    val operations = ops.mapNotNull { op -> val target = keys.indexOf(op.target); val tool = keys.indexOf(op.tool); if (target < 0 || tool < 0 || target == tool) null else SolidOperation(target, tool, op.join, op.keepTool) }
    val combined = SolidCombiner.apply(raw.map { it!! }, operations)
    combined.forEach { mesh -> if (mesh != null) issues += MeshBuilder.validate(mesh) }
    if (issues.any { it.blocking }) throw MeshException(issues)
    val removed = combined.indices.filter { combined[it] == null }
    val ignore = operations.map { it.target to it.tool }.toSet() + removed.flatMap { r -> specs.indices.map { r to it } }
    val overlaps = MeshBuilder.overlapIssues(specs, ignore)
    if (strict && overlaps.any { it.blocking }) throw MeshException(overlaps)
    val kept = combined.indices.filter { combined[it] != null }
    // A shape made of a sheet takes the sheet's color and look.
    fun sheetOf(i: Int): Material? = objects[i].let { e -> guides.filter { it.plane == e.plane && it.id in e.guideIds }.firstNotNullOfOrNull { g -> g.materialId?.let(materials::get)?.takeIf { it.isSheet } } }
    val shapeMeshes = kept.map { i -> val mesh = combined[i]!!; sheetOf(i)?.let { mesh.copy(argb = it.argb) } ?: mesh }
    val memberFinishes = guides.filter { g -> g.materialId?.let(materials::get)?.isSheet == false }.associate { (it.plane to -(it.id + 1)) to materials.getValue(it.materialId!!).finish }
    return ModelBuild(shapeMeshes + members.map { it.second }, kept.map { keys[it] } + members.map { it.first }, issues + overlaps,
        kept.map { sheetOf(it)?.finish ?: Finish.PLAIN } + members.map { memberFinishes[it.first] ?: Finish.PLAIN })
}

private fun buildMeshes(guides: List<GuideLine>, extrusions: List<Extrusion>, ops: List<SolidOp>, setup: ModelSetup, materials: Map<String, Material> = emptyMap(), imported: List<Mesh> = emptyList()): List<Mesh> {
    val drawn = try { buildModel(guides, extrusions, ops, setup, strict = true, materials = materials).meshes }
        catch (e: IllegalStateException) { if (e is MeshException || imported.isEmpty()) throw e; emptyList() }
    val meshes = drawn + imported
    // Printable files start at X = 0 and Y = 0; Z keeps the drawing plane at 0.
    val minX = meshes.minOf { m -> m.vertices.minOf { it.x } }; val minY = meshes.minOf { m -> m.vertices.minOf { it.y } }
    return meshes.map { m -> m.copy(vertices = m.vertices.map { Vec3(it.x - minX, it.y - minY, it.z) }) }
}

private fun shapePath(shape: List<GuideLine>): android.graphics.Path = android.graphics.Path().apply {
    moveTo(shape.first().start.x, shape.first().start.y)
    shape.forEach { guide ->
        val mid = guide.curveMid
        if (mid == null) lineTo(guide.end.x, guide.end.y)
        else quadTo(2f * mid.x - (guide.start.x + guide.end.x) / 2f, 2f * mid.y - (guide.start.y + guide.end.y) / 2f, guide.end.x, guide.end.y)
    }
    close()
}

private fun pointInsidePolygon(point: Pt, polygon: List<Pt>): Boolean {
    var inside = false
    polygon.indices.forEach { i ->
        val a = polygon[i]; val b = polygon[(i + 1) % polygon.size]
        if ((a.y > point.y) != (b.y > point.y) && point.x < (b.x - a.x) * (point.y - a.y) / (b.y - a.y) + a.x) inside = !inside
    }
    return inside
}

// The guide model stores closed contours, so reject holes instead of silently filling them.
private fun editableContours(path: android.graphics.Path): List<List<Pt>>? {
    val data = path.approximate(0.35f)
    val contours = mutableListOf<MutableList<Pt>>()
    var previousFraction = -1f
    for (i in data.indices step 3) {
        val fraction = data[i]; val p = Pt(data[i + 1], data[i + 2])
        if (contours.isEmpty() || fraction == previousFraction && distance(contours.last().last(), p) > 0.6) contours.add(mutableListOf())
        if (contours.last().lastOrNull()?.let { distance(it, p) > 0.1 } != false) contours.last().add(p)
        previousFraction = fraction
    }
    val result = contours.mapNotNull { contour ->
        if (contour.size > 1 && distance(contour.first(), contour.last()) < 0.6) contour.removeAt(contour.lastIndex)
        contour.takeIf { it.size >= 3 }
    }
    if (result.any { a -> result.any { b -> a !== b && pointInsidePolygon(a.first(), b) } }) return null
    return result
}

private fun shapeBoolean(first: List<GuideLine>, second: List<GuideLine>, op: android.graphics.Path.Op): List<List<Pt>>? {
    val result = android.graphics.Path()
    if (!result.op(shapePath(first), shapePath(second), op)) return null
    return editableContours(result)
}

private fun cutShapeByLine(shape: List<GuideLine>, cutter: GuideLine, width: Int, height: Int): List<List<Pt>>? {
    if (cutter.curveMid != null) return null
    val crossings = shape.flatMap { guideIntersections(it, cutter) }.fold(mutableListOf<Pt>()) { acc, p -> if (acc.none { distance(it, p) < 0.6 }) acc.add(p); acc }
    if (crossings.size < 2) return null
    val dx = cutter.end.x - cutter.start.x; val dy = cutter.end.y - cutter.start.y
    val length = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (length < 0.001f) return null
    val ux = dx / length; val uy = dy / length; val extent = max(width, height).toFloat() * 10f
    val a = Pt(cutter.start.x - ux * extent, cutter.start.y - uy * extent)
    val b = Pt(cutter.end.x + ux * extent, cutter.end.y + uy * extent)
    fun half(side: Float) = android.graphics.Path().apply {
        moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(b.x - uy * extent * side, b.y + ux * extent * side); lineTo(a.x - uy * extent * side, a.y + ux * extent * side); close()
    }
    val results = listOf(1f, -1f).flatMap { side ->
        val result = android.graphics.Path()
        if (!result.op(shapePath(shape), half(side), android.graphics.Path.Op.INTERSECT)) return null
        editableContours(result) ?: return null
    }
    return results.takeIf { it.size >= 2 }
}

// A curve and a straight chord can form a closed face even in older projects
// where the two guides were saved as separate paths.
private fun drawableGuideShapes(guides: List<GuideLine>): List<List<GuideLine>> {
    val shapes = closedGuideShapes(guides).toMutableList()
    val used = shapes.flatten().map { it.id }.toSet()
    val remaining = guides.filter { it.id !in used }
    remaining.forEachIndexed { index, first ->
        remaining.drop(index + 1).forEach { second ->
            if ((first.curveMid != null || second.curveMid != null) && sameGuideObject(first, second)) {
                val opposite = distance(first.end, second.start) < 0.6 && distance(first.start, second.end) < 0.6
                val same = distance(first.start, second.start) < 0.6 && distance(first.end, second.end) < 0.6
                if (opposite || same) shapes.add(listOf(first, if (same) second.copy(start = second.end, end = second.start, startZMm = second.endZMm, endZMm = second.startZMm, startThicknessMm = second.endThicknessMm, endThicknessMm = second.startThicknessMm) else second))
            }
        }
    }
    return shapes
}

private data class AttachedRegion(val points: List<Pt>, val separatorIds: Set<Int>)

private fun attachedRegions(shape: List<GuideLine>, guides: List<GuideLine>): List<AttachedRegion> {
    val group = shape.first().groupId ?: return emptyList()
    val shapes = closedGuideShapes(guides)
    val closedIds = shapes.flatten().map { it.id }.toSet()
    val chains = guides.filter { it.groupId == group && it.id !in closedIds }
        .groupBy { it.pathId?.let { id -> "p$id" } ?: "i${it.id}" }.values
    return chains.mapNotNull { chain ->
        if (chain.isEmpty() || chain.zipWithNext().any { (a, b) -> distance(a.end, b.start) >= 0.6 }) return@mapNotNull null
        val startIndex = shape.indices.minByOrNull { distance(shape[it].start, chain.first().start) } ?: return@mapNotNull null
        val endIndex = shape.indices.minByOrNull { distance(shape[it].start, chain.last().end) } ?: return@mapNotNull null
        if (startIndex == endIndex || distance(shape[startIndex].start, chain.first().start) >= 0.6 || distance(shape[endIndex].start, chain.last().end) >= 0.6) return@mapNotNull null
        fun arc(from: Int, to: Int): List<GuideLine> {
            val result = mutableListOf<GuideLine>(); var index = from
            while (index != to && result.size < shape.size) { result.add(shape[index]); index = (index + 1) % shape.size }
            return result
        }
        fun arcLength(path: List<GuideLine>) = path.sumOf { edge -> guidePoints(edge).zipWithNext().sumOf { (a, b) -> distance(a, b) } }
        val forward = arc(endIndex, startIndex)
        val backward = arc(startIndex, endIndex)
        val useForward = arcLength(forward) <= arcLength(backward)
        val border = if (useForward) forward else backward
        val borderPoints = if (useForward) border.flatMap { guidePoints(it).dropLast(1) } else border.asReversed().flatMap { guidePoints(it).asReversed().dropLast(1) }
        AttachedRegion(chain.flatMap { guidePoints(it).dropLast(1) } + listOf(chain.last().end) + borderPoints.drop(1), border.map { it.id }.toSet())
    }
}

private fun sameGuideObject(a: GuideLine, b: GuideLine): Boolean =
    a.id == b.id || a.groupId != null && a.groupId == b.groupId || a.pathId != null && a.pathId == b.pathId

private fun extrusionGuides(extrusion: Extrusion, guides: List<GuideLine>): List<GuideLine> {
    val seeds = guides.filter { it.id in extrusion.guideIds }
    return guides.filter { guide -> seeds.any { sameGuideObject(it, guide) } }
}

private fun guidePointAt(guide: GuideLine, t: Float): Pt {
    val mid = guide.curveMid ?: return Pt(guide.start.x + (guide.end.x - guide.start.x) * t, guide.start.y + (guide.end.y - guide.start.y) * t)
    val s = 1f - t
    val cx = 2f * mid.x - (guide.start.x + guide.end.x) / 2f
    val cy = 2f * mid.y - (guide.start.y + guide.end.y) / 2f
    return Pt(s * s * guide.start.x + 2f * s * t * cx + t * t * guide.end.x, s * s * guide.start.y + 2f * s * t * cy + t * t * guide.end.y)
}

private fun guideNearestParameter(guide: GuideLine, point: Pt): Float {
    if (distance(point, guide.start) < 0.001) return 0f
    if (distance(point, guide.end) < 0.001) return 1f
    if (guide.curveMid == null) {
        val dx = guide.end.x - guide.start.x; val dy = guide.end.y - guide.start.y
        val length = dx * dx + dy * dy
        return if (length < 0.000001f) 0f else (((point.x - guide.start.x) * dx + (point.y - guide.start.y) * dy) / length).coerceIn(0f, 1f)
    }
    var best = 0f; var bestDistance = Double.POSITIVE_INFINITY
    for (i in 0..128) { val t = i / 128f; val d = distance(point, guidePointAt(guide, t)); if (d < bestDistance) { best = t; bestDistance = d } }
    var lo = (best - 1f / 128f).coerceAtLeast(0f); var hi = (best + 1f / 128f).coerceAtMost(1f)
    repeat(16) { val a = lo + (hi - lo) / 3f; val b = hi - (hi - lo) / 3f; if (distance(point, guidePointAt(guide, a)) < distance(point, guidePointAt(guide, b))) hi = b else lo = a }
    return (lo + hi) / 2f
}

private data class ShapeAttachment(val borderId: Int, val parameter: Float, val point: Pt)
private fun shapeAttachment(shape: List<GuideLine>, point: Pt): ShapeAttachment? = shape.map { guide ->
    val nearest = guideNearestParameter(guide, point)
    val t = if (nearest < 0.0001f) 0f else if (nearest > 0.9999f) 1f else nearest
    ShapeAttachment(guide.id, t, guidePointAt(guide, t))
}.minByOrNull { distance(point, it.point) }

private fun extrusionFaces(extrusion: Extrusion, guides: List<GuideLine>, width: Int, height: Int): List<ExtrusionFace> {
    val depth = extrusionDepth(extrusion, width, height)
    if (depth < 0.001f) return emptyList()
    val sourceGuides = extrusionGuides(extrusion, guides)
    val vertices = sourceGuides.flatMap { guidePoints(it) }
    if (vertices.isEmpty()) return emptyList()
    val centerX = vertices.map { it.x.toDouble() }.average().toFloat()
    val centerY = vertices.map { it.y.toDouble() }.average().toFloat()
    val winding = closedGuideShapes(guides).associate { shape ->
        val points = shapePoints(shape)
        val area = points.indices.sumOf { index -> val a = points[index]; val b = points[(index + 1) % points.size]; (a.x * b.y - b.x * a.y).toDouble() }
        shape.first().pathId to if (area >= 0.0) -1f else 1f
    }
    return sourceGuides.flatMap { guide ->
        val points = guidePoints(guide)
        points.zipWithNext().map { (a, b) ->
            val dx = b.x - a.x; val dy = b.y - a.y
            val mx = (a.x + b.x) / 2f; val my = (a.y + b.y) / 2f
            val lx = extrusion.sun.x - mx; val ly = -my
            val outward = winding[guide.pathId] ?: if ((-dy * (mx - centerX) + dx * (my - centerY)) < 0f) -1f else 1f
            val length = hypot(dx.toDouble(), dy.toDouble()) * hypot(lx.toDouble(), ly.toDouble())
            val light = if (length > 0.0001) (outward * (-dy * lx + dx * ly) / length).toFloat().coerceIn(-1f, 1f) else 0f
            ExtrusionFace(listOf(a, b, extrusionProject(b, extrusion, depth), extrusionProject(a, extrusion, depth)), guide, 0.10f, light)
        }
    }
}

private fun extrusionEdges(extrusion: Extrusion, guides: List<GuideLine>, width: Int, height: Int): List<Triple<Pt, Pt, GuideLine>> {
    val depth = extrusionDepth(extrusion, width, height)
    if (depth < 0.001f) return emptyList()
    fun projected(point: Pt) = extrusionProject(point, extrusion, depth)
    val edges = mutableListOf<Triple<Pt, Pt, GuideLine>>()
    val seen = mutableListOf<Pt>()
    extrusionGuides(extrusion, guides).forEach { guide ->
        guidePoints(guide).zipWithNext().forEach { (start, end) -> edges.add(Triple(projected(start), projected(end), guide)) }
        listOf(guide.start, guide.end).forEach { point ->
            if (seen.none { distance(it, point) < 0.6 }) { seen.add(point); edges.add(Triple(point, projected(point), guide)) }
        }
    }
    return edges
}

private fun projectToGuide(point: Pt, guide: GuideLine): Pt {
    guide.curveMid?.let { mid ->
        val cx = 2f * mid.x - (guide.start.x + guide.end.x) / 2f
        val cy = 2f * mid.y - (guide.start.y + guide.end.y) / 2f
        var best = guide.start
        var bestDistance = distance(point, best)
        var previous = guide.start
        for (i in 1..48) {
            val t = i / 48f; val s = 1f - t
            val next = Pt(s * s * guide.start.x + 2f * s * t * cx + t * t * guide.end.x, s * s * guide.start.y + 2f * s * t * cy + t * t * guide.end.y)
            val candidate = projectToGuide(point, guide.copy(start = previous, end = next, curveMid = null))
            val d = distance(point, candidate)
            if (d < bestDistance) { best = candidate; bestDistance = d }
            previous = next
        }
        return best
    }
    val dx = guide.end.x - guide.start.x; val dy = guide.end.y - guide.start.y
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared <= 0f) 0f else (((point.x - guide.start.x) * dx + (point.y - guide.start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return Pt(guide.start.x + dx * t, guide.start.y + dy * t)
}

private fun closestGuide(point: Pt, guides: List<GuideLine>, radius: Double): GuideLine? =
    guides.minByOrNull { distance(point, projectToGuide(point, it)) }?.takeIf { distance(point, projectToGuide(point, it)) <= radius }

/**
 * A shape's outline with points that add nothing removed (Douglas–Peucker, about 0.2% of its size): curves keep
 * their look in a few dozen points, so 3D cuts and joins stay quick. Corners always stay.
 */
private fun simplifiedOutline(outline: List<OutlinePoint>): List<OutlinePoint> {
    if (outline.size <= 24) return outline
    val size = max(outline.maxOf { it.x } - outline.minOf { it.x }, outline.maxOf { it.y } - outline.minOf { it.y })
    val tolerance = max(0.2, size * 0.002)
    fun distance(p: OutlinePoint, a: OutlinePoint, b: OutlinePoint): Double {
        val dx = b.x - a.x; val dy = b.y - a.y; val l2 = dx * dx + dy * dy
        if (l2 < 1e-12) return hypot(p.x - a.x, p.y - a.y)
        val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0.0, 1.0)
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }
    val keep = BooleanArray(outline.size)
    fun simplify(from: Int, to: Int) {
        if (to <= from + 1) return
        var worst = -1; var most = 0.0
        for (i in from + 1 until to) { val d = distance(outline[i], outline[from], outline[to % outline.size]); if (d > most) { most = d; worst = i } }
        // Depth or thickness changing along the outline also counts as a corner.
        val varies = (from + 1 until to).any { i -> abs(outline[i].z - outline[from].z) > 0.01 || abs(outline[i].thicknessMm - outline[from].thicknessMm) > 0.01 }
        if (most > tolerance || varies) { if (worst < 0) worst = (from + to) / 2; keep[worst] = true; simplify(from, worst); simplify(worst, to) }
    }
    // Split the closed loop at its first point and the point farthest from it.
    val far = outline.indices.maxBy { hypot(outline[it].x - outline[0].x, outline[it].y - outline[0].y) }
    keep[0] = true; keep[far] = true
    simplify(0, far); simplify(far, outline.size)
    val result = outline.filterIndexed { i, _ -> keep[i] }
    return if (result.size >= 3) result else outline
}

/** The closed shape (not a hole) that the hole [hole] lies inside, as one of its lines. */
private fun holeContainer(hole: GuideLine, all: List<GuideLine>): GuideLine? {
    val shapes = closedGuideShapes(all)
    val pts = shapes.firstOrNull { p -> p.any { it.id == hole.id } }?.let(::shapePoints) ?: return null
    return shapes.filter { p -> p.none { it.hole } && pts.all { pointInsidePolygon(it, shapePoints(p)) } }.minByOrNull { p -> shapePoints(p).let { s -> abs(s.indices.sumOf { i -> val a = s[i]; val b = s[(i + 1) % s.size]; (a.x * b.y - b.x * a.y).toDouble() }) } }?.first()
}
/** The holes lying inside the closed shape that [lines] belong to. */
private fun holesInside(lines: List<GuideLine>, all: List<GuideLine>): List<GuideLine> {
    val shapes = closedGuideShapes(all)
    val outer = shapes.firstOrNull { p -> p.none { it.hole } && p.any { g -> lines.any { it.id == g.id } } } ?: return emptyList()
    val pts = shapePoints(outer)
    return shapes.filter { p -> p.all { it.hole } && shapePoints(p).all { pointInsidePolygon(it, pts) } }.flatten()
}

/** A circle or oval: one closed path of six or more curved pieces (drawn with the Circle tool). */
private fun isCircleShape(pieces: List<GuideLine>) = pieces.size >= 6 && pieces.all { it.curveMid != null && it.pathId != null && it.pathId == pieces.first().pathId }
/** The circle or oval [guide] belongs to, in its own view's canvas, or null. */
private fun circleOf(guide: GuideLine, guides: List<GuideLine>): List<GuideLine>? = guide.pathId?.let { p -> guides.filter { it.pathId == p && it.plane == guide.plane } }?.takeIf(::isCircleShape)

/** Lines whose end points can be picked: all but a circle's pieces, whose joints are hidden (the circle is one thing). */
private fun pointGuides(guides: List<GuideLine>): List<GuideLine> {
    val smooth = smoothOutlineIds(guides)
    return if (smooth.isEmpty()) guides else guides.filterNot { it.id in smooth }
}
/**
 * Lines of smooth outlines, whose joints aren't points to pick or mark: circles and ovals, outlines of many small
 * pieces (a cut that followed a curve), and holes.
 */
private fun smoothOutlineIds(guides: List<GuideLine>): Set<Int> =
    guides.filter { it.pathId != null }.groupBy { it.plane to it.pathId }.values
        .filter { p -> p.size >= 6 && p.all { it.curveMid != null } || p.size >= 24 || p.all { it.hole } }.flatten().map { it.id }.toSet() +
        guides.filter { it.hole }.map { it.id }
private fun closestGuideEndpoint(point: Pt, guides: List<GuideLine>, radius: Double): GuideEndpoint? =
    guides.flatMap { guide -> listOf(GuideEndpoint(guide.id, true) to guide.start, GuideEndpoint(guide.id, false) to guide.end) }
        .minByOrNull { distance(point, it.second) }
        ?.takeIf { distance(point, it.second) <= radius }
        ?.first

private fun segmentIntersection(a: Pt, b: Pt, c: Pt, d: Pt): Pt? {
    val rx = b.x - a.x; val ry = b.y - a.y
    val sx = d.x - c.x; val sy = d.y - c.y
    val cross = rx * sy - ry * sx
    if (abs(cross) < 0.001f) return null
    val qpx = c.x - a.x; val qpy = c.y - a.y
    val t = (qpx * sy - qpy * sx) / cross
    val u = (qpx * ry - qpy * rx) / cross
    return if (t in 0f..1f && u in 0f..1f) Pt(a.x + t * rx, a.y + t * ry) else null
}

private fun guideIntersections(first: GuideLine, second: GuideLine): List<Pt> =
    guidePoints(first).zipWithNext().flatMap { (a, b) ->
        guidePoints(second).zipWithNext().mapNotNull { (c, d) -> segmentIntersection(a, b, c, d) }
    }

private fun findJunction(point: Pt, radius: Double, lines: List<MeasureLine>, guides: List<GuideLine>, tool: Tool): JunctionAction? {
    val candidates = mutableListOf<JunctionAction>()
    val includeMeasurements = tool != Tool.CROP
    val includeGuides = tool != Tool.CROP
    if (includeGuides) {
        val closedShapes = closedGuideShapes(guides)
        val containing = closedShapes.filter { pointInsidePolygon(point, shapePoints(it)) }
        if (containing.size >= 2) return JunctionAction(JunctionKind.SHAPE_OVERLAP, containing[0].first().id, containing[1].first().id, point)
        if (containing.size == 1) {
            val shape = containing.first()
            val boundaryIds = closedShapes.flatten().map { it.id }.toSet()
            guides.filter { it.id !in boundaryIds && it.curveMid == null && !sameGuideObject(it, shape.first()) && distance(point, projectToGuide(point, it)) <= radius }
                .minByOrNull { distance(point, projectToGuide(point, it)) }
                ?.takeIf { cutter -> shape.flatMap { guideIntersections(it, cutter) }.fold(mutableListOf<Pt>()) { acc, p -> if (acc.none { distance(it, p) < 0.6 }) acc.add(p); acc }.size >= 2 }
                ?.let { return JunctionAction(JunctionKind.CUT_SHAPE_LINE, shape.first().id, it.id, projectToGuide(point, it)) }
        }
        guides.filter { it.pathId != null }.groupBy { it.pathId }.values.forEach { path ->
            if (path.size >= 2 && path.zipWithNext().all { (a, b) -> distance(a.end, b.start) < 0.6 }) {
                val a = path.first().start; val b = path.last().end
                if (distance(a, b) >= 0.6 && distance(a, b) <= radius && distance(point, a) <= radius && distance(point, b) <= radius)
                    candidates.add(JunctionAction(JunctionKind.CLOSE_GUIDE_PATH, path.first().id, path.last().id, Pt((a.x + b.x) / 2f, (a.y + b.y) / 2f)))
            }
        }
        val shapes = closedGuideShapes(guides)
        val closedIds = shapes.flatten().map { it.id }.toSet()
        val chains = guides.filter { it.id !in closedIds }.groupBy { it.pathId?.let { id -> "p$id" } ?: "i${it.id}" }.values
        shapes.forEach { shape -> chains.forEach { chain ->
            if (!sameGuideObject(shape.first(), chain.first()) && minOf(distance(point, chain.first().start), distance(point, chain.last().end)) <= radius * 1.2) {
                val start = chain.first().start; val end = chain.last().end
                val a = shapeAttachment(shape, start); val b = shapeAttachment(shape, end)
                val tolerance = (radius * 0.2).coerceAtLeast(0.6)
                if (a != null && b != null && distance(start, a.point) <= tolerance && distance(end, b.point) <= tolerance && distance(a.point, b.point) > 0.6 && minOf(distance(point, a.point), distance(point, b.point)) <= radius) {
                    candidates.add(JunctionAction(JunctionKind.MERGE_SHAPE_GUIDE, shape.first().id, chain.first().id, if (distance(point, a.point) <= distance(point, b.point)) a.point else b.point))
                }
            }
        } }
    }
    fun endpoints(items: List<GuideLine>, kind: JunctionKind) {
        val groups = items.groupBy { it.pathId?.let { id -> "p$id" } ?: "i${it.id}" }.values
        val terminals = groups.flatMap { group -> listOf(group.first().let { it.id to it.start }, group.last().let { it.id to it.end }) }
        terminals.forEachIndexed { index, (firstId, firstPoint) -> terminals.drop(index + 1).forEach { (secondId, secondPoint) ->
            val first = items.first { it.id == firstId }; val second = items.first { it.id == secondId }
            if (firstId != secondId && (first.pathId == null || first.pathId != second.pathId) && distance(firstPoint, secondPoint) <= radius && distance(point, firstPoint) <= radius && distance(point, secondPoint) <= radius) {
                candidates.add(JunctionAction(kind, firstId, secondId, Pt((firstPoint.x + secondPoint.x) / 2f, (firstPoint.y + secondPoint.y) / 2f)))
            }
        } }
    }
    if (includeMeasurements) endpoints(lines.filter { !it.isReference }.sortedBy { it.segmentIndex }.map { GuideLine(it.id, it.start, it.end, true, pathId = it.pathId) }, JunctionKind.JOIN_MEASURE_ENDPOINTS)
    if (includeGuides) endpoints(guides, JunctionKind.JOIN_GUIDE_ENDPOINTS)
    if (includeMeasurements) {
        lines.filter { !it.isReference && it.pathId != null }.groupBy { it.pathId }.values.forEach { path ->
            path.sortedBy { it.segmentIndex }.zipWithNext().forEach { (first, second) ->
                if (distance(first.end, second.start) < 0.6 && distance(point, first.end) <= radius) candidates.add(JunctionAction(JunctionKind.SPLIT_MEASUREMENT, first.id, second.id, first.end))
            }
        }
    }
    if (includeGuides) {
        guides.filter { it.pathId != null }.groupBy { it.pathId }.values.forEach { path ->
            path.zipWithNext().forEach { (first, second) ->
                if (distance(first.end, second.start) < 0.6 && distance(point, first.end) <= radius) candidates.add(JunctionAction(JunctionKind.SPLIT_GUIDE, first.id, second.id, first.end))
            }
        }
    }
    fun crossing(first: GuideLine, second: GuideLine): Pt? {
        val intersection = guideIntersections(first, second).minByOrNull { distance(point, it) }
        if (intersection != null) return intersection
        return listOf(first.start, first.end).firstNotNullOfOrNull { a ->
            listOf(second.start, second.end).firstOrNull { b -> distance(a, b) < 0.6 }?.let { b -> Pt((a.x + b.x) / 2f, (a.y + b.y) / 2f) }
        }
    }
    fun collectCrossings(items: List<GuideLine>, kind: JunctionKind) {
        val nearby = items.filter { distance(point, projectToGuide(point, it)) <= radius }
        nearby.forEachIndexed { index, first -> nearby.drop(index + 1).forEach { second ->
            crossing(first, second)?.takeIf { intersection ->
                distance(point, intersection) <= radius && !(first.pathId != null && first.pathId == second.pathId &&
                    (distance(first.end, second.start) < 0.6 || distance(second.end, first.start) < 0.6) &&
                    (distance(intersection, first.start) < 0.6 || distance(intersection, first.end) < 0.6))
            }?.let { candidates.add(JunctionAction(kind, first.id, second.id, it)) }
        } }
    }
    if (includeMeasurements) collectCrossings(lines.filter { !it.isReference }.map { GuideLine(it.id, it.start, it.end, true, pathId = it.pathId) }, JunctionKind.JOIN_MEASUREMENTS)
    if (includeGuides) collectCrossings(guides, JunctionKind.JOIN_GUIDES)
    return candidates.filter { it.kind == JunctionKind.CLOSE_GUIDE_PATH || it.kind == JunctionKind.MERGE_SHAPE_GUIDE }.minByOrNull { distance(point, it.point) }
        ?: candidates.filter { it.kind == JunctionKind.JOIN_MEASURE_ENDPOINTS || it.kind == JunctionKind.JOIN_GUIDE_ENDPOINTS }.minByOrNull { distance(point, it.point) }
        ?: candidates.minByOrNull { distance(point, it.point) }
}

private fun snapToGuide(point: Pt, guides: List<GuideLine>, radius: Double): Pt? {
    if (guides.isEmpty()) return null
    val endpoint = guides.flatMap { listOf(it.start, it.end) }.minByOrNull { distance(point, it) }
        ?.takeIf { distance(point, it) <= radius }
    if (endpoint != null) return endpoint
    var nearestIntersection: Pt? = null
    var intersectionDistance = radius
    val nearby = guides.filter { distance(point, projectToGuide(point, it)) <= radius * 1.1 }
    for (first in nearby.indices) for (second in first + 1 until nearby.size) {
        guideIntersections(nearby[first], nearby[second]).forEach { intersection ->
            val candidateDistance = distance(point, intersection)
            if (candidateDistance <= intersectionDistance) { nearestIntersection = intersection; intersectionDistance = candidateDistance }
        }
    }
    if (nearestIntersection != null) return nearestIntersection
    val guide = closestGuide(point, guides, radius) ?: return null
    return projectToGuide(point, guide)
}

private data class HoughPeak(val angleIndex: Int, val distanceIndex: Int, val votes: Int)

private fun detectStrongGuideLines(source: Bitmap): List<GuideLine> {
    val largest = max(source.width, source.height).coerceAtLeast(1)
    val workScale = min(1f, 720f / largest)
    val work = if (workScale < 1f) Bitmap.createScaledBitmap(source, (source.width * workScale).roundToInt(), (source.height * workScale).roundToInt(), true) else source
    val width = work.width; val height = work.height
    if (width < 32 || height < 32) return emptyList()
    val pixels = IntArray(width * height)
    work.getPixels(pixels, 0, width, 0, 0, width, height)
    val gray = IntArray(pixels.size) { index ->
        val value = pixels[index]
        ((AndroidColor.red(value) * 30 + AndroidColor.green(value) * 59 + AndroidColor.blue(value) * 11) / 100)
    }
    val angleSteps = 36
    val maxDistance = hypot(width.toDouble(), height.toDouble()).roundToInt()
    val bins = maxDistance * 2 + 1
    val votes = IntArray(angleSteps * bins)
    val edgePoints = ArrayList<Pair<Int, Int>>()
    for (y in 1 until height - 1 step 2) for (x in 1 until width - 1 step 2) {
        val gx = gray[y * width + x + 1] - gray[y * width + x - 1]
        val gy = gray[(y + 1) * width + x] - gray[(y - 1) * width + x]
        if (abs(gx) + abs(gy) < 120) continue
        edgePoints += x to y
        var angle = Math.atan2(gy.toDouble(), gx.toDouble())
        if (angle < 0.0) angle += Math.PI
        if (angle >= Math.PI) angle -= Math.PI
        val angleIndex = (angle / Math.PI * angleSteps).roundToInt().mod(angleSteps)
        val theta = angleIndex * Math.PI / angleSteps
        val distance = (x * cos(theta) + y * sin(theta)).roundToInt() + maxDistance
        if (distance in 0 until bins) votes[angleIndex * bins + distance]++
    }
    val threshold = max(14, min(width, height) / 22)
    val peaks = votes.indices.mapNotNull { index -> votes[index].takeIf { it >= threshold }?.let { HoughPeak(index / bins, index % bins, it) } }
        .sortedByDescending { it.votes }
    val accepted = mutableListOf<Triple<Double, Double, Pair<Pt, Pt>>>()
    peaks.forEach { peak ->
        if (accepted.size >= 36) return@forEach
        val theta = peak.angleIndex * Math.PI / angleSteps
        val normalX = cos(theta); val normalY = sin(theta)
        val distanceFromOrigin = peak.distanceIndex - maxDistance
        if (accepted.any { (angle, distanceValue, _) -> abs(angle - theta) < Math.toRadians(8.0) && abs(distanceValue - distanceFromOrigin) < 12.0 }) return@forEach
        val directionX = -normalY; val directionY = normalX
        var start = Float.POSITIVE_INFINITY; var end = Float.NEGATIVE_INFINITY
        edgePoints.forEach { (x, y) ->
            if (abs(x * normalX + y * normalY - distanceFromOrigin) <= 2.4) {
                val projection = (x * directionX + y * directionY).toFloat()
                start = min(start, projection); end = max(end, projection)
            }
        }
        if (!start.isFinite() || end - start < max(52f, min(width, height) * 0.13f)) return@forEach
        val startPoint = Pt(((normalX * distanceFromOrigin + directionX * start) / workScale).toFloat(), ((normalY * distanceFromOrigin + directionY * start) / workScale).toFloat())
        val endPoint = Pt(((normalX * distanceFromOrigin + directionX * end) / workScale).toFloat(), ((normalY * distanceFromOrigin + directionY * end) / workScale).toFloat())
        accepted += Triple(theta, distanceFromOrigin.toDouble(), startPoint to endPoint)
    }
    return accepted.mapIndexed { index, (_, _, segment) -> GuideLine(index + 1, segment.first, segment.second, manual = false) }
}
private fun calibrationOf(lines: List<MeasureLine>, unit: String): Calibration =
    Calibration.from(lines.filter { it.isReference && it.isScaleReference }.map { line ->
        CalibrationReference((line.end.x - line.start.x).toDouble(), (line.end.y - line.start.y).toDouble(), line.value * unitFactorMm(unit), CalibrationAxis.of(line.axis))
    })

private fun recomputeScale(lines: List<MeasureLine>, unit: String): Double? {
    // Calibration is the source of truth whenever one or more calibration lines exist.
    val calibration = calibrationOf(lines, unit)
    if (calibration.status != CalibrationStatus.Missing) {
        // Contradictory references still yield their average so nothing is lost, but the editor flags them.
        return if (calibration.status is CalibrationStatus.Incomplete) null else calibration.meanPxPerMm?.times(unitFactorMm(unit))
    }
    val manualReference = lines.lastOrNull { !it.isReference && it.isScaleReference && it.value > abs(it.endZMm - it.startZMm) / unitFactorMm(unit) }
    return manualReference?.let { distance(it.start, it.end) / kotlin.math.sqrt(it.value * it.value - Math.pow((it.endZMm - it.startZMm) / unitFactorMm(unit), 2.0)) }
}

/** X and Y pixels per unit when the user explicitly calibrated the axes separately. */
private fun axisScaleOf(lines: List<MeasureLine>, unit: String): Pair<Double, Double>? {
    val calibration = calibrationOf(lines, unit)
    if (!calibration.isPerAxis) return null
    return calibration.pxPerMmX!! * unitFactorMm(unit) to calibration.pxPerMmY!! * unitFactorMm(unit)
}

private fun frontLength(start: Pt, end: Pt, scale: Double, axes: Pair<Double, Double>?): Double =
    if (axes == null) distance(start, end) / scale else hypot((end.x - start.x) / axes.first, (end.y - start.y) / axes.second)

private fun nextId(lines: List<MeasureLine>) = (lines.maxOfOrNull { it.id } ?: 0) + 1
private fun closestEndpoint(point: Pt, lines: List<MeasureLine>, maxDistance: Double): Endpoint? = lines.flatMap { listOf(Endpoint(it.id, true) to it.start, Endpoint(it.id, false) to it.end) }.minByOrNull { distance(point, it.second) }?.takeIf { distance(point, it.second) <= maxDistance }?.first
private fun distanceToLine(point: Pt, line: MeasureLine): Double {
    val dx = line.end.x - line.start.x
    val dy = line.end.y - line.start.y
    val lengthSquared = dx * dx + dy * dy
    val t = if (lengthSquared == 0f) 0f else (((point.x - line.start.x) * dx + (point.y - line.start.y) * dy) / lengthSquared).coerceIn(0f, 1f)
    return hypot((point.x - (line.start.x + t * dx)).toDouble(), (point.y - (line.start.y + t * dy)).toDouble())
}
private fun closestLine(point: Pt, lines: List<MeasureLine>): MeasureLine? = lines.minByOrNull { distanceToLine(point, it) }
private fun labelAnchor(line: MeasureLine): Pt {
    val dx = line.end.x - line.start.x; val dy = line.end.y - line.start.y
    val length = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
    return Pt((line.start.x + line.end.x) / 2f + dx / length * line.labelOffset, (line.start.y + line.end.y) / 2f + dy / length * line.labelOffset)
}
private fun closestLabel(point: Pt, lines: List<MeasureLine>, radius: Double): MeasureLine? = lines.minByOrNull { distance(point, labelAnchor(it)) }
    ?.takeIf { distance(point, labelAnchor(it)) <= radius * 5.5 }
private fun selectableMeasurement(point: Pt, lines: List<MeasureLine>, radius: Double): MeasureLine? = lines.filterNot { it.isReference }
    .minByOrNull { line -> minOf(distanceToLine(point, line), distance(point, Pt((line.start.x + line.end.x) / 2f, (line.start.y + line.end.y) / 2f))) }
    ?.takeIf { line ->
        val midpoint = Pt((line.start.x + line.end.x) / 2f, (line.start.y + line.end.y) / 2f)
        distanceToLine(point, line) <= radius * 2.2 || distance(point, midpoint) <= radius * 3.2
    }
private fun format(value: Double) = DecimalFormat("0.##").format(value)
private fun formatMeasure(value: Double, unit: String): String {
    val places = MeasurementPrecision.places[unit] ?: 2
    return DecimalFormat(if (places == 0) "0" else "0." + "#".repeat(places)).format(value)
}
private fun unitFactorMm(unit: String) = when (unit) { "cm" -> 10.0; "m" -> 1000.0; "in" -> 25.4; "ft" -> 304.8; else -> 1.0 }

private fun isHorizontalCalibration(line: MeasureLine): Boolean =
    line.isReference && kotlin.math.abs(line.end.x - line.start.x) >= kotlin.math.abs(line.end.y - line.start.y)

private fun horizontalCalibrationLevel(lines: List<MeasureLine>): Float? {
    val angles = lines.filter(::isHorizontalCalibration).map { line ->
        var angle = Math.toDegrees(atan2((line.end.y - line.start.y).toDouble(), (line.end.x - line.start.x).toDouble())).toFloat()
        if (angle > 90f) angle -= 180f
        if (angle < -90f) angle += 180f
        angle
    }
    return angles.takeIf { it.isNotEmpty() }
        ?.average()
        ?.toFloat()
        ?.let { (-it).coerceIn(-30f, 30f) }
}

private fun autoCalibrationLevel(lines: List<MeasureLine>): Float? {
    val axisOffsets = lines.filter { it.isReference }.map { line ->
        var angle = Math.toDegrees(atan2((line.end.y - line.start.y).toDouble(), (line.end.x - line.start.x).toDouble())).toFloat()
        while (angle > 90f) angle -= 180f
        while (angle < -90f) angle += 180f
        if (isHorizontalCalibration(line)) angle else if (angle >= 0f) angle - 90f else angle + 90f
    }
    return axisOffsets.takeIf { it.isNotEmpty() }?.average()?.toFloat()?.let { (-it).coerceIn(-30f, 30f) }
}

private data class PerspectiveResult(val bitmap: Bitmap, val lines: List<MeasureLine>, val guides: List<GuideLine>)

private fun perspectiveCorrect(source: Bitmap, lines: List<MeasureLine>, guides: List<GuideLine>): PerspectiveResult? {
    val guideIds = guides.mapIndexed { index, _ -> Int.MIN_VALUE + index }
    val guideAsLines = guides.mapIndexed { index, guide ->
        MeasureLine(guideIds[index], guide.start, guide.end, 0.0, false, "")
    }
    val midpointIds = guides.mapIndexed { index, _ -> Int.MIN_VALUE + guides.size + index }
    val midpointLines = guides.mapIndexedNotNull { index, guide -> guide.curveMid?.let { MeasureLine(midpointIds[index], it, it, 0.0, false, "") } }
    val corrected = perspectiveCorrectLines(source, lines + guideAsLines + midpointLines) ?: return null
    val mappedById = corrected.second.associateBy { it.id }
    val mappedGuides = guides.mapIndexed { index, guide ->
        mappedById[guideIds[index]]?.let { mapped -> guide.copy(start = mapped.start, end = mapped.end, curveMid = mappedById[midpointIds[index]]?.start) } ?: guide
    }
    return PerspectiveResult(corrected.first, corrected.second.filterNot { it.id in guideIds || it.id in midpointIds }, mappedGuides)
}

private fun perspectiveCorrectLines(source: Bitmap, lines: List<MeasureLine>): Pair<Bitmap, List<MeasureLine>>? {
    val references = lines.filter { it.isReference }
    val vertical = references.filter { kotlin.math.abs(it.end.y - it.start.y) > kotlin.math.abs(it.end.x - it.start.x) }
    val horizontal = references.filter { kotlin.math.abs(it.end.x - it.start.x) >= kotlin.math.abs(it.end.y - it.start.y) }
    if (vertical.size >= 2 && horizontal.isNotEmpty()) {
        rectifyCalibrationRectangle(source, lines, vertical, horizontal)?.let { return it }
    }
    // A photo may itself be rotated. The pair of parallel calibration sides can
    // therefore be screen-horizontal with a screen-vertical connecting reference.
    if (horizontal.size >= 2 && vertical.isNotEmpty()) {
        rectifyCalibrationRectangle(source, lines, horizontal, vertical)?.let { return it }
    }
    val candidates = if (vertical.size >= 2) vertical to true else if (horizontal.size >= 2) horizontal to false else return null
    val pair = candidates.first.indices.flatMap { first -> (first + 1 until candidates.first.size).map { second -> candidates.first[first] to candidates.first[second] } }
        .maxByOrNull { (first, second) -> kotlin.math.abs((first.end.x - first.start.x) * (second.end.y - second.start.y) - (first.end.y - first.start.y) * (second.end.x - second.start.x)) }
        ?: return null
    fun intersection(first: MeasureLine, second: MeasureLine): Pt? {
        val dx1 = first.end.x - first.start.x; val dy1 = first.end.y - first.start.y
        val dx2 = second.end.x - second.start.x; val dy2 = second.end.y - second.start.y
        val determinant = dx1 * dy2 - dy1 * dx2
        if (kotlin.math.abs(determinant) < 0.001f) return null
        val rx = second.start.x - first.start.x; val ry = second.start.y - first.start.y
        val t = (rx * dy2 - ry * dx2) / determinant
        return Pt(first.start.x + t * dx1, first.start.y + t * dy1)
    }
    val vanishing = intersection(pair.first, pair.second) ?: return null
    val useVertical = candidates.second
    val denominator = if (useVertical) vanishing.y else vanishing.x
    val dimension = if (useVertical) source.height.toFloat() else source.width.toFloat()
    // A vanishing point inside the image would fold the projective transform.
    if (kotlin.math.abs(denominator) < dimension * 0.2f || denominator in -dimension * 0.2f..dimension * 1.2f) return null
    fun project(x: Float, y: Float): Pt {
        val w = if (useVertical) 1f - y / denominator else 1f - x / denominator
        return Pt(x / w, y / w)
    }
    fun averageDirection(group: List<MeasureLine>): Pt? {
        val vectors = group.map { line ->
            val start = project(line.start.x, line.start.y); val end = project(line.end.x, line.end.y)
            Pt(end.x - start.x, end.y - start.y)
        }.filter { hypot(it.x.toDouble(), it.y.toDouble()) > 0.01 }
        val seed = vectors.firstOrNull() ?: return null
        var totalX = 0f; var totalY = 0f
        vectors.forEach { vector ->
            val sign = if (vector.x * seed.x + vector.y * seed.y < 0f) -1f else 1f
            totalX += vector.x * sign; totalY += vector.y * sign
        }
        return Pt(totalX, totalY)
    }
    // The projective step makes the converging family parallel. The affine step then
    // maps the horizontal and vertical reference directions to perpendicular axes.
    val horizontalDirection = averageDirection(if (useVertical) horizontal else candidates.first)
    val verticalDirection = averageDirection(if (useVertical) candidates.first else vertical)
    val horizontalUnits = (if (useVertical) horizontal else candidates.first).map { it.value }.average().toFloat()
    val verticalUnits = (if (useVertical) candidates.first else vertical).map { it.value }.average().toFloat()
    val axisDeterminant = horizontalDirection?.let { h -> verticalDirection?.let { v -> h.x * v.y - h.y * v.x } }
    fun raw(x: Float, y: Float): Pt {
        val point = project(x, y)
        val h = horizontalDirection; val v = verticalDirection; val determinant = axisDeterminant
        return when {
            h != null && v != null && determinant != null && kotlin.math.abs(determinant) > 0.001f -> {
                // Each rectified axis is scaled by its entered real-world calibration size.
                // This preserves squares and rectangles instead of forcing all references equal.
                Pt((v.y * point.x - v.x * point.y) / determinant * horizontalUnits, (-h.y * point.x + h.x * point.y) / determinant * verticalUnits)
            }
            // With only one parallel family, projective rectification is still valid.
            // Rotate it onto its natural axis without inventing a second-scale ratio.
            useVertical && v != null -> {
                val upright = if (v.y < 0f) Pt(-v.x, -v.y) else v
                val theta = Math.PI / 2.0 - atan2(upright.y.toDouble(), upright.x.toDouble())
                val c = cos(theta).toFloat(); val s = sin(theta).toFloat()
                Pt(c * point.x - s * point.y, s * point.x + c * point.y)
            }
            !useVertical && h != null -> {
                val rightward = if (h.x < 0f) Pt(-h.x, -h.y) else h
                val theta = -atan2(rightward.y.toDouble(), rightward.x.toDouble())
                val c = cos(theta).toFloat(); val s = sin(theta).toFloat()
                Pt(c * point.x - s * point.y, s * point.x + c * point.y)
            }
            else -> point
        }
    }
    val corners = arrayOf(Pt(0f, 0f), Pt(source.width.toFloat(), 0f), Pt(source.width.toFloat(), source.height.toFloat()), Pt(0f, source.height.toFloat()))
    val warpedCorners = corners.map { raw(it.x, it.y) }
    val warpedMinX = warpedCorners.minOf { it.x }; val warpedMaxX = warpedCorners.maxOf { it.x }
    val warpedMinY = warpedCorners.minOf { it.y }; val warpedMaxY = warpedCorners.maxOf { it.y }
    val fit = min(source.width / (warpedMaxX - warpedMinX), source.height / (warpedMaxY - warpedMinY)) * 0.94f
    val offsetX = (source.width - (warpedMaxX - warpedMinX) * fit) / 2f
    val offsetY = (source.height - (warpedMaxY - warpedMinY) * fit) / 2f
    val sourcePoints = floatArrayOf(0f, 0f, source.width.toFloat(), 0f, source.width.toFloat(), source.height.toFloat(), 0f, source.height.toFloat())
    val targetPoints = warpedCorners.flatMap { point -> listOf((point.x - warpedMinX) * fit + offsetX, (point.y - warpedMinY) * fit + offsetY) }.toFloatArray()
    val matrix = AndroidMatrix().apply { setPolyToPoly(sourcePoints, 0, targetPoints, 0, 4) }
    // Fit the complete transformed source image, not only the calibrated rectangle.
    val imageCorners = floatArrayOf(0f, 0f, source.width.toFloat(), 0f, source.width.toFloat(), source.height.toFloat(), 0f, source.height.toFloat())
    matrix.mapPoints(imageCorners)
    val xs = imageCorners.filterIndexed { index, _ -> index % 2 == 0 }
    val ys = imageCorners.filterIndexed { index, _ -> index % 2 == 1 }
    val minX = xs.min(); val maxX = xs.max(); val minY = ys.min(); val maxY = ys.max()
    val fitAll = min(source.width / (maxX - minX), source.height / (maxY - minY)) * 0.94f
    val shiftX = (source.width - (maxX - minX) * fitAll) / 2f - minX * fitAll
    val shiftY = (source.height - (maxY - minY) * fitAll) / 2f - minY * fitAll
    val fittedTargets = targetPoints.mapIndexed { index, value -> if (index % 2 == 0) value * fitAll + shiftX else value * fitAll + shiftY }.toFloatArray()
    matrix.setPolyToPoly(sourcePoints, 0, fittedTargets, 0, 4)
    val corrected = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    AndroidCanvas(corrected).apply { fillWithCornerTiles(source); drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) }
    fun map(point: Pt): Pt {
        val values = floatArrayOf(point.x, point.y)
        matrix.mapPoints(values)
        return Pt(values[0], values[1])
    }
    return corrected to lines.map { line -> line.copy(start = map(line.start), end = map(line.end)) }
}

private fun rectifyCalibrationRectangle(source: Bitmap, lines: List<MeasureLine>, vertical: List<MeasureLine>, horizontal: List<MeasureLine>): Pair<Bitmap, List<MeasureLine>>? {
    val sides = vertical.indices.flatMap { first -> (first + 1 until vertical.size).map { second -> vertical[first] to vertical[second] } }
        .maxByOrNull { (first, second) ->
            val firstMid = Pt((first.start.x + first.end.x) / 2f, (first.start.y + first.end.y) / 2f)
            val secondMid = Pt((second.start.x + second.end.x) / 2f, (second.start.y + second.end.y) / 2f)
            distance(firstMid, secondMid)
        } ?: return null
    val (firstSide, secondSide) = sides
    fun nearest(point: Pt, line: MeasureLine): Pt = listOf(line.start, line.end).minBy { distance(point, it) }
    val base = horizontal.minByOrNull { line ->
        min(
            distance(line.start, nearest(line.start, firstSide)) + distance(line.end, nearest(line.end, secondSide)),
            distance(line.start, nearest(line.start, secondSide)) + distance(line.end, nearest(line.end, firstSide))
        )
    } ?: return null
    val direct = distance(base.start, nearest(base.start, firstSide)) + distance(base.end, nearest(base.end, secondSide))
    val reverse = distance(base.start, nearest(base.start, secondSide)) + distance(base.end, nearest(base.end, firstSide))
    var firstEdge = if (direct <= reverse) firstSide else secondSide
    var secondEdge = if (direct <= reverse) secondSide else firstSide
    var bottomLeft = nearest(base.start, firstEdge)
    var bottomRight = nearest(base.end, secondEdge)
    var topLeft = if (distance(bottomLeft, firstEdge.start) < distance(bottomLeft, firstEdge.end)) firstEdge.end else firstEdge.start
    var topRight = if (distance(bottomRight, secondEdge.start) < distance(bottomRight, secondEdge.end)) secondEdge.end else secondEdge.start
    val baseDx = bottomRight.x - bottomLeft.x; val baseDy = bottomRight.y - bottomLeft.y
    // Choose a stable screen direction, rather than the arbitrary order in which a line was drawn.
    if ((kotlin.math.abs(baseDx) >= kotlin.math.abs(baseDy) && baseDx < 0f) || (kotlin.math.abs(baseDy) > kotlin.math.abs(baseDx) && baseDy < 0f)) {
        val swapBottom = bottomLeft; bottomLeft = bottomRight; bottomRight = swapBottom
        val swapTop = topLeft; topLeft = topRight; topRight = swapTop
        val swapEdge = firstEdge; firstEdge = secondEdge; secondEdge = swapEdge
    }
    val verticalUnits = (firstEdge.value + secondEdge.value) / 2.0
    val horizontalUnits = base.value
    if (verticalUnits <= 0.0 || horizontalUnits <= 0.0) return null
    val ratio = (horizontalUnits / verticalUnits).toFloat()
    val targetWidth = min(source.width * 0.90f, source.height * 0.90f * ratio)
    val targetHeight = targetWidth / ratio
    val left = (source.width - targetWidth) / 2f; val top = (source.height - targetHeight) / 2f
    // Keep the source's horizontal/vertical orientation, but align it exactly to the
    // screen axes. This levels a slightly tilted reference without swapping the photo
    // by 90 degrees when the source image itself is rotated.
    val stableBaseDx = bottomRight.x - bottomLeft.x; val stableBaseDy = bottomRight.y - bottomLeft.y
    val axisX = if (kotlin.math.abs(stableBaseDx) >= kotlin.math.abs(stableBaseDy)) Pt(1f, 0f) else Pt(0f, 1f)
    val baseMid = Pt((bottomLeft.x + bottomRight.x) / 2f, (bottomLeft.y + bottomRight.y) / 2f)
    val oppositeMid = Pt((topLeft.x + topRight.x) / 2f, (topLeft.y + topRight.y) / 2f)
    val depthDx = oppositeMid.x - baseMid.x; val depthDy = oppositeMid.y - baseMid.y
    val perpendicular = Pt(-axisX.y, axisX.x)
    // Rectify the two calibration directions to exactly 90 degrees while retaining
    // the side of the base on which the opposite reference lies.
    val axisY = if (perpendicular.x * depthDx + perpendicular.y * depthDy >= 0f) perpendicular else Pt(-perpendicular.x, -perpendicular.y)
    val targetMid = Pt(source.width / 2f, source.height / 2f)
    val targetBaseMid = Pt(targetMid.x - axisY.x * targetHeight / 2f, targetMid.y - axisY.y * targetHeight / 2f)
    val targetA = Pt(targetBaseMid.x - axisX.x * targetWidth / 2f, targetBaseMid.y - axisX.y * targetWidth / 2f)
    val targetB = Pt(targetBaseMid.x + axisX.x * targetWidth / 2f, targetBaseMid.y + axisX.y * targetWidth / 2f)
    val targetC = Pt(targetB.x + axisY.x * targetHeight, targetB.y + axisY.y * targetHeight)
    val targetD = Pt(targetA.x + axisY.x * targetHeight, targetA.y + axisY.y * targetHeight)
    val sourcePoints = floatArrayOf(bottomLeft.x, bottomLeft.y, bottomRight.x, bottomRight.y, topRight.x, topRight.y, topLeft.x, topLeft.y)
    val targetPoints = floatArrayOf(targetA.x, targetA.y, targetB.x, targetB.y, targetC.x, targetC.y, targetD.x, targetD.y)
    val matrix = AndroidMatrix().apply { setPolyToPoly(sourcePoints, 0, targetPoints, 0, 4) }
    val imageCorners = floatArrayOf(0f, 0f, source.width.toFloat(), 0f, source.width.toFloat(), source.height.toFloat(), 0f, source.height.toFloat())
    matrix.mapPoints(imageCorners)
    val xs = imageCorners.filterIndexed { index, _ -> index % 2 == 0 }
    val ys = imageCorners.filterIndexed { index, _ -> index % 2 == 1 }
    val boundsMinX = xs.min(); val boundsMaxX = xs.max(); val boundsMinY = ys.min(); val boundsMaxY = ys.max()
    val fitAll = min(source.width / (boundsMaxX - boundsMinX), source.height / (boundsMaxY - boundsMinY)) * 0.94f
    val shiftX = (source.width - (boundsMaxX - boundsMinX) * fitAll) / 2f - boundsMinX * fitAll
    val shiftY = (source.height - (boundsMaxY - boundsMinY) * fitAll) / 2f - boundsMinY * fitAll
    val fittedTargets = targetPoints.mapIndexed { index, value -> if (index % 2 == 0) value * fitAll + shiftX else value * fitAll + shiftY }.toFloatArray()
    matrix.setPolyToPoly(sourcePoints, 0, fittedTargets, 0, 4)
    val corrected = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    AndroidCanvas(corrected).apply { fillWithCornerTiles(source); drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) }
    fun map(point: Pt): Pt = floatArrayOf(point.x, point.y).let { values -> matrix.mapPoints(values); Pt(values[0], values[1]) }
    val mappedA = map(bottomLeft); val mappedB = map(bottomRight)
    val mappedC = map(topRight); val mappedD = map(topLeft)
    fun lockToCorners(line: MeasureLine, sourceNear: Pt, sourceFar: Pt, mappedNear: Pt, mappedFar: Pt): MeasureLine {
        return if (distance(line.start, sourceNear) <= distance(line.end, sourceNear)) {
            line.copy(start = mappedNear, end = mappedFar)
        } else {
            line.copy(start = mappedFar, end = mappedNear)
        }
    }
    return corrected to lines.map { line ->
        when (line.id) {
            firstEdge.id -> lockToCorners(line, bottomLeft, topLeft, mappedA, mappedD)
            secondEdge.id -> lockToCorners(line, bottomRight, topRight, mappedB, mappedC)
            base.id -> {
                val startAtA = distance(base.start, bottomLeft) <= distance(base.start, bottomRight)
                line.copy(start = if (startAtA) mappedA else mappedB, end = if (startAtA) mappedB else mappedA)
            }
            else -> line.copy(start = map(line.start), end = map(line.end))
        }
    }
}

private fun AndroidCanvas.fillWithCornerTiles(source: Bitmap) {
    val tileSize = min(5, min(source.width, source.height))
    if (tileSize <= 0) return
    val tiles = arrayOf(
        Bitmap.createBitmap(source, 0, 0, tileSize, tileSize),
        Bitmap.createBitmap(source, source.width - tileSize, 0, tileSize, tileSize),
        Bitmap.createBitmap(source, 0, source.height - tileSize, tileSize, tileSize),
        Bitmap.createBitmap(source, source.width - tileSize, source.height - tileSize, tileSize, tileSize)
    )
    val width = width; val height = height
    for (y in 0 until height step tileSize) for (x in 0 until width step tileSize) {
        val tile = when {
            y < height / 2 && x < width / 2 -> tiles[0]
            y < height / 2 -> tiles[1]
            x < width / 2 -> tiles[2]
            else -> tiles[3]
        }
        drawBitmap(tile, x.toFloat(), y.toFloat(), null)
    }
}

private fun imageCaptureTime(file: File): Long = runCatching {
    ExifInterface(file.absolutePath).getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        ?.let { SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(it)?.time }
}.getOrNull() ?: file.lastModified()

private fun formatCaptureTime(time: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(time))

private fun displayedUnit(unit: String, visibility: String, reference: Boolean): String = if (visibility == "all" || visibility == "calibration" && reference) " $unit" else ""

private fun silhouetteHeight(lines: List<MeasureLine>, unit: String, figure: ComparisonFigure): Float? = recomputeScale(lines, unit)
    ?.let { it * figure.sizeMm / unitFactorMm(unit) }
    ?.takeIf { it.isFinite() && it > 0.0 && it < Float.MAX_VALUE }
    ?.toFloat()

private fun drawComparisonFigure(canvas: AndroidCanvas, figure: ComparisonFigure, left: Float, bottom: Float, height: Float, opacity: Int = 51) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK; alpha = opacity; style = Paint.Style.FILL }
    canvas.save()
    canvas.translate(left, bottom - height)
    canvas.scale(height / 1000f, height / 1000f)
    fun polygon(vararg coordinates: Float) {
        val path = android.graphics.Path().apply {
            moveTo(coordinates[0], coordinates[1])
            for (i in 2 until coordinates.size step 2) lineTo(coordinates[i], coordinates[i + 1])
            close()
        }
        canvas.drawPath(path, paint)
    }
    when (figure) {
        ComparisonFigure.STANDING -> {
            canvas.drawCircle(140f, 65f, 65f, paint)
            polygon(105f,140f, 175f,140f, 215f,165f, 278f,440f, 240f,450f, 197f,265f, 195f,530f, 225f,990f, 165f,1000f, 140f,650f, 115f,1000f, 55f,990f, 85f,530f, 83f,265f, 40f,450f, 2f,440f, 65f,165f)
        }
        ComparisonFigure.TOP -> {
            canvas.drawOval(155f,250f,845f,700f,paint)
            canvas.drawOval(295f,60f,705f,510f,paint)
            canvas.drawOval(110f,435f,255f,710f,paint); canvas.drawOval(745f,435f,890f,710f,paint)
            canvas.drawOval(300f,675f,465f,1000f,paint); canvas.drawOval(535f,675f,700f,1000f,paint)
        }
        ComparisonFigure.SITTING -> {
            canvas.drawCircle(290f,65f,65f,paint)
            polygon(250f,140f,330f,140f,370f,190f,380f,480f,575f,540f,630f,950f,560f,970f,510f,620f,330f,600f,250f,530f,220f,270f,170f,480f,110f,465f,175f,180f)
            polygon(330f,560f,470f,610f,470f,940f,390f,960f,380f,680f,280f,640f)
            canvas.drawRect(130f,570f,430f,610f,paint); canvas.drawRect(130f,610f,170f,1000f,paint)
        }
        ComparisonFigure.HAND -> {
            polygon(260f,1000f,230f,770f,100f,545f,90f,470f,125f,445f,170f,465f,270f,600f,230f,170f,245f,105f,280f,100f,310f,140f,345f,470f,340f,70f,355f,15f,395f,10f,420f,55f,440f,465f,455f,95f,475f,45f,510f,50f,530f,100f,530f,485f,565f,175f,590f,135f,625f,145f,635f,200f,610f,640f,550f,800f,530f,1000f)
        }
        ComparisonFigure.CAT -> {
            canvas.drawOval(250f,390f,790f,900f,paint)
            canvas.drawCircle(700f,315f,170f,paint)
            polygon(565f,220f,585f,55f,675f,165f, 750f,165f,830f,55f,850f,250f)
            polygon(300f,760f,370f,760f,380f,995f,280f,995f); polygon(650f,760f,725f,760f,750f,995f,645f,995f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 80f; paint.strokeCap = Paint.Cap.ROUND
            val tail = android.graphics.Path().apply { moveTo(300f,700f); cubicTo(20f,760f,20f,390f,130f,310f) }; canvas.drawPath(tail,paint)
        }
        ComparisonFigure.DOG -> {
            canvas.drawOval(160f,330f,800f,730f,paint)
            canvas.drawCircle(820f,320f,160f,paint)
            polygon(700f,205f,750f,90f,825f,245f, 955f,290f,1080f,330f,1070f,415f,935f,425f,850f,470f)
            polygon(240f,650f,330f,650f,300f,1000f,200f,1000f); polygon(640f,650f,735f,650f,760f,1000f,660f,1000f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 65f; paint.strokeCap = Paint.Cap.ROUND; canvas.drawLine(200f,400f,30f,195f,paint)
        }
        ComparisonFigure.BEE -> {
            canvas.drawOval(150f,400f,1010f,800f,paint); canvas.drawCircle(1110f,515f,165f,paint)
            canvas.drawOval(390f,60f,800f,460f,paint); canvas.drawOval(650f,175f,985f,480f,paint)
            polygon(155f,565f,20f,670f,180f,690f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 45f; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(370f,740f,270f,960f,paint); canvas.drawLine(625f,760f,575f,980f,paint); canvas.drawLine(850f,710f,920f,935f,paint)
            canvas.drawLine(1100f,355f,1040f,205f,paint); canvas.drawLine(1180f,365f,1270f,235f,paint)
        }
        ComparisonFigure.SPARROW -> {
            canvas.drawOval(220f,260f,1050f,790f,paint); canvas.drawCircle(1040f,300f,215f,paint)
            polygon(1220f,300f,1500f,375f,1220f,430f)
            polygon(280f,440f,5f,340f,190f,710f,340f,700f)
            polygon(375f,390f,900f,410f,750f,650f,430f,680f)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 42f; paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(600f,725f,570f,960f,paint); canvas.drawLine(785f,725f,755f,960f,paint)
            canvas.drawLine(490f,970f,650f,970f,paint); canvas.drawLine(675f,970f,835f,970f,paint)
        }
    }
    canvas.restore()
}

private fun legendEntries(lines: List<MeasureLine>, unit: String): List<Pair<String, String>> {
    val names = measurementPointNames(lines)
    return lines.filterNot { it.isReference }.map { line ->
        val points = names[line.id]?.let { "${it.first}-${it.second}" }.orEmpty()
        val name = if (line.label.isBlank()) points else "$points / ${line.label}"
        name to if (line.value > 0.0 && line.value.isFinite()) "${formatMeasure(line.value, unit)}${if (line.hasDepth()) "*" else ""} $unit" else "Unknown"
    }
}

private fun legendTotal(lines: List<MeasureLine>, unit: String): String {
    val measurements = lines.filterNot { it.isReference }
    val known = measurements.filter { it.value > 0.0 && it.value.isFinite() }
    return "Total${if (known.size < measurements.size) " (partial)" else ""}: ${formatMeasure(known.sumOf { it.value }, unit)} $unit"
}

private fun exportAnnotated(context: Context, original: Bitmap, lines: List<MeasureLine>, guides: List<GuideLine>, exportGuides: Boolean, unit: String, unitVisibility: String, rotation: Float, options: PictureOptions, extrusions: List<Extrusion>) {
    val entries = if (options.legend) legendEntries(lines, unit) else emptyList()
    val legendScale = min(original.width, original.height) / 900f
    val rowHeight = (32f * legendScale).coerceAtLeast(16f)
    val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.BLACK; textSize = rowHeight * 0.6f }
    val valueWidth = entries.maxOfOrNull { legendPaint.measureText(it.second) } ?: 0f
    val longestName = entries.maxOfOrNull { legendPaint.measureText(it.first) } ?: 0f
    val tableWidth = (longestName + valueWidth + rowHeight * 2f).coerceIn(original.width * 0.45f, original.width.toFloat())
    val tableLeft = original.width - tableWidth
    val nameWidth = (tableWidth - valueWidth - rowHeight * 1.5f).coerceAtLeast(rowHeight)
    val wrappedEntries = entries.map { (name, value) ->
        val parts = mutableListOf<String>()
        var remaining = name
        while (remaining.isNotEmpty()) {
            val count = legendPaint.breakText(remaining, true, nameWidth, null).coerceAtLeast(1)
            parts.add(remaining.take(count))
            remaining = remaining.drop(count)
        }
        (if (parts.isEmpty()) listOf("") else parts) to value
    }
    val legendRows = (if (options.legend) wrappedEntries.sumOf { it.first.size } + 1 else 0) + (if (options.sum) 1 else 0)
    val legendHeight = if (legendRows > 0) (rowHeight * (legendRows + 1)).toInt() else 0
    val output = Bitmap.createBitmap(original.width, original.height + legendHeight, Bitmap.Config.ARGB_8888); val canvas = AndroidCanvas(output)
    canvas.drawColor(AndroidColor.WHITE)
    canvas.save()
    canvas.rotate(rotation, original.width / 2f, original.height / 2f)
    val showGuides = guides.isNotEmpty()
    val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = (255f * (1f - options.transparency / 100f)).roundToInt() }
    canvas.drawBitmap(original, 0f, 0f, imagePaint)
    options.figure?.let { figure -> silhouetteHeight(lines, unit, figure)?.let { drawComparisonFigure(canvas, figure, original.width * 0.06f, original.height * 0.95f, it) } }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG); val scale = min(original.width, original.height) / 900f
    fun linePaint(color: Int, width: Float) = paint.apply { this.color = color; strokeWidth = width * scale; style = Paint.Style.STROKE }
    fun outlinedText(text: String, x: Float, y: Float, color: Int, size: Float) {
        paint.textSize = size; paint.isFakeBoldText = true; paint.style = Paint.Style.STROKE; paint.strokeWidth = (size * 0.22f).coerceAtLeast(2f * scale); paint.color = AndroidColor.WHITE
        canvas.drawText(text, x, y, paint)
        paint.style = Paint.Style.FILL; paint.color = color
        canvas.drawText(text, x, y, paint)
    }
    val shapes = drawableGuideShapes(guides)
    val separators = shapes.flatMap { attachedRegions(it, guides) }.flatMap { it.separatorIds }.toSet()
    // Holes stay empty in the picture too.
    canvas.save()
    if (showGuides) shapes.filter { s -> s.all { it.hole } }.forEach { canvas.clipOutPath(shapePath(it)) }
    if (showGuides) shapes.filterNot { s -> s.all { it.hole } }.forEach { shape ->
        (shape.first().fillTransparency ?: if (extrusions.any { extrusion -> (extrusion.depthMm ?: 0.0) > 0.0 && shape.all { guide -> extrusionGuides(extrusion, guides).any { it.id == guide.id } } }) 90 else null)?.let { transparency ->
            canvas.save()
            (listOf(shapePoints(shape)) + attachedRegions(shape, guides).map { it.points }).forEach { points ->
                if (points.isNotEmpty()) {
                    val path = android.graphics.Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = guideColor(shape.first().colorIndex).toArgb(); alpha = ((100 - transparency) * 255f / 100f).roundToInt(); style = Paint.Style.FILL }
                    canvas.drawPath(path, fill)
                    canvas.clipOutPath(path)
                }
            }
            canvas.restore()
        }
    }
    canvas.restore()
    canvas.save()
    if (showGuides) shapes.forEach { shape ->
        (listOf(shapePoints(shape)) + attachedRegions(shape, guides).map { it.points }).forEach { points ->
            if (points.isNotEmpty()) {
                val path = android.graphics.Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                canvas.clipOutPath(path)
            }
        }
    }
    canvas.restore()
    // As on screen: filled shapes get a thin single border; plain lines and unfilled outlines the double line.
    val filledIds = (shapes + closedGuideShapes(guides)).filter { shape ->
        val t = shape.first().fillTransparency ?: if (extrusions.any { e -> (e.depthMm ?: 0.0) > 0.0 && shape.all { g -> extrusionGuides(e, guides).any { it.id == g.id } } }) 90 else null
        t != null && t < 100
    }.flatten().map { it.id }.toSet()
    if (showGuides) guides.forEach { guide ->
        val mid = guide.curveMid
        val separator = guide.id in separators
        if (guide.id in filledIds) {
            val thin = linePaint(guideColor(guide.colorIndex).toArgb(), 1.5f)
            if (mid != null) canvas.drawPath(android.graphics.Path().apply { moveTo(guide.start.x, guide.start.y); quadTo(2f * mid.x - (guide.start.x + guide.end.x) / 2f, 2f * mid.y - (guide.start.y + guide.end.y) / 2f, guide.end.x, guide.end.y) }, thin)
            else canvas.drawLine(guide.start.x, guide.start.y, guide.end.x, guide.end.y, thin)
            return@forEach
        }
        val color = guideColor(guide.colorIndex).copy(alpha = if (separator) 0.45f else 1f).toArgb()
        val width = if (separator) (guide.strokeWidth * 0.35f).coerceAtLeast(1f) else guide.strokeWidth
        if (mid != null) {
            val path = android.graphics.Path().apply { moveTo(guide.start.x, guide.start.y); quadTo(2f * mid.x - (guide.start.x + guide.end.x) / 2f, 2f * mid.y - (guide.start.y + guide.end.y) / 2f, guide.end.x, guide.end.y) }
            canvas.drawPath(path, linePaint(color, width))
            if (!separator) canvas.drawPath(path, linePaint(AndroidColor.WHITE, (guide.strokeWidth * 0.45f).coerceAtLeast(1.5f)))
        } else {
            canvas.drawLine(guide.start.x, guide.start.y, guide.end.x, guide.end.y, linePaint(color, width))
            if (!separator) canvas.drawLine(guide.start.x, guide.start.y, guide.end.x, guide.end.y, linePaint(AndroidColor.WHITE, (guide.strokeWidth * 0.45f).coerceAtLeast(1.5f)))
        }
        guide.lengthMm?.let { mm ->
            val center = guidePointAt(guide, 0.5f)
            val text = "${formatMeasure(mm / unitFactorMm(unit), unit)} $unit"
            paint.textSize = 16f * scale
            outlinedText(text, center.x - paint.measureText(text) / 2f, center.y, AndroidColor.BLACK, 16f * scale)
        }
    }
    if (showGuides) guides.filter { it.pathId != null || it.curveMid != null }
        .flatMap { guide -> listOf(guide.start to guide, guide.end to guide) }
        .fold(emptyList<Pair<Pt, GuideLine>>()) { vertices, vertex -> if (vertices.any { distance(it.first, vertex.first) < 0.6 }) vertices else vertices + vertex }
        .forEach { (point, guide) ->
            paint.style = Paint.Style.FILL; paint.color = guideColor(guide.colorIndex).toArgb(); canvas.drawCircle(point.x, point.y, 3f * scale, paint)
        }
    val pointNames = measurementPointNames(lines)
    val drawnNames = mutableSetOf<String>()
    lines.forEach { line ->
        val a = android.graphics.PointF(line.start.x, line.start.y); val b = android.graphics.PointF(line.end.x, line.end.y)
        val annotationColor = measurementColor(line).toArgb()
        canvas.drawLine(a.x, a.y, b.x, b.y, linePaint(AndroidColor.BLACK, 9f)); canvas.drawLine(a.x, a.y, b.x, b.y, linePaint(annotationColor, 4f))
        listOf(a, b).forEach { p ->
            paint.style = Paint.Style.FILL; paint.color = AndroidColor.BLACK; canvas.drawCircle(p.x, p.y, 12f * scale, paint); paint.color = annotationColor; canvas.drawCircle(p.x, p.y, 8f * scale, paint)
        }
        pointNames[line.id]?.let { names ->
            if (drawnNames.add(names.first)) outlinedText(names.first, a.x + 12f * scale, a.y - 12f * scale, AndroidColor.BLACK, 16f * scale)
            if (drawnNames.add(names.second)) outlinedText(names.second, b.x + 12f * scale, b.y - 12f * scale, AndroidColor.BLACK, 16f * scale)
        }
        val text = if (!line.isReference && line.value <= 0.0) "${if (line.label.isBlank()) "" else line.label + ": "}?" else if (line.isReference && !line.isScaleReference) line.label.ifBlank { "Level" } else "${if (line.label.isBlank()) "" else line.label + ": "}${formatMeasure(line.value, unit)}${if (line.hasDepth()) "*" else ""}${displayedUnit(unit, unitVisibility, line.isReference)}"
        val textSize = 16f * scale; paint.textSize = textSize; val width = paint.measureText(text)
        var angle = Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat()
        if (angle > 90f || angle < -90f) angle += 180f
        val midpoint = android.graphics.PointF((a.x + b.x) / 2f, (a.y + b.y) / 2f)
        val length = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
        val radians = Math.toRadians(angle.toDouble())
        val normalX = (-sin(radians)).toFloat(); val normalY = cos(radians).toFloat()
        val offset = if (width + 30f * scale > length) 25f * scale else 0f
        val x = midpoint.x + normalX * offset + (b.x - a.x) / length.coerceAtLeast(1f) * line.labelOffset; val y = midpoint.y + normalY * offset + (b.y - a.y) / length.coerceAtLeast(1f) * line.labelOffset
        canvas.save(); canvas.rotate(angle, x, y)
        outlinedText(text, x - width / 2f, y + textSize * 0.35f, annotationColor, textSize)
        canvas.restore()
    }
    canvas.restore()
    if (legendHeight > 0) {
        var y = original.height + rowHeight
        if (options.legend) {
            legendPaint.isFakeBoldText = true; canvas.drawText("Measurements", tableLeft + rowHeight * 0.5f, y, legendPaint); legendPaint.isFakeBoldText = false; y += rowHeight
            wrappedEntries.forEach { (parts, value) ->
                canvas.drawText(parts.first(), tableLeft + rowHeight * 0.5f, y, legendPaint)
                canvas.drawText(value, original.width - rowHeight * 0.5f - legendPaint.measureText(value), y, legendPaint)
                y += rowHeight
                parts.drop(1).forEach { part -> canvas.drawText(part, tableLeft + rowHeight * 0.5f, y, legendPaint); y += rowHeight }
            }
        }
        if (options.sum) { legendPaint.isFakeBoldText = true; val total = legendTotal(lines, unit); canvas.drawText(total, (original.width - rowHeight * 0.5f - legendPaint.measureText(total)).coerceAtLeast(0f), y, legendPaint) }
    }
    val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "Idea3D_${System.currentTimeMillis()}.png"); put(MediaStore.Images.Media.MIME_TYPE, "image/png"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Idea 3D") }
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri -> context.contentResolver.openOutputStream(uri)?.use { output.compress(Bitmap.CompressFormat.PNG, 100, it) } }
}

private fun exportMeasurementLog(context: Context, lines: List<MeasureLine>, scale: Double?, unit: String) {
    val references = lines.filter { it.isReference }
    val manual = lines.filterNot { it.isReference }
    val text = buildString {
        appendLine("Idea 3D measurement log")
        appendLine("Unit: $unit")
        appendLine("Scale: ${scale?.let { format(it) + " pixels per " + unit } ?: "not calibrated"}")
        appendLine()
        appendLine("REFERENCE LINES")
        references.forEachIndexed { index, line -> appendLine("${index + 1}. ${line.label.ifBlank { "Reference" }}: ${formatMeasure(line.value, unit)} $unit | start=${format(line.start.x.toDouble())},${format(line.start.y.toDouble())} px | end=${format(line.end.x.toDouble())},${format(line.end.y.toDouble())} px") }
        appendLine()
        appendLine("MEASUREMENTS")
        manual.forEachIndexed { index, line -> appendLine("${index + 1}. ${line.label.ifBlank { "Measurement" }}: ${if (line.value <= 0.0) "Unknown" else "${formatMeasure(line.value, unit)} $unit"} | start=${format(line.start.x.toDouble())},${format(line.start.y.toDouble())} px | end=${format(line.end.x.toDouble())},${format(line.end.y.toDouble())} px") }
    }
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "Idea3D_measurements_${System.currentTimeMillis()}.txt")
        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        put(MediaStore.Downloads.RELATIVE_PATH, "Download/Idea 3D")
    }
    context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { uri ->
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(text) }
    }
}

private fun exportBoschDiagnosticLog(context: Context) {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "Idea3D_Bosch_diagnostic_${System.currentTimeMillis()}.txt")
        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
        put(MediaStore.Downloads.RELATIVE_PATH, "Download/Idea 3D")
    }
    context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)?.let { uri ->
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(BoschDebugLog.exportText()) }
    }
}
