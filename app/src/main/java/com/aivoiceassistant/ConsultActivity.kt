package com.aivoiceassistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.aivoiceassistant.data.api.ApiClient
import com.aivoiceassistant.data.api.Content
import com.aivoiceassistant.data.api.CreateSessionRequest
import com.aivoiceassistant.data.api.GeminiRequest
import com.aivoiceassistant.data.api.Part
import com.aivoiceassistant.data.api.SaveMessageRequest
import kotlinx.coroutines.*
import java.util.Locale

class ConsultActivity : AppCompatActivity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton



    private lateinit var btnMic: ImageButton
    private lateinit var btnSavePdf: ImageButton
    private lateinit var scrollViewChat: ScrollView

    private val SYSTEM_PROMPT = """
        You are a compassionate, knowledgeable AI doctor assistant.
        - Listen to the user's symptoms carefully.
        - Ask relevant follow-up questions when needed.
        - Provide a clear, informative, and empathetic response.
        - Suggest possible causes, home remedies, and when to see a real doctor.
        - Never replace a real doctor but always guide the user responsibly.
        - If the user asks a question NOT related to symptoms, diseases, or medical topics, politely refuse to answer and ask them to only ask for medical or symptoms related questions.
        Keep your responses concise and easy to understand.
    """.trimIndent()

    private val conversationHistory = mutableListOf<Content>()
    private var consultSessionId: Int = 0

    private val scope  = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val userId get() = SessionManager.userId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_consult)

        SessionManager.init(applicationContext)

        chatContainer  = findViewById(R.id.chatContainer)
        edtMessage     = findViewById(R.id.edtMessage)
        btnSend        = findViewById(R.id.btnSend)
        btnMic         = findViewById(R.id.btnMic)
        btnSavePdf     = findViewById(R.id.btnSavePdf)
        scrollViewChat = findViewById(R.id.scrollViewChat)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        // Removed incorrect system prompt initialization as content
        addBotMessage("👋 Hello! I'm your AI Doctor Assistant.\nDescribe your symptoms and I'll help you.")

        createDbSession()

        btnSend.setOnClickListener {
            val text = edtMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessage(text)
                edtMessage.text.clear()
                askGemini(text)
            }
        }

        btnMic.setOnClickListener { startVoiceInput() }
        
        btnSavePdf.setOnClickListener {
            val transcript = formatConversationHistory()
            if (transcript.isNotEmpty()) {
                val intent = Intent(this, PdfPreviewActivity::class.java)
                intent.putExtra("EXTRA_CHAT_TEXT", transcript)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No conversation to save yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatConversationHistory(): String {
        if (conversationHistory.isEmpty()) return ""
        val builder = StringBuilder()
        for (content in conversationHistory) {
            val role = if (content.role == "user") "Patient" else "AI Doctor"
            val text = content.parts.firstOrNull()?.text ?: ""
            builder.append("$role:\n$text\n\n")
        }
        return builder.toString().trim()
    }

    // ── Create DB session ───────────────────────────────────────────────────
    private fun createDbSession() {
        if (userId <= 0) return
        scope.launch {
            try {
                val resp = withContext(Dispatchers.IO) {
                    ApiClient.phpApi.createConsultSession(CreateSessionRequest(user_id = userId))
                }
                if (resp.success) consultSessionId = resp.session_id
            } catch (_: Exception) { /* Silent */ }
        }
    }

    // ── Save message to DB silently ─────────────────────────────────────────
    private fun saveMessageToDB(role: String, content: String) {
        if (consultSessionId <= 0) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.phpApi.saveConsultMessage(
                        SaveMessageRequest(session_id = consultSessionId, role = role, content = content)
                    )
                }
            } catch (_: Exception) { /* Silent */ }
        }
    }

    // ── Voice input ─────────────────────────────────────────────────────────
    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your symptoms…")
        }
        try {
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQ_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input not supported.", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Required for voice result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull().orEmpty()
            if (spoken.isNotEmpty()) edtMessage.setText(spoken)
        }
    }

    // ── Chat bubbles ────────────────────────────────────────────────────────
    private fun addUserMessage(text: String): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.setPadding(32, 24, 32, 24)
        tv.setTextColor(0xFFFFFFFF.toInt())
        tv.textSize = 15f
        tv.background = getDrawable(R.drawable.bg_chat_user)
        tv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.END; setMargins(120, 12, 0, 12) }
        
        // Add elevation for modern look
        tv.elevation = 2f
        
        chatContainer.addView(tv)
        scrollBottom()
        return tv
    }

    private fun addBotMessage(text: String): TextView {
        val tv = TextView(this)
        tv.text = ""
        tv.setPadding(32, 24, 32, 24)
        tv.setTextColor(0xFF1F2937.toInt())
        tv.textSize = 15f
        tv.background = getDrawable(R.drawable.bg_chat_ai)
        tv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.START; setMargins(0, 12, 120, 12) }
        
        // Add elevation for modern look
        tv.elevation = 2f
        
        chatContainer.addView(tv)
        scrollBottom()
        
        scope.launch {
            for (i in text.indices) {
                tv.append(text[i].toString())
                if (i % 3 == 0) scrollBottom()
                delay(15)
            }
            scrollBottom()
        }
        return tv
    }

    private fun scrollBottom() {
        scrollViewChat.post { scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun addLoadingBubble(): TextView {
        val tv = TextView(this)
        tv.text = "."
        tv.setPadding(32, 24, 32, 24)
        tv.setTextColor(0xFF1F2937.toInt())
        tv.textSize = 24f
        tv.background = getDrawable(R.drawable.bg_chat_ai)
        tv.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.START; setMargins(0, 12, 120, 12) }
        tv.elevation = 2f
        
        chatContainer.addView(tv)
        scrollBottom()
        
        scope.launch {
            var dots = 1
            while (isActive && tv.parent != null) {
                tv.text = ".".repeat(dots)
                dots = if (dots < 3) dots + 1 else 1
                delay(400)
            }
        }
        return tv
    }

    // ── Gemini call with auto-retry on 429 ─────────────────────────────────
    private fun askGemini(userMsg: String, retryCount: Int = 0) {
        btnSend.isEnabled = false
        btnMic.isEnabled  = false

        if (retryCount == 0) {
            conversationHistory.add(Content(role = "user", parts = listOf(Part(userMsg))))
            saveMessageToDB("user", userMsg)
        }
        
        val loadingBubble = addLoadingBubble()

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.api.sendPrompt(
                        GeminiRequest(
                            systemInstruction = Content(parts = listOf(Part(SYSTEM_PROMPT))),
                            contents = conversationHistory.toList()
                        )
                    )
                }

                val replyTextRaw = response.candidates
                    .firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "⚠ No response. Please try again."
                val replyText = replyTextRaw.replace("*", "")

                conversationHistory.add(Content(role = "model", parts = listOf(Part(replyText))))
                saveMessageToDB("model", replyText)

                chatContainer.removeView(loadingBubble)
                addBotMessage(replyText)

            } catch (e: Exception) {
                val msg = e.message ?: ""
                val is429 = msg.contains("429") ||
                        msg.contains("Too Many Requests", ignoreCase = true)

                // Auto-retry up to 3 times with countdown for 429
                if (is429 && retryCount < 3) {
                    chatContainer.removeView(loadingBubble)
                    val waitSeconds = (retryCount + 1) * 30  // 30s → 60s → 90s
                    delay(waitSeconds * 1000L)
                    askGemini(userMsg, retryCount + 1)
                    return@launch  // skip finally re-enable until real result
                }

                chatContainer.removeView(loadingBubble)
                // Show user-friendly error
                val friendly = when {
                    is429 ->
                        "⚠ The AI is currently busy (rate limit).\nPlease wait 1 minute and try again."
                    msg.contains("404") ->
                        "⚠ AI model not found. Check your API key."
                    msg.contains("401") || msg.contains("403") ->
                        "⚠ Invalid or expired API key."
                    msg.contains("Unable to resolve host") ||
                    msg.contains("timeout", ignoreCase = true) ->
                        "⚠ No internet. Please check your network."
                    else -> "⚠ Error: $msg"
                }
                addBotMessage(friendly)

            } finally {
                btnSend.isEnabled = true
                btnMic.isEnabled  = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val REQ_SPEECH = 101
    }
}
