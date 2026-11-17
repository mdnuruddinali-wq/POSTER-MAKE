package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CanvasFormat
import com.example.data.model.FilterPreset
import com.example.data.model.LayerType
import com.example.data.model.PosterCopySuggestion
import com.example.data.model.PosterFont
import com.example.data.model.ShapeKind
import com.example.data.model.TemplateCatalog
import com.example.data.model.parseHexColor
import com.example.ui.components.PosterCanvasPreview
import com.example.ui.theme.CoralSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.AiCopyResultState
import com.example.ui.viewmodel.EditorState
import com.example.util.BitmapStudioUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasEditorScreen(
    editorState: EditorState,
    aiCopyState: AiCopyResultState,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleGrid: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onSelectLayer: (String?) -> Unit,
    onMoveLayer: (String, Float, Float) -> Unit,
    onSetFormat: (CanvasFormat) -> Unit,
    onSetGradient: (String, String) -> Unit,
    onSetPhotoBitmap: (android.graphics.Bitmap) -> Unit,
    onSetFilterPreset: (FilterPreset) -> Unit,
    onUpdatePhotoAdjustments: (Double?, Double?, Double?, Double?) -> Unit,
    onAddTextLayer: (String, PosterFont) -> Unit,
    onAddShapeLayer: (ShapeKind, String) -> Unit,
    onUpdateSelectedLayer: ((com.example.data.model.CanvasLayer) -> com.example.data.model.CanvasLayer) -> Unit,
    onDeleteSelectedLayer: () -> Unit,
    onGenerateInEditorCopy: (String, CanvasFormat) -> Unit,
    onApplyCopySuggestion: (PosterCopySuggestion) -> Unit,
    onSaveToCloud: () -> Unit,
    onNotify: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeToolTab by remember { mutableIntStateOf(2) } // 0:Format, 1:Backdrop, 2:Text, 3:Shapes, 4:Filters, 5:AI
    var showExportSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapStudioUtils.loadBitmapFromUri(context, uri)
                if (bmp != null) {
                    onSetPhotoBitmap(bmp)
                } else {
                    onNotify("Unable to load selected photo.")
                }
            }
        }
    }

    val selectedLayer = remember(editorState.layers, editorState.selectedLayerId) {
        editorState.layers.firstOrNull { it.id == editorState.selectedLayerId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Studio Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("editor_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = editorState.project.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { showRenameDialog = true }
                    .padding(horizontal = 6.dp)
                    .testTag("editor_title_button")
            )

            IconButton(
                onClick = onUndo,
                enabled = editorState.undoStack.isNotEmpty(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("editor_undo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (editorState.undoStack.isNotEmpty()) Color.White else Color.Gray
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = editorState.redoStack.isNotEmpty(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("editor_redo_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    tint = if (editorState.redoStack.isNotEmpty()) Color.White else Color.Gray
                )
            }

            IconButton(
                onClick = onToggleGrid,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("editor_grid_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.GridOn,
                    contentDescription = "Toggle alignment grid",
                    tint = if (editorState.showGrid) CoralSpark else Color.White
                )
            }

            IconButton(
                onClick = onSaveToCloud,
                enabled = !editorState.isSaving,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("editor_save_cloud_button")
            ) {
                if (editorState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CoralSpark, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Save to cloud",
                        tint = WarmGold
                    )
                }
            }

            Button(
                onClick = { showExportSheet = true },
                colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("editor_export_button")
            ) {
                Text("Export", style = MaterialTheme.typography.labelLarge)
            }
        }

        // Interactive Canvas Stage
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            PosterCanvasPreview(
                project = editorState.project,
                layers = editorState.layers,
                activeBitmap = editorState.activeBitmap,
                selectedLayerId = editorState.selectedLayerId,
                showGrid = editorState.showGrid,
                interactive = true,
                onSelectLayer = onSelectLayer,
                onMoveLayer = onMoveLayer
            )
        }

        // Active Inspector Panel + Tool Dock
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, bottom = 10.dp)) {
                // Contextual Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    when (activeToolTab) {
                        0 -> FormatInspector(
                            currentRatio = editorState.project.aspectRatio,
                            onSetFormat = onSetFormat
                        )
                        1 -> BackdropInspector(
                            onSetGradient = onSetGradient,
                            onPickPhoto = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            overlayOpacity = editorState.project.overlayOpacity.toFloat(),
                            onOverlayChange = { onUpdatePhotoAdjustments(null, null, null, it.toDouble()) }
                        )
                        2 -> TextLayerInspector(
                            selectedLayer = selectedLayer,
                            onAddText = { onAddTextLayer("NEW HEADLINE", PosterFont.SPACE_GROTESK) },
                            onUpdateLayer = onUpdateSelectedLayer,
                            onDeleteLayer = onDeleteSelectedLayer
                        )
                        3 -> ShapesInspector(
                            selectedLayer = selectedLayer,
                            onAddShape = onAddShapeLayer,
                            onUpdateLayer = onUpdateSelectedLayer,
                            onDeleteLayer = onDeleteSelectedLayer
                        )
                        4 -> PhotoFilterInspector(
                            project = editorState.project,
                            onSelectFilter = onSetFilterPreset,
                            onAdjust = onUpdatePhotoAdjustments
                        )
                        else -> AiAssistInspector(
                            format = CanvasFormat.fromRatio(editorState.project.aspectRatio),
                            aiCopyState = aiCopyState,
                            onGenerateCopy = onGenerateInEditorCopy,
                            onApplyCopy = onApplyCopySuggestion
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Tool Tabs
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tools = listOf(
                        Triple(0, "Format", Icons.Filled.AspectRatio),
                        Triple(1, "Backdrop", Icons.Filled.Image),
                        Triple(2, "Text", Icons.Filled.TextFields),
                        Triple(3, "Shapes", Icons.Filled.Interests),
                        Triple(4, "Filters", Icons.Filled.PhotoFilter),
                        Triple(5, "AI Copy", Icons.Filled.AutoAwesome)
                    )
                    items(tools) { (idx, label, icon) ->
                        ToolDockChip(
                            icon = icon,
                            label = label,
                            selected = activeToolTab == idx,
                            onClick = { activeToolTab = idx }
                        )
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var draftTitle by remember(editorState.project.title) { mutableStateOf(editorState.project.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Design") },
            text = {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it.take(100) },
                    singleLine = true,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (draftTitle.isNotBlank()) onUpdateTitle(draftTitle)
                    showRenameDialog = false
                }) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showExportSheet) {
        ModalBottomSheet(onDismissRequest = { if (!isExporting) showExportSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val fmt = CanvasFormat.fromRatio(editorState.project.aspectRatio)
                Text("Export & Share Asset", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Renders high-resolution PNG (${fmt.exportWidth} × ${fmt.exportHeight} px) with all typography, shapes, and photo grades.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val bmp = BitmapStudioUtils.renderProjectToBitmap(
                                context,
                                editorState.project.copy(layers = editorState.layers.map { it.toMap() }),
                                editorState.activeBitmap
                            )
                            val res = BitmapStudioUtils.saveExportToGallery(context, bmp, editorState.project.title)
                            isExporting = false
                            showExportSheet = false
                            res.fold(
                                onSuccess = { onNotify(it) },
                                onFailure = { onNotify("Export failed: ${it.localizedMessage}") }
                            )
                        }
                    },
                    enabled = !isExporting,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_save_gallery_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isExporting) "Rendering..." else "Save High-Res PNG to Gallery")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val bmp = BitmapStudioUtils.renderProjectToBitmap(
                                context,
                                editorState.project.copy(layers = editorState.layers.map { it.toMap() }),
                                editorState.activeBitmap
                            )
                            val shareIntent = BitmapStudioUtils.createShareIntent(context, bmp, editorState.project.title)
                            isExporting = false
                            showExportSheet = false
                            if (shareIntent != null) {
                                context.startActivity(Intent.createChooser(shareIntent, "Share Poster"))
                            } else {
                                onNotify("Unable to create share file.")
                            }
                        }
                    },
                    enabled = !isExporting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("export_share_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share to Social Apps")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ToolDockChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) CoralSpark else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .height(48.dp)
            .testTag("tool_tab_${label.replace(" ", "_").lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormatInspector(
    currentRatio: String,
    onSetFormat: (CanvasFormat) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Canvas Aspect Ratio", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CanvasFormat.entries) { fmt ->
                FilterChip(
                    selected = currentRatio == fmt.id,
                    onClick = { onSetFormat(fmt) },
                    label = { Text("${fmt.label} (${fmt.ratioLabel})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoralSpark,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Text(
            text = "All layers scale proportionally across social formats.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackdropInspector(
    onSetGradient: (String, String) -> Unit,
    onPickPhoto: () -> Unit,
    overlayOpacity: Float,
    onOverlayChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gradients & Photos", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = onPickPhoto,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("editor_pick_photo_button")
            ) {
                Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick Photo")
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(TemplateCatalog.paletteSwatches) { (c1, c2) ->
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(parseHexColor(c1), parseHexColor(c2))
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                        .clickable { onSetGradient(c1, c2) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Scrim", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
            Slider(
                value = overlayOpacity,
                onValueChange = onOverlayChange,
                valueRange = 0f..0.75f,
                colors = SliderDefaults.colors(thumbColor = CoralSpark, activeTrackColor = CoralSpark),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TextLayerInspector(
    selectedLayer: com.example.data.model.CanvasLayer?,
    onAddText: () -> Unit,
    onUpdateLayer: ((com.example.data.model.CanvasLayer) -> com.example.data.model.CanvasLayer) -> Unit,
    onDeleteLayer: () -> Unit
) {
    val textColors = listOf("#FFFFFF", "#111018", "#FFC44D", "#FF5C39", "#8A7BFF", "#00D2B8")
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddText,
                colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("editor_add_text_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text")
            }

            if (selectedLayer != null && selectedLayer.type == LayerType.TEXT) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedLayer.uppercase,
                        onClick = { onUpdateLayer { it.copy(uppercase = !it.uppercase) } },
                        label = { Text("AA") }
                    )
                    FilterChip(
                        selected = selectedLayer.badgeBgHex.isNotBlank(),
                        onClick = {
                            onUpdateLayer {
                                it.copy(badgeBgHex = if (it.badgeBgHex.isBlank()) "#FF5C39" else "")
                            }
                        },
                        label = { Text("Pill") }
                    )
                    IconButton(onClick = onDeleteLayer) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete layer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        if (selectedLayer != null && selectedLayer.type == LayerType.TEXT) {
            OutlinedTextField(
                value = selectedLayer.text,
                onValueChange = { txt -> onUpdateLayer { it.copy(text = txt.take(160)) } },
                label = { Text("Selected text") },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_selected_text_input")
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PosterFont.entries) { f ->
                    FilterChip(
                        selected = selectedLayer.font == f,
                        onClick = { onUpdateLayer { it.copy(font = f) } },
                        label = { Text(f.label) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Size", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(42.dp))
                Slider(
                    value = selectedLayer.fontSizeSp,
                    onValueChange = { sz -> onUpdateLayer { it.copy(fontSizeSp = sz) } },
                    valueRange = 11f..54f,
                    modifier = Modifier.weight(1f)
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(textColors) { hex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseHexColor(hex))
                            .border(
                                width = if (selectedLayer.colorHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                color = if (selectedLayer.colorHex.equals(hex, ignoreCase = true)) CoralSpark else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { onUpdateLayer { it.copy(colorHex = hex) } }
                    )
                }
            }
        } else {
            Text(
                text = "Tap any text layer on the canvas to edit, or tap + Add Text.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShapesInspector(
    selectedLayer: com.example.data.model.CanvasLayer?,
    onAddShape: (ShapeKind, String) -> Unit,
    onUpdateLayer: ((com.example.data.model.CanvasLayer) -> com.example.data.model.CanvasLayer) -> Unit,
    onDeleteLayer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Graphic Shapes & Frames", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ShapeKind.entries) { kind ->
                OutlinedButton(
                    onClick = { onAddShape(kind, "#FFC44D") },
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("+ ${kind.label}")
                }
            }
        }
        if (selectedLayer != null && selectedLayer.type == LayerType.SHAPE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Scale", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(46.dp))
                Slider(
                    value = selectedLayer.scale,
                    onValueChange = { s -> onUpdateLayer { it.copy(scale = s) } },
                    valueRange = 0.5f..2.2f,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDeleteLayer) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete shape", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PhotoFilterInspector(
    project: com.example.data.model.DesignProject,
    onSelectFilter: (FilterPreset) -> Unit,
    onAdjust: (Double?, Double?, Double?, Double?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FilterPreset.entries) { preset ->
                FilterChip(
                    selected = project.filterPreset == preset.id,
                    onClick = { onSelectFilter(preset) },
                    label = { Text(preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bright", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
            Slider(
                value = project.brightness.toFloat(),
                onValueChange = { onAdjust(it.toDouble(), null, null, null) },
                valueRange = -0.5f..0.5f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Contrast", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
            Slider(
                value = project.contrast.toFloat(),
                onValueChange = { onAdjust(null, it.toDouble(), null, null) },
                valueRange = 0.6f..1.7f,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Color", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(56.dp))
            Slider(
                value = project.saturation.toFloat(),
                onValueChange = { onAdjust(null, null, it.toDouble(), null) },
                valueRange = 0f..1.8f,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AiAssistInspector(
    format: CanvasFormat,
    aiCopyState: AiCopyResultState,
    onGenerateCopy: (String, CanvasFormat) -> Unit,
    onApplyCopy: (PosterCopySuggestion) -> Unit
) {
    var topic by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it.take(200) },
                placeholder = { Text("Topic e.g. Summer Coffee Sale") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { onGenerateCopy(topic, format) },
                enabled = aiCopyState !is AiCopyResultState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = Color(0xFF241800)),
                modifier = Modifier.height(50.dp)
            ) {
                Text(if (aiCopyState is AiCopyResultState.Loading) "..." else "Write")
            }
        }
        when (aiCopyState) {
            is AiCopyResultState.Success -> {
                val s = aiCopyState.suggestion
                Button(
                    onClick = { onApplyCopy(s) },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Apply \"${s.headline}\" & Palette")
                }
            }
            is AiCopyResultState.Error -> {
                Text(aiCopyState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            else -> {
                Text(
                    "Generate AI headlines, CTA badges & palette directly onto this canvas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
