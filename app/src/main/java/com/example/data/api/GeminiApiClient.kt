package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    suspend fun analyzeJournalEntry(content: String): JournalAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e("GeminiApiClient", "API key is missing or is placeholder")
            return@withContext JournalAnalysisResult(
                mood = "Calm",
                sentimentScore = 4.0f,
                summary = "Journal processed. Please set up a real GEMINI_API_KEY in the AI Studio Secrets panel to enable authentic Gemini sentiments, summaries, and action tasks.",
                tasks = listOf("Take a deep breath and stretches", "Drink a glass of water", "Plan your major focus goal")
            )
        }

        val promptText = """
            You are a helpful mindfulness coach and productivity planner. Analyze the following daily journal entry. 
            Provide:
            1. A primary mood category of the writer (must be exactly one of: Balanced, Happy, Calm, Inspired, Sad, Anxious, or Tired).
            2. A corresponding numeric sentiment score from 1.0 (very low/anxious/sad) to 5.0 (highly positive/happy).
            3. A short, beautiful 1-2 sentence mindful summary/reflection offering support or synthesis.
            4. Up to 4 actionable, short, concrete sub-tasks (goals) extracted from or inspired by their writing (e.g. "Drink water", "Take a 5-minute walk", "Call Mom", "Finish reading chapter 3"). Keep tasks short, clear, and actionable.

            Provide the response strictly as a JSON object with this exact schema:
            {
              "mood": "Calm",
              "sentimentScore": 4.0,
              "summary": "Your reflection here...",
              "tasks": ["Task A", "Task B"]
            }

            Do NOT add markdown flags, backticks, or any conversational text around the JSON. Direct JSON output only.
            
            Journal Entry:
            "$content"
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.7)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonRequest.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiApiClient", "Response error code ${response.code}: $errBody")
                    throw Exception("API Error: ${response.code}")
                }
                val responseString = response.body?.string() ?: throw Exception("Empty response body")
                Log.d("GeminiApiClient", "Raw API Response: $responseString")

                val rootObj = JSONObject(responseString)
                val candidatesJson = rootObj.getJSONArray("candidates")
                val partsJson = candidatesJson.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                val textResponse = partsJson.getJSONObject(0).getString("text").trim()

                val cleanJson = if (textResponse.startsWith("```json")) {
                    textResponse.substringAfter("```json").substringBeforeLast("```").trim()
                } else if (textResponse.startsWith("```")) {
                    textResponse.substringAfter("```").substringBeforeLast("```").trim()
                } else {
                    textResponse
                }

                val resultObj = JSONObject(cleanJson)
                val mood = resultObj.optString("mood", "Neutral")
                val score = resultObj.optDouble("sentimentScore", 3.0).toFloat()
                val summary = resultObj.optString("summary", "Processed successfully.")
                
                val tasksJson = resultObj.optJSONArray("tasks")
                val tasksList = mutableListOf<String>()
                if (tasksJson != null) {
                    for (i in 0 until tasksJson.length()) {
                        tasksList.add(tasksJson.getString(i))
                    }
                }

                JournalAnalysisResult(mood, score, summary, tasksList)
            }
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "API failure", e)
            JournalAnalysisResult(
                mood = "Neutral",
                sentimentScore = 3.0f,
                summary = "Processed offline. Error calling Gemini API: ${e.localizedMessage}",
                tasks = listOf("Complete a small mindful step today", "Keep writing your thoughts")
            )
        }
    }

    suspend fun generateWeeklyInsights(entriesList: List<String>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your GEMINI_API_KEY in the AI Studio Secrets panel of the editor to unlock custom AI wellness summaries, weekly emotional tracking reviews, and alignment goals."
        }
        if (entriesList.isEmpty()) {
            return@withContext "You don't have any daily journal logs recorded yet. Create a logs record in the Journal tab so Gemini AI can track mental health patterns and supply personalized suggestions."
        }

        val entriesText = entriesList.joinToString("\n---\n")
        val promptText = """
            You are a compassionate, skilled mindfulness coach and spiritual guide. Analyze this list of a person's recent journal logs to provide a brief (100-150 words), supportive, and clear mental health summary. Point out any notable strengths, subtle emotional patterns, and give 2 practical, easy recommendations for their week ahead. Address the writer directly with gentle, encouraging tones.

            Entries:
            $entriesText
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonRequest.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL?key=$apiKey")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext "API error: ${response.code}"
                val responseString = response.body?.string() ?: return@withContext "Empty response"
                val rootObj = JSONObject(responseString)
                val textResponse = rootObj.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text").trim()
                textResponse
            }
        } catch (e: Exception) {
            "Error analyzing journal trends: ${e.localizedMessage}"
        }
    }
}

data class JournalAnalysisResult(
    val mood: String,
    val sentimentScore: Float,
    val summary: String,
    val tasks: List<String>
)
