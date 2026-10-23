package com.davoyans.idea3d

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davoyans.idea3d.core.MemberProfile
import com.davoyans.idea3d.core.ProfileKind
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.DecimalFormat
import java.util.UUID

/** A bar or pipe that lines can be made of. Materials are shared by every project. */
internal data class Material(val id: String, val name: String, val profile: MemberProfile, val argb: Int) {
    /** For example "Rectangular tube 40 × 20 × 2 mm". */
    val description: String get() {
        val f = DecimalFormat("0.##")
        val size = if (profile.kind.round) "Ø${f.format(profile.widthMm)}" else "${f.format(profile.widthMm)} × ${f.format(profile.heightMm)}"
        return "${profile.kind.label} $size${if (profile.kind.hollow) " × ${f.format(profile.wallMm)}" else ""} mm"
    }
}

/** Keeps the material list in the app's own storage, outside any project, so every project sees the same list. */
internal object MaterialStore {
    private fun file(context: Context) = File(context.filesDir, "materials.json")

    private val defaults = listOf(
        Material("iron-rect-40x20", "Iron pipe, rectangular", MemberProfile(ProfileKind.RECT_TUBE, 40.0, 20.0, 2.0), 0xFF5F6B6D.toInt()),
        Material("iron-square-20", "Iron pipe, square", MemberProfile(ProfileKind.RECT_TUBE, 20.0, 20.0, 1.5), 0xFF4E5A5C.toInt()),
        Material("iron-round-25", "Iron pipe, round", MemberProfile(ProfileKind.ROUND_TUBE, 25.0, 25.0, 2.0), 0xFF6D7778.toInt()),
        Material("wood-50x100", "Wood beam", MemberProfile(ProfileKind.RECT_BAR, 50.0, 100.0), 0xFFB5865A.toInt()),
    )

    fun load(context: Context): List<Material> {
        val f = file(context)
        if (!f.exists()) return defaults
        return runCatching {
            val array = JSONArray(f.readText())
            (0 until array.length()).map { i -> array.getJSONObject(i).let { o ->
                Material(o.getString("id"), o.getString("name"), MemberProfile(ProfileKind.of(o.optString("kind")), o.getDouble("width"), o.optDouble("height", o.getDouble("width")), o.optDouble("wall", 0.0)), o.optInt("color", 0xFF5F6B6D.toInt()))
            } }
        }.getOrDefault(defaults)
    }

    fun save(context: Context, materials: List<Material>) {
        file(context).writeText(JSONArray().apply {
            materials.forEach { m -> put(JSONObject().apply { put("id", m.id); put("name", m.name); put("kind", m.profile.kind.name); put("width", m.profile.widthMm); put("height", m.profile.heightMm); put("wall", m.profile.wallMm); put("color", m.argb) }) }
        }.toString())
    }
}

private val materialColors = listOf(0xFF5F6B6D, 0xFF2F3A3C, 0xFF8A9497, 0xFFB5865A, 0xFF7A4E2D, 0xFF1565C0, 0xFFC62828, 0xFF2E7D32).map { it.toInt() }

/** The shared materials list: add, edit and delete bars and pipes. */
@Composable internal fun MaterialsScreen(materials: List<Material>, onChange: (List<Material>) -> Unit, onBack: () -> Unit) {
    var editing by remember { mutableStateOf<Material?>(null) }
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("Back") }
            Text("Materials", Modifier.weight(1f), fontSize = 26.sp, fontWeight = FontWeight.Black)
            Button(onClick = { adding = true }, shape = RoundedCornerShape(8.dp)) { Text("Add") }
        }
        Text("Shared by all projects. Pick one in a line's details to make that line a bar or pipe in 3D and in exports.", fontSize = 13.sp, color = Color(0xFF4D5A57))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            materials.forEach { material ->
                Card(Modifier.fillMaxWidth().clickable { editing = material }, shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(28.dp).background(Color(material.argb), RoundedCornerShape(6.dp)))
                        Column(Modifier.weight(1f)) {
                            Text(material.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(material.description, fontSize = 13.sp, color = Color(0xFF4D5A57))
                        }
                    }
                }
            }
            if (materials.isEmpty()) Text("No materials yet. Tap Add.", color = Color(0xFF4D5A57))
        }
    }
    if (adding) MaterialDialog(null, onDismiss = { adding = false }, onDelete = null) { onChange(materials + it); adding = false }
    editing?.let { material ->
        MaterialDialog(material, onDismiss = { editing = null }, onDelete = { onChange(materials.filterNot { it.id == material.id }); editing = null }) { updated ->
            onChange(materials.map { if (it.id == material.id) updated else it }); editing = null
        }
    }
}

@Composable private fun MaterialDialog(initial: Material?, onDismiss: () -> Unit, onDelete: (() -> Unit)?, onSave: (Material) -> Unit) {
    fun text(v: Double?) = v?.let { DecimalFormat("0.##").format(it) } ?: ""
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kind by remember { mutableStateOf(initial?.profile?.kind ?: ProfileKind.RECT_TUBE) }
    var width by remember { mutableStateOf(text(initial?.profile?.widthMm)) }
    var height by remember { mutableStateOf(text(initial?.profile?.heightMm)) }
    var wall by remember { mutableStateOf(text(initial?.profile?.wallMm?.takeIf { it > 0 })) }
    var color by remember { mutableStateOf(initial?.argb ?: materialColors.first()) }
    var confirmDelete by remember { mutableStateOf(false) }
    fun number(s: String) = s.replace(',', '.').toDoubleOrNull()
    val w = number(width); val h = if (kind.round) w else number(height); val t = if (kind.hollow) number(wall) else 0.0
    val profile = if (w != null && h != null && t != null) MemberProfile(kind, w, h, t) else null
    val problem = profile?.problem()
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete ${initial?.name}?") },
        text = { Text("Lines made of it become plain lines again.") },
        confirmButton = { TextButton(onClick = { onDelete?.invoke() }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    else AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "New material" else "Edit material") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, placeholder = { Text("Iron pipe") }, singleLine = true)
            Text("Profile", fontWeight = FontWeight.Medium)
            ProfileKind.entries.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { k -> FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text(k.label, fontSize = 13.sp) }) } }
            }
            OutlinedTextField(width, { width = it }, label = { Text(if (kind.round) "Outer diameter (mm)" else "Width (mm)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (!kind.round) OutlinedTextField(height, { height = it }, label = { Text("Height (mm)") }, supportingText = { Text("Along the drawing's depth axis.") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            if (kind.hollow) OutlinedTextField(wall, { wall = it }, label = { Text("Wall thickness (mm)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text("Color", fontWeight = FontWeight.Medium)
            materialColors.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c -> Box(Modifier.size(30.dp).background(Color(c), CircleShape).border(if (c == color) 3.dp else 0.dp, Color(0xFF00897B), CircleShape).clickable { color = c }) }
                }
            }
            problem?.let { Text(it, color = Color(0xFFB3261E), fontSize = 13.sp) }
            if (onDelete != null) TextButton(onClick = { confirmDelete = true }) { Text("Delete material", color = Color(0xFFB3261E)) }
        }
    }, confirmButton = {
        TextButton(enabled = name.isNotBlank() && profile != null && problem == null, onClick = { onSave(Material(initial?.id ?: UUID.randomUUID().toString(), name.trim(), profile!!, color)) }) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
