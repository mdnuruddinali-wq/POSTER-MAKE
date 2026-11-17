package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.ai.AiStudioService
import com.example.data.model.AiGenerationRecord
import com.example.data.model.BackgroundType
import com.example.data.model.CanvasFormat
import com.example.data.model.CanvasLayer
import com.example.data.model.DesignProject
import com.example.data.model.FilterPreset
import com.example.data.model.LayerType
import com.example.data.model.PosterCopySuggestion
import com.example.data.model.PosterFont
import com.example.data.model.ShapeKind
import com.example.data.model.StarterTemplate
import com.example.data.model.StudioCategory
import com.example.data.model.UiState
import com.example.data.repository.StudioRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

sealed interface AiImageResultState {
    data object Idle : AiImageResultState
    data class Loading(val label: String) : AiImageResultState
    data class Success(
        val bitmap: Bitmap,
        val prompt: String,
        val stylePreset: String,
        val format: CanvasFormat,
        val localPath: String? = null,
        val isPhotoEdit: Boolean = false
    ) : AiImageResultState
    data class Error(val message: String) : AiImageResultState
}

sealed interface AiCopyResultState {
    data object Idle : AiCopyResultState
    data object Loading : AiCopyResultState
    data class Success(val suggestion: PosterCopySuggestion) : AiCopyResultState
    data class Error(val message: String) : AiCopyResultState
}

data class EditorState(
    val project: DesignProject = DesignProject(),
    val layers: List<CanvasLayer> = emptyList(),
    val selectedLayerId: String? = null,
    val activeBitmap: Bitmap? = null,
    val showGrid: Boolean = false,
    val isSaving: Boolean = false,
    val undoStack: List<Pair<DesignProject, List<CanvasLayer>>> = emptyList(),
    val redoStack: List<Pair<DesignProject, List<CanvasLayer>>> = emptyList()
)

