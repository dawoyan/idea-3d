package com.davoyans.idea3d

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.opengl.GLSurfaceView
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import android.graphics.Paint
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToLong

internal data class ViewerMeasurement(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val label: String, val color: Int, val z1: Float = 0f, val z2: Float = 0f)
internal enum class ViewerAngle(val yaw: Float, val pitch: Float, val label: String, val orthographic: Boolean) {
    FRONT(0f, 0f, "Front", true), TOP(0f, 90f, "Top", true), PROFILE(-90f, 0f, "Profile", true), THREE_D(30f, 25f, "3D", false);
    /** Top and Profile look along a plane that contains Z, so depth can be edited directly there. */
    val showsDepth get() = this == TOP || this == PROFILE
}

/** An editable outline vertex of an object, in model millimeters (z is the front surface). */
internal data class ViewerHandle(val objectIndex: Int, val x: Float, val y: Float, val z: Float)

internal enum class ViewerEditKind { MOVE, FRONT, BACK, POINTS }

/** A Z edit made by dragging in the Top or Profile view. [handles] index [ViewerMesh.handles] for [ViewerEditKind.POINTS]. */
internal data class ViewerEdit(val objectIndex: Int, val kind: ViewerEditKind, val deltaMm: Double, val handles: List<Int> = emptyList())

/** Maps model millimeters to the viewer's normalized space. Reused across rebuilds so edits don't make the view jump. */
internal class ViewerFrame(val center: FloatArray, val extent: Float) {
    val mmPerUnit get() = extent / 2f
    fun toUnit(mm: Float, axis: Int) = (mm - center[axis]) / mmPerUnit
    fun toMm(unit: Float, axis: Int) = unit * mmPerUnit + center[axis]
}

/** Camera state shared by successive views of the same viewer session, so a rebuilt model keeps the zoom and pan. */
internal class ViewerCamera(initial: ViewerAngle) {
    @Volatile var angle = initial; private set
    @Volatile var yaw = initial.yaw
    @Volatile var pitch = initial.pitch
    @Volatile var zoom = 1f
    @Volatile var panX = 0f
    @Volatile var panY = 0f
    fun look(next: ViewerAngle) { angle = next; yaw = next.yaw; pitch = next.pitch }
    fun reset() { look(angle); zoom = 1f; panX = 0f; panY = 0f }
}

