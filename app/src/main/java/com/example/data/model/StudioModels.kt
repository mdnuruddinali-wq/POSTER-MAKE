package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class CanvasFormat(
    val id: String,
    val label: String,
    val ratioLabel: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val exportWidth: Int,
    val exportHeight: Int,
    val defaultCategory: StudioCategory
) {
    POSTER_3_4("3:4", "Poster", "3:4", 3f, 4f, 1080, 1440, StudioCategory.POSTER),
    SQUARE_1_1("1:1", "Social Post", "1:1", 1f, 1f, 1080, 1080, StudioCategory.SOCIAL),
    BANNER_16_9("16:9", "Web Banner", "16:9", 16f, 9f, 1920, 1080, StudioCategory.BANNER),
    STORY_9_16("9:16", "Story / Reel", "9:16", 9f, 16f, 1080, 1920, StudioCategory.STORY),
    FLYER_4_5("4:5", "Portrait Feed", "4:5", 4f, 5f, 1080, 1350, StudioCategory.POSTER);

    companion object {
        fun fromRatio(raw: String): CanvasFormat =
            entries.firstOrNull { it.id == raw } ?: POSTER_3_4
    }
}

enum class StudioCategory(val id: String, val label: String) {
    POSTER("POSTER", "Posters"),
    BANNER("BANNER", "Banners"),
    SOCIAL("SOCIAL", "Social"),
    STORY("STORY", "Stories"),
    PHOTO_EDIT("PHOTO_EDIT", "Photo Studio");

    companion object {
        fun fromId(raw: String): StudioCategory =
            entries.firstOrNull { it.id == raw } ?: POSTER
    }
}

enum class BackgroundType(val id: String) {
    GRADIENT("GRADIENT"),
    SOLID("SOLID"),
    TEMPLATE_RES("TEMPLATE_RES"),
    LOCAL_IMAGE("LOCAL_IMAGE");

    companion object {
        fun fromId(raw: String): BackgroundType =
            entries.firstOrNull { it.id == raw } ?: GRADIENT
    }
}

enum class FilterPreset(val id: String, val label: String) {
    ORIGINAL("ORIGINAL", "Original"),
    VIVID("VIVID", "Vivid"),
    NOIR("NOIR", "Noir B&W"),
    WARM_FILM("WARM_FILM", "Warm Film"),
    COOL_STUDIO("COOL_STUDIO", "Cool Studio"),
    DUOTONE("DUOTONE", "Coral Duo");

    companion object {
        fun fromId(raw: String): FilterPreset =
            entries.firstOrNull { it.id == raw } ?: ORIGINAL
    }
}

enum class PosterFont(val id: String, val label: String, @DrawableRes val fontRes: Int) {
    SPACE_GROTESK("SPACE_GROTESK", "Space Bold", R.font.space_grotesk),
    PLUS_JAKARTA("PLUS_JAKARTA", "Jakarta Sans", R.font.plus_jakarta_sans),
    PLAYFAIR("PLAYFAIR", "Playfair Serif", R.font.playfair_display),
    PACIFICO("PACIFICO", "Pacifico Script", R.font.pacifico);

    companion object {
        fun fromId(raw: String): PosterFont =
            entries.firstOrNull { it.id == raw } ?: SPACE_GROTESK
    }
}

enum class ShapeKind(val id: String, val label: String) {
    PILL_BADGE("PILL_BADGE", "Pill Badge"),
    CIRCLE_GLOW("CIRCLE_GLOW", "Circle Accent"),
    FRAME_BORDER("FRAME_BORDER", "Frame Border"),
    DIAMOND_SPARK("DIAMOND_SPARK", "Star Accent");

    companion object {
        fun fromId(raw: String): ShapeKind =
            entries.firstOrNull { it.id == raw } ?: PILL_BADGE
    }
}

enum class LayerType(val id: String) {
    TEXT("TEXT"),
    SHAPE("SHAPE");

    companion object {
        fun fromId(raw: String): LayerType =
            entries.firstOrNull { it.id == raw } ?: TEXT
    }
}

