package com.fotolab.ai

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

private data class PhotoPreset(val id: String, val name: String, val brightness: Float, val contrast: Float, val saturation: Float)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FotoLabApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FotoLabApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("fotolab_presets", 0) }
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var edited by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var presets by remember { mutableStateOf(loadPresets(prefs.getString("items", "[]") ?: "[]")) }
    var dialogName by remember { mutableStateOf("") }
    var dialogPreset by remember { mutableStateOf<PhotoPreset?>(null) }
    var dialogMode by remember { mutableStateOf("save") }
    var showPresetDialog by remember { mutableStateOf(false) }
    var showAiInfo by remember { mutableStateOf(false) }

    fun refresh() { edited = original?.let { applyAdjustments(it, brightness, contrast, saturation) } }
    fun persist() { prefs.edit().putString("items", presetsToJson(presets)).apply() }
    fun savePreset(name: String, duplicate: Boolean = false) {
        val clean = name.trim()
        if (clean.isBlank()) return
        val base = PhotoPreset(java.util.UUID.randomUUID().toString(), clean, brightness, contrast, saturation)
        presets = if (duplicate) presets + base else {
            val existing = presets.indexOfFirst { it.id == dialogPreset?.id }
            if (dialogMode == "rename" && existing >= 0) presets.toMutableList().also { it[existing] = it[existing].copy(name = clean) }
            else presets + base
        }
        persist()
        showPresetDialog = false
        dialogName = ""
        Toast.makeText(context, "Preset salvo", Toast.LENGTH_SHORT).show()
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    original = BitmapFactory.decodeStream(stream)
                    brightness = 0f; contrast = 1f; saturation = 1f
                    refresh()
                }
            } catch (_: Exception) { Toast.makeText(context, "Não foi possível abrir a foto", Toast.LENGTH_SHORT).show() }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFB69CFF), secondary = Color(0xFF70E1D1), background = Color(0xFF111018), surface = Color(0xFF1B1924))) {
        Scaffold(containerColor = Color(0xFF111018), topBar = {
            TopAppBar(title = { Column { Text("FotoLab AI", fontWeight = FontWeight.Bold); Text("Seu estilo. Suas regras.", style = MaterialTheme.typography.labelSmall, color = Color.LightGray) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111018), titleContentColor = Color.White))
        }) { padding ->
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 28.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.PhotoLibrary, null); Spacer(Modifier.width(6.dp)); Text("Abrir foto") }
                        OutlinedButton(onClick = { original?.let { saveToGallery(context, edited ?: it) } ?: Toast.makeText(context, "Abra uma foto primeiro", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Exportar") }
                    }
                }
                item {
                    Box(Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 390.dp).background(Color(0xFF201D2A), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                        val bitmap = edited
                        if (bitmap != null) Image(bitmap.asImageBitmap(), contentDescription = "Prévia da foto editada", modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp))
                        else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) { Icon(Icons.Default.PhotoLibrary, null, tint = Color(0xFFB69CFF), modifier = Modifier.size(42.dp)); Spacer(Modifier.height(10.dp)); Text("Escolha uma foto para começar", color = Color.White); Text("Sua edição fica no aparelho", color = Color.LightGray, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                item { Text("AJUSTES", color = Color(0xFFB69CFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) }
                item { SliderControl("Brilho", brightness, -100f, 100f, "${brightness.toInt()}") { brightness = it; refresh() } }
                item { SliderControl("Contraste", contrast, 0.5f, 2f, "${(contrast * 100).toInt()}%") { contrast = it; refresh() } }
                item { SliderControl("Saturação", saturation, 0f, 2f, "${(saturation * 100).toInt()}%") { saturation = it; refresh() } }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { brightness = 0f; contrast = 1f; saturation = 1f; refresh() }, modifier = Modifier.weight(1f)) { Text("Redefinir") }
                        Button(onClick = { dialogMode = "save"; dialogPreset = null; dialogName = "Meu preset"; showPresetDialog = true }, enabled = original != null, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Salvar preset") }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("MEUS PRESETS", color = Color(0xFFB69CFF), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Text("${presets.size} salvos", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (presets.isEmpty()) item { Text("Seus estilos salvos aparecerão aqui. Edite uma foto e toque em “Salvar preset”.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium) }
                items(presets, key = { it.id }) { preset ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF201D2A)), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(preset.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Brilho ${preset.brightness.toInt()} · Contraste ${(preset.contrast * 100).toInt()}% · Saturação ${(preset.saturation * 100).toInt()}%", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(onClick = { if (original == null) Toast.makeText(context, "Abra uma foto primeiro", Toast.LENGTH_SHORT).show() else { brightness = preset.brightness; contrast = preset.contrast; saturation = preset.saturation; refresh() } }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Aplicar") }
                                IconButton(onClick = { dialogMode = "rename"; dialogPreset = preset; dialogName = preset.name; showPresetDialog = true }) { Icon(Icons.Default.Edit, "Renomear", tint = Color.White) }
                                IconButton(onClick = { dialogMode = "duplicate"; dialogPreset = preset; dialogName = "${preset.name} cópia"; showPresetDialog = true }) { Icon(Icons.Default.Add, "Duplicar", tint = Color.White) }
                                IconButton(onClick = { presets = presets.filterNot { it.id == preset.id }; persist() }) { Icon(Icons.Default.Delete, "Excluir", tint = Color(0xFFFF8A8A)) }
                            }
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF201D2A)), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("FERRAMENTAS DE IA", color = Color(0xFFB69CFF), fontWeight = FontWeight.Bold)
                            Text("Remoção de objetos, troca de fundo e restauração inteligente entram na próxima etapa com integração de IA.", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { showAiInfo = true }, modifier = Modifier.fillMaxWidth()) { Text("Ver plano de IA") }
                        }
                    }
                }
            }
        }
    }
    if (showPresetDialog) AlertDialog(onDismissRequest = { showPresetDialog = false }, title = { Text(when(dialogMode) { "rename" -> "Renomear preset"; "duplicate" -> "Duplicar preset"; else -> "Salvar preset" }) }, text = { OutlinedTextField(value = dialogName, onValueChange = { dialogName = it }, label = { Text("Nome do preset") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (dialogMode == "rename") savePreset(dialogName) else savePreset(dialogName, duplicate = dialogMode == "duplicate") }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { showPresetDialog = false }) { Text("Cancelar") } })
    if (showAiInfo) AlertDialog(onDismissRequest = { showAiInfo = false }, title = { Text("Próxima etapa: IA") }, text = { Text("A base do app já permite editar fotos e gerenciar presets localmente. Para remoção de objetos, troca de fundo e restauração, será necessário integrar um modelo de IA no dispositivo ou um serviço online, com atenção a custo, privacidade e conexão.") }, confirmButton = { TextButton(onClick = { showAiInfo = false }) { Text("Entendi") } })
}

