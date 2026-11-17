package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.data.model.BackgroundType
import com.example.data.model.CanvasFormat
import com.example.data.model.CanvasLayer
import com.example.data.model.DesignProject
import com.example.data.model.FilterPreset
import com.example.data.model.LayerType
import com.example.data.model.ShapeKind
import com.example.data.model.TemplateCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

object BitmapStudioUtils {

    fun buildColorMatrix(
        filterPreset: FilterPreset,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ): ColorMatrix {
        val result = ColorMatrix()

        // Preset base matrix
        when (filterPreset) {
            FilterPreset.ORIGINAL -> Unit
            FilterPreset.VIVID -> {
                val sat = ColorMatrix()
                sat.setSaturation(1.38f)
                result.postConcat(sat)
            }
            FilterPreset.NOIR -> {
                val gray = ColorMatrix()
                gray.setSaturation(0f)
                result.postConcat(gray)
                val highContrast = ColorMatrix(
                    floatArrayOf(
                        1.25f, 0f, 0f, 0f, -20f,
                        0f, 1.25f, 0f, 0f, -20f,
                        0f, 0f, 1.25f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                result.postConcat(highContrast)
            }
            FilterPreset.WARM_FILM -> {
                val warm = ColorMatrix(
                    floatArrayOf(
                        1.12f, 0.04f, 0f, 0f, 10f,
                        0f, 1.03f, 0f, 0f, 4f,
                        0f, 0f, 0.88f, 0f, -6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                result.postConcat(warm)
            }
            FilterPreset.COOL_STUDIO -> {
                val cool = ColorMatrix(
                    floatArrayOf(
                        0.92f, 0f, 0f, 0f, -4f,
                        0f, 1.02f, 0.04f, 0f, 4f,
                        0f, 0.04f, 1.16f, 0f, 10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                result.postConcat(cool)
            }
            FilterPreset.DUOTONE -> {
                val gray = ColorMatrix()
                gray.setSaturation(0.15f)
                result.postConcat(gray)
                val coralTint = ColorMatrix(
                    floatArrayOf(
                        1.18f, 0.08f, 0f, 0f, 18f,
                        0f, 0.90f, 0.05f, 0f, 0f,
                        0f, 0.05f, 1.05f, 0f, 12f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                result.postConcat(coralTint)
            }
        }

        // User Saturation
        if (saturation != 1f) {
            val satMatrix = ColorMatrix()
            satMatrix.setSaturation(saturation)
            result.postConcat(satMatrix)
        }

        // User Contrast + Brightness
        val b = brightness * 100f
        val translate = (-0.5f * contrast + 0.5f) * 255f + b
        if (contrast != 1f || brightness != 0f) {
            val bcMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, translate,
                    0f, contrast, 0f, 0f, translate,
                    0f, 0f, contrast, 0f, translate,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            result.postConcat(bcMatrix)
        }

        return result
    }

    suspend fun saveBitmapLocally(context: Context, bitmap: Bitmap, prefix: String = "asset"): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.filesDir, "designs").apply { mkdirs() }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
            val scaled = scaleDownIfNeeded(bitmap, 1600)
            FileOutputStream(file).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.absolutePath
        }

    suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val decoded = BitmapFactory.decodeStream(stream)
                    decoded?.let { scaleDownIfNeeded(it, 1440) }
                }
            } catch (_: Exception) {
                null
            }
        }

    fun loadBitmapFromPath(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val largest = max(w, h)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val nw = (w * ratio).toInt().coerceAtLeast(1)
        val nh = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }

    suspend fun renderProjectToBitmap(
        context: Context,
        project: DesignProject,
        activeBitmapOverride: Bitmap? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val format = CanvasFormat.fromRatio(project.aspectRatio)
        val width = format.exportWidth
        val height = format.exportHeight
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val primaryColor = safeParseColor(project.backgroundColorHex, Color.parseColor("#14121F"))
        val secondaryColor = safeParseColor(project.secondaryColorHex, Color.parseColor("#FF5C39"))
        val bgType = BackgroundType.fromId(project.backgroundType)

        // 1. Background fill or gradient
        when (bgType) {
            BackgroundType.SOLID -> {
                canvas.drawColor(primaryColor)
            }
            BackgroundType.GRADIENT, BackgroundType.TEMPLATE_RES, BackgroundType.LOCAL_IMAGE -> {
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, 0f, width.toFloat(), height.toFloat(),
                        primaryColor, secondaryColor, Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
        }

        // 2. Background image (template drawable or local AI/picked image) with ColorMatrix filter
        val sourceBitmap: Bitmap? = when {
            activeBitmapOverride != null -> activeBitmapOverride
            bgType == BackgroundType.LOCAL_IMAGE -> loadBitmapFromPath(project.localImagePath)
            bgType == BackgroundType.TEMPLATE_RES -> {
                val resId = TemplateCatalog.drawableForKey(project.templateResKey)
                resId?.let { BitmapFactory.decodeResource(context.resources, it) }
            }
            else -> null
        }

        if (sourceBitmap != null) {
            val matrix = buildColorMatrix(
                filterPreset = FilterPreset.fromId(project.filterPreset),
                brightness = project.brightness.toFloat(),
                contrast = project.contrast.toFloat(),
                saturation = project.saturation.toFloat()
            )
            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            val srcRect = centerCropRect(sourceBitmap.width, sourceBitmap.height, width, height)
            val dstRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawBitmap(sourceBitmap, srcRect, dstRect, imgPaint)
        }

        // 3. Scrim overlay for text legibility
        val scrimAlpha = (project.overlayOpacity.coerceIn(0.0, 0.85) * 255).toInt()
        if (scrimAlpha > 0) {
            val scrimPaint = Paint().apply {
                color = Color.argb(scrimAlpha, 12, 10, 20)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }

        // 4. Layers (shapes + typography)
        val scaleFactor = width / 360f
        val layers = project.parsedLayers()
        for (layer in layers) {
            drawLayer(context, canvas, layer, width, height, scaleFactor)
        }

        output
    }

    private fun drawLayer(
        context: Context,
        canvas: Canvas,
        layer: CanvasLayer,
        width: Int,
        height: Int,
        scaleFactor: Float
    ) {
        val cx = layer.xFraction * width
        val cy = layer.yFraction * height
        val layerColor = safeParseColor(layer.colorHex, Color.WHITE)

        when (layer.type) {
            LayerType.SHAPE -> {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layerColor
                }
                when (layer.shapeKind) {
                    ShapeKind.CIRCLE_GLOW -> {
                        val radius = min(width, height) * 0.14f * layer.scale
                        paint.alpha = 180
                        canvas.drawCircle(cx, cy, radius, paint)
                    }
                    ShapeKind.PILL_BADGE -> {
                        val w = width * 0.36f * layer.scale
                        val h = 36f * scaleFactor * layer.scale
                        val rect = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
                        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
                    }
                    ShapeKind.FRAME_BORDER -> {
                        val inset = 22f * scaleFactor
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 3f * scaleFactor * layer.scale
                        paint.alpha = 210
                        val rect = RectF(inset, inset, width - inset, height - inset)
                        canvas.drawRoundRect(rect, 18f * scaleFactor, 18f * scaleFactor, paint)
                    }
                    ShapeKind.DIAMOND_SPARK -> {
                        val r = min(width, height) * 0.08f * layer.scale
                        val path = Path().apply {
                            moveTo(cx, cy - r)
                            lineTo(cx + r * 0.8f, cy)
                            lineTo(cx, cy + r)
                            lineTo(cx - r * 0.8f, cy)
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
            LayerType.TEXT -> {
                val displayText = if (layer.uppercase) layer.text.uppercase() else layer.text
                if (displayText.isBlank()) return

                val typeface = try {
                    ResourcesCompat.getFont(context, layer.font.fontRes) ?: Typeface.DEFAULT_BOLD
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }

                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = layerColor
                    textSize = layer.fontSizeSp * scaleFactor
                    this.typeface = typeface
                    letterSpacing = if (layer.uppercase) 0.06f else 0f
                }

                val maxTextWidth = (width * 0.84f).toInt().coerceAtLeast(100)
                val layout = StaticLayout.Builder
                    .obtain(displayText, 0, displayText.length, textPaint, maxTextWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.08f)
                    .setIncludePad(false)
                    .build()

                val textHeight = layout.height.toFloat()
                var maxLineW = 0f
                for (i in 0 until layout.lineCount) {
                    maxLineW = max(maxLineW, layout.getLineWidth(i))
                }

                // Optional pill badge behind text
                if (layer.badgeBgHex.isNotBlank()) {
                    val padH = 16f * scaleFactor
                    val padV = 8f * scaleFactor
                    val badgeRect = RectF(
                        cx - maxLineW / 2f - padH,
                        cy - textHeight / 2f - padV,
                        cx + maxLineW / 2f + padH,
                        cy + textHeight / 2f + padV
                    )
                    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = safeParseColor(layer.badgeBgHex, Color.parseColor("#FF5C39"))
                    }
                    canvas.drawRoundRect(badgeRect, 999f, 999f, badgePaint)
                }

                canvas.save()
                canvas.translate(cx - maxTextWidth / 2f, cy - textHeight / 2f)
                layout.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcRatio = srcW.toFloat() / srcH.toFloat()
        val dstRatio = dstW.toFloat() / dstH.toFloat()
        return if (srcRatio > dstRatio) {
            val cropW = (srcH * dstRatio).toInt()
            val left = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / dstRatio).toInt()
            val top = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
    }

    private fun safeParseColor(hex: String, fallback: Int): Int {
        return try {
            Color.parseColor(hex.trim())
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun saveExportToGallery(context: Context, bitmap: Bitmap, title: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(32).ifBlank { "PosterStudio" }
                val filename = "${safeName}_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PosterStudio")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext Result.failure(Exception("Unable to create MediaStore entry."))
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                    Result.success("Saved to Pictures/PosterStudio/$filename")
                } else {
                    val path = saveBitmapLocally(context, bitmap, safeName)
                    Result.success("Saved to $path")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun createShareIntent(context: Context, bitmap: Bitmap, title: String): Intent? =
        withContext(Dispatchers.IO) {
            try {
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportDir, "share_${System.currentTimeMillis()}.png")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (_: Exception) {
                null
            }
        }
}
