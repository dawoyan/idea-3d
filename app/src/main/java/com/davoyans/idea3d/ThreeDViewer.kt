package com.davoyans.idea3d

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.provider.MediaStore
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
import kotlin.math.hypot
import kotlin.math.max

internal data class ViewerMeasurement(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val label: String, val color: Int, val z1: Float = 0f, val z2: Float = 0f)
internal enum class ViewerAngle(val yaw: Float, val pitch: Float, val label: String, val orthographic: Boolean) {
    FRONT(0f, 0f, "Front", true), TOP(0f, 90f, "Top", true), PROFILE(-90f, 0f, "Profile", true), THREE_D(30f, 25f, "3D", false)
}

internal class ViewerMesh private constructor(val vertices: FloatBuffer, val normals: FloatBuffer, val colors: FloatBuffer, val count: Int, val picturePlane: FloatBuffer, val frontArrow: FloatBuffer, val measurements: List<ViewerMeasurement>, val triangleObjects: IntArray, val objectNames: List<String>) {
    companion object {
        fun fromStl(bytes: ByteArray, triangleColors: List<Int>, measurements: List<ViewerMeasurement> = emptyList(), triangleObjects: IntArray = IntArray(0), objectNames: List<String> = emptyList()): ViewerMesh {
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
            val extent = (0..2).maxOf { high[it] - low[it] }.coerceAtLeast(0.00001f)
            val center = FloatArray(3) { (high[it] + low[it]) / 2f }
            fun normalized(value: Float, axis: Int) = (value - center[axis]) * 2f / extent
            vertices.indices.forEach { i -> vertices[i] = (vertices[i] - (high[i % 3] + low[i % 3]) / 2f) * 2f / extent }
            fun buffer(values: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(values); position(0) }
            val x0 = normalized(low[0], 0); val x1 = normalized(high[0], 0)
            val y0 = normalized(low[1], 1); val y1 = normalized(high[1], 1)
            val z0 = normalized(0f, 2)
            val plane = floatArrayOf(x0,y0,z0, x1,y0,z0, x1,y0,z0, x1,y1,z0, x1,y1,z0, x0,y1,z0, x0,y1,z0, x0,y0,z0)
            val arrow = floatArrayOf(x0,y0,z0, x0,y0,z0+0.35f, x0,y0,z0+0.35f, x0+0.08f,y0,z0+0.25f, x0,y0,z0+0.35f, x0-0.08f,y0,z0+0.25f)
            val normalizedMeasurements = measurements.map { it.copy(x1 = normalized(it.x1, 0), y1 = normalized(it.y1, 1), x2 = normalized(it.x2, 0), y2 = normalized(it.y2, 1), z1 = normalized(it.z1, 2), z2 = normalized(it.z2, 2)) }
            return ViewerMesh(buffer(vertices), buffer(normals), buffer(colors), count * 3, buffer(plane), buffer(arrow), normalizedMeasurements, triangleObjects.takeIf { it.size == count } ?: IntArray(count) { -1 }, objectNames)
        }
    }
}

private class ModelRenderer(private val mesh: ViewerMesh, initialAngle: ViewerAngle) : GLSurfaceView.Renderer {
    @Volatile var yaw = initialAngle.yaw
    @Volatile var pitch = initialAngle.pitch
    @Volatile var zoom = 1f
    @Volatile var panX = 0f
    @Volatile var panY = 0f
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
        gl.glLoadIdentity(); gl.glTranslatef(panX, panY, -20f)
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, floatArrayOf((sunPosition - 0.5f) * 8f, 3f, 2f, 0f), 0)
        gl.glScalef(zoom, zoom, zoom); gl.glRotatef(pitch, 1f, 0f, 0f); gl.glRotatef(yaw, 0f, 1f, 0f)
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

private class ModelView(context: Context, private val mesh: ViewerMesh, initialAngle: ViewerAngle) : GLSurfaceView(context) {
    private val modelRenderer = ModelRenderer(mesh, initialAngle)
    val measurementOverlay = MeasurementOverlay(context, mesh, modelRenderer)
    var angle = initialAngle; private set
    var onObjectTap: ((Int) -> Unit)? = null
    private var previousX = 0f; private var previousY = 0f; private var previousSpan = 0f; private var previousPointers = 0
    private var downX = 0f; private var downY = 0f; private var moved = false; private var multiTouch = false
    init { setEGLContextClientVersion(1); setRenderer(modelRenderer); renderMode = RENDERMODE_WHEN_DIRTY }
    fun reset() { modelRenderer.yaw = angle.yaw; modelRenderer.pitch = angle.pitch; modelRenderer.zoom = 1f; modelRenderer.panX = 0f; modelRenderer.panY = 0f; requestRender(); measurementOverlay.invalidate() }
    fun setAngle(next: ViewerAngle) { angle = next; modelRenderer.yaw = next.yaw; modelRenderer.pitch = next.pitch; requestRender(); measurementOverlay.invalidate() }

