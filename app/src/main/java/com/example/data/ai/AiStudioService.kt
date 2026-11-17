package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.CanvasFormat
import com.example.data.model.PosterCopySuggestion
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiStudioService"
private const val AI_TIMEOUT_MS = 60_000L

class AiStudioService {

    private val posterCopySchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string(description = "Punchy 2 to 5 word poster headline"),
            "subheadline" to Schema.string(description = "Supporting tagline under 10 words"),
            "ctaBadge" to Schema.string(description = "Short 2 to 3 word badge or call to action"),
            "socialCaption" to Schema.string(description = "Engaging social media post caption with 3 hashtags"),
            "primaryHex" to Schema.string(description = "Dark or bold background hex color e.g. #14121F"),
            "accentHex" to Schema.string(description = "Vibrant contrasting accent hex color e.g. #FF5C39")
        )
    )

    private val copywriterModel: GenerativeModel by lazy {
        val config = generationConfig {
            responseMimeType = "application/json"
            responseSchema = posterCopySchema
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = config
        )
    }

    private fun imageModelFor(format: CanvasFormat): GenerativeModel {
        val ratio = when (format) {
            CanvasFormat.POSTER_3_4 -> AspectRatio.PORTRAIT_3x4
            CanvasFormat.SQUARE_1_1 -> AspectRatio.SQUARE_1x1
            CanvasFormat.BANNER_16_9 -> AspectRatio.LANDSCAPE_16x9
            CanvasFormat.STORY_9_16 -> AspectRatio.PORTRAIT_9x16
            CanvasFormat.FLYER_4_5 -> AspectRatio.PORTRAIT_3x4
        }
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
            imageConfig = ImageConfig.builder()
                .setAspectRatio(ratio)
                .setImageSize(ImageSize.SIZE_1K)
                .build()
        }
        return Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    private val photoEditModel: GenerativeModel by lazy {
        val config = generationConfig {
            responseModalities = listOf(ResponseModality.IMAGE)
        }
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = config
        )
    }

    suspend fun generatePosterOrBanner(
        prompt: String,
        stylePreset: String,
        format: CanvasFormat,
        includeSpaceForText: Boolean = true
    ): Result<Bitmap> {
        return try {
            val framing = if (includeSpaceForText) {
                "Leave clean balanced negative space for poster typography overlay, avoid cluttered tiny text."
            } else {
                "Complete cohesive social media artwork composition."
            }
            val fullPrompt = "Design a high-impact ${format.label} artwork in '$stylePreset' aesthetic. " +
                "Subject: ${prompt.trim()}. $framing Crisp studio lighting, rich color harmony, production graphic design quality."

            val bitmap = withTimeout(AI_TIMEOUT_MS) {
                val model = imageModelFor(format)
                val response = model.generateContent(fullPrompt)
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
            }
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No image was returned by the model."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Image generation timed out", e)
            Result.failure(Exception("Image generation timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Quota exceeded for image generation", e)
            Result.failure(Exception("AI image quota or billing limit reached. Please check your Firebase Blaze plan or try later."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image generation failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate visual asset."))
        }
    }

    suspend fun editPhoto(
        sourceBitmap: Bitmap,
        instruction: String
    ): Result<Bitmap> {
        return try {
            val bitmap = withTimeout(AI_TIMEOUT_MS) {
                val response = photoEditModel.generateContent(
                    content {
                        image(sourceBitmap)
                        text(
                            "Edit this photo professionally while preserving key subject identity: ${instruction.trim()}. " +
                                "Output a crisp, high-resolution, social-media-ready result."
                        )
                    }
                )
                val imagePart = response.candidates.firstOrNull()
                    ?.content?.parts?.filterIsInstance<ImagePart>()
                    ?.firstOrNull()
                imagePart?.image
            }
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IllegalStateException("No edited image was returned by the model."))
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Photo edit timed out", e)
            Result.failure(Exception("AI photo editing timed out after 60 seconds. Please try again."))
        } catch (e: QuotaExceededException) {
            Log.e(TAG, "Quota exceeded for photo editing", e)
            Result.failure(Exception("AI image quota or billing limit reached. Please check your Firebase Blaze plan or try later."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo edit failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to edit photo."))
        }
    }

    suspend fun generatePosterCopy(
        topic: String,
        formatLabel: String
    ): Result<PosterCopySuggestion> {
        return try {
            val suggestion = withTimeout(AI_TIMEOUT_MS) {
                val response = copywriterModel.generateContent(
                    "You are a senior brand art director. Create punchy social media copy and a high-contrast color palette " +
                        "for a $formatLabel about: ${topic.trim()}."
                )
                val jsonText = response.text
                    ?: throw IllegalStateException("Model returned empty copywriting response.")
                val json = JSONObject(jsonText)
                PosterCopySuggestion(
                    headline = json.optString("headline", "BOLD STUDIO").trim(),
                    subheadline = json.optString("subheadline", "Crafted for modern creators").trim(),
                    ctaBadge = json.optString("ctaBadge", "EXPLORE NOW").trim(),
                    socialCaption = json.optString("socialCaption", "Fresh design drop #PosterStudio #Design #Creative").trim(),
                    primaryHex = normalizeHex(json.optString("primaryHex", "#14121F"), "#14121F"),
                    accentHex = normalizeHex(json.optString("accentHex", "#FF5C39"), "#FF5C39")
                )
            }
            Result.success(suggestion)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Copywriter timed out", e)
            Result.failure(Exception("Copy generation timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Copywriter failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to generate copy & palette."))
        }
    }

    private fun normalizeHex(raw: String, fallback: String): String {
        val clean = raw.trim()
        return if (clean.matches(Regex("^#[0-9A-Fa-f]{6}$"))) clean else fallback
    }
}
