package com.taskmanager.app

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.*

class PollService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var pollJob: Job? = null

    // Track per-task last-seen reply count to detect new replies
    private val knownReplyCounts = mutableMapOf<Long, Int>()

    companion object {
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(1, NotificationHelper.buildForegroundNotification(this))
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startPolling()
        return START_STICKY // restart if killed by system
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive) {
                poll()
                val intervalMs = Prefs.getPollInterval(applicationContext).toLong() * 60_000L
                delay(intervalMs)
            }
        }
    }

    private suspend fun poll() {
        if (!Prefs.isLoggedIn(applicationContext)) return

        val result = ApiClient.getTasks()
        if (result is ApiResult.Error && result.code == 401) {
            // Session expired — stop service
            stopSelf()
            return
        }
        if (result !is ApiResult.Success) return

        val tasks = result.data
        val currentIds = tasks.map { it.id }.toSet()
        val knownIds = Prefs.getKnownTaskIds(applicationContext)

        // ── New tasks (IDs not seen before) ──────────────────────────
        if (knownIds.isNotEmpty()) { // skip first run
            val newTasks = tasks.filter { it.id !in knownIds }
            val myId = Prefs.getUserId(applicationContext)
            newTasks.forEach { task ->
                // Notify if assigned to me OR I'm admin
                if (task.assignedTo == myId || Prefs.getUserRole(applicationContext) == "admin") {
                    NotificationHelper.notifyNewTask(applicationContext, task)
                }
            }
        }

        // ── New replies on existing tasks ─────────────────────────────
        val myId = Prefs.getUserId(applicationContext)
        tasks.forEach { task ->
            val prevCount = knownReplyCounts[task.id]
            val currCount = task.thread.size
            if (prevCount != null && currCount > prevCount) {
                // There are new messages — notify if I'm involved and I didn't send it
                val isMyTask = task.assignedTo == myId || Prefs.getUserRole(applicationContext) == "admin"
                if (isMyTask) {
                    val lastMsg = task.thread.lastOrNull()
                    if (lastMsg != null && lastMsg.authorId != myId) {
                        NotificationHelper.notifyNewReply(applicationContext, task, lastMsg.authorName)
                    }
                }
            }
            knownReplyCounts[task.id] = currCount
        }

        Prefs.saveKnownTaskIds(applicationContext, currentIds)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