@Composable
private fun SliderControl(label: String, value: Float, min: Float, max: Float, valueLabel: String, onValue: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Color.White); Text(valueLabel, color = Color.LightGray) }
        Slider(value = value, onValueChange = onValue, valueRange = min..max)
    }
}

private fun applyAdjustments(source: Bitmap, brightness: Float, contrast: Float, saturation: Float): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val scale = contrast
    val translate = (1f - scale) * 128f + brightness
    val matrix = ColorMatrix(floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
    ))
    val sat = ColorMatrix().apply { setSaturation(saturation) }
    sat.postConcat(matrix)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(sat) }
    canvas.drawBitmap(source, 0f, 0f, paint)
    return output
}

private fun loadPresets(raw: String): List<PhotoPreset> = try {
    val arr = JSONArray(raw)
    (0 until arr.length()).map { i -> arr.getJSONObject(i).let { PhotoPreset(it.getString("id"), it.getString("name"), it.getDouble("brightness").toFloat(), it.getDouble("contrast").toFloat(), it.getDouble("saturation").toFloat()) } }
} catch (_: Exception) { emptyList() }

private fun presetsToJson(items: List<PhotoPreset>): String = JSONArray().apply {
    items.forEach { p -> put(JSONObject().apply { put("id", p.id); put("name", p.name); put("brightness", p.brightness.toDouble()); put("contrast", p.contrast.toDouble()); put("saturation", p.saturation.toDouble()) }) }
}.toString()

private fun saveToGallery(context: android.content.Context, bitmap: Bitmap) {
    try {
        val values = ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME, "FotoLab_${System.currentTimeMillis()}.jpg"); put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FotoLabAI") }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        Toast.makeText(context, if (uri != null) "Foto exportada para a galeria" else "Não foi possível exportar", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) { Toast.makeText(context, "Erro ao exportar a foto", Toast.LENGTH_SHORT).show() }
}