class StudioViewModel(
    private val repository: StudioRepository,
    private val aiService: AiStudioService,
    private val designsDir: File,
    private val currentUserId: String
) : ViewModel() {

    val projectsState: StateFlow<UiState<List<DesignProject>>> = repository.observeProjects(currentUserId)
        .map<List<DesignProject>, UiState<List<DesignProject>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load saved projects."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val aiHistoryState: StateFlow<UiState<List<AiGenerationRecord>>> = repository.observeAiHistory(currentUserId)
        .map<List<AiGenerationRecord>, UiState<List<AiGenerationRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to load AI generation history."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _aiImageState = MutableStateFlow<AiImageResultState>(AiImageResultState.Idle)
    val aiImageState: StateFlow<AiImageResultState> = _aiImageState.asStateFlow()

    private val _aiPhotoEditState = MutableStateFlow<AiImageResultState>(AiImageResultState.Idle)
    val aiPhotoEditState: StateFlow<AiImageResultState> = _aiPhotoEditState.asStateFlow()

    private val _aiCopyState = MutableStateFlow<AiCopyResultState>(AiCopyResultState.Idle)
    val aiCopyState: StateFlow<AiCopyResultState> = _aiCopyState.asStateFlow()

    private val _pickedPhoto = MutableStateFlow<Bitmap?>(null)
    val pickedPhoto: StateFlow<Bitmap?> = _pickedPhoto.asStateFlow()

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    fun syncUserProfile(displayName: String, email: String?, photoUrl: String?) {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, displayName, email, photoUrl)
        }
    }

    fun setPickedPhoto(bitmap: Bitmap?) {
        _pickedPhoto.value = bitmap
        _aiPhotoEditState.value = AiImageResultState.Idle
    }

    // --- AI GENERATION ---

    fun generatePosterOrBanner(
        prompt: String,
        stylePreset: String,
        format: CanvasFormat,
        includeSpaceForText: Boolean
    ) {
        if (prompt.isBlank()) {
            _aiImageState.value = AiImageResultState.Error("Please enter a creative prompt first.")
            return
        }
        viewModelScope.launch {
            _aiImageState.value = AiImageResultState.Loading("Synthesizing ${format.label} with Gemini AI...")
            val result = aiService.generatePosterOrBanner(prompt, stylePreset, format, includeSpaceForText)
            result.fold(
                onSuccess = { bmp ->
                    val savedPath = saveBitmapToDesignsDir(bmp, "ai_gen")
                    _aiImageState.value = AiImageResultState.Success(
                        bitmap = bmp,
                        prompt = prompt.trim(),
                        stylePreset = stylePreset,
                        format = format,
                        localPath = savedPath,
                        isPhotoEdit = false
                    )
                    repository.recordAiGeneration(
                        currentUserId,
                        AiGenerationRecord(
                            userId = currentUserId,
                            mode = "POSTER_BANNER",
                            prompt = prompt.trim(),
                            stylePreset = stylePreset,
                            aspectRatio = format.id,
                            resultSummary = "Generated ${format.label} in $stylePreset style",
                            localImagePath = savedPath
                        )
                    )
                },
                onFailure = { err ->
                    _aiImageState.value = AiImageResultState.Error(
                        err.localizedMessage ?: "Failed to generate image."
                    )
                }
            )
        }
    }

    fun applyAiPhotoEdit(instruction: String) {
        val source = _pickedPhoto.value
        if (source == null) {
            _aiPhotoEditState.value = AiImageResultState.Error("Pick a photo from your device first.")
            return
        }
        if (instruction.isBlank()) {
            _aiPhotoEditState.value = AiImageResultState.Error("Enter an edit instruction or tap a preset.")
            return
        }
        viewModelScope.launch {
            _aiPhotoEditState.value = AiImageResultState.Loading("Applying AI photo edit...")
            val result = aiService.editPhoto(source, instruction)
            result.fold(
                onSuccess = { bmp ->
                    val savedPath = saveBitmapToDesignsDir(bmp, "ai_edit")
                    _aiPhotoEditState.value = AiImageResultState.Success(
                        bitmap = bmp,
                        prompt = instruction.trim(),
                        stylePreset = "AI Photo Edit",
                        format = CanvasFormat.POSTER_3_4,
                        localPath = savedPath,
                        isPhotoEdit = true
                    )
                    repository.recordAiGeneration(
                        currentUserId,
                        AiGenerationRecord(
                            userId = currentUserId,
                            mode = "PHOTO_EDIT",
                            prompt = instruction.trim(),
                            stylePreset = "AI Photo Edit",
                            aspectRatio = "custom",
                            resultSummary = "Edited photo with Gemini Nano Banana 2.1",
                            localImagePath = savedPath
                        )
                    )
                },
                onFailure = { err ->
                    _aiPhotoEditState.value = AiImageResultState.Error(
                        err.localizedMessage ?: "Failed to edit photo."
                    )
                }
            )
        }
    }

    fun generateCopySuggestion(topic: String, format: CanvasFormat) {
        if (topic.isBlank()) {
            _aiCopyState.value = AiCopyResultState.Error("Describe your event, product, or post topic first.")
            return
        }
        viewModelScope.launch {
            _aiCopyState.value = AiCopyResultState.Loading
            val result = aiService.generatePosterCopy(topic, format.label)
            result.fold(
                onSuccess = { suggestion ->
                    _aiCopyState.value = AiCopyResultState.Success(suggestion)
                    repository.recordAiGeneration(
                        currentUserId,
                        AiGenerationRecord(
                            userId = currentUserId,
                            mode = "COPYWRITER",
                            prompt = topic.trim(),
                            stylePreset = "Brand Director",
                            aspectRatio = format.id,
                            resultSummary = "${suggestion.headline} — ${suggestion.subheadline}"
                        )
                    )
                },
                onFailure = { err ->
                    _aiCopyState.value = AiCopyResultState.Error(
                        err.localizedMessage ?: "Failed to generate copy."
                    )
                }
            )
        }
    }

    // --- EDITOR LIFECYCLE ---

    fun openBlankFormat(format: CanvasFormat) {
        val initialLayers = listOf(
            CanvasLayer(
                id = "txt_badge",
                type = LayerType.TEXT,
                text = "NEW RELEASE",
                font = PosterFont.PLUS_JAKARTA,
                colorHex = "#111018",
                badgeBgHex = "#FFC44D",
                fontSizeSp = 13f,
                xFraction = 0.5f,
                yFraction = 0.22f,
                uppercase = true
            ),
            CanvasLayer(
                id = "txt_main",
                type = LayerType.TEXT,
                text = "CREATIVE\nSTUDIO",
                font = PosterFont.SPACE_GROTESK,
                colorHex = "#FFFFFF",
                fontSizeSp = 36f,
                xFraction = 0.5f,
                yFraction = 0.50f,
                uppercase = true
            ),
            CanvasLayer(
                id = "txt_sub",
                type = LayerType.TEXT,
                text = "Tap any layer to edit or drag",
                font = PosterFont.PLUS_JAKARTA,
                colorHex = "#E4DFFF",
                fontSizeSp = 15f,
                xFraction = 0.5f,
                yFraction = 0.78f
            )
        )
        val proj = DesignProject(
            userId = currentUserId,
            title = "New ${format.label}",
            category = format.defaultCategory.id,
            aspectRatio = format.id,
            backgroundType = BackgroundType.GRADIENT.id,
            backgroundColorHex = "#14121F",
            secondaryColorHex = "#FF5C39",
            overlayOpacity = 0.0,
            layers = initialLayers.map { it.toMap() }
        )
        _editorState.value = EditorState(
            project = proj,
            layers = initialLayers,
            selectedLayerId = "txt_main",
            activeBitmap = null
        )
    }

    fun openFromTemplate(template: StarterTemplate) {
        val proj = DesignProject(
            userId = currentUserId,
            title = template.title,
            category = template.category.id,
            aspectRatio = template.format.id,
            backgroundType = template.backgroundType.id,
            backgroundColorHex = template.backgroundColorHex,
            secondaryColorHex = template.secondaryColorHex,
            templateResKey = template.templateResKey,
            overlayOpacity = template.overlayOpacity.toDouble(),
            layers = template.layers.map { it.toMap() }
        )
        _editorState.value = EditorState(
            project = proj,
            layers = template.layers,
            selectedLayerId = template.layers.firstOrNull { it.type == LayerType.TEXT }?.id,
            activeBitmap = null
        )
    }

    fun openFromBitmap(
        bitmap: Bitmap,
        title: String,
        format: CanvasFormat,
        category: StudioCategory,
        localPath: String? = null
    ) {
        viewModelScope.launch {
            val resolvedPath = localPath ?: saveBitmapToDesignsDir(bitmap, "photo")
            val layers = if (category == StudioCategory.PHOTO_EDIT) {
                emptyList()
            } else {
                listOf(
                    CanvasLayer(
                        id = "ai_head",
                        type = LayerType.TEXT,
                        text = title.uppercase().take(28),
                        font = PosterFont.SPACE_GROTESK,
                        colorHex = "#FFFFFF",
                        fontSizeSp = 30f,
                        xFraction = 0.5f,
                        yFraction = 0.78f,
                        uppercase = true
                    )
                )
            }
            val proj = DesignProject(
                userId = currentUserId,
                title = title.take(100),
                category = category.id,
                aspectRatio = format.id,
                backgroundType = BackgroundType.LOCAL_IMAGE.id,
                backgroundColorHex = "#14121F",
                secondaryColorHex = "#FF5C39",
                localImagePath = resolvedPath,
                overlayOpacity = if (layers.isEmpty()) 0.0 else 0.18,
                layers = layers.map { it.toMap() }
            )
            _editorState.value = EditorState(
                project = proj,
                layers = layers,
                selectedLayerId = layers.firstOrNull()?.id,
                activeBitmap = bitmap
            )
        }
    }

    fun openExistingProject(project: DesignProject) {
        val loadedBitmap = if (project.backgroundType == BackgroundType.LOCAL_IMAGE.id && !project.localImagePath.isNullOrBlank()) {
            val file = File(project.localImagePath)
            if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
        } else null
        val layers = project.parsedLayers()
        _editorState.value = EditorState(
            project = project,
            layers = layers,
            selectedLayerId = layers.firstOrNull()?.id,
            activeBitmap = loadedBitmap
        )
    }

    fun applyCopySuggestionToEditor(suggestion: PosterCopySuggestion) {
        pushUndoAndUpdate { state ->
            val newLayers = listOf(
                CanvasLayer(
                    id = "cta_${UUID.randomUUID().toString().take(4)}",
                    type = LayerType.TEXT,
                    text = suggestion.ctaBadge,
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#111018",
                    badgeBgHex = suggestion.accentHex,
                    fontSizeSp = 13f,
                    xFraction = 0.5f,
                    yFraction = 0.18f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "head_${UUID.randomUUID().toString().take(4)}",
                    type = LayerType.TEXT,
                    text = suggestion.headline,
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 36f,
                    xFraction = 0.5f,
                    yFraction = 0.48f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "sub_${UUID.randomUUID().toString().take(4)}",
                    type = LayerType.TEXT,
                    text = suggestion.subheadline,
                    font = PosterFont.PLAYFAIR,
                    colorHex = "#F6F4FF",
                    fontSizeSp = 17f,
                    xFraction = 0.5f,
                    yFraction = 0.78f
                )
            )
            val updatedProject = state.project.copy(
                backgroundColorHex = suggestion.primaryHex,
                secondaryColorHex = suggestion.accentHex,
                layers = newLayers.map { it.toMap() }
            )
            state.copy(
                project = updatedProject,
                layers = newLayers,
                selectedLayerId = newLayers[1].id
            )
        }
        _snackbarMessages.tryEmit("Applied AI copy & palette to canvas")
    }

    private fun pushUndoAndUpdate(transform: (EditorState) -> EditorState) {
        val current = _editorState.value
        val snapshot = current.project to current.layers
        val nextUndo = (current.undoStack + snapshot).takeLast(20)
        val updated = transform(current)
        _editorState.value = updated.copy(
            undoStack = nextUndo,
            redoStack = emptyList()
        )
    }

    fun undo() {
        val current = _editorState.value
        val last = current.undoStack.lastOrNull() ?: return
        val currentSnapshot = current.project to current.layers
        _editorState.value = current.copy(
            project = last.first,
            layers = last.second,
            undoStack = current.undoStack.dropLast(1),
            redoStack = (current.redoStack + currentSnapshot).takeLast(20)
        )
    }

    fun redo() {
        val current = _editorState.value
        val next = current.redoStack.lastOrNull() ?: return
        val currentSnapshot = current.project to current.layers
        _editorState.value = current.copy(
            project = next.first,
            layers = next.second,
            undoStack = (current.undoStack + currentSnapshot).takeLast(20),
            redoStack = current.redoStack.dropLast(1)
        )
    }

    fun toggleGrid() {
        _editorState.value = _editorState.value.copy(showGrid = !_editorState.value.showGrid)
    }

    fun selectLayer(layerId: String?) {
        _editorState.value = _editorState.value.copy(selectedLayerId = layerId)
    }

    fun updateProjectTitle(title: String) {
        val current = _editorState.value
        _editorState.value = current.copy(
            project = current.project.copy(title = title.take(100))
        )
    }

    fun setCanvasFormat(format: CanvasFormat) {
        pushUndoAndUpdate { state ->
            state.copy(
                project = state.project.copy(
                    aspectRatio = format.id,
                    category = format.defaultCategory.id
                )
            )
        }
    }

    fun setGradientBackground(primaryHex: String, secondaryHex: String) {
        pushUndoAndUpdate { state ->
            state.copy(
                project = state.project.copy(
                    backgroundType = BackgroundType.GRADIENT.id,
                    backgroundColorHex = primaryHex,
                    secondaryColorHex = secondaryHex,
                    templateResKey = null
                ),
                activeBitmap = null
            )
        }
    }

    fun setEditorPhotoBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val path = saveBitmapToDesignsDir(bitmap, "canvas_img")
            pushUndoAndUpdate { state ->
                state.copy(
                    project = state.project.copy(
                        backgroundType = BackgroundType.LOCAL_IMAGE.id,
                        localImagePath = path,
                        templateResKey = null
                    ),
                    activeBitmap = bitmap
                )
            }
        }
    }

    fun setFilterPreset(preset: FilterPreset) {
        pushUndoAndUpdate { state ->
            state.copy(project = state.project.copy(filterPreset = preset.id))
        }
    }

    fun updatePhotoAdjustments(
        brightness: Double? = null,
        contrast: Double? = null,
        saturation: Double? = null,
        overlayOpacity: Double? = null
    ) {
        val current = _editorState.value
        val p = current.project
        _editorState.value = current.copy(
            project = p.copy(
                brightness = (brightness ?: p.brightness).coerceIn(-0.6, 0.6),
                contrast = (contrast ?: p.contrast).coerceIn(0.6, 1.8),
                saturation = (saturation ?: p.saturation).coerceIn(0.0, 2.0),
                overlayOpacity = (overlayOpacity ?: p.overlayOpacity).coerceIn(0.0, 0.8)
            )
        )
    }

    fun addTextLayer(text: String = "BOLD MESSAGE", font: PosterFont = PosterFont.SPACE_GROTESK) {
        if (_editorState.value.layers.size >= 25) {
            _snackbarMessages.tryEmit("Maximum 25 layers reached.")
            return
        }
        val newLayer = CanvasLayer(
            id = "txt_${UUID.randomUUID().toString().take(6)}",
            type = LayerType.TEXT,
            text = text,
            font = font,
            colorHex = "#FFFFFF",
            fontSizeSp = 28f,
            xFraction = 0.5f,
            yFraction = 0.5f
        )
        pushUndoAndUpdate { state ->
            val updatedLayers = state.layers + newLayer
            state.copy(
                layers = updatedLayers,
                selectedLayerId = newLayer.id,
                project = state.project.copy(layers = updatedLayers.map { it.toMap() })
            )
        }
    }

    fun addShapeLayer(shapeKind: ShapeKind, colorHex: String = "#FFC44D") {
        if (_editorState.value.layers.size >= 25) {
            _snackbarMessages.tryEmit("Maximum 25 layers reached.")
            return
        }
        val newLayer = CanvasLayer(
            id = "shp_${UUID.randomUUID().toString().take(6)}",
            type = LayerType.SHAPE,
            text = "",
            colorHex = colorHex,
            shapeKind = shapeKind,
            xFraction = 0.5f,
            yFraction = 0.5f,
            scale = 1.0f
        )
        pushUndoAndUpdate { state ->
            val updatedLayers = listOf(newLayer) + state.layers
            state.copy(
                layers = updatedLayers,
                selectedLayerId = newLayer.id,
                project = state.project.copy(layers = updatedLayers.map { it.toMap() })
            )
        }
    }

    fun updateSelectedLayer(transform: (CanvasLayer) -> CanvasLayer) {
        val current = _editorState.value
        val targetId = current.selectedLayerId ?: return
        val updatedLayers = current.layers.map { if (it.id == targetId) transform(it) else it }
        _editorState.value = current.copy(
            layers = updatedLayers,
            project = current.project.copy(layers = updatedLayers.map { it.toMap() })
        )
    }

    fun moveLayerPosition(layerId: String, dxFraction: Float, dyFraction: Float) {
        val current = _editorState.value
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == layerId) {
                layer.copy(
                    xFraction = (layer.xFraction + dxFraction).coerceIn(0.06f, 0.94f),
                    yFraction = (layer.yFraction + dyFraction).coerceIn(0.06f, 0.94f)
                )
            } else layer
        }
        _editorState.value = current.copy(
            layers = updatedLayers,
            project = current.project.copy(layers = updatedLayers.map { it.toMap() })
        )
    }

    fun deleteSelectedLayer() {
        val targetId = _editorState.value.selectedLayerId ?: return
        pushUndoAndUpdate { state ->
            val updatedLayers = state.layers.filterNot { it.id == targetId }
            state.copy(
                layers = updatedLayers,
                selectedLayerId = updatedLayers.lastOrNull()?.id,
                project = state.project.copy(layers = updatedLayers.map { it.toMap() })
            )
        }
    }

    fun saveActiveProject() {
        val state = _editorState.value
        viewModelScope.launch {
            _editorState.value = _editorState.value.copy(isSaving = true)
            val projectToSave = state.project.copy(
                userId = currentUserId,
                layers = state.layers.map { it.toMap() }
            )
            val result = repository.saveProject(currentUserId, projectToSave)
            result.fold(
                onSuccess = { savedId ->
                    _editorState.value = _editorState.value.copy(
                        isSaving = false,
                        project = projectToSave.copy(id = savedId)
                    )
                    _snackbarMessages.tryEmit("Saved \"${projectToSave.title}\" to Cloud Studio")
                },
                onFailure = { err ->
                    _editorState.value = _editorState.value.copy(isSaving = false)
                    _snackbarMessages.tryEmit("Save failed: ${err.localizedMessage ?: "Unknown error"}")
                }
            )
        }
    }

    fun saveAiResultAsProject(successState: AiImageResultState.Success) {
        viewModelScope.launch {
            val path = successState.localPath ?: saveBitmapToDesignsDir(successState.bitmap, "ai_saved")
            val project = DesignProject(
                userId = currentUserId,
                title = successState.prompt.take(40).ifBlank { "AI Poster" },
                category = if (successState.isPhotoEdit) StudioCategory.PHOTO_EDIT.id else successState.format.defaultCategory.id,
                aspectRatio = successState.format.id,
                backgroundType = BackgroundType.LOCAL_IMAGE.id,
                backgroundColorHex = "#14121F",
                secondaryColorHex = "#FF5C39",
                localImagePath = path,
                overlayOpacity = 0.0,
                layers = emptyList()
            )
            repository.saveProject(currentUserId, project).fold(
                onSuccess = { _snackbarMessages.tryEmit("Saved AI artwork to Projects") },
                onFailure = { _snackbarMessages.tryEmit("Could not save to Projects: ${it.localizedMessage}") }
            )
        }
    }

    fun toggleFavorite(project: DesignProject) {
        viewModelScope.launch {
            repository.toggleFavorite(currentUserId, project.id, !project.favorite).onFailure {
                _snackbarMessages.tryEmit("Could not update favorite status")
            }
        }
    }

    fun renameProject(project: DesignProject, newTitle: String) {
        viewModelScope.launch {
            repository.renameProject(currentUserId, project.id, newTitle).fold(
                onSuccess = { _snackbarMessages.tryEmit("Project renamed") },
                onFailure = { _snackbarMessages.tryEmit("Rename failed: ${it.localizedMessage}") }
            )
        }
    }

    fun deleteProject(project: DesignProject) {
        viewModelScope.launch {
            repository.deleteProject(currentUserId, project.id).fold(
                onSuccess = { _snackbarMessages.tryEmit("Project deleted") },
                onFailure = { _snackbarMessages.tryEmit("Delete failed: ${it.localizedMessage}") }
            )
        }
    }

    fun deleteAiHistoryRecord(record: AiGenerationRecord) {
        viewModelScope.launch {
            repository.deleteAiGeneration(currentUserId, record.id)
        }
    }

    fun notifyMessage(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    private suspend fun saveBitmapToDesignsDir(bitmap: Bitmap, prefix: String): String =
        withContext(Dispatchers.IO) {
            designsDir.mkdirs()
            val file = File(designsDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            val largest = max(bitmap.width, bitmap.height)
            val scaled = if (largest > 1600) {
                val ratio = 1600f / largest.toFloat()
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else bitmap
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.absolutePath
        }

    companion object {
        fun provideFactory(currentUserId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = StudioRepository(db)
                val ai = AiStudioService()
                val designsDir = File(app.filesDir, "designs")
                StudioViewModel(repo, ai, designsDir, currentUserId)
            }
        }
    }
}
