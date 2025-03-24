package com.example.foodresq.adaptersEtc

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.app.Activity
import android.content.Context
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PriceRecommender(private val context: Context, private val activity: Activity) {

    private val firestore = FirebaseFirestore.getInstance()


    fun recommendPrice(newProductName: String, onResult: (Int?) -> Unit) {
        firestore.collection("all_products_in_cart")
            .get()
            .addOnSuccessListener { result ->
                val promptBuilder = StringBuilder()
                promptBuilder.append("Вот список товаров, которые покупают пользователи:\n")

                for (doc in result) {
                    val name = doc.getString("name") ?: continue
                    val price = doc.getLong("price") ?: continue
                    val quantity = doc.getLong("quantity") ?: 0L
                    promptBuilder.append("- $name — $price₸ — добавлено $quantity раз\n")
                }

                promptBuilder.append("\nУчитывая информацию выше, какую цену в тенге ты порекомендуешь для нового товара. \"$newProductName\"")
                promptBuilder.append("Ответь только числом, без комментариев, валют и лишнего текста.")

                sendToGPT(promptBuilder.toString()) { gptResult ->
                    val numberRegex = Regex("\\d+")
                    val matched = numberRegex.find(gptResult ?: "")
                    val price = matched?.value?.toIntOrNull()
                    onResult(price)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("PriceRecommender", "Ошибка Firestore: ${exception.message}")
                onResult(null)
            }
    }

    private fun sendToGPT(prompt: String, onResponse: (String?) -> Unit) {
        val url = "https://api.openai.com/v1/chat/completions"
        Firebase.firestore.collection("apis").document("apikeygpt").get().addOnSuccessListener {
            val apiKey = it.getString("name").toString()
            val client = OkHttpClient()

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "Ты помощник ресторана, помогающий рекомендовать цену на товар. Отвечай только числом. Учитывай только представленные данные и обращай внимание на одинаковые названия блюд. Смещай цену товара в зависимости от количества. Используй для расчетов формулу расчета рекомендуемой цены, чтобы гарантировать постоянный ответ")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }

            val json = JSONObject().apply {
                put("model", "gpt-4o")
                put("messages", messages)
                put("temperature", 0)
            }

            val body = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer $apiKey")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("GPT", "Ошибка запроса: ${e.message}")
                    onResponse(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e("GPT", "Неверный ответ: ${response.code}")
                        onResponse(null)
                        return
                    }

                    val responseBody = response.body?.string()
                    try {
                        val json = JSONObject(responseBody)
                        val content = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")

                        activity.runOnUiThread {
                            onResponse(content.trim())
                        }
                    } catch (e: Exception) {
                        Log.e("GPT", "Ошибка парсинга: ${e.message}")
                        activity.runOnUiThread {
                            onResponse(null)
                        }
                    }
                }
            })
        }
    }
}
