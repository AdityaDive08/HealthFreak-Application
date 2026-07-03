package com.aivoiceassistant

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aivoiceassistant.data.api.ApiClient
import com.aivoiceassistant.data.api.LoginRequest
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Init session manager
        SessionManager.init(applicationContext)

        // Auto-login if session exists
        if (SessionManager.isLoggedIn) {
            goToDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        val edtEmail    = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val showPass    = findViewById<CheckBox>(R.id.chkShowPass)
        val btnLogin    = findViewById<Button>(R.id.btnLogin)
        val txtNewUser  = findViewById<TextView>(R.id.txtNewUser)
        val progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = android.view.Gravity.CENTER }
        }
        (btnLogin.parent as? LinearLayout)?.addView(progressBar)

        showPass.setOnCheckedChangeListener { _, checked ->
            edtPassword.transformationMethod =
                if (checked) HideReturnsTransformationMethod.getInstance()
                else PasswordTransformationMethod.getInstance()
            edtPassword.setSelection(edtPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val pass  = edtPassword.text.toString().trim()

            // Local validation first
            when {
                email.isEmpty() -> { edtEmail.error = "Required!"; return@setOnClickListener }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    { edtEmail.error = "Enter valid email!"; return@setOnClickListener }
                pass.isEmpty()  -> { edtPassword.error = "Required!"; return@setOnClickListener }
                pass.length < 8 -> { edtPassword.error = "Min 8 characters!"; return@setOnClickListener }
            }

            // Call PHP API
            btnLogin.isEnabled   = false
            progressBar.visibility = View.VISIBLE

            scope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.phpApi.login(LoginRequest(email, pass))
                    }
                    if (response.success) {
                        SessionManager.saveSession(response.user_id, response.name, response.email)
                        Toast.makeText(this@LoginActivity, "Welcome back, ${response.name}! 🎉", Toast.LENGTH_SHORT).show()
                        goToDashboard()
                    } else {
                        Toast.makeText(this@LoginActivity, response.message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity,
                        "Cannot connect to server. Check WiFi & XAMPP.\n${e.message}",
                        Toast.LENGTH_LONG).show()
                } finally {
                    btnLogin.isEnabled     = true
                    progressBar.visibility = View.GONE
                }
            }
        }

        txtNewUser.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
