package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AiGenerationRecord
import com.example.data.model.CanvasFormat
import com.example.data.model.DesignProject
import com.example.data.model.StudioCategory
import com.example.data.model.UiState
import com.example.ui.components.PosterCanvasPreview
import com.example.ui.theme.CoralSpark
import com.example.ui.theme.ElectricViolet
import com.example.util.BitmapStudioUtils

@Composable
fun ProjectsScreen(
    projectsState: UiState<List<DesignProject>>,
    aiHistoryState: UiState<List<AiGenerationRecord>>,
    onOpenProject: (DesignProject) -> Unit,
    onToggleFavorite: (DesignProject) -> Unit,
    onRenameProject: (DesignProject, String) -> Unit,
    onDeleteProject: (DesignProject) -> Unit,
    onDeleteAiRecord: (AiGenerationRecord) -> Unit,
    onCreateNewPoster: () -> Unit,
    onOpenAiBitmapInEditor: (android.graphics.Bitmap, String, CanvasFormat) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var renamingProject by remember { mutableStateOf<DesignProject?>(null) }
    var renameText by remember { mutableStateOf("") }

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
                text = "My Creative Library",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("projects_tab_designs")
                ) {
                    Text("Saved Designs")
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("projects_tab_ai_history")
                ) {
                    Text("AI History")
                }
            }

            if (selectedTab == 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !favoritesOnly,
                        onClick = { favoritesOnly = false },
                        label = { Text("All Designs") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralSpark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("projects_filter_all")
                    )
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = true },
                        label = { Text("Favorites") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralSpark,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("projects_filter_favorites")
                    )
                }
            }
        }

        if (selectedTab == 0) {
            when (projectsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CoralSpark)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = projectsState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is UiState.Success -> {
                    val items = remember(projectsState.data, favoritesOnly) {
                        if (favoritesOnly) projectsState.data.filter { it.favorite }
                        else projectsState.data
                    }
                    if (items.isEmpty()) {
                        EmptyLibraryState(
                            title = if (favoritesOnly) "No favorite designs yet" else "No saved designs yet",
                            subtitle = if (favoritesOnly) {
                                "Tap the heart icon on any saved poster or banner to pin it here."
                            } else {
                                "Create a poster, banner, or AI photo remix and tap Save to sync it to your cloud library."
                            },
                            actionLabel = "New Poster Canvas",
                            onAction = onCreateNewPoster
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(items, key = { it.id }) { project ->
                                SavedProjectCard(
                                    project = project,
                                    onOpen = { onOpenProject(project) },
                                    onToggleFavorite = { onToggleFavorite(project) },
                                    onRename = {
                                        renamingProject = project
                                        renameText = project.title
                                    },
                                    onDelete = { onDeleteProject(project) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            when (aiHistoryState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricViolet)
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = aiHistoryState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is UiState.Success -> {
                    if (aiHistoryState.data.isEmpty()) {
                        EmptyLibraryState(
                            title = "No AI generations yet",
                            subtitle = "Every AI poster, photo edit, and copywriting session you run in AI Lab will appear here.",
                            actionLabel = "Start a New Poster",
                            onAction = onCreateNewPoster
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(aiHistoryState.data, key = { it.id }) { record ->
                                AiHistoryCard(
                                    record = record,
                                    onOpenInEditor = { bmp ->
                                        onOpenAiBitmapInEditor(
                                            bmp,
                                            record.prompt.take(28),
                                            CanvasFormat.fromRatio(record.aspectRatio)
                                        )
                                    },
                                    onDelete = { onDeleteAiRecord(record) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    renamingProject?.let { target ->
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            title = { Text("Rename Design") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(100) },
                    singleLine = true,
                    label = { Text("Project Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_project_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameProject(target, renameText)
                        }
                        renamingProject = null
                    },
                    modifier = Modifier.testTag("confirm_rename_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SavedProjectCard(
    project: DesignProject,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpen,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.Center
            ) {
                PosterCanvasPreview(
                    project = project,
                    layers = project.parsedLayers()
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.aspectRatio} • ${StudioCategory.fromId(project.category).label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${project.parsedLayers().size} layers • ${project.filterPreset}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("fav_project_${project.id}")
                ) {
                    Icon(
                        imageVector = if (project.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Toggle favorite",
                        tint = if (project.favorite) CoralSpark else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRename,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("rename_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Rename project",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete project",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AiHistoryCard(
    record: AiGenerationRecord,
    onOpenInEditor: (android.graphics.Bitmap) -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(record.localImagePath) {
        BitmapStudioUtils.loadBitmapFromPath(record.localImagePath)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_history_card_${record.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = record.prompt,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElectricViolet.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricViolet
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = record.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${record.mode.replace("_", " ")} • ${record.stylePreset}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricViolet
                )
                if (!record.resultSummary.isNullOrBlank()) {
                    Text(
                        text = record.resultSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (bitmap != null) {
                    TextButton(
                        onClick = { onOpenInEditor(bitmap) },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Open in Studio", color = CoralSpark)
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_ai_history_${record.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete history entry",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CoralSpark.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = CoralSpark,
                modifier = Modifier.size(34.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = CoralSpark, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("empty_state_new_poster_button")
        ) {
            Text(actionLabel)
        }
    }
}
