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
import com.davoyans.idea3d.core.MeshIssue
import com.davoyans.idea3d.core.ModelExport
import com.davoyans.idea3d.core.OutlinePoint
import com.davoyans.idea3d.core.SolidSpec
import com.davoyans.idea3d.core.Vec3

private enum class Tool { NONE, CALIBRATE, MEASURE, LINES, ADJUST, CROP }
private enum class LineDrawing { STRAIGHT, BROKEN, CURVE }
private enum class PointKind { BREAKPOINT, CURVE }
private enum class CropEdge { LEFT, TOP, RIGHT, BOTTOM }
private enum class LengthSource { CALCULATED, MANUAL, MEASURED, UNKNOWN }
private data class Pt(val x: Float, val y: Float)
private data class CropFrame(val left: Float, val top: Float, val right: Float, val bottom: Float)
private data class FeatureSnap(val point: Pt, val found: Boolean)
private data class ScreenLine(val id: Int, val start: Offset, val end: Offset)
private data class MeasureLine(val id: Int, val start: Pt, val end: Pt, val value: Double, val isReference: Boolean, val label: String, val colorIndex: Int = 0, val isScaleReference: Boolean = false, val labelOffset: Float = 0f, val pathId: Int? = null, val segmentIndex: Int = 0, val lengthSource: LengthSource = LengthSource.CALCULATED, val startZMm: Double = 0.0, val endZMm: Double = 0.0, val axis: String = "", val plane: ModelPlane = ModelPlane.FRONT)
private fun MeasureLine.hasDepth() = abs(startZMm) > 0.000001 || abs(endZMm) > 0.000001
private data class GuideLine(val id: Int, val start: Pt, val end: Pt, val manual: Boolean, val pathId: Int? = null, val colorIndex: Int = 0, val strokeWidth: Float = 5f, val curveMid: Pt? = null, val fillTransparency: Int? = null, val groupId: Int? = null, val lengthMm: Double? = null, val shapeWidthMm: Double? = null, val shapeHeightMm: Double? = null, val startZMm: Double = 0.0, val endZMm: Double = 0.0, val startThicknessMm: Double? = null, val endThicknessMm: Double? = null, val plane: ModelPlane = ModelPlane.FRONT)
private data class AlignmentReference(val guide: Boolean, val id: Int, val point: Pt, val description: String)
private data class SelectedAlignmentObject(val guideIds: Set<Int>, val lineIds: Set<Int>, val points: List<Pt>)
private data class Extrusion(val id: Int, val guideIds: List<Int>, val origin: Pt, val handle: Pt, val vanishing: Pt, val sun: Pt = vanishing, val depthMm: Double? = null, val positionZMm: Double = 0.0, val plane: ModelPlane = ModelPlane.FRONT)
private enum class JunctionKind { SPLIT_MEASUREMENT, SPLIT_GUIDE, JOIN_MEASUREMENTS, JOIN_GUIDES, JOIN_MEASURE_ENDPOINTS, JOIN_GUIDE_ENDPOINTS, MERGE_SHAPE_GUIDE, CLOSE_GUIDE_PATH, SHAPE_OVERLAP, CUT_SHAPE_LINE }
private data class JunctionAction(val kind: JunctionKind, val firstId: Int, val secondId: Int, val point: Pt)
private data class JunctionArm(val lineId: Int, val endpoint: Pt, val color: Color)
private data class UndoSnapshot(val image: Bitmap?, val lines: List<MeasureLine>, val guides: List<GuideLine>, val guidesDetected: Boolean, val rotation: Float, val unit: String, val tool: Tool, val extrusions: List<Extrusion> = emptyList(), val plane: ModelPlane = ModelPlane.FRONT, val stash: Map<ModelPlane, PlaneGeometry> = emptyMap())
/** Geometry of a drawing plane that is not the one being edited; items keep their [ModelPlane] tag. */
private data class PlaneGeometry(val lines: List<MeasureLine> = emptyList(), val guides: List<GuideLine> = emptyList(), val extrusions: List<Extrusion> = emptyList())
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
private data class SavedProject(val id: String, val name: String, val unit: String, val lines: List<MeasureLine>, val guides: List<GuideLine> = emptyList(), val guidesDetected: Boolean = false, val capturedAt: Long, val rotation: Float = 0f, val options: PictureOptions = PictureOptions(), val extrusions: List<Extrusion> = emptyList(), val blankScene: Boolean = false)

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
        }) }
        val extrusions = JSONArray()
        project.extrusions.forEach { e -> extrusions.put(JSONObject().apply { put("id", e.id); put("guides", JSONArray(e.guideIds)); put("ox", e.origin.x); put("oy", e.origin.y); put("hx", e.handle.x); put("hy", e.handle.y); put("vx", e.vanishing.x); put("vy", e.vanishing.y); put("sunX", e.sun.x); put("sunY", e.sun.y); e.depthMm?.let { put("depthMm", it) }; put("positionZMm", e.positionZMm); if (e.plane != ModelPlane.FRONT) put("plane", e.plane.name) }) }
        File(directory(context), "${project.id}.json").writeText(JSONObject().apply { put("id", project.id); put("name", project.name); put("unit", project.unit); put("capturedAt", project.capturedAt); put("rotation", project.rotation); put("lines", values); put("guides", guides); put("guidesDetected", project.guidesDetected); put("extrusions", extrusions); put("figure", project.options.figure?.name ?: ""); put("sum", project.options.sum); put("legend", project.options.legend); put("guess", project.options.guess); put("transparency", project.options.transparency); put("blankScene", project.blankScene) }.toString())
    }
    private fun trash(context: Context) = File(directory(context), "trash").apply { mkdirs() }
    private fun projectFiles(dir: File, id: String) = listOf("$id.json", "$id.jpg", "$id-thumb.png").map { File(dir, it) }
    /** Moves a project aside so the delete can be undone; only the most recent deletion is kept. */
    fun moveToTrash(context: Context, id: String) {
        trash(context).listFiles()?.forEach { it.delete() }
        projectFiles(directory(context), id).zip(projectFiles(trash(context), id)).forEach { (from, to) -> if (from.isFile) from.renameTo(to) }
    }
    fun restoreFromTrash(context: Context, id: String) {
        projectFiles(trash(context), id).zip(projectFiles(directory(context), id)).forEach { (from, to) -> if (from.isFile) from.renameTo(to) }
    }
    fun delete(context: Context, id: String) { File(directory(context), "$id.json").delete(); imageFile(context, id).delete(); thumbnailFile(context, id).delete() }
    fun exportProject(context: Context, id: String, uri: Uri) {
        val json = File(directory(context), "$id.json")
        val image = imageFile(context, id)
        require(json.isFile && image.isFile) { "Project files are missing" }
        ZipOutputStream(context.contentResolver.openOutputStream(uri) ?: error("Cannot open destination")).use { zip ->
            listOf("project.json" to json, "image" to image).forEach { (name, file) ->
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
                        else -> error("Unexpected project entry")
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
            save(context, decode(data, System.currentTimeMillis()))
        } catch (error: Exception) {
            delete(context, id)
            throw error
        }
    }
    private fun read(file: File): SavedProject = decode(JSONObject(file.readText()), file.lastModified())
    private fun decode(json: JSONObject, fallbackTime: Long): SavedProject {
        val values = json.getJSONArray("lines")
        val lines = buildList { for (index in 0 until values.length()) { val line = values.getJSONObject(index); val fixed = line.optBoolean("scaleReference", line.getBoolean("reference")); val source = LengthSource.entries.firstOrNull { it.name == line.optString("lengthSource") } ?: if (fixed) LengthSource.MANUAL else LengthSource.CALCULATED; add(MeasureLine(line.getInt("id"), Pt(line.getDouble("sx").toFloat(), line.getDouble("sy").toFloat()), Pt(line.getDouble("ex").toFloat(), line.getDouble("ey").toFloat()), line.getDouble("value"), line.getBoolean("reference"), line.optString("label"), line.optInt("color"), fixed, line.optDouble("labelOffset", 0.0).toFloat(), line.takeIf { it.has("pathId") }?.getInt("pathId"), line.optInt("segmentIndex", 0), source, line.optDouble("startZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, line.optDouble("endZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, CalibrationAxis.of(line.optString("axis")).key, ModelPlane.of(line.optString("plane")))) } }
        val guideValues = json.optJSONArray("guides") ?: JSONArray()
        val guides = buildList { for (index in 0 until guideValues.length()) { val guide = guideValues.getJSONObject(index); add(GuideLine(guide.getInt("id"), Pt(guide.getDouble("sx").toFloat(), guide.getDouble("sy").toFloat()), Pt(guide.getDouble("ex").toFloat(), guide.getDouble("ey").toFloat()), guide.optBoolean("manual", false), guide.takeIf { it.has("pathId") }?.getInt("pathId"), guide.optInt("color", 0), guide.optDouble("width", 5.0).toFloat(), if (guide.has("curveX") && guide.has("curveY")) Pt(guide.getDouble("curveX").toFloat(), guide.getDouble("curveY").toFloat()) else null, guide.optInt("fillTransparency", -1).takeIf { it in listOf(20, 50, 90) }, guide.takeIf { it.has("groupId") }?.getInt("groupId"), guide.takeIf { it.has("lengthMm") }?.optDouble("lengthMm")?.takeIf { it.isFinite() && it > 0 }, guide.takeIf { it.has("shapeWidthMm") }?.optDouble("shapeWidthMm")?.takeIf { it.isFinite() && it > 0 }, guide.takeIf { it.has("shapeHeightMm") }?.optDouble("shapeHeightMm")?.takeIf { it.isFinite() && it > 0 }, guide.optDouble("startZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, guide.optDouble("endZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, guide.takeIf { it.has("startThicknessMm") }?.optDouble("startThicknessMm")?.takeIf { it.isFinite() && it > 0 }, guide.takeIf { it.has("endThicknessMm") }?.optDouble("endThicknessMm")?.takeIf { it.isFinite() && it > 0 }, ModelPlane.of(guide.optString("plane")))) } }
        val figure = ComparisonFigure.entries.firstOrNull { it.name == json.optString("figure") } ?: if (!json.has("figure") && json.optBoolean("silhouette")) ComparisonFigure.STANDING else null
        val transparency = json.optInt("transparency", 0).takeIf { it in listOf(0, 10, 90, 100) } ?: 0
        val extrusionValues = json.optJSONArray("extrusions") ?: JSONArray()
        val extrusions = buildList { for (index in 0 until extrusionValues.length()) { val item = extrusionValues.getJSONObject(index); val ids = item.getJSONArray("guides"); add(Extrusion(item.getInt("id"), (0 until ids.length()).map { ids.getInt(it) }, Pt(item.getDouble("ox").toFloat(), item.getDouble("oy").toFloat()), Pt(item.getDouble("hx").toFloat(), item.getDouble("hy").toFloat()), Pt(item.getDouble("vx").toFloat(), item.getDouble("vy").toFloat()), Pt(item.optDouble("sunX", item.getDouble("vx") * 0.3).toFloat(), item.optDouble("sunY", item.getDouble("vy")).toFloat()), item.takeIf { it.has("depthMm") }?.optDouble("depthMm")?.takeIf { it.isFinite() && it >= 0 }, item.optDouble("positionZMm", 0.0).takeIf { it.isFinite() } ?: 0.0, ModelPlane.of(item.optString("plane")))) } }
        return SavedProject(json.getString("id"), json.getString("name"), json.optString("unit", "mm"), lines, guides, json.optBoolean("guidesDetected", false), json.optLong("capturedAt", fallbackTime), json.optDouble("rotation", 0.0).toFloat(), PictureOptions(figure, json.optBoolean("sum"), json.optBoolean("legend"), json.optBoolean("guess"), transparency), extrusions, json.optBoolean("blankScene", false))
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
    var showPictureEdit by remember { mutableStateOf(false) }
    var showPointDepth by remember { mutableStateOf(false) }
    var viewerModel by remember { mutableStateOf<ViewerMesh?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    var viewerObjects by remember { mutableStateOf<List<Pair<ModelPlane, Int>>>(emptyList()) }
    var viewerHandlePoints by remember { mutableStateOf<List<Pt>>(emptyList()) }
    var viewerUndoBase by remember { mutableIntStateOf(0) }
    var viewerEditObject by remember { mutableStateOf<Int?>(null) }
    var viewerAngle by remember { mutableStateOf(ViewerAngle.THREE_D) }
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
    fun planeGeometry(plane: ModelPlane) = if (plane == editPlane) PlaneGeometry(lines.map { it.copy(plane = plane) }, guides.map { it.copy(plane = plane) }, extrusions.map { it.copy(plane = plane) }) else planeStash[plane] ?: PlaneGeometry()
    fun allLines() = ModelPlane.entries.flatMap { planeGeometry(it).lines }
    fun allGuides() = ModelPlane.entries.flatMap { planeGeometry(it).guides }
    fun allExtrusions() = ModelPlane.entries.flatMap { planeGeometry(it).extrusions }
    // Only the photo can be calibrated, so Top and Profile reuse the front references; their pixels are uniform.
    fun scaleLines(input: List<MeasureLine>) = if (editPlane == ModelPlane.FRONT) input else input + planeGeometry(ModelPlane.FRONT).lines.filter { it.isReference }
    fun frontAxes() = if (blankScene) null else axisScaleOf(planeGeometry(ModelPlane.FRONT).lines, unit)
    // Top and Profile have no photo; they draw on a blank canvas of the same size.
    val planeCanvas = remember(bitmap, editPlane == ModelPlane.FRONT || blankScene) {
        bitmap?.let { image -> if (editPlane == ModelPlane.FRONT || blankScene) image else Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565).apply { eraseColor(AndroidColor.WHITE) } }
    }
    var ghosts by remember { mutableStateOf<List<Pair<Int, FloatArray>>>(emptyList()) }
    fun recalculate(input: List<MeasureLine>): List<MeasureLine> {
        val nextScale = if (blankScene) unitFactorMm(unit) else recomputeScale(scaleLines(input), unit)
        val nextAxes = if (blankScene || editPlane != ModelPlane.FRONT) null else axisScaleOf(input, unit)
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
        val snapshot = UndoSnapshot(if (includeImage) bitmap?.copy(Bitmap.Config.ARGB_8888, true) else null, lines, guides, guidesDetected, levelRotation, unit, tool, extrusions, editPlane, planeStash)
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
            CropEdge.RIGHT -> frame.copy(right = point.x.coerceIn(frame.left + minSize, bitmap?.width?.toFloat() ?: frame.right))
            CropEdge.TOP -> frame.copy(top = point.y.coerceIn(0f, frame.bottom - minSize))
            CropEdge.BOTTOM -> frame.copy(bottom = point.y.coerceIn(frame.top + minSize, bitmap?.height?.toFloat() ?: frame.bottom))
            null -> frame
        }
    }
    fun finishCropDrag() { activeCropEdge = null }
    fun applyCrop() {
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
        cropFrame = CropFrame(0f, 0f, rotated.width.toFloat(), rotated.height.toFloat()); zoom = 1f; pan = Offset.Zero
    }
    fun expandCanvas(left: Int, top: Int, right: Int, bottom: Int) {
        val source = bitmap ?: return
        val frame = cropFrame ?: return
        val width = source.width.toLong() + left + right; val height = source.height.toLong() + top + bottom
        if (width > 32767 || height > 32767 || width * height > 40_000_000L) {
            Toast.makeText(context, "Canvas is too large. Use smaller margins.", Toast.LENGTH_LONG).show(); return
        }
        val expanded = try {
            Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888).also { output ->
                AndroidCanvas(output).apply { drawColor(AndroidColor.WHITE); drawBitmap(source, left.toFloat(), top.toFloat(), null) }
            }
        } catch (_: OutOfMemoryError) {
            Toast.makeText(context, "Not enough memory for that canvas size.", Toast.LENGTH_LONG).show(); return
        }
        fun moved(point: Pt) = Pt(point.x + left, point.y + top)
        bitmap = expanded
        lines = lines.map { it.copy(start = moved(it.start), end = moved(it.end)) }
        guides = guides.map { it.copy(start = moved(it.start), end = moved(it.end), curveMid = it.curveMid?.let(::moved)) }
        extrusions = extrusions.map { it.copy(origin = moved(it.origin), handle = moved(it.handle), vanishing = moved(it.vanishing), sun = moved(it.sun)) }
        cropFrame = CropFrame(frame.left, frame.top, frame.right + left + right, frame.bottom + top + bottom)
        selectedVertices = emptyList(); zoom = 1f; pan = Offset.Zero; showCanvasSize = false
    }
    fun cancelCrop() {
        cropSession?.let { snapshot -> bitmap = snapshot.image; lines = recalculate(snapshot.lines); guides = snapshot.guides; guidesDetected = snapshot.guidesDetected; levelRotation = snapshot.rotation; unit = snapshot.unit; tool = snapshot.tool; extrusions = snapshot.extrusions }
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
            unit = defaultUnit; editPlane = ModelPlane.FRONT; planeStash = emptyMap(); lines = emptyList(); guides = emptyList(); guidesDetected = false; extrusions = emptyList(); selectedExtrusionId = null; scale = null; referenceCount = 0; draftStart = null; pendingEnd = null; pictureOptions = PictureOptions()
            tool = Tool.NONE; zoom = 1f; pan = Offset.Zero; levelRotation = 0f; undoHistory = emptyList(); selectedLineId = null; selectedEndpoint = null
            ProjectStore.save(context, SavedProject(id, imageDisplayName(context, uri).ifBlank { "Picture ${library.size + 1}" }, unit, emptyList(), emptyList(), false, projectCapturedAt, levelRotation))
            library = ProjectStore.list(context)
    }
    fun newScene() {
        val id = UUID.randomUUID().toString()
        val image = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888).apply { eraseColor(AndroidColor.WHITE) }
        ProjectStore.imageFile(context, id).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        blankScene = true; bitmap = image; projectId = id; projectName = "Scene ${library.count { it.blankScene } + 1}"; projectCapturedAt = System.currentTimeMillis()
        unit = defaultUnit; editPlane = ModelPlane.FRONT; planeStash = emptyMap(); lines = emptyList(); guides = emptyList(); guidesDetected = true; extrusions = emptyList(); selectedExtrusionId = null
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
            result.onSuccess { library = ProjectStore.list(context); Toast.makeText(context, "Project imported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Could not import project: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        }
    }
    val projectExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val id = projectExportId; projectExportId = null
        if (uri != null && id != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { ProjectStore.exportProject(context, id, uri) } }
            result.onSuccess { Toast.makeText(context, "Project exported", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Could not export project: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
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
            result.onSuccess { staged.delete(); Toast.makeText(context, "${extension.uppercase()} model exported", Toast.LENGTH_LONG).show() }
                .onFailure { Toast.makeText(context, "3D export failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show() }
        } else staged.delete()
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
    LaunchedEffect(projectId, projectName, lines, guides, guidesDetected, unit, projectCapturedAt, levelRotation, pictureOptions, extrusions, blankScene, editPlane, planeStash) {
        if (blankScene) scale = unitFactorMm(unit)
        val front = planeGeometry(ModelPlane.FRONT)
        projectId?.let { id -> ProjectStore.save(context, SavedProject(id, projectName, unit, allLines(), allGuides(), guidesDetected, projectCapturedAt, levelRotation, pictureOptions, allExtrusions(), blankScene)); bitmap?.let { ProjectStore.writeThumbnail(context, id, it, front.guides, front.lines) }; library = ProjectStore.list(context) }
    }
    // Objects from the other planes, flattened into this plane's canvas so new shapes can be lined up with them.
    LaunchedEffect(projectId, editPlane, planeStash, scale, unit, bitmap) {
        val image = bitmap ?: return@LaunchedEffect
        val others = ModelPlane.entries.filter { it != editPlane }.map { planeGeometry(it) }
        val plane = editPlane; val pixels = scale; val axes = frontAxes(); val modelUnit = unit
        ghosts = withContext(Dispatchers.Default) { runCatching {
            val mapping = planeMapping(plane, image.width, image.height, pixels, modelUnit, axes) ?: return@runCatching emptyList()
            buildSolidSpecs(others.flatMap { it.guides }, others.flatMap { it.extrusions }, pixels, modelUnit, image.width, image.height, axes, strict = false).mapNotNull { spec -> runCatching { MeshBuilder.extrude(spec) }.getOrNull() }.map { mesh ->
                val triangles = ArrayList<Float>()
                mesh.triangles.forEach { t ->
                    val a = mapping.pixel(mesh.vertices[t.a]); var b = mapping.pixel(mesh.vertices[t.b]); var c = mapping.pixel(mesh.vertices[t.c])
                    val area = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
                    if (abs(area) < 0.01f) return@forEach
                    // One winding for every triangle, so the filled union has no holes where faces overlap.
                    if (area < 0) { val swap = b; b = c; c = swap }
                    triangles.addAll(listOf(a.x, a.y, b.x, b.y, c.x, c.y))
                }
                mesh.argb to triangles.toFloatArray()
            }
        }.getOrDefault(emptyList()) }
    }
    fun handleTap(point: Pt, hitRadius: Double) {
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
                    selectedGuideId = guide?.id
                    guideDeleteCandidateId = guide?.id
                }
            }
            Tool.ADJUST -> {
                panOnEmptyDrag = false
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
                selectedExtrusionId?.let { id -> extrusions.firstOrNull { it.id == id }?.let { extrusion ->
                    if (distance(point, extrusion.vanishing) <= hitRadius * 2 && distance(point, extrusion.vanishing) < distance(point, extrusion.handle)) return
                } }
                extrusions.firstOrNull { distance(point, depthHandle(it, it.handle)) <= hitRadius * 2 }?.let { selectedExtrusionId = it.id; selectedEndpoint = null; selectedGuideEndpoint = null; selectedWholeLineId = null; selectedWholeGuideId = null; selectedLabelId = null; return }
                selectedExtrusionId = null
                val priorLineId = selectedWholeLineId ?: selectedEndpoint?.lineId ?: selectedLineId.takeIf { selectedGuideId == null }
                val priorGuideId = selectedWholeGuideId ?: selectedGuideEndpoint?.guideId ?: selectedGuideId.takeIf { selectedWholeLineId == null && selectedEndpoint == null }
                val preferredEndpoint = priorLineId?.let { id -> closestEndpoint(point, lines.filter { it.id == id }, hitRadius) }
                val preferredGuideEndpoint = priorGuideId?.let { id -> closestGuideEndpoint(point, guides.filter { it.id == id }, hitRadius) }
                val endpoint = preferredEndpoint ?: if (preferredGuideEndpoint == null) closestEndpoint(point, lines, hitRadius) else null
                val guideEndpoint = preferredGuideEndpoint ?: if (preferredEndpoint == null) closestGuideEndpoint(point, guides, hitRadius) else null
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
                if (draftStart == null) {
                    activePolylineId = nextGuidePathId(guides)
                    draftStart = point
                } else if (distance(draftStart!!, point) > 4.0) {
                    pushUndo()
                    guides = guides + GuideLine(nextGuideId(guides), draftStart!!, point, manual = true, pathId = activePolylineId, colorIndex = guideColorIndex, strokeWidth = guideStrokeWidth)
                    draftStart = point
                }
            } else if (draftStart == null) {
                val selected = closestGuide(point, guides, hitRadius * 0.7)
                if (selected != null) {
                    selectedGuideId = selected.id
                    guideDeleteCandidateId = selected.id
                } else {
                    selectedGuideId = null
                    guideDeleteCandidateId = null
                    draftStart = point
                }
            } else {
                if (distance(draftStart!!, point) > 4.0) {
                    pushUndo()
                    val start = draftStart!!
                    val guide = GuideLine(nextGuideId(guides), start, point, manual = true, colorIndex = guideColorIndex, strokeWidth = guideStrokeWidth, curveMid = if (lineDrawing == LineDrawing.CURVE) Pt((start.x + point.x) / 2f, (start.y + point.y) / 2f) else null)
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
        val guide = (if (targetId != null) guides.firstOrNull { it.id == targetId } else closestGuide(point, guides, radius * 2)) ?: return
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
    fun cloneGuide(id: Int) {
        val image = bitmap ?: return
        val original = guides.firstOrNull { it.id == id } ?: return
        val source = guides.filter { sameGuideObject(it, original) }
        if (source.isEmpty()) return
        val shift = min(image.width, image.height) * 0.03f
        val maxX = source.maxOf { max(it.start.x, it.end.x) }
        val maxY = source.maxOf { max(it.start.y, it.end.y) }
        val dx = if (maxX + shift <= image.width) shift else -shift
        val dy = if (maxY + shift <= image.height) shift else -shift
        var next = nextGuideId(guides)
        var nextPath = nextGuidePathId(guides)
        val paths = source.mapNotNull { it.pathId }.distinct().associateWith { nextPath++ }
        val group = if (original.groupId != null) (guides.mapNotNull { it.groupId }.maxOrNull() ?: 0) + 1 else null
        val copies = source.map { guide -> guide.copy(id = next++, start = Pt(guide.start.x + dx, guide.start.y + dy), end = Pt(guide.end.x + dx, guide.end.y + dy), curveMid = guide.curveMid?.let { Pt(it.x + dx, it.y + dy) }, manual = true, pathId = guide.pathId?.let { paths[it] }, groupId = group) }
        pushUndo(); guides = guides + copies
        selectedExtrusionId = null; selectedGuideEndpoint = null; selectedWholeGuideId = copies.first().id; selectedGuideId = copies.first().id
    }
    fun handleDragStart(sourcePoint: Pt, hitRadius: Double) {
        if (tool == Tool.ADJUST && selectedVertices.isNotEmpty() && selectedVertices.any { distance(sourcePoint, it) <= hitRadius * 1.5 }) {
            groupDragLast = sourcePoint; pushUndo(); return
        }
        if (tool == Tool.ADJUST && selectedWholeGuideId != null) {
            val selected = guides.firstOrNull { it.id == selectedWholeGuideId }
            val objectGuides = selected?.let { guide -> guides.filter { sameGuideObject(it, guide) } }.orEmpty()
            val shape = closedGuideShapes(guides).firstOrNull { path -> selected != null && sameGuideObject(path.first(), selected) }
            if (objectGuides.any { distance(sourcePoint, projectToGuide(sourcePoint, it)) <= hitRadius * 1.7 } || shape != null && pointInsidePolygon(sourcePoint, shapePoints(shape))) {
                activeWholeGuideId = selectedWholeGuideId; activeWholeGuideLastPoint = sourcePoint; pushUndo(); return
            }
        }
        if (tool == Tool.ADJUST && closestEndpoint(sourcePoint, lines, hitRadius) == null && closestGuideEndpoint(sourcePoint, guides, hitRadius) == null && closestLine(sourcePoint, lines)?.let { distanceToLine(sourcePoint, it) <= hitRadius } != true && closestGuide(sourcePoint, guides, hitRadius) == null && selectedExtrusionId == null) {
            selectionStart = sourcePoint; selectionEnd = sourcePoint; return
        }
        if (tool == Tool.LINES || tool == Tool.ADJUST) {
            val curve = guides.firstOrNull { it.id == selectedGuideId && it.curveMid != null && distance(sourcePoint, it.curveMid) <= hitRadius * 1.5 }
            if (curve != null) { activeCurveId = curve.id; if (tool == Tool.ADJUST) { selectedCurveId = curve.id; selectedExtrusionId = null; selectedGuideEndpoint = null; selectedWholeGuideId = null }; curveDragLast = sourcePoint; pushUndo(); return }
        }
        if (tool != Tool.ADJUST) return
        selectedExtrusionId?.let { id ->
            val extrusion = extrusions.firstOrNull { it.id == id } ?: return
            val handle = depthHandle(extrusion, extrusion.handle)
            if (distance(sourcePoint, extrusion.vanishing) <= hitRadius * 2 && distance(sourcePoint, extrusion.vanishing) < distance(sourcePoint, handle)) {
                vanishingDragLast = sourcePoint; pushUndo()
            } else if (distance(sourcePoint, handle) <= hitRadius * 2) { draggingExtrusion = true; pushUndo() }
            return
        }
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
                val groupId = existingGroups.firstOrNull() ?: ((guides.mapNotNull { it.groupId }.maxOrNull() ?: 0) + 1)
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
                scale = if (blankScene) unitFactorMm(unit) else recomputeScale(scaleLines(lines), unit)
                axisScale = if (blankScene || editPlane != ModelPlane.FRONT) null else axisScaleOf(lines, unit)
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
    fun applyShapeOperation(action: JunctionAction, operation: String) {
        val shapes = closedGuideShapes(guides)
        val first = shapes.firstOrNull { shape -> shape.any { it.id == action.firstId } } ?: return
        val second = shapes.firstOrNull { shape -> shape.any { it.id == action.secondId } }
        val cutter = guides.firstOrNull { it.id == action.secondId }
        val target = if (operation == "cut-first") second else first
        val razor = if (operation == "cut-first") first else second
        val result = when (operation) {
            "merge" -> second?.let { shapeBoolean(first, it, android.graphics.Path.Op.UNION) }
            "cut-first", "cut-second" -> if (target != null && razor != null) shapeBoolean(target, razor, android.graphics.Path.Op.DIFFERENCE) else null
            "line" -> if (cutter != null) cutShapeByLine(first, cutter, bitmap?.width ?: 1, bitmap?.height ?: 1) else null
            else -> null
        }
        if (result.isNullOrEmpty()) {
            Toast.makeText(context, "Cannot create editable shapes from this cut.", Toast.LENGTH_LONG).show()
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
        val ids = guides.filter { sameGuideObject(it, selected) }.map { it.id }.toSet()
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
        val objects = selectedAlignmentObjects()
        if (objects.size >= 2) { alignSelectedObjects(horizontal, objects); return }
        val refs = alignmentReferences()
        if (refs.size > 1) pendingAlignment = horizontal else alignSelectedVertices(horizontal, refs.firstOrNull())
    }
    fun handleDrag(point: Pt, snappedToFeature: Boolean) {
        if (selectionStart != null) { selectionEnd = point; return }
        groupDragLast?.let { previous ->
            val dx = point.x - previous.x; val dy = point.y - previous.y
            transformSelectedVertices { Pt(it.x + dx, it.y + dy) }
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
        if (tool == Tool.ADJUST) activePoint?.let { endpoint ->
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
        if (tool == Tool.ADJUST) activeGuideEndpoint?.let { endpoint ->
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
        val start = selectionStart; val end = selectionEnd
        if (start != null && end != null) {
            val x1 = min(start.x, end.x); val x2 = max(start.x, end.x); val y1 = min(start.y, end.y); val y2 = max(start.y, end.y)
            selectedVertices = (lines.flatMap { listOf(it.start, it.end) } + guides.flatMap { listOf(it.start, it.end) }).filter { it.x in x1..x2 && it.y in y1..y2 }.fold(emptyList()) { acc, p -> if (acc.any { distance(it, p) < 0.6 }) acc else acc + p }
            selectedEndpoint = null; selectedGuideEndpoint = null; selectedWholeLineId = null; selectedWholeGuideId = null; selectedCurveId = null; selectedExtrusionId = null; selectedGuideId = null; selectedLabelId = null
        }
        selectionStart = null; selectionEnd = null; groupDragLast = null; activeCurveId = null; curveDragLast = null; draggingExtrusion = false; vanishingDragLast = null; activePoint = null; activeGuideEndpoint = null; activeGuideVertexPoint = null; activeWholeLineId = null; activeWholeLineLastPoint = null; activeWholeGuideId = null; activeWholeGuideLastPoint = null; magnifierPoint = null
    }
    fun handleDragCancel() { selectionStart = null; selectionEnd = null; handleDragEnd() }
    fun nudgeSelected(dx: Float, dy: Float) {
        if (selectedVertices.isNotEmpty()) { pushUndo(); transformSelectedVertices { Pt(it.x + dx, it.y + dy) }; return }
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
        val stash = planeStash.toMutableMap()
        stash[editPlane] = planeGeometry(editPlane)
        val loaded = stash.remove(next) ?: PlaneGeometry()
        planeStash = stash.filterValues { it != PlaneGeometry() }; editPlane = next
        guides = loaded.guides; extrusions = loaded.extrusions; lines = recalculate(loaded.lines)
        referenceCount = lines.count { it.isReference }
        selectedExtrusionId = null; selectedCurveId = null; selectedVertices = emptyList(); selectedLineId = null; selectedEndpoint = null; selectedGuideId = null; selectedGuideEndpoint = null
        selectedWholeGuideId = null; selectedWholeLineId = null; selectedLabelId = null; draftStart = null; pendingEnd = null; activePolylineId = null; activeMeasurePathId = null; deleteCandidateId = null
        if (tool == Tool.CALIBRATE || tool == Tool.CROP) tool = Tool.NONE
        zoom = 1f; pan = Offset.Zero
    }

    fun restoreLastUndo() {
        val previous = undoHistory.lastOrNull() ?: return
        previous.image?.let { restored ->
            bitmap = restored
            projectId?.let { id -> ProjectStore.imageFile(context, id).outputStream().use { restored.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
        editPlane = previous.plane; planeStash = previous.stash
        lines = recalculate(previous.lines).map { line -> previous.lines.firstOrNull { it.id == line.id && (it.lengthSource == LengthSource.MANUAL || it.lengthSource == LengthSource.MEASURED) }?.let { line.copy(value = it.value) } ?: line }; guides = previous.guides; guidesDetected = previous.guidesDetected; extrusions = previous.extrusions; selectedExtrusionId = selectedExtrusionId?.takeIf { id -> extrusions.any { it.id == id } }; levelRotation = previous.rotation; unit = previous.unit; tool = previous.tool; referenceCount = lines.count { it.isReference }
        undoHistory = undoHistory.dropLast(1)
    }

    /** Replaces one plane's guides and objects, whether it is the plane being edited or a stashed one. */
    fun setPlaneShapes(plane: ModelPlane, nextGuides: List<GuideLine>, nextExtrusions: List<Extrusion>) {
        if (plane == editPlane) { guides = nextGuides; extrusions = nextExtrusions }
        else planeStash = planeStash + (plane to (planeStash[plane] ?: PlaneGeometry()).copy(guides = nextGuides.map { it.copy(plane = plane) }, extrusions = nextExtrusions.map { it.copy(plane = plane) }))
    }

    /** Builds the viewer model from every plane. An open viewer keeps its frame so edits don't move the camera; [revertOnFailure] undoes an edit the model can't show. */
    fun open3dView(angle: ViewerAngle, revertOnFailure: Boolean = false) {
        val guideSnapshot = allGuides(); val extrusionSnapshot = allExtrusions(); val modelScale = scale; val modelAxes = frontAxes(); val modelUnit = unit; val measurementSnapshot = allLines().filterNot { it.isReference }; val openedProject = projectId
        val width = bitmap?.width ?: 1; val height = bitmap?.height ?: 1
        val frame = viewerModel?.frame
        scope.launch {
            val result = withContext(Dispatchers.Default) { runCatching {
                val colors = mutableListOf<Int>()
                val objects = mutableListOf<Extrusion>()
                val specs = buildSolidSpecs(guideSnapshot, extrusionSnapshot, modelScale, modelUnit, width, height, modelAxes, objects)
                // The viewer shows intersecting objects (with a warning) so they can be moved apart; export still refuses them.
                val issues = mutableListOf<MeshIssue>()
                val meshes = specs.mapNotNull { spec -> try { MeshBuilder.extrude(spec).also { issues += MeshBuilder.validate(it) } } catch (e: MeshException) { issues += e.issues; null } }
                if (issues.any { it.blocking }) throw MeshException(issues)
                val warnings = MeshBuilder.overlapIssues(specs).map { it.toString() }
                meshes.forEach { mesh -> repeat(mesh.triangles.size) { colors.add(mesh.argb) } }
                val stl = ModelExport.stl(meshes)
                val triangleObjects = meshes.flatMapIndexed { index, mesh -> List(mesh.triangles.size) { index } }.toIntArray()
                // Extruded meshes list the viewer-facing vertices first, then the matching far ones.
                val vertexBack = meshes.flatMap { mesh -> val n = mesh.vertices.size / 2; mesh.triangles.flatMap { t -> listOf(t.a >= n, t.b >= n, t.c >= n) } }.toBooleanArray()
                fun mapping(plane: ModelPlane) = planeMapping(plane, width, height, modelScale, modelUnit, modelAxes)!!
                val handlePoints = mutableListOf<Pt>()
                val handles = objects.flatMapIndexed { index, extrusion ->
                    val edges = extrusionGuides(extrusion, guideSnapshot.filter { it.plane == extrusion.plane })
                    val points = mutableListOf<Pair<Pt, Double>>()
                    edges.forEach { edge -> listOf(edge.start to edge.startZMm, edge.end to edge.endZMm).forEach { (p, z) -> if (points.none { distance(it.first, p) < 0.5 }) points += p to z } }
                    points.map { (p, z) -> handlePoints += p; val v = mapping(extrusion.plane).model(p, facePositionMm(extrusion) + z); ViewerHandle(index, v.x.toFloat(), v.y.toFloat(), v.z.toFloat()) }
                }
                val measurements = measurementSnapshot.map { line ->
                    val value = if (line.lengthSource == LengthSource.UNKNOWN) "?" else "${formatMeasure(line.value, modelUnit)}${if (line.hasDepth()) "*" else ""} $modelUnit"
                    val a = mapping(line.plane).model(line.start, line.startZMm); val b = mapping(line.plane).model(line.end, line.endZMm)
                    ViewerMeasurement(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), listOf(line.label, value).filter { it.isNotBlank() }.joinToString(": "), measurementColor(line).toArgb(), a.z.toFloat(), b.z.toFloat())
                }
                val mesh = ViewerMesh.fromStl(stl, colors, measurements, triangleObjects, meshes.map { it.name }, vertexBack, handles, specs.map { spec -> spec.outline.minOf { it.thicknessMm }.toFloat() }, warnings, frame, specs.map { it.plane.normalAxis.ordinal })
                Triple(mesh, objects.map { it.plane to it.id }, handlePoints.toList())
            } }
            if (projectId != openedProject) return@launch
            result.onSuccess { (mesh, ids, points) ->
                if (viewerModel == null) viewerUndoBase = undoHistory.size
                viewerAngle = angle; viewerModel = mesh; viewerObjects = ids; viewerHandlePoints = points
            }.onFailure { error ->
                if (revertOnFailure && undoHistory.size > viewerUndoBase) {
                    restoreLastUndo()
                    Toast.makeText(context, "Change not applied: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    open3dView(angle)
                } else Toast.makeText(context, "3D view: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Applies a drag from a flat viewer view along an object's depth axis, as one undo step. */
    fun applyViewerEdit(edit: ViewerEdit) {
        val (plane, id) = viewerObjects.getOrNull(edit.objectIndex) ?: run { open3dView(viewerAngle); return }
        val geometry = planeGeometry(plane)
        val extrusion = geometry.extrusions.firstOrNull { it.id == id }
        val depth = extrusion?.depthMm
        if (extrusion == null || depth == null) { open3dView(viewerAngle); return }
        val d = edit.deltaMm
        val face = facePositionMm(extrusion)
        val edgeIds = extrusionGuides(extrusion, geometry.guides).map { it.id }.toSet()
        pushUndo()
        var nextGuides = geometry.guides
        // The far side is the facing surface minus thickness, so changing only one side changes thickness.
        fun thicker(by: Double) { nextGuides = nextGuides.map { g -> if (g.id !in edgeIds) g else g.copy(startThicknessMm = g.startThicknessMm?.plus(by), endThicknessMm = g.endThicknessMm?.plus(by)) } }
        val next = when (edit.kind) {
            ViewerEditKind.MOVE -> extrusion.copy(positionZMm = extrusion.positionZMm + d)
            ViewerEditKind.FRONT -> { thicker(d); withFacePosition(extrusionWithDepth(extrusion, depth + d, scale, unit), face + d, depth + d) }
            ViewerEditKind.BACK -> { thicker(-d); withFacePosition(extrusionWithDepth(extrusion, depth - d, scale, unit), face, depth - d) }
            ViewerEditKind.POINTS -> {
                val points = edit.handles.mapNotNull { viewerHandlePoints.getOrNull(it) }
                fun near(p: Pt) = points.any { distance(it, p) < 0.5 }
                nextGuides = nextGuides.map { g -> if (g.id !in edgeIds) g else g.copy(startZMm = if (near(g.start)) g.startZMm + d else g.startZMm, endZMm = if (near(g.end)) g.endZMm + d else g.endZMm) }
                extrusion
            }
        }
        setPlaneShapes(plane, nextGuides, geometry.extrusions.map { if (it.id == id) next else it })
        open3dView(viewerAngle, revertOnFailure = true)
    }

    BackHandler(enabled = presentation || bitmap != null || showSettings) {
        when {
            presentation -> setPresentation(false)
            showSettings -> showSettings = false
            showMeasurements -> showMeasurements = false
            bitmap != null -> {
                bitmap = null; projectId = null; projectName = ""; selectedLineId = null; selectedVertices = emptyList()
            }
        }
    }

    if (presentation && bitmap != null) {
        Box(Modifier.fillMaxSize()) {
            ImageEditor(
                modifier = Modifier.fillMaxSize(), bitmap = planeCanvas!!, lines = lines, guides = guides, ghosts = ghosts, selectedGuideId = selectedGuideId, unit = unit, drawingScale = scale, draftStart = draftStart, pendingEnd = pendingEnd, tool = tool,
                selectedEndpoint = selectedEndpoint, magnifierPoint = selectedPoint ?: magnifierPoint, deleteCandidateId = deleteCandidateId, alignmentLineIds = alignmentLineIds, alignmentFlashToken = alignmentFlashToken, edgeSnapPoint = edgeSnapPoint, edgeFlashToken = edgeFlashToken, adjustmentIndicatorPoint = adjustmentIndicatorPoint, adjustmentLineId = adjustmentLineId, adjustmentGuideId = adjustmentGuideId, adjustmentFlashToken = adjustmentFlashToken, guideColorIndex = guideColorIndex, guideStrokeWidth = guideStrokeWidth, rotation = if (editPlane == ModelPlane.FRONT) levelRotation else 0f, unitVisibility = unitVisibility, zoom = zoom, pan = pan, cropFrame = cropFrame, pictureOptions = pictureOptions, extrusions = extrusions, selectedExtrusionId = selectedExtrusionId, onTripleTap = { point, radius -> activateExtrusion(point, radius) },
                onTransform = { nextZoom, nextPan -> zoom = nextZoom; pan = nextPan },
                onDoubleTap = { setPresentation(false) }, doubleTapEnabled = true,
                onJunction = { junctionAction = it }, onAddPoint = ::addGuidePoint, onTap = ::handleTap, onLongPress = ::handleLongPress, onDragStart = ::handleDragStart, onDrag = ::handleDrag, onDragEnd = ::handleDragEnd, onDragCancel = ::handleDragCancel, onCropStart = ::startCrop, onCropDrag = ::dragCrop, onCropEnd = ::finishCropDrag,
                selectedCurveId = selectedCurveId, selectedLabelId = selectedLabelId, selectedWholeLineId = selectedWholeLineId, selectedGuideEndpoint = selectedGuideEndpoint, selectedWholeGuideId = selectedWholeGuideId, selectedVertices = selectedVertices, selectionStart = selectionStart, selectionEnd = selectionEnd, panOnEmptyDrag = panOnEmptyDrag, showFineControls = tool == Tool.ADJUST && (selectedVertices.isNotEmpty() || selectedCurveId != null || selectedExtrusionId != null || selectedEndpoint != null || selectedLabelId != null || selectedWholeLineId != null || selectedGuideEndpoint != null || selectedWholeGuideId != null), onNudge = ::nudgeSelected
            )
            IconButton(onClick = { setPresentation(false) }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).background(Color(0xCC17211E), CircleShape)) {
                Icon(painterResource(R.drawable.ic_fullscreen_exit), contentDescription = "Exit full screen", tint = Color.White)
            }
        }
    } else if (showMeasurements && bitmap != null) {
        MeasurementsScreen(lines = lines, unit = unit, sumEnabled = pictureOptions.sum, onBack = { showMeasurements = false }, onEdit = { line -> selectedLineId = line.id; showLineEdit = true })
    } else if (showSettings) {
        SettingsScreen(
            currentUnit = defaultUnit, colorIndex = defaultColor, unitVisibility = unitVisibility, exportGuides = exportGuides, hasProject = bitmap != null,
            onUnit = { next -> defaultUnit = next; preferences.edit().putString("default-unit", next).apply() },
            onPrecision = { measureUnit, places -> MeasurementPrecision.places[measureUnit] = places; preferences.edit().putInt("precision-$measureUnit", places).apply() },
            onColor = { next -> defaultColor = next; preferences.edit().putInt("default-color", next).apply() },
            onUnitVisibility = { next -> unitVisibility = next; preferences.edit().putString("unit-visibility", next).apply() },
            onExportGuides = { enabled -> exportGuides = enabled; preferences.edit().putBoolean("export-guides", enabled).apply() },
            onExportLog = { if (bitmap != null) { exportMeasurementLog(context, lines, scale, unit); Toast.makeText(context, "Measurement log saved", Toast.LENGTH_SHORT).show() } },
            onExportBoschLog = { exportBoschDiagnosticLog(context); Toast.makeText(context, "Bosch diagnostic log saved", Toast.LENGTH_SHORT).show() },
            onBack = { showSettings = false }
        )
    } else Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { if (bitmap != null) TopAppBar(title = {
            if (tool != Tool.CROP) OutlinedButton(onClick = { showPictureEdit = true }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp)) {
                Text(projectName.ifBlank { "Idea 3D" }, fontWeight = FontWeight.Bold, letterSpacing = 0.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }, actions = {
            if (tool == Tool.CROP) {
                TextButton(onClick = ::cancelCrop) { Text("Cancel") }
                IconButton(onClick = { rotateCrop(clockwise = false) }) { Icon(painterResource(R.drawable.ic_rotate_left), contentDescription = "Rotate left 90 degrees") }
                IconButton(onClick = { rotateCrop(clockwise = true) }) { Icon(painterResource(R.drawable.ic_rotate_right), contentDescription = "Rotate right 90 degrees") }
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
            if (tool == Tool.ADJUST && selectedVertices.size >= 2) {
                IconButton(onClick = { requestAlignment(true) }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_align_horizontal), contentDescription = "Align points horizontally") }
                IconButton(onClick = { requestAlignment(false) }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_align_vertical), contentDescription = "Align points vertically") }
                IconButton(onClick = { showPointDepth = true }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_cube), contentDescription = "Move selected points in 3D") }
            }
            if (tool == Tool.ADJUST && panOnEmptyDrag) IconButton(onClick = { panOnEmptyDrag = false }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_select_area), contentDescription = "Select points in an area") }
            if (tool == Tool.ADJUST && selectedExtrusionId != null) IconButton(onClick = { extrusionEditId = selectedExtrusionId }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_cube), contentDescription = "Edit 3D depth") }
            if (tool == Tool.ADJUST && selectedGuideId != null && selectedVertices.isEmpty()) {
                IconButton(onClick = { guideLengthEditId = selectedGuideId }, modifier = Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_measure_straight), contentDescription = "Set line length") }
                IconButton(onClick = { pointKind = PointKind.BREAKPOINT }, modifier = Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_point_break), contentDescription = "Add breakpoint", tint = if (pointKind == PointKind.BREAKPOINT) Color(0xFF006D67) else Color.Gray) }
                IconButton(onClick = { pointKind = PointKind.CURVE }, modifier = Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_point_curve), contentDescription = "Add curve point", tint = if (pointKind == PointKind.CURVE) Color(0xFF006D67) else Color.Gray) }
                if (selectedGuideEndpoint != null || selectedCurveId != null) IconButton(onClick = ::deleteSelectedGuidePoint, modifier = Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_delete_point), contentDescription = "Delete selected point", tint = Color(0xFFB3261E)) }
            }
            if (tool == Tool.ADJUST && selectedGuideEndpoint == null && selectedCurveId == null) selectedWholeGuideId?.let { id ->
                IconButton(onClick = { cloneGuide(id) }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_clone), contentDescription = "Clone guide") }
                IconButton(onClick = { guides.firstOrNull { it.id == id }?.let { guide -> val image = bitmap ?: return@let; activateExtrusion(Pt((guide.start.x + guide.end.x) / 2f, (guide.start.y + guide.end.y) / 2f), min(image.width, image.height) * 0.04, id) } }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_cube), contentDescription = "Add 3D depth") }
            }
            selectedGuideId?.let { id ->
                if (guides.firstOrNull { it.id == id }?.let { selected -> closedGuideShapes(guides).any { path -> sameGuideObject(path.first(), selected) } } == true) {
                    IconButton(onClick = { shapeEditId = id }, modifier = Modifier.size(40.dp)) { Icon(painterResource(R.drawable.ic_edit_shape), contentDescription = "Edit shape") }
                }
            }
            deleteCandidateId?.let { id ->
                Button(onClick = { deleteMeasurement(id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Delete", fontSize = 12.sp) }
            }
            guideDeleteCandidateId?.let { id ->
                if (tool != Tool.ADJUST) IconButton(onClick = { guideLengthEditId = id }, modifier = Modifier.size(36.dp)) { Icon(painterResource(R.drawable.ic_measure_straight), contentDescription = "Set line length") }
                Button(onClick = { deleteGuide(id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)), contentPadding = PaddingValues(horizontal = 10.dp)) { Text("Delete", fontSize = 12.sp) }
            }
            if (editPlane == ModelPlane.FRONT && selectedVertices.isEmpty() && (tool != Tool.ADJUST || selectedGuideId == null)) IconButton(onClick = {
                bitmap?.let { image -> cropSession = UndoSnapshot(image.copy(Bitmap.Config.ARGB_8888, true), lines, guides, guidesDetected, levelRotation, unit, tool, extrusions); cropFrame = CropFrame(0f, 0f, image.width.toFloat(), image.height.toFloat()); tool = Tool.CROP; draftStart = null; pendingEnd = null }
            }) { Icon(painterResource(R.drawable.ic_crop), contentDescription = "Crop picture") }
            if (tool != Tool.ADJUST && selectedVertices.isEmpty()) Box {
                IconButton(onClick = { showViewMenu = true }, modifier = Modifier.size(48.dp)) { Icon(painterResource(R.drawable.ic_viewpoints), contentDescription = "View options") }
                DropdownMenu(expanded = showViewMenu, onDismissRequest = { showViewMenu = false }) {
                    DropdownMenuItem(text = { Text("Front view") }, onClick = { showViewMenu = false; open3dView(ViewerAngle.FRONT) })
                    DropdownMenuItem(text = { Text("Top view") }, onClick = { showViewMenu = false; open3dView(ViewerAngle.TOP) })
                    DropdownMenuItem(text = { Text("Profile view") }, onClick = { showViewMenu = false; open3dView(ViewerAngle.PROFILE) })
                    DropdownMenuItem(text = { Text("Full view") }, onClick = { showViewMenu = false; setPresentation(true) })
                    DropdownMenuItem(text = { Text("3D view") }, onClick = { showViewMenu = false; open3dView(ViewerAngle.THREE_D) })
                }
            }
            }
        }) },
        bottomBar = { if (bitmap != null && tool == Tool.CROP) Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(8.dp), horizontalArrangement = Arrangement.Center) { OutlinedButton(onClick = { showCanvasSize = true }) { Text("Canvas size") } }
        else if (bitmap != null) BottomControls(editPlane == ModelPlane.FRONT, tool, lineDrawing, measureDrawing, (activePolylineId != null || activeMeasurePathId != null) && draftStart != null, guideColorIndex, guideStrokeWidth, unknownSize, onUnknownSize = { unknownSize = it; preferences.edit().putBoolean("unknown-size", it).apply() }, onTool = { panOnEmptyDrag = false; selectedVertices = emptyList(); selectedCurveId = null; tool = it; draftStart = null; pendingEnd = null; activePolylineId = null; activeMeasurePathId = null; deleteCandidateId = null; guideDeleteCandidateId = null }, onFinishBroken = ::finishBrokenGuide, onLineDrawing = { lineDrawing = it }, onMeasureDrawing = { measureDrawing = it }, onGuideStyle = { color, width -> guideColorIndex = color; guideStrokeWidth = width }) }
    ) { padding ->
        if (bitmap == null) LibraryScreen(Modifier.padding(padding), library, { picker.launch("image/*") }, ::newScene, { projectPicker.launch(arrayOf("application/zip", "application/octet-stream")) }, { showSettings = true }, { project -> renameTarget = project }, { project ->
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
                blankScene = project.blankScene; bitmap = loaded; projectId = project.id; projectName = project.name; projectCapturedAt = project.capturedAt; levelRotation = project.rotation; undoHistory = emptyList(); unit = project.unit; editPlane = ModelPlane.FRONT
                planeStash = ModelPlane.entries.filter { it != ModelPlane.FRONT }.associateWith { plane -> PlaneGeometry(project.lines.filter { it.plane == plane }, project.guides.filter { it.plane == plane }, project.extrusions.filter { it.plane == plane }) }.filterValues { it != PlaneGeometry() }
                lines = project.lines.filter { it.plane == ModelPlane.FRONT }; guides = project.guides.filter { it.plane == ModelPlane.FRONT }; guidesDetected = project.guidesDetected; extrusions = project.extrusions.filter { it.plane == ModelPlane.FRONT }; selectedExtrusionId = null; selectedCurveId = null; pictureOptions = project.options; referenceCount = project.lines.count { it.isReference && it.plane == ModelPlane.FRONT }; scale = if (project.blankScene) unitFactorMm(project.unit) else recomputeScale(project.lines.filter { it.plane == ModelPlane.FRONT }, project.unit); axisScale = if (project.blankScene) null else axisScaleOf(project.lines.filter { it.plane == ModelPlane.FRONT }, project.unit); tool = Tool.NONE; zoom = 1f; pan = Offset.Zero
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
            StatusStrip(tool, scale, lines.count { !it.isReference }, referenceCount, drawingEnd = draftStart != null, onMeasurements = { showMeasurements = true })
            PlaneSwitcher(editPlane, onPlane = ::switchPlane)
            if (!blankScene && editPlane == ModelPlane.FRONT) CalibrationBanner(remember(lines, unit) { calibrationOf(lines, unit) })
            Box(Modifier.fillMaxWidth().weight(1f)) {
            ImageEditor(
                modifier = Modifier.fillMaxSize(),
                bitmap = planeCanvas!!, lines = lines, guides = guides, ghosts = ghosts, selectedGuideId = selectedGuideId, unit = unit, drawingScale = scale, draftStart = draftStart, pendingEnd = pendingEnd, tool = tool, zoom = zoom, pan = pan,
                selectedEndpoint = selectedEndpoint, magnifierPoint = selectedPoint ?: magnifierPoint, deleteCandidateId = deleteCandidateId, alignmentLineIds = alignmentLineIds, alignmentFlashToken = alignmentFlashToken, edgeSnapPoint = edgeSnapPoint, edgeFlashToken = edgeFlashToken, adjustmentIndicatorPoint = adjustmentIndicatorPoint, adjustmentLineId = adjustmentLineId, adjustmentGuideId = adjustmentGuideId, adjustmentFlashToken = adjustmentFlashToken, guideColorIndex = guideColorIndex, guideStrokeWidth = guideStrokeWidth, rotation = if (editPlane == ModelPlane.FRONT) levelRotation else 0f, unitVisibility = unitVisibility, cropFrame = cropFrame, pictureOptions = pictureOptions, extrusions = extrusions, selectedExtrusionId = selectedExtrusionId, onTripleTap = { point, radius -> activateExtrusion(point, radius) },
                onTransform = { nextZoom, nextPan -> zoom = nextZoom; pan = nextPan }, onDoubleTap = { }, doubleTapEnabled = false,
                onJunction = { junctionAction = it }, onAddPoint = ::addGuidePoint, onTap = ::handleTap, onLongPress = ::handleLongPress, onDragStart = ::handleDragStart, onDrag = ::handleDrag, onDragEnd = ::handleDragEnd, onDragCancel = ::handleDragCancel, onCropStart = ::startCrop, onCropDrag = ::dragCrop, onCropEnd = ::finishCropDrag,
                selectedCurveId = selectedCurveId, selectedLabelId = selectedLabelId, selectedWholeLineId = selectedWholeLineId, selectedGuideEndpoint = selectedGuideEndpoint, selectedWholeGuideId = selectedWholeGuideId, selectedVertices = selectedVertices, selectionStart = selectionStart, selectionEnd = selectionEnd, panOnEmptyDrag = panOnEmptyDrag, showFineControls = tool == Tool.ADJUST && (selectedVertices.isNotEmpty() || selectedCurveId != null || selectedExtrusionId != null || selectedEndpoint != null || selectedLabelId != null || selectedWholeLineId != null || selectedGuideEndpoint != null || selectedWholeGuideId != null), onNudge = ::nudgeSelected
            )
            }
            if (tool == Tool.CALIBRATE) LevelDragger(rotation = levelRotation, onRotation = { levelRotation = it }, onAuto = {
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
                } else Toast.makeText(context, "Add two parallel-side references and one connecting reference", Toast.LENGTH_SHORT).show()
            })
            // The end-point hint lives in the status strip so the canvas never shifts mid-gesture.
            if (tool == Tool.CALIBRATE) CalibrationList(lines, unit, Modifier.fillMaxWidth().heightIn(max = 96.dp)) { line -> selectedLineId = line.id; showLineEdit = true }
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
        onConfirm = { entered, label, enteredUnit, levelOnly, fromBosch, axis ->
            val ref = tool == Tool.CALIBRATE
            val storedValue = entered?.times(unitFactorMm(enteredUnit))?.div(unitFactorMm(unit)) ?: 0.0
            val pathId = if (!ref && pendingBrokenSegment) activeMeasurePathId else null
            val line = MeasureLine(nextId(lines), draftStart!!, pendingEnd!!, storedValue, ref, label, colorIndex = if (!ref && fromBosch) BOSCH_COLOR_INDEX else defaultColor, isScaleReference = !levelOnly && entered != null, pathId = pathId, segmentIndex = if (pathId != null) lines.count { it.pathId == pathId } + 1 else 0, lengthSource = if (!ref && entered == null) LengthSource.UNKNOWN else if (fromBosch) LengthSource.MEASURED else LengthSource.MANUAL, axis = if (ref) axis.key else "")
            pushUndo()
            val previousScale = scale
            val updated = recalculate(lines + line)
            lines = if (previousScale == null && scale != null && pictureOptions.guess && !unknownSize) recalculate(updated.map { if (!it.isReference && it.lengthSource == LengthSource.UNKNOWN) it.copy(lengthSource = LengthSource.CALCULATED) else it }) else updated
            if (ref) referenceCount++
            draftStart = if (pendingBrokenSegment) pendingEnd else null; pendingEnd = null; showDistanceEntry = false; pendingBrokenSegment = false
            if (ref && scale != null) tool = Tool.MEASURE
        }
    )
    if (showCanvasSize && bitmap != null && tool == Tool.CROP) CanvasSizeDialog(bitmap!!.width, bitmap!!.height, onDismiss = { showCanvasSize = false }, onApply = ::expandCanvas)
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
    shapeEditId?.let { id ->
        val selected = guides.firstOrNull { it.id == id }
        val path = selected?.let { target -> closedGuideShapes(guides).firstOrNull { group -> sameGuideObject(group.first(), target) } }
        if (path != null) {
          val points = shapePoints(path)
          val left = points.minOf { it.x }; val top = points.minOf { it.y }
          val width = (points.maxOf { it.x } - left).coerceAtLeast(0.001f); val height = (points.maxOf { it.y } - top).coerceAtLeast(0.001f)
          val shapeIds = guides.filter { sameGuideObject(it, path.first()) }.map { it.id }.toSet()
          val shapeExtrusion = extrusions.firstOrNull { it.guideIds.any { id -> id in shapeIds } }
          ShapeEditDialog(path.first(), shapeExtrusion, guides, extrusions, unit, scale, width, height, onDismiss = { shapeEditId = null }, onSave = { color, transparency, widthValue, heightValue, depthValue, depthEdited, positionZ ->
            pushUndo()
            val targetWidth = widthValue?.let { value -> scale?.let { value * it } }
            val targetHeight = heightValue?.let { value -> scale?.let { value * it } }
            val sx = (targetWidth?.div(width) ?: 1.0).toFloat()
            val sy = (targetHeight?.div(height) ?: if (scale == null && widthValue != null && heightValue != null) width * heightValue / (widthValue * height) else 1.0).toFloat()
            fun resize(point: Pt) = Pt(left + (point.x - left) * sx, top + (point.y - top) * sy)
            val ids = guides.filter { sameGuideObject(it, path.first()) }.map { it.id }.toSet()
            val changed = abs(sx - 1f) > 0.0001f || abs(sy - 1f) > 0.0001f
            guides = guides.map { guide -> if (guide.id in ids) guide.copy(start = resize(guide.start), end = resize(guide.end), curveMid = guide.curveMid?.let(::resize), colorIndex = color, fillTransparency = if (guide.pathId == path.first().pathId) transparency else guide.fillTransparency, lengthMm = if (changed) null else guide.lengthMm, shapeWidthMm = if (guide.id == path.first().id) widthValue?.times(unitFactorMm(unit)) else guide.shapeWidthMm, shapeHeightMm = if (guide.id == path.first().id) heightValue?.times(unitFactorMm(unit)) else guide.shapeHeightMm) else guide }
            extrusions = extrusions.map { extrusion -> if (extrusion.guideIds.any { it in ids }) { val resized = extrusion.copy(origin = resize(extrusion.origin), handle = resize(extrusion.handle)); resized.depthMm?.let { extrusionWithDepth(resized, it, scale, unit) } ?: resized } else extrusion }
            if (depthEdited && shapeExtrusion != null) extrusions = extrusions.map { extrusion ->
                if (extrusion.id != shapeExtrusion.id) extrusion else if (depthValue == null) extrusion.copy(depthMm = null) else extrusionWithDepth(extrusion, depthValue * unitFactorMm(unit), scale, unit)
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
    guideLengthEditId?.let { id -> guides.firstOrNull { it.id == id }?.let { guide ->
        var length by remember(id, guide.lengthMm, unit) { mutableStateOf(guide.lengthMm?.let { (it / unitFactorMm(unit)).toString() } ?: "") }
        val value = length.replace(',', '.').toDoubleOrNull()
        AlertDialog(onDismissRequest = { guideLengthEditId = null }, title = { Text("Line length") },
            text = { Column {
                OutlinedTextField(length, { if (decimalInputPattern.matches(it)) length = it }, label = { Text("Length ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (scale == null) Text("Set a calibration length to resize the line to scale.")
            } },
            confirmButton = { TextButton(enabled = value != null && value > 0, onClick = {
                val mm = value!! * unitFactorMm(unit)
                val oldEnd = guide.end
                val currentLength = scale?.let { s -> guidePoints(guide).zipWithNext().sumOf { (a, b) -> frontLength(a, b, s, axisScale) } } ?: 0.0
                val factor = if (currentLength > 1e-9) value / currentLength else 1.0
                val newEnd = Pt((guide.start.x + (guide.end.x - guide.start.x) * factor).toFloat(), (guide.start.y + (guide.end.y - guide.start.y) * factor).toFloat())
                pushUndo()
                guides = guides.map { item ->
                    if (item.id == guide.id) item.copy(end = newEnd, curveMid = item.curveMid?.let { Pt((item.start.x + (it.x - item.start.x) * factor).toFloat(), (item.start.y + (it.y - item.start.y) * factor).toFloat()) }, lengthMm = mm)
                    else if (sameGuideObject(item, guide)) item.copy(start = if (distance(item.start, oldEnd) < 0.6) newEnd else item.start, end = if (distance(item.end, oldEnd) < 0.6) newEnd else item.end, lengthMm = if (distance(item.start, oldEnd) < 0.6 || distance(item.end, oldEnd) < 0.6) null else item.lengthMm)
                    else item
                }
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
        else {
            val arms = if (action.kind == JunctionKind.JOIN_MEASUREMENTS) {
                lines.filter { it.id == action.firstId || it.id == action.secondId }.flatMap { line -> listOf(line.start, line.end).filter { distance(it, action.point) >= 0.6 }.map { JunctionArm(line.id, it, measurementColor(line)) } }
            } else {
                guides.filter { it.id == action.firstId || it.id == action.secondId }.flatMap { line -> listOf(line.start, line.end).filter { distance(it, action.point) >= 0.6 }.map { JunctionArm(line.id, it, guideColor(line.colorIndex)) } }
            }
            JoinJunctionDialog(action, arms, levelRotation, onDismiss = { junctionAction = null }, onJoin = { chosen, keep -> applyJunction(action, chosen, keep) })
        }
    }
    selectedLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
        if (showLineEdit) LineEditDialog(line, unit, measurementPointNames(lines)[line.id], onDismiss = { showLineEdit = false }, onSave = save@{ value, label, colorIndex, lengthEdited, fromBosch, startZ, endZ ->
            val startMm = startZ * unitFactorMm(unit); val endMm = endZ * unitFactorMm(unit)
            val depthChanged = startMm != line.startZMm || endMm != line.endZMm
            val fixed = value > 0 && (line.isScaleReference || lengthEdited)
            val dz = abs(endMm - startMm) / unitFactorMm(unit)
            if (fixed && dz > value) { Toast.makeText(context, "Z difference cannot exceed the measurement length", Toast.LENGTH_LONG).show(); return@save }
            if ((depthChanged || lengthEdited && line.hasDepth()) && fixed && scale == null) { Toast.makeText(context, "Set a calibration scale first", Toast.LENGTH_LONG).show(); return@save }
            var start = line.start; var end = line.end
            val moveStart = startMm != line.startZMm && endMm == line.endZMm
            if ((depthChanged || lengthEdited && line.hasDepth()) && fixed && scale != null) {
                val oldLength = frontLength(start, end, scale!!, axisScale)
                if (distance(start, end) < 0.00001) { Toast.makeText(context, "Cannot set direction for a zero-length line", Toast.LENGTH_LONG).show(); return@save }
                val projection = projectedLengthForFixedTrue(value, dz) ?: 0.0
                val ratio = (projection / oldLength).toFloat()
                if (moveStart) start = Pt(end.x + (start.x - end.x) * ratio, end.y + (start.y - end.y) * ratio)
                else end = Pt(start.x + (end.x - start.x) * ratio, start.y + (end.y - start.y) * ratio)
                val oldPoint = if (moveStart) line.start else line.end
                if (line.pathId != null && lines.any { it.id != line.id && it.pathId == line.pathId && it.isScaleReference && (distance(it.start, oldPoint) < 0.6 || distance(it.end, oldPoint) < 0.6) }) {
                    Toast.makeText(context, "Split this point from the adjoining fixed-length segment first", Toast.LENGTH_LONG).show(); return@save
                }
            }
            pushUndo()
            lines = recalculate(lines.map { item ->
                if (item.id == line.id) item.copy(start = start, end = end, startZMm = startMm, endZMm = endMm, value = value, label = label, colorIndex = colorIndex, isScaleReference = if (lengthEdited && value <= 0.0 && !item.isReference) false else item.isScaleReference || lengthEdited, lengthSource = if (!lengthEdited) item.lengthSource else if (value <= 0.0 && !item.isReference) LengthSource.UNKNOWN else if (fromBosch) LengthSource.MEASURED else LengthSource.MANUAL)
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
        bitmap?.let { image -> exportAnnotated(context, image, lines, guides, exportGuides, unit, unitVisibility, levelRotation, pictureOptions, extrusions); Toast.makeText(context, "Saved to Pictures/Idea 3D", Toast.LENGTH_SHORT).show() }
    }, onExport3d = {
        val exportGuidesSnapshot = allGuides(); val exportExtrusionsSnapshot = allExtrusions()
        val exportScale = scale; val exportUnit = unit; val exportAxes = frontAxes(); val exportWidth = bitmap?.width ?: 1; val exportHeight = bitmap?.height ?: 1
        modelReport = ModelReport.Checking
        scope.launch {
            val result = withContext(Dispatchers.Default) { runCatching {
                val meshes = buildMeshes(exportGuidesSnapshot, exportExtrusionsSnapshot, exportScale, exportUnit, exportWidth, exportHeight, exportAxes)
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
    viewerModel?.let { mesh -> ThreeDViewerDialog(mesh, viewerAngle, initialSun = ((extrusions.firstOrNull()?.sun?.x ?: ((bitmap?.width ?: 1) * 0.15f)) / (bitmap?.width ?: 1)).coerceIn(0f, 1f), onSunChange = { position ->
        val width = bitmap?.width ?: 1
        if (extrusions.any { abs(it.sun.x - position * width) > 0.01f }) { pushUndo(); extrusions = extrusions.map { it.copy(sun = Pt(position * width, 0f)) } }
    }, onAngleChange = { viewerAngle = it }, onEditObject = { viewerEditObject = it }, onClose = { viewerModel = null; viewerEditObject = null; showPictureEdit = false },
        format = { formatLength(it, LengthUnit.of(unit), MeasurementPrecision.places[unit] ?: 2) }, stepMm = unitFactorMm(unit) / Math.pow(10.0, (MeasurementPrecision.places[unit] ?: 2).toDouble()),
        onEdit = ::applyViewerEdit, canUndo = undoHistory.size > viewerUndoBase, onUndo = { restoreLastUndo(); open3dView(viewerAngle) },
        onDraw = { angle -> viewerModel = null; viewerEditObject = null; switchPlane(when (angle) { ViewerAngle.TOP -> ModelPlane.TOP; ViewerAngle.PROFILE -> ModelPlane.PROFILE; else -> ModelPlane.FRONT }); if (tool == Tool.NONE) tool = Tool.LINES }) }
    viewerEditObject?.let { index ->
        val target = viewerObjects.getOrNull(index)
        val geometry = target?.let { planeGeometry(it.first) }
        val extrusion = geometry?.extrusions?.firstOrNull { it.id == target.second }
        val name = viewerModel?.objectNames?.getOrNull(index) ?: "Object"
        if (target == null || geometry == null || extrusion == null) viewerEditObject = null
        else ObjectDepthDialog(name, target.first, facePositionMm(extrusion), extrusion.depthMm, unit, onDismiss = { viewerEditObject = null }, onRemove3d = {
            pushUndo()
            setPlaneShapes(target.first, geometry.guides, geometry.extrusions.filterNot { it.id == extrusion.id })
            viewerEditObject = null
            if (allExtrusions().isEmpty()) viewerModel = null else open3dView(viewerAngle)
        }) { faceMm, depthMm ->
            pushUndo()
            val resized = if (depthMm != null) extrusionWithDepth(extrusion, depthMm, scale, unit) else extrusion
            setPlaneShapes(target.first, geometry.guides, geometry.extrusions.map { if (it.id == extrusion.id) withFacePosition(resized, faceMm, resized.depthMm ?: 0.0) else it })
            viewerEditObject = null
            open3dView(viewerAngle)
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

@Composable private fun LibraryScreen(modifier: Modifier, projects: List<SavedProject>, onImport: () -> Unit, onNewScene: () -> Unit, onImportProject: () -> Unit, onSettings: () -> Unit, onRename: (SavedProject) -> Unit, onExportProject: (SavedProject) -> Unit, onConvert: (SavedProject, String) -> Unit, onOpen: (SavedProject) -> Unit, onDelete: (SavedProject) -> Unit) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Library", Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.Black)
            TextButton(onClick = onSettings) { Text("Settings") }
            Button(onClick = onImport, shape = RoundedCornerShape(8.dp)) { Text("Import picture") }
        }
        Row { TextButton(onClick = onNewScene) { Text("New empty scene") }; TextButton(onClick = onImportProject) { Text("Import saved project") } }
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
    onExportBoschLog: () -> Unit,
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
            HorizontalDivider()
            BoschConnectionSettings(currentUnit)
            OutlinedButton(onClick = onExportBoschLog, shape = RoundedCornerShape(8.dp)) { Text("Export Bosch diagnostic log") }
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current picture", fontWeight = FontWeight.Bold)
                Text("Export a plain text record of the measurements on the open picture.", fontSize = 13.sp, color = Color(0xFF53615D))
                OutlinedButton(onClick = onExportLog, enabled = hasProject, shape = RoundedCornerShape(8.dp)) { Text("Export measurement log") }
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
@Composable private fun ObjectDepthDialog(name: String, plane: ModelPlane, zMm: Double, depthMm: Double?, unit: String, onDismiss: () -> Unit, onRemove3d: () -> Unit, onSave: (Double, Double?) -> Unit) {
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
            TextButton(onClick = { confirmRemove = true }) { Text("Remove 3D (keep shape)") }
        }
    }, confirmButton = { TextButton(enabled = valid, onClick = { onSave(zValue!!, if (depthEdited) depthValue else null) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/** Picks the drawing plane. Every plane has the same tools; shapes drawn in Top extrude along Y, in Profile along X. */
@Composable private fun PlaneSwitcher(plane: ModelPlane, onPlane: (ModelPlane) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF2F5F4)).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        ModelPlane.entries.forEach { candidate ->
            FilterChip(selected = plane == candidate, onClick = { onPlane(candidate) }, label = { Text(candidate.label, fontSize = 13.sp) })
        }
        Text(when (plane) {
            ModelPlane.FRONT -> "X →  Y ↑  depth Z"
            ModelPlane.TOP -> "X →  Z ↓ front  depth Y"
            ModelPlane.PROFILE -> "Z ← front  Y ↑  depth X"
        }, fontSize = 11.sp, color = Color(0xFF4D5A57), maxLines = 1, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
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

@Composable private fun StatusStrip(tool: Tool, scale: Double?, count: Int, refs: Int, drawingEnd: Boolean = false, onMeasurements: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 8.dp)) {
        val status = when { drawingEnd -> "Tap the end point"; tool == Tool.NONE -> "Choose a tool"; tool == Tool.CROP -> "Adjust crop, then confirm"; tool == Tool.LINES -> "Tap points to draw a guide"; tool == Tool.ADJUST -> "Select a point, line or label"; tool == Tool.CALIBRATE -> "Tap two points to calibrate"; else -> "Tap two points to measure" }
        Text(status, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count measurements", modifier = Modifier.clickable(onClick = onMeasurements).padding(vertical = 6.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF006E67))
            Text("  |  $refs reference${if (refs == 1) "" else "s"}", fontSize = 12.sp, color = Color(0xFF4D5A57))
        }
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

@Composable private fun BottomControls(calibrationEnabled: Boolean, tool: Tool, lineDrawing: LineDrawing, measureDrawing: LineDrawing, drawingBroken: Boolean, guideColorIndex: Int, guideStrokeWidth: Float, unknownSize: Boolean, onUnknownSize: (Boolean) -> Unit, onTool: (Tool) -> Unit, onFinishBroken: () -> Unit, onLineDrawing: (LineDrawing) -> Unit, onMeasureDrawing: (LineDrawing) -> Unit, onGuideStyle: (Int, Float) -> Unit) {
    var lineMenuOpen by remember { mutableStateOf(false) }
    var measureMenuOpen by remember { mutableStateOf(false) }
    Surface(shadowElevation = 8.dp) { Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
            // Only the photo can be calibrated; Top and Profile reuse its scale.
            if (calibrationEnabled) ToolIconButton("Calibrate", R.drawable.ic_calibrate_level, tool == Tool.CALIBRATE, { onTool(if (tool == Tool.CALIBRATE) Tool.NONE else Tool.CALIBRATE) }, Modifier.weight(1f))
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
                ToolIconButton(if (tool == Tool.LINES && lineDrawing == LineDrawing.CURVE) "Curve" else "Lines", R.drawable.ic_guide_line, tool == Tool.LINES && lineDrawing != LineDrawing.BROKEN, { val selected = tool == Tool.LINES && lineDrawing != LineDrawing.BROKEN; onLineDrawing(LineDrawing.STRAIGHT); onTool(if (selected) Tool.NONE else Tool.LINES) }, Modifier.fillMaxWidth(), description = "Guide line", onLongPress = { lineMenuOpen = true })
                DropdownMenu(expanded = lineMenuOpen, onDismissRequest = { lineMenuOpen = false }) {
                    Text("Line type", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Bold)
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.STRAIGHT) "Straight guide  ✓" else "Straight guide") }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.STRAIGHT); onTool(Tool.LINES) })
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.BROKEN) "Broken guide  ✓" else "Broken guide") }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.BROKEN); onTool(Tool.LINES) })
                    DropdownMenuItem(text = { Text(if (lineDrawing == LineDrawing.CURVE) "Curve guide  ✓" else "Curve guide") }, onClick = { lineMenuOpen = false; onLineDrawing(LineDrawing.CURVE); onTool(Tool.LINES) })
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
            if (drawingBroken) ToolButton("Finish", true, onFinishBroken, Modifier.weight(1f))
            else ToolButton("Adjust", tool == Tool.ADJUST, { onTool(if (tool == Tool.ADJUST) Tool.NONE else Tool.ADJUST) }, Modifier.weight(1f))
    } }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable private fun ToolIconButton(label: String, icon: Int, selected: Boolean, action: () -> Unit, modifier: Modifier, enabled: Boolean = true, description: String = label, onLongPress: (() -> Unit)? = null) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(42.dp)
                .background(if (selected) Color(0xFFE2ECE9) else Color.Transparent, CircleShape)
                .combinedClickable(enabled = enabled, onClick = action, onLongClick = onLongPress),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(icon), contentDescription = description, tint = Color.Unspecified, modifier = Modifier.size(26.dp)) }
        Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (enabled) Color(0xFF173C39) else Color(0xFF8A9691))
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

@Composable private fun ImageEditor(modifier: Modifier, bitmap: Bitmap, lines: List<MeasureLine>, guides: List<GuideLine>, ghosts: List<Pair<Int, FloatArray>> = emptyList(), selectedGuideId: Int?, unit: String, drawingScale: Double?, draftStart: Pt?, pendingEnd: Pt?, tool: Tool, selectedEndpoint: Endpoint?, magnifierPoint: Pt?, deleteCandidateId: Int?, alignmentLineIds: Set<Int>, alignmentFlashToken: Int, edgeSnapPoint: Pt?, edgeFlashToken: Int, adjustmentIndicatorPoint: Pt?, adjustmentLineId: Int?, adjustmentGuideId: Int?, adjustmentFlashToken: Int, guideColorIndex: Int, guideStrokeWidth: Float, rotation: Float, unitVisibility: String, zoom: Float, pan: Offset, cropFrame: CropFrame?, onTransform: (Float, Offset) -> Unit, onDoubleTap: () -> Unit, doubleTapEnabled: Boolean, onJunction: (JunctionAction) -> Unit, onAddPoint: (Pt, Double) -> Unit, onTap: (Pt, Double) -> Unit, onLongPress: (Pt, Double) -> Unit, onDragStart: (Pt, Double) -> Unit, onDrag: (Pt, Boolean) -> Unit, onDragEnd: () -> Unit, onDragCancel: () -> Unit, onCropStart: (Pt, Double) -> Unit, onCropDrag: (Pt) -> Unit, onCropEnd: () -> Unit, selectedCurveId: Int?, selectedLabelId: Int?, selectedWholeLineId: Int?, selectedGuideEndpoint: GuideEndpoint?, selectedWholeGuideId: Int?, selectedVertices: List<Pt>, selectionStart: Pt?, selectionEnd: Pt?, panOnEmptyDrag: Boolean, showFineControls: Boolean, onNudge: (Float, Float) -> Unit, pictureOptions: PictureOptions, extrusions: List<Extrusion>, selectedExtrusionId: Int?, onTripleTap: (Pt, Double) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    fun selectionFeedback() { if (!vibrateSelection(context)) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    val holdScope = rememberCoroutineScope()
    BoxWithConstraints(modifier.background(Color(0xFF18201E)).clipToBounds()) {
        val viewW = constraints.maxWidth.toFloat(); val viewH = constraints.maxHeight.toFloat()
        val snapRadiusOnScreen = with(LocalDensity.current) { 15.dp.toPx() }
        val junctionRadiusOnScreen = with(LocalDensity.current) { 32.dp.toPx() }
        val baseFit = min(viewW / bitmap.width, viewH / bitmap.height)
        val fit = baseFit * zoom
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
            return Pt(((unrotatedView.x - useLeft) / useFit).coerceIn(0f, bitmap.width.toFloat()), ((unrotatedView.y - useTop) / useFit).coerceIn(0f, bitmap.height.toFloat()))
        }
        fun viewAt(p: Pt) = Offset(left + p.x * fit, top + p.y * fit)
        val latestZoom = rememberUpdatedState(zoom)
        val latestPan = rememberUpdatedState(pan)
        val latestDragStart = rememberUpdatedState(onDragStart)
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
            var tapCount = 0
            var pendingTap: kotlinx.coroutines.Job? = null
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val gestureLines = latestLines.value
                val gestureGuides = latestGuides.value
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
                            val nextZoom = (localZoom * (distance / lastDistance)).coerceIn(1f, 8f)
                            val focus = sourceAt(lastCentroid, localZoom, localPan)
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
                        val dragThreshold = if (adjusting && tool == Tool.ADJUST) 0.0 else if (tool == Tool.ADJUST || tool == Tool.CROP) 1.0 else 8.0
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
                            } else if (tool == Tool.LINES && !transformed && gestureGuides.any { it.id == latestSelectedGuideId.value && it.curveMid?.let { mid -> distance(sourceAt(down.position, localZoom, localPan), mid) < 42.0 / (baseFit * localZoom) } == true }) {
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
                drawImage(bitmap.asImageBitmap(), dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()), dstSize = androidx.compose.ui.unit.IntSize(imageW.toInt(), imageH.toInt()), alpha = 1f - pictureOptions.transparency / 100f)
                if (ghosts.isNotEmpty()) drawIntoCanvas { canvas ->
                    // Drawn opaque into a translucent layer so overlapping faces don't darken.
                    canvas.saveLayer(androidx.compose.ui.geometry.Rect(left, top, left + imageW, top + imageH), androidx.compose.ui.graphics.Paint().apply { alpha = 0.3f })
                    ghosts.forEach { (argb, triangles) ->
                        val path = androidx.compose.ui.graphics.Path()
                        var i = 0
                        while (i + 5 < triangles.size) {
                            path.moveTo(left + triangles[i] * fit, top + triangles[i + 1] * fit); path.lineTo(left + triangles[i + 2] * fit, top + triangles[i + 3] * fit); path.lineTo(left + triangles[i + 4] * fit, top + triangles[i + 5] * fit); path.close()
                            i += 6
                        }
                        drawPath(path, Color(argb).copy(alpha = 1f))
                    }
                    canvas.restore()
                }
                // Top and Profile canvases put the front drawing plane (Z = 0) across their middle.
                if (DepthWords.plane != ModelPlane.FRONT) {
                    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                    val zeroColor = Color(0xFF607D8B)
                    if (DepthWords.plane == ModelPlane.TOP) drawLine(zeroColor, Offset(left, top + imageH / 2f), Offset(left + imageW, top + imageH / 2f), strokeWidth = 3f, pathEffect = dash)
                    else drawLine(zeroColor, Offset(left + imageW / 2f, top), Offset(left + imageW / 2f, top + imageH), strokeWidth = 3f, pathEffect = dash)
                }
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
                shapes.forEach { shape ->
                    (shape.first().fillTransparency ?: if (extrusions.any { extrusion -> extrusionDepth(extrusion, bitmap.width, bitmap.height) >= 0.001f && shape.all { guide -> extrusionGuides(extrusion, guides).any { it.id == guide.id } } }) 90 else null)?.let { transparency ->
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
                drawContext.canvas.save()
                shapes.forEach { shape ->
                    (listOf(shapePoints(shape)) + attachedRegions(shape, guides).map { it.points }).forEach { points ->
                        if (points.isNotEmpty()) {
                            val path = androidx.compose.ui.graphics.Path().apply { val first = viewAt(points.first()); moveTo(first.x, first.y); points.drop(1).forEach { viewAt(it).let { p -> lineTo(p.x, p.y) } }; close() }
                            drawContext.canvas.clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference)
                        }
                    }
                }
                drawContext.canvas.save()
                extrusions.forEach { extrusion ->
                    extrusionFaces(extrusion, guides, bitmap.width, bitmap.height).forEach { face ->
                        val path = androidx.compose.ui.graphics.Path().apply {
                            val first = viewAt(face.points.first())
                            moveTo(first.x, first.y)
                            face.points.drop(1).forEach { point -> viewAt(point).let { lineTo(it.x, it.y) } }
                            close()
                        }
                        drawPath(path, guideColor(face.guide.colorIndex).copy(alpha = face.opacity))
                        if (face.light != 0f) drawPath(path, (if (face.light > 0f) Color.White else Color.Black).copy(alpha = 0.05f * abs(face.light)))
                        drawContext.canvas.clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference)
                    }
                }
                drawContext.canvas.restore()
                extrusions.forEach { extrusion ->
                    extrusionEdges(extrusion, guides, bitmap.width, bitmap.height).forEach { (start, end, guide) ->
                        drawLine(guideColor(guide.colorIndex), viewAt(start), viewAt(end), (guide.strokeWidth * 0.5f).coerceAtLeast(1f) * zoom)
                        drawLine(Color.White, viewAt(start), viewAt(end), (guide.strokeWidth * 0.2f).coerceAtLeast(0.5f) * zoom)
                    }
                }
                drawContext.canvas.restore()
                val selectedGuide = selectedGuideId?.let { id -> guides.firstOrNull { it.id == id } }
                guides.forEach { guide ->
                    val start = viewAt(guide.start); val end = viewAt(guide.end)
                    val separator = guide.id in separators
                    val width = if (separator) (guide.strokeWidth * 0.35f).coerceAtLeast(1f) else guide.strokeWidth
                    val lineColor = guideColor(guide.colorIndex).copy(alpha = if (separator) 0.45f else 1f)
                    if (guide.curveMid != null) {
                        val mid = viewAt(guide.curveMid)
                        val path = androidx.compose.ui.graphics.Path().apply { moveTo(start.x, start.y); quadraticTo(2f * mid.x - (start.x + end.x) / 2f, 2f * mid.y - (start.y + end.y) / 2f, end.x, end.y) }
                        drawPath(path, if (!separator && guide.id == selectedGuideId && tool != Tool.LINES) Color(0xFFCF2020) else lineColor, style = Stroke(width * zoom))
                        if (!separator) drawPath(path, Color.White, style = Stroke((guide.strokeWidth * 0.45f).coerceAtLeast(1.5f) * zoom))
                        if (guide.id == selectedGuideId && (tool == Tool.LINES || tool == Tool.ADJUST)) {
                            listOf(start, end).forEach { drawCircle(guideColor(guide.colorIndex), 6.dp.toPx(), it); drawCircle(Color.White, 3.dp.toPx(), it) }
                            drawCircle(Color.White, 10.dp.toPx(), mid)
                            drawCircle(if (curveBlink) Color(0xFF0066BE) else Color.White, 8.dp.toPx(), mid)
                        }
                    } else if (!separator && selectedGuide != null && sameGuideObject(guide, selectedGuide)) {
                        drawLine(Color(0xFFCF2020), start, end, (guide.strokeWidth + 2f) * zoom)
                        drawLine(Color.White, start, end, (guide.strokeWidth * 0.45f).coerceAtLeast(1.5f) * zoom)
                    } else {
                        drawLine(lineColor, start, end, width * zoom)
                        if (!separator) drawLine(Color.White, start, end, (guide.strokeWidth * 0.45f).coerceAtLeast(1.5f) * zoom)
                    }
                    guide.lengthMm?.let { mm ->
                        val center = viewAt(guidePointAt(guide, 0.5f))
                        val text = "${formatMeasure(mm / unitFactorMm(unit), unit)} $unit"
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; textSize = 14.sp.toPx(); isFakeBoldText = true; style = Paint.Style.STROKE; strokeWidth = 4.dp.toPx(); color = AndroidColor.WHITE }
                        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y, paint)
                        paint.style = Paint.Style.FILL; paint.color = AndroidColor.BLACK
                        drawContext.canvas.nativeCanvas.drawText(text, center.x, center.y, paint)
                    }
                }
                if (tool == Tool.ADJUST) extrusions.firstOrNull { it.id == selectedExtrusionId }?.let { extrusion ->
                    fun cube(center: Offset, color: Color) {
                        val r = 11.dp.toPx()
                        drawCircle(Color.White, r * 1.5f, center)
                        drawCircle(color, r * 1.5f, center, style = Stroke(2.dp.toPx()))
                        val a = center + Offset(0f, -r); val b = center + Offset(r, -r * 0.45f); val c = center + Offset(r, r * 0.55f)
                        val d = center + Offset(0f, r); val e = center + Offset(-r, r * 0.55f); val f = center + Offset(-r, -r * 0.45f)
                        listOf(a to b, b to c, c to d, d to e, e to f, f to a, a to center, c to center, e to center).forEach { (start, end) -> drawLine(color, start, end, 2.dp.toPx()) }
                    }
                    if (vanishingBlink) cube(viewAt(depthHandle(extrusion, extrusion.handle)), Color(0xFF0066BE))
                    cube(viewAt(extrusion.vanishing), Color(0xFFD00000))
                }
                guides.filter { it.pathId != null || it.curveMid != null }.sortedBy { if (selectedGuide != null && sameGuideObject(it, selectedGuide)) 0 else 1 }
                    .flatMap { guide -> listOf(guide.start to guide, guide.end to guide) }
                    .fold(emptyList<Pair<Pt, GuideLine>>()) { vertices, vertex -> if (vertices.any { distance(it.first, vertex.first) < 0.6 }) vertices else vertices + vertex }
                    .forEach { (point, guide) ->
                        val center = viewAt(point)
                        val active = selectedGuide != null && sameGuideObject(guide, selectedGuide)
                        drawCircle(guideColor(guide.colorIndex), if (active) (guide.strokeWidth + 5f) * zoom else 3.dp.toPx(), center)
                        if (active) drawCircle(Color.White, (guide.strokeWidth * 0.55f).coerceAtLeast(3f) * zoom, center)
                    }
                val screenLines = lines.map { ScreenLine(it.id, viewAt(it.start), viewAt(it.end)) }
                val occupiedLabels = mutableListOf<androidx.compose.ui.geometry.Rect>()
                val pointNames = measurementPointNames(lines)
                val drawnNames = mutableSetOf<String>()
                if (unknownBlinkOn) lines.filter { !it.isReference && it.value <= 0.0 }.forEach { line ->
                    drawLine(Color.White, viewAt(line.start), viewAt(line.end), 14f * zoom)
                }
                lines.forEach { line ->
                    drawAnnotation(viewAt(line.start), viewAt(line.end), line.value, line.label, line.isReference, line.isScaleReference, line.colorIndex, line.labelOffset * fit, unit, unitVisibility, zoom, line.id, isHorizontalCalibration(line), screenLines, occupiedLabels, line.pathId != null && !line.isScaleReference, !line.isReference && line.value <= 0.0, line.hasDepth())
                    pointNames[line.id]?.let { names ->
                        fun vertexLabel(point: Offset, text: String) {
                            if (!drawnNames.add(text)) return
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15f * zoom.coerceIn(1f, 3f); isFakeBoldText = true }
                            val x = point.x + 12f * zoom; val y = point.y - 12f * zoom
                            drawContext.canvas.nativeCanvas.drawText(text, x, y, Paint(paint).apply { style = Paint.Style.STROKE; strokeWidth = 4f * zoom; color = AndroidColor.WHITE })
                            drawContext.canvas.nativeCanvas.drawText(text, x, y, paint.apply { color = AndroidColor.BLACK })
                        }
                        vertexLabel(viewAt(line.start), names.first)
                        vertexLabel(viewAt(line.end), names.second)
                    }
                }
            if (heldLineBlink) heldLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
                val a = viewAt(line.start); val b = viewAt(line.end)
                drawLine(Color.White, a, b, 14f * zoom)
                drawLine(Color(0xFF0066BE), a, b, 8f * zoom)
            }
            if (heldLineBlink) heldGuideId?.let { id -> guides.firstOrNull { it.id == id } }?.let { guide ->
                drawLine(Color.White, viewAt(guide.start), viewAt(guide.end), (guide.strokeWidth + 9f) * zoom)
                drawLine(Color(0xFF0066BE), viewAt(guide.start), viewAt(guide.end), (guide.strokeWidth + 4f) * zoom)
            }
            if (deleteBlinkOn) deleteCandidateId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
                val a = viewAt(line.start); val b = viewAt(line.end)
                drawLine(Color.Black, a, b, 11f * zoom)
                drawLine(Color(0xFFCF2020), a, b, 6f * zoom)
                drawCircle(Color(0xFFCF2020), 14f * zoom, a)
                drawCircle(Color(0xFFCF2020), 14f * zoom, b)
            }
            if (alignmentBlinkOn) lines.filter { it.id in alignmentLineIds }.forEach { line ->
                val a = viewAt(line.start); val b = viewAt(line.end)
                drawLine(Color.Black, a, b, 11f * zoom)
                drawLine(Color(0xFF0066CC), a, b, 6f * zoom)
                drawCircle(Color(0xFF0066CC), 14f * zoom, a)
                drawCircle(Color(0xFF0066CC), 14f * zoom, b)
            }
            if (edgeBlinkOn) edgeSnapPoint?.let { point ->
                val center = viewAt(point)
                drawCircle(Color.White, 18f * zoom, center, style = Stroke(6f * zoom))
                drawCircle(Color(0xFF19B45A), 18f * zoom, center, style = Stroke(3f * zoom))
            }
            if (adjustmentBlinkOn) {
                adjustmentLineId?.let { id -> lines.firstOrNull { it.id == id } }?.let { line ->
                    drawLine(Color.Black, viewAt(line.start), viewAt(line.end), 11f * zoom)
                    drawLine(Color(0xFF0066BE), viewAt(line.start), viewAt(line.end), 6f * zoom)
                }
                adjustmentGuideId?.let { id -> guides.firstOrNull { it.id == id } }?.let { guide ->
                    drawLine(Color.Black, viewAt(guide.start), viewAt(guide.end), 9f * zoom)
                    drawLine(Color(0xFF0066BE), viewAt(guide.start), viewAt(guide.end), 4f * zoom)
                }
                adjustmentIndicatorPoint?.let { point ->
                    val center = viewAt(point)
                    drawCircle(Color.White, 20f * zoom, center, style = Stroke(7f * zoom))
                    drawCircle(Color(0xFF0066BE), 20f * zoom, center, style = Stroke(3f * zoom))
                }
            }
            draftStart?.let { start -> drawPoint(viewAt(start), if (tool == Tool.LINES) guideColor(guideColorIndex) else Color(0xFFFFD400)) }
            if (draftStart != null && pendingEnd != null) drawLine(if (tool == Tool.LINES) guideColor(guideColorIndex) else Color(0xFFFFD400), viewAt(draftStart), viewAt(pendingEnd!!), if (tool == Tool.LINES) guideStrokeWidth else 3f)
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedEndpoint?.let { endpoint ->
                lines.firstOrNull { it.id == endpoint.lineId }?.let { line ->
                    drawLine(Color.Black, viewAt(line.start), viewAt(line.end), 10f * zoom)
                    drawLine(Color(0xFF0066BE), viewAt(line.start), viewAt(line.end), 5f * zoom)
                    val point = viewAt(if (endpoint.isStart) line.start else line.end)
                    drawCircle(Color.White, 20f * zoom, point, style = Stroke(6f * zoom))
                    drawCircle(Color(0xFF0066BE), 20f * zoom, point, style = Stroke(3f * zoom))
                }
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedGuideEndpoint?.let { endpoint ->
                guides.firstOrNull { it.id == endpoint.guideId }?.let { guide ->
                    val point = viewAt(if (endpoint.isStart) guide.start else guide.end)
                    drawCircle(Color.Black, 18f * zoom, point, style = Stroke(6f * zoom))
                    drawCircle(Color(0xFF0066BE), 18f * zoom, point, style = Stroke(3f * zoom))
                }
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedWholeLineId?.let { id ->
                val pathId = lines.firstOrNull { it.id == id }?.pathId
                lines.filter { it.id == id || pathId != null && it.pathId == pathId }.forEach { line ->
                    drawLine(Color(0xFF0066BE), viewAt(line.start), viewAt(line.end), 7f * zoom)
                }
            }
            if (tool == Tool.ADJUST && selectionBlinkOn) selectedWholeGuideId?.let { id ->
                val pathId = guides.firstOrNull { it.id == id }?.pathId
                guides.filter { it.id == id || pathId != null && it.pathId == pathId }.forEach { guide ->
                    drawLine(Color(0xFF0066BE), viewAt(guide.start), viewAt(guide.end), 7f * zoom)
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
            FineAdjustPad(onNudge)
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
    drawCircle(Color.Black, 12f * zoom, p); drawCircle(color, 8f * zoom, p); drawCircle(Color(0xFF18201E), 3f * zoom, p)
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

@Composable private fun DistanceDialog(isReference: Boolean, referenceCount: Int, unit: String, unknownSize: Boolean, onUnknownSize: (Boolean) -> Unit, onDismiss: () -> Unit, onConfirm: (Double?, String, String, Boolean, Boolean, CalibrationAxis) -> Unit) {
    val context = LocalContext.current
    var value by remember { mutableStateOf("") }; var label by remember { mutableStateOf("") }
    var axis by remember { mutableStateOf(CalibrationAxis.UNIFORM) }
    var enteredUnit by remember { mutableStateOf(unit) }
    var unitMenuOpen by remember { mutableStateOf(false) }
    var levelOnly by remember { mutableStateOf(false) }
    var boschReadingUsed by remember { mutableStateOf(false) }
    val openedReadingSequence = remember { BoschGlmConnection.lastReading?.sequence ?: 0L }
    val boschReading = BoschGlmConnection.lastReading
    val boschReady = BoschGlmConnection.ready
    val boschStatus = BoschGlmConnection.status
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    fun connectBosch() = BoschGlmConnection.connect(context.applicationContext)
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (requiredPermissions.all { granted[it] == true || context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) connectBosch()
        else BoschGlmConnection.permissionDenied()
    }
    LaunchedEffect(boschReading?.sequence) {
        val reading = boschReading
        if (reading != null && reading.sequence > openedReadingSequence) {
            val targetUnit = if (isReference) enteredUnit else unit
            value = format(reading.meters * 1000.0 / unitFactorMm(targetUnit))
            boschReadingUsed = true
        }
    }
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
            if (!levelOnly && (isReference || !unknownSize)) OutlinedTextField(value, { if (decimalInputPattern.matches(it)) { value = it; boschReadingUsed = false } }, label = { Text("Distance (${if (isReference) enteredUnit else unit})") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (!levelOnly && (isReference || !unknownSize) && !boschReady) OutlinedButton(onClick = {
                if (requiredPermissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) connectBosch()
                else bluetoothPermission.launch(requiredPermissions)
            }) { Text("Connect Bosch GLM") }
            if (!levelOnly && (isReference || !unknownSize) && boschReady) Text("Bosch connected. The next laser measurement fills the distance field automatically.", fontSize = 12.sp, color = Color(0xFF1565C0))
            if (!levelOnly && (isReference || !unknownSize) && boschStatus.isNotBlank()) Text(boschStatus, fontSize = 12.sp, color = Color(0xFF4D5A57))
            if (boschReadingUsed) Text("Laser measurement received.", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
            OutlinedTextField(label, { label = it }, label = { Text("Label (optional)") }, singleLine = true)
        }
    }, confirmButton = { TextButton(onClick = { if (isReference && levelOnly) onConfirm(null, label, enteredUnit, true, false, CalibrationAxis.UNIFORM) else if (!isReference && unknownSize) onConfirm(null, label, unit, false, false, CalibrationAxis.UNIFORM) else value.toDoubleOrNull()?.takeIf { it > 0 }?.let { onConfirm(it, label, if (isReference) enteredUnit else unit, false, boschReadingUsed, axis) } }, enabled = (isReference && levelOnly) || (!isReference && unknownSize) || value.toDoubleOrNull()?.let { it > 0 } == true) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
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

@Composable private fun JoinJunctionDialog(action: JunctionAction, arms: List<JunctionArm>, rotation: Float, onDismiss: () -> Unit, onJoin: (List<JunctionArm>, Boolean) -> Unit) {
    var selected by remember(action) { mutableStateOf(emptySet<Int>()) }
    var keepRemaining by remember(action) { mutableStateOf(false) }
    var showDeleted by remember(action) { mutableStateOf(true) }
    LaunchedEffect(selected) {
        showDeleted = true
        if (selected.size == 2) while (true) {
            delay(450)
            showDeleted = !showDeleted
        }
    }
    fun toggle(index: Int) {
        selected = if (index in selected) selected - index else if (selected.size < 2) selected + index else selected
    }
    fun diagramEndpoint(arm: JunctionArm, center: Offset, radius: Float): Offset {
        val dx = (arm.endpoint.x - action.point.x).toDouble(); val dy = (arm.endpoint.y - action.point.y).toDouble()
        val length = hypot(dx, dy).coerceAtLeast(0.001)
        val angle = Math.toRadians(rotation.toDouble())
        return center + Offset(((dx * cos(angle) - dy * sin(angle)) / length * radius).toFloat(), ((dx * sin(angle) + dy * cos(angle)) / length * radius).toFloat())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join at crossing") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (keepRemaining) "Select two portions to join. The others will remain separate." else "Select two portions to keep. The others will be deleted.")
                Canvas(Modifier.fillMaxWidth().height(180.dp).pointerInput(action, arms, selected, rotation) {
                    detectTapGestures { tap ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = min(size.width, size.height) * 0.35f
                        if ((tap - center).getDistance() > radius * 0.2f) {
                            val index = arms.indices.minByOrNull { i ->
                                val end = diagramEndpoint(arms[i], center, radius)
                                val direction = end - center
                                val t = (((tap.x - center.x) * direction.x + (tap.y - center.y) * direction.y) / (direction.x * direction.x + direction.y * direction.y).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                (tap - (center + direction * t)).getDistance()
                            }
                            if (index != null) toggle(index)
                        }
                    }
                }) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) * 0.35f
                    arms.forEachIndexed { index, arm ->
                        val end = diagramEndpoint(arm, center, radius)
                        val deleted = selected.size == 2 && index !in selected && !keepRemaining
                        val lineColor = if (deleted) Color(0xFFD50000).copy(alpha = if (showDeleted) 1f else 0.25f) else if (keepRemaining && selected.size == 2 && index !in selected) Color(0xFF66716C) else Color(0xFF00823B)
                        drawLine(Color.White, center, end, 7.dp.toPx())
                        drawLine(lineColor, center, end, 4.dp.toPx())
                        drawCircle(Color.White, 15.dp.toPx(), end)
                        drawCircle(lineColor, 15.dp.toPx(), end, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(0, 130, 59); textAlign = Paint.Align.CENTER; textSize = 20.sp.toPx(); isFakeBoldText = true }
                        drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), end.x, end.y - (paint.ascent() + paint.descent()) / 2, paint)
                    }
                    drawCircle(Color(0xFF17211E), 7f, center)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    arms.indices.forEach { index ->
                        FilterChip(selected = index in selected, onClick = { toggle(index) }, label = { Text((index + 1).toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00823B)) })
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { keepRemaining = !keepRemaining }) {
                    Checkbox(checked = keepRemaining, onCheckedChange = { keepRemaining = it })
                    Text("Don't delete remaining lines")
                }
                if (action.kind == JunctionKind.JOIN_MEASUREMENTS) Text("A measurement cut at the crossing becomes unknown.", fontSize = 12.sp, color = Color(0xFF53615D))
            }
        },
        confirmButton = { TextButton(onClick = { onJoin(selected.sorted().map { arms[it] }, keepRemaining) }, enabled = selected.size == 2) { Text("Join") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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

@Composable private fun PictureEditDialog(currentName: String, currentUnit: String, currentOptions: PictureOptions, hasScale: Boolean, onExportImage: () -> Unit, onExport3d: () -> Unit, onDismiss: () -> Unit, onSave: (String, String, Map<String, Int>, PictureOptions) -> Unit) {
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

@Composable private fun ShapeEditDialog(guide: GuideLine, extrusion: Extrusion?, guides: List<GuideLine>, extrusions: List<Extrusion>, unit: String, scale: Double?, width: Float, height: Float, onDismiss: () -> Unit, onSave: (Int, Int?, Double?, Double?, Double?, Boolean, Double?) -> Unit) {
    var color by remember(guide.id) { mutableIntStateOf(guide.colorIndex) }
    var transparency by remember(guide.id) { mutableStateOf(guide.fillTransparency) }
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
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit shape") }, text = {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(shapeWidth, { if (decimalInputPattern.matches(it)) { shapeWidth = it; widthEdited = true } }, label = { Text("Width ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                OutlinedTextField(shapeHeight, { if (decimalInputPattern.matches(it)) { shapeHeight = it; heightEdited = true } }, label = { Text("Height ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
            }
            if (extrusion != null) {
                OutlinedTextField(depth, { if (decimalInputPattern.matches(it)) { depth = it; depthEdited = true } }, label = { Text("Depth ${DepthWords.axis} ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
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
            Text("Fill transparency")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(null to "None", 20 to "20%", 50 to "50%", 90 to "90%").forEach { (value, label) ->
                    FilterChip(selected = transparency == value, onClick = { transparency = value }, label = { Text(label, fontSize = 12.sp) })
                }
            }
        }
    }, confirmButton = { TextButton(enabled = (shapeWidth.isBlank() || widthValue != null && widthValue > 0) && (shapeHeight.isBlank() || heightValue != null && heightValue > 0) && (depth.isBlank() || depthValue != null && depthValue >= 0) && (extrusion == null || positionValue != null && positionValue.isFinite()), onClick = { onSave(color, transparency, widthValue, heightValue, depthValue, depthEdited, if (extrusion != null) positionValue else null) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun LineEditDialog(line: MeasureLine, unit: String, points: Pair<String, String>?, onDismiss: () -> Unit, onSave: (Double, String, Int, Boolean, Boolean, Double, Double) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    var value by remember(line.id) { mutableStateOf(if (!line.isReference && line.value <= 0.0) "" else format(line.value)) }
    var label by remember(line.id) { mutableStateOf(line.label) }
    var colorIndex by remember(line.id) { mutableIntStateOf(line.colorIndex) }
    var boschReadingUsed by remember(line.id) { mutableStateOf(false) }
    var lengthEdited by remember(line.id) { mutableStateOf(false) }
    var startZ by remember(line.id, unit) { mutableStateOf(java.math.BigDecimal.valueOf(abs(line.startZMm) / unitFactorMm(unit)).stripTrailingZeros().toPlainString()) }
    var endZ by remember(line.id, unit) { mutableStateOf(java.math.BigDecimal.valueOf(abs(line.endZMm) / unitFactorMm(unit)).stripTrailingZeros().toPlainString()) }
    var startBack by remember(line.id) { mutableStateOf(line.startZMm < 0) }
    var endBack by remember(line.id) { mutableStateOf(line.endZMm < 0) }
    fun zValue(text: String, back: Boolean): Double? = text.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }?.let { if (back) -it else it }
    val openedReadingSequence = remember(line.id) { BoschGlmConnection.lastReading?.sequence ?: 0L }
    val boschReading = BoschGlmConnection.lastReading
    val boschStatus = BoschGlmConnection.status
    val boschReady = BoschGlmConnection.ready
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    fun connectBosch() = BoschGlmConnection.connect(context.applicationContext)
    val bluetoothPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (requiredPermissions.all { granted[it] == true || context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) connectBosch()
        else Unit
    }
    LaunchedEffect(boschReading?.sequence) {
        val reading = boschReading
        if (reading != null && reading.sequence > openedReadingSequence) {
            value = format(reading.meters * 1000.0 / unitFactorMm(unit))
            colorIndex = BOSCH_COLOR_INDEX
            boschReadingUsed = true
            lengthEdited = true
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (line.isReference) "Edit reference" else points?.let { "Edit ${it.first} - ${it.second}" } ?: "Edit measurement") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("A manually entered size stays fixed. Calibration lines always determine the scale for calculated lines.")
                OutlinedTextField(value = value, onValueChange = { if (decimalInputPattern.matches(it)) { value = it; lengthEdited = true; boschReadingUsed = false } }, label = { Text("Size ($unit)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                if (!boschReady) OutlinedButton(onClick = {
                    if (requiredPermissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) connectBosch()
                    else bluetoothPermission.launch(requiredPermissions)
                }) { Text("Connect Bosch GLM") }
                else Text("Bosch connected. The next laser measurement fills this field automatically.", fontSize = 12.sp, color = Color(0xFF1565C0))
                if (boschStatus.isNotBlank()) Text(boschStatus, fontSize = 12.sp, color = Color(0xFF4D5A57))
                if (boschReadingUsed) Text("Laser measurement: this line will be blue.", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
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
                if (!line.isReference) OutlinedButton(onClick = { val a = zValue(startZ, startBack); val b = zValue(endZ, endBack); if (a != null && b != null) onSave(0.0, label, colorIndex, true, false, a, b) }) { Text("Set unknown") }
                if (!line.isReference) Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Color")
                    lineColors.indices.forEach { index ->
                        Box(Modifier.size(28.dp).background(lineColor(index), CircleShape).border(3.dp, if (colorIndex == index) Color.White else Color.Transparent, CircleShape).clickable { colorIndex = index })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { val a = zValue(startZ, startBack); val b = zValue(endZ, endBack); value.toDoubleOrNull()?.takeIf { it > 0 }?.let { if (a != null && b != null) onSave(if (lengthEdited) it else line.value, label, colorIndex, lengthEdited, boschReadingUsed, a, b) } }, enabled = value.toDoubleOrNull()?.let { it > 0 } == true && zValue(startZ, startBack) != null && zValue(endZ, endBack) != null) { Text("Save") } },
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

private fun nextGuideId(guides: List<GuideLine>) = (guides.maxOfOrNull { it.id } ?: 0) + 1
private fun nextGuidePathId(guides: List<GuideLine>) = (guides.mapNotNull { it.pathId }.maxOrNull() ?: 0) + 1

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
}

private fun planeMapping(plane: ModelPlane, width: Int, height: Int, scale: Double?, unit: String, axisScale: Pair<Double, Double>? = null): PlaneMapping? {
    val pixelsPerUnit = scale?.takeIf { it.isFinite() && it > 0 } ?: return null
    val uniform = unitFactorMm(unit) / pixelsPerUnit
    return when (plane) {
        ModelPlane.FRONT -> PlaneMapping(plane, 0.0, height.toDouble(), axisScale?.first?.let { unitFactorMm(unit) / it } ?: uniform, axisScale?.second?.let { unitFactorMm(unit) / it } ?: uniform)
        ModelPlane.TOP -> PlaneMapping(plane, 0.0, height / 2.0, uniform, uniform)
        ModelPlane.PROFILE -> PlaneMapping(plane, width / 2.0, height.toDouble(), uniform, uniform)
    }
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
private fun buildSolidSpecs(guides: List<GuideLine>, extrusions: List<Extrusion>, scale: Double?, unit: String, width: Int, height: Int, axisScale: Pair<Double, Double>? = null, objectsOut: MutableList<Extrusion>? = null, strict: Boolean = true): List<SolidSpec> {
    scale?.takeIf { it.isFinite() && it > 0 } ?: error("Set a calibration length before exporting")
    val issues = mutableListOf<MeshIssue>()
    val specs = mutableListOf<SolidSpec>()
    var count = 0
    ModelPlane.entries.forEach { plane ->
        val planeGuides = guides.filter { it.plane == plane }
        val planeExtrusions = extrusions.filter { it.plane == plane }
        val mapping = planeMapping(plane, width, height, scale, unit, axisScale)!!
        val models = drawableGuideShapes(planeGuides).mapNotNull { shape ->
            val name = "${if (plane == ModelPlane.FRONT) "" else "${plane.label} "}Shape ${++count}"
            val extrusion = planeExtrusions.firstOrNull { ex -> extrusionGuides(ex, planeGuides).any { g -> shape.any { it.id == g.id } } }
            if (extrusion == null) { issues += MeshIssue(name, "has no 3D depth yet; use Make 3D or exclude it"); return@mapNotNull null }
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
            specs += SolidSpec(name, outline, model.color, plane)
            objectsOut?.add(model.extrusion.copy(plane = plane))
        }
    }
    if (!strict) return specs
    if (count == 0) error("Draw a closed shape and add 3D depth first")
    if (issues.isNotEmpty()) throw MeshException(issues)
    return specs
}

private fun buildMeshes(guides: List<GuideLine>, extrusions: List<Extrusion>, scale: Double?, unit: String, width: Int, height: Int, axisScale: Pair<Double, Double>? = null): List<Mesh> {
    val meshes = MeshBuilder.buildAll(buildSolidSpecs(guides, extrusions, scale, unit, width, height, axisScale))
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
    if (showGuides) shapes.forEach { shape ->
        (shape.first().fillTransparency ?: if (extrusions.any { extrusion -> extrusionDepth(extrusion, original.width, original.height) >= 0.001f && shape.all { guide -> extrusionGuides(extrusion, guides).any { it.id == guide.id } } }) 90 else null)?.let { transparency ->
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
    canvas.save()
    if (showGuides) shapes.forEach { shape ->
        (listOf(shapePoints(shape)) + attachedRegions(shape, guides).map { it.points }).forEach { points ->
            if (points.isNotEmpty()) {
                val path = android.graphics.Path().apply { moveTo(points.first().x, points.first().y); points.drop(1).forEach { lineTo(it.x, it.y) }; close() }
                canvas.clipOutPath(path)
            }
        }
    }
    canvas.save()
    if (showGuides) extrusions.forEach { extrusion ->
        extrusionFaces(extrusion, guides, original.width, original.height).forEach { face ->
            val path = android.graphics.Path().apply {
                moveTo(face.points.first().x, face.points.first().y)
                face.points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = guideColor(face.guide.colorIndex).toArgb()
                alpha = (255f * face.opacity).roundToInt()
                style = Paint.Style.FILL
            }
            canvas.drawPath(path, fill)
            if (face.light != 0f) {
                val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (face.light > 0f) AndroidColor.WHITE else AndroidColor.BLACK; alpha = (255f * 0.05f * abs(face.light)).roundToInt(); style = Paint.Style.FILL }
                canvas.drawPath(path, shade)
            }
            canvas.clipOutPath(path)
        }
    }
    canvas.restore()
    if (showGuides) extrusions.forEach { extrusion ->
        extrusionEdges(extrusion, guides, original.width, original.height).forEach { (start, end, guide) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint(guideColor(guide.colorIndex).toArgb(), (guide.strokeWidth * 0.5f).coerceAtLeast(1f)))
            canvas.drawLine(start.x, start.y, end.x, end.y, linePaint(AndroidColor.WHITE, (guide.strokeWidth * 0.2f).coerceAtLeast(0.5f)))
        }
    }
    canvas.restore()
    if (showGuides) guides.forEach { guide ->
        val mid = guide.curveMid
        val separator = guide.id in separators
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
