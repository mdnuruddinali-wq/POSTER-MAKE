package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CanvasFormat
import com.example.data.model.PosterCopySuggestion
import com.example.data.model.TemplateCatalog
import com.example.data.model.parseHexColor
import com.example.ui.theme.CoralSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.WarmGold
import com.example.ui.viewmodel.AiCopyResultState
import com.example.ui.viewmodel.AiImageResultState
import com.example.util.BitmapStudioUtils
import kotlinx.coroutines.launch

@Composable
fun AiLabScreen(
    initialMode: Int = 0,
    aiImageState: AiImageResultState,
    aiPhotoEditState: AiImageResultState,
    aiCopyState: AiCopyResultState,
    pickedPhoto: Bitmap?,
    onSetPickedPhoto: (Bitmap?) -> Unit,
    onGeneratePoster: (String, String, CanvasFormat, Boolean) -> Unit,
    onApplyAiPhotoEdit: (String) -> Unit,
    onGenerateCopy: (String, CanvasFormat) -> Unit,
    onOpenGeneratedInEditor: (AiImageResultState.Success) -> Unit,
    onSaveGeneratedToProjects: (AiImageResultState.Success) -> Unit,
    onApplyCopyToNewPoster: (PosterCopySuggestion) -> Unit,
    onNotify: (String) -> Unit
) {
    var selectedTab by rememberSaveable(initialMode) { mutableIntStateOf(initialMode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "AI Creative Lab",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    modifier = Modifier.testTag("ai_tab_poster_gen")
                ) {
                    Text("Poster / Banner")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    modifier = Modifier.testTag("ai_tab_photo_edit")
                ) {
                    Text("Photo Edit")
                }
                SegmentedButton(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    modifier = Modifier.testTag("ai_tab_copywriter")
                ) {
                    Text("Copy & Palette")
                }
            }
        }

        when (selectedTab) {
            0 -> AiPosterGeneratorTab(
                state = aiImageState,
                onGenerate = onGeneratePoster,
                onOpenInEditor = onOpenGeneratedInEditor,
                onSaveToProjects = onSaveGeneratedToProjects,
                onNotify = onNotify
            )
            1 -> AiPhotoEditorTab(
                pickedPhoto = pickedPhoto,
                state = aiPhotoEditState,
                onSetPickedPhoto = onSetPickedPhoto,
                onApplyEdit = onApplyAiPhotoEdit,
                onOpenInEditor = onOpenGeneratedInEditor,
                onSaveToProjects = onSaveGeneratedToProjects,
                onNotify = onNotify
            )
            else -> AiCopywriterTab(
                state = aiCopyState,
                onGenerateCopy = onGenerateCopy,
                onApplyCopyToNewPoster = onApplyCopyToNewPoster,
                onNotify = onNotify
            )
        }
    }
}