internal class ViewerMesh private constructor(
    val vertices: FloatBuffer, val baseVertices: FloatArray, val normals: FloatBuffer, val colors: FloatBuffer, val baseColors: FloatArray, val count: Int,
    val picturePlane: FloatBuffer, val frontArrow: FloatBuffer, val measurements: List<ViewerMeasurement>, val triangleObjects: IntArray, val objectNames: List<String>,
    /** Per drawn vertex: true when it belongs to the back surface. */
    val vertexBack: BooleanArray, val handles: List<ViewerHandle>, val frame: ViewerFrame,
    val objectFront: FloatArray, val objectBack: FloatArray, val objectCenterX: FloatArray, val objectCenterY: FloatArray,
    val objectMinThicknessMm: FloatArray, val warnings: List<String>,
) {
    fun frontMm(o: Int) = frame.toMm(objectFront[o], 2).toDouble()
    fun backMm(o: Int) = frame.toMm(objectBack[o], 2).toDouble()

    companion object {
        fun fromStl(
            bytes: ByteArray, triangleColors: List<Int>, measurements: List<ViewerMeasurement> = emptyList(), triangleObjects: IntArray = IntArray(0), objectNames: List<String> = emptyList(),
            vertexBack: BooleanArray = BooleanArray(0), handles: List<ViewerHandle> = emptyList(), minThicknessMm: List<Float> = emptyList(), warnings: List<String> = emptyList(), frame: ViewerFrame? = null,
        ): ViewerMesh {
            val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(bytes.size >= 84) { "Invalid 3D model" }
            data.position(80)
            val count = data.int
            require(count > 0 && 84L + count.toLong() * 50L == bytes.size.toLong() && triangleColors.size == count) { "Invalid 3D model" }
            val vertices = FloatArray(count * 9)
            val normals = FloatArray(count * 9)
            val colors = FloatArray(count * 12)
            val low = FloatArray(3) { Float.POSITIVE_INFINITY }; val high = FloatArray(3) { Float.NEGATIVE_INFINITY }
            repeat(count) { triangle ->
                val nx = data.float; val ny = data.float; val nz = data.float
                val color = triangleColors[triangle]
                repeat(3) { vertex ->
                    val index = triangle * 9 + vertex * 3
                    repeat(3) { axis -> val v = data.float; require(v.isFinite()) { "Invalid model coordinate" }; vertices[index + axis] = v; low[axis] = minOf(low[axis], v); high[axis] = maxOf(high[axis], v) }
                    normals[index] = nx; normals[index + 1] = ny; normals[index + 2] = nz
                    val ci = triangle * 12 + vertex * 4
                    // Lift very dark materials slightly so light and shade on faces stay readable.
                    colors[ci] = 0.22f + 0.78f * android.graphics.Color.red(color) / 255f
                    colors[ci + 1] = 0.22f + 0.78f * android.graphics.Color.green(color) / 255f
                    colors[ci + 2] = 0.22f + 0.78f * android.graphics.Color.blue(color) / 255f
                    colors[ci + 3] = 1f
                }
                data.short
            }
            // Keep the picture plane in view even when the entire model is displaced in Z.
            low[2] = minOf(low[2], 0f); high[2] = maxOf(high[2], 0f)
            measurements.forEach { line -> low[2] = minOf(low[2], line.z1, line.z2); high[2] = maxOf(high[2], line.z1, line.z2) }
            val space = frame ?: ViewerFrame(FloatArray(3) { (high[it] + low[it]) / 2f }, (0..2).maxOf { high[it] - low[it] }.coerceAtLeast(0.00001f))
            vertices.indices.forEach { i -> vertices[i] = space.toUnit(vertices[i], i % 3) }
            fun buffer(values: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values); position(0) }
            val x0 = space.toUnit(low[0], 0); val x1 = space.toUnit(high[0], 0)
            val y0 = space.toUnit(low[1], 1); val y1 = space.toUnit(high[1], 1)
            val z0 = space.toUnit(0f, 2)
            val plane = floatArrayOf(x0,y0,z0, x1,y0,z0, x1,y0,z0, x1,y1,z0, x1,y1,z0, x0,y1,z0, x0,y1,z0, x0,y0,z0)
            val arrow = floatArrayOf(x0,y0,z0, x0,y0,z0+0.35f, x0,y0,z0+0.35f, x0+0.08f,y0,z0+0.25f, x0,y0,z0+0.35f, x0-0.08f,y0,z0+0.25f)
            val normalizedMeasurements = measurements.map { it.copy(x1 = space.toUnit(it.x1, 0), y1 = space.toUnit(it.y1, 1), x2 = space.toUnit(it.x2, 0), y2 = space.toUnit(it.y2, 1), z1 = space.toUnit(it.z1, 2), z2 = space.toUnit(it.z2, 2)) }
            val objects = triangleObjects.takeIf { it.size == count } ?: IntArray(count) { -1 }
            val back = vertexBack.takeIf { it.size == count * 3 }
            val objectCount = objectNames.size
            val front = FloatArray(objectCount) { Float.NEGATIVE_INFINITY }; val rear = FloatArray(objectCount) { Float.POSITIVE_INFINITY }
            val minX = FloatArray(objectCount) { Float.POSITIVE_INFINITY }; val maxX = FloatArray(objectCount) { Float.NEGATIVE_INFINITY }
            val minY = FloatArray(objectCount) { Float.POSITIVE_INFINITY }; val maxY = FloatArray(objectCount) { Float.NEGATIVE_INFINITY }
            for (v in 0 until count * 3) {
                val o = objects[v / 3]; if (o !in 0 until objectCount) continue
                val x = vertices[v * 3]; val y = vertices[v * 3 + 1]; val z = vertices[v * 3 + 2]
                if (back == null || !back[v]) front[o] = maxOf(front[o], z)
                if (back == null || back[v]) rear[o] = minOf(rear[o], z)
                minX[o] = minOf(minX[o], x); maxX[o] = maxOf(maxX[o], x); minY[o] = minOf(minY[o], y); maxY[o] = maxOf(maxY[o], y)
            }
            val normalizedHandles = handles.map { ViewerHandle(it.objectIndex, space.toUnit(it.x, 0), space.toUnit(it.y, 1), space.toUnit(it.z, 2)) }
            return ViewerMesh(
                buffer(vertices), vertices.copyOf(), buffer(normals), buffer(colors), colors, count * 3, buffer(plane), buffer(arrow), normalizedMeasurements, objects, objectNames,
                back ?: BooleanArray(count * 3), normalizedHandles, space, front, rear,
                FloatArray(objectCount) { (minX[it] + maxX[it]) / 2f }, FloatArray(objectCount) { (minY[it] + maxY[it]) / 2f },
                FloatArray(objectCount) { minThicknessMm.getOrNull(it) ?: ((front[it] - rear[it]) * space.mmPerUnit) }, warnings,
            )
        }
    }
}

