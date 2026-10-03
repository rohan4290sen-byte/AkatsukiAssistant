package com.akatsuki.assistant.brain

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class AkatsukiCommand(
    val action: String,      // CALL, SMS, WHATSAPP, TORCH, OPEN_APP, ALARM, BATTERY, CHAT
    val target: String = "", // Contact name, phone number, app name, or toggle state
    val data: String = "",   // Message body, time, or extra data
    val speech: String       // Natural Indian voice spoken reply in Hinglish/Hindi/English
)

class GeminiBrain(private var apiKey: String) {

    private val tag = "GeminiBrain"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun updateApiKey(newKey: String) {
        this.apiKey = newKey
    }

    suspend fun processUserCommand(userInput: String): AkatsukiCommand = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AkatsukiCommand(
                action = "CHAT",
                speech = "Sir, kripya settings me jakar apni Google Gemini API key enter karein."
            )
        }

        val systemInstruction = """
            You are 'Akatsuki', an intelligent, obedient, and sharp personal AI assistant running on a OnePlus Nord CE 3 Lite (Android 14) phone.
            The user communicates in natural Hinglish, Hindi, or English.
            
            YOUR JOB:
            Analyze the user's command and decide if it is a phone action or a conversation.
            You MUST respond ONLY with a raw JSON object (NO markdown fences, NO extra words):
            {
               "action": "ACTION_TYPE",
               "target": "target info",
               "data": "extra details",
               "speech": "Your response to speak out loud in natural, crisp, polite Indian Hinglish (saaf aur normal Hindi/Hinglish bolna hai)"
            }
            
            SUPPORTED ACTIONS:
            - "CALL": Make phone call. Target is contact name or phone number. (e.g. "Rahul ko call lagao" -> action: "CALL", target: "Rahul", speech: "Rahul ko call mila raha hoon, Sir.")
            - "SMS": Send SMS. Target is phone number or name, data is message.
            - "WHATSAPP": Send WhatsApp message or open chat. Target is contact name, data is message. (e.g. "Rahul ko WhatsApp par hello bolo" -> action: "WHATSAPP", target: "Rahul", data: "hello", speech: "Rahul ka WhatsApp open kar raha hoon.")
            - "TORCH": Flashlight on/off. Target: "ON" or "OFF". (e.g. "Torch on karo" -> action: "TORCH", target: "ON", speech: "Torch on kar di hai, Sir.")
            - "OPEN_APP": Open any app like YouTube, Spotify, WhatsApp, Camera, Calculator, Gallery, Instagram. Target: app name. (e.g. "YouTube chalao" -> action: "OPEN_APP", target: "YouTube", speech: "YouTube khol raha hoon.")
            - "ALARM": Set alarm. Target is time (e.g. "06:00"), data is label.
            - "BATTERY": Check phone battery. Target: "CHECK". (speech: "Battery check kar raha hoon.")
            - "CHAT": Normal conversation, general knowledge, questions, advice. Target: "", data: "", speech: "Concise, polite, crisp answer in natural Indian Hinglish."
        """.trimIndent()

        val jsonBody = JsonObject().apply {
            val contents = com.google.gson.JsonArray().apply {
                val userContent = JsonObject().apply {
                    addProperty("role", "user")
                    val parts = com.google.gson.JsonArray().apply {
                        val part = JsonObject().apply {
                            addProperty("text", "$systemInstruction\n\nUser said: \"$userInput\"")
                        }
                        add(part)
                    }
                    add("parts", parts)
                }
                add(userContent)
            }
            add("contents", contents)

            val genConfig = JsonObject().apply {
                addProperty("temperature", 0.2)
                addProperty("response_mime_type", "application/json")
            }
            add("generationConfig", genConfig)
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey"
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(tag, "Gemini API error: ${response.code} - $responseBody")
                return@withContext AkatsukiCommand(
                    action = "CHAT",
                    speech = "Sir, Gemini API se connect hone me samasya aayi hai. Kripya API key ya internet check karein."
                )
            }

            // Extract the generated text from response
            val parsedResponse = gson.fromJson(responseBody, JsonObject::class.java)
            val candidates = parsedResponse.getAsJsonArray("candidates")
            if (candidates != null && candidates.size() > 0) {
                val candidate = candidates[0].asJsonObject
                val content = candidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                val text = parts[0].asJsonObject.get("text").asString.trim()

                // Clean potential markdown backticks
                val cleanJson = text.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val cmdObj = gson.fromJson(cleanJson, JsonObject::class.java)

                return@withContext AkatsukiCommand(
                    action = cmdObj.get("action")?.asString ?: "CHAT",
                    target = cmdObj.get("target")?.asString ?: "",
                    data = cmdObj.get("data")?.asString ?: "",
                    speech = cmdObj.get("speech")?.asString ?: "Ji Sir, samajh gaya."
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error processing Gemini command: ${e.message}", e)
        }

        return@withContext AkatsukiCommand(
            action = "CHAT",
            speech = "Maaf kijiye Sir, main aapki baat poori tarah samajh nahi paaya."
        )
    }
}
