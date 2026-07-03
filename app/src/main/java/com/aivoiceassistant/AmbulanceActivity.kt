package com.aivoiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.telephony.SmsManager
import android.view.KeyEvent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aivoiceassistant.data.api.ApiClient
import com.aivoiceassistant.data.api.AmbulanceRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import java.util.*

class AmbulanceActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var edtName: EditText
    private lateinit var edtPhone: EditText
    private lateinit var spinnerType: Spinner
    private lateinit var txtLocation: TextView
    private lateinit var btnRequest: Button
    private lateinit var btnWhatsApp: Button
    private lateinit var btnVoiceSOS: Button
    private lateinit var btnOpenMap: Button

    private var latitude = ""
    private var longitude = ""

    private val REQ_SPEECH = 300
    private val PERMISSION_LOCATION = 200
    private val PERMISSION_SMS = 201
    private val PERMISSION_AUDIO = 202

    private val scope  = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val userId get() = SessionManager.userId

    // Panic via volume up (3 presses quickly)
    private var volumePressCount = 0
    private var lastVolumePressTime = 0L
    private val PANIC_INTERVAL_MS = 3000L     // 3 seconds window

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ambulance)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        SessionManager.init(applicationContext)

        edtName = findViewById(R.id.edtName)
        edtPhone = findViewById(R.id.edtPhone)
        spinnerType = findViewById(R.id.spinnerEmergencyType)
        txtLocation = findViewById(R.id.txtLocation)
        btnRequest = findViewById(R.id.btnRequestAmbulance)
        btnWhatsApp = findViewById(R.id.btnWhatsAppSOS)
        btnVoiceSOS = findViewById(R.id.btnVoiceSOS)
        btnOpenMap = findViewById(R.id.btnOpenMap)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Spinner data
        val emergencyTypes = arrayOf(
            "Accident",
            "Breathing Issue",
            "Heart Pain",
            "Trauma / Bleeding",
            "Unconscious",
            "Other"
        )
        spinnerType.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, emergencyTypes)

        // Get GPS location on start
        requestLocation()

        // Main Request button -> SMS to 108 + family + open dialer
        btnRequest.setOnClickListener {
            triggerEmergency(fullMode = true)
        }

        // WhatsApp SOS only
        btnWhatsApp.setOnClickListener {
            sendWhatsAppSOS()
        }

        // Voice SOS
        btnVoiceSOS.setOnClickListener {
            startVoiceSOS()
        }

        // Open nearest hospital in Maps
        btnOpenMap.setOnClickListener {
            openNearestHospitalMap()
        }
    }

    // =============== EMERGENCY CORE LOGIC ===============

    private fun triggerEmergency(fullMode: Boolean) {
        val name = edtName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val emergency = spinnerType.selectedItem.toString()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Please enter name & contact number", Toast.LENGTH_SHORT).show()
            return
        }

        if (latitude.isEmpty() || longitude.isEmpty()) {
            Toast.makeText(this, "Location not available. Please enable GPS", Toast.LENGTH_SHORT).show()
            return
        }

        val message =
            "🚨 Ambulance Request\nName: $name\nContact: $phone\nType: $emergency\n" +
                    "Location: https://maps.google.com/?q=$latitude,$longitude"

        // 1) SMS to 108 + family
        sendEmergencySMS(message)

        if (fullMode) {
            // 2) Open dialer with 108
            openDialer("108")

            // 3) Optional: WhatsApp also
            sendWhatsAppSOS(message)
        }

        Toast.makeText(this, "Emergency request initiated!", Toast.LENGTH_LONG).show()

        // 4) Log to DB
        logRequestToDB(name, phone, emergency, "Button")
    }

    private fun sendEmergencySMS(message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                PERMISSION_SMS
            )
            Toast.makeText(this, "Grant SMS permission and try again", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔴 Replace with your trusted numbers
        val numbers = listOf(
            "108",          // Govt ambulance
            "91XXXXXXXXXX", // Family 1 (with country code)
            "91YYYYYYYYYY"  // Family 2
        )

        val smsManager = SmsManager.getDefault()
        for (num in numbers) {
            smsManager.sendTextMessage(num, null, message, null, null)
        }
    }

    private fun openDialer(number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
        startActivity(intent)
    }

    // =============== WHATSAPP SOS ===============

    private fun sendWhatsAppSOS(customMessage: String? = null) {
        val name = edtName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()
        val emergency = spinnerType.selectedItem.toString()

        val message = customMessage ?: run {
            if (latitude.isEmpty() || longitude.isEmpty()) {
                Toast.makeText(this, "Location not available for WhatsApp SOS", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            "🚨 Ambulance SOS (WhatsApp)\nName: $name\nContact: $phone\nType: $emergency\n" +
                    "Location: https://maps.google.com/?q=$latitude,$longitude"
        }

        // 🔴 Replace with one WhatsApp number WITH country code (no +)
        val whatsappNumber = "91ZZZZZZZZZZ"

        try {
            val uri = Uri.parse("smsto:$whatsappNumber")
            val intent = Intent(Intent.ACTION_SENDTO, uri)
            intent.setPackage("com.whatsapp")
            intent.putExtra(Intent.EXTRA_TEXT, message)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    // =============== VOICE SOS ===============

    private fun startVoiceSOS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_AUDIO
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'help' or 'ambulance' to trigger SOS")

        try {
            startActivityForResult(intent, REQ_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.getOrNull(0)?.lowercase(Locale.getDefault()) ?: ""

            if ("help" in spokenText || "ambulance" in spokenText || "bachao" in spokenText) {
                triggerEmergency(fullMode = true)
                val n = edtName.text.toString().trim()
                val p = edtPhone.text.toString().trim()
                val e = spinnerType.selectedItem.toString()
                logRequestToDB(n, p, e, "Voice")
            } else {
                Toast.makeText(this, "Command not recognised: $spokenText", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // =============== MAP NAVIGATION ===============

    private fun openNearestHospitalMap() {
        val uri = if (latitude.isNotEmpty() && longitude.isNotEmpty()) {
            Uri.parse("geo:$latitude,$longitude?q=hospital")
        } else {
            Uri.parse("geo:0,0?q=hospital")
        }

        val mapIntent = Intent(Intent.ACTION_VIEW, uri)
        mapIntent.setPackage("com.google.android.apps.maps")
        startActivity(mapIntent)
    }

    // =============== GPS LOCATION ===============

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_LOCATION
            )
            return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latitude = location.latitude.toString()
                longitude = location.longitude.toString()
                txtLocation.text = "📍 Location acquired"
            } else {
                txtLocation.text = "⚠ GPS not available"
            }
        }
    }

    // =============== PANIC via VOLUME UP ===============

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            val now = System.currentTimeMillis()
            if (now - lastVolumePressTime > PANIC_INTERVAL_MS) {
                // Reset if last press was long ago
                volumePressCount = 0
            }
            volumePressCount++
            lastVolumePressTime = now

            if (volumePressCount >= 3) {
                volumePressCount = 0
                triggerEmergency(fullMode = true)
                Toast.makeText(this, "Panic SOS triggered!", Toast.LENGTH_SHORT).show()
                val n = edtName.text.toString().trim()
                val p = edtPhone.text.toString().trim()
                val e = spinnerType.selectedItem.toString()
                logRequestToDB(n, p, e, "PanicVolume")
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ── Log to Database ──────────────────────────────────────────────────────
    private fun logRequestToDB(name: String, phone: String, emergency: String, triggeredVia: String) {
        scope.launch {
            try {
                val lat = latitude.toDoubleOrNull() ?: 0.0
                val lng = longitude.toDoubleOrNull() ?: 0.0
                withContext(Dispatchers.IO) {
                    ApiClient.phpApi.logAmbulanceRequest(
                        AmbulanceRequest(
                            user_id        = userId,
                            patient_name   = name.ifEmpty { "Unknown" },
                            contact_phone  = phone.ifEmpty { "N/A" },
                            emergency_type = emergency,
                            latitude       = lat,
                            longitude      = lng,
                            sms_sent       = 1,
                            whatsapp_sent  = 0,
                            triggered_via  = triggeredVia
                        )
                    )
                }
            } catch (_: Exception) { /* Silent */ }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
