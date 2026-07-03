package com.aivoiceassistant

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aivoiceassistant.data.api.ApiClient
import com.aivoiceassistant.data.api.RegisterRequest
import kotlinx.coroutines.*

class RegisterActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtName     = findViewById<EditText>(R.id.edtName)
        val edtEmail    = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val name  = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val pass  = edtPassword.text.toString().trim()

            // Local validation
            when {
                name.isEmpty()  -> { edtName.error = "Required!"; return@setOnClickListener }
                email.isEmpty() -> { edtEmail.error = "Required!"; return@setOnClickListener }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    { edtEmail.error = "Enter valid email!"; return@setOnClickListener }
                pass.length < 8 -> { edtPassword.error = "Min 8 characters!"; return@setOnClickListener }
                !pass.any { it.isDigit() } ->
                    { edtPassword.error = "Include at least 1 number!"; return@setOnClickListener }
                !pass.any { !it.isLetterOrDigit() } ->
                    { edtPassword.error = "Include 1 special character!"; return@setOnClickListener }
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Creating Account…"

            scope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        ApiClient.phpApi.register(RegisterRequest(name, email, pass))
                    }
                    if (response.success) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Account created! Please Login 🎉",
                            Toast.LENGTH_LONG
                        ).show()
                        startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, response.message, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Cannot connect to server. Check WiFi & XAMPP.\n${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    btnRegister.isEnabled = true
                    btnRegister.text = "Create Account"
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