@Composable
private fun AiPosterGeneratorTab(
    state: AiImageResultState,
    onGenerate: (String, String, CanvasFormat, Boolean) -> Unit,
    onOpenInEditor: (AiImageResultState.Success) -> Unit,
    onSaveToProjects: (AiImageResultState.Success) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by rememberSaveable { mutableStateOf("") }
    var selectedStyle by rememberSaveable { mutableStateOf(TemplateCatalog.stylePresets.first()) }
    var selectedFormat by remember { mutableStateOf(CanvasFormat.POSTER_3_4) }
    var leaveSpaceForText by rememberSaveable { mutableStateOf(true) }

    val quickPrompts = listOf(
        "Midnight indie music festival with neon laser beams",
        "Luxury skincare product on sunlit travertine stone",
        "Flash sneaker drop poster in bold street editorial style",
        "Artisan matcha cafe grand opening announcement banner"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it.take(500) },
            label = { Text("Describe your poster or banner") },
            placeholder = { Text("e.g., Retro sunset synthwave concert poster with palm silhouettes") },
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_poster_prompt_input")
        )

        // Inspiration Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { sample ->
                FilterChip(
                    selected = prompt == sample,
                    onClick = { prompt = sample },
                    label = { Text(sample.take(28) + "…") },
                    modifier = Modifier.testTag("ai_prompt_chip_${sample.hashCode()}")
                )
            }
        }

        // Format Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Format & Aspect Ratio", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        CanvasFormat.POSTER_3_4,
                        CanvasFormat.BANNER_16_9,
                        CanvasFormat.SQUARE_1_1,
                        CanvasFormat.STORY_9_16
                    )
                ) { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { selectedFormat = fmt },
                        label = { Text("${fmt.label} (${fmt.ratioLabel})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralSpark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("ai_format_${fmt.id}")
                    )
                }
            }
        }

        // Style Preset Selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Art Direction Style", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(TemplateCatalog.stylePresets) { style ->
                    FilterChip(
                        selected = selectedStyle == style,
                        onClick = { selectedStyle = style },
                        label = { Text(style) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("ai_style_${style.replace(" ", "_").lowercase()}")
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Typography-Ready Composition", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Leaves balanced negative space for headline overlays",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = leaveSpaceForText,
                onCheckedChange = { leaveSpaceForText = it },
                modifier = Modifier.testTag("ai_negative_space_switch")
            )
        }

        Button(
            onClick = { onGenerate(prompt, selectedStyle, selectedFormat, leaveSpaceForText) },
            enabled = state !is AiImageResultState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("ai_generate_poster_button")
        ) {
            if (state is AiImageResultState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Synthesizing...")
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate ${selectedFormat.label}", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Result Display
        AiImageResultCard(
            state = state,
            onOpenInEditor = onOpenInEditor,
            onSaveToProjects = onSaveToProjects,
            onShare = { bmp, title ->
                scope.launch {
                    val intent = BitmapStudioUtils.createShareIntent(context, bmp, title)
                    if (intent != null) {
                        context.startActivity(android.content.Intent.createChooser(intent, "Share AI Design"))
                    } else {
                        onNotify("Could not prepare share image.")
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AiPhotoEditorTab(
    pickedPhoto: Bitmap?,
    state: AiImageResultState,
    onSetPickedPhoto: (Bitmap?) -> Unit,
    onApplyEdit: (String) -> Unit,
    onOpenInEditor: (AiImageResultState.Success) -> Unit,
    onSaveToProjects: (AiImageResultState.Success) -> Unit,
    onNotify: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editPrompt by rememberSaveable { mutableStateOf("") }
    var showOriginal by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapStudioUtils.loadBitmapFromUri(context, uri)
                if (bmp != null) {
                    onSetPickedPhoto(bmp)
                } else {
                    onNotify("Unable to decode selected image.")
                }
            }
        }
    }

    val quickEdits = listOf(
        "Studio Relight" to "Apply crisp commercial studio rim lighting and clean depth of field",
        "Sunlit Marble" to "Replace the background with a sunlit minimalist travertine studio backdrop",
        "Golden Hour" to "Add warm golden hour cinematic sunlight and subtle Kodak film tones",
        "Cyber Neon" to "Transform lighting into vibrant magenta and cyan neon night atmosphere",
        "Retro Print" to "Stylize as a bold risograph editorial print with rich grain and contrast"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            onClick = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pick_photo_for_ai_edit")
        ) {
            if (pickedPhoto == null) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddPhotoAlternate,
                        contentDescription = stringResource(id = R.string.cd_picked_photo),
                        tint = ElectricViolet,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("Select a Photo to Edit", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Uses Android Photo Picker with zero storage permissions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Image(
                        bitmap = pickedPhoto.asImageBitmap(),
                        contentDescription = stringResource(id = R.string.cd_picked_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Surface(
                        color = Color(0xCC111018),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Tap to change photo",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("1-Tap AI Photo Presets", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickEdits) { (label, instruction) ->
                    FilterChip(
                        selected = editPrompt == instruction,
                        onClick = { editPrompt = instruction },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("ai_photo_preset_${label.replace(" ", "_").lowercase()}")
                    )
                }
            }
        }

        OutlinedTextField(
            value = editPrompt,
            onValueChange = { editPrompt = it.take(500) },
            label = { Text("Describe your AI photo edit") },
            placeholder = { Text("e.g., Remove background clutter and add dramatic studio spotlight") },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_photo_edit_instruction_input")
        )

        Button(
            onClick = { onApplyEdit(editPrompt) },
            enabled = state !is AiImageResultState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("apply_ai_photo_edit_button")
        ) {
            if (state is AiImageResultState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Transforming Photo...")
            } else {
                Icon(Icons.Filled.PhotoFilter, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply AI Photo Edit", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (state is AiImageResultState.Success && pickedPhoto != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Compare with Original", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = showOriginal,
                    onCheckedChange = { showOriginal = it },
                    modifier = Modifier.testTag("compare_before_after_switch")
                )
            }
        }

        if (showOriginal && pickedPhoto != null && state is AiImageResultState.Success) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = pickedPhoto.asImageBitmap(),
                    contentDescription = "Original photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            AiImageResultCard(
                state = state,
                onOpenInEditor = onOpenInEditor,
                onSaveToProjects = onSaveToProjects,
                onShare = { bmp, title ->
                    scope.launch {
                        val intent = BitmapStudioUtils.createShareIntent(context, bmp, title)
                        if (intent != null) {
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Edited Photo"))
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun AiCopywriterTab(
    state: AiCopyResultState,
    onGenerateCopy: (String, CanvasFormat) -> Unit,
    onApplyCopyToNewPoster: (PosterCopySuggestion) -> Unit,
    onNotify: (String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var topic by rememberSaveable { mutableStateOf("") }
    var format by remember { mutableStateOf(CanvasFormat.POSTER_3_4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it.take(300) },
            label = { Text("Campaign, product, or event topic") },
            placeholder = { Text("e.g., Weekend pottery workshop for beginners in Brooklyn") },
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_copy_topic_input")
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(CanvasFormat.entries) { fmt ->
                FilterChip(
                    selected = format == fmt,
                    onClick = { format = fmt },
                    label = { Text(fmt.label) },
                    modifier = Modifier.testTag("copy_format_${fmt.id}")
                )
            }
        }

        Button(
            onClick = { onGenerateCopy(topic, format) },
            enabled = state !is AiCopyResultState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = WarmGold, contentColor = Color(0xFF241800)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("generate_ai_copy_button")
        ) {
            if (state is AiCopyResultState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Writing Copy & Palette...")
            } else {
                Icon(Icons.Filled.Palette, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate Headlines & Palette", style = MaterialTheme.typography.titleMedium)
            }
        }

        when (state) {
            is AiCopyResultState.Idle -> {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Gemini 3.8 Flash crafts punchy poster headlines, CTA badges, social captions, and a matching color palette.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
            is AiCopyResultState.Loading -> {
                ShimmerLoadingBox(label = "Generating structured copy & colors...")
            }
            is AiCopyResultState.Error -> {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is AiCopyResultState.Success -> {
                val s = state.suggestion
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseHexColor(s.primaryHex))
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(parseHexColor(s.accentHex))
                            )
                            Text(
                                text = "${s.primaryHex} / ${s.accentHex}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Surface(
                            color = parseHexColor(s.accentHex),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            Text(
                                text = s.ctaBadge,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Text(text = s.headline, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = s.subheadline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.socialCaption,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(s.socialCaption))
                                        onNotify("Caption copied to clipboard")
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("copy_caption_button")
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy caption")
                                }
                            }
                        }

                        Button(
                            onClick = { onApplyCopyToNewPoster(s) },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("apply_copy_to_canvas_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create Poster with This Copy")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiImageResultCard(
    state: AiImageResultState,
    onOpenInEditor: (AiImageResultState.Success) -> Unit,
    onSaveToProjects: (AiImageResultState.Success) -> Unit,
    onShare: (Bitmap, String) -> Unit
) {
    when (state) {
        is AiImageResultState.Idle -> Unit
        is AiImageResultState.Loading -> {
            ShimmerLoadingBox(label = state.label)
        }
        is AiImageResultState.Error -> {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_error_banner")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AI Generation Error",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        is AiImageResultState.Success -> {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_result_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        bitmap = state.bitmap.asImageBitmap(),
                        contentDescription = stringResource(id = R.string.cd_generated_image),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.FillWidth
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onOpenInEditor(state) },
                            colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_ai_in_editor_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit in Studio")
                        }

                        OutlinedButton(
                            onClick = { onSaveToProjects(state) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_ai_to_projects_button")
                        ) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = "Save to projects")
                        }

                        OutlinedButton(
                            onClick = { onShare(state.bitmap, state.prompt) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("share_ai_image_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Share image")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShimmerLoadingBox(label: String) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E1B2E),
            Color(0xFF332D4F),
            Color(0xFF1E1B2E)
        ),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 300f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            CircularProgressIndicator(color = WarmGold)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = "Usually takes 15–45 seconds",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFB5B0CC)
            )
        }
    }
}