private class ModelRenderer(private val mesh: ViewerMesh, val camera: ViewerCamera) : GLSurfaceView.Renderer {
    @Volatile var showPicturePlane = false
    @Volatile var showFrontArrow = false
    @Volatile var sunPosition = 0.15f
    @Volatile var capture: ((Bitmap?) -> Unit)? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0
    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        gl.glClearColor(0.94f, 0.96f, 0.95f, 1f)
        gl.glEnable(GL10.GL_DEPTH_TEST)
        gl.glEnable(GL10.GL_LIGHTING); gl.glEnable(GL10.GL_LIGHT0); gl.glEnable(GL10.GL_COLOR_MATERIAL)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, floatArrayOf(0.55f, 0.55f, 0.55f, 1f), 0)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, floatArrayOf(0.75f, 0.75f, 0.75f, 1f), 0)
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY); gl.glEnableClientState(GL10.GL_NORMAL_ARRAY); gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
    }
    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        surfaceWidth = width; surfaceHeight = height
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL10.GL_PROJECTION); gl.glLoadIdentity()
        val ratio = width.toFloat() / height.coerceAtLeast(1)
        val halfHeight = if (ratio < 1f) 2f / ratio.coerceAtLeast(0.01f) else 2f
        gl.glOrthof(-halfHeight * ratio, halfHeight * ratio, -halfHeight, halfHeight, 1f, 100f)
        gl.glMatrixMode(GL10.GL_MODELVIEW)
    }
    override fun onDrawFrame(gl: GL10) {
        val callback = capture
        capture = null
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT or GL10.GL_DEPTH_BUFFER_BIT)
        gl.glLoadIdentity(); gl.glTranslatef(camera.panX, camera.panY, -20f)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, floatArrayOf((sunPosition - 0.5f) * 8f, 3f, 2f, 0f), 0)
        gl.glScalef(camera.zoom, camera.zoom, camera.zoom); gl.glRotatef(camera.pitch, 1f, 0f, 0f); gl.glRotatef(camera.yaw, 0f, 1f, 0f)
        gl.glDisable(GL10.GL_LIGHTING); gl.glDisableClientState(GL10.GL_NORMAL_ARRAY); gl.glDisableClientState(GL10.GL_COLOR_ARRAY)
        gl.glLineWidth(2f)
        if (showPicturePlane && callback == null) {
            mesh.picturePlane.position(0); gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.picturePlane)
            gl.glColor4f(0.35f, 0.4f, 0.38f, 1f); gl.glDrawArrays(GL10.GL_LINES, 0, 8)
        }
        if (showFrontArrow && callback == null) {
            mesh.frontArrow.position(0); gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.frontArrow)
            gl.glColor4f(0.85f, 0.1f, 0.1f, 1f); gl.glDrawArrays(GL10.GL_LINES, 0, 6)
        }
        gl.glEnable(GL10.GL_LIGHTING); gl.glEnableClientState(GL10.GL_NORMAL_ARRAY); gl.glEnableClientState(GL10.GL_COLOR_ARRAY)
        mesh.vertices.position(0); mesh.normals.position(0); mesh.colors.position(0)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.vertices); gl.glNormalPointer(GL10.GL_FLOAT, 0, mesh.normals); gl.glColorPointer(4, GL10.GL_FLOAT, 0, mesh.colors)
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, mesh.count)
        if (callback != null) {
            val bitmap = runCatching {
                require(surfaceWidth > 0 && surfaceHeight > 0)
                val pixels = ByteBuffer.allocateDirect(surfaceWidth * surfaceHeight * 4)
                gl.glReadPixels(0, 0, surfaceWidth, surfaceHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixels)
                val colors = IntArray(surfaceWidth * surfaceHeight)
                for (y in 0 until surfaceHeight) for (x in 0 until surfaceWidth) {
                    val offset = (y * surfaceWidth + x) * 4
                    colors[(surfaceHeight - 1 - y) * surfaceWidth + x] = android.graphics.Color.rgb(pixels.get(offset).toInt() and 255, pixels.get(offset + 1).toInt() and 255, pixels.get(offset + 2).toInt() and 255)
                }
                Bitmap.createBitmap(colors, surfaceWidth, surfaceHeight, Bitmap.Config.ARGB_8888)
            }.getOrNull()
            callback(bitmap)
        }
    }
}

private class OverlayView(context: Context, private val paint: (Canvas) -> Unit) : View(context) {
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); if (width > 0 && height > 0) paint(canvas) }
}

/** What a one-finger drag in the Top or Profile view is changing. */
private data class DragTarget(val objectIndex: Int, val kind: ViewerEditKind, val handles: List<Int> = emptyList())

private class ModelView(context: Context, private val mesh: ViewerMesh, private val camera: ViewerCamera) : GLSurfaceView(context) {
    private val modelRenderer = ModelRenderer(mesh, camera)
    val overlay = OverlayView(context, ::drawOverlay)
    var showMeasurements = false
    var onObjectTap: ((Int) -> Unit)? = null
    var onSelect: ((Int?) -> Unit)? = null
    /** Null keeps the viewer read-only apart from the tap dialog. */
    var onEdit: ((ViewerEdit) -> Unit)? = null
    var format: (Double) -> String = { "${(it * 10).roundToLong() / 10.0} mm" }
    var stepMm = 1.0
    var selected: Int? = null; private set
    private val density = resources.displayMetrics.density
    private val current = mesh.baseVertices.copyOf()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var previousX = 0f; private var previousY = 0f; private var previousSpan = 0f; private var previousPointers = 0
    private var downX = 0f; private var downY = 0f; private var moved = false; private var multiTouch = false
    private var target: DragTarget? = null
    private var dragDeltaMm = 0.0
    private var snapLabel: String? = null
    private var snapZMm: Double? = null
    /** An edit sent for rebuilding; its preview stays until the rebuilt model replaces this view. */
    private var committed = false
    init { setEGLContextClientVersion(1); setRenderer(modelRenderer); renderMode = RENDERMODE_WHEN_DIRTY }

    private val editable get() = onEdit != null && camera.angle.showsDepth && mesh.objectNames.isNotEmpty()

    fun reset() { camera.reset(); refresh() }
    fun setAngle(next: ViewerAngle) { camera.look(next); cancelDrag(); refresh() }
    private fun refresh() { requestRender(); overlay.invalidate() }

    fun select(index: Int?) {
        val next = index?.takeIf { it in mesh.objectNames.indices }
        if (next == selected) return
        selected = next
        // Tint the selected object toward blue so it stays recognizable in every view.
        val colors = mesh.baseColors.copyOf()
        if (next != null) for (v in 0 until mesh.count) if (mesh.triangleObjects[v / 3] == next) {
            val c = v * 4
            colors[c] = colors[c] * 0.65f + 0.10f; colors[c + 1] = colors[c + 1] * 0.65f + 0.30f; colors[c + 2] = colors[c + 2] * 0.65f + 0.35f
        }
        queueEvent { mesh.colors.position(0); mesh.colors.put(colors); mesh.colors.position(0) }
        refresh()
    }

    private fun project(x: Float, y: Float, z: Float, combined: FloatArray = viewMatrix(camera, width, height)): FloatArray {
        val out = FloatArray(4); Matrix.multiplyMV(out, 0, combined, 0, floatArrayOf(x, y, z, 1f), 0)
        return floatArrayOf((out[0] / out[3] + 1f) * width / 2f, (1f - out[1] / out[3]) * height / 2f, out[2] / out[3])
    }

