package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CanvasFormat
import com.example.data.model.DesignProject
import com.example.data.model.StarterTemplate
import com.example.data.model.StudioCategory
import com.example.data.model.TemplateCatalog
import com.example.data.model.UiState
import com.example.ui.components.PosterCanvasPreview
import com.example.ui.theme.CoralSpark
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.WarmGold
import com.example.util.BitmapStudioUtils
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    userName: String,
    userPhotoUrl: String?,
    projectsState: UiState<List<DesignProject>>,
    onSignOut: () -> Unit,
    onOpenAiPosterTab: () -> Unit,
    onOpenAiPhotoTab: () -> Unit,
    onCreateBlankFormat: (CanvasFormat) -> Unit,
    onOpenTemplate: (StarterTemplate) -> Unit,
    onOpenProject: (DesignProject) -> Unit,
    onOpenPickedPhotoInEditor: (android.graphics.Bitmap) -> Unit,
    onViewAllTemplates: () -> Unit,
    onViewAllProjects: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val quickPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = BitmapStudioUtils.loadBitmapFromUri(context, uri)
                if (bmp != null) {
                    onOpenPickedPhotoInEditor(bmp)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Top Header Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!userPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = userPhotoUrl,
                            contentDescription = stringResource(id = R.string.cd_user_avatar),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(CoralSpark.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.take(1).uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = CoralSpark
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "PosterStudio",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Welcome back, ${userName.substringBefore(" ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = stringResource(id = R.string.sign_out),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Featured Hero Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onOpenAiPosterTab() }
                    .testTag("hero_ai_generate_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_studio),
                    contentDescription = stringResource(id = R.string.cd_hero_banner),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xEB111018),
                                    Color(0x99111018),
                                    Color(0x33111018)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        color = WarmGold.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "GEMINI AI ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGold
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Generate Posters\n& Social Banners",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Turn any idea into layered studio artwork.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDCD8EE)
                        )
                    }

                    Button(
                        onClick = onOpenAiPosterTab,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralSpark,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("hero_create_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create with AI", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Quick Studio Actions Row (Poster, Banner, Photo AI, Edit Device Photo)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Start",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        QuickActionTile(
                            icon = Icons.Filled.AutoAwesome,
                            title = "AI Poster",
                            subtitle = "3:4 & 4:5",
                            accent = CoralSpark,
                            tag = "quick_ai_poster",
                            onClick = onOpenAiPosterTab
                        )
                    }
                    item {
                        QuickActionTile(
                            icon = Icons.Filled.PhotoFilter,
                            title = "AI Photo Edit",
                            subtitle = "Relight & Remix",
                            accent = ElectricViolet,
                            tag = "quick_ai_photo",
                            onClick = onOpenAiPhotoTab
                        )
                    }
                    item {
                        QuickActionTile(
                            icon = Icons.Filled.Image,
                            title = "Edit Photo",
                            subtitle = "Pick from Device",
                            accent = WarmGold,
                            tag = "quick_pick_photo",
                            onClick = {
                                quickPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                    item {
                        QuickActionTile(
                            icon = Icons.Filled.ViewCarousel,
                            title = "Web Banner",
                            subtitle = "16:9 Canvas",
                            accent = Color(0xFF00D2B8),
                            tag = "quick_banner_canvas",
                            onClick = { onCreateBlankFormat(CanvasFormat.BANNER_16_9) }
                        )
                    }
                    item {
                        QuickActionTile(
                            icon = Icons.Filled.AspectRatio,
                            title = "Social Post",
                            subtitle = "1:1 Square",
                            accent = Color(0xFFFF7A9E),
                            tag = "quick_square_canvas",
                            onClick = { onCreateBlankFormat(CanvasFormat.SQUARE_1_1) }
                        )
                    }
                    item {
                        QuickActionTile(
                            icon = Icons.Filled.Add,
                            title = "Story / Reel",
                            subtitle = "9:16 Vertical",
                            accent = Color(0xFF70A1FF),
                            tag = "quick_story_canvas",
                            onClick = { onCreateBlankFormat(CanvasFormat.STORY_9_16) }
                        )
                    }
                }
            }
        }

        // Featured Starter Templates
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Featured Templates",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onViewAllTemplates,
                        modifier = Modifier.testTag("home_see_all_templates")
                    ) {
                        Text("See all", color = CoralSpark)
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(TemplateCatalog.templates, key = { it.id }) { tpl ->
                        TemplatePreviewCard(
                            template = tpl,
                            onClick = { onOpenTemplate(tpl) }
                        )
                    }
                }
            }
        }

        // Recent Cloud Projects
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Cloud Designs",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onViewAllProjects,
                        modifier = Modifier.testTag("home_see_all_projects")
                    ) {
                        Text("Manage", color = CoralSpark)
                    }
                }

                when (projectsState) {
                    is UiState.Loading -> {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Syncing your saved designs...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(18.dp)
                            )
                        }
                    }
                    is UiState.Error -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = projectsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (projectsState.data.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Your cloud studio is ready",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Start from any template above or generate a custom poster in AI Lab, then save it to access across devices.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                projectsState.data.take(3).forEach { project ->
                                    RecentProjectRowCard(
                                        project = project,
                                        onClick = { onOpenProject(project) }
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

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(124.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TemplatePreviewCard(
    template: StarterTemplate,
    onClick: () -> Unit
) {
    val previewProject = com.example.data.model.DesignProject(
        title = template.title,
        category = template.category.id,
        aspectRatio = template.format.id,
        backgroundType = template.backgroundType.id,
        backgroundColorHex = template.backgroundColorHex,
        secondaryColorHex = template.secondaryColorHex,
        templateResKey = template.templateResKey,
        overlayOpacity = template.overlayOpacity.toDouble()
    )
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(172.dp)
            .testTag("template_card_${template.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                contentAlignment = Alignment.Center
            ) {
                PosterCanvasPreview(
                    project = previewProject,
                    layers = template.layers
                )
            }
            Column {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${template.format.ratioLabel} • ${template.category.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecentProjectRowCard(
    project: DesignProject,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_project_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                PosterCanvasPreview(
                    project = project,
                    layers = project.parsedLayers()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.aspectRatio} • ${StudioCategory.fromId(project.category).label} • ${project.parsedLayers().size} layers",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (project.favorite) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = CoralSpark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
