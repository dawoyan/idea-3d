package com.davoyans.idea3d

import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.opengl.GLSurfaceView
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import android.graphics.Paint
import android.opengl.Matrix
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.hypot

internal data class ViewerMeasurement(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val label: String, val color: Int, val z1: Float = 0f, val z2: Float = 0f)

/** Maps model millimeters to the viewer's normalized space. Reused across rebuilds so edits don't make the view jump. */
internal class ViewerFrame(val center: FloatArray, val extent: Float) {
    val mmPerUnit get() = extent / 2f
    fun toUnit(mm: Float, axis: Int) = (mm - center[axis]) / mmPerUnit
}

/** Orbit camera shared by successive models of one viewing session, so a rebuilt model keeps the angle, zoom and pan. */
internal class ViewerCamera {
    @Volatile var yaw = 30f
    @Volatile var pitch = 25f
    @Volatile var zoom = 1f
    @Volatile var panX = 0f
    @Volatile var panY = 0f
    fun reset() { yaw = 30f; pitch = 25f; zoom = 1f; panX = 0f; panY = 0f }
}

internal class ViewerMesh private constructor(
    val vertices: FloatBuffer, private val cpuVertices: FloatArray, val normals: FloatBuffer, val colors: FloatBuffer, val count: Int,
    val picturePlane: FloatBuffer, val frontArrow: FloatBuffer, val measurements: List<ViewerMeasurement>, val triangleObjects: IntArray, val objectNames: List<String>,
    val frame: ViewerFrame, val warnings: List<String>, val texCoords: FloatBuffer, val groups: List<Triple<Finish, Int, Int>>,
) {
    fun vertex(i: Int, axis: Int) = cpuVertices[i * 3 + axis]

    companion object {
        fun fromStl(
            bytes: ByteArray, triangleColors: List<Int>, measurements: List<ViewerMeasurement> = emptyList(), triangleObjects: IntArray = IntArray(0), objectNames: List<String> = emptyList(),
            warnings: List<String> = emptyList(), frame: ViewerFrame? = null, triangleFinishes: List<Finish> = emptyList(),
        ): ViewerMesh {
            val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            require(bytes.size >= 84) { "Invalid 3D model" }
            data.position(80)
            val count = data.int
            require(count > 0 && 84L + count.toLong() * 50L == bytes.size.toLong() && triangleColors.size == count) { "Invalid 3D model" }
            val vertices = FloatArray(count * 9)
            val normals = FloatArray(count * 9)
            val colors = FloatArray(count * 12)
            val texture = FloatArray(count * 6)
            fun finishOf(t: Int) = triangleFinishes.getOrNull(t) ?: Finish.PLAIN
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
                    colors[ci + 3] = if (finishOf(triangle) == Finish.GLASS) 0.38f else 1f
                }
                // Textures lie flat on each face: the two axes across its normal, repeating every 600 mm.
                val ax = kotlin.math.abs(nx); val ay = kotlin.math.abs(ny); val az = kotlin.math.abs(nz)
                repeat(3) { vertex ->
                    val index = triangle * 9 + vertex * 3
                    val x = vertices[index]; val y = vertices[index + 1]; val z = vertices[index + 2]
                    val (u, v) = when { ax >= ay && ax >= az -> z to y; ay >= az -> x to z; else -> x to y }
                    texture[triangle * 6 + vertex * 2] = u / 600f; texture[triangle * 6 + vertex * 2 + 1] = v / 600f
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
            // Triangles are drawn grouped by finish, glass last so what is behind it shows through.
            val order = (0 until count).sortedBy { t -> finishOf(t).let { if (it == Finish.GLASS) Int.MAX_VALUE else it.ordinal } }
            fun reorder(values: FloatArray, per: Int) = FloatArray(values.size).also { out -> order.forEachIndexed { i, t -> System.arraycopy(values, t * per, out, i * per, per) } }
            val objects = triangleObjects.takeIf { it.size == count } ?: IntArray(count) { -1 }
            val groups = mutableListOf<Triple<Finish, Int, Int>>()
            order.forEachIndexed { i, t -> val f = finishOf(t); if (groups.lastOrNull()?.first == f) groups[groups.lastIndex] = Triple(f, groups.last().second, groups.last().third + 3) else groups += Triple(f, i * 3, 3) }
            val sortedVertices = reorder(vertices, 9)
            return ViewerMesh(buffer(sortedVertices), sortedVertices, buffer(reorder(normals, 9)), buffer(reorder(colors, 12)), count * 3, buffer(plane), buffer(arrow), normalizedMeasurements,
                IntArray(count) { objects[order[it]] }, objectNames, space, warnings, buffer(reorder(texture, 6)), groups)
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
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, floatArrayOf(1f, 1f, 1f, 1f), 0)
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA)
        val ids = IntArray(3); gl.glGenTextures(3, ids, 0)
        listOf(Finish.METAL, Finish.WOOD, Finish.STONE).forEachIndexed { i, finish ->
            textures[finish] = ids[i]
            gl.glBindTexture(GL10.GL_TEXTURE_2D, ids[i])
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat()); gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT.toFloat()); gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT.toFloat())
            val bitmap = finishTexture(finish); android.opengl.GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0); bitmap.recycle()
        }
    }
    private val textures = HashMap<Finish, Int>()
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
        mesh.vertices.position(0); mesh.normals.position(0); mesh.colors.position(0); mesh.texCoords.position(0)
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mesh.vertices); gl.glNormalPointer(GL10.GL_FLOAT, 0, mesh.normals); gl.glColorPointer(4, GL10.GL_FLOAT, 0, mesh.colors)
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY); gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mesh.texCoords)
        mesh.groups.forEach { (finish, first, vertexCount) ->
            val texture = textures[finish]
            if (texture != null) { gl.glEnable(GL10.GL_TEXTURE_2D); gl.glBindTexture(GL10.GL_TEXTURE_2D, texture); gl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL10.GL_MODULATE.toFloat()) } else gl.glDisable(GL10.GL_TEXTURE_2D)
            // Metal and glass shine; wood and stone barely.
            val shine = when (finish) { Finish.METAL -> 0.9f; Finish.GLASS -> 0.8f; Finish.PLAIN -> 0.15f; else -> 0.05f }
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_SPECULAR, floatArrayOf(shine, shine, shine, 1f), 0)
            gl.glMaterialf(GL10.GL_FRONT_AND_BACK, GL10.GL_SHININESS, if (finish == Finish.METAL || finish == Finish.GLASS) 60f else 10f)
            if (finish == Finish.GLASS) { gl.glEnable(GL10.GL_BLEND); gl.glDepthMask(false) }
            gl.glDrawArrays(GL10.GL_TRIANGLES, first, vertexCount)
            if (finish == Finish.GLASS) { gl.glDisable(GL10.GL_BLEND); gl.glDepthMask(true) }
        }
        gl.glDisable(GL10.GL_TEXTURE_2D); gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY)
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

