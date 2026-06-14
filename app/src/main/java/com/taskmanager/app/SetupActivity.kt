package com.taskmanager.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        val etIp   = findViewById<EditText>(R.id.etServerIp)
        val etPort = findViewById<EditText>(R.id.etServerPort)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val tvError    = findViewById<TextView>(R.id.tvSetupError)
        val progress   = findViewById<ProgressBar>(R.id.setupProgress)

        // Pre-fill with default
        etIp.setText("192.168.1.100")
        etPort.setText("8090")

        btnConnect.setOnClickListener {
            val ip   = etIp.text.toString().trim()
            val port = etPort.text.toString().trim()

            if (ip.isEmpty()) {
                tvError.text = "يرجى إدخال عنوان IP"
                tvError.visibility = android.view.View.VISIBLE
                return@setOnClickListener
            }

            val url = "http://$ip:$port"
            tvError.visibility = android.view.View.GONE
            progress.visibility = android.view.View.VISIBLE
            btnConnect.isEnabled = false

            lifecycleScope.launch {
                val ok = withContext(Dispatchers.IO) {
                    ApiClient.configure(url)
                    ApiClient.me()
                }
                progress.visibility = android.view.View.GONE
                btnConnect.isEnabled = true

                when (ok) {
                    is ApiResult.Success -> {
                        // Server reachable — save and go to login
                        Prefs.setServer(this@SetupActivity, url)
                        startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                        finish()
                    }
                    is ApiResult.Error -> {
                        if (ok.code == 401) {
                            // Server found, user not logged in yet — that's fine
                            Prefs.setServer(this@SetupActivity, url)
                            startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                            finish()
                        } else {
                            tvError.text = "لا يمكن الاتصال بـ $url\nتأكد من IP والبورت وأن الجهاز على نفس الشبكة"
                            tvError.visibility = android.view.View.VISIBLE
                        }
                    }
                }
            }
        }
    }
}
