package com.taskmanager.app

import android.content.Context

object Prefs {
    private const val FILE = "taskmanager_prefs"
    private const val KEY_SERVER = "server_url"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ROLE = "user_role"
    private const val KEY_USER_JOB = "user_job"
    private const val KEY_USER_COLOR = "user_color"
    private const val KEY_LAST_TASK_IDS = "last_task_ids"
    private const val KEY_POLL_INTERVAL = "poll_interval"

    fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun setServer(ctx: Context, url: String) = sp(ctx).edit().putString(KEY_SERVER, url).apply()
    fun getServer(ctx: Context): String = sp(ctx).getString(KEY_SERVER, "") ?: ""

    fun setUser(ctx: Context, user: User) {
        sp(ctx).edit()
            .putInt(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.name)
            .putString(KEY_USER_ROLE, user.role)
            .putString(KEY_USER_JOB, user.jobTitle)
            .putString(KEY_USER_COLOR, user.color)
            .apply()
    }

    fun getUserId(ctx: Context) = sp(ctx).getInt(KEY_USER_ID, 0)
    fun getUserName(ctx: Context) = sp(ctx).getString(KEY_USER_NAME, "") ?: ""
    fun getUserRole(ctx: Context) = sp(ctx).getString(KEY_USER_ROLE, "") ?: ""
    fun getUserJob(ctx: Context) = sp(ctx).getString(KEY_USER_JOB, "") ?: ""
    fun getUserColor(ctx: Context) = sp(ctx).getString(KEY_USER_COLOR, "#4f8ef7") ?: "#4f8ef7"

    fun clearUser(ctx: Context) {
        sp(ctx).edit()
            .remove(KEY_USER_ID).remove(KEY_USER_NAME)
            .remove(KEY_USER_ROLE).remove(KEY_USER_JOB).apply()
    }

    fun isLoggedIn(ctx: Context) = getUserId(ctx) != 0

    // Store the set of known task IDs to detect new ones during polling
    fun saveKnownTaskIds(ctx: Context, ids: Set<Long>) {
        sp(ctx).edit().putString(KEY_LAST_TASK_IDS, ids.joinToString(",")).apply()
    }
    fun getKnownTaskIds(ctx: Context): Set<Long> {
        val raw = sp(ctx).getString(KEY_LAST_TASK_IDS, "") ?: ""
        return if (raw.isEmpty()) emptySet()
        else raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun setPollInterval(ctx: Context, minutes: Int) = sp(ctx).edit().putInt(KEY_POLL_INTERVAL, minutes).apply()
    fun getPollInterval(ctx: Context) = sp(ctx).getInt(KEY_POLL_INTERVAL, 1) // default: every 1 min
}
