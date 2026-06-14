package com.taskmanager.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class Task(
    val id: Long = 0,
    val title: String = "",
    val status: String = "pending",
    val priority: String = "med",
    val category: String = "other",
    val assignedTo: Int = 0,
    val createdBy: Int = 0,
    val dueDate: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val unread: Boolean = false,
    val overdue: Boolean = false,
    val thread: List<ThreadMessage> = emptyList()
)

data class ThreadMessage(
    val id: Long = 0,
    val authorId: Int = 0,
    val authorName: String = "",
    val authorRole: String = "",
    val message: String = "",
    val timestamp: String = "",
    val attachments: List<Attachment> = emptyList()
)

data class Attachment(
    val originalName: String = "",
    val filename: String = "",
    val url: String = ""
)

data class User(
    val id: Int = 0,
    val name: String = "",
    val username: String = "",
    val role: String = "employee",
    val jobTitle: String = "",
    val color: String = "#4f8ef7"
)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

object ApiClient {
    private val gson = Gson()
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private var baseUrl = ""
    private var cookieJar = PersistentCookieJar()

    private val client by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun configure(url: String) {
        baseUrl = url.trimEnd('/')
    }

    fun getBaseUrl() = baseUrl

    private fun url(path: String) = "$baseUrl/api$path"

    fun login(username: String, password: String): ApiResult<User> {
        val body = gson.toJson(mapOf("username" to username, "password" to password))
        val req = Request.Builder().url(url("/login"))
            .post(body.toRequestBody(JSON)).build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) {
                ApiResult.Success(gson.fromJson(raw, User::class.java))
            } else {
                val err = runCatching { gson.fromJson(raw, JsonObject::class.java)?.get("error")?.asString }.getOrNull()
                ApiResult.Error(err ?: "خطأ في تسجيل الدخول", res.code)
            }
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال بالخادم: ${e.message}")
        }
    }

    fun logout(): ApiResult<Unit> {
        val req = Request.Builder().url(url("/logout")).post("".toRequestBody(JSON)).build()
        return try {
            client.newCall(req).execute()
            cookieJar.clear()
            ApiResult.Success(Unit)
        } catch (e: IOException) {
            ApiResult.Error(e.message ?: "خطأ")
        }
    }

    fun me(): ApiResult<User> {
        val req = Request.Builder().url(url("/me")).get().build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) ApiResult.Success(gson.fromJson(raw, User::class.java))
            else ApiResult.Error("غير مسجل دخول", res.code)
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال: ${e.message}")
        }
    }

    fun getTasks(): ApiResult<List<Task>> {
        val req = Request.Builder().url(url("/tasks")).get().build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) {
                val type = object : TypeToken<List<Task>>() {}.type
                ApiResult.Success(gson.fromJson(raw, type))
            } else ApiResult.Error("فشل جلب الطلبات", res.code)
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال: ${e.message}")
        }
    }

    fun getTask(id: Long): ApiResult<Task> {
        val req = Request.Builder().url(url("/tasks/$id")).get().build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) ApiResult.Success(gson.fromJson(raw, Task::class.java))
            else ApiResult.Error("فشل جلب الطلب", res.code)
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال: ${e.message}")
        }
    }

    fun sendReply(taskId: Long, message: String): ApiResult<Task> {
        val body = gson.toJson(mapOf("message" to message))
        val req = Request.Builder().url(url("/tasks/$taskId/reply"))
            .post(body.toRequestBody(JSON)).build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) ApiResult.Success(gson.fromJson(raw, Task::class.java))
            else {
                val err = runCatching { gson.fromJson(raw, JsonObject::class.java)?.get("error")?.asString }.getOrNull()
                ApiResult.Error(err ?: "فشل الإرسال")
            }
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال: ${e.message}")
        }
    }

    fun changeStatus(taskId: Long, status: String): ApiResult<Task> {
        val body = gson.toJson(mapOf("status" to status))
        val req = Request.Builder().url(url("/tasks/$taskId"))
            .patch(body.toRequestBody(JSON)).build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) ApiResult.Success(gson.fromJson(raw, Task::class.java))
            else ApiResult.Error("فشل التحديث", res.code)
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال: ${e.message}")
        }
    }

    fun getDirectory(): ApiResult<List<User>> {
        val req = Request.Builder().url(url("/directory")).get().build()
        return try {
            val res = client.newCall(req).execute()
            val raw = res.body?.string() ?: ""
            if (res.isSuccessful) {
                val type = object : TypeToken<List<User>>() {}.type
                ApiResult.Success(gson.fromJson(raw, type))
            } else ApiResult.Error("فشل جلب المستخدمين", res.code)
        } catch (e: IOException) {
            ApiResult.Error("تعذر الاتصال: ${e.message}")
        }
    }
}

// Simple in-memory cookie jar (persists session within app lifecycle)
class PersistentCookieJar : CookieJar {
    private val store = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store.removeAll { c -> cookies.any { it.name == c.name } }
        store.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        store.filter { it.matches(url) }

    fun clear() = store.clear()
}
