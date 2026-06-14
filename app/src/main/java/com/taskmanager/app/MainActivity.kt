package com.taskmanager.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvTasks: RecyclerView
    private lateinit var layoutLogin: View
    private lateinit var layoutMain: View
    private lateinit var tvUserName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var tvUnread: TextView
    private lateinit var spinnerFilter: Spinner

    private var tasks = listOf<Task>()
    private var directory = listOf<User>()
    private var currentFilter = "all"
    private var adapter: TaskAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.createChannels(this)

        // Check first-run setup
        if (Prefs.getServer(this).isEmpty()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        ApiClient.configure(Prefs.getServer(this))

        bindViews()
        setupFilterSpinner()
        requestNotificationPermission()

        if (Prefs.isLoggedIn(this)) {
            showMain()
            loadData()
        } else {
            showLogin()
        }

        // Handle notification tap → open specific task
        val openTaskId = intent.getLongExtra("open_task_id", -1)
        if (openTaskId != -1L) {
            lifecycleScope.launch { openTaskDetail(openTaskId) }
        }
    }

    private fun bindViews() {
        layoutLogin = findViewById(R.id.layoutLogin)
        layoutMain  = findViewById(R.id.layoutMain)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvTasks     = findViewById(R.id.rvTasks)
        tvUserName  = findViewById(R.id.tvUserName)
        tvUserRole  = findViewById(R.id.tvUserRole)
        tvEmpty     = findViewById(R.id.tvEmpty)
        tvUnread    = findViewById(R.id.tvUnread)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        // Login
        val btnLogin  = findViewById<Button>(R.id.btnLogin)
        val etUser    = findViewById<EditText>(R.id.etUsername)
        val etPass    = findViewById<EditText>(R.id.etPassword)
        val tvError   = findViewById<TextView>(R.id.tvLoginError)
        val loginProg = findViewById<ProgressBar>(R.id.loginProgress)

        btnLogin.setOnClickListener {
            val u = etUser.text.toString().trim()
            val p = etPass.text.toString()
            if (u.isEmpty() || p.isEmpty()) {
                tvError.text = "يرجى إدخال اسم المستخدم وكلمة المرور"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            tvError.visibility = View.GONE
            loginProg.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { ApiClient.login(u, p) }
                loginProg.visibility = View.GONE
                btnLogin.isEnabled = true
                when (result) {
                    is ApiResult.Success -> {
                        Prefs.setUser(this@MainActivity, result.data)
                        showMain()
                        loadData()
                        startPollService()
                    }
                    is ApiResult.Error -> {
                        tvError.text = result.message
                        tvError.visibility = View.VISIBLE
                    }
                }
            }
        }

        // Change server button
        findViewById<Button>(R.id.btnChangeServer).setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        // Logout
        findViewById<ImageButton>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("تسجيل الخروج")
                .setMessage("هل تريد تسجيل الخروج؟")
                .setPositiveButton("خروج") { _, _ ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) { ApiClient.logout() }
                        Prefs.clearUser(this@MainActivity)
                        stopService(Intent(this@MainActivity, PollService::class.java))
                        showLogin()
                    }
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }

        // Refresh
        swipeRefresh.setColorSchemeColors(Color.parseColor("#4f8ef7"))
        swipeRefresh.setOnRefreshListener { loadData() }

        // RecyclerView
        rvTasks.layoutManager = LinearLayoutManager(this)
        adapter = TaskAdapter(emptyList(), directory) { task ->
            lifecycleScope.launch { openTaskDetail(task.id) }
        }
        rvTasks.adapter = adapter
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("الكل", "معلقة", "جارية", "منجزة", "متأخرة")
        val filterKeys = arrayOf("all", "pending", "progress", "done", "overdue")
        spinnerFilter.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, filters)
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentFilter = filterKeys[pos]
                applyFilter()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            val tasksResult = withContext(Dispatchers.IO) { ApiClient.getTasks() }
            val dirResult   = withContext(Dispatchers.IO) { ApiClient.getDirectory() }

            swipeRefresh.isRefreshing = false

            if (tasksResult is ApiResult.Error && tasksResult.code == 401) {
                Prefs.clearUser(this@MainActivity)
                showLogin()
                return@launch
            }

            if (tasksResult is ApiResult.Success) tasks = tasksResult.data
            if (dirResult   is ApiResult.Success) directory = dirResult.data

            adapter?.updateDirectory(directory)
            applyFilter()
            updateUnreadBadge()
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "pending"  -> tasks.filter { it.status == "pending" }
            "progress" -> tasks.filter { it.status == "progress" }
            "done"     -> tasks.filter { it.status == "done" }
            "overdue"  -> tasks.filter { it.overdue }
            else       -> tasks
        }
        adapter?.updateTasks(filtered)
        tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateUnreadBadge() {
        val count = tasks.count { it.unread }
        tvUnread.text = if (count > 0) "$count غير مقروء" else ""
        tvUnread.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    private suspend fun openTaskDetail(taskId: Long) {
        val result = withContext(Dispatchers.IO) { ApiClient.getTask(taskId) }
        if (result is ApiResult.Success) {
            showTaskDialog(result.data)
            // Refresh to clear unread
            val idx = tasks.indexOfFirst { it.id == taskId }
            if (idx >= 0) {
                tasks = tasks.toMutableList().also { it[idx] = it[idx].copy(unread = false) }
                updateUnreadBadge()
            }
        }
    }

    private fun showTaskDialog(task: Task) {
        val dialog = AlertDialog.Builder(this, R.style.TaskDialog)
        val view = layoutInflater.inflate(R.layout.dialog_task, null)
        dialog.setView(view)

        val tvTitle    = view.findViewById<TextView>(R.id.tvTaskTitle)
        val tvStatus   = view.findViewById<TextView>(R.id.tvTaskStatus)
        val tvPriority = view.findViewById<TextView>(R.id.tvTaskPriority)
        val tvAssigned = view.findViewById<TextView>(R.id.tvTaskAssigned)
        val rvThread   = view.findViewById<RecyclerView>(R.id.rvThread)
        val etReply    = view.findViewById<EditText>(R.id.etReply)
        val btnSend    = view.findViewById<Button>(R.id.btnSendReply)
        val btnPending = view.findViewById<Button>(R.id.btnSetPending)
        val btnProgress= view.findViewById<Button>(R.id.btnSetProgress)
        val btnDone    = view.findViewById<Button>(R.id.btnSetDone)

        val person = directory.find { it.id == task.assignedTo }
        tvTitle.text    = task.title
        tvStatus.text   = statusLabel(task.status)
        tvPriority.text = priorityLabel(task.priority)
        tvAssigned.text = "👤 ${person?.name ?: "—"}"

        rvThread.layoutManager = LinearLayoutManager(this)
        val threadAdapter = ThreadAdapter(task.thread.toMutableList(), Prefs.getUserId(this))
        rvThread.adapter = threadAdapter
        rvThread.scrollToPosition(task.thread.size - 1)

        val myId = Prefs.getUserId(this)
        val isAdmin = Prefs.getUserRole(this) == "admin"
        val isOwner = task.assignedTo == myId
        val canChange = isAdmin || isOwner

        btnPending.visibility  = if (canChange) View.VISIBLE else View.GONE
        btnProgress.visibility = if (canChange) View.VISIBLE else View.GONE
        btnDone.visibility     = if (canChange) View.VISIBLE else View.GONE

        val dlg = dialog.create()

        btnSend.setOnClickListener {
            val msg = etReply.text.toString().trim()
            if (msg.isEmpty()) { Toast.makeText(this, "اكتب رسالة", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            btnSend.isEnabled = false
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { ApiClient.sendReply(task.id, msg) }
                btnSend.isEnabled = true
                if (result is ApiResult.Success) {
                    etReply.setText("")
                    threadAdapter.addMessages(result.data.thread)
                    rvThread.scrollToPosition(result.data.thread.size - 1)
                    loadData()
                } else {
                    Toast.makeText(this@MainActivity, "فشل الإرسال", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fun changeStatus(status: String) {
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) { ApiClient.changeStatus(task.id, status) }
                if (result is ApiResult.Success) {
                    tvStatus.text = statusLabel(status)
                    loadData()
                    Toast.makeText(this@MainActivity, "تم تحديث الحالة", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnPending.setOnClickListener  { changeStatus("pending") }
        btnProgress.setOnClickListener { changeStatus("progress") }
        btnDone.setOnClickListener     { changeStatus("done") }

        dlg.show()
    }

    private fun showLogin() {
        layoutLogin.visibility = View.VISIBLE
        layoutMain.visibility  = View.GONE
    }

    private fun showMain() {
        layoutLogin.visibility = View.GONE
        layoutMain.visibility  = View.VISIBLE
        tvUserName.text = Prefs.getUserName(this)
        tvUserRole.text = if (Prefs.getUserRole(this) == "admin") "مدير النظام" else Prefs.getUserJob(this)
        startPollService()
    }

    private fun startPollService() {
        if (!PollService.isRunning) {
            val svc = Intent(this, PollService::class.java)
            ContextCompat.startForegroundService(this, svc)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Prefs.isLoggedIn(this)) loadData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val id = intent.getLongExtra("open_task_id", -1)
        if (id != -1L) lifecycleScope.launch { openTaskDetail(id) }
    }

    private fun statusLabel(s: String) = when(s) {
        "pending"  -> "⏳ معلقة"
        "progress" -> "🔄 جارية"
        "done"     -> "✅ منجزة"
        else -> s
    }
    private fun priorityLabel(p: String) = when(p) {
        "high" -> "🔴 عالية"
        "low"  -> "🟢 منخفضة"
        else   -> "🟡 متوسطة"
    }
}