    /** Screen direction of +Z (unit vector) and how many pixels one normalized unit spans. */
    private fun zAxisOnScreen(): Triple<Float, Float, Float> {
        val combined = viewMatrix(camera, width, height)
        val a = project(0f, 0f, 0f, combined); val b = project(0f, 0f, 1f, combined)
        val length = hypot(b[0] - a[0], b[1] - a[1]).coerceAtLeast(0.0001f)
        return Triple((b[0] - a[0]) / length, (b[1] - a[1]) / length, length)
    }

    /** Returns the object under a screen point, preferring the triangle nearest the viewer. */
    fun pickObject(sx: Float, sy: Float): Int? {
        if (width == 0 || height == 0) return null
        val combined = viewMatrix(camera, width, height)
        fun project(i: Int) = project(current[i * 3], current[i * 3 + 1], current[i * 3 + 2], combined)
        var best: Int? = null; var bestDepth = Float.POSITIVE_INFINITY
        for (t in 0 until mesh.count / 3) {
            val a = project(t * 3); val b = project(t * 3 + 1); val c = project(t * 3 + 2)
            val d1 = (sx - b[0]) * (a[1] - b[1]) - (a[0] - b[0]) * (sy - b[1])
            val d2 = (sx - c[0]) * (b[1] - c[1]) - (b[0] - c[0]) * (sy - c[1])
            val d3 = (sx - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (sy - a[1])
            val hasNeg = d1 < 0 || d2 < 0 || d3 < 0; val hasPos = d1 > 0 || d2 > 0 || d3 > 0
            if (hasNeg && hasPos) continue
            val depth = (a[2] + b[2] + c[2]) / 3f
            if (depth < bestDepth) { bestDepth = depth; best = mesh.triangleObjects[t] }
        }
        return best?.takeIf { it >= 0 }
    }

    /** Front and back depth handles sit just outside the object along Z so they never cover its points. */
    private fun faceHandles(o: Int): Pair<FloatArray, FloatArray> {
        val (ux, uy, _) = zAxisOnScreen()
        val offset = 26f * density
        val front = project(mesh.objectCenterX[o], mesh.objectCenterY[o], mesh.objectFront[o])
        val back = project(mesh.objectCenterX[o], mesh.objectCenterY[o], mesh.objectBack[o])
        return floatArrayOf(front[0] + ux * offset, front[1] + uy * offset) to floatArrayOf(back[0] - ux * offset, back[1] - uy * offset)
    }

    private fun hitTarget(x: Float, y: Float): DragTarget? {
        selected?.let { o ->
            val reach = 24f * density
            val points = mesh.handles.indices.filter { mesh.handles[it].objectIndex == o }.map { i -> val h = mesh.handles[i]; i to project(h.x, h.y, h.z) }
            points.minByOrNull { hypot(it.second[0] - x, it.second[1] - y) }?.takeIf { hypot(it.second[0] - x, it.second[1] - y) <= reach }?.let { nearest ->
                // Points that line up in this view (e.g. top and bottom corners in the top view) look like one point, so they move together.
                val group = points.filter { hypot(it.second[0] - nearest.second[0], it.second[1] - nearest.second[1]) < 3f * density }.map { it.first }
                return DragTarget(o, ViewerEditKind.POINTS, group)
            }
            val (front, back) = faceHandles(o)
            val toFront = hypot(front[0] - x, front[1] - y); val toBack = hypot(back[0] - x, back[1] - y)
            if (minOf(toFront, toBack) <= reach) return DragTarget(o, if (toFront <= toBack) ViewerEditKind.FRONT else ViewerEditKind.BACK)
        }
        return pickObject(x, y)?.let { DragTarget(it, ViewerEditKind.MOVE) }
    }

    /** Z values (mm) that the dragged part currently has, and what they can snap to. */
    private fun movingValues(t: DragTarget): List<Double> = when (t.kind) {
        ViewerEditKind.MOVE -> listOf(mesh.frontMm(t.objectIndex), mesh.backMm(t.objectIndex))
        ViewerEditKind.FRONT -> listOf(mesh.frontMm(t.objectIndex))
        ViewerEditKind.BACK -> listOf(mesh.backMm(t.objectIndex))
        ViewerEditKind.POINTS -> listOf(mesh.frame.toMm(mesh.handles[t.handles.first()].z, 2).toDouble())
    }

    private fun updateDrag(x: Float, y: Float) {
        val t = target ?: return
        val (ux, uy, pixelsPerUnit) = zAxisOnScreen()
        val mmPerPixel = mesh.frame.mmPerUnit / pixelsPerUnit
        var delta = ((x - downX) * ux + (y - downY) * uy) * mmPerPixel.toDouble()
        val minThickness = 0.1
        fun clamp(d: Double) = when (t.kind) {
            ViewerEditKind.FRONT -> max(d, minThickness - mesh.objectMinThicknessMm[t.objectIndex])
            ViewerEditKind.BACK -> minOf(d, mesh.objectMinThicknessMm[t.objectIndex] - minThickness)
            else -> d
        }
        delta = clamp(delta)
        val targets = buildList {
            add(0.0 to "Z = 0")
            mesh.objectNames.indices.filter { it != t.objectIndex }.forEach { o ->
                add(mesh.frontMm(o) to "${mesh.objectNames[o]} front"); add(mesh.backMm(o) to "${mesh.objectNames[o]} back")
            }
        }
        val reach = 10f * density * mmPerPixel
        val snap = movingValues(t).flatMap { value -> targets.map { (z, label) -> Triple(z - value, z, label) } }
            .filter { abs(it.first - delta) <= reach }.minByOrNull { abs(it.first - delta) }
        val wasSnapped = snapLabel
        if (snap != null && clamp(snap.first) == snap.first) { delta = snap.first; snapZMm = snap.second; snapLabel = snap.third }
        else { delta = clamp(Math.round(delta / stepMm) * stepMm); snapZMm = null; snapLabel = null }
        if (snapLabel != null && snapLabel != wasSnapped) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        dragDeltaMm = delta
        preview(t, delta)
    }

    private fun preview(t: DragTarget?, deltaMm: Double) {
        val d = (deltaMm / mesh.frame.mmPerUnit).toFloat()
        val groups = t?.takeIf { it.kind == ViewerEditKind.POINTS }?.handles?.map { mesh.handles[it] }.orEmpty()
        for (v in 0 until mesh.count) {
            val i = v * 3 + 2
            current[i] = mesh.baseVertices[i]
            if (t == null || mesh.triangleObjects[v / 3] != t.objectIndex) continue
            val affected = when (t.kind) {
                ViewerEditKind.MOVE -> true
                ViewerEditKind.FRONT -> !mesh.vertexBack[v]
                ViewerEditKind.BACK -> mesh.vertexBack[v]
                ViewerEditKind.POINTS -> groups.any { abs(it.x - current[v * 3]) < 1e-4f && abs(it.y - current[v * 3 + 1]) < 1e-4f }
            }
            if (affected) current[i] += d
        }
        val snapshot = current.copyOf()
        queueEvent { mesh.vertices.position(0); mesh.vertices.put(snapshot); mesh.vertices.position(0) }
        refresh()
    }

    private fun cancelDrag() {
        if (target != null && !committed) preview(null, 0.0)
        target = null; snapLabel = null; snapZMm = null; dragDeltaMm = 0.0
    }

    fun setReferencesVisible(frame: Boolean, arrow: Boolean) {
        modelRenderer.showPicturePlane = frame; modelRenderer.showFrontArrow = arrow
        requestRender()
    }
    fun setSunPosition(position: Float) { modelRenderer.sunPosition = position.coerceIn(0f, 1f); requestRender() }
    fun exportImage() {
        modelRenderer.capture = { bitmap ->
            Thread {
                val result = runCatching {
                    requireNotNull(bitmap) { "Could not capture the 3D view" }
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "Idea3D_3D_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Idea 3D")
                    }
                    val resolver = context.contentResolver
                    val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                    try {
                        requireNotNull(resolver.openOutputStream(uri)).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    } catch (error: Exception) { resolver.delete(uri, null, null); throw error }
                }
                bitmap?.recycle()
                post { Toast.makeText(context, if (result.isSuccess) "3D image saved to Pictures/Idea 3D" else "3D image export failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show() }
            }.start()
            requestRender()
        }
        requestRender()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (committed) return true
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; moved = false; multiTouch = false
                target = if (editable) hitTarget(event.x, event.y) else null
            }
            MotionEvent.ACTION_POINTER_DOWN -> { multiTouch = true; cancelDrag() }
            MotionEvent.ACTION_UP -> {
                previousPointers = 0
                val t = target
                if (t != null && moved && !multiTouch) {
                    if (abs(dragDeltaMm) > 1e-6) { committed = true; snapLabel = null; overlay.invalidate(); onEdit?.invoke(ViewerEdit(t.objectIndex, t.kind, dragDeltaMm, t.handles)) }
                    else cancelDrag()
                    return true
                }
                cancelDrag()
                if (!moved && !multiTouch) {
                    val hit = pickObject(event.x, event.y)
                    if (editable) {
                        if (hit != null && hit == selected) onObjectTap?.invoke(hit)
                        else { select(hit); onSelect?.invoke(hit) }
                    } else hit?.let { onObjectTap?.invoke(it) }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> { previousPointers = 0; cancelDrag(); return true }
            MotionEvent.ACTION_POINTER_UP -> { previousPointers = 0; return true }
        }
        val count = event.pointerCount
        val x = if (count >= 2) (event.getX(0) + event.getX(1)) / 2f else event.x
        val y = if (count >= 2) (event.getY(0) + event.getY(1)) / 2f else event.y
        val span = if (count >= 2) hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1)) else 0f
        if (event.actionMasked == MotionEvent.ACTION_MOVE && hypot(event.x - downX, event.y - downY) > 12f * density) {
            if (!moved) target?.let { t -> if (t.objectIndex != selected) { select(t.objectIndex); onSelect?.invoke(t.objectIndex) } }
            moved = true
        }
        if (event.actionMasked == MotionEvent.ACTION_MOVE && target != null && count == 1) {
            if (moved) updateDrag(event.x, event.y)
        } else if (event.actionMasked == MotionEvent.ACTION_MOVE && previousPointers == count && (moved || count >= 2)) {
            if (count >= 2 || camera.angle.orthographic) {
                if (count >= 2 && previousSpan > 1f) camera.zoom = (camera.zoom * span / previousSpan).coerceIn(0.2f, 5f)
                val ratio = width.toFloat() / height.coerceAtLeast(1)
                val screenScale = (if (ratio < 1f) 4f / ratio.coerceAtLeast(0.01f) else 4f) / height.coerceAtLeast(1)
                camera.panX += (x - previousX) * screenScale
                camera.panY -= (y - previousY) * screenScale
            } else {
                camera.yaw += (x - previousX) * 0.5f
                camera.pitch += (y - previousY) * 0.5f
            }
            refresh()
        }
        previousX = x; previousY = y; previousSpan = span; previousPointers = count
        return true
    }

    private fun drawOverlay(canvas: Canvas) {
        if (editable) drawDepthEditing(canvas)
        drawAxes(canvas)
        if (showMeasurements) drawMeasurements(canvas)
    }

    private fun label(canvas: Canvas, text: String, x: Float, y: Float, color: Int = android.graphics.Color.BLACK, align: Paint.Align = Paint.Align.CENTER, size: Float = 13f) {
        paint.textSize = size * density; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD; paint.textAlign = align; paint.pathEffect = null
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f * density; paint.color = android.graphics.Color.WHITE; canvas.drawText(text, x, y, paint)
        paint.style = Paint.Style.FILL; paint.color = color; canvas.drawText(text, x, y, paint)
    }

    /** Line across the whole view at a Z value, perpendicular to the Z axis on screen. */
    private fun zLine(canvas: Canvas, zMm: Double, color: Int, dashed: Boolean): FloatArray {
        val (ux, uy, _) = zAxisOnScreen()
        val p = project(0f, 0f, mesh.frame.toUnit(zMm.toFloat(), 2))
        val reach = (width + height).toFloat()
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * density; paint.color = color
        paint.pathEffect = if (dashed) DashPathEffect(floatArrayOf(8f * density, 6f * density), 0f) else null
        canvas.drawLine(p[0] - uy * reach, p[1] + ux * reach, p[0] + uy * reach, p[1] - ux * reach, paint)
        paint.pathEffect = null
        return p
    }

    private fun drawDepthEditing(canvas: Canvas) {
        val (ux, uy, _) = zAxisOnScreen()
        val vertical = abs(uy) > abs(ux)
        // Z = 0 is the front drawing plane; label it near the screen edge so it never covers the model.
        val origin = zLine(canvas, 0.0, 0x99607D8B.toInt(), dashed = true)
        if (vertical) label(canvas, "Z = 0", 8f * density, origin[1] - 5f * density, 0xFF607D8B.toInt(), Paint.Align.LEFT, 11f)
        else label(canvas, "Z = 0", origin[0] + 6f * density, 18f * density, 0xFF607D8B.toInt(), Paint.Align.LEFT, 11f)
        val frontText = "front ${if (vertical) "↓" else "←"}"
        if (vertical) label(canvas, frontText, width - 8f * density, origin[1] + 16f * density, 0xFF1565C0.toInt(), Paint.Align.RIGHT, 11f)
        else label(canvas, frontText, origin[0] - 6f * density, 18f * density, 0xFF1565C0.toInt(), Paint.Align.RIGHT, 11f)

        snapZMm?.let { z -> zLine(canvas, z, 0xFFE65100.toInt(), dashed = true) }
        val o = selected ?: return
        val t = target
        val d = if (t != null && t.objectIndex == o) (dragDeltaMm / mesh.frame.mmPerUnit).toFloat() else 0f
        val frontShift = if (t?.kind == ViewerEditKind.MOVE || t?.kind == ViewerEditKind.FRONT) d else 0f
        val backShift = if (t?.kind == ViewerEditKind.MOVE || t?.kind == ViewerEditKind.BACK) d else 0f
        val accent = 0xFF1565C0.toInt()

        // Depth dimension beside the object, parallel to Z.
        val frontZ = mesh.objectFront[o] + frontShift; val backZ = mesh.objectBack[o] + backShift
        val f = project(mesh.objectCenterX[o], mesh.objectCenterY[o], frontZ); val b = project(mesh.objectCenterX[o], mesh.objectCenterY[o], backZ)
        val front = floatArrayOf(f[0] + ux * 26f * density, f[1] + uy * 26f * density)
        val back = floatArrayOf(b[0] - ux * 26f * density, b[1] - uy * 26f * density)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * density; paint.color = accent
        canvas.drawLine(f[0], f[1], front[0], front[1], paint); canvas.drawLine(b[0], b[1], back[0], back[1], paint)
        fun handle(p: FloatArray) {
            paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.WHITE; canvas.drawCircle(p[0], p[1], 13f * density, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f * density; paint.color = accent; canvas.drawCircle(p[0], p[1], 13f * density, paint)
            // Double arrow along Z: this handle slides only in depth.
            val ax = ux * 7f * density; val ay = uy * 7f * density; val px = -uy * 4f * density; val py = ux * 4f * density
            canvas.drawLine(p[0] - ax, p[1] - ay, p[0] + ax, p[1] + ay, paint)
            canvas.drawLine(p[0] + ax, p[1] + ay, p[0] + ax * 0.4f + px, p[1] + ay * 0.4f + py, paint); canvas.drawLine(p[0] + ax, p[1] + ay, p[0] + ax * 0.4f - px, p[1] + ay * 0.4f - py, paint)
            canvas.drawLine(p[0] - ax, p[1] - ay, p[0] - ax * 0.4f + px, p[1] - ay * 0.4f + py, paint); canvas.drawLine(p[0] - ax, p[1] - ay, p[0] - ax * 0.4f - px, p[1] - ay * 0.4f - py, paint)
        }
        handle(front); handle(back)

        // Outline points of the selected object; dots that line up in this view are drawn once.
        val drawn = mutableListOf<FloatArray>()
        mesh.handles.forEachIndexed { i, h ->
            if (h.objectIndex != o) return@forEachIndexed
            val shift = when {
                t?.kind == ViewerEditKind.MOVE -> d
                t?.kind == ViewerEditKind.POINTS && i in t.handles -> d
                else -> 0f
            }
            val p = project(h.x, h.y, h.z + shift)
            if (drawn.any { hypot(it[0] - p[0], it[1] - p[1]) < 3f * density }) return@forEachIndexed
            drawn += p
            val active = t?.kind == ViewerEditKind.POINTS && i in t.handles
            paint.style = Paint.Style.FILL; paint.color = if (active) 0xFFE65100.toInt() else android.graphics.Color.WHITE; canvas.drawCircle(p[0], p[1], 6f * density, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f * density; paint.color = if (active) 0xFFE65100.toInt() else accent; canvas.drawCircle(p[0], p[1], 6f * density, paint)
        }

        // Live value next to the finger while dragging.
        if (t != null && t.objectIndex == o && moved) {
            val dm = dragDeltaMm
            val depth = mesh.frontMm(o) - mesh.backMm(o)
            fun signed(v: Double) = (if (v >= 0) "+" else "−") + format(abs(v))
            val text = when (t.kind) {
                ViewerEditKind.MOVE -> "Z ${signed(dm)} · front ${format(mesh.frontMm(o) + dm)}"
                ViewerEditKind.FRONT -> "Front ${format(mesh.frontMm(o) + dm)} · depth ${format(depth + dm)}"
                ViewerEditKind.BACK -> "Back ${format(mesh.backMm(o) + dm)} · depth ${format(depth - dm)}"
                ViewerEditKind.POINTS -> "${t.handles.size} point${if (t.handles.size == 1) "" else "s"} Z ${format(mesh.frame.toMm(mesh.handles[t.handles.first()].z, 2) + dm)}"
            }
            label(canvas, text, width / 2f, 40f * density + if (vertical) 0f else 16f * density, 0xFF1A2421.toInt(), size = 15f)
            snapLabel?.let { label(canvas, "Snapped to $it", width / 2f, 60f * density + if (vertical) 0f else 16f * density, 0xFFE65100.toInt(), size = 12f) }
        }
    }

    private fun drawMeasurements(canvas: Canvas) {
        val combined = viewMatrix(camera, width, height)
        mesh.measurements.forEach { line ->
            val a = project(line.x1, line.y1, line.z1, combined); val b = project(line.x2, line.y2, line.z2, combined)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f * density; paint.color = android.graphics.Color.WHITE
            canvas.drawLine(a[0], a[1], b[0], b[1], paint)
            paint.strokeWidth = 2f * density; paint.color = line.color; canvas.drawLine(a[0], a[1], b[0], b[1], paint)
            paint.style = Paint.Style.FILL; canvas.drawCircle(a[0], a[1], 3f * density, paint); canvas.drawCircle(b[0], b[1], 3f * density, paint)
            label(canvas, line.label, (a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f - 4f * density, size = 14f)
        }
    }

    /** Corner gizmo with the same X right, Y up, Z toward-viewer convention used in the editor and exports. */
    private fun drawAxes(canvas: Canvas) {
        val rotation = FloatArray(16); Matrix.setIdentityM(rotation, 0)
        Matrix.rotateM(rotation, 0, camera.pitch, 1f, 0f, 0f); Matrix.rotateM(rotation, 0, camera.yaw, 0f, 1f, 0f)
        val origin = 56f * density to height - 56f * density
        val length = 24f * density
        val axes = listOf(Triple("X", floatArrayOf(1f, 0f, 0f, 0f), 0xFFD32F2F.toInt()), Triple("Y", floatArrayOf(0f, 1f, 0f, 0f), 0xFF2E7D32.toInt()), Triple("Z", floatArrayOf(0f, 0f, 1f, 0f), 0xFF1565C0.toInt()))
        val out = FloatArray(4)
        axes.map { (name, axis, color) -> Matrix.multiplyMV(out, 0, rotation, 0, axis, 0); Triple(name, floatArrayOf(out[0], out[1], out[2]), color) }
            .sortedBy { it.second[2] }
            .forEach { (name, dir, color) ->
                val ex = origin.first + dir[0] * length; val ey = origin.second - dir[1] * length
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f * density; paint.color = color
                val pointsAtViewer = abs(dir[0]) + abs(dir[1]) < 0.05f
                if (pointsAtViewer) {
                    canvas.drawCircle(origin.first, origin.second, 5f * density, paint)
                    if (dir[2] > 0) { paint.style = Paint.Style.FILL; canvas.drawCircle(origin.first, origin.second, 2f * density, paint) }
                } else canvas.drawLine(origin.first, origin.second, ex, ey, paint)
                paint.style = Paint.Style.FILL; paint.textSize = 12f * density; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
                val lx = origin.first + dir[0] * (length + 9f * density); val ly = origin.second - dir[1] * (length + 9f * density) + 4f * density
                canvas.drawText(if (pointsAtViewer) "$name${if (dir[2] > 0) "⊙" else "⊗"}" else name, if (pointsAtViewer) origin.first + 14f * density else lx, if (pointsAtViewer) origin.second - 8f * density else ly, paint)
            }
    }
}

private fun viewMatrix(camera: ViewerCamera, width: Int, height: Int): FloatArray {
    val ratio = width.toFloat() / height.coerceAtLeast(1)
    val halfHeight = if (ratio < 1f) 2f / ratio.coerceAtLeast(0.01f) else 2f
    val projection = FloatArray(16); val model = FloatArray(16); val combined = FloatArray(16)
    Matrix.orthoM(projection, 0, -halfHeight * ratio, halfHeight * ratio, -halfHeight, halfHeight, 1f, 100f)
    Matrix.setIdentityM(model, 0); Matrix.translateM(model, 0, camera.panX, camera.panY, -20f)
    Matrix.scaleM(model, 0, camera.zoom, camera.zoom, camera.zoom)
    Matrix.rotateM(model, 0, camera.pitch, 1f, 0f, 0f); Matrix.rotateM(model, 0, camera.yaw, 0f, 1f, 0f)
    Matrix.multiplyMM(combined, 0, projection, 0, model, 0)
    return combined
}

@Composable
internal fun ThreeDViewerDialog(
    mesh: ViewerMesh, initialAngle: ViewerAngle, initialSun: Float, onSunChange: (Float) -> Unit, onAngleChange: (ViewerAngle) -> Unit = {}, onEditObject: (Int) -> Unit = {}, onClose: () -> Unit,
    format: (Double) -> String = { "${(it * 10).roundToLong() / 10.0} mm" }, stepMm: Double = 1.0, onEdit: ((ViewerEdit) -> Unit)? = null, canUndo: Boolean = false, onUndo: () -> Unit = {},
) {
    val context = LocalContext.current
    // Camera and selection outlive a rebuilt model so an edit doesn't reset the view.
    val camera = remember { ViewerCamera(initialAngle) }
    var angle by remember { mutableStateOf(initialAngle) }
    var selected by remember { mutableStateOf<Int?>(null) }
    val view = remember(mesh) { ModelView(context, mesh, camera) }
    view.onObjectTap = onEditObject
    view.onSelect = { selected = it }
    view.onEdit = onEdit
    view.format = format
    view.stepMm = stepMm.coerceAtLeast(0.001)
    var showFrame by remember { mutableStateOf(false) }
    var showArrow by remember { mutableStateOf(false) }
    var showMeasurements by remember { mutableStateOf(false) }
    var sun by remember { mutableStateOf(initialSun.coerceIn(0f, 1f)) }
    LaunchedEffect(view) {
        view.setSunPosition(sun); view.setReferencesVisible(showFrame, showArrow); view.showMeasurements = showMeasurements
        selected = selected?.takeIf { it in mesh.objectNames.indices }
        view.select(selected)
    }
    DisposableEffect(view) { view.onResume(); onDispose { view.onPause() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().background(Color.White), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(onClick = onClose) { Text("Back") }
                    Text("${angle.label} view", style = MaterialTheme.typography.titleMedium)
                    Row {
                        if (onEdit != null) TextButton(onClick = onUndo, enabled = canUndo) { Text("Undo") }
                        TextButton(onClick = { view.exportImage() }) { Text("Export") }
                        TextButton(onClick = { view.reset() }) { Text("Reset") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ViewerAngle.entries.forEach { candidate ->
                        FilterChip(selected = angle == candidate, onClick = { angle = candidate; view.setAngle(candidate); onAngleChange(candidate) }, label = { Text(candidate.label) })
                    }
                }
                val selectedName = selected?.let { mesh.objectNames.getOrNull(it) }
                val hint = when {
                    onEdit != null && angle.showsDepth && selectedName != null -> {
                        val o = selected!!
                        "$selectedName: front Z ${format(mesh.frontMm(o))}, back Z ${format(mesh.backMm(o))}, depth ${format(mesh.frontMm(o) - mesh.backMm(o))}. Drag the shape to move it in Z, the round handles to change its front or back, the dots to move points. Tap it again for exact values."
                    }
                    onEdit != null && angle.showsDepth -> "${if (angle == ViewerAngle.TOP) "Looking down: X across, front (Z+) at the bottom." else "From the right: Y up, front (Z+) on the left."} Tap a shape to edit its depth, or drag it to move it in Z. Drag empty space to pan, pinch to zoom."
                    angle.orthographic -> "Drag to pan, pinch to zoom. Tap an object to set its Z position and depth."
                    else -> "Drag to orbit, two fingers to pan and zoom. Tap an object to set its Z position and depth."
                }
                Text(hint, style = MaterialTheme.typography.bodySmall, color = Color(0xFF4D5A57), modifier = Modifier.padding(horizontal = 12.dp))
                if (mesh.warnings.isNotEmpty()) Text(mesh.warnings.joinToString("\n") + "\nExport stays blocked until this is fixed.", Modifier.fillMaxWidth().padding(top = 4.dp).background(Color(0xFFFFE3E0)).padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A1C12))
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = showMeasurements, onCheckedChange = { showMeasurements = it; view.showMeasurements = it; view.overlay.invalidate() }, modifier = Modifier.size(32.dp))
                        Text("Sizes", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = showArrow, onCheckedChange = { showArrow = it; view.setReferencesVisible(showFrame, showArrow) }, modifier = Modifier.size(32.dp))
                        Text("Front", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = showFrame, onCheckedChange = { showFrame = it; view.setReferencesVisible(showFrame, showArrow) }, modifier = Modifier.size(32.dp))
                        Text("Frame", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(48.dp)) {
                    Slider(value = sun, onValueChange = { sun = it; view.setSunPosition(it) }, onValueChangeFinished = { onSunChange(sun) }, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = Color.Transparent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
                    Icon(painterResource(R.drawable.ic_sun), contentDescription = null, tint = Color(0xFFE7A500), modifier = Modifier.offset(x = (maxWidth - 32.dp) * sun, y = 8.dp).size(32.dp))
                }
                // A rebuilt model creates a new GL view; key the host so it is actually swapped in.
                androidx.compose.runtime.key(view) {
                    AndroidView(factory = { FrameLayout(it).apply { addView(view); addView(view.overlay) } }, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}
