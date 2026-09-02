package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "tools") val tools: List<Map<String, Any>>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "role") val role: String? = null,
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = 0.7f,
    @Json(name = "topP") val topP: Float? = 0.95f,
    @Json(name = "topK") val topK: Int? = 40,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = 2048
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @Json(name = "promptFeedback") val promptFeedback: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null,
    @Json(name = "finishReason") val finishReason: String? = null,
    @Json(name = "groundingMetadata") val groundingMetadata: GroundingMetadata? = null
)

@JsonClass(generateAdapter = true)
data class GroundingMetadata(
    @Json(name = "webSearchQueries") val webSearchQueries: List<String>? = null,
    @Json(name = "searchEntryPoint") val searchEntryPoint: SearchEntryPoint? = null,
    @Json(name = "groundingChunks") val groundingChunks: List<GroundingChunk>? = null,
    @Json(name = "groundingSupports") val groundingSupports: List<GroundingSupport>? = null
)

@JsonClass(generateAdapter = true)
data class SearchEntryPoint(
    @Json(name = "renderedContent") val renderedContent: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingChunk(
    @Json(name = "web") val web: GroundingWebSource? = null
)

@JsonClass(generateAdapter = true)
data class GroundingWebSource(
    @Json(name = "uri") val uri: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class GroundingSupport(
    @Json(name = "groundingChunkIndices") val groundingChunkIndices: List<Int>? = null,
    @Json(name = "confidenceScores") val confidenceScores: List<Float>? = null
)

@JsonClass(generateAdapter = true)
data class GroundingSource(
    @Json(name = "title") val title: String,
    @Json(name = "url") val url: String
)