    /** Returns the object under a screen point, preferring the triangle nearest the viewer. */
    fun pickObject(sx: Float, sy: Float): Int? {
        if (width == 0 || height == 0) return null
        val combined = viewMatrix(modelRenderer, width, height)
        val v = FloatArray(mesh.count * 3); mesh.vertices.position(0); mesh.vertices.get(v); mesh.vertices.position(0)
        val out = FloatArray(4)
        fun project(i: Int): FloatArray { Matrix.multiplyMV(out, 0, combined, 0, floatArrayOf(v[i * 3], v[i * 3 + 1], v[i * 3 + 2], 1f), 0); return floatArrayOf((out[0] / out[3] + 1f) * width / 2f, (1f - out[1] / out[3]) * height / 2f, out[2] / out[3]) }
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; moved = false; multiTouch = false }
            MotionEvent.ACTION_POINTER_DOWN -> multiTouch = true
            MotionEvent.ACTION_UP -> {
                previousPointers = 0
                if (!moved && !multiTouch) pickObject(event.x, event.y)?.let { onObjectTap?.invoke(it) }
                return true
            }
            MotionEvent.ACTION_CANCEL -> { previousPointers = 0; return true }
            MotionEvent.ACTION_POINTER_UP -> { previousPointers = 0; return true }
        }
        val count = event.pointerCount
        val x = if (count >= 2) (event.getX(0) + event.getX(1)) / 2f else event.x
        val y = if (count >= 2) (event.getY(0) + event.getY(1)) / 2f else event.y
        val span = if (count >= 2) hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1)) else 0f
        if (event.actionMasked == MotionEvent.ACTION_MOVE && hypot(event.x - downX, event.y - downY) > 12f * resources.displayMetrics.density) moved = true
        if (event.actionMasked == MotionEvent.ACTION_MOVE && previousPointers == count && (moved || count >= 2)) {
            if (count >= 2 || angle.orthographic) {
                if (count >= 2 && previousSpan > 1f) modelRenderer.zoom = (modelRenderer.zoom * span / previousSpan).coerceIn(0.2f, 5f)
                val ratio = width.toFloat() / height.coerceAtLeast(1)
                val screenScale = (if (ratio < 1f) 4f / ratio.coerceAtLeast(0.01f) else 4f) / height.coerceAtLeast(1)
                modelRenderer.panX += (x - previousX) * screenScale
                modelRenderer.panY -= (y - previousY) * screenScale
            } else {
                modelRenderer.yaw += (x - previousX) * 0.5f
                modelRenderer.pitch += (y - previousY) * 0.5f
            }
            requestRender()
            measurementOverlay.invalidate()
        }
        previousX = x; previousY = y; previousSpan = span; previousPointers = count
        return true
    }
}

private fun viewMatrix(renderer: ModelRenderer, width: Int, height: Int): FloatArray {
    val ratio = width.toFloat() / height.coerceAtLeast(1)
    val halfHeight = if (ratio < 1f) 2f / ratio.coerceAtLeast(0.01f) else 2f
    val projection = FloatArray(16); val model = FloatArray(16); val combined = FloatArray(16)
    Matrix.orthoM(projection, 0, -halfHeight * ratio, halfHeight * ratio, -halfHeight, halfHeight, 1f, 100f)
    Matrix.setIdentityM(model, 0); Matrix.translateM(model, 0, renderer.panX, renderer.panY, -20f)
    Matrix.scaleM(model, 0, renderer.zoom, renderer.zoom, renderer.zoom)
    Matrix.rotateM(model, 0, renderer.pitch, 1f, 0f, 0f); Matrix.rotateM(model, 0, renderer.yaw, 0f, 1f, 0f)
    Matrix.multiplyMM(combined, 0, projection, 0, model, 0)
    return combined
}

