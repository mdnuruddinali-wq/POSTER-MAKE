package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackgroundType
import com.example.data.model.CanvasFormat
import com.example.data.model.CanvasLayer
import com.example.data.model.DesignProject
import com.example.data.model.FilterPreset
import com.example.data.model.LayerType
import com.example.data.model.PosterFont
import com.example.data.model.ShapeKind
import com.example.data.model.TemplateCatalog
import com.example.data.model.parseHexColor
import com.example.ui.theme.CoralSpark
import com.example.util.BitmapStudioUtils
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PosterCanvasPreview(
    project: DesignProject,
    layers: List<CanvasLayer>,
    modifier: Modifier = Modifier,
    activeBitmap: Bitmap? = null,
    selectedLayerId: String? = null,
    showGrid: Boolean = false,
    interactive: Boolean = false,
    onSelectLayer: ((String?) -> Unit)? = null,
    onMoveLayer: ((String, Float, Float) -> Unit)? = null
) {
    val format = remember(project.aspectRatio) { CanvasFormat.fromRatio(project.aspectRatio) }
    val primaryColor = remember(project.backgroundColorHex) {
        parseHexColor(project.backgroundColorHex, Color(0xFF14121F))
    }
    val secondaryColor = remember(project.secondaryColorHex) {
        parseHexColor(project.secondaryColorHex, Color(0xFFFF5C39))
    }
    val bgType = remember(project.backgroundType) { BackgroundType.fromId(project.backgroundType) }

    val loadedLocalBitmap = remember(project.localImagePath, activeBitmap) {
        activeBitmap ?: if (bgType == BackgroundType.LOCAL_IMAGE) {
            BitmapStudioUtils.loadBitmapFromPath(project.localImagePath)
        } else null
    }
    val templateResId = remember(project.templateResKey) {
        TemplateCatalog.drawableForKey(project.templateResKey)
    }

    val colorFilter = remember(
        project.filterPreset,
        project.brightness,
        project.contrast,
        project.saturation
    ) {
        val androidMatrix = BitmapStudioUtils.buildColorMatrix(
            filterPreset = FilterPreset.fromId(project.filterPreset),
            brightness = project.brightness.toFloat(),
            contrast = project.contrast.toFloat(),
            saturation = project.saturation.toFloat()
        )
        ColorFilter.colorMatrix(ColorMatrix(androidMatrix.array))
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(format.widthRatio / format.heightRatio)
            .clip(RoundedCornerShape(14.dp))
            .background(
                when (bgType) {
                    BackgroundType.SOLID -> Brush.linearGradient(listOf(primaryColor, primaryColor))
                    else -> Brush.linearGradient(listOf(primaryColor, secondaryColor))
                }
            )
            .pointerInput(interactive) {
                if (interactive) {
                    detectTapGestures { onSelectLayer?.invoke(null) }
                }
            }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val scaleFactor = (widthPx / 360f).coerceAtLeast(0.25f)

        // Background image if present
        if (loadedLocalBitmap != null) {
            Image(
                bitmap = loadedLocalBitmap.asImageBitmap(),
                contentDescription = project.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter
            )
        } else if (bgType == BackgroundType.TEMPLATE_RES && templateResId != null) {
            Image(
                painter = painterResource(id = templateResId),
                contentDescription = project.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter
            )
        }

        // Scrim overlay
        if (project.overlayOpacity > 0.01) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0C0A14).copy(alpha = project.overlayOpacity.toFloat().coerceIn(0f, 0.85f)))
            )
        }

        // Shape layers rendered behind text or in order
        layers.forEach { layer ->
            val cxPx = layer.xFraction * widthPx
            val cyPx = layer.yFraction * heightPx
            val isSelected = interactive && layer.id == selectedLayerId
            val layerColor = parseHexColor(layer.colorHex, Color.White)

            when (layer.type) {
                LayerType.SHAPE -> {
                    when (layer.shapeKind) {
                        ShapeKind.FRAME_BORDER -> {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val inset = 18f * scaleFactor
                                drawRoundRect(
                                    color = layerColor.copy(alpha = 0.82f),
                                    topLeft = Offset(inset, inset),
                                    size = androidx.compose.ui.geometry.Size(
                                        size.width - inset * 2f,
                                        size.height - inset * 2f
                                    ),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f * scaleFactor),
                                    style = Stroke(width = 2.5f * scaleFactor * layer.scale)
                                )
                            }
                        }
                        ShapeKind.CIRCLE_GLOW -> {
                            val diameterDp = with(density) {
                                (min(widthPx, heightPx) * 0.28f * layer.scale).toDp()
                            }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        val half = with(density) { (diameterDp / 2).toPx() }
                                        IntOffset((cxPx - half).roundToInt(), (cyPx - half).roundToInt())
                                    }
                                    .size(diameterDp)
                                    .clip(CircleShape)
                                    .background(layerColor.copy(alpha = 0.72f))
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, CoralSpark, CircleShape)
                                        else Modifier
                                    )
                                    .layerPointerModifier(
                                        interactive = interactive,
                                        layerId = layer.id,
                                        widthPx = widthPx,
                                        heightPx = heightPx,
                                        onSelectLayer = onSelectLayer,
                                        onMoveLayer = onMoveLayer
                                    )
                            )
                        }
                        ShapeKind.PILL_BADGE -> {
                            val wDp = with(density) { (widthPx * 0.36f * layer.scale).toDp() }
                            val hDp = with(density) { (34f * scaleFactor * layer.scale).toDp() }
                            Box(
                                modifier = Modifier
                                    .offset {
                                        val hw = with(density) { (wDp / 2).toPx() }
                                        val hh = with(density) { (hDp / 2).toPx() }
                                        IntOffset((cxPx - hw).roundToInt(), (cyPx - hh).roundToInt())
                                    }
                                    .size(wDp, hDp)
                                    .clip(RoundedCornerShape(50))
                                    .background(layerColor)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, CoralSpark, RoundedCornerShape(50))
                                        else Modifier
                                    )
                                    .layerPointerModifier(
                                        interactive = interactive,
                                        layerId = layer.id,
                                        widthPx = widthPx,
                                        heightPx = heightPx,
                                        onSelectLayer = onSelectLayer,
                                        onMoveLayer = onMoveLayer
                                    )
                            )
                        }
                        ShapeKind.DIAMOND_SPARK -> {
                            val sizeDp = with(density) { (min(widthPx, heightPx) * 0.16f * layer.scale).toDp() }
                            Canvas(
                                modifier = Modifier
                                    .offset {
                                        val half = with(density) { (sizeDp / 2).toPx() }
                                        IntOffset((cxPx - half).roundToInt(), (cyPx - half).roundToInt())
                                    }
                                    .size(sizeDp)
                                    .then(
                                        if (isSelected) Modifier.border(1.5.dp, CoralSpark, RoundedCornerShape(6.dp))
                                        else Modifier
                                    )
                                    .layerPointerModifier(
                                        interactive = interactive,
                                        layerId = layer.id,
                                        widthPx = widthPx,
                                        heightPx = heightPx,
                                        onSelectLayer = onSelectLayer,
                                        onMoveLayer = onMoveLayer
                                    )
                            ) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, 0f)
                                    lineTo(size.width * 0.9f, size.height / 2f)
                                    lineTo(size.width / 2f, size.height)
                                    lineTo(size.width * 0.1f, size.height / 2f)
                                    close()
                                }
                                drawPath(path, color = layerColor)
                            }
                        }
                    }
                }
                LayerType.TEXT -> {
                    val displayText = if (layer.uppercase) layer.text.uppercase() else layer.text
                    if (displayText.isNotBlank()) {
                        val fontFamily = remember(layer.font) {
                            when (layer.font) {
                                PosterFont.SPACE_GROTESK -> FontFamily(Font(PosterFont.SPACE_GROTESK.fontRes, FontWeight.Bold))
                                PosterFont.PLUS_JAKARTA -> FontFamily(Font(PosterFont.PLUS_JAKARTA.fontRes, FontWeight.SemiBold))
                                PosterFont.PLAYFAIR -> FontFamily(Font(PosterFont.PLAYFAIR.fontRes, FontWeight.Bold))
                                PosterFont.PACIFICO -> FontFamily(Font(PosterFont.PACIFICO.fontRes, FontWeight.Normal))
                            }
                        }
                        val scaledFontSp = (layer.fontSizeSp * scaleFactor).coerceAtLeast(6f).sp
                        val badgeColor = if (layer.badgeBgHex.isNotBlank()) {
                            parseHexColor(layer.badgeBgHex, CoralSpark)
                        } else Color.Transparent

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = (16f * scaleFactor).dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Surface(
                                color = badgeColor,
                                shape = RoundedCornerShape(999.dp),
                                border = if (isSelected) BorderStroke(1.5.dp, CoralSpark) else null,
                                modifier = Modifier
                                    .offset {
                                        val estHalfY = with(density) { (scaledFontSp.toPx() * 0.8f) }
                                        val dx = (layer.xFraction - 0.5f) * widthPx
                                        IntOffset(dx.roundToInt(), (cyPx - estHalfY).roundToInt())
                                    }
                                    .layerPointerModifier(
                                        interactive = interactive,
                                        layerId = layer.id,
                                        widthPx = widthPx,
                                        heightPx = heightPx,
                                        onSelectLayer = onSelectLayer,
                                        onMoveLayer = onMoveLayer
                                    )
                            ) {
                                Text(
                                    text = displayText,
                                    color = layerColor,
                                    textAlign = TextAlign.Center,
                                    style = TextStyle(
                                        fontFamily = fontFamily,
                                        fontSize = scaledFontSp,
                                        lineHeight = scaledFontSp * 1.12f,
                                        letterSpacing = if (layer.uppercase) 0.8.sp else 0.sp
                                    ),
                                    modifier = Modifier.padding(
                                        horizontal = if (layer.badgeBgHex.isNotBlank()) (14f * scaleFactor).dp else (4f * scaleFactor).dp,
                                        vertical = if (layer.badgeBgHex.isNotBlank()) (6f * scaleFactor).dp else (2f * scaleFactor).dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Alignment guide overlay when toggled on in editor
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val guideColor = Color.White.copy(alpha = 0.28f)
                val stroke = 1.dp.toPx()
                drawLine(guideColor, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), stroke)
                drawLine(guideColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), stroke)
                drawLine(guideColor.copy(alpha = 0.14f), Offset(size.width / 3f, 0f), Offset(size.width / 3f, size.height), stroke)
                drawLine(guideColor.copy(alpha = 0.14f), Offset(size.width * 2f / 3f, 0f), Offset(size.width * 2f / 3f, size.height), stroke)
            }
        }
    }
}

private fun Modifier.layerPointerModifier(
    interactive: Boolean,
    layerId: String,
    widthPx: Float,
    heightPx: Float,
    onSelectLayer: ((String?) -> Unit)?,
    onMoveLayer: ((String, Float, Float) -> Unit)?
): Modifier {
    if (!interactive) return this
    return this
        .pointerInput(layerId) {
            detectTapGestures {
                onSelectLayer?.invoke(layerId)
            }
        }
        .pointerInput(layerId, widthPx, heightPx) {
            detectDragGestures(
                onDragStart = { onSelectLayer?.invoke(layerId) },
                onDrag = { change, dragAmount ->
                    change.consume()
                    if (widthPx > 0f && heightPx > 0f) {
                        onMoveLayer?.invoke(
                            layerId,
                            dragAmount.x / widthPx,
                            dragAmount.y / heightPx
                        )
                    }
                }
            )
        }
}
