package com.aivoiceassistant

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.aivoiceassistant.data.api.ApiClient
import com.aivoiceassistant.data.api.SosRequest
import kotlinx.coroutines.*

class SosActivity : AppCompatActivity(), LocationListener {

    private lateinit var locationManager: LocationManager
    private lateinit var spinnerEmergency: Spinner
    private lateinit var btnGetLocation: Button
    private lateinit var btnSendAlert: Button
    private lateinit var txtLocation: TextView

    private var latitude: Double? = null
    private var longitude: Double? = null

    private val scope  = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val userId get() = SessionManager.userId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sos)
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        SessionManager.init(applicationContext)

        spinnerEmergency = findViewById(R.id.spinnerEmergency)
        btnGetLocation   = findViewById(R.id.btnGetLocation)
        btnSendAlert     = findViewById(R.id.btnSendAlert)

        txtLocation = TextView(this).apply {
            textSize = 16f
            setPadding(10, 20, 10, 10)
        }
        findViewById<LinearLayout>(R.id.rootLayout).addView(txtLocation)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        val emergencyTypes = listOf(
            "Choose Emergency Type", "Fire", "Crime",
            "Accident", "Medical Emergency", "Other"
        )
        spinnerEmergency.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, emergencyTypes
        )

        btnGetLocation.setOnClickListener { getCurrentLocation() }
        btnSendAlert.setOnClickListener   { sendAlert() }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101
            )
            return
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 5f, this)
        Toast.makeText(this, "Fetching location…", Toast.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(location: Location) {
        latitude  = location.latitude
        longitude = location.longitude
        txtLocation.text = "📍 Location:\nLatitude: $latitude\nLongitude: $longitude"
    }

    private fun sendAlert() {
        val emergencyType = spinnerEmergency.selectedItem.toString()

        if (emergencyType == "Choose Emergency Type") {
            Toast.makeText(this, "Please select a valid emergency type!", Toast.LENGTH_SHORT).show()
            return
        }
        if (latitude == null || longitude == null) {
            Toast.makeText(this, "Get your location first!", Toast.LENGTH_SHORT).show()
            return
        }

        val message = "EMERGENCY: $emergencyType\nLocation: https://maps.google.com/?q=$latitude,$longitude"
        Toast.makeText(this, "Alert Sent:\n$message", Toast.LENGTH_LONG).show()

        // Log to database
        logAlertToDB(emergencyType)
    }

    private fun logAlertToDB(emergencyType: String) {
        if (userId <= 0) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    ApiClient.phpApi.logSosAlert(
                        SosRequest(
                            user_id        = userId,
                            emergency_type = emergencyType,
                            latitude       = latitude ?: 0.0,
                            longitude      = longitude ?: 0.0
                        )
                    )
                }
                Toast.makeText(this@SosActivity, "✅ Alert logged to database", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@SosActivity,
                    "Alert sent but not saved to DB: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