private class MeasurementOverlay(context: Context, private val mesh: ViewerMesh, private val renderer: ModelRenderer) : View(context) {
    var showMeasurements = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: android.graphics.Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val density = resources.displayMetrics.density
        drawAxes(canvas, density)
        if (!showMeasurements) return
        val combined = viewMatrix(renderer, width, height)
        fun project(x: Float, y: Float, z: Float): Pair<Float, Float> {
            val out = FloatArray(4); Matrix.multiplyMV(out, 0, combined, 0, floatArrayOf(x, y, z, 1f), 0)
            return (out[0] / out[3] + 1f) * width / 2f to (1f - out[1] / out[3]) * height / 2f
        }
        mesh.measurements.forEach { line ->
            val (x1, y1) = project(line.x1, line.y1, line.z1); val (x2, y2) = project(line.x2, line.y2, line.z2)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f * density; paint.color = android.graphics.Color.WHITE
            canvas.drawLine(x1, y1, x2, y2, paint)
            paint.strokeWidth = 2f * density; paint.color = line.color; canvas.drawLine(x1, y1, x2, y2, paint)
            paint.style = Paint.Style.FILL; canvas.drawCircle(x1, y1, 3f * density, paint); canvas.drawCircle(x2, y2, 3f * density, paint)
            paint.textSize = 14f * density; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
            val x = (x1 + x2) / 2f; val y = (y1 + y2) / 2f - 4f * density
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f * density; paint.color = android.graphics.Color.WHITE; canvas.drawText(line.label, x, y, paint)
            paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.BLACK; canvas.drawText(line.label, x, y, paint)
        }
    }

    /** Corner gizmo with the same X right, Y up, Z toward-viewer convention used in the editor and exports. */
    private fun drawAxes(canvas: android.graphics.Canvas, density: Float) {
        val rotation = FloatArray(16); Matrix.setIdentityM(rotation, 0)
        Matrix.rotateM(rotation, 0, renderer.pitch, 1f, 0f, 0f); Matrix.rotateM(rotation, 0, renderer.yaw, 0f, 1f, 0f)
        val origin = 56f * density to height - 56f * density
        val length = 24f * density
        val axes = listOf(Triple("X", floatArrayOf(1f, 0f, 0f, 0f), 0xFFD32F2F.toInt()), Triple("Y", floatArrayOf(0f, 1f, 0f, 0f), 0xFF2E7D32.toInt()), Triple("Z", floatArrayOf(0f, 0f, 1f, 0f), 0xFF1565C0.toInt()))
        val out = FloatArray(4)
        axes.map { (name, axis, color) -> Matrix.multiplyMV(out, 0, rotation, 0, axis, 0); Triple(name, floatArrayOf(out[0], out[1], out[2]), color) }
            .sortedBy { it.second[2] }
            .forEach { (name, dir, color) ->
                val ex = origin.first + dir[0] * length; val ey = origin.second - dir[1] * length
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 2.5f * density; paint.color = color
                if (kotlin.math.abs(dir[0]) + kotlin.math.abs(dir[1]) < 0.05f) {
                    // Axis points straight at or away from the viewer.
                    canvas.drawCircle(origin.first, origin.second, 5f * density, paint)
                    if (dir[2] > 0) { paint.style = Paint.Style.FILL; canvas.drawCircle(origin.first, origin.second, 2f * density, paint) }
                } else canvas.drawLine(origin.first, origin.second, ex, ey, paint)
                paint.style = Paint.Style.FILL; paint.textSize = 12f * density; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
                val lx = origin.first + dir[0] * (length + 9f * density); val ly = origin.second - dir[1] * (length + 9f * density) + 4f * density
                canvas.drawText(if (kotlin.math.abs(dir[0]) + kotlin.math.abs(dir[1]) < 0.05f) "$name${if (dir[2] > 0) "⊙" else "⊗"}" else name, if (kotlin.math.abs(dir[0]) + kotlin.math.abs(dir[1]) < 0.05f) origin.first + 14f * density else lx, if (kotlin.math.abs(dir[0]) + kotlin.math.abs(dir[1]) < 0.05f) origin.second - 8f * density else ly, paint)
            }
    }
}

@Composable
internal fun ThreeDViewerDialog(mesh: ViewerMesh, initialAngle: ViewerAngle, initialSun: Float, onSunChange: (Float) -> Unit, onAngleChange: (ViewerAngle) -> Unit = {}, onEditObject: (Int) -> Unit = {}, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = remember(mesh) { ModelView(context, mesh, initialAngle) }
    var angle by remember(mesh) { mutableStateOf(initialAngle) }
    view.onObjectTap = onEditObject
    var showFrame by remember { mutableStateOf(false) }
    var showArrow by remember { mutableStateOf(false) }
    var showMeasurements by remember { mutableStateOf(false) }
    var sun by remember(mesh) { mutableStateOf(initialSun.coerceIn(0f, 1f)) }
    LaunchedEffect(view) { view.setSunPosition(sun) }
    DisposableEffect(view) { view.onResume(); onDispose { view.onPause() } }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().background(Color.White), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onClose) { Text("Back") }
                    Text("${angle.label} view", modifier = Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { view.exportImage() }) { Text("Export") }
                    TextButton(onClick = { view.reset() }) { Text("Reset") }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ViewerAngle.entries.forEach { candidate ->
                        FilterChip(selected = angle == candidate, onClick = { angle = candidate; view.setAngle(candidate); onAngleChange(candidate) }, label = { Text(candidate.label) })
                    }
                }
                Text(if (angle.orthographic) "Drag to pan, pinch to zoom. Tap an object to set its Z position and depth." else "Drag to orbit, two fingers to pan and zoom. Tap an object to set its Z position and depth.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4D5A57), modifier = Modifier.padding(horizontal = 12.dp))
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = showMeasurements, onCheckedChange = { showMeasurements = it; view.measurementOverlay.showMeasurements = it; view.measurementOverlay.invalidate() }, modifier = Modifier.size(32.dp))
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
                    AndroidView(factory = { FrameLayout(it).apply { addView(view); addView(view.measurementOverlay) } }, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}