data class CanvasLayer(
    val id: String = UUID.randomUUID().toString().take(8),
    val type: LayerType = LayerType.TEXT,
    val text: String = "NEW HEADLINE",
    val font: PosterFont = PosterFont.SPACE_GROTESK,
    val colorHex: String = "#FFFFFF",
    val badgeBgHex: String = "",
    val fontSizeSp: Float = 30f,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val uppercase: Boolean = false,
    val shapeKind: ShapeKind = ShapeKind.PILL_BADGE,
    val scale: Float = 1.0f
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "type" to type.id,
        "text" to text.take(180),
        "fontId" to font.id,
        "colorHex" to colorHex,
        "badgeBgHex" to badgeBgHex,
        "fontSizeSp" to fontSizeSp.toDouble(),
        "xFraction" to xFraction.coerceIn(0.05f, 0.95f).toDouble(),
        "yFraction" to yFraction.coerceIn(0.05f, 0.95f).toDouble(),
        "uppercase" to uppercase,
        "shapeKind" to shapeKind.id,
        "scale" to scale.coerceIn(0.4f, 2.5f).toDouble()
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): CanvasLayer {
            return CanvasLayer(
                id = (map["id"] as? String)?.ifBlank { null } ?: UUID.randomUUID().toString().take(8),
                type = LayerType.fromId(map["type"] as? String ?: "TEXT"),
                text = map["text"] as? String ?: "",
                font = PosterFont.fromId(map["fontId"] as? String ?: "SPACE_GROTESK"),
                colorHex = map["colorHex"] as? String ?: "#FFFFFF",
                badgeBgHex = map["badgeBgHex"] as? String ?: "",
                fontSizeSp = (map["fontSizeSp"] as? Number)?.toFloat() ?: 28f,
                xFraction = ((map["xFraction"] as? Number)?.toFloat() ?: 0.5f).coerceIn(0.05f, 0.95f),
                yFraction = ((map["yFraction"] as? Number)?.toFloat() ?: 0.5f).coerceIn(0.05f, 0.95f),
                uppercase = map["uppercase"] as? Boolean ?: false,
                shapeKind = ShapeKind.fromId(map["shapeKind"] as? String ?: "PILL_BADGE"),
                scale = ((map["scale"] as? Number)?.toFloat() ?: 1.0f).coerceIn(0.4f, 2.5f)
            )
        }
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Creator",
    val email: String? = null,
    val photoUrl: String? = null,
    val preferredFormat: String = "POSTER_3_4",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DesignProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Untitled Design",
    val category: String = "POSTER",
    val aspectRatio: String = "3:4",
    val backgroundType: String = "GRADIENT",
    val backgroundColorHex: String = "#14121F",
    val secondaryColorHex: String = "#FF5C39",
    val templateResKey: String? = null,
    val localImagePath: String? = null,
    val filterPreset: String = "ORIGINAL",
    val brightness: Double = 0.0,
    val contrast: Double = 1.0,
    val saturation: Double = 1.0,
    val overlayOpacity: Double = 0.15,
    val layers: List<Map<String, Any>> = emptyList(),
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun parsedLayers(): List<CanvasLayer> = layers.map { CanvasLayer.fromMap(it) }

    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to ownerUid,
            "title" to title.trim().ifBlank { "Untitled Design" }.take(100),
            "category" to StudioCategory.fromId(category).id,
            "aspectRatio" to CanvasFormat.fromRatio(aspectRatio).id,
            "backgroundType" to BackgroundType.fromId(backgroundType).id,
            "backgroundColorHex" to backgroundColorHex.take(12),
            "secondaryColorHex" to secondaryColorHex.take(12),
            "templateResKey" to templateResKey?.take(64)?.ifBlank { null },
            "localImagePath" to localImagePath?.take(300)?.ifBlank { null },
            "filterPreset" to FilterPreset.fromId(filterPreset).id,
            "brightness" to brightness.coerceIn(-1.0, 1.0),
            "contrast" to contrast.coerceIn(0.5, 2.0),
            "saturation" to saturation.coerceIn(0.0, 2.0),
            "overlayOpacity" to overlayOpacity.coerceIn(0.0, 0.85),
            "layers" to layers.take(25),
            "favorite" to favorite,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "title" to title.trim().ifBlank { "Untitled Design" }.take(100),
            "category" to StudioCategory.fromId(category).id,
            "aspectRatio" to CanvasFormat.fromRatio(aspectRatio).id,
            "backgroundType" to BackgroundType.fromId(backgroundType).id,
            "backgroundColorHex" to backgroundColorHex.take(12),
            "secondaryColorHex" to secondaryColorHex.take(12),
            "templateResKey" to templateResKey?.take(64)?.ifBlank { null },
            "localImagePath" to localImagePath?.take(300)?.ifBlank { null },
            "filterPreset" to FilterPreset.fromId(filterPreset).id,
            "brightness" to brightness.coerceIn(-1.0, 1.0),
            "contrast" to contrast.coerceIn(0.5, 2.0),
            "saturation" to saturation.coerceIn(0.0, 2.0),
            "overlayOpacity" to overlayOpacity.coerceIn(0.0, 0.85),
            "layers" to layers.take(25),
            "favorite" to favorite,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class AiGenerationRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "POSTER_BANNER",
    val prompt: String = "",
    val stylePreset: String = "Editorial Minimal",
    val aspectRatio: String = "3:4",
    val resultSummary: String? = null,
    val localImagePath: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to ownerUid,
            "mode" to mode,
            "prompt" to prompt.trim().take(1000).ifBlank { "Creative prompt" },
            "stylePreset" to stylePreset.take(60).ifBlank { "Studio" },
            "aspectRatio" to aspectRatio.take(16).ifBlank { "3:4" },
            "resultSummary" to resultSummary?.take(2000)?.ifBlank { null },
            "localImagePath" to localImagePath?.take(300)?.ifBlank { null },
            "createdAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

data class StarterTemplate(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: StudioCategory,
    val format: CanvasFormat,
    val backgroundType: BackgroundType,
    val backgroundColorHex: String,
    val secondaryColorHex: String,
    val templateResKey: String? = null,
    val overlayOpacity: Float = 0.25f,
    val layers: List<CanvasLayer>
)

data class PosterCopySuggestion(
    val headline: String,
    val subheadline: String,
    val ctaBadge: String,
    val socialCaption: String,
    val primaryHex: String,
    val accentHex: String
)

fun parseHexColor(hex: String, fallback: Color = Color(0xFF14121F)): Color {
    return try {
        val clean = hex.trim().removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> android.graphics.Color.parseColor("#FF$clean")
            8 -> android.graphics.Color.parseColor("#$clean")
            else -> return fallback
        }
        Color(colorInt)
    } catch (_: Exception) {
        fallback
    }
}