private class ModelView(context: Context, private val mesh: ViewerMesh, private val camera: ViewerCamera) : GLSurfaceView(context) {
    private val modelRenderer = ModelRenderer(mesh, camera)
    val overlay = OverlayView(context, ::drawOverlay)
    var showMeasurements = false
    var onObjectTap: ((Int) -> Unit)? = null
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var previousX = 0f; private var previousY = 0f; private var previousSpan = 0f; private var previousPointers = 0
    private var downX = 0f; private var downY = 0f; private var moved = false; private var multiTouch = false
    init { setEGLContextClientVersion(1); setRenderer(modelRenderer); renderMode = RENDERMODE_WHEN_DIRTY }

    fun reset() { camera.reset(); refresh() }
    private fun refresh() { requestRender(); overlay.invalidate() }

    private fun project(x: Float, y: Float, z: Float, combined: FloatArray = viewMatrix(camera, width, height)): FloatArray {
        val out = FloatArray(4); Matrix.multiplyMV(out, 0, combined, 0, floatArrayOf(x, y, z, 1f), 0)
        return floatArrayOf((out[0] / out[3] + 1f) * width / 2f, (1f - out[1] / out[3]) * height / 2f, out[2] / out[3])
    }

    /** Returns the object under a screen point, preferring the triangle nearest the viewer. */
    private fun pickObject(sx: Float, sy: Float): Int? {
        if (width == 0 || height == 0) return null
        val combined = viewMatrix(camera, width, height)
        fun project(i: Int) = project(mesh.vertex(i, 0), mesh.vertex(i, 1), mesh.vertex(i, 2), combined)
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
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> { previousPointers = 0; return true }
        }
        val count = event.pointerCount
        val x = if (count >= 2) (event.getX(0) + event.getX(1)) / 2f else event.x
        val y = if (count >= 2) (event.getY(0) + event.getY(1)) / 2f else event.y
        val span = if (count >= 2) hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1)) else 0f
        if (event.actionMasked == MotionEvent.ACTION_MOVE && hypot(event.x - downX, event.y - downY) > 12f * density) moved = true
        if (event.actionMasked == MotionEvent.ACTION_MOVE && previousPointers == count && (moved || count >= 2)) {
            if (count >= 2) {
                if (previousSpan > 1f) camera.zoom = (camera.zoom * span / previousSpan).coerceIn(0.2f, 5f)
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
        drawAxes(canvas)
        if (showMeasurements) drawMeasurements(canvas)
    }

    private fun drawMeasurements(canvas: Canvas) {
        val combined = viewMatrix(camera, width, height)
        mesh.measurements.forEach { line ->
            val a = project(line.x1, line.y1, line.z1, combined); val b = project(line.x2, line.y2, line.z2, combined)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f * density; paint.color = android.graphics.Color.WHITE
            canvas.drawLine(a[0], a[1], b[0], b[1], paint)
            paint.strokeWidth = 2f * density; paint.color = line.color; canvas.drawLine(a[0], a[1], b[0], b[1], paint)
            paint.style = Paint.Style.FILL; canvas.drawCircle(a[0], a[1], 3f * density, paint); canvas.drawCircle(b[0], b[1], 3f * density, paint)
            paint.textSize = 14f * density; paint.typeface = android.graphics.Typeface.DEFAULT_BOLD; paint.textAlign = Paint.Align.CENTER
            val x = (a[0] + b[0]) / 2f; val y = (a[1] + b[1]) / 2f - 4f * density
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f * density; paint.color = android.graphics.Color.WHITE; canvas.drawText(line.label, x, y, paint)
            paint.style = Paint.Style.FILL; paint.color = android.graphics.Color.BLACK; canvas.drawText(line.label, x, y, paint)
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

/**
 * The orbitable 3D view, shown in place of the drawing canvas. Drawing and reshaping happen in the Front, Top and
 * Profile planes; here one finger orbits, two fingers pan and zoom, and a tap opens an object's exact values.
 */
@Composable
internal fun ThreeDViewerPane(
    mesh: ViewerMesh,
    camera: ViewerCamera,
    onEditObject: (Int) -> Unit,
    detail: Int,
    onDetailChange: (Float) -> Unit,
    onDetailChangeFinished: () -> Unit,
    showDrawings: Boolean,
    onShowDrawingsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = remember(mesh) { ModelView(context, mesh, camera) }
    val figureOverlay = remember(mesh) { ScaleFigureOverlay(context, mesh, camera) }
    view.onObjectTap = onEditObject
    var showFrame by remember { mutableStateOf(false) }
    var showArrow by remember { mutableStateOf(false) }
    var showMeasurements by remember { mutableStateOf(false) }
    var showFigure by remember { mutableStateOf(false) }
    // Lighting is intentionally fixed: a quiet top-left sun makes surfaces legible
    // without turning the viewer into a lighting editor.
    LaunchedEffect(view) { view.setSunPosition(0.15f); view.setReferencesVisible(showFrame, showArrow); view.showMeasurements = showMeasurements }
    DisposableEffect(view) { view.onResume(); onDispose { view.onPause() } }
    Column(modifier) {
        // One slim row: what to show and the view actions.
        Row(Modifier.fillMaxWidth().background(Color(0xFFF2F5F4)).padding(horizontal = 6.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            CompactToggle("Sizes", showMeasurements) { showMeasurements = it; view.showMeasurements = it; view.overlay.invalidate() }
            CompactToggle("Drawings", showDrawings) { onShowDrawingsChange(it) }
            CompactToggle("Figure", showFigure) { showFigure = it }
            CompactToggle("Front", showArrow) { showArrow = it; view.setReferencesVisible(showFrame, showArrow) }
            CompactToggle("Frame", showFrame) { showFrame = it; view.setReferencesVisible(showFrame, showArrow) }
            CompactToggle("Reset", false) { view.reset() }
            CompactToggle("Save", false) { view.exportImage() }
        }
        // This occupies the old lighting-control position. Light stays fixed at
        // top-left; drag only when a smoother curved-member preview is needed.
        Row(Modifier.fillMaxWidth().background(Color(0xFFF2F5F4)).padding(horizontal = 12.dp).height(36.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_detail), contentDescription = "3D detail", tint = Color(0xFF00695C), modifier = Modifier.size(20.dp))
            Slider(
                value = detail.toFloat(),
                onValueChange = onDetailChange,
                onValueChangeFinished = onDetailChangeFinished,
                valueRange = 1f..12f,
                steps = 10,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
            Text(detail.toString(), fontSize = 12.sp, color = Color(0xFF00695C))
        }
        if (mesh.warnings.isNotEmpty()) Text(mesh.warnings.joinToString("\n"), Modifier.fillMaxWidth().background(Color(0xFFFFF4D6)).padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 12.sp, color = Color(0xFF5C4400))
        // A rebuilt model creates a new GL view. Figure visibility is updated in
        // the existing host: recreating it would try to parent the GL view twice.
        androidx.compose.runtime.key(view) {
            AndroidView(
                factory = { FrameLayout(it).apply { addView(view); addView(view.overlay) } },
                update = { host ->
                    host.removeView(figureOverlay)
                    if (showFigure) host.addView(figureOverlay)
                },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
        }
    }
}

/** A quiet, automatically chosen scale reference in the 3D corner. */
private class ScaleFigureOverlay(context: Context, mesh: ViewerMesh, private val camera: ViewerCamera) : View(context) {
    // Viewer vertices are normalized to -1..1 for OpenGL; the frame retains the
    // actual millimetre span and is therefore the only useful scale here.
    private val sizeMm = mesh.frame.extent
    private val kind = if (sizeMm >= 1200f) "Human" else if (sizeMm >= 400f) "German shepherd" else "Black-and-white cat"
    private val referenceMm = when (kind) { "Human" -> 1750f; "German shepherd" -> 650f; else -> 260f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    init { isClickable = false; isFocusable = false }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Match the apparent model scale: zooming the model also zooms this
        // reference. It remains tucked into a corner so it never hides faces.
        val h = (height * .45f * (referenceMm / sizeMm.coerceAtLeast(1f)) * camera.zoom).coerceIn(20f, height * .72f)
        val x = width * .06f; val bottom = height * .93f
        paint.textSize = 28f; paint.color = android.graphics.Color.DKGRAY; canvas.drawText(kind, x, bottom - h - 12f, paint)
        paint.color = android.graphics.Color.BLACK
        if (kind == "Human") { canvas.drawCircle(x + h*.16f, bottom-h*.82f, h*.09f, paint); canvas.drawRect(x+h*.11f, bottom-h*.73f, x+h*.21f, bottom-h*.3f, paint); canvas.drawRect(x+h*.04f, bottom-h*.3f, x+h*.11f, bottom, paint); canvas.drawRect(x+h*.21f, bottom-h*.3f, x+h*.28f, bottom, paint) }
        else { canvas.drawOval(x, bottom-h*.55f, x+h*.55f, bottom-h*.15f, paint); canvas.drawCircle(x+h*.58f, bottom-h*.48f, h*.14f, paint); canvas.drawRect(x+h*.1f, bottom-h*.2f, x+h*.18f, bottom, paint); canvas.drawRect(x+h*.4f, bottom-h*.2f, x+h*.48f, bottom, paint); if (kind.contains("cat")) { paint.color = android.graphics.Color.WHITE; canvas.drawOval(x+h*.18f, bottom-h*.47f, x+h*.42f, bottom-h*.2f, paint) } }
        postInvalidateOnAnimation()
    }
}

/** A small pill button: filled when [on]. Keeps the 3D controls on one row even on narrow phones. */
@Composable
private fun CompactToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Text(label, fontSize = 12.sp, color = if (on) Color.White else Color(0xFF00695C), maxLines = 1,
        modifier = Modifier.background(if (on) Color(0xFF00695C) else Color.Transparent, shape).border(1.dp, Color(0xFF00695C), shape)
            .clickable { onChange(!on) }.padding(horizontal = 8.dp, vertical = 5.dp))
}


/** Light grey patterns that tint a material's color: brushed metal, wood grain and speckled stone. */
private fun finishTexture(finish: Finish): Bitmap {
    val size = 256
    val random = java.util.Random(finish.ordinal * 7919L + 17)
    val pixels = IntArray(size * size)
    val streaks = FloatArray(size) { 0.86f + random.nextFloat() * 0.14f }
    for (y in 0 until size) for (x in 0 until size) {
        val shade = when (finish) {
            Finish.METAL -> (streaks[y] + (random.nextFloat() - 0.5f) * 0.05f)
            Finish.WOOD -> {
                // Rings bent by a slow wave, with fine fibres along the grain.
                val ring = kotlin.math.sin((y + 9f * kotlin.math.sin(x / 37f) + 4f * kotlin.math.sin(x / 11f)) / 5.5f)
                0.78f + 0.14f * ring * ring + (random.nextFloat() - 0.5f) * 0.05f
            }
            else -> 0.8f + random.nextFloat() * 0.2f - if (random.nextFloat() < 0.04f) 0.18f else 0f
        }.coerceIn(0f, 1f)
        val c = (shade * 255).toInt()
        pixels[y * size + x] = android.graphics.Color.rgb(c, c, c)
    }
    return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}
