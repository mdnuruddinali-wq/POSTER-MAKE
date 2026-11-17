package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

object TemplateCatalog {

    @DrawableRes
    fun drawableForKey(key: String?): Int? = when (key) {
        "img_tpl_music_poster" -> R.drawable.img_tpl_music_poster
        "img_tpl_sale_banner" -> R.drawable.img_tpl_sale_banner
        "img_tpl_editorial" -> R.drawable.img_tpl_editorial
        "img_hero_studio" -> R.drawable.img_hero_studio
        else -> null
    }

    val templates: List<StarterTemplate> = listOf(
        StarterTemplate(
            id = "tpl_bauhaus_fest",
            title = "Midnight Sound Fest",
            subtitle = "Bold Bauhaus Event Poster",
            category = StudioCategory.POSTER,
            format = CanvasFormat.POSTER_3_4,
            backgroundType = BackgroundType.TEMPLATE_RES,
            backgroundColorHex = "#14121F",
            secondaryColorHex = "#FF5C39",
            templateResKey = "img_tpl_music_poster",
            overlayOpacity = 0.28f,
            layers = listOf(
                CanvasLayer(
                    id = "l1",
                    type = LayerType.TEXT,
                    text = "LIVE IN CONCERT • NOV 28",
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#14121F",
                    badgeBgHex = "#FFC44D",
                    fontSizeSp = 13f,
                    xFraction = 0.5f,
                    yFraction = 0.16f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "l2",
                    type = LayerType.TEXT,
                    text = "ELECTRIC\nHORIZON",
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 40f,
                    xFraction = 0.5f,
                    yFraction = 0.45f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "l3",
                    type = LayerType.TEXT,
                    text = "Rooftop Amphitheater • Doors 8PM",
                    font = PosterFont.PLAYFAIR,
                    colorHex = "#F6F4FF",
                    fontSizeSp = 17f,
                    xFraction = 0.5f,
                    yFraction = 0.80f
                )
            )
        ),
        StarterTemplate(
            id = "tpl_studio_drop",
            title = "Autumn Capsule Banner",
            subtitle = "3D Podium Product Hero",
            category = StudioCategory.BANNER,
            format = CanvasFormat.BANNER_16_9,
            backgroundType = BackgroundType.TEMPLATE_RES,
            backgroundColorHex = "#241914",
            secondaryColorHex = "#E68A5C",
            templateResKey = "img_tpl_sale_banner",
            overlayOpacity = 0.22f,
            layers = listOf(
                CanvasLayer(
                    id = "b1",
                    type = LayerType.TEXT,
                    text = "NEW ARRIVALS",
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#FFFFFF",
                    badgeBgHex = "#FF5C39",
                    fontSizeSp = 12f,
                    xFraction = 0.26f,
                    yFraction = 0.24f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "b2",
                    type = LayerType.TEXT,
                    text = "STUDIO DROP\nUP TO 40% OFF",
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 28f,
                    xFraction = 0.30f,
                    yFraction = 0.54f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "b3",
                    type = LayerType.TEXT,
                    text = "Limited Edition Collection",
                    font = PosterFont.PLAYFAIR,
                    colorHex = "#FFE8D6",
                    fontSizeSp = 15f,
                    xFraction = 0.28f,
                    yFraction = 0.80f
                )
            )
        ),
        StarterTemplate(
            id = "tpl_sunlit_atelier",
            title = "Sunlit Editorial",
            subtitle = "Minimal Lifestyle Poster",
            category = StudioCategory.POSTER,
            format = CanvasFormat.POSTER_3_4,
            backgroundType = BackgroundType.TEMPLATE_RES,
            backgroundColorHex = "#3B3228",
            secondaryColorHex = "#D9B99B",
            templateResKey = "img_tpl_editorial",
            overlayOpacity = 0.18f,
            layers = listOf(
                CanvasLayer(
                    id = "e1",
                    type = LayerType.SHAPE,
                    shapeKind = ShapeKind.FRAME_BORDER,
                    colorHex = "#FFFFFF",
                    xFraction = 0.5f,
                    yFraction = 0.5f,
                    scale = 1.0f
                ),
                CanvasLayer(
                    id = "e2",
                    type = LayerType.TEXT,
                    text = "Atelier Botanica",
                    font = PosterFont.PLAYFAIR,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 34f,
                    xFraction = 0.5f,
                    yFraction = 0.38f
                ),
                CanvasLayer(
                    id = "e3",
                    type = LayerType.TEXT,
                    text = "Handcrafted Ceramics & Slow Living",
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#FFF8F0",
                    fontSizeSp = 14f,
                    xFraction = 0.5f,
                    yFraction = 0.52f
                ),
                CanvasLayer(
                    id = "e4",
                    type = LayerType.TEXT,
                    text = "SHOP EXHIBITION",
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#1A1826",
                    badgeBgHex = "#FFFFFF",
                    fontSizeSp = 13f,
                    xFraction = 0.5f,
                    yFraction = 0.82f,
                    uppercase = true
                )
            )
        ),
        StarterTemplate(
            id = "tpl_flash_social",
            title = "Creator Podcast Square",
            subtitle = "High-Contrast Social Post",
            category = StudioCategory.SOCIAL,
            format = CanvasFormat.SQUARE_1_1,
            backgroundType = BackgroundType.GRADIENT,
            backgroundColorHex = "#181333",
            secondaryColorHex = "#FF5C39",
            overlayOpacity = 0.0f,
            layers = listOf(
                CanvasLayer(
                    id = "s1",
                    type = LayerType.SHAPE,
                    shapeKind = ShapeKind.CIRCLE_GLOW,
                    colorHex = "#FFC44D",
                    xFraction = 0.80f,
                    yFraction = 0.22f,
                    scale = 1.1f
                ),
                CanvasLayer(
                    id = "s2",
                    type = LayerType.TEXT,
                    text = "EPISODE 42 • OUT NOW",
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#111018",
                    badgeBgHex = "#FFC44D",
                    fontSizeSp = 13f,
                    xFraction = 0.5f,
                    yFraction = 0.22f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "s3",
                    type = LayerType.TEXT,
                    text = "DESIGNING\nTHE FUTURE",
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 36f,
                    xFraction = 0.5f,
                    yFraction = 0.50f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "s4",
                    type = LayerType.TEXT,
                    text = "Stream on all platforms",
                    font = PosterFont.PACIFICO,
                    colorHex = "#FFE5DF",
                    fontSizeSp = 20f,
                    xFraction = 0.5f,
                    yFraction = 0.78f
                )
            )
        ),
        StarterTemplate(
            id = "tpl_story_launch",
            title = "Reel / Story Promo",
            subtitle = "Vertical 9:16 Flash Launch",
            category = StudioCategory.STORY,
            format = CanvasFormat.STORY_9_16,
            backgroundType = BackgroundType.GRADIENT,
            backgroundColorHex = "#0F2027",
            secondaryColorHex = "#7C6BFF",
            overlayOpacity = 0.0f,
            layers = listOf(
                CanvasLayer(
                    id = "st1",
                    type = LayerType.TEXT,
                    text = "WEEKEND EXCLUSIVE",
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#FFFFFF",
                    badgeBgHex = "#FF5C39",
                    fontSizeSp = 13f,
                    xFraction = 0.5f,
                    yFraction = 0.20f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "st2",
                    type = LayerType.TEXT,
                    text = "SUMMER\nFLASH\nSALE",
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 44f,
                    xFraction = 0.5f,
                    yFraction = 0.48f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "st3",
                    type = LayerType.TEXT,
                    text = "Use code STUDIO30",
                    font = PosterFont.PACIFICO,
                    colorHex = "#FFC44D",
                    fontSizeSp = 22f,
                    xFraction = 0.5f,
                    yFraction = 0.74f
                )
            )
        ),
        StarterTemplate(
            id = "tpl_cyber_banner",
            title = "Tech Keynote Banner",
            subtitle = "Wide 16:9 Announcement",
            category = StudioCategory.BANNER,
            format = CanvasFormat.BANNER_16_9,
            backgroundType = BackgroundType.TEMPLATE_RES,
            backgroundColorHex = "#111018",
            secondaryColorHex = "#7C6BFF",
            templateResKey = "img_hero_studio",
            overlayOpacity = 0.35f,
            layers = listOf(
                CanvasLayer(
                    id = "kb1",
                    type = LayerType.TEXT,
                    text = "CREATOR SUMMIT 2026",
                    font = PosterFont.SPACE_GROTESK,
                    colorHex = "#FFFFFF",
                    fontSizeSp = 30f,
                    xFraction = 0.5f,
                    yFraction = 0.42f,
                    uppercase = true
                ),
                CanvasLayer(
                    id = "kb2",
                    type = LayerType.TEXT,
                    text = "RESERVE YOUR PASS",
                    font = PosterFont.PLUS_JAKARTA,
                    colorHex = "#111018",
                    badgeBgHex = "#FFC44D",
                    fontSizeSp = 13f,
                    xFraction = 0.5f,
                    yFraction = 0.72f,
                    uppercase = true
                )
            )
        )
    )

    val stylePresets: List<String> = listOf(
        "Editorial Minimal",
        "Bold Bauhaus",
        "Neon Cyberpunk",
        "Luxury Gold",
        "3D Product Studio",
        "Retro Risograph",
        "Sunlit Organic"
    )

    val paletteSwatches: List<Pair<String, String>> = listOf(
        "#14121F" to "#FF5C39",
        "#1D1640" to "#8A7BFF",
        "#0E2A2F" to "#FFC44D",
        "#2B1218" to "#FF7A59",
        "#F5EFE6" to "#2A2438",
        "#101820" to "#00D2B8"
    )
}
